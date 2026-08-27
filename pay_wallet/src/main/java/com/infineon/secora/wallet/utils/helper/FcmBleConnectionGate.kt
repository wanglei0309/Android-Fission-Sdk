// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: FcmBleConnectionGate.kt prompts the user to reconnect BLE before FCM-driven
 * secure-element flows when the target wearable is disconnected.
 **/
package com.infineon.secora.wallet.utils.helper

import android.Manifest
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.DialogCommonMessageBinding
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wearable.ble.BleProtocol
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

object FcmBleConnectionGate {

    private val logger: ApplicationLogger =
        getApplicationLogger(FcmBleConnectionGate::class.java.simpleName)

    private const val GATE_DECISION_TTL_MS = 120_000L

    private val isDialogVisible = AtomicBoolean(false)

    @Volatile
    private var activeReconnectDialog: Dialog? = null

    @Volatile
    private var activeGateContinuation: CancellableContinuation<GateResult>? = null

    private data class CachedGateDecision(
        val result: GateResult,
        val recordedAtMs: Long
    )

    /** Per-seId successful reconnect only; negative outcomes are not cached so each FCM event can re-prompt. */
    private val cachedDecisions = ConcurrentHashMap<String, CachedGateDecision>()

    /** Result of the BLE gate before an FCM detach/delete flow continues. */
    enum class GateResult {
        /** Target device is already connected, or user reconnected successfully. */
        Proceed,

        /** User tapped No on the reconnect dialog. */
        UserDeclinedReconnect,

        /** BLE unavailable (off, no address, etc.) — dialog was not shown. */
        Aborted,

        /** User tapped Yes but reconnect failed. */
        Failed,
    }

    /**
     * Ensures BLE is connected for [seId] before running pending delete scripts.
     * Shows a reconnect dialog when the target wearable is disconnected.
     *
     * @param activity   Host activity for dialogs and BLE connection.
     * @param seId       Secure element ID of the notification target device.
     * @param bleAddress BLE MAC for [seId]; when blank, reconnect cannot proceed.
     * @return [GateResult.Proceed] to continue the FCM flow, otherwise abort script execution.
     */
    suspend fun ensureBleConnected(
        activity: MainActivity,
        seId: String,
        bleAddress: String
    ): GateResult {
        val targetSeId = seId.trim()
        if (FcmSecureFlowCoordinator.isPortalDeviceDetachActive(targetSeId)) {
            logger.debug("FCM BLE gate: portal device detach active, skipping reconnect seId=$targetSeId")
            return GateResult.Aborted
        }
        resolvePreConnectGateResult(activity, targetSeId)?.let { return it }

        val address = resolveBleAddress(targetSeId, bleAddress)
        if (address.isEmpty()) {
            logger.debug("FCM BLE gate: no BLE address for seId=$targetSeId")
            return GateResult.Aborted
        }

        return awaitUserReconnectDecision(activity, targetSeId, address)
    }

    /**
     * Dismisses an in-flight reconnect dialog so Device Detach can run first during portal detach.
     *
     * @param seId Secure element ID of the portal detach target; used for logging only.
     */
    fun cancelForPortalDeviceDetach(seId: String?) {
        val targetSeId = seId?.trim().orEmpty()
        if (targetSeId.isEmpty()) return
        logger.debug("FCM BLE gate: cancelling reconnect dialog for portal detach seId=$targetSeId")
        val continuation = activeGateContinuation
        activeGateContinuation = null
        resumeGate(continuation, GateResult.Aborted)
        val dialog = activeReconnectDialog
        activeReconnectDialog = null
        if (dialog == null) {
            isDialogVisible.set(false)
            return
        }
        val context = dialog.context
        if (context is MainActivity) {
            context.runOnUiThread {
                dismissReconnectDialog(dialog)
            }
        } else {
            dismissReconnectDialog(dialog)
        }
    }

    /**
     * Dismisses the reconnect dialog and clears the visible-dialog flag.
     *
     * @param dialog The active reconnect [Dialog], if any.
     */
    private fun dismissReconnectDialog(dialog: Dialog) {
        try {
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        } catch (_: Exception) {
            // best-effort dismiss
        } finally {
            isDialogVisible.set(false)
        }
    }

    /**
     * Dismisses the tracked reconnect dialog when the gate coroutine is cancelled.
     */
    private fun activityRunOnMainThreadDismiss() {
        val dialog = activeReconnectDialog
        activeReconnectDialog = null
        dismissReconnectDialog(dialog ?: return)
    }

