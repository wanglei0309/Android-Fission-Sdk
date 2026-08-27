// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: BluetoothStateManager.kt is a singleton utility that tracks the Bluetooth connection status across the app
 * It provides a centralized way to check or update whether a Bluetooth device is currently connected.
 **/
package com.infineon.secora.wallet.domain.wearable.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import com.infineon.secora.wallet.client.util.PropertiesLoader
import com.infineon.secora.wallet.PayExternalLaunch
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.utils.constants.Constants.SECORA
import com.infineon.secora.wearable.BleConnectConfig
import com.infineon.secora.wearable.SecoraWearableSDK
import com.infineon.secora.wearable.ble.BleProtocol
import com.infineon.secora.wearable.protocolapi.IHostSharedBleProtocol
import com.infineon.secora.wearable.protocolapi.ISecoraBleProtocol
import java.util.Properties
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * [BluetoothStateManager] is a singleton object used to keep track of the
 * current Bluetooth connection state across the entire application.
 *
 * It stores all BLE devices that are in connected state (multiple devices can
 * be connected at the same time). Use this to show green BLE icon for connected
 * devices and black for disconnected.
 *
 * Usage Example:
 * ```kotlin
 * // Check if any device is connected
 * if (BluetoothStateManager.isConnected) { ... }
 *
 * // Check if a specific device (by address or seId) is connected
 * if (BluetoothStateManager.isDeviceConnected(seId, context)) { ... }
 * if (BluetoothStateManager.isDeviceConnectedByAddress(address)) { ... }
 *
 * // Add/remove by address (called from receivers and when app connects/disconnects)
 * BluetoothStateManager.addConnectedDevice(address)
 * BluetoothStateManager.removeConnectedDevice(address)
 * ```
 */
object BluetoothStateManager {

    private const val BLE_PARAMETER_CONFIG_PROPERTIES = "bleParameterConfig.properties"

    private const val PROP_BLE_SERVICE_UUID = "BLE_SERVICE_UUID"
    private const val PROP_BLE_CCCD_UUID = "BLE_CCCD_UUID"
    private const val PROP_BLE_REQUEST_UUID = "BLE_REQUEST_UUID"
    private const val PROP_BLE_RESPONSE_UUID = "BLE_RESPONSE_UUID"
    private const val PROP_BLE_DEVICE_NAME_DISCOVERY_FILTER_PATTERN =
        "BLE_DEVICE_NAME_DISCOVERY_FILTER_PATTERN"

    /**
     * Primary BLE service UUID from `assets/bleParameterConfig.properties` (`BLE_SERVICE_UUID`), with [PropertiesLoader] fallback.
     */
    fun bleServiceUuid(context: Context): String = readBleProperty(context, PROP_BLE_SERVICE_UUID)

    /**
     * CCCD UUID from `assets/bleParameterConfig.properties` (`BLE_CCCD_UUID`), with [PropertiesLoader] fallback.
     */
    fun bleCccdUuid(context: Context): String = readBleProperty(context, PROP_BLE_CCCD_UUID)

    /**
     * Request characteristic UUID from `assets/bleParameterConfig.properties` (`BLE_REQUEST_UUID`), with [PropertiesLoader] fallback.
     */
    fun bleRequestUuid(context: Context): String = readBleProperty(context, PROP_BLE_REQUEST_UUID)

    /**
     * Response characteristic UUID from `assets/bleParameterConfig.properties` (`BLE_RESPONSE_UUID`), with [PropertiesLoader] fallback.
     */
    fun bleResponseUuid(context: Context): String = readBleProperty(context, PROP_BLE_RESPONSE_UUID)

    /**
     * Prefix used when filtering discovered devices for the scan list (BLE scan, classic discovery, bonded pre-seed).
     * Loaded from `assets/bleParameterConfig.properties` (`BLE_DEVICE_NAME_DISCOVERY_FILTER_PATTERN`); names must
     * [String.startsWith] this value. Falls back to [SECORA] when unset or empty.
     */
    fun bleDeviceNameDiscoveryFilterPattern(context: Context): String {
        val pattern = readBleProperty(context, PROP_BLE_DEVICE_NAME_DISCOVERY_FILTER_PATTERN)
        return pattern.ifEmpty { SECORA }
    }

