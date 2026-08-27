// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: ScriptHandler.kt manages all Bluetooth Low Energy (BLE) communication and secure element script
 * operations such as installation, deletion, and SEID fetching. It ensures thread-safe execution, handles
 * permission checks, manages connection states, and uses callbacks to update UI and logs during secure script processing.
 **/
package com.infineon.secora.wallet.domain.wearable.ble.script

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.PayExternalLaunch
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.domain.walletsdk.WalletRepository
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.utils.Utils
import com.infineon.secora.wallet.utils.helper.ConfiguredWalletIdentity
import com.infineon.secora.wallet.utils.helper.SecureElementScriptCoordinator
import com.infineon.secora.wallet.utils.constants.Constants.BLE_GET_RESPONSE_CHAIN_BUDGET_PER_APDU_SECONDS
import com.infineon.secora.wallet.utils.constants.Constants.BLE_PER_APDU_TIMEOUT_SECONDS
import com.infineon.secora.wallet.utils.constants.Constants.BLE_SCRIPT_MAX_TIMEOUT_SECONDS
import com.infineon.secora.wallet.utils.constants.Constants.BLE_SCRIPT_MIN_TIMEOUT_SECONDS
import com.infineon.secora.wallet.utils.constants.Constants.BLE_SCRIPT_OPERATION_TIMEOUT_SECONDS
import com.infineon.secora.wallet.utils.constants.Constants.BLE_SCRIPT_TIMEOUT_BUFFER_SECONDS
import com.infineon.secora.wallet.utils.constants.Constants.BLUETOOTH_NOT_CONNECTED
import com.infineon.secora.wallet.utils.constants.Constants.BLUETOOTH_PERMISSION_DENIED
import com.infineon.secora.wallet.utils.constants.Constants.BLUETOOTH_PERMISSION_MISSING
import com.infineon.secora.wallet.utils.constants.Constants.BLUETOOTH_PERMISSION_REQUIRE
import com.infineon.secora.wallet.utils.constants.Constants.CONNECTION_FAILED
import com.infineon.secora.wallet.utils.constants.Constants.CONNECTION_TIMED_OUT
import com.infineon.secora.wallet.utils.constants.Constants.TIMEOUT_EXCEPTION
import com.infineon.secora.wearable.ble.BleProtocol
import com.infineon.secora.wearable.protocolapi.ISecoraBleProtocol
import com.infineon.secora.wearable.scriptloader.JsonScriptLoader.ApduExecutionResult
import com.infineon.secora.wearable.util.CPLCData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

/**
 * Data class to hold script execution result including success status and APDU results
 */
data class ScriptExecutionResult(
    val success: Boolean,
    val apduResults: List<ApduExecutionResult> = emptyList(),
    val sdScript: String? = null
)

/**
 *
 * Handles all BLE script-related operations such as connecting, disconnecting,
 * and communicating with Bluetooth Low Energy (BLE) devices.
 *
 * This class abstracts the BLE protocol layer and provides thread-safe access
 * to BLE operations
 *
 * @param context The application context used for BLE operations.
 * @param callbacks Optional callbacks to notify about BLE events.
 */