    /**
     * Resolves an early gate outcome before showing the reconnect dialog (already connected,
     * Bluetooth off, or a valid cached decision).
     *
     * @param activity    Host activity for Bluetooth state checks.
     * @param targetSeId  Secure element ID of the FCM target device.
     * @return A terminal [GateResult], or `null` when the reconnect dialog should be shown.
     */
    private fun resolvePreConnectGateResult(
        activity: MainActivity,
        targetSeId: String
    ): GateResult? {
        if (targetSeId.isEmpty()) {
            return GateResult.Proceed
        }
        if (!BluetoothStateManager.isBluetoothTurnedOn(activity)) {
            logger.debug("FCM BLE gate: Bluetooth is off for seId=$targetSeId")
            return GateResult.Aborted
        }
        if (BluetoothStateManager.isDeviceConnected(targetSeId, activity)) {
            logger.debug("FCM BLE gate: target already connected seId=$targetSeId")
            clearCachedDecision(targetSeId)
            return GateResult.Proceed
        }
        getCachedDecision(targetSeId)?.let { cached ->
            // Stale Proceed must not skip the reconnect dialog when the device has since disconnected.
            if (cached == GateResult.Proceed) {
                logger.debug(
                    "FCM BLE gate: ignoring stale Proceed cache — target not connected seId=$targetSeId"
                )
                clearCachedDecision(targetSeId)
                return null
            }
            logger.debug(
                "FCM BLE gate: reusing cached decision=$cached for seId=$targetSeId"
            )
            return cached
        }
        return null
    }

    /**
     * Resolves the BLE MAC for [targetSeId], preferring the FCM payload and falling back to prefs.
     *
     * @param targetSeId  Secure element ID of the target device.
     * @param bleAddress  BLE address from the notification, if any.
     * @return Trimmed MAC address, or empty when unknown.
     */
    private fun resolveBleAddress(
        targetSeId: String,
        bleAddress: String
    ): String {
        val trimmed = bleAddress.trim()
        if (trimmed.isNotEmpty()) return trimmed
        return StorageRepository.readString(PreferenceKey.bleAddressKey(targetSeId)).trim()
    }

    /**
     * Shows the reconnect dialog and suspends until the user accepts, declines, or the gate is cancelled.
     *
     * @param activity    Host activity for the dialog.
     * @param targetSeId  Secure element ID of the target device.
     * @param address     BLE MAC used when the user taps Yes.
     * @return [GateResult] from the user's choice or a pre-connect abort.
     */
    private suspend fun awaitUserReconnectDecision(
        activity: MainActivity,
        targetSeId: String,
        address: String
    ): GateResult = suspendCancellableCoroutine { continuation ->
        activeGateContinuation = continuation
        continuation.invokeOnCancellation {
            clearActiveGateContinuation(continuation)
            activityRunOnMainThreadDismiss()
        }
        showReconnectDialog(
            activity = activity,
            onYes = { handleReconnectAccepted(activity, targetSeId, address, continuation) },
            onNo = { handleReconnectDialogNo(targetSeId, continuation) },
            onUnavailable = { handleReconnectDialogUnavailable(continuation) }
        )
    }

    /**
     * Clears the tracked gate continuation when it matches the active dialog flow.
     *
     * @param continuation Suspend continuation from [awaitUserReconnectDecision].
     */
    private fun clearActiveGateContinuation(continuation: CancellableContinuation<GateResult>) {
        if (activeGateContinuation === continuation) {
            activeGateContinuation = null
        }
    }

    /**
     * Handles the user tapping Yes on the FCM reconnect dialog.
     *
     * @param activity    Host activity for loading UI and BLE connect.
     * @param targetSeId  Secure element ID of the notification target device.
     * @param address     BLE MAC to reconnect to.
     * @param continuation Suspend continuation to resume with the gate outcome.
     */
    private fun handleReconnectAccepted(
        activity: MainActivity,
        targetSeId: String,
        address: String,
        continuation: CancellableContinuation<GateResult>
    ) {
        clearActiveGateContinuation(continuation)
        activity.lifecycleScope.launch {
            val connected = reconnectToTarget(activity, targetSeId, address)
            if (connected) {
                cacheDecision(targetSeId, GateResult.Proceed)
                resumeGate(continuation, GateResult.Proceed)
            } else {
                awaitReconnectFailedAck(activity)
                resumeGate(continuation, GateResult.Failed)
            }
        }
    }