    /**
     * Resolves a BLE-related property from the in-memory loader first, then from `bleParameterConfig.properties` in assets.
     *
     * @param context Android context (application context is used for assets).
     * @param key     Property name (e.g. [PROP_BLE_SERVICE_UUID]).
     * @return Trimmed value or empty string if unset or unreadable.
     */
    private fun readBleProperty(context: Context, key: String): String {
        PropertiesLoader.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return try {
            val props = Properties()
            context.applicationContext.assets.open(BLE_PARAMETER_CONFIG_PROPERTIES).use { props.load(it) }
            props.getProperty(key)?.trim().orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val connectionListeners = mutableListOf<Runnable>()
    private val lock = Any()
    private val connectedDeviceAddresses = mutableSetOf<String>()

    /** Active SECORA protocol instance, kept so it can be reused across fragments / ScriptHandler instances. */
    @Volatile
    var activeProtocol: ISecoraBleProtocol? = null
        private set

    /**
     * Registers a listener to be run on the main thread when connection state changes.
     * Call [removeOnConnectionStateChanged] with the same runnable to unregister.
     */
    @MainThread
    fun addOnConnectionStateChanged(listener: Runnable) {
        connectionListeners.add(listener)
    }

    /**
     * Unregisters a previously added connection state listener.
     */
    @MainThread
    fun removeOnConnectionStateChanged(listener: Runnable) {
        connectionListeners.remove(listener)
    }

    /**
     * Whether at least one BLE device is currently connected.
     */
    val isConnected: Boolean
        get() = synchronized(lock) { connectedDeviceAddresses.isNotEmpty() }

    var isBluetoothEnabled: Boolean = false
        private set

    /**
     * Normalizes an SE ID for comparison (e.g. strip dashes, trim).
     */
    private fun normalizeSeId(seId: String?): String? =
        seId?.replace("-", "")?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * Normalizes Bluetooth address to a canonical form for comparison (uppercase, no separators).
     * Ensures add/remove match whether address comes from prefs, Intent, or device (with or without colons).
     */
    private fun normalizeAddress(address: String?): String? =
        address?.trim()?.uppercase()?.replace(":", "")?.replace("-", "")?.takeIf { it.isNotEmpty() }

    /**
     * Returns true if the device with the given [seId] is in the connected set.
     * Resolves seId to address via prefs (BLE_ADDRESS_$seId, SELECTED_DEVICE_ADDRESS) and
     * reconciles against [activeProtocol] for host-shared GATT links.
     */
    fun isDeviceConnected(seId: String?, context: Context? = null): Boolean {
        if (seId.isNullOrBlank() || context == null) return false
        if (!isBluetoothTurnedOn(context)) return false

        reconcileActiveProtocolConnection(seId)

        val address = resolveDeviceAddress(seId)
        return !address.isNullOrBlank() && isDeviceConnectedByAddress(address)
    }

    /**
     * Resolves the BLE MAC for [seId] from persisted mappings and host-launch context.
     */
    private fun resolveDeviceAddress(seId: String): String? {
        val normalizedSeId = normalizeSeId(seId) ?: return null

        StorageRepository.readString(PreferenceKey.bleAddressKey(seId))
            .takeIf { it.isNotBlank() }
            ?.let { return it }
        StorageRepository.readString(PreferenceKey.bleAddressKey(normalizedSeId))
            .takeIf { it.isNotBlank() }
            ?.let { return it }

        val currentSeId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)
        if (normalizeSeId(currentSeId) != normalizedSeId) return null

        StorageRepository.readString(PreferenceKey.SELECTED_DEVICE_ADDRESS)
            .takeIf { it.isNotBlank() }
            ?.let { return it }

        return activeProtocol?.bluetoothDevice?.address?.takeIf { it.isNotBlank() }
    }

    /**
     * Re-adds the host/shared SECORA link to the connected set when [activeProtocol] is live
     * but a transient ACL disconnect cleared the address cache.
     */
    private fun reconcileActiveProtocolConnection(seId: String) {
        val protocol = activeProtocol ?: return
        val protocolAddress = protocol.bluetoothDevice.address?.takeIf { it.isNotBlank() } ?: return
        val resolved = resolveDeviceAddress(seId)

        val matches = when {
            resolved != null ->
                normalizeAddress(resolved) == normalizeAddress(protocolAddress)
            PayExternalLaunch.isHostLaunch() ->
                normalizeSeId(StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)) ==
                    normalizeSeId(seId) &&
                    PayExternalLaunch.addressesMatch(
                        StorageRepository.readString(PreferenceKey.SELECTED_DEVICE_ADDRESS),
                        protocolAddress
                    )
            else -> false
        }

        if (!matches) return

        addConnectedDevice(protocolAddress, seId)
        if (StorageRepository.readString(PreferenceKey.bleAddressKey(seId)).isBlank()) {
            StorageRepository.saveString(PreferenceKey.bleAddressKey(seId), protocolAddress)
        }
    }

