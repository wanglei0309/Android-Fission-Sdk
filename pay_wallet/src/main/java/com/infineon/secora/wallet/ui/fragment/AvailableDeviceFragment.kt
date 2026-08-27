// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: AvailableDeviceFragment.kt is a comprehensive fragment that manages Bluetooth and BLE scanning for SECORA wearable devices.
 * It handles device discovery, connection, registration, and UI updates while managing permissions, connection states, and error handling.
 * The fragment integrates with SecoraWalletSDK
 **/
package com.infineon.secora.wallet.ui.fragment

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.PayExternalLaunch
import com.infineon.secora.wallet.utils.helper.ConfiguredWalletIdentity
import com.infineon.secora.wallet.adapter.BluetoothScanAdapter
import com.infineon.secora.wallet.adapter.PaymentDeviceAdapter
import com.infineon.secora.wallet.cdcvm.BodyPresenceTracker
import com.infineon.secora.wallet.cdcvm.CdcvmApi
import com.infineon.secora.wallet.cdcvm.WearableHandState
import com.infineon.secora.wallet.cdcvm.WearableStatus
import com.infineon.secora.wallet.cdcvm.WearableStatusMonitor
import com.infineon.secora.wallet.client.data.models.AcknowledgeResponse
import com.infineon.secora.wallet.client.data.models.DeleteScriptResponse
import com.infineon.secora.wallet.client.data.models.GetPendingResponse
import com.infineon.secora.wallet.client.data.models.ScriptExecutionResults
import com.infineon.secora.wallet.client.data.models.common.DeleteDeviceResponse
import com.infineon.secora.wallet.client.data.models.common.DeleteScript
import com.infineon.secora.wallet.client.data.models.common.DeleteScriptList
import com.infineon.secora.wallet.client.data.models.common.PaymentDeviceResponseBody
import com.infineon.secora.wallet.client.data.models.prepse.ScriptItem
import com.infineon.secora.wallet.client.operations.common.enums.CardStatus
import com.infineon.secora.wallet.client.operations.middleware.callbacks.UiCallback
import com.infineon.secora.wallet.client.util.AppDispatchers
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.DialogCommonMessageBinding
import com.infineon.secora.wallet.databinding.FragmentWalletAvailableDeviceBinding
import com.infineon.secora.wallet.utils.helper.showSecure
import com.infineon.secora.wallet.domain.devicedetach.DeviceDetachBleCleanup
import com.infineon.secora.wallet.domain.devicedetach.DeviceDetachTargetResolver
import com.infineon.secora.wallet.domain.walletsdk.WalletRepository
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothUiStateManager
import com.infineon.secora.wallet.domain.wearable.ble.script.ScriptHandler
import com.infineon.secora.wallet.domain.wearable.ble.script.ScriptRunner
import com.infineon.secora.wallet.firebase.AppEvent
import com.infineon.secora.wallet.firebase.EventBus
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.models.BluetoothDeviceUiModel
import com.infineon.secora.wallet.models.PaymentDeviceList
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.utils.CommonResponse
import com.infineon.secora.wallet.utils.helper.FcmSecureFlowCoordinator
import com.infineon.secora.wallet.utils.ImageUtils
import com.infineon.secora.wallet.utils.helper.ManualDeviceDelinkGate
import com.infineon.secora.wallet.utils.helper.NfcScriptExecutionTracker
import com.infineon.secora.wallet.utils.helper.PendingDeleteScriptExecutionGate
import com.infineon.secora.wallet.utils.helper.ScriptDataParser
import com.infineon.secora.wallet.utils.StringUtils.isEmptyString
import com.infineon.secora.wallet.utils.Utils.isNetworkAvailable
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.constants.Constants
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_DEVICE_DETACH_COMPLETED
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_DEVICE_STATUS_UPDATE
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_LISTENER
import com.infineon.secora.wallet.utils.constants.Constants.BLUETOOTH_PERMISSION
import com.infineon.secora.wallet.utils.constants.Constants.BLUETOOTH_SCAN_PERMISSION_REQUIRED
import com.infineon.secora.wallet.utils.constants.Constants.CPLC
import com.infineon.secora.wallet.utils.constants.Constants.DEFAULT_CARD_CHANGE
import com.infineon.secora.wallet.utils.constants.Constants.DEFAULT_DEVICE_NAME
import com.infineon.secora.wallet.utils.constants.Constants.DEVICE_SUSPEND_UPDATE
import com.infineon.secora.wallet.utils.constants.Constants.DELETE_SCRIPT_CLEAR_DEFAULT
import com.infineon.secora.wallet.utils.constants.Constants.UNKNOWN_DEVICE
import com.infineon.secora.wearable.SecoraWearableSDK
import com.infineon.secora.wearable.apdu.ApduResponsesItem
import com.infineon.secora.wearable.ble.BleProtocol
import com.infineon.secora.wearable.nfc.ScriptExecutionCallback
import com.infineon.secora.wearable.protocolapi.IHostSharedBleProtocol
import com.infineon.secora.wearable.protocolapi.IAsyncProtocol
import com.infineon.secora.wearable.protocolapi.ISecoraBleProtocol
import com.infineon.secora.wearable.util.SeConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock

/*
 * AvailableDeviceFragment is used to show the available Bluetooth devices
 */