    /**
     * Handles the user tapping No on the FCM reconnect dialog.
     *
     * @param targetSeId   Secure element ID of the notification target device.
     * @param continuation Suspend continuation to resume with [GateResult.UserDeclinedReconnect].
     */
    private fun handleReconnectDialogNo(
        targetSeId: String,
        continuation: CancellableContinuation<GateResult>
    ) {
        clearActiveGateContinuation(continuation)
        handleReconnectDeclined(targetSeId, continuation)
    }

    /**
     * Handles the reconnect dialog not being shown (already visible or activity finishing).
     *
     * @param continuation Suspend continuation to resume with [GateResult.Aborted].
     */
    private fun handleReconnectDialogUnavailable(
        continuation: CancellableContinuation<GateResult>
    ) {
        clearActiveGateContinuation(continuation)
        resumeGate(continuation, GateResult.Aborted)
    }

    /**
     * Suspends until the user dismisses the reconnect-failed status dialog (OK).
     */
    private suspend fun awaitReconnectFailedAck(activity: MainActivity) {
        suspendCancellableCoroutine { continuation ->
            showReconnectFailedDialog(activity) {
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
        }
    }

    /**
     * One-button status dialog for a failed reconnect attempt.
     */
    private fun showReconnectFailedDialog(
        activity: MainActivity,
        onOk: () -> Unit
    ) {
        activity.runOnUiThread {
            if (activity.isFinishing) return@runOnUiThread
            val dialogViewBinding = DialogCommonMessageBinding.inflate(LayoutInflater.from(activity))
            val dialog = Dialog(activity).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(dialogViewBinding.root)
                setCancelable(false)
            }
            dialogViewBinding.txtTitle.text = activity.getString(R.string.text_secora_wallet)
            dialogViewBinding.txtMessage.text =
                activity.getString(R.string.bluetooth_reconnect_failed)
            dialogViewBinding.txtOK.text = activity.getString(R.string.ok)
            dialogViewBinding.txtCancel.visibility = View.GONE
            dialogViewBinding.txtOK.setOnClickListener {
                dialog.dismiss()
                onOk()
            }
            dialog.showSecure()
        }
    }

    /**
     * Caches and resumes with [GateResult.UserDeclinedReconnect] when the user taps No.
     *
     * @param targetSeId    Secure element ID of the target device.
     * @param continuation  Coroutine to resume with the declined outcome.
     */
    private fun handleReconnectDeclined(
        targetSeId: String,
        continuation: CancellableContinuation<GateResult>
    ) {
        cacheDecision(targetSeId, GateResult.UserDeclinedReconnect)
        resumeGate(continuation, GateResult.UserDeclinedReconnect)
    }

    /**
     * Resumes the gate coroutine when still active.
     *
     * @param continuation  Suspend continuation from [awaitUserReconnectDecision].
     * @param result        Outcome to deliver to the caller of [ensureBleConnected].
     */
    private fun resumeGate(
        continuation: CancellableContinuation<GateResult>?,
        result: GateResult
    ) {
        if (continuation != null && continuation.isActive) {
            continuation.resume(result)
        }
    }

    /**
     * Stores SE ID and BLE address in preferences before attempting reconnect.
     *
     * @param context    Application or activity context for prefs.
     * @param seId       Secure element ID of the target device.
     * @param bleAddress BLE MAC to persist for script execution.
     */
    private fun prepareDeviceContext(seId: String, bleAddress: String) {
        StorageRepository.saveString(PreferenceKey.DEVICE_SE_ID, seId)
        val address = bleAddress.trim()
        if (address.isNotEmpty()) {
            StorageRepository.saveString(PreferenceKey.bleAddressKey(seId), address)
            StorageRepository.saveString(PreferenceKey.SELECTED_DEVICE_ADDRESS, address)
        }
    }