    /**
     * Returns true if the device with the given Bluetooth [address] is in the connected set.
     */
    fun isDeviceConnectedByAddress(address: String?): Boolean {
        if (address.isNullOrBlank()) return false
        val a = normalizeAddress(address) ?: return false
        return synchronized(lock) { connectedDeviceAddresses.any { normalizeAddress(it) == a } }
    }

    /**
     * Adds a device to the connected set by address. Call when ACTION_ACL_CONNECTED or when app establishes BLE connection.
     * @param address Bluetooth MAC address of the device (stored normalized).
     * @param seId Ignored; kept for API compatibility. List items use [isDeviceConnected](seId, context) which resolves via prefs.
     */
    fun addConnectedDevice(address: String, seId: String? = null) {
        val addr = normalizeAddress(address) ?: return
        var changed = false
        synchronized(lock) {
            if (connectedDeviceAddresses.add(addr)) changed = true
        }
        if (changed) notifyListeners()
    }

    /**
     * Removes only this device from the connected set by address. Call when ACTION_ACL_DISCONNECTED for that device.
     * Other connected devices remain; their BLE icons stay green.
     */
    fun removeConnectedDevice(address: String) {
        val address = normalizeAddress(address) ?: return
        var changed = false
        synchronized(lock) {
            if (connectedDeviceAddresses.remove(address)) changed = true
        }
        if (changed) notifyListeners()
    }

    /**
     * Clears all connected devices (e.g. when Bluetooth is turned off).
     */
    fun clearAllConnectedDevices() {
        var changed = false
        synchronized(lock) {
            if (connectedDeviceAddresses.isNotEmpty()) changed = true
            connectedDeviceAddresses.clear()
        }
        if (changed) notifyListeners()
    }

    private fun notifyListeners() {
        val listeners = connectionListeners.toList()
        mainHandler.post { listeners.forEach { it.run() } }
    }

    /**
     * Stores the active BLE protocol so it can be reused by any ScriptHandler.
     * Call this whenever a new BLE connection is successfully established.
     */
    fun setActiveProtocol(protocol: ISecoraBleProtocol?) {
        activeProtocol = protocol
    }

    /**
     * Registers a SECORA protocol backed by the host Fission BLE link (no second GATT).
     *
     * @param protocol Protocol discovered on the shared connection.
     * @param deviceAddress Normalized MAC of the connected wearable.
     * @param seId Optional SE ID to associate with the connection cache.
     */
    fun registerHostProtocol(
        protocol: ISecoraBleProtocol,
        deviceAddress: String,
        seId: String? = null
    ) {
        activeProtocol = protocol
        addConnectedDevice(deviceAddress, seId)
        val effectiveSeId = seId?.takeIf { it.isNotBlank() }
            ?: StorageRepository.readString(PreferenceKey.DEVICE_SE_ID).takeIf { it.isNotBlank() }
        if (effectiveSeId != null) {
            StorageRepository.saveString(PreferenceKey.bleAddressKey(effectiveSeId), deviceAddress)
        }
        StorageRepository.saveString(PreferenceKey.SELECTED_DEVICE_ADDRESS, deviceAddress)
        getApplicationLogger("BluetoothStateManager")
            .debug("Registered host shared SECORA protocol for $deviceAddress seId=$effectiveSeId")
    }

    /**
     * Disconnects and clears any active BLE protocol.
     * Must be called before opening a new GATT connection to the same device to
     * avoid duplicate connections (which hang on Android 13).
     */
    fun disconnectActiveProtocol() {
        val old = activeProtocol
        activeProtocol = null
        if (old != null) {
            try {
                when (old) {
                    is IHostSharedBleProtocol -> old.releaseSharedChannel()
                    else -> SecoraWearableSDK.getInstance().getInterface().disconnectBLEDevice(old)
                }
            } catch (_: Exception) {
                // best-effort disconnect
            }
        }
    }