class ScriptHandler(
    private val context: Context,
    private val callbacks: Callbacks? = null
) {
    private val logger: ApplicationLogger = getApplicationLogger(ScriptHandler::class.java.simpleName)

    companion object {
        /**
         * Dedicated thread for JSON script APDU chains so the main thread is never blocked on BLE I/O.
         */
        private val bleScriptExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "BleScriptWorker").apply { isDaemon = true }
        }

        /**
         * True when [throwable] (or a cause in its chain) indicates a BLE link/transport failure
         * during script execution (disconnect, write failure, GATT drop, etc.).
         */
        fun isBleTransportError(throwable: Throwable?): Boolean {
            var t: Throwable? = throwable
            var depth = 0
            while (t != null && depth < 8) {
                val msg = t.message?.lowercase().orEmpty()
                if (msg.contains("ble protocol") ||
                    msg.contains("could not write") ||
                    msg.contains("connection state") ||
                    msg.contains("gatt") ||
                    msg.contains(BLUETOOTH_NOT_CONNECTED.lowercase())
                ) {
                    return true
                }
                t = t.cause
                depth++
            }
            return false
        }

        /**
         * Returns true when [throwable] (or a cause in its chain) is a secure-element APDU status-word failure
         * from [com.infineon.secora.wearable.scriptloader.JsonScriptLoader].
         *
         * @param throwable Error thrown during script execution.
         * @return true when the failure is an APDU status-word error.
         */
        fun isScriptApduStatusWordError(throwable: Throwable?): Boolean {
            var t: Throwable? = throwable
            var depth = 0
            while (t != null && depth < 8) {
                if (t.javaClass.name.endsWith("StatusWordError")) {
                    return true
                }
                val msg = t.message.orEmpty()
                if (msg.contains("returned status word 0x", ignoreCase = true) ||
                    msg.contains("Error during JSON script loading", ignoreCase = true)
                ) {
                    return true
                }
                t = t.cause
                depth++
            }
            return false
        }

        /**
         * Runs [supplier] on the dedicated BLE script worker thread.
         *
         * @param supplier Async BLE/script work to execute off the main thread.
         * @return Future chaining the supplier result.
         */
        private fun <T> runOnBleScriptExecutor(
            supplier: () -> CompletableFuture<T>
        ): CompletableFuture<T> {
            return CompletableFuture.supplyAsync({ Unit }, bleScriptExecutor)
                .thenCompose { supplier() }
        }

        /**
         * Script-level timeout derived from APDU count: any step in the chain can stall independently.
         */
        private fun computeBleScriptTimeoutSeconds(jsonBytes: ByteArray): Long {
            return try {
                val apduCount = JSONObject(String(jsonBytes, StandardCharsets.UTF_8))
                    .optJSONArray("apdu_list")
                    ?.length()
                    ?.takeIf { it > 0 }
                    ?: 1
                val perApduBudget =
                    BLE_PER_APDU_TIMEOUT_SECONDS + BLE_GET_RESPONSE_CHAIN_BUDGET_PER_APDU_SECONDS
                val computed = apduCount * perApduBudget + BLE_SCRIPT_TIMEOUT_BUFFER_SECONDS
                computed.coerceIn(BLE_SCRIPT_MIN_TIMEOUT_SECONDS, BLE_SCRIPT_MAX_TIMEOUT_SECONDS)
            } catch (_: Exception) {
                BLE_SCRIPT_OPERATION_TIMEOUT_SECONDS
            }
        }

        /** TITAN / FCM identity — hardcoded per Infineon (assets/configuration.json). */
        const val TITAN_OEM_ID = "3502"
        const val TITAN_OEM_NAME = "TITAN"
        const val TITAN_SE_ID = "3502"
        const val TITAN_SE_GROUP_ID = "3502"
        const val TITAN_WEARABLE_ID = "35123512"

        /**
         * Persists Infineon TITAN OEM / wearable constants. CPLC icType-seGroup is stored separately
         * after [handleFetchCPLCDataSuccess] reads the device.
         */
        fun applyTitanHardcodedIdentity(context: Context) {
            StorageRepository.saveString(PreferenceKey.CPLC_OEM_ID, TITAN_OEM_ID)
            StorageRepository.saveString(PreferenceKey.WEARABLE_MODEL_ID, TITAN_WEARABLE_ID)
        }

        /**
         * Seeds wearable / OEM prefs from [applyTitanHardcodedIdentity] before CPLC fetch.
         */
        private fun seedConfiguredWearableModelId(context: Context) {
            applyTitanHardcodedIdentity(context)
        }
    }

    private var bleProtocol: ISecoraBleProtocol? = null
    private var bleProtocolFuture: CompletableFuture<BleProtocol>? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private val isConnecting = AtomicBoolean(false)
    private val lock = ReentrantLock() // For synchronizing BLE operations

    /**
     *
     * Safely sets the current BLE protocol instance and updates the connection state.
     *
     * This function uses a lock to ensure thread-safety.
     *
     * @param protocol The BLE protocol instance to set.
     */
    fun setBleProtocol(protocol: ISecoraBleProtocol) {
        lock.lock()
        try {
            this.bleProtocol = protocol
            BluetoothStateManager.setActiveProtocol(protocol)
            val seId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID).takeIf { it.isNotBlank() }
            protocol.bluetoothDevice.address?.let { addr ->
                BluetoothStateManager.addConnectedDevice(addr, seId)
            }
            callbacks?.updateLogs("BLE protocol set successfully")
        } finally {
            lock.unlock()
        }
    }

    /**
     *
     * Defines a set of callback methods used for communicating UI or log updates
     * from background to the UI layer.
     */
    interface Callbacks {
        fun showLoading(show: Boolean, msg: String = "Please wait")
        fun showToast(message: String)
        fun updateLogs(message: String)
    }

    /**
     *
     * Checks whether the app has all the necessary Bluetooth-related permissions.
     *
     * This function adapts to different Android API levels:
     * - For Android 12 (API 31, S) and above, it checks for the new Bluetooth permissions
     *   ("BLUETOOTH_CONNECT" and "BLUETOOTH_SCAN").
     * - For older Android versions, it checks for legacy Bluetooth and location permissions.
     *
     * @return "true" if all required permissions are granted, otherwise `false`.
     */
    private fun hasBluetoothPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )

        // Verify that all required permissions are granted
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     *
     * Initiates a Bluetooth Low Energy (BLE) connection in a thread-safe manner.
     *
     * - Acquires a lock to ensure thread-safe BLE connection handling.
     * - Verifies that all required Bluetooth permissions are granted.
     * - Retrieves the target Bluetooth device instance.
     * - Attempts to connect if a device is found; otherwise handles missing device scenarios.
     *
     * @return A [CompletableFuture] resolving to "true" on successful connection "false" otherwise.
     */
    private fun connectBle(): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        lock.lock()
        try {
            if (!hasBluetoothPermissions()) {
                handleBluetoothPermissionError(future)
                return future
            }

            bluetoothDevice = selectedBluetoothDevice
            bluetoothDevice?.let { device ->
                handleDeviceConnection(device, future)
            } ?: run {
                handleNoDeviceSelected(future)
            }
        } finally {
            lock.unlock()
        }

        return future
    }

    /**
     *
     * Handles the case where Bluetooth permissions are missing or not granted.
     *
     * This method:
     * - Log a message indicating missing Bluetooth permissions.
     * - Show a user-facing toast to inform about required permissions.
     * - Mark the future as failed (connection cannot proceed).
     *
     * @param future The [CompletableFuture] representing the ongoing BLE connection attempt.
     */
    private fun handleBluetoothPermissionError(future: CompletableFuture<Boolean>) {
        callbacks?.updateLogs(BLUETOOTH_PERMISSION_MISSING)
        callbacks?.showToast(BLUETOOTH_PERMISSION_REQUIRE)
        future.complete(false)
    }

    /**
     *
     * Handles the logic for connecting to a Bluetooth device safely.
     *
     * @param device The BluetoothDevice to connect to.
     * @param future A CompletableFuture used to signal the connection result.
     */
    private fun handleDeviceConnection(
        device: BluetoothDevice,
        future: CompletableFuture<Boolean>
    ) {
        if (reuseExistingBleConnection(future)) return

        val inFlight = bleProtocolFuture
        if (inFlight == null || inFlight.isDone) {
            startNewBleConnection(device, future)
        } else {
            awaitInFlightBleConnection(inFlight, future)
        }
    }

    /**
     * Reuses [BluetoothStateManager.activeProtocol] on a fresh ScriptHandler instance (e.g. Card Settings PPSE).
     */
    private fun adoptActiveBleProtocolIfAvailable(): Boolean {
        bleProtocol?.let { return true }
        val existing = BluetoothStateManager.activeProtocol ?: return false
        if (!BluetoothStateManager.isConnected) return false
        bleProtocol = existing
        try {
            bluetoothDevice = existing.bluetoothDevice
        } catch (_: SecurityException) {
            // Protocol alone is sufficient for transceive.
        }
        callbacks?.updateLogs("Adopted active BLE protocol")
        return true
    }

    /** True when a live protocol is available locally or on [BluetoothStateManager]. */
    private fun hasUsableBleProtocol(): Boolean =
        adoptActiveBleProtocolIfAvailable()

    /**
     * Reuses an active BLE protocol when the shared connection is still alive.
     *
     * @param future CompletableFuture to complete when reuse succeeds.
     * @return `true` when an existing connection was reused and [future] was completed.
     */
    private fun reuseExistingBleConnection(future: CompletableFuture<Boolean>): Boolean {
        val existing = BluetoothStateManager.activeProtocol
        if (existing == null || !BluetoothStateManager.isConnected) return false
        bleProtocol = existing
        callbacks?.updateLogs("Reusing existing BLE connection")
        future.complete(true)
        return true
    }

    /**
     * Starts a new BLE connection or reports that one is already in progress.
     *
     * @param device Target wearable Bluetooth device.
     * @param future CompletableFuture signaling connect success or failure.
     */
    private fun startNewBleConnection(device: BluetoothDevice, future: CompletableFuture<Boolean>) {
        if (isConnecting.compareAndSet(false, true)) {
            attemptConnection(device, future)
        } else {
            handleConnectionInProgress(future)
        }
    }

    /**
     * Waits for an in-flight BLE connect started elsewhere and completes [future] with the outcome.
     *
     * @param inFlight Pending connect future from a prior [attemptConnection] call.
     * @param future CompletableFuture to complete when the in-flight connect settles.
     */
    private fun awaitInFlightBleConnection(
        inFlight: CompletableFuture<BleProtocol>,
        future: CompletableFuture<Boolean>
    ) {
        inFlight.whenComplete { protocol, throwable ->
            if (throwable == null && protocol != null && BluetoothStateManager.isConnected) {
                bleProtocol = protocol
                BluetoothStateManager.setActiveProtocol(protocol)
                if (!future.isDone) future.complete(true)
            } else if (!future.isDone) {
                future.complete(false)
            }
        }
    }

    /**
     *
     * Attempts to establish a Bluetooth connection with the given device.
     *
     * @param device The BluetoothDevice to connect to.
     * @param future A CompletableFuture used to notify the connection result asynchronously.
     */
    private fun attemptConnection(device: BluetoothDevice, future: CompletableFuture<Boolean>) {
        if (PayExternalLaunch.isHostLaunch()) {
            (context as? android.app.Activity)?.runOnUiThread {
                isConnecting.set(false)
                if (adoptActiveBleProtocolIfAvailable()) {
                    if (!future.isDone) future.complete(true)
                } else {
                    callbacks?.updateLogs(
                        "Host launch: shared SECORA protocol unavailable (do not open second GATT)"
                    )
                    if (!future.isDone) future.complete(false)
                }
            }
            return
        }

        callbacks?.showLoading(true, "")
        try {
            BluetoothStateManager.disconnectActiveProtocol()
            bleProtocol = null

            val connectTimeoutSeconds = 20L
            bleProtocolFuture =
                BluetoothStateManager.connectBleDevice(context, device)
                    .orTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                    .whenComplete { protocol, throwable ->
                        handleConnectionResult(protocol, throwable, future)
                    }
        } catch (e: SecurityException) {
            handleSecurityException(e, future)
        }
    }

    /**
     *
     * Handles the result of a Bluetooth connection attempt.
     *
     * @param protocol  The BleProtocol instance returned on a successful connection (null if failed).
     * @param throwable The Throwable returned if the connection failed (null if successful).
     * @param future    A CompletableFuture used to signal the final connection status.
     */
    private fun handleConnectionResult(
        protocol: ISecoraBleProtocol?,
        throwable: Throwable?,
        future: CompletableFuture<Boolean>
    ) {
        (context as? android.app.Activity)?.runOnUiThread {
            isConnecting.set(false)
            if (throwable == null && protocol != null) {
                handleSuccessfulConnection(protocol, future)
            } else {
                handleFailedConnection(throwable, future)
            }
        }
    }

    /**
     *
     * Handles post-connection logic when the BLE device is successfully connected.
     *
     * @param protocol The established [BleProtocol] instance representing the active BLE session.
     * @param future   A [CompletableFuture] used to signal the successful completion of the connection.
     */
    private fun handleSuccessfulConnection(
        protocol: ISecoraBleProtocol,
        future: CompletableFuture<Boolean>
    ) {
        bleProtocol = protocol
        BluetoothStateManager.setActiveProtocol(protocol)
        val seId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID).takeIf { it.isNotBlank() }
        protocol.bluetoothDevice.address?.let { addr ->
            BluetoothStateManager.addConnectedDevice(addr, seId)
        }
        callbacks?.updateLogs("BLE connected successfully")
        if (!future.isDone) future.complete(true)
    }

    /**
     *
     * Handles cleanup and error reporting when a BLE connection fails.
     *
     * @param throwable The [Throwable] containing the cause of the connection failure (if any).
     * @param future    A [CompletableFuture] used to signal that the connection attempt was unsuccessful.
     */
    private fun handleFailedConnection(throwable: Throwable?, future: CompletableFuture<Boolean>) {
        bleProtocol = null
        BluetoothStateManager.setActiveProtocol(null)
        val message = when {
            throwable is TimeoutException -> CONNECTION_TIMED_OUT
            throwable?.message.isNullOrBlank() -> throwable?.javaClass?.simpleName ?: CONNECTION_FAILED
            else -> throwable.message
        }
        callbacks?.updateLogs("BLE connection failed: $message")
        if (!future.isDone) future.complete(false)
    }

    /**
     *
     * Handles security-related exceptions that occur during the BLE connection attempt.
     *
     * @param e The [SecurityException] thrown during the BLE connection process.
     * @param future  A [CompletableFuture] used to signal that the connection attempt
     * failed due to a security issue.
     */
    private fun handleSecurityException(e: SecurityException, future: CompletableFuture<Boolean>) {
        isConnecting.set(false)
        callbacks?.updateLogs("SecurityException in connectBle: ${e.message}")
        callbacks?.showToast(BLUETOOTH_PERMISSION_DENIED)
        if (!future.isDone) future.complete(false)
    }

    /**
     *
     * Handles the scenario where a BLE connection attempt is already in progress.
     *
     * @param future A [CompletableFuture] used to signal that a new connection attempt
     * cannot proceed because another one is currently active.
     */
    private fun handleConnectionInProgress(future: CompletableFuture<Boolean>) {
        callbacks?.updateLogs("BLE connection already in progress")
        if (!future.isDone) future.complete(false)
    }

    /**
     *
     * Handles the case when no Bluetooth device has been selected for connection.
     *
     * @param future A [CompletableFuture] used to signal that the connection attempt
     * cannot proceed because no device was chosen.
     */
    private fun handleNoDeviceSelected(future: CompletableFuture<Boolean>) {
        callbacks?.updateLogs(context.getString(R.string.no_bluetooth_device_selected))
        if (!future.isDone) future.complete(false)
    }

    /**
     *
     * Retrieves the previously selected Bluetooth device from stored preferences.
     *
     * @return The [BluetoothDevice] object if found and accessible, otherwise `null`.
     */
    private val selectedBluetoothDevice: BluetoothDevice?
        get() {
            val deviceAddress = StorageRepository.readString(PreferenceKey.SELECTED_DEVICE_ADDRESS)

            if (deviceAddress.isEmpty()) {
                callbacks?.updateLogs("No device address found in preferences")
                return null
            }

            return try {
                val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
                val bluetoothAdapter = bluetoothManager?.adapter
                bluetoothAdapter?.getRemoteDevice(deviceAddress)
            } catch (e: SecurityException) {
                callbacks?.updateLogs("SecurityException in getBluetoothDevice: ${e.message}")
                callbacks?.showToast(BLUETOOTH_PERMISSION_DENIED)
                null
            }
        }

    /**
     * Executes delete script for card deletion operations.
     * This method provides a separate implementation for delete operations
     * to allow for future differentiation in the sesdk layer.
     *
     * @param jsonBytes The JSON script bytes to execute for deletion
     * @param clearDefaultCard When true, clear default card after delete (delink or default-card delete)
     * @return CompletableFuture that completes with execution success boolean
     */
    fun deleteScript(
        jsonBytes: ByteArray?,
        clearDefaultCard: Boolean = false
    ): CompletableFuture<Boolean> {
        return deleteScriptWithRetry(jsonBytes, attempt = 0, clearDefaultCard = clearDefaultCard)
    }

    /**
     * Runs delete-script execution with an internal retry counter for timeout recovery.
     *
     * @param jsonBytes Optional JSON delete script; falls back to stored install script when null.
     * @param attempt Zero-based attempt index (timeout path may increment and recurse once).
     * @param clearDefaultCard When true, clear default card after delete.
     * @return Future completing with delete success or failing exceptionally.
     */
    private fun deleteScriptWithRetry(
        jsonBytes: ByteArray?,
        attempt: Int,
        clearDefaultCard: Boolean
    ): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        lock.lock()
        try {
            if (!hasBluetoothPermissions()) {
                callbacks?.updateLogs("deleteScript failed: $BLUETOOTH_PERMISSION_MISSING")
                callbacks?.showToast(BLUETOOTH_PERMISSION_REQUIRE)
                future.completeExceptionally(SecurityException(BLUETOOTH_PERMISSION_MISSING))
                return SecureElementScriptCoordinator.trackFuture(future)
            }

            if (BluetoothStateManager.isConnected && hasUsableBleProtocol()) {
                deleteScriptInternal(jsonBytes, future, attempt, clearDefaultCard)
            } else {
                handleDeleteScriptConnectionRequired(jsonBytes, future, attempt, clearDefaultCard)
            }
        } finally {
            lock.unlock()
        }

        return SecureElementScriptCoordinator.trackFuture(future)
    }

    /**
     *
     * Deletes an installed script on the connected BLE device.
     *
     * @param jsonBytes Optional JSON byte array containing the delete script payload.
     * @param future    A [CompletableFuture] used to signal the success or failure of the deletion process.
     */
    private fun deleteScriptInternal(
        jsonBytes: ByteArray?,
        future: CompletableFuture<Boolean>,
        attempt: Int,
        clearDefaultCard: Boolean
    ) {
        try {
            callbacks?.updateLogs("Starting deleteScript (clearDefaultCard=$clearDefaultCard)")
            callbacks?.showLoading(true, context.getString(R.string.text_deleting_script))

            val installScript = StorageRepository.readString(PreferenceKey.INSTALL_SCRIPT).toByteArray()
            val jsonBytesResult = jsonBytes ?: installScript
            val scriptTimeoutSeconds = computeBleScriptTimeoutSeconds(jsonBytesResult)
            val scriptRunner = ScriptRunner()

            runOnBleScriptExecutor {
                scriptRunner.executeScript(
                    isDeleteScriptExecution = true,
                    clearDefaultCard = clearDefaultCard,
                    protocol = bleProtocol!!,
                    jsonBytes = jsonBytesResult
                )
            }
                .orTimeout(scriptTimeoutSeconds, TimeUnit.SECONDS)
                .thenAccept {
                    handleDeleteScriptSuccess(true, future)
                }
                .exceptionally { throwable ->
                    handleDeleteScriptFailure(throwable, future, jsonBytes, attempt, clearDefaultCard)
                    null
                }

        } catch (e: Exception) {
            handleDeleteScriptException(e, future)
        }
    }

    /**
     *
     * Handles the result of a successful delete script operation.
     *
     * This method updates the UI, logs the result, and completes the associated [CompletableFuture].
     *
     * @param success Indicates whether the script deletion succeeded (`true`) or failed (`false`).
     * @param future  A [CompletableFuture] used to signal the completion status of the delete operation.
     */
    private fun handleDeleteScriptSuccess(success: Boolean, future: CompletableFuture<Boolean>) {
        callbacks?.updateLogs("deleteScript success: $success")
        callbacks?.showLoading(true)
        if (!future.isDone) future.complete(success)
    }

    /**
     *
     * Handles a failure that occurs during the delete script operation.
     *
     * @param throwable The [Throwable] containing details about the failure.
     * @param future A [CompletableFuture] used to signal that the delete operation failed.
     */
    private fun handleDeleteScriptFailure(
        throwable: Throwable,
        future: CompletableFuture<Boolean>,
        jsonBytes: ByteArray?,
        attempt: Int,
        clearDefaultCard: Boolean
    ) {
        val message = throwable.message ?: throwable::class.java.simpleName
        callbacks?.updateLogs("deleteScript failed: $message")

        if (isBleTransportError(throwable) || !BluetoothStateManager.isConnected) {
            failDeleteScriptFutureImmediately(throwable, future)
            return
        }

        val maxAttempts = 1
        if (isTimeoutError(throwable) && attempt < maxAttempts && BluetoothStateManager.isConnected) {
            SecureElementScriptCoordinator.forceResetActiveScripts()
            resetBleConnectionForRetry(TIMEOUT_EXCEPTION)
            connectBle().thenCompose { connected ->
                if (connected) deleteScriptWithRetry(jsonBytes, attempt + 1, clearDefaultCard)
                else CompletableFuture.failedFuture(IllegalStateException(BLUETOOTH_NOT_CONNECTED))
            }.thenAccept { result ->
                callbacks?.showLoading(false)
                if (!future.isDone) future.complete(result)
            }.exceptionally { retryThrowable ->
                callbacks?.showLoading(false)
                if (!future.isDone) future.completeExceptionally(retryThrowable)
                null
            }
            return
        }

        callbacks?.showLoading(false)
        if (!future.isDone) future.completeExceptionally(throwable)
    }

    /**
     *
     * Handles unexpected exceptions that occur during the delete script process.
     *
     * @param e The [Exception] thrown during the delete operation.
     * @param future A [CompletableFuture] used to signal that the delete operation encountered an exception.
     */
    private fun handleDeleteScriptException(e: Exception, future: CompletableFuture<Boolean>) {
        callbacks?.updateLogs("deleteScript exception: ${e.message}")
        callbacks?.showLoading(false)
        if (!future.isDone) future.completeExceptionally(e)
    }

    /**
     *
     * Handles the scenario where a BLE connection is required before deleting a script.
     *
     * @param jsonBytes Optional JSON byte array containing the delete script payload.
     * @param future    A [CompletableFuture] used to signal the final result of the delete operation.
     */
    private fun handleDeleteScriptConnectionRequired(
        jsonBytes: ByteArray?,
        future: CompletableFuture<Boolean>,
        attempt: Int,
        clearDefaultCard: Boolean
    ) {
        callbacks?.updateLogs("deleteScript: Attempting to connect to BLE device")
        connectBle().thenCompose { success ->
            if (success) deleteScriptWithRetry(jsonBytes, attempt, clearDefaultCard)
            else CompletableFuture.failedFuture(
                IllegalStateException(
                    BLUETOOTH_NOT_CONNECTED
                )
            )
        }.thenAccept { result ->
            future.complete(result)
        }.exceptionally { throwable ->
            future.completeExceptionally(throwable)
            null
        }
    }

    /**
     *
     * Fetches SEID from device using CPLC script.
     * This method calls ScriptRunner to handle the SEID fetching process.
     *
     * @return CompletableFuture that completes with SEID string
     */
    fun fetchSEId(): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        lock.lock()
        try {
            if (!hasBluetoothPermissions()) {
                callbacks?.updateLogs("fetchSEId failed: $BLUETOOTH_PERMISSION_MISSING")
                callbacks?.showToast(BLUETOOTH_PERMISSION_REQUIRE)
                future.completeExceptionally(SecurityException(BLUETOOTH_PERMISSION_MISSING))
                return future
            }

            if (BluetoothStateManager.isConnected && hasUsableBleProtocol()) {
                executeFetchSEId(future)
            } else {
                handleFetchSEIdConnectionRequired(future)
            }
        } finally {
            lock.unlock()
        }

        return SecureElementScriptCoordinator.trackFuture(future)
    }

    /**
     *
     * Executes the process of fetching the SEID (Secure Element Identifier) from the connected BLE device.
     *
     * This method initializes a ScriptRunner to communicate with the BLE device and retrieve the SEID
     *
     * @param future A [CompletableFuture] used to signal the success or failure of the SEID fetch operation.
     */
    private fun executeFetchSEId(future: CompletableFuture<String>) {
        try {
            callbacks?.updateLogs("Starting fetchSEId")
            callbacks?.showLoading(true, context.getString(R.string.text_fetching_seid))
            seedConfiguredWearableModelId(context)

            val scriptRunner = ScriptRunner()
            scriptRunner.fetchCPLCData(context, bleProtocol!!)
                .thenAccept { cplcData ->
                    handleFetchCPLCDataSuccess(cplcData, future)
                }
                .exceptionally { throwable ->
                    handleFetchSEIdFailure(throwable, future)
                    null
                }
        } catch (e: Exception) {
            handleFetchSEIdException(e, future)
        }
    }

    /**
     *
     * Handles successful retrieval of SE ID.
     *
     * - Updates the log with success message.
     * - Hides the loading indicator.
     * - Completes the future with the retrieved SE ID if not already done.
     *
     * @param seId The successfully fetched SE ID string.
     * @param future The CompletableFuture to complete with the result.
     */
    private fun handleFetchSEIdSuccess(seId: String, future: CompletableFuture<String>) {
        callbacks?.updateLogs("fetchSEId success: $seId")
        callbacks?.showLoading(true)
        if (!future.isDone) future.complete(seId)
    }

    /**
     * Completes the SEID future from CPLC script results: logs CPLC fields, persists OEM / SE type group and
     * wearable model ID when valid, then completes with the SEID hex string.
     *
     * @param cplcData Parsed CPLC payload, or null to complete the future exceptionally.
     * @param future CompletableFuture for the SEID fetch operation.
     */
    private fun handleFetchCPLCDataSuccess(cplcData: CPLCData?, future: CompletableFuture<String>) {
        if (cplcData == null) {
            val exception = IllegalStateException(context.getString(R.string.text_cplc_null_data))
            handleFetchSEIdFailure(exception, future)
            return
        }

        val seId = cplcData.seIdHex
        val appCtx = context.applicationContext

        val oemIdHex = "3502"
        val seGroupIdHex = "3502"
        val wearableModelIdHex = "35123512"

        StorageRepository.saveString(PreferenceKey.CPLC_OEM_ID, oemIdHex)
        StorageRepository.saveString(PreferenceKey.WEARABLE_MODEL_ID, wearableModelIdHex)
        ConfiguredWalletIdentity.seedHardcodedIdentity(appCtx)
        val seTypeGroup = ConfiguredWalletIdentity.readPersistedSeTypeGroup(appCtx)

        callbacks?.updateLogs(
            "fetchCPLCData success: seIdHex=$seId (device), hardcoded oem=$oemIdHex, " +
                "oemName=$TITAN_OEM_NAME, seTypeGroup=$seTypeGroup, seGroupIdHex=$seGroupIdHex, " +
                "cplc icType=${cplcData.icTypeHex}, seGroup=${cplcData.seGroupIdHex}, " +
                "wearable=$wearableModelIdHex"
        )

        CoroutineScope(Dispatchers.IO).launch {
            WalletRepository.saveOEMDetails(
                context = appCtx,
                oemId = oemIdHex,
                infineonSalesCodeAndGroup = seTypeGroup
            )
        }

        handleFetchSEIdSuccess(seId, future)
    }

    /**
     *
     * Handles failure during SE ID fetching.
     *
     * - Logs the failure message.
     * - Hides the loading indicator.
     * - Completes the future exceptionally with the thrown error if not already completed.
     *
     * @param throwable The cause of failure.
     * @param future The CompletableFuture to complete exceptionally.
     */
    private fun handleFetchSEIdFailure(throwable: Throwable, future: CompletableFuture<String>) {
        callbacks?.updateLogs("fetchSEId failed: ${throwable.message}")
        callbacks?.showLoading(false)
        if (!future.isDone) future.completeExceptionally(throwable)
    }

    /**
     *
     * Handles unexpected exceptions that occur during SE ID fetching.
     *
     * - Logs the exception message.
     * - Hides the loading indicator.
     * - Completes the future exceptionally with the exception if not already completed.
     *
     * @param e The exception that occurred.
     * @param future The CompletableFuture to complete exceptionally.
     */
    private fun handleFetchSEIdException(e: Exception, future: CompletableFuture<String>) {
        callbacks?.updateLogs("fetchSEId exception: ${e.message}")
        callbacks?.showLoading(false)
        if (!future.isDone) future.completeExceptionally(e)
    }

    /**
     *
     * Handles cases where a BLE connection is required before fetching SE ID.
     *
     * - Logs that a BLE connection attempt is being made.
     * - Tries to connect to the BLE device.
     * - On successful connection, attempts to fetch the SE ID.
     * - If connection or fetching fails, completes the future exceptionally.
     *
     * Requires Android 12 (API 31) or higher.
     *
     * @param future The CompletableFuture to complete once the SE ID is fetched or an error occurs.
     */
    private fun handleFetchSEIdConnectionRequired(future: CompletableFuture<String>) {
        callbacks?.updateLogs("fetchSEId: Attempting to connect to BLE device")
        connectBle().thenCompose { success ->
            if (success) fetchSEId()
            else CompletableFuture.failedFuture(
                IllegalStateException(
                    BLUETOOTH_NOT_CONNECTED
                )
            )
        }.thenAccept { result ->
            future.complete(result)
        }.exceptionally { throwable ->
            future.completeExceptionally(throwable)
            null
        }
    }

    /**
     * Returns true if [throwable] or any cause in its chain indicates a timeout (type or message).
     *
     * @param throwable Root error from an async BLE or script step; may be null.
     */
    private fun isTimeoutError(throwable: Throwable?): Boolean {
        var t: Throwable? = throwable
        var depth = 0
        while (t != null && depth < 8) {
            if (t is TimeoutException) return true
            val msg = t.message
            if (msg != null && msg.contains(TIMEOUT_EXCEPTION, ignoreCase = true)) return true
            t = t.cause
            depth++
        }
        return false
    }

    /**
     * True when the failure is a BLE link/transport problem (disconnect, write failure, GATT drop).
     * These should surface to the UI immediately instead of blocking on script/connect retries.
     */
    private fun isBleTransportError(throwable: Throwable?): Boolean =
        Companion.isBleTransportError(throwable)

    /**
     * Fails an install-script future immediately on BLE transport loss without retrying.
     *
     * @param throwable Root transport error to propagate.
     * @param future Install-script future to complete exceptionally.
     */
    private fun failScriptFutureImmediately(
        throwable: Throwable,
        future: CompletableFuture<ScriptExecutionResult>
    ) {
        SecureElementScriptCoordinator.forceResetActiveScripts()
        resetBleConnectionForRetry("transport/disconnect")
        callbacks?.showLoading(false)
        if (!future.isDone) future.completeExceptionally(throwable)
    }

    /**
     * Fails a delete-script future immediately on BLE transport loss without retrying.
     *
     * @param throwable Root transport error to propagate.
     * @param future Delete-script future to complete exceptionally.
     */
    private fun failDeleteScriptFutureImmediately(
        throwable: Throwable,
        future: CompletableFuture<Boolean>
    ) {
        SecureElementScriptCoordinator.forceResetActiveScripts()
        resetBleConnectionForRetry("transport/disconnect")
        callbacks?.showLoading(false)
        if (!future.isDone) future.completeExceptionally(throwable)
    }

    /**
     * Tears down the current BLE session and tracking state so a subsequent connect can start clean
     * (e.g. after a timeout during PPSE).
     *
     * @param reason Short text for logs explaining why the reset runs.
     */
    private fun resetBleConnectionForRetry(reason: String) {
        try {
            callbacks?.updateLogs("Resetting BLE connection ($reason)")
        } catch (_: Exception) {
            // best-effort logging
        }

        // Host 共享 GATT：只清 ScriptHandler 本地引用，不能 release 全局 activeProtocol。
        if (PayExternalLaunch.isHostLaunch()) {
            bleProtocol = null
            bleProtocolFuture = null
            adoptActiveBleProtocolIfAvailable()
            return
        }

        // Ensure any hanging GATT/protocol is torn down so we don't keep reusing a stuck instance.
        val addr = try {
            bleProtocol?.bluetoothDevice?.address ?: bluetoothDevice?.address
        } catch (_: Exception) {
            null
        }
        BluetoothStateManager.disconnectActiveProtocol()
        BluetoothStateManager.setActiveProtocol(null)
        if (!addr.isNullOrBlank()) {
            BluetoothStateManager.removeConnectedDevice(addr)
        }

        bleProtocol = null
        bleProtocolFuture = null
    }

    /**
     * Runs PPSE script execution with an internal retry counter used when recovering from timeouts.
     *
     * @param aid Payment application AID for set-default PPSE flow.
     * @param cardType Card type identifier for the script.
     * @param attempt Zero-based attempt index (timeout path may increment and recurse once).
     * @return Future completing with script success or failing exceptionally.
     */
    private fun executePPSEScriptWithRetry(
        aid: String,
        cardType: String,
        otherCardAppletInstanceAids: List<String>,
        attempt: Int
    ): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        lock.lock()
        try {
            if (!hasBluetoothPermissions()) {
                callbacks?.updateLogs("executePPSEScript failed: $BLUETOOTH_PERMISSION_MISSING")
                callbacks?.showToast(BLUETOOTH_PERMISSION_REQUIRE)
                future.completeExceptionally(SecurityException(BLUETOOTH_PERMISSION_MISSING))
                return SecureElementScriptCoordinator.trackFuture(future)
            }

            if (BluetoothStateManager.isConnected && hasUsableBleProtocol()) {
                executePPSEScriptInternal(aid, cardType, otherCardAppletInstanceAids, future, attempt)
            } else {
                handleExecutePPSEScriptConnectionRequired(aid, cardType, otherCardAppletInstanceAids, future)
            }
        } finally {
            lock.unlock()
        }

        return SecureElementScriptCoordinator.trackFuture(future)
    }

    /**
     * Executes PPSE script for MCM operations.
     * This method calls ScriptRunner to handle the PPSE script execution.
     *
     * @param ppseFileName The logical name of the PPSE file (e.g., "PPSE-01", "PPSE-02", "PPSE-03")
     * @return CompletableFuture that completes with execution success boolean
     */
    fun executePPSEScript(
        aid: String,
        cardType: String,
        otherCardAppletInstanceAids: List<String> = emptyList()
    ): CompletableFuture<Boolean> {
        return executePPSEScriptWithRetry(aid, cardType, otherCardAppletInstanceAids, attempt = 0)
    }

    /**
     *
     * Executes a PPSE (Proximity Payment System Environment) script asynchronously.
     * This method handles the overall flow, including logging, showing a loading state,
     * and delegating success/failure handling to appropriate methods.
     */
    private fun executePPSEScriptInternal(
        aid: String,
        cardType: String,
        otherCardAppletInstanceAids: List<String>,
        future: CompletableFuture<Boolean>,
        attempt: Int
    ) {
        try {
            callbacks?.showLoading(true, context.getString(R.string.text_executing_ppse_script))

            // Create a ScriptRunner instance and call executePPSEScript
            val scriptRunner = ScriptRunner() // No script loader needed for PPSE
            scriptRunner.executePPSEScript(context, bleProtocol!!, aid, cardType, otherCardAppletInstanceAids)
                .thenAccept { success ->
                    handleExecutePPSEScriptSuccess(success, future)
                }
                .exceptionally { throwable ->
                    handleExecutePPSEScriptFailure(
                        throwable,
                        future,
                        aid,
                        cardType,
                        otherCardAppletInstanceAids,
                        attempt
                    )
                    null
                }
        } catch (e: Exception) {
            handleExecutePPSEScriptException(e, future)
        }
    }

    /**
     *
     * Handles the successful completion of the PPSE script execution.
     * Updates logs, hides loading indicator, and completes the given future successfully.
     */
    private fun handleExecutePPSEScriptSuccess(
        success: Boolean,
        future: CompletableFuture<Boolean>
    ) {
        callbacks?.updateLogs("executePPSEScript success: $success")
        callbacks?.showLoading(true)
        if (!future.isDone) future.complete(success)
    }

    /**
     *
     * Handles failure during PPSE script execution (caught in the async chain).
     * Logs the failure, hides loading indicator, and completes the future exceptionally.
     */
    private fun handleExecutePPSEScriptFailure(
        throwable: Throwable,
        future: CompletableFuture<Boolean>,
        aid: String,
        cardType: String,
        otherCardAppletInstanceAids: List<String>,
        attempt: Int
    ) {
        val message = throwable.message ?: throwable::class.java.simpleName
        callbacks?.updateLogs("executePPSEScript failed: $message")

        val maxAttempts = 1
        if (isTimeoutError(throwable) && attempt < maxAttempts) {
            resetBleConnectionForRetry(TIMEOUT_EXCEPTION)
            connectBle().thenCompose { connected ->
                if (connected) executePPSEScriptWithRetry(aid, cardType, otherCardAppletInstanceAids, attempt + 1)
                else CompletableFuture.failedFuture(IllegalStateException(BLUETOOTH_NOT_CONNECTED))
            }.thenAccept { result ->
                callbacks?.showLoading(false)
                if (!future.isDone) future.complete(result)
            }.exceptionally { retryThrowable ->
                callbacks?.showLoading(false)
                if (!future.isDone) future.completeExceptionally(retryThrowable)
                null
            }
            return
        }
        callbacks?.showLoading(false)
        if (!future.isDone) future.completeExceptionally(throwable)
    }

    /**
     *
     * Handles synchronous exceptions thrown during the setup or execution phase
     * (outside the CompletableFuture chain).
     */
    private fun handleExecutePPSEScriptException(e: Exception, future: CompletableFuture<Boolean>) {
        callbacks?.updateLogs("executePPSEScript exception: ${e.message}")
        callbacks?.showLoading(false)
        if (!future.isDone) future.completeExceptionally(e)
    }

    /**
     *
     * Handles the case where a BLE connection is required before executing the PPSE script.
     * Attempts to establish the BLE connection first, then executes the script upon success.
     */
    private fun handleExecutePPSEScriptConnectionRequired(
        aid: String,
        cardType: String,
        otherCardAppletInstanceAids: List<String>,
        future: CompletableFuture<Boolean>
    ) {
        callbacks?.updateLogs("executePPSEScript: Attempting to connect to BLE device")
        connectBle().thenCompose { success ->
            if (success) executePPSEScript(aid, cardType, otherCardAppletInstanceAids)
            else CompletableFuture.failedFuture(
                IllegalStateException(
                    BLUETOOTH_NOT_CONNECTED
                )
            )
        }.thenAccept { result ->
            future.complete(result)
        }.exceptionally { throwable ->
            future.completeExceptionally(throwable)
            null
        }
    }

    /**
     *
     * Executes script for card operations (install, MCM, etc.).
     * This method calls ScriptRunner to handle the script execution.
     *
     * @param jsonBytes The JSON script bytes to execute
     * @return CompletableFuture that completes with ScriptExecutionResult containing success status and APDU results
     */
    fun executeScript(jsonBytes: ByteArray?): CompletableFuture<ScriptExecutionResult> {
        return executeScriptWithRetry(jsonBytes, attempt = 0)
    }

    /**
     * Runs install-script execution with an internal retry counter for timeout recovery.
     *
     * @param jsonBytes Optional JSON install script; falls back to stored install script when null.
     * @param attempt Zero-based attempt index (timeout path may increment and recurse once).
     * @return Future completing with script results or failing exceptionally.
     */
    private fun executeScriptWithRetry(
        jsonBytes: ByteArray?,
        attempt: Int
    ): CompletableFuture<ScriptExecutionResult> {
        val future = CompletableFuture<ScriptExecutionResult>()
        lock.lock()
        try {
            if (!hasBluetoothPermissions()) {
                callbacks?.updateLogs("executeScript failed: $BLUETOOTH_PERMISSION_MISSING")
                callbacks?.showToast(BLUETOOTH_PERMISSION_REQUIRE)
                future.completeExceptionally(SecurityException(BLUETOOTH_PERMISSION_MISSING))
                return SecureElementScriptCoordinator.trackFuture(future)
            }

            if (BluetoothStateManager.isConnected && hasUsableBleProtocol()) {
                executeScriptInternal(jsonBytes, future, attempt)
            } else {
                handleExecuteScriptConnectionRequired(jsonBytes, future, attempt)
            }
        } finally {
            lock.unlock()
        }

        return SecureElementScriptCoordinator.trackFuture(future)
    }

    /**
     *
     * Executes a script asynchronously using BLE protocol.
     *
     * @param jsonBytes Optional script data as ByteArray. If null, the script is loaded from preferences.
     * @param future CompletableFuture to communicate success or failure of the operation.
     */
    private fun executeScriptInternal(
        jsonBytes: ByteArray?,
        future: CompletableFuture<ScriptExecutionResult>,
        attempt: Int
    ) {
        try {
            callbacks?.updateLogs("Starting executeScript")
            callbacks?.showLoading(true, context.getString(R.string.text_executing_script))

            val installScript = StorageRepository.readString(PreferenceKey.INSTALL_SCRIPT).toByteArray()
            val jsonBytesResult = jsonBytes ?: installScript
            val scriptTimeoutSeconds = computeBleScriptTimeoutSeconds(jsonBytesResult)
            val scriptRunner = ScriptRunner()

            runOnBleScriptExecutor {
                scriptRunner.executeScript(
                    isDeleteScriptExecution = false,
                    protocol = bleProtocol!!,
                    jsonBytes = jsonBytesResult
                )
            }
                .orTimeout(scriptTimeoutSeconds, TimeUnit.SECONDS)
                .thenAccept { results ->
                    results.forEach { result ->
                        logger.debug("[Seq ${result.apduCommandId}] SW: ${result.apduCommandResponse}")
                        logger.debug("<-- ${result.hexResponse}")
                        callbacks?.updateLogs(
                            "executeScript [Seq ${result.apduCommandId}] SW: ${result.apduCommandResponse}\n" +
                                "<-- ${result.hexResponse}"
                        )
                    }
                    handleExecuteScriptSuccess(results, future)
                }
                .exceptionally { throwable ->
                    handleExecuteScriptFailure(throwable, future, jsonBytes, attempt)
                    null
                }
        } catch (e: Exception) {
            handleExecuteScriptException(e, future)
        }
    }

    /**
     *
     * Handles successful completion of the script execution.
     *
     * @param results List of APDU execution results.
     * @param future CompletableFuture to mark as complete with the result.
     */
    private fun handleExecuteScriptSuccess(
        results: List<ApduExecutionResult>,
        future: CompletableFuture<ScriptExecutionResult>
    ) {
        val success = results.isNotEmpty()
        callbacks?.updateLogs("executeScript success: $success")
        callbacks?.showLoading(true)

        val sdScript = Utils.extractSdScript(results)
        if (!future.isDone) {
            future.complete(ScriptExecutionResult(success, results, sdScript))
        }
    }

    /**
     *
     * Handles failures that occur asynchronously during script execution.
     *
     * @param throwable The error/exception thrown during execution.
     * @param future CompletableFuture to mark as completed exceptionally.
     */
    private fun handleExecuteScriptFailure(
        throwable: Throwable,
        future: CompletableFuture<ScriptExecutionResult>,
        jsonBytes: ByteArray?,
        attempt: Int
    ) {
        val message = throwable.message ?: throwable::class.java.simpleName
        callbacks?.updateLogs("executeScript failed: $message")

        if (isBleTransportError(throwable) || !BluetoothStateManager.isConnected) {
            failScriptFutureImmediately(throwable, future)
            return
        }

        val maxAttempts = 1
        if (isTimeoutError(throwable) && attempt < maxAttempts && BluetoothStateManager.isConnected) {
            SecureElementScriptCoordinator.forceResetActiveScripts()
            resetBleConnectionForRetry(TIMEOUT_EXCEPTION)
            connectBle().thenCompose { connected ->
                if (connected) executeScriptWithRetry(jsonBytes, attempt + 1)
                else CompletableFuture.failedFuture(IllegalStateException(BLUETOOTH_NOT_CONNECTED))
            }.thenAccept { result ->
                callbacks?.showLoading(false)
                if (!future.isDone) future.complete(result)
            }.exceptionally { retryThrowable ->
                callbacks?.showLoading(false)
                if (!future.isDone) future.completeExceptionally(retryThrowable)
                null
            }
            return
        }

        callbacks?.showLoading(false)
        if (!future.isDone) future.completeExceptionally(throwable)
    }

    /**
     *
     * Handles exceptions that occur synchronously (caught in try/catch) during setup or execution.
     *
     * @param e The caught exception.
     * @param future CompletableFuture to mark as completed exceptionally.
     */
    private fun handleExecuteScriptException(e: Exception, future: CompletableFuture<ScriptExecutionResult>) {
        callbacks?.updateLogs("executeScript exception: ${e.message}")
        callbacks?.showLoading(false)
        if (!future.isDone) future.completeExceptionally(e)
    }

    /**
     *
     * Attempts to establish a BLE connection before executing the script.
     * Used when BLE connection is required but not yet established.
     *
     * @param jsonBytes Optional script data as ByteArray.
     * @param future CompletableFuture to signal success/failure of the operation.
     */
    private fun handleExecuteScriptConnectionRequired(
        jsonBytes: ByteArray?,
        future: CompletableFuture<ScriptExecutionResult>,
        attempt: Int
    ) {
        callbacks?.updateLogs("executeScript: Attempting to connect to BLE device")
        connectBle().thenCompose { success ->
            if (success) executeScriptWithRetry(jsonBytes, attempt)
            else CompletableFuture.failedFuture(
                IllegalStateException(
                    BLUETOOTH_NOT_CONNECTED
                )
            )
        }.thenAccept { result ->
            future.complete(result)
        }.exceptionally { throwable ->
            future.completeExceptionally(throwable)
            null
        }
    }
}