    /**
     * Shows the wearable reconnect prompt on the main thread (FCM card-deleted flow only).
     *
     * @param activity       Host activity; must not be finishing.
     * @param onYes          Invoked when the user taps Yes (dialog is dismissed first).
     * @param onNo           Invoked when the user taps No.
     * @param onUnavailable  Invoked when a dialog is already visible or the activity is finishing.
     */
    private fun showReconnectDialog(
        activity: MainActivity,
        onYes: () -> Unit,
        onNo: () -> Unit,
        onUnavailable: () -> Unit
    ) {
        if (!isDialogVisible.compareAndSet(false, true)) {
            onUnavailable()
            return
        }

        activity.runOnUiThread {
            if (activity.isFinishing) {
                isDialogVisible.set(false)
                onUnavailable()
                return@runOnUiThread
            }

            val dialogViewBinding = DialogCommonMessageBinding.inflate(LayoutInflater.from(activity))
            val dialog = Dialog(activity).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(dialogViewBinding.root)
                setCancelable(false)
            }

            dialogViewBinding.txtTitle.text = activity.getString(R.string.text_secora_wallet)
            dialogViewBinding.txtMessage.text =
                activity.getString(R.string.wearable_device_disconnected_reconnect_prompt)
            dialogViewBinding.txtOK.text = activity.getString(R.string.text_yes)
            dialogViewBinding.txtCancel.text = activity.getString(R.string.text_no)
            dialogViewBinding.txtCancel.visibility = View.VISIBLE

            fun dismissAndReset() {
                if (activeReconnectDialog === dialog) {
                    activeReconnectDialog = null
                }
                if (dialog.isShowing) dialog.dismiss()
                isDialogVisible.set(false)
            }

            activeReconnectDialog = dialog

            dialogViewBinding.txtOK.setOnClickListener {
                dismissAndReset()
                onYes()
            }

            dialogViewBinding.txtCancel.setOnClickListener {
                dismissAndReset()
                onNo()
            }

            dialog.setOnDismissListener {
                if (activeReconnectDialog === dialog) {
                    activeReconnectDialog = null
                }
                isDialogVisible.set(false)
            }

            dialog.showSecure()
        }
    }

    /**
     * Disconnects other wearables, connects to [bleAddress], and tracks the active protocol.
     *
     * @param activity    Host activity for permissions, loading, and BLE connect.
     * @param seId        Secure element ID of the target device.
     * @param bleAddress  BLE MAC to connect to.
     * @return `true` when GATT connect succeeds within the timeout.
     */
    private suspend fun reconnectToTarget(
        activity: MainActivity,
        seId: String,
        bleAddress: String
    ): Boolean = suspendCancellableCoroutine { continuation ->
        if (!hasBluetoothConnectPermission(activity)) {
            resumeBoolean(continuation, false)
            return@suspendCancellableCoroutine
        }

        prepareDeviceContext(seId, bleAddress)
        disconnectOtherDevices()
        activity.showLoading(true, activity.getString(R.string.bluetooth_reconnecting))

        val device = obtainRemoteDevice(activity, bleAddress, continuation)
            ?: return@suspendCancellableCoroutine

        startBleReconnect(activity, seId, bleAddress, device, continuation)
    }

    /**
     * Obtains a [BluetoothDevice] for [bleAddress] when Bluetooth is available.
     *
     * @param activity      Host activity for the Bluetooth manager.
     * @param bleAddress    BLE MAC to resolve.
     * @param continuation  Coroutine to resume with `false` on adapter or parse failure.
     * @return Remote device, or `null` when resolution fails.
     */
    private fun obtainRemoteDevice(
        activity: MainActivity,
        bleAddress: String,
        continuation: CancellableContinuation<Boolean>
    ): BluetoothDevice? {
        val adapter = activity.getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null) {
            hideReconnectLoading(activity)
            resumeBoolean(continuation, false)
            return null
        }
        return parseRemoteDevice(activity, adapter, bleAddress, continuation)
    }

    /**
     * Parses [bleAddress] into a [BluetoothDevice], handling invalid MAC errors.
     *
     * @param activity      Host activity for hiding loading on failure.
     * @param adapter       System Bluetooth adapter.
     * @param bleAddress    BLE MAC to parse.
     * @param continuation  Coroutine to resume with `false` when the address is invalid.
     * @return Remote device, or `null` on [IllegalArgumentException].
     */
    private fun parseRemoteDevice(
        activity: MainActivity,
        adapter: BluetoothAdapter,
        bleAddress: String,
        continuation: CancellableContinuation<Boolean>
    ): BluetoothDevice? = try {
        adapter.getRemoteDevice(bleAddress)
    } catch (e: IllegalArgumentException) {
        logger.debug("FCM BLE reconnect: invalid address $bleAddress — ${e.message}")
        hideReconnectLoading(activity)
        resumeBoolean(continuation, false)
        null
    }

    /**
     * Starts GATT connect for [device] and resumes [continuation] on the main thread.
     *
     * @param activity      Host activity for loading UI.
     * @param seId          Secure element ID to register on success.
     * @param bleAddress    BLE MAC of [device].
     * @param device        Remote device to connect to.
     * @param continuation  Coroutine to resume with connect success or failure.
     */
    private fun startBleReconnect(
        activity: MainActivity,
        seId: String,
        bleAddress: String,
        device: BluetoothDevice,
        continuation: CancellableContinuation<Boolean>
    ) {
        BluetoothStateManager.connectBleDevice(activity, device)
            .orTimeout(25, TimeUnit.SECONDS)
            .whenComplete { protocol, throwable ->
                activity.runOnUiThread {
                    hideReconnectLoading(activity)
                    val success = applyReconnectOutcome(seId, bleAddress, protocol, throwable)
                    resumeBoolean(continuation, success)
                }
            }
    }

    /**
     * Updates [BluetoothStateManager] from the async connect result.
     *
     * @param seId         Secure element ID of the target device.
     * @param bleAddress   BLE MAC that was connected.
     * @param protocol     Active GATT protocol on success; `null` on failure.
     * @param throwable    Connect error, if any.
     * @return `true` when [protocol] is non-null and no error occurred.
     */
    private fun applyReconnectOutcome(
        seId: String,
        bleAddress: String,
        protocol: BleProtocol?,
        throwable: Throwable?
    ): Boolean {
        val success = throwable == null && protocol != null
        if (success) {
            BluetoothStateManager.setActiveProtocol(protocol)
            BluetoothStateManager.addConnectedDevice(bleAddress, seId)
            logger.debug("FCM BLE reconnect: connected seId=$seId")
        } else {
            BluetoothStateManager.disconnectActiveProtocol()
            logger.debug("FCM BLE reconnect failed seId=$seId: ${throwable?.message}")
        }
        return success
    }

    /**
     * Hides the FCM reconnect loading overlay on [activity].
     *
     * @param activity Host activity that owns the loader.
     */
    private fun hideReconnectLoading(activity: MainActivity) {
        activity.showLoading(false, "")
    }

    /**
     * Resumes a boolean reconnect continuation when still active.
     *
     * @param continuation  Suspend continuation from [reconnectToTarget].
     * @param value         Connect success flag.
     */
    private fun resumeBoolean(
        continuation: CancellableContinuation<Boolean>,
        value: Boolean
    ) {
        if (continuation.isActive) {
            continuation.resume(value)
        }
    }

    /** Disconnects any active GATT session and clears connected-device tracking. */
    private fun disconnectOtherDevices() {
        BluetoothStateManager.disconnectActiveProtocol()
        BluetoothStateManager.clearAllConnectedDevices()
    }

    /**
     * Returns whether [Manifest.permission.BLUETOOTH_CONNECT] is granted.
     *
     * @param context Context used for the permission check.
     */
    private fun hasBluetoothConnectPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Caches a successful reconnect for [seId] within the TTL.
     * Declines and reconnect failures are not cached so each new card-delete FCM can show the dialog again.
     *
     * @param seId    Secure element ID of the target device.
     * @param result  User or system decision to reuse.
     */
    private fun cacheDecision(seId: String, result: GateResult) {
        val key = seId.trim()
        if (key.isEmpty() || result != GateResult.Proceed) return
        cachedDecisions[key] = CachedGateDecision(result, System.currentTimeMillis())
    }

    /**
     * Returns a non-expired cached gate outcome for [seId], if any.
     *
     * @param seId  Secure element ID of the target device.
     */
    private fun getCachedDecision(seId: String): GateResult? {
        val key = seId.trim()
        if (key.isEmpty()) return null
        val entry = cachedDecisions[key] ?: return null
        if (System.currentTimeMillis() - entry.recordedAtMs > GATE_DECISION_TTL_MS) {
            cachedDecisions.remove(key)
            return null
        }
        return entry.result
    }

    /**
     * Clears the cached gate outcome for [seId] (e.g. when the device disconnects).
     *
     * @param seId  Secure element ID of the target device.
     */
    private fun clearCachedDecision(seId: String) {
        val key = seId.trim()
        if (key.isNotEmpty()) {
            cachedDecisions.remove(key)
        }
    }
}