    /**
     * Updates the cached phone Bluetooth adapter enabled flag (for UI that cannot read adapter state directly).
     *
     * @param enabled `true` when Bluetooth is on.
     */
    fun setBluetoothEnabled(enabled: Boolean) {
        isBluetoothEnabled = enabled
    }

    /**
     * Single entry point for BLE connection in the app.
     * Replaces direct BleProtocol.connect() calls. Use this wherever a BLE connection is needed.
     *
     * @param context The application context
     * @param device  The Bluetooth device to connect to
     * @return CompletableFuture that completes with BleProtocol on success
     */
    fun connectBleDevice(context: Context, device: BluetoothDevice): CompletableFuture<BleProtocol> {
        val appContext = context.applicationContext
        val connectConfig = BleConnectConfig(
            device,
            bleServiceUuid(appContext),
            bleCccdUuid(appContext),
            bleRequestUuid(appContext),
            bleResponseUuid(appContext)
        )
        return SecoraWearableSDK.getInstance().getInterface().connectBLEDevice(context, connectConfig)
    }

    /**
     * Check for the phone’s Bluetooth status.
     *
     * @param context The application context.
     */
    fun isBluetoothTurnedOn(context: Context): Boolean {
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        val adapter = bluetoothManager?.adapter
        return adapter?.isEnabled == true
    }

    /**
     * Ensures a BLE connection exists for the given Secure Element (SE).
     *
     * This method:
     * 1. Returns immediately if a valid BLE connection already exists
     * 2. Attempts to reconnect using the SE → Bluetooth address mapping
     * 3. Enforces a timeout to avoid hanging flows
     * 4. Guarantees the callback is invoked exactly once
     *
     * @param context Android context required for BLE connection
     * @param bluetoothAdapter Adapter used to retrieve the remote BLE device
     * @param currentProtocol Existing BLE protocol instance (if already connected)
     * @param seId Secure Element identifier used to derive the BLE address
     * @param seIdToBluetoothAddress Function mapping SE ID → Bluetooth MAC address
     * @param onReady Callback invoked with a valid protocol or null on failure
     */
    fun ensureBleConnection(
        context: Context,
        bluetoothAdapter: BluetoothAdapter,
        currentProtocol: ISecoraBleProtocol?,
        seId: String?,
        seIdToBluetoothAddress: (String) -> String,
        onReady: (ISecoraBleProtocol?) -> Unit
    ) {
        val logger = getApplicationLogger("BluetoothStateManager")
        val handler = Handler(Looper.getMainLooper())
        var completed = false

        fun finish(protocol: ISecoraBleProtocol?) {
            if (completed) return
            completed = true
            handler.removeCallbacksAndMessages(null)
            onReady(protocol)
        }

        val active = activeProtocol
        if (isConnected && currentProtocol != null) {
            logger.info("BLE already connected → continuing delink")
            finish(currentProtocol)
            return
        }
        if (active != null && isConnected) {
            logger.info("Reusing active BLE protocol for delink")
            finish(active)
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            logger.error("Bluetooth is OFF")
            finish(null)
            return
        }

        if (seId.isNullOrBlank()) {
            logger.error("SE ID missing")
            finish(null)
            return
        }

        val address = runCatching { seIdToBluetoothAddress(seId) }.getOrNull()
        if (address == null) {
            logger.error("Failed to derive BLE address")
            finish(null)
            return
        }

        val reconnectTimeoutMs = 28_000L
        handler.postDelayed({
            logger.error("BLE reconnect timeout (${reconnectTimeoutMs / 1000}s)")
            finish(null)
        }, reconnectTimeoutMs)

        try {
            val device = bluetoothAdapter.getRemoteDevice(address)
            logger.info("Attempting BLE reconnect → $address")

            connectBleDevice(context, device)
                .orTimeout(25, TimeUnit.SECONDS)
                .thenApply { protocol ->
                    logger.info("BLE reconnected successfully")
                    setActiveProtocol(protocol)
                    addConnectedDevice(address, seId)
                    finish(protocol)
                }
                .exceptionally { error ->
                    logger.error("BLE reconnect failed: ${error.message}")
                    finish(null)
                    null
                }

        } catch (e: Exception) {
            logger.error("BLE reconnect exception: ${e.message}")
            finish(null)
        }
    }
}