class AvailableDeviceFragment : BaseFragment(), ScriptExecutionCallback {

    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)
    private lateinit var binding: FragmentWalletAvailableDeviceBinding
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val bluetoothDevices = mutableListOf<BluetoothDevice>()
    private lateinit var deviceAdapter: BluetoothScanAdapter
    private lateinit var activity: MainActivity
    private val bluetoothList = mutableListOf<String>()
    private val bleLock = ReentrantLock()
    private val handler = Handler(Looper.getMainLooper())

    // Connected BLE devices are stored in BluetoothStateManager (multi-device); use it for icon state.
    private var isScanning = false
    private var lastConnectedDevice: BluetoothDevice? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanRunnable: Runnable? = null
    private val scanTimeoutMs = 15000L // 15 seconds scan timeout
    private val scanLoaderMinVisibleMs = 1000L
    private val scanNoDeviceCheckpointMs = 5000L
    private var scanLoaderStartTimeMs = 0L
    private var scanLoaderSessionToken = 0L
    private var noDeviceCheckpointRunnable: Runnable? = null
    private var delayedScanRunnable: Runnable? = null
    private val scanRetryDelayMs = 5000L
    private var activeDialog: AlertDialog? = null
    private var postRegisterLoaderSafetyRunnable: Runnable? = null
    private val linkedDeviceAddresses = mutableSetOf<String>()
    private val scanUpdateHandler = Handler(Looper.getMainLooper())
    private var isUpdateScheduled = false
    private var scanEventCount = 0
    private var lastLogTime = 0L
    private var tagId = ""

    private data class ScriptExecutionContext(
        val deleteScriptList: List<Any>,
        val currentIndex: Int,
        val seId: String,
        val selectedDevice: String,
        val retryCount: Int,
        val scriptResults: MutableList<Boolean>,
        val suppressPostDelinkActions: Boolean = false,
        val delinkFromDifferentUser: Boolean = false
    )

    data class DelinkOptions(
        val suppressPostDelinkActions: Boolean = false,
        val delinkFromDifferentUser: Boolean = false
    )

    enum class SecureBLEState {
        UNPAIRED,
        PAIRING,
        PAIRED_CONNECTED,
        GHOST_PAIRED
    }

    private var secureBleState: SecureBLEState = SecureBLEState.UNPAIRED

    private companion object {
        private const val FALLBACK_SEQUENCE_COUNTER = "161"
    }

    /**
     * BLE Scan callback for handling scan results and errors.
     */
    private val bleScanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!hasBluetoothPermissions()) {
                handleMissingPermissions()
            } else {
                handleScanResult(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            logger.error("BLE scan failed with error code: $errorCode")
            isScanning = false
            noDeviceCheckpointRunnable?.let { handler.removeCallbacks(it) }
            noDeviceCheckpointRunnable = null
            scanRunnable?.let { handler.removeCallbacks(it) }
            scanRunnable = null
            requireActivity().runOnUiThread {
                applyScanBackgroundBlur(false)
                binding.scanLoader.clProgressBar.visibility = View.GONE
                binding.rvScanDevices.visibility = View.VISIBLE
                binding.swipeRefreshScan.isRefreshing = false
                binding.swipeRefreshScan.isEnabled = true

                when (errorCode) {
                    ScanCallback.SCAN_FAILED_ALREADY_STARTED -> {
                        showScanEmptyStateMessage(getString(R.string.bluetooth_scan_already_active))
                    }

                    ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> {
                        showScanEmptyStateMessage(getString(R.string.bluetooth_scan_failed_try_again_delayed))

                        delayedScanRunnable?.let { handler.removeCallbacks(it) }
                        delayedScanRunnable = Runnable {
                            if (
                                isAdded &&
                                lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                                binding.swipeRefreshScan.visibility == View.VISIBLE
                            ) {
                                logger.debug("Retrying BLE scan after scan frequency backoff")
                                startBleScan()
                            }
                        }
                        handler.postDelayed(delayedScanRunnable!!, scanRetryDelayMs)
                    }

                    ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> {
                        showScanEmptyStateMessage(getString(R.string.bluetooth_scan_feature_unsupported))
                    }

                    ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> {
                        showScanEmptyStateMessage(getString(R.string.bluetooth_scan_internal_error))
                    }

                    else -> {
                        showScanEmptyStateMessage(getString(R.string.bluetooth_scan_failed_generic))
                    }
                }
            }
        }
    }

    /**
     * Checks and shows permission-related messages/toasts.
     */
    private fun handleMissingPermissions() {
        logger.debug("BLE scan: BLUETOOTH_CONNECT permission not granted")
        showToast(BLUETOOTH_PERMISSION)
        requestBluetoothPermissions()
        StorageRepository.saveBoolean(PreferenceKey.BACK_PRESSED_FLAG, false)
        binding.customBottom.root.visibility = View.VISIBLE
        binding.customBottom.llActive.visibility = View.VISIBLE
        binding.customBottom.llPassive.visibility = View.GONE
        binding.tvCancel.visibility = View.GONE
        binding.tvNoDevice.visibility =
            if (distinctDevices.isEmpty()) View.VISIBLE else View.GONE
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        showLoading(false)
    }

    /**
     * Determines whether a BLE scan result should be ignored before adding to the device list.
     *
     * Filters out non-SECORA devices, already-linked wearables, and duplicates already present
     * in [bluetoothDevices].
     *
     * @param result The [ScanResult] from the BLE scan callback.
     * @param device The [BluetoothDevice] associated with the scan result.
     * @return A reason code (`not_secora`, `linked`, `duplicate`) when the result should be skipped,
     *         or `null` when the device may be added.
     */
    private fun getBleScanIgnoreReason(result: ScanResult, device: BluetoothDevice): String? {
        if (!isSecoraScanResult(result)) return "not_secora"
        if (linkedDeviceAddresses.contains(device.address)) return "linked"
        if (bluetoothDevices.any { it.address == device.address }) return "duplicate"
        return null
    }

    /**
     * Handles the result of a BLE scan for a discovered Bluetooth device.
     *
     * @param result [ScanResult] received from scanning callback
     *
     * @throws SecurityException If required Bluetooth permissions are missing.
     */
    private fun handleScanResult(result: ScanResult) {
        background {
            if (!bluetoothAdapter.isEnabled) {
                return@background
            }
            if (!isAdded || view == null) return@background

            try {
                val startTime = SystemClock.elapsedRealtime()
                val device = result.device
                val rssi = result.rssi
                val deviceName = withContext(AppDispatchers.IO) { resolveDeviceNameFromScan(result) }

                val ignoreReason = getBleScanIgnoreReason(result, device)
                if (ignoreReason != null) {
                    logger.debug(
                        "BLE scan ignored device: name=$deviceName addr=${device.address} reason=$ignoreReason"
                    )
                    return@background
                }

                logDeviceDetails(device, deviceName, rssi)

                bluetoothDevices.add(device)

                scanEventCount++
                val now = SystemClock.elapsedRealtime()

                if (now - lastLogTime > 1000) {
                    logger.info("SCAN_STATS → devices=${bluetoothDevices.size}, events/sec=$scanEventCount")
                    scanEventCount = 0
                    lastLogTime = now
                }

                val processingTime = SystemClock.elapsedRealtime() - startTime
                if (processingTime > 50) {
                    logger.info("SLOW_SCAN_PROCESS → ${processingTime}ms for ${device.address}")
                }

                if (bluetoothDevices.size == 1) {
                    hideScanLoaderWithMinimumDuration()
                }

                logger.info("DEVICE_ADDED → ${deviceName} (${device.address}) total=${bluetoothDevices.size}")

                updateUIWithDevice(device)

            } catch (e: SecurityException) {
                logger.debug("SecurityException in BLE scan: ${e.message}")
                handleMissingPermissions()
            }
        }
    }

    /**
     * Logs non-identifying BLE device metadata when [Manifest.permission.BLUETOOTH_CONNECT] is granted.
     *
     * @param device Discovered device.
     * @param name   Device name, if readable.
     * @param rssi   Signal strength from the scan callback.
     */
    private fun logDeviceDetails(device: BluetoothDevice, name: String?, rssi: Int) {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            logger.debug("BLE scan found device: $name (${device.address}) RSSI: $rssi")
            logger.debug("Device details - Name: $name, Type: ${device.type}, BondState: ${device.bondState}")
            logger.info("BLE scan result: Device='$name', Address='${device.address}', RSSI=$rssi, Type=${device.type}")
        } else {
            logger.debug("BLUETOOTH_CONNECT permission not granted. Cannot log detailed device info.")
        }
    }

    /**
     * Returns the configured prefix used to filter SECORA devices during classic Bluetooth discovery.
     */
    private fun bleDiscoveryDeviceNamePrefix(): String =
        BluetoothStateManager.bleDeviceNameDiscoveryFilterPattern(requireContext())

    /**
     * Determines if the given device name matches the configured BLE discovery prefix
     * ([BluetoothStateManager.bleDeviceNameDiscoveryFilterPattern], from `bleParameterConfig.properties`).
     *
     * @param name The name of the Bluetooth device.
     * @return True if the name is non-null and starts with that prefix, false otherwise.
     */
    private fun isSecoraDevice(name: String?): Boolean {
        val prefix = bleDiscoveryDeviceNamePrefix()
        return name != null && name.startsWith(prefix)
    }

    /** Wearable already connected via Fission SDK before wallet was opened. */
    private fun isHostPreconnectedDevice(device: BluetoothDevice): Boolean =
        PayExternalLaunch.isHostSelectedDeviceAddress(device.address)

    private fun addressesMatch(a: String?, b: String?): Boolean =
        PayExternalLaunch.addressesMatch(a, b)

    private fun resolveHostDeviceDisplayName(device: BluetoothDevice): String {
        return StorageRepository.readString(PreferenceKey.DEVICE_NAME).takeIf { it.isNotBlank() }
            ?: getSafeDeviceName(device)?.takeIf { it.isNotBlank() }
            ?: device.address
    }

    /**
     * Shows the Fission host's already-connected wearable in the add-device list without BLE scan.
     */
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    private fun presentHostPreconnectedDeviceIfNeeded() {
        if (!PayExternalLaunch.isHostLaunch()) return
        if (!hasBluetoothPermissions()) return
        if (!::bluetoothAdapter.isInitialized || !bluetoothAdapter.isEnabled) return

        val mac = StorageRepository.readString(PreferenceKey.SELECTED_DEVICE_ADDRESS).trim()
        if (mac.isEmpty() || !BluetoothAdapter.checkBluetoothAddress(mac)) return

        if (distinctDevices.isNotEmpty()) {
            restoreRegisteredDeviceListUi()
            logger.info("Host launch: showing registered device list (${distinctDevices.size} device(s))")
            return
        }

        // Device already paired locally — wait for getDeviceList instead of flashing scan UI.
        if (getPairedSeIds().isNotEmpty()) {
            logger.debug("Host launch: paired device exists locally, waiting for server device list")
            return
        }

        if (BluetoothStateManager.activeProtocol == null &&
            !BluetoothStateManager.isDeviceConnectedByAddress(mac)
        ) {
            logger.debug("Host preconnected: waiting for shared SECORA protocol for $mac")
            return
        }

        val device = try {
            bluetoothAdapter.getRemoteDevice(mac)
        } catch (e: IllegalArgumentException) {
            logger.debug("Host preconnected: invalid Bluetooth address $mac")
            return
        }

        if (isScanning) {
            try {
                stopBleScan()
            } catch (e: SecurityException) {
                logger.debug("SecurityException stopping scan for host device: ${e.message}")
            }
        }

        if (bluetoothDevices.none { addressesMatch(it.address, device.address) }) {
            bluetoothDevices.add(device)
        }
        BluetoothStateManager.addConnectedDevice(device.address)

        if (!hostPreconnectedUiPresented) {
            hostPreconnectedUiPresented = true
            binding.customBottom.root.visibility = View.GONE
            binding.tvNoDevice.visibility = View.GONE
            binding.tvCancel.visibility = View.GONE
            binding.swipeRefreshScan.visibility = View.VISIBLE
            binding.swipeRefreshDevices.visibility = View.GONE
            binding.rvScanDevices.visibility = View.VISIBLE
            binding.scanLoader.clProgressBar.visibility = View.GONE
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }

        if (!::deviceAdapter.isInitialized) {
            initializeDeviceAdapter()
        } else {
            publishSortedScanDeviceList()
        }
        clearScanEmptyStateMessage()
        logger.info(
            "Host preconnected device presented: ${resolveHostDeviceDisplayName(device)} (${device.address})"
        )
        activity.refreshHostExitButton()
        prefetchHostCplcSeTypeGroupIfNeeded()
    }

    /** Switches from add-device scan UI back to the server registered device list. */
    private fun restoreRegisteredDeviceListUi() {
        if (!::binding.isInitialized) return
        binding.swipeRefreshScan.visibility = View.GONE
        binding.rvScanDevices.visibility = View.GONE
        binding.swipeRefreshDevices.visibility = View.VISIBLE
        binding.rvDevices.visibility = View.VISIBLE
        binding.tvCancel.visibility = View.GONE
        binding.customBottom.root.visibility = View.VISIBLE
        binding.customBottom.llActive.visibility = View.VISIBLE
        binding.customBottom.llPassive.visibility = View.GONE
        hostPreconnectedUiPresented = false
        activity.refreshHostExitButton()
    }

    /** Host 启动时同步 Infineon demo 硬编码 OEM / SE_TYPE_GROUP 到 wallet SDK。 */
    private fun prefetchHostCplcSeTypeGroupIfNeeded() {
        if (!PayExternalLaunch.isHostLaunch()) return
        lifecycleScope.launch(AppDispatchers.IO) {
            WalletRepository.syncOemDetailsFromPreferences(requireContext().applicationContext)
            val seTypeGroup = ConfiguredWalletIdentity.readPersistedSeTypeGroup(requireContext())
            logger.info("Host OEM sync seTypeGroup=$seTypeGroup")
        }
    }

    private fun isBondedDevice(device: BluetoothDevice): Boolean =
        hasBluetoothPermissions() && device.bondState == BluetoothDevice.BOND_BONDED

    private fun isGattConnected(device: BluetoothDevice): Boolean {
        if (!hasBluetoothPermissions()) return false
        return try {
            val bluetoothManager =
                requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.getConnectionState(device, BluetoothProfile.GATT) ==
                BluetoothProfile.STATE_CONNECTED
        } catch (_: SecurityException) {
            false
        }
    }

    private fun isConnectedScanDevice(device: BluetoothDevice): Boolean =
        BluetoothStateManager.isDeviceConnectedByAddress(device.address) || isGattConnected(device)

    /**
     * Lower rank = higher priority: paired + connected + name filter first.
     */
    private fun scanDeviceSortRank(device: BluetoothDevice): Int {
        val name = getSafeDeviceName(device)
        val nameMatch = isSecoraDevice(name)
        val bonded = isBondedDevice(device)
        val connected = isConnectedScanDevice(device)
        return when {
            bonded && connected && nameMatch -> 0
            bonded && connected -> 1
            bonded && nameMatch -> 2
            connected && nameMatch -> 3
            nameMatch -> 4
            bonded -> 5
            else -> 6
        }
    }

    private fun sortedScanDevices(): List<BluetoothDevice> =
        bluetoothDevices.sortedWith(
            compareBy<BluetoothDevice>({ scanDeviceSortRank(it) }, { getSafeDeviceName(it).orEmpty() })
        )

    private fun publishSortedScanDeviceList() {
        if (!::deviceAdapter.isInitialized) {
            initializeDeviceAdapter()
            return
        }
        deviceAdapter.submitList(list = convertBluetoothDeviceToUiModel())
    }

    /**
     * Resolves a display name from BLE advertisement data first, then bonded cache.
     * After unbond, [BluetoothDevice.getName] is often null until the name appears in the scan record.
     */
    private fun resolveDeviceNameFromScan(result: ScanResult): String? {
        val fromAdvertisement = sanitizeBleDeviceName(result.scanRecord?.deviceName)
        if (fromAdvertisement != null) return fromAdvertisement
        return sanitizeBleDeviceName(getSafeDeviceName(result.device))
    }

    /** Strips non-alphanumeric characters from BLE names (garbled advertisement bytes after unbond). */
    private fun sanitizeBleDeviceName(name: String?): String? {
        if (name.isNullOrBlank()) return null
        val cleaned = name.trim()
            .filter { it.isLetterOrDigit() || it == ' ' || it == '_' || it == '-' }
            .trim()
        return cleaned.takeIf { it.isNotEmpty() }
    }

    /** True when the scan record advertises the configured SECORA GATT service UUID. */
    private fun advertisesSecoraService(result: ScanResult): Boolean {
        val serviceUuidStr = BluetoothStateManager.bleServiceUuid(requireContext()).trim()
        if (serviceUuidStr.isEmpty()) return false
        val targetUuid = runCatching { UUID.fromString(serviceUuidStr) }.getOrNull() ?: return false
        val advertised = result.scanRecord?.serviceUuids?.toList() ?: return false
        return advertised.any { it.uuid == targetUuid }
    }

    /** Accepts SECORA wearables by advertised name or GATT service UUID (needed post-unbond). */
    private fun isSecoraScanResult(result: ScanResult): Boolean {
        if (isSecoraDevice(resolveDeviceNameFromScan(result))) return true
        return advertisesSecoraService(result)
    }

    /**
     * Updates the UI to reflect the new SECORA device.
     *
     * @param device The SECORA Bluetooth device.
     */
    private fun updateUIWithDevice(device: BluetoothDevice) {
        if (!bluetoothAdapter.isEnabled) {
            return
        }
        onUiThread {
            val deviceName = if (ContextCompat.checkSelfPermission(
                    activity, Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                device.name ?: return@onUiThread
            } else {
                return@onUiThread
            }

            deviceName.hashCode()
            clearScanEmptyStateMessage()

            if (!::deviceAdapter.isInitialized) {
                initializeDeviceAdapter()
            } else {
                scheduleAdapterUpdate()
            }
        }
    }

    /**
     * Initializes the Bluetooth device adapter and sets it to the RecyclerView.
     * Sets up the click listener to handle device selection and disconnection logic.
     */
    private fun initializeDeviceAdapter() {
        logger.debug("Initializing deviceAdapter for scan results")
        deviceAdapter = BluetoothScanAdapter { selectedDevice ->
            val bleDevice = bluetoothDevices.find { device ->
                device.address == selectedDevice.address
            } ?: return@BluetoothScanAdapter

            logger.debug("Selected device: ${getSafeDeviceName(bleDevice)}")
            handleDeviceSelection(bleDevice)
        }

        binding.rvScanDevices.adapter = deviceAdapter
        deviceAdapter.submitList(list = convertBluetoothDeviceToUiModel())
        binding.swipeRefreshScan.visibility = View.VISIBLE
        binding.tvNoDevice.visibility = View.GONE
        logger.debug("Device adapter initialized and set to RecyclerView")
    }

    private fun convertBluetoothDeviceToUiModel(): List<BluetoothDeviceUiModel> {
        return sortedScanDevices()
            .mapNotNull { device ->
                if (!bluetoothAdapter.isEnabled) return@mapNotNull null
                val name = when {
                    isHostPreconnectedDevice(device) -> resolveHostDeviceDisplayName(device)
                    else -> getSafeDeviceName(device)?.takeIf { isSecoraDevice(it) }
                } ?: return@mapNotNull null
                BluetoothDeviceUiModel(name = name, address = device.address)
            }
    }

    /**
     * Shows a scan empty-state message in the RecyclerView empty row.
     *
     * @param message Text to display when no scan devices are available.
     */
    private fun showScanEmptyStateMessage(message: String) {
        if (!::deviceAdapter.isInitialized) {
            initializeDeviceAdapter()
        }
        deviceAdapter.setEmptyStateMessage(message)
    }

    /**
     * Clears any custom scan empty-state message and falls back to default text.
     */
    private fun clearScanEmptyStateMessage() {
        if (::deviceAdapter.isInitialized) {
            deviceAdapter.setEmptyStateMessage(null)
        }
    }

    /**
     * Handles logic when a SECORA device is selected.
     *
     * @param selectedDevice The selected Bluetooth device.
     */
    private fun handleDeviceSelection(selectedDevice: BluetoothDevice) {

        dismissFragmentScanOverlay()
        if (ContextCompat.checkSelfPermission(
                activity, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            logger.debug("BLUETOOTH_CONNECT permission not granted. Cannot handle device selection safely.")
            return
        }

        // Allow multiple simultaneous BLE connections: do not disconnect first device when user selects second.
        val selectedDeviceName = sanitizeBleDeviceName(selectedDevice.name) ?: UNKNOWN_DEVICE
        logger.debug("Showing connection dialog for device: $selectedDeviceName")
        customSuccessDialogScanResult(
            selectedDeviceName, selectedDeviceScanResult = selectedDevice
        )
    }

    /**
     * Launcher to handle the result of a Bluetooth enable request.
     */
    private var enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (hasBluetoothPermissions()) {

                    try {
                        proceedWithScan()
                    } catch (e: SecurityException) {
                        logger.debug("SecurityException in enableBluetoothLauncher: ${e.message}")
                        showToast(BLUETOOTH_PERMISSION)
                        requestBluetoothPermissions()
                        binding.customBottom.llActive.visibility = View.VISIBLE
                        binding.customBottom.llPassive.visibility = View.GONE
                        binding.tvCancel.visibility = View.GONE
                        binding.tvNoDevice.visibility = View.VISIBLE
                        activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
                        showLoading(false)
                    }
                } else {
                    logger.debug("Bluetooth permissions not granted")
                    showToast(BLUETOOTH_PERMISSION)
                    requestBluetoothPermissions()
                    showLoading(false)
                }
            } else {
                showToast(resources.getString(R.string.bluetooth_is_required_to_scan_devices))
                showLoading(false)
            }
        }
    private var distinctDevices: List<PaymentDeviceList> = ArrayList()
    private var isLoading: Boolean = false
    private var userName: String = ""
    private var profileImage: String = ""
    private var email: String = ""
    private var bleProtocol: BleProtocol? = null

    /** Multiple BLE connections: address (normalized) -> protocol. Keeps first device connected when connecting second. */
    private val connectedBleProtocols = mutableMapOf<String, BleProtocol>()
    private var retryCount = 3
    private var suppressDeleteScriptLoaderHides: Boolean = false

    /**
     * Normalizes a BLE MAC for comparison (trim, uppercase, non-blank).
     *
     * @param addr Raw address from scan or preferences.
     */
    private fun normalizeAddress(addr: String?) = addr?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }

    /** True after the Fission host pre-connected wearable is shown without BLE scan. */
    private var hostPreconnectedUiPresented = false

    private var selectedDeviceName: String? = null

    /**
     * Job for collecting events from EventBus.
     */
    private var eventCollectorJob: Job? = null
    private var resetWhenModelIsNotPresent: Boolean = false

    /**
     * After FCM device detach: drop this device from linked filters and refresh scan so only the
     * detached unit can reappear (other paired devices stay filtered if still linked on server).
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleDeviceDetachCompleted(event: AppEvent) {
        val detachedAddress = event.getStringExtra(BundleKey.DEVICE_BLE_ADDRESS).orEmpty()
        if (detachedAddress.isNotEmpty()) {
            linkedDeviceAddresses.removeIf {
                normalizeAddress(it) == normalizeAddress(detachedAddress)
            }
            logger.debug(
                "Device detach completed: removed $detachedAddress from linkedDeviceAddresses"
            )
        }
        distinctDevices = emptyList()
        binding.rvDevices.adapter = null
        refreshDeviceList()
        getDeviceList(requireContext(), showLoader = false)
    }

    /**
     * Handles refresh events received from EventBus.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun handleRefreshEvent(event: AppEvent) {
        try {
            if (isAdded) {
                when (event.action) {
                    ACTION_LISTENER -> {
                        if (FcmSecureFlowCoordinator.isFlowInProgress()) {
                            logger.debug(
                                "AvailableDeviceFragment: skip ACTION_LISTENER clear during FCM detach/delete flow"
                            )
                            return
                        }

                        if (::activity.isInitialized && !activity.isFinishing) {
                            StorageRepository.clearAllLocalCardData(activity)
                        }
                    }

                    ACTION_DEVICE_DETACH_COMPLETED -> {
                        handleDeviceDetachCompleted(event)
                    }

                    DEFAULT_CARD_CHANGE -> {
                        logger.debug("Default card changed - refreshing device list")
                        refreshDeviceList()
                    }
                }
            } else {
                logger.debug("AvailableDeviceFragment: Fragment not attached when event received")
            }
        } catch (e: Exception) {
            logger.debug("AvailableDeviceFragment: Error handling event: $e")
        }
    }

    /**
     * to handle fcm event if device is suspended or activated show dialog
     */
    fun handleSuspendActivationNotificationEvent(event: AppEvent) {
        val deviceName = event.getStringExtra(BundleKey.DEVICE_NAME)
        val msgType = event.getStringExtra(BundleKey.MSG_TYPE)
        if (msgType == DEVICE_SUSPEND_UPDATE) logger.debug("deviceStatus :: noti suspend") else logger.debug("deviceStatus :: noti Activated")
        val displayMsg = if (msgType == DEVICE_SUSPEND_UPDATE) getString(
            R.string.suspended_message,
            deviceName
        ) else getString(R.string.activated_message, deviceName)
        statusDialog(activity, displayMsg, {
            getDeviceList(activity)
        })
    }

    /**
     * Receiver to handle BLE connection state changes
     */
    private val connectionStateReceiver = object : BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val extras = intent.extras ?: return
                    val device: BluetoothDevice? =
                        BundleCompat.getParcelable(
                            extras,
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    device?.let {
                        if (PayExternalLaunch.isHostSelectedDeviceAddress(it.address)) {
                            logger.debug(
                                "Host launch: ACL reconnected for ${resolveHostDeviceDisplayName(it)}"
                            )
                            BluetoothStateManager.addConnectedDevice(it.address)
                            handler.postDelayed({
                                if (isAdded && hasBluetoothPermissions()) {
                                    presentHostPreconnectedDeviceIfNeeded()
                                }
                            }, 500)
                        }
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val extras = intent.extras ?: return

                    val device: BluetoothDevice? =
                        BundleCompat.getParcelable(
                            extras,
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    device?.let {
                        if (PayExternalLaunch.shouldIgnoreHostAclDisconnect(it.address)) {
                            logger.debug(
                                "Host launch: ignoring transient ACL disconnect for ${resolveHostDeviceDisplayName(it)}"
                            )
                            handler.postDelayed({
                                if (isAdded && hasBluetoothPermissions()) {
                                    presentHostPreconnectedDeviceIfNeeded()
                                }
                            }, 800)
                            return@let
                        }
                        if (hasBluetoothPermissions()) {
                            val deviceName = getSafeDeviceName(it)
                            logger.debug("deviceName which disconnected: $deviceName")
                            disconnectAndRemoveDevice(it.address)
                        } else {
                            logger.debug("BLUETOOTH_CONNECT permission not granted for disconnection handling")
                            showToast(BLUETOOTH_PERMISSION)
                            requestBluetoothPermissions()
                        }
                    }
                }

                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_OFF -> {
                            logger.debug("Bluetooth turned off, clearing device list")
                            stopBleScan()
                            bluetoothDevices.clear()
                            connectedBleProtocols.values.forEach {
                                disconnectProtocolAndRemoveFromState(it)
                            }
                            connectedBleProtocols.clear()
                            bleProtocol = null
                            BluetoothStateManager.clearAllConnectedDevices()
                            if (::deviceAdapter.isInitialized) {
                                deviceAdapter.submitList(emptyList())
                                showScanEmptyStateMessage(getString(R.string.no_secora_device_found))
                            }
                            // Refresh paired list so BLE icons turn black (BT is off).
                            binding.rvDevices.adapter?.notifyDataSetChanged()
                        }

                        BluetoothAdapter.STATE_ON -> {
                            logger.debug("Bluetooth turned on, refreshing device list")
                            refreshDeviceList()
                        }
                    }
                }
            }
        }
    }

    /**
     * BroadcastReceiver to handle discovered classic Bluetooth devices.
     * Filters for SECORA devices and updates the UI accordingly.
     */
    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    handleBluetoothDeviceFound(intent)
                } else {
                    logger.debug("No BLUETOOTH_CONNECT permission when reading name")
                }
            }
        }
    }

    /**
     * Handles logic for when a Bluetooth device is found during classic scan.
     *
     * @param intent The broadcast intent containing Bluetooth device data.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleBluetoothDeviceFound(intent: Intent) {
        if (!hasBluetoothPermissions()) {
            handleMissingPermissions()
            return
        }

        val device = extractBluetoothDevice(intent) ?: return

        if (!hasBluetoothPermissions()) {
            handleMissingPermissions()
            return
        }

        val deviceName = device.name
        if (deviceName != null && deviceName.startsWith(bleDiscoveryDeviceNamePrefix())) {
            handleSecoraDeviceFound(device)
        } else {
            logger.info("Classic Bluetooth discovery found device: $deviceName (${device.address})")
        }
    }

    /**
     * Extracts the BluetoothDevice from the Intent in a backward-compatible way.
     *
     * @param intent The intent from ACTION_FOUND broadcast.
     * @return The extracted BluetoothDevice, or null if not found.
     */
    private fun extractBluetoothDevice(intent: Intent): BluetoothDevice? {
        val extras = intent.extras ?: return null

        return BundleCompat.getParcelable(
            extras,
            BluetoothDevice.EXTRA_DEVICE,
            BluetoothDevice::class.java
        )
    }

    /**
     * Handles SECORA device detection, filtering, locking, and updating UI.
     *
     * @param device The Bluetooth device detected.
     */
    private fun handleSecoraDeviceFound(device: BluetoothDevice) {
        logger.info("Classic Bluetooth discovery found SECORA device: ${device.name} (${device.address})")

        bleLock.lock()
        if (ActivityCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TO DO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }

        // Do not show already-linked devices in the scan list.
        if (linkedDeviceAddresses.any { normalizeAddress(it) == normalizeAddress(device.address) }) {
            logger.debug("Skipping linked device in classic discovery: ${device.address}")
            return
        }

        selectedDeviceName = device.name
        try {
            if (bluetoothDevices.none { it.address == device.address }) {
                bluetoothDevices.add(device)
                if (bluetoothDevices.size == 1) {
                    hideScanLoaderWithMinimumDuration()
                }
                logger.info("Added SECORA device from classic discovery: ${device.name} (Total: ${bluetoothDevices.size})")
                updateClassicBluetoothUI()
            } else {
                logger.debug("Device ${device.name} already in scan list, skipping")
            }
        } finally {
            bleLock.unlock()
        }
    }

    /**
     * Updates the UI after detecting a SECORA device via classic Bluetooth discovery.
     *
     * Clears the empty-state message, refreshes [deviceAdapter] with [bluetoothDevices],
     * and shows the scan list while hiding the no-device placeholder.
     */
    private fun updateClassicBluetoothUI() {
        requireActivity().runOnUiThread {
            clearScanEmptyStateMessage()
            if (::deviceAdapter.isInitialized) {
                deviceAdapter.submitList(list = convertBluetoothDeviceToUiModel())
                logger.debug("Updated adapter after classic discovery")
            } else {
                initializeDeviceAdapter()
            }

            binding.swipeRefreshScan.visibility = View.VISIBLE
            binding.tvNoDevice.visibility = View.GONE
        }
    }

    /**
     * onCreateView method is used to inflate the layout for this fragment
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     *                 any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment’s
     *                  UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state.
     * @return The root View for the fragment's UI, or null if no UI is needed.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletAvailableDeviceBinding.inflate(inflater, container, false)
        activity = requireActivity() as MainActivity
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        binding.tvCancel.visibility = View.GONE

        // Start collecting events from EventBus
        eventCollectorJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                EventBus.events.collect { event ->
                    if ((event.action == ACTION_LISTENER ||
                            event.action == DEFAULT_CARD_CHANGE ||
                            event.action == ACTION_DEVICE_DETACH_COMPLETED) &&
                        ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        handleRefreshEvent(event)
                    } else if (event.action == ACTION_DEVICE_STATUS_UPDATE) {
                        handleSuspendActivationNotificationEvent(event)
                    }
                }
            }
        }
        return binding.root
    }

    /**
     * Called when the fragment view is created.
     * Sets up UI elements, user profile icon, Bluetooth launcher, and back-press behavior.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        restorePairedDevices()
        setupProfileIcon()
        initView()
        setupBackPressedHandler()
        registerBluetoothLauncher()
        statusMonitor.attach(viewLifecycleOwner, requireContext())
        registerHostConnectionListener()
        if (hasBluetoothPermissions()) {
            presentHostPreconnectedDeviceIfNeeded()
        }
    }

    /** Re-presents the Fission host wearable when the shared SECORA link becomes ready. */
    private fun registerHostConnectionListener() {
        if (!PayExternalLaunch.isHostLaunch()) return
        val listener = Runnable {
            if (!isAdded || !::binding.isInitialized || !hasBluetoothPermissions()) return@Runnable
            presentHostPreconnectedDeviceIfNeeded()
        }
        BluetoothStateManager.addOnConnectionStateChanged(listener)
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                BluetoothStateManager.removeOnConnectionStateChanged(listener)
            }
        })
    }

    /** Hand state last pushed to [WearableHandState], to avoid needless adapter refreshes. */
    private var lastHand: WearableHandState.Hand = WearableHandState.Hand.HIDDEN

    /** Shared poller producing the wearable status that drives the per-row hand icon. */
    private val statusMonitor = WearableStatusMonitor(onUpdate = ::onWearableStatus)

    /**
     * Maps each polled [status] to the connected wearable's hand (off-body / on-body / verified,
     * hidden when unreachable) and refreshes the list only when it changes.
     */
    private fun onWearableStatus(status: WearableStatus) {
        val hand = when {
            status.unreachable -> WearableHandState.Hand.HIDDEN
            status.presence == BodyPresenceTracker.Presence.ON_BODY && status.verified ->
                WearableHandState.Hand.VERIFIED
            status.presence == BodyPresenceTracker.Presence.ON_BODY -> WearableHandState.Hand.ON_BODY
            status.presence == BodyPresenceTracker.Presence.OFF_BODY -> WearableHandState.Hand.OFF_BODY
            else -> WearableHandState.Hand.HIDDEN
        }
        publishHand(if (status.unreachable) null else CdcvmApi.activeDeviceId(), hand)
    }

    /** Stores the resolved hand and refreshes the list only when it changed. */
    private fun publishHand(seId: String?, hand: WearableHandState.Hand) {
        if (seId == null) {
            WearableHandState.clear()
        } else {
            WearableHandState.update(seId, hand)
        }
        if (hand != lastHand) {
            lastHand = hand
            if (isAdded && ::binding.isInitialized) {
                binding.rvDevices.adapter?.notifyDataSetChanged()
            }
        }
    }

    /**
     * Sets the visibility and image of the profile icon in the toolbar
     * based on stored user data such as name, email, and profile image URL.
     */
    private fun setupProfileIcon() {
        activity.binding.toolbar.profileIcon.visibility = View.VISIBLE
        userName = StorageRepository.readString(PreferenceKey.USER_NAME)
        profileImage = StorageRepository.readString(PreferenceKey.PROFILE_IMAGE)
        email = StorageRepository.readString(PreferenceKey.EMAIL_ID)

        when {
            !isEmptyString(email) && isEmptyString(userName) && isEmptyString(profileImage) -> {
                activity.binding.toolbar.profileIcon.setImageBitmap(
                    activity.createInitialsDrawable(
                        email
                    )
                )
            }

            !isEmptyString(userName) && isEmptyString(profileImage) -> {
                activity.binding.toolbar.profileIcon.setImageBitmap(
                    activity.createInitialsDrawable(
                        userName
                    )
                )
            }

            !isEmptyString(profileImage) -> {
                loadProfileImage(profileImage)
            }

            else -> {
                activity.binding.toolbar.profileIcon.setImageResource(R.drawable.ic_user_avatar)
            }
        }
    }

    /**
     * Loads a circular profile image from a URL using Glide.
     *
     * @param imageUrl The URL of the profile image.
     */
    private fun loadProfileImage(imageUrl: String) {
        try {
            Glide.with(this).load(imageUrl).circleCrop().into(activity.binding.toolbar.profileIcon)
        } catch (e: IllegalArgumentException) {
            logger.debug("Error loading profile image: ${e.message}")
        }
    }

    /**
     * Registers a back press callback to handle custom behavior when back is pressed.
     * If loading is not active, navigates back using [backExit].
     */
    private fun setupBackPressedHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (PayExternalLaunch.isHostLaunch() && !activity.isWalletTabVisible()) {
                        PayExternalLaunch.exitToHost(activity)
                        return
                    }
                    binding.customBottom.root.visibility = View.VISIBLE
                    binding.customBottom.llActive.visibility = View.VISIBLE
                    binding.customBottom.llPassive.visibility = View.GONE
                    // Stop scanning if currently scanning
                    activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    StorageRepository.saveBoolean(PreferenceKey.BACK_PRESSED_FLAG, false)

                    if (isScanning) {
                        if (ContextCompat.checkSelfPermission(
                                activity,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            stopWearableScanCompletely()
                        } else {
                            logger.debug("No BLUETOOTH_CONNECT permission when scanning a device")
                            isScanning = false
                            dismissFragmentScanOverlay()
                            resetScanningUI()
                            return
                        }
                        resetScanningUI()
                        return
                    }
                    if (resetWhenModelIsNotPresent) {
                        resetWhenModelIsNotPresent = false
                        resetScanningUI()
                    }

                    if (!isLoading) backExit()
                }
            })
    }

    /**
     * Stops the ongoing BLE scan and resets all related scan state.
     *
     * This method clears the discovered Bluetooth devices list, updates the
     * adapter with an empty snapshot, resets the scanning UI, and marks
     * the scanning flag as inactive.
     *
     * Requires BLUETOOTH_SCAN permission to safely stop scanning operations.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun stopWearableScanCompletely() {
        dismissFragmentScanOverlay()
        stopBleScan()
        bluetoothDevices.clear()
        if (::deviceAdapter.isInitialized) {
            deviceAdapter.submitList(list = convertBluetoothDeviceToUiModel())
            resetScanningUI()
        }
        isScanning = false
    }


    /**
     * Registers an activity result launcher to handle enabling Bluetooth.
     * Proceeds with BLE scan if successful, otherwise shows error.
     */
    private fun registerBluetoothLauncher() {
        enableBluetoothLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    binding.customBottom.llActive.visibility = View.GONE
                    binding.customBottom.llPassive.visibility = View.GONE
                    binding.tvCancel.visibility = View.VISIBLE
                    activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    activity.refreshHostExitButton()
                    try {
                        proceedWithScan()
                    } catch (e: SecurityException) {
                        logger.debug("SecurityException in enableBluetoothLauncher: ${e.message}")
                        showToast(BLUETOOTH_PERMISSION)
                        requestBluetoothPermissions()
                        binding.customBottom.llActive.visibility = View.VISIBLE
                        binding.customBottom.llPassive.visibility = View.GONE
                        binding.tvCancel.visibility = View.GONE
                        binding.tvNoDevice.visibility = View.VISIBLE
                        activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
                        showLoading(false)
                    }
                } else {
                    showToast(resources.getString(R.string.bluetooth_is_required_to_scan_devices))
                    binding.customBottom.root.visibility = View.VISIBLE
                    binding.customBottom.llActive.visibility = View.VISIBLE
                    binding.customBottom.llPassive.visibility = View.GONE
                    binding.tvCancel.visibility = View.GONE
                    binding.tvNoDevice.visibility = View.VISIBLE
                    activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    showLoading(false)
                }
            }
    }

    /**
     * Initializes views and sets up listeners
     */
    private fun initView() {
        checkPermissions()
        initBluetooth()
        setupSwipeRefresh()
        displayDeviceStatusIfRequired({
            CoroutineScope(Dispatchers.IO).launch {
                getDeviceList(requireContext())
            }
        })

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        ContextCompat.registerReceiver(
            requireContext(), receiver, filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        // Register connection state receiver
        val connectionFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            requireContext(), connectionStateReceiver,
            connectionFilter, ContextCompat.RECEIVER_EXPORTED
        )

        binding.customBottom.btnScan.setOnClickListener {
          addBLuetoothDevice()
        }

        binding.customBottom.llActive.setOnClickListener {
            addBLuetoothDevice()
        }

        binding.tvCancel.setOnClickListener {
            binding.customBottom.root.visibility = View.VISIBLE
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                stopBleScan()
            } else {
                logger.debug("BLUETOOTH_CONNECT permission not granted. Cannot log detailed device info.")
            }

            resetScanningUI()
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            logger.debug("Scanning cancelled by user")
            StorageRepository.saveBoolean(PreferenceKey.BACK_PRESSED_FLAG, false)
            activity.refreshHostExitButton()
        }

        binding.customBottom.btnPassive.setOnClickListener {
            ensureNfcReadyThenRun {
                showLoading(binding.loadingIcon.customProgressBar, true)
                showNfcSheet(parentFragmentManager)
                NfcScriptExecutionTracker.onNfcScriptStarted()
                SecoraWearableSDK.getInstance().getInterface().executeNfcOperation(
                    requireActivity(),
                    CPLC,
                    null,
                    null,
                    null,
                    this
                )
            }
        }
    }

    private fun addBLuetoothDevice() {
        val bluetoothManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(
                requireContext(), R.string.bluetooth_not_supported, Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!hasBluetoothPermissions()) {
            logger.debug("Add Bluetooth: Bluetooth permissions not granted")
            handleMissingPermissions()
            return
        }

        if (PayExternalLaunch.isHostLaunch() &&
            StorageRepository.readString(PreferenceKey.SELECTED_DEVICE_ADDRESS).isNotBlank() &&
            BluetoothStateManager.activeProtocol != null
        ) {
            presentHostPreconnectedDeviceIfNeeded()
            return
        }

        binding.customBottom.root.visibility = View.GONE
        binding.tvNoDevice.visibility = View.GONE
        binding.tvCancel.visibility = View.GONE
        binding.customBottom.llActive.visibility = View.GONE
        binding.customBottom.llPassive.visibility = View.GONE
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        StorageRepository.saveBoolean(PreferenceKey.BACK_PRESSED_FLAG, true)

        try {
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
            } else {
                proceedWithScan()
            }
        } catch (e: SecurityException) {
            logger.debug("SecurityException starting Bluetooth enable/scan: ${e.message}")
            handleMissingPermissions()
        }
    }

    /**
     * Sets up SwipeRefreshLayout for pull-to-refresh scan functionality.
     * When user pulls down on the scan device list, BLE scan restarts.
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefreshScan.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorAccent,
            R.color.tealGreen
        )

        binding.swipeRefreshScan.setOnChildScrollUpCallback { _, _ ->
            binding.rvScanDevices.visibility == View.VISIBLE &&
                binding.rvScanDevices.canScrollVertically(-1)
        }
        binding.swipeRefreshDevices.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorAccent,
            R.color.tealGreen
        )
        binding.swipeRefreshScan.setOnRefreshListener {
            if (PayExternalLaunch.isHostLaunch() && distinctDevices.isNotEmpty()) {
                logger.debug("Pull-to-refresh: host launch with registered devices → refreshing device list")
                restoreRegisteredDeviceListUi()
                lifecycleScope.launch(AppDispatchers.IO) { getDeviceList(activity) }
                binding.swipeRefreshScan.isRefreshing = false
                return@setOnRefreshListener
            }
            val isScanScreen = binding.swipeRefreshScan.visibility == View.VISIBLE &&
                binding.swipeRefreshDevices.visibility != View.VISIBLE
            if (isScanScreen) {
                logger.debug("Pull-to-refresh: scan screen → restarting BLE scan")
                refreshScanDevices()
            } else {
                logger.debug("Pull-to-refresh: device list screen (from scan swipe) → refreshing payment device list")
                lifecycleScope.launch(AppDispatchers.IO) { getDeviceList(activity) }
            }
        }

        binding.swipeRefreshDevices.setOnRefreshListener {
            logger.debug("Pull-to-refresh: device list screen → refreshing payment device list")
            lifecycleScope.launch(AppDispatchers.IO) { getDeviceList(activity) }
        }
    }

    /**
     * Clears the current scanned device list and restarts BLE scanning.
     * Called when user triggers pull-to-refresh.
     */
    private fun refreshScanDevices() {
        binding.swipeRefreshScan.isEnabled = true
        // Stop any ongoing scan first
        if (isScanning && hasBluetoothPermissions()) {
            try {
                stopBleScan()
            } catch (e: SecurityException) {
                logger.debug("SecurityException stopping scan: ${e.message}")
            }
        }

        // Clear existing scanned devices
        bluetoothDevices.clear()
        if (::deviceAdapter.isInitialized) {
            deviceAdapter.submitList(list = convertBluetoothDeviceToUiModel())
        }
        clearScanEmptyStateMessage()

        // Start fresh scan
        if (hasBluetoothPermissions()) {
            startBleScan()
        }
        binding.swipeRefreshScan.isRefreshing = false
    }

    /**
     * Stops pull-to-refresh on both the device list and scan list swipe containers.
     */
    private fun stopAllSwipeRefreshing() {
        binding.swipeRefreshScan.isRefreshing = false
        binding.swipeRefreshDevices.isRefreshing = false
    }

    /**
     * Registers a payment device via SDK and handles various server responses.
     *
     * Whether the device is a dummy device.
     * @param device Name of the device to register.
     * @param seId Secure Element ID of the device.
     * Whether this device was previously registered.
     */
    private fun registerDeviceApiCall(
        device: String, seId: String,
        wearableModelIdHex: String = "",
        delinkFromDifferentUser: Boolean = false
    ) {
        val isNfcRegistrationAttempt = device.startsWith(Constants.NFC_DEVICE_MODEL, ignoreCase = true)
        if (!isNetworkAvailable(requireContext())) {
            showLoading(false)
            confirmDataDialog(getString(R.string.data_enable))
            return
        }

        lifecycleScope.launch {
            showRegistrationProgress(true)
            val sdkResult =
                WalletRepository.registerPaymentDevice(
                    context = activity,
                    device = device,
                    seId = seId,
                    wearableModelIdHex = wearableModelIdHex
                )
            if (sdkResult.isSuccess) {
                handleRegisterResponse(sdkResult.statusMessage, seId, device, delinkFromDifferentUser)
                return@launch
            }

            // Error case scenario: force clear blur/loader so dialog OK cannot leave a blocked overlay.
            forceHideLoading()
            handleSessionOrError(sdkResult.errorMessage) {
                if (!isNfcRegistrationAttempt && !isNFC()) {
                    safelyDisconnectBLE(seId)
                    clearGhostPairing(seId)
                    handleScanScreen()
                }
                statusDialog(activity, sdkResult.errorMessage)
            }
        }
    }

    /**
     * Handles SDK register response based on statusMessage value.
     */
    private suspend fun handleRegisterResponse(
        statusMessage: String, seId: String, device: String,
        delinkFromDifferentUser: Boolean = false
    ) {
        when (statusMessage) {
            CommonResponse.SUCCESS.response -> {
                handleSuccessState(seId, device, delinkFromDifferentUser)
            }
            else -> handleOtherErrors(statusMessage, seId, device)
        }
    }

    /**
     * Completes the device registration success path: persists the device name, stops scanning, refreshes UI, and loads pending tasks.
     *
     * @param seId The Secure Element ID for the registered device.
     * @param device Display name of the wearable to store in preferences.
     * @param delinkFromDifferentUser When true, skips the immediate pending-task fetch for delink flows.
     */
    private suspend fun handleSuccessState(
        seId: String,
        device: String,
        delinkFromDifferentUser: Boolean = false
    ) {
        StorageRepository.saveString(PreferenceKey.DEVICE_NAME, device)

        // Register flow starts with showLoading(true); clear it before pending-task delete scripts.
        // This path should run without any loader on UI.
        forceHideLoading()
        dismissFragmentScanOverlay()
        // Stop scanning on successful device registration
        if (isScanning && hasBluetoothPermissions()) {
            try {
                stopBleScan()
                logger.debug("Scan stopped - device registered successfully")
            } catch (e: SecurityException) {
                logger.debug("SecurityException stopping scan: ${e.message}")
            }
        }

        activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        clearDevice()
        if (!delinkFromDifferentUser) {
            getPendingTask(seId, "", fromRegisterPaymentSuccess = true)
        }
        // Refresh device list in the background without blocking the UI.
        getDeviceList(requireContext(), showLoader = false)

        binding.apply {
            tvTitle.text = getString(R.string.wearables)
            tvCancel.visibility = View.GONE
            swipeRefreshScan.visibility = View.GONE
            customBottom.root.visibility = View.VISIBLE
            swipeRefreshDevices.visibility = View.VISIBLE
            customBottom.llActive.visibility = View.VISIBLE
            customBottom.llPassive.visibility = View.GONE
        }

        if (::activity.isInitialized && !activity.isFinishing) {
            StorageRepository.clearAllLocalCardData(activity)
        }
    }

    /**
     * Handles non-success and non-token-expired server responses.
     */
    private fun handleOtherErrors(
        statusMessage: String?, seId: String, device: String
    ) {
        val isNfcRegistrationAttempt = device.startsWith(Constants.NFC_DEVICE_MODEL, ignoreCase = true)
        showLoading(true)

        if (statusMessage?.startsWith(Constants.DEVICE_LINKED_WITH_ANOTHER_USER_MESSAGE, true) == true) {
            val paymentAppInstanceId = extractPaymentAppInstanceId(statusMessage)

            showDeLinkDialog(
                message = getString(R.string.text_delink_confirm_message),
                paymentAppInstanceId = paymentAppInstanceId,
                seId = seId,
                selectedDevice = device,
                connected = isNFC() || BluetoothStateManager.isConnected
            )
            return
        }

        if (!isNfcRegistrationAttempt && !isNFC()) {
            bleProtocol?.let { protocol ->
                SecoraWearableSDK.getInstance().getInterface().disconnectBLEDevice(protocol)
            }

            clearGhostPairing(seId)
            handleScanScreen()
            resetWhenModelIsNotPresent = true
        }
        // In some sequential flows, hide calls can be suppressed; force clear to avoid stuck blur UI.
        forceHideLoading()
        statusDialog(requireActivity(), statusMessage)
    }

    /**
     * Extracts paymentAppInstanceId from a server error message, if available.
     */
    private fun extractPaymentAppInstanceId(message: String?): String {
        val keyword = "paymentAppInstanceId = "
        val startIndex = message?.indexOf(keyword) ?: -1
        return if (startIndex != -1) {
            message?.substring(startIndex + keyword.length)?.removeSuffix(")") ?: ""
        } else {
            ""
        }
    }

    /**
     * Checks required permissions
     */
    private fun checkPermissions() {
        if (!hasBluetoothPermissions() &&
            !StorageRepository.readBoolean(PreferenceKey.HAS_REQUESTED_NEARBY_DEVICES_PERMISSION)
        ) {
            requestBluetoothPermissions()
        }
    }

    /**
     * Initializes Bluetooth
     */
    private fun initBluetooth() {
        val bluetoothManager = requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (!requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(requireContext(), R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Safely retrieves the device name with permission checks
     */
    private fun getSafeDeviceName(device: BluetoothDevice): String? {
        return if (hasBluetoothPermissions()) {
            try {
                device.name
            } catch (e: SecurityException) {
                logger.debug("SecurityException accessing device name: ${e.message}")
                null
            }
        } else {
            logger.debug("BLUETOOTH_CONNECT permission not granted for device name access")
            null
        }
    }

    /**
     * Starts Bluetooth discovery
     */
    private fun startDiscovery() {
        if (!hasBluetoothPermissions()) {
            logger.debug("Bluetooth scan permission not granted")
            showToast(BLUETOOTH_SCAN_PERMISSION_REQUIRED)
            requestBluetoothPermissions()
            showLoading(false)
            return
        }

        try {
            if (hasBluetoothPermissions() && bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            activity.showLoading(false, "")
            isLoading = false
            activity.showScannerLoading(false)
            binding.swipeRefreshScan.visibility = View.VISIBLE
            binding.tvNoDevice.visibility = View.GONE
            startBleScan()
        } catch (e: SecurityException) {
            logger.debug("SecurityException in startDiscovery: ${e.message}")
            showToast(BLUETOOTH_PERMISSION)
            requestBluetoothPermissions()
            showLoading(false)
        }
    }

    /**
     * Starts BLE scan for devices
     */
    private fun startBleScan() {
        if (!hasBluetoothPermissions()) {
            logger.debug("Bluetooth scan permission not granted")
            showToast(BLUETOOTH_SCAN_PERMISSION_REQUIRED)
            requestBluetoothPermissions()
            showLoading(false)
            return
        }

        try {
            // Stop any existing scan
            stopBleScan()
            beginScanSectionLoading()

            // Initialize BLE scanner
            bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            if (bluetoothLeScanner == null) {
                logger.debug("BLE scanner not available")
                showToast(getString(R.string.text_ble_scanner_not_available))
                showLoading(false)
                return
            }

            // Clear existing devices
            bluetoothDevices.clear()

            // Check bonded devices first to include already paired SECORA devices
            try {
                if (hasBluetoothPermissions()) {
                    checkBondedDevices()
                    logger.info("Checked bonded devices, found ${bluetoothDevices.size} devices")
                    if (bluetoothDevices.isNotEmpty()) {
                        publishSortedScanDeviceList()
                        clearScanEmptyStateMessage()
                        hideScanLoaderWithMinimumDuration()
                    }
                }
            } catch (e: SecurityException) {
                logger.debug("SecurityException checking bonded devices: ${e.message}")
            }

            // Start BLE scan with aggressive settings to find more devices
            val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT).build()
            val scanFilters = mutableListOf<ScanFilter>()

            logger.info("Starting BLE scan with aggressive settings to find all devices")
            bluetoothLeScanner?.startScan(scanFilters, scanSettings, bleScanCallback)
            isScanning = true
            startClassicDiscoveryIfPermitted()
            logger.info("BLE scan started successfully")

            // Set scan timeout
            scanRunnable = Runnable {
                stopBleScan()
                logger.info("BLE scan timeout reached")
                logger.info("Devices found during BLE scan: ${bluetoothDevices.size}")
                bluetoothDevices.forEach { device ->
                    logger.info("Found device: ${getSafeDeviceName(device)} (${device.address})")
                }
                if (bluetoothDevices.isEmpty()) {
                    showNoDeviceFoundAfterTimeout()
                } else {
                    if (!::deviceAdapter.isInitialized) {
                        initializeDeviceAdapter()
                    } else {
                        deviceAdapter.submitList(list = convertBluetoothDeviceToUiModel())
                    }
                    logger.info("BLE scan found ${bluetoothDevices.size} devices, no fallback needed")
                }
            }
            handler.postDelayed(scanRunnable!!, scanTimeoutMs)
        } catch (e: SecurityException) {
            logger.debug("SecurityException starting BLE scan: ${e.message}")
            showToast(BLUETOOTH_PERMISSION)
            requestBluetoothPermissions()
            isScanning = false
            showLoading(false)
        } catch (e: Exception) {
            logger.debug("Failed to start BLE scan: ${e.message}")
            showToast(getString(R.string.text_failed_to_start_BLE_scan))
            isScanning = false
            showLoading(false)
        }
    }

    /**
     * Stops BLE scan
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun stopBleScan() {
        binding.swipeRefreshScan.isRefreshing = false
        delayedScanRunnable?.let { handler.removeCallbacks(it) }
        delayedScanRunnable = null
        stopBleScanner()
        stopClassicDiscovery()
        isScanning = false
        noDeviceCheckpointRunnable?.let { handler.removeCallbacks(it) }
        noDeviceCheckpointRunnable = null
        scanRunnable?.let { handler.removeCallbacks(it) }
        scanRunnable = null
    }

    /**
     * Stops the BLE scanner if currently scanning.
     */
    private fun stopBleScanner() {
        if (!isScanning || bluetoothLeScanner == null || !hasBluetoothPermissions()) return
        try {
            bluetoothLeScanner?.stopScan(bleScanCallback)
            logger.info("BLE scan stopped")
        } catch (e: SecurityException) {
            logger.debug("SecurityException stopping BLE scan: ${e.message}")
        } catch (e: Exception) {
            logger.debug("Error stopping BLE scan: ${e.message}")
        }
    }

    /**
     * Starts classic Bluetooth discovery alongside BLE scan (registered via [receiver]).
     */
    private fun startClassicDiscoveryIfPermitted() {
        if (!hasBluetoothPermissions()) return
        try {
            if (!bluetoothAdapter.isDiscovering) {
                val started = bluetoothAdapter.startDiscovery()
                logger.info("Classic Bluetooth discovery started alongside BLE scan: success=$started")
            }
        } catch (e: SecurityException) {
            logger.debug("SecurityException starting classic discovery: ${e.message}")
        }
    }

    /**
     * Stops classic Bluetooth discovery if currently discovering.
     */
    private fun stopClassicDiscovery() {
        val adapter = bluetoothAdapter

        // Explicit permission check required for Android 12+
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            logger.debug("BLUETOOTH_CONNECT permission not granted")
            return
        }

        if (!adapter.isDiscovering) return

        try {
            adapter.cancelDiscovery()
            logger.info("Classic Bluetooth discovery stopped")
        } catch (e: SecurityException) {
            logger.error("SecurityException stopping classic Bluetooth discovery:: $e")
        }
    }

    /**
     * Stops BLE scanning, and disconnects any
     * connected Bluetooth devices when the fragment is destroyed.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDestroy() {
        super.onDestroy()
        stopBleScan()
        bleLock.lock()
        try {
            connectedBleProtocols.values.forEach { disconnectProtocolAndRemoveFromState(it) }
            connectedBleProtocols.clear()
            bleProtocol = null
            logger.debug("Bluetooth disconnected in onDestroy: onDestroy")
        } finally {
            bleLock.unlock()
        }
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * Registers Bluetooth UI state listener to refresh device list when connection state changes
     * (e.g. after reconnecting and cancelling delink confirmation).
     */
    override fun onStart() {
        super.onStart()
        BluetoothUiStateManager.register(requireContext(), connectionStateRefreshListener)
    }

    override fun onStop() {
        super.onStop()
        BluetoothUiStateManager.unregister(connectionStateRefreshListener)
    }

    private val connectionStateRefreshListener: () -> Unit = {
        if (isAdded) {
            refreshDeviceListConnectionState()
        }
    }

    /**
     * Cleans up all BroadcastReceivers registered in onCreateView() to ensure proper
     * alignment with the Fragment view lifecycle.
     */
    override fun onDestroyView() {
        super.onDestroyView()

        try {
            requireContext().unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            logger.noStackTraceLog("Receiver not registered during cleanup", e)
        }

        try {
            requireContext().unregisterReceiver(connectionStateReceiver)
        } catch (e: IllegalArgumentException) {
            logger.noStackTraceLog("ConnectionReceiver not registered during cleanup", e)
        }

        // Cancel event collection
        eventCollectorJob?.cancel()
        eventCollectorJob = null
    }

    /**
     * Controls the visibility of a loading indicator and updates the parent activity’s loading state.
     *
     * @param show `true` to display the loading indicator, `false` to hide it.
     */
    private fun showLoading(show: Boolean) {
        requireActivity().runOnUiThread {
            if (!show && FcmSecureFlowCoordinator.isLoaderHoldActive()) {
                logger.debug("Suppressing loader hide during FCM secure flow")
                return@runOnUiThread
            }
            if (suppressDeleteScriptLoaderHides && !show) {
                logger.debug("Suppressing loader hide while sequential delete scripts are in progress")
                return@runOnUiThread
            }
            isLoading = show
            activity.showLoading(
                show, if (show) getString(R.string.text_please_wait) else ""
            )
        }
    }

    /**
     * Uses pull-to-refresh on the device list when visible; otherwise falls back to the full-screen loader.
     */
    private fun showRegistrationProgress(show: Boolean) {
        requireActivity().runOnUiThread {
            if (!isAdded) return@runOnUiThread
            if (binding.swipeRefreshDevices.isVisible) {
                binding.swipeRefreshDevices.isRefreshing = show
                if (show) forceHideLoading()
            } else {
                showLoading(show)
            }
        }
    }

    /**
     * Always hides loader/blur immediately, bypassing suppression logic.
     */
    private fun forceHideLoading() {
        suppressDeleteScriptLoaderHides = false
        isLoading = false
        if (::binding.isInitialized) {
            binding.swipeRefreshDevices.isRefreshing = false
        }
        logger.debug("forceHideLoading: clearing loader/blur state")
        activity.showLoading(false, "")
    }

    /**
     * backExit method is used to go back to Home Fragment
     */
    private fun backExit() {
        if (PayExternalLaunch.isHostLaunch()) {
            PayExternalLaunch.exitToHost(activity)
            return
        }
        val navController = findNavController()
        val currentId = navController.currentDestination?.id
        currentId?.let { navController.navigate(it) }
    }

    /**
     * Fetches the list of registered payment devices using the SDK API and updates the UI accordingly.
     * On success, the device list is shown and individual item interactions are configured.
     * On long press, device de-link API is triggered.
     *
     * @param context The context used for API call and preference storage.
     * @param showLoader When true, shows the full-screen loader; when false, uses pull-to-refresh only.
     */
    private fun getDeviceList(context: Context, showLoader: Boolean = true) {
        stopScanIfFetchingDeviceList()
        if (isLoginOlderThanSessionExpiryDuration()) {
            navigateToLoginScreen()
            return
        }

        if (showLoader) {
            showLoading(true)
            schedulePostRegisterLoaderSafetyHide()
        } else {
            clearPostRegisterLoaderSafetyHide()
            if (binding.swipeRefreshDevices.isVisible) {
                binding.swipeRefreshDevices.isRefreshing = true
            }
        }

        lifecycleScope.launch {
            val sdkResult = WalletRepository.fetchPaymentDevices(context = context)
            if (sdkResult.isSuccess) {
                sdkResult.response?.let { paymentDeviceResponseBody ->
                    onPaymentDevicesFetchSuccess(paymentDeviceResponseBody, showLoader)
                }
            } else {
                clearPostRegisterLoaderSafetyHide()
                showLoading(false)
                forceHideLoading()
                stopAllSwipeRefreshing()
                handleSessionOrError(sdkResult.errorMessage)
            }
        }
    }

    /**
     * Stops BLE and classic discovery before fetching the registered device list from the server.
     */
    private fun stopScanIfFetchingDeviceList() {
        if (isScanning && hasBluetoothPermissions()) {
            try {
                stopBleScan()
                logger.debug("Scan stopped - fetching device list")
            } catch (e: SecurityException) {
                logger.debug("SecurityException stopping scan: ${e.message}")
            }
        }
    }

    /**
     * Applies a successful fetchPaymentDevices response: updates linked addresses, list UI, and scan adapter.
     *
     * @param response   SDK response containing registered payment devices.
     * @param showLoader Whether the fetch was started with the full-screen loader.
     */
    private fun onPaymentDevicesFetchSuccess(response: PaymentDeviceResponseBody, showLoader: Boolean = true) {
        stopFetchUiState(showLoader)
        stopAllSwipeRefreshing()
        if (isResponseInvalid(response)) {
            return
        }

        when (response.statusMessage) {
            CommonResponse.SUCCESS.response -> applySuccessfulPaymentDeviceListResponse(
                response,
                suppressUiActions = !showLoader
            )

        }

        refreshLinkedDeviceAddresses()
        refreshScanAdapterAfterFetch()
    }

    /**
     * Hides loaders and swipe-refresh indicators after a device-list fetch completes.
     *
     * @param showLoader Mirrors the flag passed to [getDeviceList].
     */
    private fun stopFetchUiState(showLoader: Boolean) {
        clearPostRegisterLoaderSafetyHide()
        binding.swipeRefreshDevices.isRefreshing = false
        if (showLoader) showLoading(false) else forceHideLoading()
    }

    /**
     * Schedules a safety timeout to hide the loader if registration or fetch hangs.
     *
     * @param timeoutMs Maximum time before the loader is force-hidden.
     */
    private fun schedulePostRegisterLoaderSafetyHide(timeoutMs: Long = 12000L) {
        clearPostRegisterLoaderSafetyHide()
        val runnable = Runnable {
            if (!isAdded) return@Runnable
            if (isLoading) {
                logger.debug("Post-register loader safety timeout reached; force hiding loader")
                forceHideLoading()
                stopAllSwipeRefreshing()
            }
        }
        postRegisterLoaderSafetyRunnable = runnable
        handler.postDelayed(runnable, timeoutMs)
    }

    /**
     * Cancels the post-register loader safety timeout when the flow completes normally.
     */
    private fun clearPostRegisterLoaderSafetyHide() {
        postRegisterLoaderSafetyRunnable?.let { handler.removeCallbacks(it) }
        postRegisterLoaderSafetyRunnable = null
    }

    /**
     * Rebuilds the in-memory set of BLE addresses for devices already linked on the server.
     */
    private fun refreshLinkedDeviceAddresses() {
        linkedDeviceAddresses.clear()
        distinctDevices.forEach { device ->
            device.seId?.let { seId ->
                val address = getBluetoothAddressForSeId(seId)
                if (address.isNotEmpty()) {
                    linkedDeviceAddresses.add(address)
                }
            }
        }
    }

    /**
     * Notifies the scan adapter after the linked-device list changes so bonded devices are filtered correctly.
     */
    private fun refreshScanAdapterAfterFetch() {
        if (!isNFC() && ::deviceAdapter.isInitialized) {
            try {
                removeLinkedDevicesFromList()
            } catch (e: Exception) {
                TODO("Not yet implemented")
            }
            requireActivity().runOnUiThread {
                deviceAdapter.submitList(list = convertBluetoothDeviceToUiModel())
            }
        }
    }

    /**
     * Maps the SDK payment-device response into UI [PaymentDeviceList] items.
     *
     * @param response Successful fetchPaymentDevices response body.
     */
    private fun getListData(response: PaymentDeviceResponseBody): List<PaymentDeviceList> {
        return response.paymentDeviceLists.distinctBy { it.deviceName }.map {
            PaymentDeviceList(
                seId = it.seId,
                paymentAppInstanceId = it.paymentAppInstanceId,
                deviceModel = it.deviceModel,
                walletAppInstanceId = it.walletAppInstanceId,
                deviceName = it.deviceName,
                wearableDeviceModelId = it.wearableDeviceModelId,
                data = ImageUtils.base64ToBitmap(it.deviceMediaContent?.data!!),
                status = it.status
            )
        }.also { devices ->
            devices.forEach { device ->
                DeviceDetachTargetResolver.savePaymentAppToSeIdMapping(
                    requireContext(),
                    device.paymentAppInstanceId,
                    device.seId.orEmpty()
                )
            }
        }
    }

    /**
     * Updates adapter data and visibility after a valid payment-device list is received.
     *
     * @param response Successful fetchPaymentDevices response body.
     */
    private fun applySuccessfulPaymentDeviceListResponse(
        response: PaymentDeviceResponseBody,
        suppressUiActions: Boolean = false
    ) {
        logger.debug("Applying payment device list response. suppressUiActions=$suppressUiActions")
        distinctDevices = getListData(response);
        val serverSeIds = distinctDevices.mapNotNull { it.seId }.toSet()
        val localSeIds = getPairedSeIds()

        localSeIds
            .filterNot { it in serverSeIds }
            .forEach { localSeId ->
                logger.debug("Clearing stale local pairing for seId=$localSeId")
                clearGhostPairing(localSeId)
            }
        requireActivity().runOnUiThread {
            setupDeviceListUI()
        }
    }

    /**
     * Validates the payment device response structure to ensure required fields are not empty.
     *
     * @param response The response body from fetchPaymentDevices API.
     * @return True if the response is invalid; false otherwise.
     */
    private fun isResponseInvalid(response: PaymentDeviceResponseBody): Boolean {
        return response.statusMessage.isNullOrEmpty() || (response.paymentDeviceLists.isNotEmpty() && response.paymentDeviceLists.any {
            it.seId.isNullOrEmpty() || it.deviceModel.isNullOrEmpty() || it.paymentAppInstanceId.isNullOrEmpty() || it.deviceName.isNullOrEmpty()
        })
    }

    /**
     * to handle wearable device item click event
     */
    private fun handleDeviceClickEvent(device: PaymentDeviceList) {
        logger.debug("deviceStatus :: on item clicked")
        if (isLoginOlderThanSessionExpiryDuration()) {
            navigateToLoginScreen()
            return
        }
        if (!isNetworkAvailable(requireContext())) {
            confirmDataDialog(getString(R.string.data_enable))
            return
        }

        logger.debug("deviceStatus :: status " + device.status)
        if (device.status?.isNotEmpty() == true
            && (device.status.toString().equals(CardStatus.SUSPENDED.toString(), ignoreCase = true))
        ) {
            statusDialog(activity, getString(R.string.suspended_message, device.deviceName));
            return
        }

        DeviceDetachTargetResolver.savePaymentAppToSeIdMapping(
            requireContext(),
            device.paymentAppInstanceId,
            device.seId.orEmpty()
        )
        StorageRepository.apply {
            saveString(
                PreferenceKey.PAYMENT_APP_INSTANCE_ID,
                device.paymentAppInstanceId.toString()
            )
            saveString(PreferenceKey.DEVICE_SE_ID, device.seId.toString())
            saveString(PreferenceKey.DEVICE_NAME, device.deviceName.toString())
            saveString(PreferenceKey.DEVICE_IMAGE, ImageUtils.bitmapToBase64(device.data!!))
            saveString(PreferenceKey.WEARABLE_MODEL_ID, ConfiguredWalletIdentity.WEARABLE_ID)
        }
        CardListFragment.shouldForceApiRefresh = true

        val bundle = Bundle().apply {
            putString(BundleKey.PAYMENT_APP_INSTANCE_ID, device.paymentAppInstanceId)
            putString(BundleKey.DEVICE_NAME, device.deviceName)
            putString(
                BundleKey.WEARABLE_MODEL_ID,
                ConfiguredWalletIdentity.WEARABLE_ID
            )
        }

        findNavController().navigate(R.id.cardListFragment, bundle)
    }

    /**
     * Updates the UI with the fetched list of payment devices and sets click handlers.
     *
     * @param context The application or fragment context.
     * The valid response containing device list.
     */
    private fun setupDeviceListUI() {
        if (distinctDevices.isNotEmpty()) {
            restoreRegisteredDeviceListUi()
            updateDistinctListForAdapter()
            binding.tvNoDevice.visibility = View.GONE
            statusMonitor.requestPoll()
            binding.rvDevices.adapter =
                PaymentDeviceAdapter(distinctDevices, onItemClicked = { _, pos ->
                    handleDeviceClickEvent(distinctDevices[pos])
                }, onItemLongPress = { pos, deviceName ->
                    val device = distinctDevices[pos]
                    DeviceDetachTargetResolver.savePaymentAppToSeIdMapping(
                        requireContext(),
                        device.paymentAppInstanceId,
                        device.seId.orEmpty()
                    )

                    // Store selected device name (and seId) so background reconnect scan can match by name
                    StorageRepository.apply {
                        saveString(PreferenceKey.PAYMENT_APP_INSTANCE_ID, device.paymentAppInstanceId.toString())
                        saveString(PreferenceKey.DEVICE_SE_ID, device.seId.toString())
                        saveString(PreferenceKey.DEVICE_NAME, device.deviceName.toString())
                        saveString(PreferenceKey.WEARABLE_MODEL_ID, ConfiguredWalletIdentity.WEARABLE_ID)
                    }

                    handleDeviceLongPressClickEvent(device, deviceName)
                }, onButtonClick = { _, pos ->

                    if (!isNetworkAvailable(requireContext())) {
                        confirmDataDialog(getString(R.string.data_enable))
                        return@PaymentDeviceAdapter
                    }

                    val device = distinctDevices[pos]
                    customSuccessDialog(
                        device.deviceModel.toString(), device.seId.toString(), isExisting = true
                    )
                })
        } else {
            // Important: clear old adapter data so stale rows are not shown when server returns empty list.
            binding.rvDevices.adapter = PaymentDeviceAdapter(
                emptyList(),
                onItemClicked = { _, _ -> },
                onItemLongPress = { _, _ -> },
                onButtonClick = { _, _ -> }
            )
            binding.swipeRefreshDevices.visibility = View.VISIBLE
            binding.tvNoDevice.visibility =
                if (hostPreconnectedUiPresented && bluetoothDevices.isNotEmpty()) View.GONE
                else View.VISIBLE
            if (hasBluetoothPermissions()) {
                presentHostPreconnectedDeviceIfNeeded()
            }
        }
    }

    /**
     * Updates the device list with accessibility status based on the current wallet association.
     *
     * Marks devices as accessible when multi-companion support is enabled or when the
     * device is associated with the active wallet.
     */
    private fun updateDistinctListForAdapter() {
        val walletId = StorageRepository.readString(key = PreferenceKey.WALLET_ID)

        distinctDevices = distinctDevices.map { device ->
            device.copy(
                isDeviceAccessible = Constants.IS_MULTI_COMPANION_ENABLED ||
                    device.walletAppInstanceId == walletId
            )
        }
    }

    /**
     * Handles long-press on a registered device: shows delink confirmation when BLE is connected or NFC is used.
     *
     * @param device     Registered payment device from the server list.
     * @param deviceName Display name shown in the delink dialog.
     */
    private fun handleDeviceLongPressClickEvent(device: PaymentDeviceList, deviceName: String) {
        if (!isNFC()) {
            customDeleteDialog(
                device.paymentAppInstanceId.toString(),
                device.seId.toString(),
                deviceName
            )
        } else {
            ensureNfcReadyThenRun {
                customDeleteDialog(
                    device.paymentAppInstanceId.toString(),
                    device.seId.toString(),
                    deviceName
                )
            }
        }
    }

    /**
     * Displays a custom confirmation dialog asking the user if they want to add the selected device.
     *
     * @param selectedDevice The name or identifier of the selected device.
     * @param seId The Secure Element (SE) ID associated with the device.
     * @param isExisting Indicates whether the device is already known in the system.
     */
    private fun customSuccessDialog(
        selectedDevice: String, seId: String, isExisting: Boolean
    ) {
        logger.debug("customSuccessDialog $isExisting selectedDevice $selectedDevice")
        if (activeDialog?.isShowing == true) return

        val dialogViewBinding = DialogCommonMessageBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogViewBinding.root)
            .create()
        activeDialog = alertDialog // Keep track of it

        dialogViewBinding.txtOK.text = getString(R.string.ok)
        dialogViewBinding.txtCancel.text = getString(R.string.cancel)
        dialogViewBinding.txtMessage.text = getString(
            R.string.wants_to_add_device,
            selectedDevice
        )

        dialogViewBinding.txtCancel.setOnClickListener {
            alertDialog.dismiss()
        }
        dialogViewBinding.txtOK.setOnClickListener {
            if (!isNetworkAvailable(requireContext())) {
                showLoading(false)
                confirmDataDialog(getString(R.string.data_enable))
                return@setOnClickListener
            }
            alertDialog.dismiss()
            if (bleProtocol != null) {
                val wearableModelIdHex = StorageRepository.readString(PreferenceKey.WEARABLE_MODEL_ID)
                registerDeviceApiCall(
                    selectedDevice,
                    seId,
                    wearableModelIdHex
                )
            } else {
                CoroutineScope(AppDispatchers.MAIN).launch {
                    activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    getDeviceList(requireContext())
                    binding.tvTitle.text = getString(R.string.wearables)
                    binding.tvCancel.visibility = View.GONE
                    binding.swipeRefreshScan.visibility = View.GONE
                    binding.swipeRefreshDevices.visibility = View.VISIBLE
                    binding.customBottom.llActive.visibility = View.VISIBLE
                    binding.customBottom.llPassive.visibility = View.GONE
                    showLoading(false)
                }
            }
        }

        alertDialog.setOnDismissListener {
            activeDialog = null
            showLoading(false)
        }
        alertDialog.showSecure()
    }

    /**
     * Displays a dialog to confirm adding a selected Bluetooth device and initiates connection.
     *
     * @param selectedDevice Name of the selected Bluetooth device.
     * @param selectedDeviceScanResult The selected Bluetooth device object.
     */
    private fun customSuccessDialogScanResult(
        selectedDevice: String, selectedDeviceScanResult: BluetoothDevice
    ) {
        val isExisting = false
        logger.debug("customSuccessDialogScanResult $isExisting selectedDevice $selectedDevice")
        if (activeDialog?.isShowing == true) return
        if (!hasBluetoothPermissions()) {
            handleMissingBluetoothPermission()
            return
        }

        if (!isNetworkAvailable(requireContext())) {
            confirmDataDialog(getString(R.string.data_enable))
            return
        }

        val dialog = buildConfirmationDialog(selectedDevice) { confirmed ->
            if (confirmed) {
                logger.info(
                    "PAIR_FLOW: USER_CONFIRMED → starting pairing for " +
                        "${selectedDeviceScanResult.address}"
                )
                activity.showLoading(true, getString(R.string.pairing_device))
                connectToDevice(selectedDevice, selectedDeviceScanResult, isExisting)
            } else {
                setSecureState(SecureBLEState.UNPAIRED)
            }
        }
        activeDialog = dialog
        dialog.setOnDismissListener { activeDialog = null }
        dialog.showSecure()
    }

    /**
     * Handles missing Bluetooth permission by logging and requesting it.
     */
    private fun handleMissingBluetoothPermission() {
        logger.debug("BLUETOOTH_CONNECT permission not granted")
        showToast(BLUETOOTH_PERMISSION)
        requestBluetoothPermissions()
        showLoading(false)
    }

    /**
     * Builds a confirmation dialog for adding a device.
     */
    private fun buildConfirmationDialog(
        selectedDevice: String, onResult: (confirmed: Boolean) -> Unit
    ): AlertDialog {
        val dialogViewBinding = DialogCommonMessageBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogViewBinding.root)
            .create()

        dialogViewBinding.txtMessage.text =
            getString(R.string.wants_to_add_device, selectedDevice)

        dialogViewBinding.txtOK.apply {
            text = getString(R.string.add)
            setOnClickListener {
                alertDialog.dismiss()
                onResult(true)
            }
        }

        dialogViewBinding.txtCancel.apply {
            text = getString(R.string.cancel)
            setOnClickListener {
                alertDialog.dismiss()
                onResult(false)
            }
        }

        alertDialog.setOnDismissListener {
            showLoading(false)
        }
        return alertDialog
    }

    /**
     * Connects to the selected Bluetooth device and initiates SEID fetch.
     */
    private fun connectToDevice(
        selectedDevice: String, selectedDeviceScanResult: BluetoothDevice, isExisting: Boolean
    ) {
        if (tryConnectViaHostProtocol(selectedDevice, selectedDeviceScanResult, isExisting)) {
            return
        }

        // Hard stop ghost-paired reconnects
        if (secureBleState == SecureBLEState.GHOST_PAIRED) {
            logger.debug("Ghost paired device → forcing fresh pairing")
            secureBleState = SecureBLEState.UNPAIRED
        }

        // Stop scanning when connecting to a device
        if (isScanning && hasBluetoothPermissions()) {
            try {
                stopBleScan()
                logger.debug("Scan stopped - connecting to device")
            } catch (e: SecurityException) {
                logger.debug("SecurityException stopping scan: ${e.message}")
            }
        }
        clearPreviousDevicesData()
        val ctx = requireContext()
        SecoraWearableSDK.getInstance().getInterface().waitForBondThenProceed(
            ctx,
            selectedDeviceScanResult,
            20_000L,
            handler,
            {
                background {
                    if (!isAdded) return@background
                    // Keep existing BLE connections when adding another device.
                    // UI should show green icon for all currently connected devices.
                    try {
                        logger.debug("connectToDevice $isExisting selectedDevice $selectedDevice")
                        lastConnectedDevice = selectedDeviceScanResult
                        BluetoothStateManager.connectBleDevice(ctx, selectedDeviceScanResult)
                            .thenApplyAsync { protocol ->
                                activity.runOnUiThread {
                                    bleProtocol = protocol
                                    normalizeAddress(selectedDeviceScanResult.address)?.let {
                                        connectedBleProtocols[it] = protocol
                                    }
                                    secureBleState = SecureBLEState.PAIRING
                                    fetchSEIDFromDevice(
                                        protocol,
                                        selectedDeviceScanResult,
                                        selectedDevice,
                                        isExisting
                                    )
                                }
                                null
                            }
                            .exceptionally { cause ->
                                activity.runOnUiThread {
                                    handleConnectionFailure(selectedDeviceScanResult, cause)
                                }
                                null
                            }
                    } catch (e: SecurityException) {
                        logger.debug("SecurityException during BLE connection: ${e.message}")
                        handleMissingBluetoothPermission()
                    }
                }
            },
            {
                handleMissingBluetoothPermission()
            }
        )
    }

    /**
     * Handles Bluetooth connection failure.
     */
    private fun handleConnectionFailure(device: BluetoothDevice, cause: Throwable) {
        requireActivity().runOnUiThread {
            val rootMessage = getRootCauseMessage(cause)
            val deviceName = getSafeDeviceName(device) ?: DEFAULT_DEVICE_NAME
            if (isMissingRequiredGattUuidError(cause)) {
                showBleCompatibilityDialog(deviceName)
            } else {
                showToast(getString(R.string.text_failed_to_connect, deviceName))
            }
            logger.debug("Connection failed: $rootMessage")
            showLoading(false)
        }
    }

    /**
     * Returns true if the failure chain includes the “missing required GATT UUIDs” error from the wearable SDK.
     *
     * @param throwable Root or wrapped error from BLE connect / GATT setup.
     */
    private fun isMissingRequiredGattUuidError(throwable: Throwable?): Boolean {
        var current = throwable
        while (current != null) {
            val message = current.message ?: ""
            if (message.contains(SeConstants.BLE_MISSING_REQUIRED_GATT_UUIDS, ignoreCase = true)) {
                return true
            }
            current = current.cause
        }
        return false
    }

    /**
     * Walks the cause chain and returns the last non-blank [Throwable.message], or `"unknown"`.
     *
     * @param throwable Any error from connection or script execution.
     */
    private fun getRootCauseMessage(throwable: Throwable?): String {
        var current = throwable
        var lastMessage = throwable?.message ?: "unknown"
        while (current != null) {
            if (!current.message.isNullOrBlank()) {
                lastMessage = current.message!!
            }
            current = current.cause
        }
        return lastMessage
    }

    /**
     * Shows a non-cancelable dialog when the peripheral does not expose the GATT UUIDs required by this app build.
     *
     * @param deviceName User-visible device label for the message body.
     */
    private fun showBleCompatibilityDialog(deviceName: String) {
        if (!isAdded) return
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.text_secora_wallet))
            .setMessage(getString(R.string.ble_missing_required_uuids_message, deviceName))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton?.let { button ->
                (button.parent as? android.widget.LinearLayout)?.gravity = android.view.Gravity.CENTER_HORIZONTAL
                button.textAlignment = View.TEXT_ALIGNMENT_CENTER
            }
        }

        dialog.showSecure()

        dialog.findViewById<TextView>(android.R.id.message)?.apply {
            gravity = android.view.Gravity.START
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }
    }

    /**
     * Reuses the host Fission BLE link instead of opening a second GATT connection.
     */
    private fun tryConnectViaHostProtocol(
        selectedDevice: String,
        device: BluetoothDevice,
        isExisting: Boolean,
    ): Boolean {
        if (!isHostPreconnectedDevice(device)) return false
        val protocol = BluetoothStateManager.activeProtocol ?: return false

        if (isScanning && hasBluetoothPermissions()) {
            try {
                stopBleScan()
                logger.debug("Scan stopped - reusing host SECORA protocol")
            } catch (e: SecurityException) {
                logger.debug("SecurityException stopping scan: ${e.message}")
            }
        }
        // Do not call clearPreviousDevicesData() here — it releases the host shared SECORA channel.
        lastConnectedDevice = device
        (protocol as? BleProtocol)?.let { bleProtocol = it }
        BluetoothStateManager.setActiveProtocol(protocol)
        BluetoothStateManager.addConnectedDevice(device.address)
        secureBleState = SecureBLEState.PAIRING
        activity.showLoading(true, getString(R.string.pairing_device))
        fetchSEIDFromDevice(protocol, device, selectedDevice, isExisting)
        return true
    }

    private fun disconnectProtocolSafely(protocol: ISecoraBleProtocol) {
        if (protocol is IHostSharedBleProtocol) return
        if (protocol is BleProtocol) {
            SecoraWearableSDK.getInstance().getInterface().disconnectBLEDevice(protocol)
        }
    }

    /**
     * Fetches the SEID from the device using a script and handles post-fetch logic.
     */
    private fun fetchSEIDFromDevice(
        protocol: ISecoraBleProtocol,
        device: BluetoothDevice,
        selectedDevice: String,
        isExisting: Boolean,
    ) {
        try {
            logger.debug("fetchSEIDFromDevice $isExisting selectedDevice $selectedDevice")
            // Create ScriptHandler instance and use fetchSEId method
            val scriptHandler = createScriptHandler()
            scriptHandler.setBleProtocol(protocol)

            scriptHandler.fetchSEId().handle { seId, throwable ->
                requireActivity().runOnUiThread {
                    if (throwable != null || seId == null) {
                        logger.error(
                            "PAIRING_FAILED for ${device.address}: " +
                                (throwable?.message ?: "seId was null")
                        )

                        // Clear ALL stored pairing for this device
                        val knownSeIds: List<String> = getPairedSeIds().toList()
                        knownSeIds.forEach { clearGhostPairing(it) }

                        secureBleState = SecureBLEState.UNPAIRED
                        showToast("Device pairing failed. Please try again.")
                        showLoading(false)

                        disconnectProtocolSafely(protocol)
                    } else {
                        val wearableModelIdHex = StorageRepository.readString(PreferenceKey.WEARABLE_MODEL_ID)
                        onSEIDFetched(seId, device, selectedDevice, wearableModelIdHex)
                    }
                }
                null
            }
        } catch (e: Exception) {
            logger.debug("Error fetching SEID: ${e.message}")
            requireActivity().runOnUiThread {
                showToast("SEID fetch error: ${e.message}")
                showLoading(false)
            }
            disconnectProtocolSafely(protocol)
        }
    }

    /**
     * Handles the logic after SEID is successfully fetched.
     */
    private fun onSEIDFetched(
        seId: String,
        device: BluetoothDevice,
        selectedDevice: String,
        wearableModelIdHex: String = ""
    ) {
        secureBleState = SecureBLEState.PAIRED_CONNECTED

        val deviceAddress = try {
            device.address
        } catch (e: SecurityException) {
            logger.debug("Failed to get device address: ${e.message}")
            ""
        }

        // Add this device to connected set so this device's BLE icon is green; others from set stay green too.
        BluetoothStateManager.addConnectedDevice(deviceAddress, seId)
        bleProtocol?.let { normalizeAddress(deviceAddress)?.let { key -> connectedBleProtocols[key] = it } }

        addPairedDevice(seId, deviceAddress)
        storeDeviceInfo(deviceAddress, seId)
        StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID).takeIf { it.isNotBlank() }
            ?.let { pid ->
                DeviceDetachTargetResolver.savePaymentAppToSeIdMapping(requireContext(), pid, seId)
            }

        binding.swipeRefreshScan.visibility = View.GONE
        if (bleProtocol != null || BluetoothStateManager.activeProtocol != null) {
            registerDeviceApiCall(selectedDevice, seId, wearableModelIdHex)
        } else {
            showRegisteredDeviceUI()
        }
    }

    /**
     * Saves selected device info to preferences.
     */
    private fun storeDeviceInfo(deviceAddress: String, seId: String) {
        StorageRepository.saveString(PreferenceKey.SELECTED_DEVICE_ADDRESS, deviceAddress)
        StorageRepository.saveString(PreferenceKey.DEVICE_SE_ID, seId)
    }

    /**
     * Shows the UI when device is already registered.
     */
    private fun showRegisteredDeviceUI() {
        with(binding) {
            swipeRefreshScan.visibility = View.GONE
            swipeRefreshDevices.visibility = View.VISIBLE
            tvTitle.text = getString(R.string.wearables)
            tvCancel.visibility = View.GONE
            customBottom.root.visibility = View.VISIBLE
            customBottom.llActive.visibility = View.VISIBLE
            customBottom.llPassive.visibility = View.GONE
        }
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        getDeviceList(requireContext())
        showLoading(false)
    }

    /**
     * Shows a confirmation dialog to delete (de-link) a registered payment device.
     *
     * @param paymentAppInstanceId The unique ID of the payment app instance.
     * @param seId The Secure Element Identifier of the device.
     * @param selectedDevice The name of the device to be shown in the dialog.
     */
    private fun customDeleteDialog(
        paymentAppInstanceId: String, seId: String, selectedDevice: String?
    ) {
        val dialogViewBinding = DialogCommonMessageBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogViewBinding.root)
            .create()
        setupDeleteDialogUI(dialogViewBinding, selectedDevice)

        dialogViewBinding.txtOK.setOnClickListener {

            if (!isNetworkAvailable(requireContext())) {
                alertDialog.dismiss()
                confirmDataDialog(getString(R.string.data_enable))
                return@setOnClickListener
            }
            alertDialog.dismiss()
            showLoading(true)
            if (!isNFC())
                handleDeleteDeviceRequest(paymentAppInstanceId, seId, selectedDevice)
            else
                handleDeleteDeviceRequestNFC(paymentAppInstanceId, seId, selectedDevice)
        }

        dialogViewBinding.txtCancel.setOnClickListener {
            alertDialog.dismiss()
            showLoading(false)
            refreshDeviceListConnectionState()
        }
        alertDialog.showSecure()
    }

    /**
     * Refreshes the device list adapter so BLE connection status (green/black icon) is updated.
     * Call when connection state changes (e.g. after reconnecting and cancelling delink).
     */
    private fun refreshDeviceListConnectionState() {
        if (isAdded && binding.rvDevices.isVisible) {
            binding.rvDevices.adapter?.notifyDataSetChanged()
        }
    }

    /**
     * Configures the UI of the delete confirmation dialog.
     */
    private fun setupDeleteDialogUI(dialogBinding: DialogCommonMessageBinding, selectedDevice: String?) {
        dialogBinding.txtTitle.text = getString(
            R.string.confirm_delete_with_device, getString(R.string.text_confirm_delete),
            selectedDevice
        )
        dialogBinding.txtMessage.text = getString(R.string.de_link_device)
        dialogBinding.txtOK.text = getString(R.string.text_proceed)
        dialogBinding.txtCancel.text = getString(R.string.cancel)
    }

    /**
     * Handles the logic to perform API call for deleting the device.
     */
    private fun handleDeleteDeviceRequest(
        paymentAppInstanceId: String, seId: String, selectedDevice: String?
    ) {
        if (!isNetworkAvailable(requireContext())) {
            showLoading(false)
            confirmDataDialog(getString(R.string.data_enable))
            return
        }

        ManualDeviceDelinkGate.markManualDelinkCompleted(seId, paymentAppInstanceId)

        if (!isTargetDeviceBleConnected(seId)) {
            removePaymentDeviceWithoutBle(
                paymentAppInstanceId = paymentAppInstanceId,
                seId = seId,
                onSuccess = { response ->
                    handleDeviceDeleteSuccess(
                        response = response,
                        seId = seId,
                        selectedDevice = selectedDevice
                    )
                }
            )
            return
        }

        removePaymentDeviceWithBle(
            paymentAppInstanceId = paymentAppInstanceId,
            seId = seId,
            onSuccess = { response ->
                handleDeviceDeleteSuccess(
                    response = response,
                    seId = seId,
                    selectedDevice = selectedDevice
                )
            }
        )
    }

    /**
     * Initiates device deletion over NFC: removePaymentDevice and follow-up delete scripts.
     *
     * @param paymentAppInstanceId Payment app instance ID for the delink API.
     * @param seId                   Secure element ID of the device.
     * @param selectedDevice         Display name for success and error dialogs.
     */
    private fun handleDeleteDeviceRequestNFC(
        paymentAppInstanceId: String, seId: String, selectedDevice: String?
    ) {
        if (!isNetworkAvailable(requireContext())) {
            showLoading(false)
            confirmDataDialog(getString(R.string.data_enable))
            return
        }

        fetchSequenceNumberFromDevice(
            onRetrieved = { currentSequenceCounter ->

                lifecycleScope.launch {
                    suppressDeleteScriptLoaderHides = false
                    activity.showLoading(true, getString(R.string.text_please_wait))

                    val sdkResult = WalletRepository.removePaymentDevice(
                        context = activity,
                        paymentAppInstanceId = paymentAppInstanceId,
                        seId = seId,
                        connected = true,
                        currentSequenceCounter = currentSequenceCounter
                    )
                    if (sdkResult.isSuccess) {
                        sdkResult.response?.let { deleteDeviceResponse ->
                            handleDeviceDeleteSuccess(
                                response = deleteDeviceResponse,
                                seId = seId,
                                selectedDevice = selectedDevice
                            )
                        }
                    } else {
                        showLoading(false)
                        handleSessionOrError(sdkResult.errorMessage)
                    }
                }

            },
            onFailed = {
            })
    }

    /**
     * Handles success response from delete device API call.
     */
    private fun handleDeviceDeleteSuccess(response: DeleteDeviceResponse, seId: String, selectedDevice: String?) {
        val statusMessage = response.statusMessage
        if (statusMessage.isNullOrEmpty()) {
            logger.debug("DeleteDeviceResponse statusMessage is null/empty; hiding loader")
            statusDialog(
                requireActivity(), getString(R.string.something_went_wrong)
            )
            forceHideLoading()
            return
        }

        requireActivity().runOnUiThread {
            when (statusMessage) {
                CommonResponse.SUCCESS.response -> {
                    if (!isNFC())
                        processSuccessfulDeviceDelete(response, seId, selectedDevice)
                    else
                        processSuccessfulDeviceDeleteNFC(response, seId, selectedDevice)
                }

                else -> {
                    showLoading(false)
                    deLinkDialog(requireContext(), statusMessage)
                }
            }
        }
    }

    /**
     * Processes logic after a successful device deletion.
     */
    private fun processSuccessfulDeviceDelete(
        response: DeleteDeviceResponse, seId: String, selectedDevice: String?
    ) {
        val paymentAppInstanceId = StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID).trim()
            .takeIf { it.isNotEmpty() }
        ManualDeviceDelinkGate.markManualDelinkCompleted(
            seId,
            paymentAppInstanceId
        )

        val address = getBluetoothAddressForSeId(seId)
        if (address.isNotEmpty()) {
            linkedDeviceAddresses.remove(address)
            logger.info("Delinked device removed from linkedDeviceAddresses: $address")
        }

        bluetoothDevices.clear()
        if (::deviceAdapter.isInitialized) {
            deviceAdapter.submitList(emptyList())
        }

        lifecycleScope.launch {
            if (::activity.isInitialized && !activity.isFinishing) {
                StorageRepository.clearAllLocalCardData(activity)
            }
        }

        val deleteScripts = response.deleteScriptList

        if (deleteScripts.isEmpty() || shouldSkipDeleteScriptsForDisconnectedBle(seId)) {
            val message =
                getString(R.string.device_delinked_successfully, selectedDevice)
            deLinkDialog(requireContext(), message)
            completeManualDelinkBleCleanup(seId)
            suppressDeleteScriptLoaderHides = false
            showLoading(false)
            clearDevice()
        } else {
            if (hasBluetoothPermissions()) {
                try {
                    executeDeleteScriptsSequentially(deleteScripts, seId, selectedDevice ?: "")
                } catch (e: SecurityException) {
                    PendingDeleteScriptExecutionGate.end(seId)
                    logger.debug("SecurityException during script execution: ${e.message}")
                    showLoading(false)
                }
            } else {
                logger.debug("Bluetooth permissions missing for script execution")
                showLoading(false)
            }
        }
    }

    /**
     * Post-delete cleanup for NFC delink: local prefs, BLE disconnect, and device list refresh.
     *
     * @param response       Successful deleteDevice SDK response.
     * @param seId           Secure element ID of the removed device.
     * @param selectedDevice Display name for user-facing messages.
     */
    private fun processSuccessfulDeviceDeleteNFC(
        response: DeleteDeviceResponse, seId: String, selectedDevice: String?
    ) {
        val paymentAppInstanceId =
            StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID).trim().takeIf { it.isNotEmpty() }
        ManualDeviceDelinkGate.markManualDelinkCompleted(
            seId,
            paymentAppInstanceId
        )

        if (::deviceAdapter.isInitialized) {
            deviceAdapter.submitList(emptyList())
        }

        lifecycleScope.launch {
            if (::activity.isInitialized && !activity.isFinishing) {
                StorageRepository.clearAllLocalCardData(activity)
            }
        }

        val deleteScripts = response.deleteScriptList
        if (deleteScripts.isEmpty()) {
            val message =
                getString(R.string.device_delinked_successfully, selectedDevice)
            deLinkDialog(requireContext(), message)
            suppressDeleteScriptLoaderHides = false
            showLoading(false)
        } else {
            try {
                executeDeleteScriptsSequentially(deleteScripts, seId, selectedDevice ?: "")
            } catch (e: SecurityException) {
                PendingDeleteScriptExecutionGate.end(seId)
                logger.debug("SecurityException during script execution: ${e.message}")
                showLoading(false)
            }
        }
    }

    /**
     * Disconnects BLE and resets state safely with lock.
     * @param seId Optional SE ID; when provided and [removeBond] is true, also removes the system bond (unpair).
     * @param removeBond When true and [seId] has a known address, calls SDK removeBond after disconnect (e.g. on delink).
     */
    private fun safelyDisconnectBLE(seId: String, removeBond: Boolean = false) {
        val deviceAddress = getBluetoothAddressForSeId(seId)
        bleLock.lock()
        try {
            val didDisconnect = when {
                !hasBluetoothPermissions() || deviceAddress.isBlank() -> fallbackProtocolDisconnect()
                !hasBluetoothConnectPermission() -> false
                else -> disconnectUsingAddress(seId, deviceAddress, removeBond)
            }
            finalizeSafelyDisconnect(didDisconnect)
        } catch (e: Exception) {
            logger.debug("Exception during BLE disconnection: ${e.message}")
        } finally {
            bleLock.unlock()
        }
    }

    /**
     * Returns whether [Manifest.permission.BLUETOOTH_CONNECT] is granted for disconnect and bond removal.
     */
    private fun hasBluetoothConnectPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Disconnects the active GATT protocol when address-based disconnect is unavailable.
     */
    private fun fallbackProtocolDisconnect(): Boolean {
        logger.debug("BLE permission denied or address null. Falling back to protocol disconnect.")
        bleProtocol?.let { disconnectProtocolAndRemoveFromState(it) }
        return true
    }

    /**
     * Disconnects BLE for [seId] using [deviceAddress] and optionally removes the system bond.
     *
     * @param seId           Secure element ID of the device.
     * @param deviceAddress  BLE MAC used for disconnect and bond removal.
     * @param removeBond     When true, unpairs the device after disconnect (manual delink).
     */
    private fun disconnectUsingAddress(seId: String, deviceAddress: String, removeBond: Boolean): Boolean {
        if (!BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
            logger.debug(
                "Invalid BLE address derived for seId=$seId. deviceAddress=$deviceAddress. " +
                    "Falling back to protocol disconnect."
            )
            return fallbackProtocolDisconnect()
        }

        disconnectAndRemoveDevice(deviceAddress)
        disconnectActiveProtocolForAddress(deviceAddress)
        if (removeBond) {
            removeBondSafely(deviceAddress)
        }
        return true
    }

    /**
     * Disconnects [BluetoothStateManager.activeProtocol] when it targets [deviceAddress].
     * ScriptHandler stores the active GATT session there and may not mirror it in [connectedBleProtocols].
     */
    private fun disconnectActiveProtocolForAddress(deviceAddress: String) {
        val active = BluetoothStateManager.activeProtocol ?: return
        if (PayExternalLaunch.shouldIgnoreHostAclDisconnect(deviceAddress) &&
            active is IHostSharedBleProtocol
        ) {
            logger.debug("Host launch: skip disconnectActiveProtocol for shared SECORA channel")
            return
        }
        if (normalizeAddress(active.bluetoothDevice.address) != normalizeAddress(deviceAddress)) return
        BluetoothStateManager.disconnectActiveProtocol()
        clearFragmentBleStateForAddress(deviceAddress)
    }

    /**
     * Full post-delink BLE cleanup: disconnect GATT, unbond, and clear local pairing prefs.
     * Matches the FCM device-detach path so the wearable can advertise again for scanning.
     */
    private fun completeManualDelinkBleCleanup(seId: String) {
        val addressHint = getBluetoothAddressForSeId(seId)
        DeviceDetachBleCleanup.cleanup(requireContext(), seId, addressHint)
        clearFragmentBleStateForAddress(addressHint)
        logger.info("Manual delink BLE cleanup completed for seId=$seId address=$addressHint")
    }

    /** Clears fragment-local protocol references for [deviceAddress] after disconnect/unbond. */
    private fun clearFragmentBleStateForAddress(deviceAddress: String) {
        if (deviceAddress.isBlank()) return
        val key = normalizeAddress(deviceAddress) ?: return
        bleLock.lock()
        try {
            connectedBleProtocols.remove(key)
            if (bleProtocol?.bluetoothDevice?.address?.let { normalizeAddress(it) == key } == true) {
                bleProtocol = connectedBleProtocols.values.firstOrNull()
            }
        } finally {
            bleLock.unlock()
        }
    }

    /**
     * Removes the Android Bluetooth bond for [deviceAddress] when connect permission is granted.
     *
     * @param deviceAddress BLE MAC of the paired wearable.
     */
    private fun removeBondSafely(deviceAddress: String) {
        try {
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            SecoraWearableSDK.getInstance().getInterface().removeBond(device)
            logger.debug("Bond removed for delinked device: $deviceAddress")
        } catch (e: SecurityException) {
            logger.debug("SecurityException removing bond: ${e.message}")
        } catch (e: IllegalArgumentException) {
            logger.debug("Invalid address removing bond: $deviceAddress. ${e.message}")
        }
    }

    /**
     * Refreshes the device list UI after [safelyDisconnectBLE] completes.
     *
     * @param didDisconnect Whether a disconnect or bond removal was attempted.
     */
    private fun finalizeSafelyDisconnect(didDisconnect: Boolean) {
        if (!didDisconnect) return
        bleProtocol = null
        logger.debug("Bluetooth safely disconnected")
    }

    /**
     * deLinkDialog function displays a custom dialog to inform the user with a message.
     * It prevents dialog display if the activity is finishing.
     * On OK button click, it dismisses the dialog and launches a coroutine to fetch the device list.
     */
    fun deLinkDialog(activity: Context, message: String?) {
        requireActivity().runOnUiThread {
            if (!(activity as Activity).isFinishing) {
                val dialogViewBinding = DialogCommonMessageBinding.inflate(layoutInflater)
                val alertDialog = Dialog(requireContext()).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setContentView(dialogViewBinding.root)
                    setCancelable(false)
                }

                dialogViewBinding.txtTitle.text = getString(R.string.text_secora_wallet)
                dialogViewBinding.txtMessage.text = message
                dialogViewBinding.txtCancel.visibility = View.GONE

                dialogViewBinding.txtOK.setOnClickListener {
                    alertDialog.dismiss()
                    CoroutineScope(Dispatchers.IO).launch {
                        getDeviceList(requireContext())
                    }
                    showLoading(true)
                }
                alertDialog.setOnDismissListener {
                    showLoading(true)
                }
                alertDialog.showSecure()
            }
        }
    }

    /**
     * Displays a non-cancelable dialog notifying the user that the card or device
     * is linked to another user or account, and handles the de-linking flow.
     *
     * @param message The message to display inside the dialog.
     * @param paymentAppInstanceId ID of the payment app instance to be de-linked.
     * @param seId Secure Element ID.
     * @param selectedDevice The name or ID of the selected Bluetooth device.
     * @param connected Whether the device is currently connected.
     */
    private fun showDeLinkDialog(
        message: String?, paymentAppInstanceId: String, seId: String, selectedDevice: String?, connected: Boolean
    ) {
        requireActivity().runOnUiThread {
            val activity = activity as Activity
            if (activity.isFinishing) return@runOnUiThread

            val dialogViewBinding = DialogCommonMessageBinding.inflate(layoutInflater)
            val alertDialog = Dialog(requireContext()).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(dialogViewBinding.root)
                setCancelable(false)
            }
            alertDialog.apply {
                dialogViewBinding.txtTitle.text = getString(R.string.text_secora_wallet)
                dialogViewBinding.txtMessage.text = message
                dialogViewBinding.txtOK.text = getString(R.string.text_proceed)
                dialogViewBinding.txtCancel.apply {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        dismiss()
                        showLoading(false)
                        handleScanScreen()
                    }
                }
                dialogViewBinding.txtOK.setOnClickListener {
                    StorageRepository.saveString(PreferenceKey.DEVICE_NAME, selectedDevice.toString())
                    if (!isNFC())
                        onOkClicked(alertDialog, paymentAppInstanceId, seId, selectedDevice)
                    else
                        onOkNFCClicked(alertDialog, paymentAppInstanceId, seId, selectedDevice, connected)
                }
                showSecure()
            }
        }
    }

    /**
     * Handles UI reset and device list refresh when returning to the Scan screen.
     *
     * - Rebuilds linked device addresses to ensure recently reconnected devices
     *   (e.g., before Cancel action) are treated as linked.
     * - Removes already linked devices from the scan list.
     * - Updates adapter with the filtered device list.
     * - Resets UI state: shows scan loader, title, cancel option,
     *   and prepares RecyclerView for fresh scan results.
     */
    private fun handleScanScreen() {
        // Rebuild so device just reconnected (before Cancel) is treated as linked and excluded from scan list
        if (!isNFC()) {
            rebuildLinkedDeviceAddresses()
            removeLinkedDevicesFromList()
            if (::deviceAdapter.isInitialized) {
                deviceAdapter.submitList(list = convertBluetoothDeviceToUiModel())
            }
        }
        binding.tvNoDevice.visibility = View.GONE
        binding.tvTitle.text = getString(R.string.scan_devices)
        binding.scanLoader.tvBt.visibility = View.VISIBLE
        binding.tvCancel.visibility = View.VISIBLE
        binding.swipeRefreshDevices.visibility = View.GONE
        binding.swipeRefreshScan.visibility = View.VISIBLE
    }

    /**
     * Returns true when the target wearable is connected over BLE for script execution.
     *
     * @param seId Secure element ID of the device being delinked or updated.
     */
    private fun isTargetDeviceBleConnected(seId: String): Boolean =
        BluetoothStateManager.isDeviceConnected(seId, requireContext())

    /**
     * Delete scripts require an active BLE session; skip them when the target device is not connected.
     *
     * @param seId Secure element ID of the device being delinked.
     * @return `true` for non-NFC flows when BLE is not connected to [seId].
     */
    private fun shouldSkipDeleteScriptsForDisconnectedBle(seId: String): Boolean =
        !isNFC() && !isTargetDeviceBleConnected(seId)

    /**
     * Calls removePaymentDevice when BLE is not connected, using a fallback sequence counter.
     *
     * @param paymentAppInstanceId Payment app instance ID for the delink API.
     * @param seId                   Secure element ID of the device.
     * @param onSuccess              Invoked with the SDK response when removal succeeds.
     * @param onComplete             Invoked after the API call finishes (success or failure).
     */
    private fun removePaymentDeviceWithoutBle(
        paymentAppInstanceId: String,
        seId: String,
        onSuccess: (DeleteDeviceResponse) -> Unit,
        onComplete: () -> Unit = {}
    ) {
        lifecycleScope.launch {
            suppressDeleteScriptLoaderHides = false
            activity.showLoading(true, getString(R.string.text_please_wait))
            val sdkResult = WalletRepository.removePaymentDevice(
                context = activity,
                paymentAppInstanceId = paymentAppInstanceId,
                seId = seId,
                connected = false,
                currentSequenceCounter = FALLBACK_SEQUENCE_COUNTER
            )
            onComplete()
            if (sdkResult.isSuccess) {
                sdkResult.response?.let(onSuccess)
            } else {
                showLoading(false)
                handleSessionOrError(sdkResult.errorMessage)
            }
        }
    }

    /**
     * Reads the sequence counter over BLE and calls removePaymentDevice when the target device is connected.
     * Falls back to [removePaymentDeviceWithoutBle] when the counter cannot be read.
     *
     * @param paymentAppInstanceId Payment app instance ID for the delink API.
     * @param seId                   Secure element ID of the device.
     * @param onSuccess              Invoked with the SDK response when removal succeeds.
     */
    private fun removePaymentDeviceWithBle(
        paymentAppInstanceId: String,
        seId: String,
        onSuccess: (DeleteDeviceResponse) -> Unit
    ) {
        suppressDeleteScriptLoaderHides = false
        activity.showLoading(true, getString(R.string.text_please_wait))

        fetchSequenceNumberFromDevice(
            onRetrieved = { currentSequenceCounter ->
                lifecycleScope.launch {
                    val sdkResult = WalletRepository.removePaymentDevice(
                        context = activity,
                        paymentAppInstanceId = paymentAppInstanceId,
                        seId = seId,
                        connected = true,
                        currentSequenceCounter = currentSequenceCounter
                    )
                    if (sdkResult.isSuccess) {
                        sdkResult.response?.let(onSuccess)
                    } else {
                        showLoading(false)
                        handleSessionOrError(sdkResult.errorMessage)
                    }
                }
            },
            onFailed = {
                removePaymentDeviceWithoutBle(
                    paymentAppInstanceId = paymentAppInstanceId,
                    seId = seId,
                    onSuccess = onSuccess
                )
            },
            allowReconnectPrompt = false
        )
    }

    /**
     * Handles Proceed on the de-link dialog for BLE devices.
     * Calls removePaymentDevice with or without BLE based on current connection state.
     *
     * @param alertDialog            De-link confirmation dialog to dismiss.
     * @param paymentAppInstanceId   Payment app instance ID for the delink API.
     * @param seId                     Secure element ID of the device.
     * @param selectedDevice           Display name used in post-delink success handling.
     */
    private fun onOkClicked(
        alertDialog: Dialog,
        paymentAppInstanceId: String,
        seId: String,
        selectedDevice: String?
    ) {
        alertDialog.dismiss()

        if (!isNetworkAvailable(requireContext())) {
            showLoading(false)
            confirmDataDialog(getString(R.string.data_enable))
            return
        }

        if (!isTargetDeviceBleConnected(seId)) {
            ManualDeviceDelinkGate.markManualDelinkCompleted(seId, paymentAppInstanceId)
            removePaymentDeviceWithoutBle(
                paymentAppInstanceId = paymentAppInstanceId,
                seId = seId,
                onSuccess = { response ->
                    forceHideLoading()
                    handleRemoveDeviceSuccess(response, seId, selectedDevice)
                },
                onComplete = { forceHideLoading() }
            )
            return
        }

        ManualDeviceDelinkGate.markManualDelinkCompleted(seId, paymentAppInstanceId)
        removePaymentDeviceWithBle(
            paymentAppInstanceId = paymentAppInstanceId,
            seId = seId,
            onSuccess = { response ->
                forceHideLoading()
                handleRemoveDeviceSuccess(response, seId, selectedDevice)
            }
        )
    }

    /**
     * Handles the OK button click event in the dialog.
     */
    private fun onOkNFCClicked(
        alertDialog: Dialog, paymentAppInstanceId: String, seId: String, selectedDevice: String?, connected: Boolean
    ) {
        alertDialog.dismiss()
        if (!isNetworkAvailable(requireContext())) {
            showLoading(false)
            confirmDataDialog(getString(R.string.data_enable))
            return
        }

        fetchSequenceNumberFromDevice(
            onRetrieved = { currentSequenceCounter ->
                lifecycleScope.launch {
                    suppressDeleteScriptLoaderHides = false
                    activity.showLoading(true, getString(R.string.text_please_wait))

                    val sdkResult = WalletRepository.removePaymentDevice(
                        context = activity,
                        paymentAppInstanceId = paymentAppInstanceId,
                        seId = seId,
                        connected = connected,
                        currentSequenceCounter = currentSequenceCounter
                    )
                    forceHideLoading()

                    if (sdkResult.isSuccess) {
                        sdkResult.response?.let { deleteDeviceResponse ->
                            handleRemoveDeviceSuccess(deleteDeviceResponse, seId, selectedDevice)
                        }
                    } else {
                        handleSessionOrError(sdkResult.errorMessage)
                    }
                }

            },
            onFailed = {
            })

    }

    /**
     * Handles the successful response from removePaymentDevice.
     */
    private fun handleRemoveDeviceSuccess(
        response: DeleteDeviceResponse, seId: String, selectedDevice: String?
    ) {

        activity.runOnUiThread {
            val status = response.statusMessage
            if (status.isNullOrEmpty()) {
                logger.debug("DeleteDeviceResponse statusMessage is null/empty")
                return@runOnUiThread
            }

            if (status == CommonResponse.SUCCESS.response) {
                processSuccessfulRemovalForAnotherUser(response, seId, selectedDevice)
            } else {
                deLinkDialog(requireContext(), status)
            }
        }
    }

    /**
     * Processes the response when removal was successful.
     */
    private fun processSuccessfulRemovalForAnotherUser(
        response: DeleteDeviceResponse, seId: String, selectedDevice: String?
    ) {
        val digitizeRef = response.deleteScriptList.firstOrNull()?.digitizationReferenceNumber

        if (secureBleState == SecureBLEState.GHOST_PAIRED) {
            logger.debug("Old phone detected (GHOST_PAIRED) — skipping re-registration")
            return
        }

        if (response.deleteScriptList.isEmpty() || shouldSkipDeleteScriptsForDisconnectedBle(seId)) {
            logger.debug("deleteScriptList empty or BLE disconnected, skipping per-script execution")
            val message =
                getString(R.string.device_delinked_successfully, selectedDevice)
            deLinkDialog(requireContext(), message)
            handlePostDelinkTasks(seId, digitizeRef, selectedDevice)

            val wearableModelIdHex = StorageRepository.readString(PreferenceKey.WEARABLE_MODEL_ID)
            registerDeviceApiCall(
                selectedDevice.toString(),
                seId,
                wearableModelIdHex
            )
        } else {
            executeScriptsOrShowPermissionError(response, seId, selectedDevice)
        }
    }

    /**
     * Executes delete scripts if permissions are available, otherwise shows error.
     */
    private fun executeScriptsOrShowPermissionError(
        response: DeleteDeviceResponse, seId: String, selectedDevice: String?
    ) {
        if (hasBluetoothPermissions()) {
            try {
                executeDeleteScriptsSequentially(
                    response.deleteScriptList, seId, selectedDevice ?: "",
                    suppressPostDelinkActions = false,
                    delinkFromDifferentUser = true
                )
            } catch (e: SecurityException) {
                logger.debug("SecurityException during script execution: ${e.message}")
                showLoading(false)
                deLinkDialog(
                    requireContext(), "Permission denied. Please grant Bluetooth permissions and try again."
                )
            }
        } else {
            logger.debug("Bluetooth permissions not available for script execution")
            showLoading(false)
            deLinkDialog(
                requireContext(), "Bluetooth permissions required. Please grant permissions and try again."
            )
        }
    }

    /**
     * Runs post de-linking tasks such as registering device API calls.
     */
    private fun handlePostDelinkTasks(seId: String, digitizeRef: String?, selectedDevice: String?) {
        getPendingTask(seId, digitizeRef ?: "")
        registerDeviceApiCall(
            selectedDevice ?: "",
            seId,
            StorageRepository.readString(PreferenceKey.WEARABLE_MODEL_ID)
        )
    }

    /**
     * Initiates Bluetooth scanning for available devices.
     *
     * This method handles permission checks, view updates, and the Bluetooth discovery process.
     * It ensures that discovery is properly restarted and the UI reflects scanning status.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun proceedWithScan() {
        bluetoothDevices.clear()
        clearScanEmptyStateMessage()
        binding.rvScanDevices.visibility = View.INVISIBLE
        binding.scanLoader.clProgressBar.visibility = View.VISIBLE
        if (::deviceAdapter.isInitialized) {
            deviceAdapter.submitList(emptyList())
        }

        isLoading = true
        if (!checkAndRequestBluetoothPermissions()) return

        cancelOngoingDiscovery()
        updateUiForScanning()
        handler.postDelayed({
            requireActivity().runOnUiThread {
                prepareForNewScan()
                startDiscovery()
                logger.debug("Scan started immediately")
            }
        }, 100) // Minimal delay for UI smoothness
    }

    /**
     * Checks if the app has the required Bluetooth permissions.
     * If not, it requests them and stops the scanning process.
     *
     * @return true if permissions are granted, false otherwise
     */
    private fun checkAndRequestBluetoothPermissions(): Boolean {
        if (!hasBluetoothPermissions()) {
            logger.info("BluetoothScan Bluetooth scan permission not granted")
            showToast(BLUETOOTH_SCAN_PERMISSION_REQUIRED)
            requestBluetoothPermissions()
            showLoading(false)
            return false
        }
        return true
    }

    /**
     * Cancels any ongoing Bluetooth discovery, if running and permitted.
     */
    private fun cancelOngoingDiscovery() {
        dismissFragmentScanOverlay()
        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (bluetoothAdapter?.isDiscovering == true) {
                try {
                    if (hasBluetoothPermissions()) {
                        bluetoothAdapter.cancelDiscovery()
                    }
                } catch (e: SecurityException) {
                    logger.debug("SecurityException canceling discovery: ${e.message}")
                    showToast(BLUETOOTH_PERMISSION)
                    requestBluetoothPermissions()
                    showLoading(false)
                }
            }
        } else {
            logger.debug("BLUETOOTH_SCAN permission not granted — cannot cancel discovery")
            requestBluetoothPermissions()
        }
    }

    /**
     * Hides the fragment BLE scan overlay (spinner + "Scanning…" / BT hint), clears pending
     * scan-loader callbacks, and dismisses the activity-level scanner overlay if shown.
     * (Standalone `view.isGone` without assignment is a no-op; this used to leave the overlay stuck after back.)
     */
    private fun dismissFragmentScanOverlay() {
        noDeviceCheckpointRunnable?.let { handler.removeCallbacks(it) }
        noDeviceCheckpointRunnable = null
        scanLoaderSessionToken++
        if (!::binding.isInitialized) return
        binding.scanLoader.clProgressBar.isGone = true
        binding.scanLoader.tvBt.visibility = View.GONE
        if (::activity.isInitialized) {
            activity.showScannerLoading(false)
        }
    }

    /**
     * Updates the UI to show that scanning is in progress.
     */
    private fun updateUiForScanning() {
        requireActivity().runOnUiThread {
            binding.tvNoDevice.visibility = View.GONE
            binding.tvTitle.text = getString(R.string.scan_devices)
            binding.scanLoader.tvBt.visibility = View.VISIBLE
            binding.tvCancel.visibility = View.VISIBLE
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            activity.refreshHostExitButton()
            binding.swipeRefreshDevices.visibility = View.GONE
            binding.swipeRefreshScan.visibility = View.VISIBLE
            logger.debug("Loader shown for scan")
        }
    }

    private fun beginScanSectionLoading() {
        scanLoaderSessionToken++
        val currentSession = scanLoaderSessionToken
        scanLoaderStartTimeMs = SystemClock.elapsedRealtime()
        binding.scanLoader.clProgressBar.visibility = View.VISIBLE
        binding.scanLoader.tvBt.visibility = View.VISIBLE
        binding.rvScanDevices.visibility = View.INVISIBLE
        clearScanEmptyStateMessage()
        applyScanBackgroundBlur(true)
        noDeviceCheckpointRunnable?.let { handler.removeCallbacks(it) }
        noDeviceCheckpointRunnable = Runnable {
            if (!isAdded || currentSession != scanLoaderSessionToken || bluetoothDevices.isNotEmpty()) return@Runnable
            logger.info(
                "No SECORA device after ${scanNoDeviceCheckpointMs}ms; keep scanning for remaining timeout window"
            )
            binding.scanLoader.clProgressBar.visibility = View.VISIBLE
        }
        handler.postDelayed(noDeviceCheckpointRunnable!!, scanNoDeviceCheckpointMs)
    }

    private fun hideScanLoaderWithMinimumDuration() {
        val currentSession = scanLoaderSessionToken
        val elapsed = SystemClock.elapsedRealtime() - scanLoaderStartTimeMs
        val remaining = (scanLoaderMinVisibleMs - elapsed).coerceAtLeast(0L)
        handler.postDelayed({
            if (!isAdded || currentSession != scanLoaderSessionToken) return@postDelayed
            binding.scanLoader.clProgressBar.visibility = View.GONE
            binding.rvScanDevices.visibility = View.VISIBLE
            clearScanEmptyStateMessage()
            applyScanBackgroundBlur(false)
        }, remaining)
    }

    private fun showNoDeviceFoundAfterTimeout() {
        if (!isAdded) return
        binding.scanLoader.clProgressBar.visibility = View.GONE
        binding.rvScanDevices.visibility = View.VISIBLE
        applyScanBackgroundBlur(false)
        binding.swipeRefreshScan.isRefreshing = false
        binding.swipeRefreshScan.isEnabled = true
        showScanEmptyStateMessage(getString(R.string.no_secora_device_found))
    }

    private fun applyScanBackgroundBlur(enabled: Boolean) {
        binding.swipeRefreshScan.setRenderEffect(
            if (enabled) RenderEffect.createBlurEffect(28f, 28f, Shader.TileMode.CLAMP) else null
        )
    }

    /**
     * Resets the UI to normal state when scanning is stopped.
     */
    private fun resetScanningUI() {
        requireActivity().runOnUiThread {
            dismissFragmentScanOverlay()
            showLoading(false)
            binding.tvCancel.visibility = View.GONE
            binding.swipeRefreshScan.visibility = View.GONE
            binding.rvScanDevices.visibility = View.VISIBLE
            clearScanEmptyStateMessage()
            applyScanBackgroundBlur(false)
            binding.customBottom.root.visibility = View.VISIBLE
            binding.customBottom.llActive.visibility = View.VISIBLE
            binding.customBottom.llPassive.visibility = View.GONE
            binding.tvTitle.text = getString(R.string.wearables)

            binding.swipeRefreshDevices.visibility = View.VISIBLE
            if (distinctDevices.isEmpty()) {
                binding.tvNoDevice.visibility = View.VISIBLE
            }

            logger.debug("Scanning UI reset to normal state")
            StorageRepository.saveBoolean(PreferenceKey.BACK_PRESSED_FLAG, false)
            activity.refreshHostExitButton()
        }
    }

    /**
     * Prepares the device list and UI before initiating a new Bluetooth scan.
     */
    private fun prepareForNewScan() {
        bluetoothList.clear()
        rebuildLinkedDeviceAddresses()
        if (hasBluetoothPermissions()) {
            try {
                removeDisconnectedDevices()
                removeLinkedDevicesFromList()
            } catch (e: SecurityException) {
                logger.debug("SecurityException in removeDisconnectedDevices: ${e.message}")
                showToast(BLUETOOTH_PERMISSION)
                requestBluetoothPermissions()
            }
        } else {
            logger.debug("BLUETOOTH_CONNECT permission not granted for removeDisconnectedDevices")
            showToast(BLUETOOTH_PERMISSION)
            requestBluetoothPermissions()
        }

        if (::deviceAdapter.isInitialized) {
            deviceAdapter.submitList(list = convertBluetoothDeviceToUiModel())
        }
    }

    /**
     * Removes linked/paired devices from the scan list so they do not appear as "available"
     * (e.g. after reconnect from BaseFragment or after power-cycling the device).
     */
    private fun removeLinkedDevicesFromList() {
        if (linkedDeviceAddresses.isEmpty()) return
        val before = bluetoothDevices.size
        bluetoothDevices.removeAll { device ->
            linkedDeviceAddresses.any { normalizeAddress(it) == normalizeAddress(device.address) }
        }
        if (bluetoothDevices.size != before) {
            logger.debug("Removed ${before - bluetoothDevices.size} linked device(s) from scan list")
        }
    }

    /**
     * Stops scanning when fragment is paused to save battery and resources.
     */
    override fun onPause() {
        super.onPause()
        if (isScanning) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                stopBleScan()
            } else {
                logger.debug("No BLUETOOTH_CONNECT permission when scanning a device")
            }
            logger.debug("Scanning stopped in onPause")
        }
    }

    /**
     * Called when the Fragment becomes visible to the user and actively running.
     *
     * Rebuilds linked-device filters and refreshes the visible device list.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onResume() {
        super.onResume()
        // Rebuild linked-device scan filter from storage
        rebuildLinkedDeviceAddresses()

        refreshDeviceList()
        if (hasBluetoothPermissions()) {
            presentHostPreconnectedDeviceIfNeeded()
        }
    }

    /**
     * Proceeds with BLE connection after any necessary cleanup.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun proceedWithBleConnection(
        device: BluetoothDevice,
        deviceName: String,
        seId: String,
    ) {
        BluetoothStateManager.connectBleDevice(requireContext(), device).thenApplyAsync { protocol ->
            bleProtocol = protocol
            normalizeAddress(device.address)?.let { connectedBleProtocols[it] = protocol }
            BluetoothStateManager.addConnectedDevice(device.address, seId)
            logger.debug("BLE connection successful for device: $deviceName")
            handler.postDelayed({
                requireActivity().runOnUiThread {
                    showLoading(false)
                    customSuccessDialog(
                        deviceName, seId, false
                    )
                    verifyAfterConnect()
                }
            }, 2500)
            null
        }.exceptionally { cause ->
            requireActivity().runOnUiThread {
                showLoading(false)
                val rootMessage = getRootCauseMessage(cause)
                if (isMissingRequiredGattUuidError(cause)) {
                    showBleCompatibilityDialog(deviceName)
                } else {
                    showToast("Failed to connect to $deviceName: $rootMessage")
                }
                logger.debug("BLE connection failed: $rootMessage")
                bleProtocol?.let { protocol ->
                    SecoraWearableSDK.getInstance().getInterface().disconnectBLEDevice(protocol)
                }
                bleProtocol = null
            }
            null
        }
    }

    /**
     * Fetches any pending card-related tasks (e.g., suspend/delete operations) from the backend.
     *
     * This method chooses between two API calls depending on whether the
     * digitization reference number is available.
     *
     * @param seId The Secure Element ID of the connected device.
     * @param digitizeRef The digitization reference number of the card. If empty or "null", an alternate API is used.
     * The name or ID of the selected Bluetooth device (optional).
     */
    private fun getPendingTask(
        seId: String,
        digitizeRef: String,
        fromRegisterPaymentSuccess: Boolean = false
    ) {
        if (!fromRegisterPaymentSuccess) {
            activity.showLoading(true, getString(R.string.text_please_wait))
        }

        val safeDigitizeRef = digitizeRef.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        val callback = if (digitizeRef.isNotEmpty()) {
            createPendingTaskCallback(seId, fromRegisterPaymentSuccess)
        } else {
            createPendingTaskCallbackScript(seId, fromRegisterPaymentSuccess)
        }
        fetchSequenceNumberFromDevice(
            onRetrieved = { currentSequenceCounter ->
                logger.debug("currentSequenceCounter : $currentSequenceCounter")
                handleGetPendingTaskFlow(seId, safeDigitizeRef, currentSequenceCounter, callback)
            },
            onFailed = {

            })
    }

    /**
     * Requests the pending task from the wallet backend using the resolved sequence counter.
     *
     * @param seId The Secure Element ID of the connected device.
     * @param safeDigitizeRef Digitization reference if present; otherwise `null` so the API uses the script-based path.
     * @param currentSequenceCounter Sequence counter string obtained from the wearable (SEI TSM flow).
     * @param callback [UiCallback] that handles loading state and pending-task response processing.
     */
    private fun handleGetPendingTaskFlow(
        seId: String,
        safeDigitizeRef: String?,
        currentSequenceCounter: String,
        callback: UiCallback
    ) {
        WalletRepository.getPendingTask(
            context = activity,
            seId = seId,
            digitizationReferenceNumber = safeDigitizeRef,
            currentSequenceCounter = currentSequenceCounter,
            uiCallback = callback
        )
    }

    /**
     * Creates a [UiCallback] instance for handling pending task API responses.
     *
     * This callback handles both success and error responses,
     * manages loading UI state and processes the response accordingly.
     *
     * @param seId The Secure Element ID of the connected device.
     * @return A configured [UiCallback] instance.
     */
    private fun createPendingTaskCallback(
        seId: String,
        fromRegisterPaymentSuccess: Boolean = false
    ): UiCallback {
        return createPendingTaskUiCallback(seId, fromRegisterPaymentSuccess)
    }

    private fun createPendingTaskUiCallback(
        seId: String,
        fromRegisterPaymentSuccess: Boolean
    ): UiCallback {
        return object : UiCallback {
            override fun <T : Any?> onSuccess(ret: T) {
                super.onSuccess(ret)
                onPendingTaskSuccess(ret, seId, fromRegisterPaymentSuccess)
            }

            override fun <T : Any?> onError(ret: T) {
                onPendingTaskError(ret, fromRegisterPaymentSuccess)
            }
        }
    }

    private fun <T : Any?> onPendingTaskSuccess(
        ret: T,
        seId: String,
        fromRegisterPaymentSuccess: Boolean
    ) {
        showPendingTaskLoadingIfNeeded(fromRegisterPaymentSuccess)
        isLoading = true
        val response = ret as? GetPendingResponse
        if (response == null || response.statusMessage.isNullOrEmpty()) {
            hidePendingTaskLoadingIfNeeded(fromRegisterPaymentSuccess)
            isLoading = false
            return
        }
        if (response.deleteScriptList.isEmpty()) {
            hidePendingTaskLoadingIfNeeded(fromRegisterPaymentSuccess)
            isLoading = false
        }
        activity.runOnUiThread {
            handlePendingTaskResponseStatus(response, seId, fromRegisterPaymentSuccess)
        }
    }

    private fun <T : Any?> onPendingTaskError(ret: T, fromRegisterPaymentSuccess: Boolean) {
        hidePendingTaskLoadingIfNeeded(fromRegisterPaymentSuccess)
        isLoading = false
        val error = ret as? String ?: ""
        handleSessionOrError(error)
    }

    private fun showPendingTaskLoadingIfNeeded(fromRegisterPaymentSuccess: Boolean) {
        if (!fromRegisterPaymentSuccess) {
            activity.showLoading(true, "")
        }
    }

    private fun hidePendingTaskLoadingIfNeeded(fromRegisterPaymentSuccess: Boolean) {
        if (!fromRegisterPaymentSuccess) {
            activity.showLoading(false, "")
        }
    }

    private fun handlePendingTaskResponseStatus(
        response: GetPendingResponse,
        seId: String,
        fromRegisterPaymentSuccess: Boolean
    ) {
        when (response.statusMessage) {
            CommonResponse.SUCCESS.response -> handlePendingTaskSuccessStatus(
                response.deleteScriptList,
                seId,
                fromRegisterPaymentSuccess
            )

            else -> {
                logger.info("Status message: ${response.statusMessage}")
                hidePendingTaskLoadingIfNeeded(fromRegisterPaymentSuccess)
                isLoading = false
            }
        }
    }

    private fun handlePendingTaskSuccessStatus(
        deleteList: List<DeleteScriptResponse>,
        seId: String,
        fromRegisterPaymentSuccess: Boolean
    ) {
        if (deleteList.isEmpty()) {
            logger.debug("deleteScriptList empty, skipping per-script acknowledge")
            logger.info("deleteList is empty:  ")
            hidePendingTaskLoadingIfNeeded(fromRegisterPaymentSuccess)
            return
        }
        try {
            executeDeleteScriptsSequentially(
                deleteList,
                seId,
                "",
                suppressPostDelinkActions = fromRegisterPaymentSuccess
            )
        } catch (e: SecurityException) {
            PendingDeleteScriptExecutionGate.end(seId)
            logger.debug("SecurityException during script execution: ${e.message}")
            if (!fromRegisterPaymentSuccess) {
                showLoading(false)
            }
        }
    }

    /**
     * Creates a [UiCallback] instance for handling pending task API responses.
     *
     * This callback handles both success and error responses,
     * manages loading UI state and processes the response accordingly.
     *
     * @param seId The Secure Element ID of the connected device.
     * @return A configured [UiCallback] instance.
     */
    private fun createPendingTaskCallbackScript(
        seId: String,
        fromRegisterPaymentSuccess: Boolean = false
    ): UiCallback {
        return createPendingTaskUiCallback(seId, fromRegisterPaymentSuccess)
    }

    /**
     * Acknowledges a pending task for a card with backend confirmation.
     *
     * @param seId Secure Element ID of the connected device.
     * @param scriptId Script identifier from backend indicating the operation to acknowledge.
     * @param digitizeRef Digitization reference of the card.
     *
     * Shows loading indicator while processing.
     * Handles success and error responses with appropriate UI updates.
     */
    private fun acknowledgePendingTask(
        seId: String,
        scriptId: Int,
        digitizeRef: String,
        suppressUiActions: Boolean = false,
        delinkFromDifferentUser: Boolean = false
    ) {
        if (!suppressUiActions) {
            activity.showLoading(true, getString(R.string.text_please_wait))
        }

        lifecycleScope.launch {
            if (activity.isFinishing) return@launch
            val sdkResult = WalletRepository.acknowledgePendingTask(
                context = activity,
                seId = seId,
                scriptId = scriptId,
                digitizeRef = digitizeRef
            )
            if (sdkResult.isSuccess) {
                sdkResult.response?.let { acknowledgeResponse ->
                    handleAcknowledgeSuccess(acknowledgeResponse, suppressUiActions, delinkFromDifferentUser)
                }
            } else {
                handleAcknowledgeError(suppressUiActions)
                handleSessionOrError(sdkResult.errorMessage)
            }
        }
    }

    /**
     * Handles success response from acknowledgePendingTask.
     * Runs on UI thread and updates UI accordingly.
     */
    private fun handleAcknowledgeSuccess(
        response: AcknowledgeResponse,
        suppressUiActions: Boolean = false,
        delinkFromDifferentUser: Boolean = false
    ) {
        if (response.statusMessage.isNullOrEmpty()) {
            if (!suppressUiActions) {
                runOnUiAndHideLoading()
            }
            return
        }

        if (suppressUiActions) {
            logger.debug("Acknowledge success with UI suppression enabled")
            return
        }

        requireActivity().runOnUiThread {
            activity.showLoading(false, "")
            when (response.statusMessage) {
                CommonResponse.SUCCESS.response -> {
                    if (!delinkFromDifferentUser) {
                        refreshDeviceListSafe()
                        logger.debug("Delink operation completed successfully, refreshing device list")
                    }
                }

                else -> {
                    logger.info("Status message: ${response.statusMessage}")
                }
            }
        }
    }

    /** Handles error response from acknowledgePendingTask by hiding loading indicator */
    private fun handleAcknowledgeError(suppressUiActions: Boolean = false) {
        if (!suppressUiActions) {
            activity.showLoading(false, "")
        }
    }

    /** Runs on UI thread and hides loading indicator */
    private fun runOnUiAndHideLoading() {
        activity.showLoading(false, "")
    }

    /** Safely refreshes the Bluetooth device list with permission and exception checks */
    private fun refreshDeviceListSafe() {
        try {
            if (hasBluetoothPermissions()) {
                refreshDeviceList()
            } else {
                logger.debug("Bluetooth permissions not available for device list refresh")
            }
        } catch (e: SecurityException) {
            logger.debug("SecurityException during device list refresh: ${e.message}")
        } catch (e: Exception) {
            logger.debug("Error refreshing device list after delink: ${e.message}")
        }
    }

    /**
     * Verifies the device after a successful BLE connection.
     *
     * - Updates logs with verification progress.
     * - With the active [bleProtocol].
     * - On success, stores the SCP11 certificate and proceeds.
     * - On failure, handles the error gracefully and disconnects if necessary.
     */
    private fun verifyAfterConnect() {
        bleLock.lock()
        try {
            if (!hasBluetoothPermissions()) {
                logger.debug("Bluetooth permissions missing, cannot verify device")
                showToast(BLUETOOTH_PERMISSION)
                requestBluetoothPermissions()
                showLoading(false)
                return
            }
        } finally {
            bleLock.unlock()
        }
    }

    /**
     * Checks whether all required Bluetooth permissions are granted.
     * Uses different permission sets depending on the Android version.
     */
    private fun hasBluetoothPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(
                requireContext(), it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Requests Bluetooth-related permissions from the user.
     * Handles version-specific permission sets for Android 12+ and below.
     */
    private fun requestBluetoothPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )

        if (isNearbyDevicesPermissionPermanentlyDenied(permissions)) {
            showNearbyDevicesPermissionSettingsDialog()
            return
        }

        StorageRepository.saveBoolean(PreferenceKey.HAS_REQUESTED_NEARBY_DEVICES_PERMISSION, true)
        ActivityCompat.requestPermissions(requireActivity(), permissions, 1001)
    }

    /**
     * Android reports `false` from shouldShowRequestPermissionRationale both before the first
     * request and after repeated denials. The stored request flag distinguishes those states.
     */
    private fun isNearbyDevicesPermissionPermanentlyDenied(permissions: Array<String>): Boolean =
        StorageRepository.readBoolean(PreferenceKey.HAS_REQUESTED_NEARBY_DEVICES_PERMISSION) &&
            permissions.any { permission ->
                ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED &&
                    !shouldShowRequestPermissionRationale(permission)
            }

    /**
     * Dialog to show the user that NearBy devices permission is required to scan and connect.
     */
    private fun showNearbyDevicesPermissionSettingsDialog() {
        statusDialog(
            requireActivity(),
            getString(R.string.nearby_devices_permission_settings_message)
        ) {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", requireContext().packageName, null)
            )
            startActivity(intent)
        }
    }

    /**
     * Displays a short Toast message on the screen.
     * Safely handles nullable message inputs.
     */
    override fun showToast(message: String?) {
        Toast.makeText(requireContext(), message ?: "", Toast.LENGTH_SHORT).show()
    }

    /**
     * Decodes a Base64-encoded JSON string into a byte array.
     * Handles nested Base64 layers and verifies JSON format.
     * Returns null if decoding fails.
     */
    private fun extractJsonBytes(scriptData: String): ByteArray? {
        return try {
            if (scriptData.isEmpty()) {
                logger.debug("scriptData is null or empty")
                return null
            }

            ScriptDataParser.decodeToJsonBytes(scriptData)?.also {
                val decodedString = String(it, Charsets.UTF_8)
                logger.debug("Decoded scriptData -> $decodedString")
            }

        } catch (e: Exception) {
            logger.noStackTraceLog("ExtractJsonBytes ", e)
            null
        }
    }

    /**
     * Checks if a device is actually available and not already connected or turned off
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun isDeviceAvailable(device: BluetoothDevice): Boolean {
        if (isHostPreconnectedDevice(device)) {
            return true
        }
        return try {
            // Check if device is already in our connected list (centralized in BluetoothStateManager)
            if (BluetoothStateManager.isDeviceConnectedByAddress(device.address)) {
                logger.debug("Device ${device.name} is already in connected list, skipping")
                return false
            }

            // Check bond state - device can be available if bonded OR if discovered during scan
            val bondState = device.bondState
            val isBonded = bondState == BluetoothDevice.BOND_BONDED
            val isBonding = bondState == BluetoothDevice.BOND_BONDING
            val isBondNone = bondState == BluetoothDevice.BOND_NONE
            logger.debug("Device ${device.name} bond state: $bondState (BOND_BONDED=${BluetoothDevice.BOND_BONDED}), isBonded: $isBonded, isBonding: $isBonding, isBondNone: $isBondNone")

            // Device is available if:
            // 1. It's bonded (already paired)
            // 2. It's discovered during scan (BOND_NONE) - we can try to connect and pair
            // 3. It's currently bonding
            if (!isBonded && !isBondNone && !isBonding) {
                logger.debug("Device ${device.name} is in unknown bond state, skipping")
                return false
            }

            // Check connection state
            val isConnected = try {
                val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val connectionState = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT)
                val connected = connectionState == BluetoothProfile.STATE_CONNECTED
                logger.debug("Device ${device.name} connection state: $connectionState (STATE_CONNECTED=${BluetoothProfile.STATE_CONNECTED}), isConnected: $connected")
                connected
            } catch (e: SecurityException) {
                logger.debug("Cannot check connection state for ${device.name}: ${e.message}")
                false
            }

            // Device is available if:
            // 1. It's bonded and not connected, OR
            // 2. It's discovered during scan (BOND_NONE) and not connected, OR
            // 3. It's currently bonding and not connected
            val isAvailable = !isConnected
            logger.debug("Device ${device.name} availability check: isBonded=$isBonded, isBondNone=$isBondNone, isBonding=$isBonding, isConnected=$isConnected, isAvailable=$isAvailable")
            isAvailable
        } catch (e: Exception) {
            logger.debug("Error checking device availability for ${device.name}: ${e.message}")
            false
        }
    }

    /**
     * Properly disconnects and removes a device from the connected list
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun disconnectAndRemoveDevice(deviceAddress: String) {
        if (PayExternalLaunch.shouldIgnoreHostAclDisconnect(deviceAddress)) {
            logger.debug("Host launch: skip disconnectAndRemoveDevice for $deviceAddress")
            return
        }
        bleLock.lock()
        logger.debug("Device disconnected: $deviceAddress")
        try {
            val key = normalizeAddress(deviceAddress)
            val protocolToDisconnect = key?.let { connectedBleProtocols[it] } ?: bleProtocol?.takeIf {
                normalizeAddress(it.bluetoothDevice.address) == key
            }
            protocolToDisconnect?.let { protocol ->
                if (BluetoothStateManager.activeProtocol === protocol) {
                    BluetoothStateManager.setActiveProtocol(null)
                }
                try {
                    SecoraWearableSDK.getInstance().getInterface().disconnectBLEDevice(protocol)
                    logger.debug("Disconnected BLE protocol for device: $deviceAddress")
                } catch (e: Exception) {
                    logger.debug("Error disconnecting BLE protocol: ${e.message}")
                }
                if (key != null) connectedBleProtocols.remove(key)
                else connectedBleProtocols.entries.removeIf { it.value == protocol }
            } ?: disconnectActiveProtocolForAddress(deviceAddress)
            if (bleProtocol?.bluetoothDevice?.address?.let { normalizeAddress(it) == key } == true) {
                bleProtocol = connectedBleProtocols.values.firstOrNull()
            }

            // Remove only this device from connected set (others stay green)
            BluetoothStateManager.removeConnectedDevice(deviceAddress)

            // Remove from bluetooth devices list
            val iterator = bluetoothDevices.iterator()
            while (iterator.hasNext()) {
                val device = iterator.next()
                if (device.address == deviceAddress) {
                    logger.debug("Removing device from scan list: ${device.name}")
                    iterator.remove()
                    break
                }
            }

            // Update UI
            if (::deviceAdapter.isInitialized) {
                deviceAdapter.submitList(list = convertBluetoothDeviceToUiModel())
            }

            // Refresh paired devices list after a short post so BluetoothStateManager state is final
            // (global BluetoothStateReceiver may run after this); then BLE icon (green/black) is correct per device.
            handler.post {
                binding.rvDevices.adapter?.notifyDataSetChanged()
            }
        } finally {
            bleLock.unlock()
        }
    }

    /**
     * Refreshes the device list by checking bonded devices and updating UI
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun refreshDeviceList() {
        removeDisconnectedDevices()
        checkBondedDevices()
        if (::deviceAdapter.isInitialized) {
            deviceAdapter.submitList(list = convertBluetoothDeviceToUiModel())
        }
    }

    /**
     * Removes disconnected devices from the scan list
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun removeDisconnectedDevices() {
        logger.debug("removeDisconnectedDevices called - checking ${bluetoothDevices.size} devices")
        val iterator = bluetoothDevices.iterator()
        while (iterator.hasNext()) {
            val device = iterator.next()
            if (isHostPreconnectedDevice(device)) {
                logger.debug(
                    "Host preconnected device kept in scan list: ${resolveHostDeviceDisplayName(device)} (${device.address})"
                )
                continue
            }
            if (device.bondState == BluetoothDevice.BOND_BONDED) {
                logger.debug("Bonded device kept in scan list: ${getSafeDeviceName(device)} (${device.address})")
                continue
            }
            val deviceName = getSafeDeviceName(device)
            logger.debug("Checking device availability: $deviceName (${device.address})")
            if (!isDeviceAvailable(device)) {
                logger.debug("Removing disconnected device: $deviceName")
                iterator.remove()
                BluetoothStateManager.removeConnectedDevice(device.address)
            } else {
                logger.debug("Device $deviceName is available, keeping in list")
            }
        }
        logger.debug("After removal - ${bluetoothDevices.size} devices remain")
        if (::deviceAdapter.isInitialized) {
            deviceAdapter.submitList(list = convertBluetoothDeviceToUiModel())
        }
    }

    /**
     * Checks bonded devices and adds available ones to the scan list
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun checkBondedDevices() {
        try {
            val bondedDevices = bluetoothAdapter.bondedDevices
            logger.debug("Found ${bondedDevices.size} bonded devices")
            bondedDevices.forEach { processBondedSecoraDeviceIfEligible(it) }
        } catch (e: Exception) {
            logger.debug("Error checking bonded devices: ${e.message}")
        }
    }

    private fun processBondedSecoraDeviceIfEligible(device: BluetoothDevice) {
        val deviceName = getSafeDeviceName(device)
        if (!isSecoraDevice(deviceName)) return

        logger.debug("Checking bonded SECORA device: $deviceName (${device.address})")
        if (linkedDeviceAddresses.any { normalizeAddress(it) == normalizeAddress(device.address) }) {
            logger.debug("Skipping linked bonded device: ${device.address}")
            return
        }

        if (bluetoothDevices.any { it.address == device.address }) {
            logger.debug("Bonded device $deviceName already in scan list")
            return
        }

        bluetoothDevices.add(device)
        logger.debug(
            "Added bonded device to scan list: $deviceName " +
                "(bonded=${isBondedDevice(device)}, connected=${isConnectedScanDevice(device)})"
        )
    }

    /**
     * Extracts scriptData from a script item using reflection
     * @param scriptItem The script item object
     * @return The scriptData string or null if not found
     */
    private fun getScriptDataFromItem(scriptItem: Any): String? {
        return when (scriptItem) {
            is DeleteScriptList -> scriptItem.scriptData
            is DeleteScriptResponse -> scriptItem.scriptData
            is DeleteScript -> scriptItem.scriptData
            is ScriptItem -> scriptItem.scriptData
            else -> null
        }
    }

    /**
     * Executes delete scripts sequentially (one after another)
     * @param deleteScriptList List of scripts to execute
     * @param seId The Secure Element ID
     * @param selectedDevice The selected device name
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun executeDeleteScriptsSequentially(
        deleteScriptList: List<Any>,
        seId: String,
        selectedDevice: String,
        suppressPostDelinkActions: Boolean = false,
        delinkFromDifferentUser: Boolean = false,
    ) {
        if (deleteScriptList.isEmpty()) {
            logger.debug("No scripts to execute")
            return
        }

        if (!PendingDeleteScriptExecutionGate.tryBegin(seId)) {
            logger.debug(
                "Pending delete scripts already running for seId=$seId; skipping duplicate execution"
            )
            if (!suppressPostDelinkActions) {
                showLoading(false)
            }
            return
        }

        logger.debug("Starting sequential execution of ${deleteScriptList.size} scripts")

        if (!suppressPostDelinkActions) {
            activity.showLoading(true, getString(R.string.text_deleting_card))
        }

        // Log each script item for debugging
        deleteScriptList.forEachIndexed { index, scriptItem ->
            val scriptData = getScriptDataFromItem(scriptItem)
            logger.debug("Script ${index + 1}: scriptData length = ${scriptData?.length ?: 0}")
        }

        // Track script results
        val scriptResults = mutableListOf<Boolean>()
        // Start with the first script
        val delinkOptions = DelinkOptions(
            suppressPostDelinkActions = suppressPostDelinkActions,
            delinkFromDifferentUser = delinkFromDifferentUser
        )
        executeNextScript(
            deleteScriptList,
            0,
            seId,
            selectedDevice,
            0,
            scriptResults,
            delinkOptions
        )
    }

    /**
     * Executes a list of scripts sequentially with retry logic and BLE connection management.
     *
     * @param deleteScriptList List of scripts to execute.
     * @param currentIndex Current script index to execute.
     * @param seId Secure Element ID.
     * @param selectedDevice Selected device name.
     * @param retryCount Retry attempt count for the current script (default is 0).
     * @param scriptResults Mutable list tracking success/failure of scripts.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun executeNextScript(
        deleteScriptList: List<Any>,
        currentIndex: Int,
        seId: String,
        selectedDevice: String,
        retryCount: Int = 0,
        scriptResults: MutableList<Boolean>,
        delinkOptions: DelinkOptions
    ) {
        // Check if all scripts processed
        if (currentIndex >= deleteScriptList.size) {
            finalizeScriptExecution(
                scriptResults,
                seId,
                selectedDevice,
                delinkOptions.suppressPostDelinkActions,
                delinkOptions.delinkFromDifferentUser
            )
            return
        }

        val scriptItem = deleteScriptList[currentIndex]
        val attemptText = if (retryCount == 0) Constants.FIRST_ATTEMPT else Constants.RETRY_ATTEMPT
        logger.debug("Executing script ${currentIndex + 1}/${deleteScriptList.size} ($attemptText)")

        if (!isNFC()) {
            performBleCleanup(currentIndex) {
                val context = ScriptExecutionContext(
                    deleteScriptList = deleteScriptList,
                    currentIndex = currentIndex,
                    seId = seId,
                    selectedDevice = selectedDevice,
                    retryCount = retryCount,
                    scriptResults = scriptResults,
                    suppressPostDelinkActions = delinkOptions.suppressPostDelinkActions,
                    delinkFromDifferentUser = delinkOptions.delinkFromDifferentUser
                )
                executeScriptAfterCleanup(
                    scriptItem,
                    context
                )
            }
        } else
            executeScriptAfterCleanupNfc(
                scriptItem,
                ScriptExecutionContext(
                    deleteScriptList = deleteScriptList,
                    currentIndex = currentIndex,
                    seId = seId,
                    selectedDevice = selectedDevice,
                    retryCount = retryCount,
                    scriptResults = scriptResults,
                    suppressPostDelinkActions = delinkOptions.suppressPostDelinkActions,
                    delinkFromDifferentUser = delinkOptions.delinkFromDifferentUser
                )
            )
    }

    /**
     * Continues script execution after BLE cleanup is complete.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun executeScriptAfterCleanup(
        scriptItem: Any,
        context: ScriptExecutionContext
    ) {
        val scriptData = getScriptDataFromItem(scriptItem)
        if (scriptData == null) {
            handleScriptFailureAndContinue(context)
            return
        }

        val attemptText =
            if (context.retryCount == 0) Constants.FIRST_ATTEMPT else Constants.RETRY_ATTEMPT
        logger.debug("Script ${context.currentIndex + 1}: Processing scriptData (${scriptData.length} chars) - $attemptText")
        logger.debug("Script ${context.currentIndex + 1}: scriptData preview = ${scriptData.take(50)}...")

        val jsonBytes = extractJsonBytes(scriptData)
        if (jsonBytes == null) {
            handleScriptFailureAndContinue(context)
            return
        }

        StorageRepository.saveString(PreferenceKey.INSTALL_SCRIPT, jsonBytes.contentToString())
        val scriptHandler = createScriptHandler(context.suppressPostDelinkActions)

        scriptHandler.deleteScript(jsonBytes, clearDefaultCard = true).thenAccept { success ->
            StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)

            if (success) {
                handleScriptSuccess(context)
            } else {
                handleRetryOrFailure(context)
            }
        }.exceptionally { throwable ->
            StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)
            handleException(throwable, context)
            null
        }
    }

    /**
     * Finalize after all scripts completed:
     * - Show dialog with results
     * - Disconnect BLE safely
     * - Refresh device list
     * - Update pending task
     */
    private fun finalizeScriptExecution(
        scriptResults: List<Boolean>,
        seId: String,
        selectedDevice: String,
        suppressPostDelinkActions: Boolean = false,
        delinkFromDifferentUser: Boolean = false
    ) {
        PendingDeleteScriptExecutionGate.end(seId)
        logger.debug("All scripts completed. Results: $scriptResults $seId")
        if (!suppressPostDelinkActions) {
            activity.showLoading(true, "")
        }

        val successfulScripts = scriptResults.count { it }
        val totalScripts = scriptResults.size
        val delinkSucceeded = successfulScripts == totalScripts
        val message = if (successfulScripts == totalScripts) {
            getString(R.string.device_delinked_successfully, selectedDevice)
        } else {
            "Script execution completed. $successfulScripts out of $totalScripts scripts succeeded. Some scripts were not able to delete."
        }

        if (!suppressPostDelinkActions) {
            deLinkDialog(requireContext(), message)
        }

        if (!delinkFromDifferentUser && delinkSucceeded && !suppressPostDelinkActions) {
            val addressForLinkedRemoval = getBluetoothAddressForSeId(seId)
            if (addressForLinkedRemoval.isNotEmpty()) {
                linkedDeviceAddresses.remove(addressForLinkedRemoval)
            }
            completeManualDelinkBleCleanup(seId)
        } else if (!delinkFromDifferentUser && !suppressPostDelinkActions) {
            val address = getBluetoothAddressForSeId(seId)
            BluetoothStateManager.removeConnectedDevice(address)
            safelyDisconnectBLE(seId, removeBond = true)
        }

        if (delinkFromDifferentUser) {
            registerDeviceApiCall(
                device = selectedDevice,
                seId = seId,
                wearableModelIdHex = StorageRepository.readString(PreferenceKey.WEARABLE_MODEL_ID),
                delinkFromDifferentUser = delinkFromDifferentUser
            )
        } else {
            safelyRefreshDeviceList()
        }

    }

    /** Performs BLE cleanup with error handling before each script execution */
    private fun performBleCleanup(currentIndex: Int, onComplete: () -> Unit) {
        try {
            logger.debug("Preparing BLE state before script ${currentIndex + 1}")
            // IMPORTANT:
            // Do NOT disconnect here. Disconnecting triggers ACTION_ACL_DISCONNECTED and the
            // `BluetoothStateReceiver` log/state update, which makes the UI show "disconnected"
            // *before* delete scripts are executed. We only disconnect after all scripts finish
            // in `finalizeScriptExecution()`.
            handler.postDelayed({ onComplete() }, 1000)
        } catch (e: Exception) {
            logger.debug("Error during BLE cleanup before script ${currentIndex + 1}: ${e.message}")
            onComplete() // Continue even if cleanup fails
        }
    }

    /** Handles a failed script extraction or JSON parse, logs failure and continues */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleScriptFailureAndContinue(
        context: ScriptExecutionContext
    ) {
        logger.debug("Failed to extract script data or JSON for script ${context.currentIndex + 1}, marking as failed and continuing")
        context.scriptResults.add(false)
        val delinkOptions = DelinkOptions(
            suppressPostDelinkActions = context.suppressPostDelinkActions,
            delinkFromDifferentUser = context.delinkFromDifferentUser
        )
        executeNextScript(
            context.deleteScriptList,
            context.currentIndex + 1,
            context.seId,
            context.selectedDevice,
            0,
            context.scriptResults,
            delinkOptions
        )
    }

    /** Creates and returns a new ScriptHandler instance */
    private fun createScriptHandler(suppressPostDelinkActions: Boolean = false): ScriptHandler {
        return ScriptHandler(
            requireContext(), object : ScriptHandler.Callbacks {
                override fun showLoading(show: Boolean, msg: String) {
                    if (!suppressPostDelinkActions) {
                        this@AvailableDeviceFragment.showLoading(show)
                    }
                }

                override fun showToast(message: String) {
                    if (!suppressPostDelinkActions) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun updateLogs(message: String) {
                    logger.debug("ScriptHandler$message")
                }
            })
    }

    /**
     * Acknowledges the successful execution of a specific delete script
     * @param scriptItem The script item that was successfully executed
     * @param seId The Secure Element ID
     */
    private fun acknowledgeScriptSuccess(
        scriptItem: Any,
        seId: String,
        suppressUiActions: Boolean = false,
        delinkFromDifferentUser: Boolean = false
    ) {
        try {
            val scriptId = getScriptIdFromItem(scriptItem)
            val digitizationRef = getDigitizationRefFromItem(scriptItem)

            if (scriptId != null && digitizationRef != null) {
                logger.debug("Acknowledging successful script execution: scriptId=$scriptId, digitizationRef=$digitizationRef")
                acknowledgePendingTask(
                    seId = seId,
                    scriptId = scriptId,
                    digitizeRef = digitizationRef,
                    suppressUiActions = suppressUiActions,
                    delinkFromDifferentUser = delinkFromDifferentUser
                )
            } else {
                logger.debug("Could not extract scriptId or digitizationRef from script item")
            }
        } catch (e: Exception) {
            logger.debug("Error acknowledging script success: ${e.message}")
        }
    }

    /**
     * Extracts scriptId from a script item using reflection
     * @param scriptItem The script item object
     * @return The scriptId or null if not found
     */
    private fun getScriptIdFromItem(scriptItem: Any): Int? {
        return when (scriptItem) {
            is DeleteScriptList -> scriptItem.scriptId
            is ScriptExecutionResults -> scriptItem.scriptId
            is DeleteScriptResponse -> scriptItem.scriptId
            is DeleteScript -> scriptItem.scriptId
            else -> null
        }
    }

    /**
     * Extracts digitizationReferenceNumber from a script item using reflection
     * @param scriptItem The script item object
     * @return The digitizationReferenceNumber or null if not found
     */
    private fun getDigitizationRefFromItem(scriptItem: Any): String? {
        return when (scriptItem) {
            is DeleteScriptList -> scriptItem.digitizationReferenceNumber
            is ScriptExecutionResults -> scriptItem.digitizationReferenceNumber
            is DeleteScriptResponse -> scriptItem.digitizationReferenceNumber
            is DeleteScript -> scriptItem.digitizationReferenceNumber
            else -> null
        }
    }

    /** Handles script success: records result, updates task, proceeds to next script */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleScriptSuccess(
        context: ScriptExecutionContext
    ) {
        logger.debug("Script ${context.currentIndex + 1} executed successfully")
        context.scriptResults.add(true)

        val scriptItem = context.deleteScriptList[context.currentIndex]
        acknowledgeScriptSuccess(
            scriptItem,
            context.seId,
            suppressUiActions = context.suppressPostDelinkActions,
            delinkFromDifferentUser = context.delinkFromDifferentUser
        )
        val delinkOptions = DelinkOptions(
            suppressPostDelinkActions = context.suppressPostDelinkActions,
            delinkFromDifferentUser = context.delinkFromDifferentUser
        )
        executeNextScript(
            context.deleteScriptList,
            context.currentIndex + 1,
            context.seId,
            context.selectedDevice,
            0,
            context.scriptResults,
            delinkOptions
        )
    }

    /**
     * Handles retries or failure after a script execution attempt
     * Retries once if allowed, else marks failure and continues.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleRetryOrFailure(
        context: ScriptExecutionContext
    ) {
        if (context.retryCount == 0) {
            logger.debug("Script ${context.currentIndex + 1} failed on first attempt, retrying...")
            val delinkOptions = DelinkOptions(
                suppressPostDelinkActions = context.suppressPostDelinkActions,
                delinkFromDifferentUser = context.delinkFromDifferentUser
            )
            executeNextScript(
                context.deleteScriptList,
                context.currentIndex,
                context.seId,
                context.selectedDevice,
                1,
                context.scriptResults,
                delinkOptions
            )
        } else {
            logger.debug("Script ${context.currentIndex + 1} failed on retry attempt or no retries left, marking as failed and continuing")
            context.scriptResults.add(false)
            val delinkOptions = DelinkOptions(
                suppressPostDelinkActions = context.suppressPostDelinkActions,
                delinkFromDifferentUser = context.delinkFromDifferentUser
            )
            executeNextScript(
                context.deleteScriptList,
                context.currentIndex + 1,
                context.seId,
                context.selectedDevice,
                0,
                context.scriptResults,
                delinkOptions
            )
        }
    }

    /**
     * Handles exceptions thrown during script execution:
     * - Cleans up BLE if specific connection errors occur
     * - Retries once if possible, else records failure and continues
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleException(
        throwable: Throwable,
        context: ScriptExecutionContext
    ) {
        logger.debug("Script ${context.currentIndex + 1} execution failed with exception: ${throwable.message} (attempt ${context.retryCount + 1})")

        val errorMessage = throwable.message?.lowercase() ?: ""
        val isBleConnectionError =
            errorMessage.contains("too many register gatt interface") || errorMessage.contains("connection") || errorMessage.contains(
                "timeout"
            ) || errorMessage.contains("status=133")

        if (isBleConnectionError) {
            logger.debug("BLE connection error detected for script ${context.currentIndex + 1}, performing cleanup")
            try {
                bleProtocol?.let { disconnectProtocolAndRemoveFromState(it) }
                bleProtocol = null
            } catch (e: SecurityException) {
                logger.error("SecurityException during BLE cleanup: ${e.message}")
            } catch (e: IllegalStateException) {
                logger.error("IllegalStateException during BLE cleanup: ${e.message}")
            } catch (e: Exception) {
                logger.error("Unexpected error during BLE cleanup: ${e.message}")
                bleProtocol?.let { disconnectProtocolAndRemoveFromState(it) }
                bleProtocol = null
            }
            // Non-blocking delay for BLE cleanup, then continue with retry logic
            handler.postDelayed({
                continueAfterExceptionCleanup(context)
            }, 2000)
            return
        }

        continueAfterExceptionCleanup(context)
    }

    /**
     * Continues execution after exception cleanup delay.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun continueAfterExceptionCleanup(
        context: ScriptExecutionContext
    ) {
        val delinkOptions = DelinkOptions(
            suppressPostDelinkActions = context.suppressPostDelinkActions,
            delinkFromDifferentUser = context.delinkFromDifferentUser
        )
        if (context.retryCount == 0) {
            logger.debug("Script ${context.currentIndex + 1} failed with exception on first attempt, retrying...")
            executeNextScript(
                context.deleteScriptList,
                context.currentIndex,
                context.seId,
                context.selectedDevice,
                1,
                context.scriptResults,
                delinkOptions
            )
        } else {
            logger.debug("Script ${context.currentIndex + 1} failed with exception on retry attempt, marking as failed and continuing")
            context.scriptResults.add(false)
            executeNextScript(
                context.deleteScriptList,
                context.currentIndex + 1,
                context.seId,
                context.selectedDevice,
                0,
                context.scriptResults,
                delinkOptions
            )
        }
    }

    /** Refresh device list with permission check and error handling */
    private fun safelyRefreshDeviceList() {
        try {
            logger.debug("Refreshing device list")
            if (hasBluetoothPermissions()) {
                refreshDeviceList()
            } else {
                logger.debug("Bluetooth permissions not available for device list refresh")
            }
        } catch (e: SecurityException) {
            logger.debug("SecurityException during device list refresh: ${e.message}")
        } catch (e: Exception) {
            logger.debug("Error refreshing device list: ${e.message}")
        }
    }

    /**
     * Executes the given action on a background (IO) thread.
     */
    private fun background(action: suspend () -> Unit) {
        lifecycleScope.launch { action() }
    }

    /**
     * Executes the given action on the main (UI) thread.
     */
    private fun onUiThread(action: () -> Unit) {
        lifecycleScope.launch(AppDispatchers.MAIN) { action() }
    }

    /**
     * Updates the current Secure BLE state and logs the state transition.
     *
     * This method centralizes SecureBLEState changes to ensure
     * consistent logging and controlled state mutation.
     *
     * @param state The new SecureBLEState to transition to.
     */
    private fun setSecureState(state: SecureBLEState) {
        logger.debug("SecureBLEState: $secureBleState → $state")
        secureBleState = state
    }

    private fun clearGhostPairing(seId: String?) {
        if (seId.isNullOrEmpty()) return
        val address = getBluetoothAddressForSeId(seId)
        if (address.isNotEmpty() && hasBluetoothPermissions()) {
            try {
                val device = bluetoothAdapter.getRemoteDevice(address)
                SecoraWearableSDK.getInstance().getInterface().removeBond(device)
                logger.debug("GHOST_PAIRING_CLEARED and bond removed for seId=$seId")
            } catch (e: SecurityException) {
                logger.debug("SecurityException removing bond: ${e.message}")
            }
        }
        removePairedDevice(seId)
        logger.debug("GHOST_PAIRING_CLEARED seId=$seId")
    }

    /**
     * Returns all paired SE IDs stored locally.
     */
    private fun getPairedSeIds(): Set<String> {
        val raw = StorageRepository.readString(PreferenceKey.PAIRED_SE_IDS)

        if (raw.isNullOrBlank()) return emptySet()

        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    /**
     * Adds a newly paired device (SEID + BLE address).
     */
    private fun addPairedDevice(seId: String, deviceAddress: String) {
        val set = getPairedSeIds().toMutableSet()
        set.add(seId)

        StorageRepository.saveString(PreferenceKey.PAIRED_SE_IDS, set.joinToString(","))
        StorageRepository.saveString(PreferenceKey.bleAddressKey(seId), deviceAddress)
        logger.info("PAIRED_DEVICE_ADDED seId=$seId address=$deviceAddress")
    }

    /**
     * Removes a paired device completely.
     */
    private fun removePairedDevice(seId: String) {
        val set = getPairedSeIds().toMutableSet()
        set.remove(seId)

        StorageRepository.saveString(PreferenceKey.PAIRED_SE_IDS, set.joinToString(","))
        StorageRepository.clearString(PreferenceKey.bleAddressKey(seId))
        logger.info("PAIRED_DEVICE_REMOVED seId=$seId")
    }

    private fun restorePairedDevices() {
        val paired = getPairedSeIds()
        logger.info("Restored paired devices: $paired")
    }

    private fun getBluetoothAddressForSeId(seId: String): String {
        return StorageRepository.readString(key = PreferenceKey.bleAddressKey(seId))
    }

    /** Removes the device address from BluetoothStateManager. Catches SecurityException and logs. */
    private fun removeFromBluetoothStateSafely(address: String?) {
        if (address.isNullOrBlank()) return
        try {
            BluetoothStateManager.removeConnectedDevice(address)
        } catch (e: SecurityException) {
            logger.debug("SecurityException removing from BluetoothStateManager: ${e.message}")
        }
    }

    /** Disconnects the BLE protocol and removes its address from BluetoothStateManager. Logs any exception. */
    private fun disconnectProtocolAndRemoveFromState(protocol: BleProtocol) {
        removeFromBluetoothStateSafely(protocol.bluetoothDevice.address ?: "")
        if (BluetoothStateManager.activeProtocol === protocol) {
            BluetoothStateManager.setActiveProtocol(null)
        }
        try {
            SecoraWearableSDK.getInstance().getInterface().disconnectBLEDevice(protocol)
        } catch (e: Exception) {
            logger.debug("Exception disconnecting BLE device: ${e.message}")
        }
    }

    private fun rebuildLinkedDeviceAddresses() {
        if (linkedDeviceAddresses.isNotEmpty()) {
            logger.debug("Keeping existing linkedDeviceAddresses (server-driven) = $linkedDeviceAddresses")
            return
        }

        val pairedSeIds = getPairedSeIds()
        pairedSeIds.forEach { seId ->
            val address = getBluetoothAddressForSeId(seId)
            if (address.isNotEmpty()) {
                linkedDeviceAddresses.add(address)
            }
        }

        logger.debug("Rebuilt linkedDeviceAddresses = $linkedDeviceAddresses")
    }

    override fun onSeidDetected(
        seid: String?,
        tagId: String?,
        icTypeHex: String?,
        oemIdHex: String?,
        seGroupIdHex: String?,
        wearableModelIdHex: String?
    ) {
        persistCplcOemData(icTypeHex, oemIdHex, seGroupIdHex)
        val (normalizedSeId, normalizedTagId) =
            handleNfcCplcCallback(seid, tagId, icTypeHex, oemIdHex, seGroupIdHex, wearableModelIdHex)

        this.tagId = normalizedTagId

        // Hide BottomSheet
        hideNfcSheet()
        NfcScriptExecutionTracker.onNfcScriptFinished()
        if (normalizedSeId.isNotBlank()) {
            registerDeviceApiCall(
                Constants.NFC_DEVICE_MODEL + " " + normalizedTagId,
                normalizedSeId,
                StorageRepository.readString(PreferenceKey.WEARABLE_MODEL_ID)
            )
        }
    }

    override fun onApduProgress(apdu: String, response: String) {
        logger.debug("APDU REQUEST=$apdu \n RESPONSE=$response")
    }

    override fun onSuccess(responseItems: MutableList<ApduResponsesItem>?, completed: Boolean) {
        logger.debug("NFC Execution completed")
    }

    override fun onError(error: String) {
        NfcScriptExecutionTracker.onNfcScriptFinished()
        hideNfcSheet()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun executeScriptAfterCleanupNfc(
        scriptItem: Any,
        context: ScriptExecutionContext
    ) {
        val scriptData = getScriptDataFromItem(scriptItem)
        if (scriptData == null) {
            handleScriptFailureAndContinue(context)
            return
        }
        val attemptText =
            if (context.retryCount == 0) Constants.FIRST_ATTEMPT else Constants.RETRY_ATTEMPT
        logger.debug("Script ${context.currentIndex + 1}: Processing scriptData (${scriptData.length} chars) - $attemptText")
        logger.debug("Script ${context.currentIndex + 1}: scriptData preview = ${scriptData.take(50)}...")

        val jsonBytes = extractJsonBytes(scriptData)
        if (jsonBytes == null) {
            handleScriptFailureAndContinue(context)
            return
        }

        StorageRepository.saveString(PreferenceKey.INSTALL_SCRIPT, jsonBytes.contentToString())

        showNfcSheet(parentFragmentManager)
        NfcScriptExecutionTracker.onNfcScriptStarted()
        SecoraWearableSDK.getInstance().getInterface().executeNfcOperation(
            requireActivity(),
            DELETE_SCRIPT_CLEAR_DEFAULT,
            jsonBytes,
            null,
            null,
            object : ScriptExecutionCallback {
                override fun onSeidDetected(
                    seid: String?,
                    tagId: String?,
                    icTypeHex: String?,
                    oemIdHex: String?,
                    seGroupIdHex: String?,
                    wearableModelIdHex: String?
                ) {
                    persistCplcOemData(icTypeHex, oemIdHex, seGroupIdHex)
                    handleNfcCplcCallback(seid, tagId, icTypeHex, oemIdHex, seGroupIdHex, wearableModelIdHex)
                }

                override fun onApduProgress(request: String?, response: String?) {
                    logger.debug("APDU REQUEST=$request \n RESPONSE=$response")
                }

                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onSuccess(
                    responseItems: MutableList<ApduResponsesItem>?,
                    completed: Boolean
                ) {
                    if (!completed) {
                        logger.debug("Cleanup NFC progress callback received, waiting for completion")
                        return
                    }
                    NfcScriptExecutionTracker.onNfcScriptFinished()
                    StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)
                    hideNfcSheet()
                    handleScriptSuccess(context)
                }

                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onError(error: String) {
                    logger.debug("onError: $error")
                    NfcScriptExecutionTracker.onNfcScriptFinished()
                    handleRetryOrFailure(context)
                }
            })
        showLoading(false)
    }

    private fun scheduleAdapterUpdate() {
        if (isUpdateScheduled) return

        isUpdateScheduled = true
        val start = SystemClock.elapsedRealtime()

        scanUpdateHandler.postDelayed({
            publishSortedScanDeviceList()

            val duration = SystemClock.elapsedRealtime() - start
            if (duration > 100) {
                logger.info("UI_UPDATE_SLOW → ${duration}ms for ${bluetoothDevices.size} devices")
            }

            isUpdateScheduled = false
        }, 500)
    }

    private fun persistCplcOemData(icTypeHex: String?, oemIdHex: String?, seGroupIdHex: String?) {
        ConfiguredWalletIdentity.persistForRegistration(
            requireContext().applicationContext,
            fetchedIcTypeHex = icTypeHex.orEmpty(),
            fetchedSeGroupIdHex = seGroupIdHex.orEmpty()
        )
    }

    /**
     * Display wearable device status if device is activated/suspended.
     */
    fun displayDeviceStatusIfRequired(onOkAction: (() -> Unit)? = null) {
        val deviceName = arguments?.getString(ACTION_DEVICE_STATUS_UPDATE)
        if (deviceName.isNullOrEmpty()) {
            if (onOkAction != null) {
                onOkAction()
            }
            return;
        }
        logger.debug("deviceStatus :: available device fragment display suspended");
        statusDialog(activity, getString(R.string.suspended_message, deviceName), onOkAction)
    }

    private fun clearDevice() {
                lifecycleScope.launch {
                    val outcome = CdcvmApi.resetCvm(requireContext())
                    logger.debug("Wearable settings: clear device - ${outcome.describe()}")
//                    if (outcome.isSuccess) {
//                        showToast(getString(R.string.wearable_clear_device_success))
//                    } else {
//                        showToast(outcome.describe())
//                    }
                }

    }
}
