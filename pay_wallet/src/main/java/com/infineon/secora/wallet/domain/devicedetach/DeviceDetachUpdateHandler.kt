// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: DeviceDetachUpdateHandler.kt handles FCM "Device Detach Update" notifications:
 * waits for in-flight scripts, runs pending delete scripts, cleans up BLE pairing, and navigates to the device list.
 **/
package com.infineon.secora.wallet.domain.devicedetach

import com.infineon.secora.wallet.client.util.AppDispatchers
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.firebase.EventBus
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.utils.helper.FcmSecureFlowCoordinator
import com.infineon.secora.wallet.utils.helper.FcmSecureFlowCoordinator.FlowKind
import com.infineon.secora.wallet.utils.helper.ManualDeviceDelinkGate
import com.infineon.secora.wallet.utils.helper.SecureElementScriptCoordinator
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_DEVICE_DETACH_COMPLETED
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_OPEN_DEVICE_LIST
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object DeviceDetachUpdateHandler {

    private val logger: ApplicationLogger =
        getApplicationLogger(DeviceDetachUpdateHandler::class.java.simpleName)

    /**
     * Entry point for device-detach FCM handling from [com.infineon.secora.wallet.ui.home.MainActivity].
     * Runs serialized via [FcmSecureFlowCoordinator].
     *
     * @param activity               Host activity for UI, SDK, and navigation.
     * @param scope                  Coroutine scope (typically [androidx.lifecycle.lifecycleScope]).
     * @param paymentAppInstanceId   Payment app instance ID from FCM, if present.
     * @param seIdFromNotification   Optional SE ID from the FCM payload.
     */
    fun handle(
        activity: MainActivity,
        scope: CoroutineScope,
        paymentAppInstanceId: String?,
        seIdFromNotification: String? = null
    ) {
        val deviceSeId = StorageRepository.readString(key = PreferenceKey.DEVICE_SE_ID).trim()
        FcmSecureFlowCoordinator.markDeviceDetachScheduled(
            seIdFromNotification?.trim().orEmpty().ifBlank {
                deviceSeId
            }
        )

        scope.launch {
            FcmSecureFlowCoordinator.runSerialized(FlowKind.DEVICE_DETACH) {
                processDetach(activity, paymentAppInstanceId, seIdFromNotification)
            }
        }
    }

    /**
     * Runs the full detach flow: loader, idle wait, target resolution, local cleanup,
     * pending delete scripts, BLE cleanup, and navigation to the device list.
     */
    private suspend fun processDetach(
        activity: MainActivity,
        paymentAppInstanceId: String?,
        seIdFromNotification: String?
    ) {
        logger.debug(
            "Device detach flow started paymentAppInstanceId=$paymentAppInstanceId seIdFromFcm=$seIdFromNotification"
        )

        SecureElementScriptCoordinator.awaitIdle()

        val target = DeviceDetachTargetResolver.resolve(
            context = activity,
            paymentAppInstanceId = paymentAppInstanceId,
            seIdFromNotification = seIdFromNotification
        )

        val resolvedSeId = target?.seId?.trim().orEmpty().ifBlank {
            seIdFromNotification?.trim().orEmpty()
        }
        if (ManualDeviceDelinkGate.shouldSkipFcmDeviceDetach(
                resolvedSeId,
                target?.paymentAppInstanceId ?: paymentAppInstanceId
            )
        ) {
            logger.debug(
                "Device detach FCM: skipped — user already delinked manually seId=$resolvedSeId"
            )
            return
        }

        if (target != null) {
            logger.debug(
                "Device detach target seId=${target.seId} bleAddress=${target.bleAddress} " +
                    "pid=${target.paymentAppInstanceId}"
            )
        }

        FcmSecureFlowCoordinator.acquireLoaderHold()

        try {
            clearDetachPreferences(paymentAppInstanceId)
            StorageRepository.clearAllLocalCardData(activity)

            if (target == null) {
                logger.debug("Device detach: could not resolve target device, navigating to device list")
                navigateToDeviceList()
                return
            }

            val seId = target.seId
            val displayName = DeviceDetachTargetResolver.resolveDisplayName(
                activity,
                seId,
                target.bleAddress
            )
            val targetBleConnected = BluetoothStateManager.isDeviceConnected(seId, activity)

            if (targetBleConnected) {
                val pendingResult = PendingDeleteScriptExecutor.run(
                    activity = activity,
                    seId = seId,
                    digitizationReferenceNumber = null,
                    onLoading = { /* loader owned by this handler for the full flow */ }
                )

                when (pendingResult) {
                    is PendingDeleteScriptExecutor.Result.NoScripts -> {
                        logger.debug("Device detach: no pending scripts")
                    }

                    is PendingDeleteScriptExecutor.Result.ScriptsExecuted -> {
                        logger.debug(
                            "Device detach: executed ${pendingResult.scriptCount} script(s)"
                        )
                    }

                    is PendingDeleteScriptExecutor.Result.Failed -> {
                        logger.debug("Device detach pending/scripts failed: ${pendingResult.message}, continuing cleanup")
                    }
                }
            } else {
                logger.debug(
                    "Device detach: BLE not connected for seId=$seId, skipping getPending and delete scripts"
                )
            }

            val bleHint = target.bleAddress.ifBlank {
                DeviceDetachTargetResolver.resolve(activity, paymentAppInstanceId, seIdFromNotification)
                    ?.bleAddress.orEmpty()
            }

            val cleanup = DeviceDetachBleCleanup.cleanup(activity, seId, bleHint)
            var portalDelinkSuccessMessage: String? = null
            if (cleanup != null) {
                target.paymentAppInstanceId?.let { pid ->
                    StorageRepository.clearString(PreferenceKey.paymentAppSeIdKey(pid))
                }
                postDetachCompleted(cleanup.seId, cleanup.bleAddress)
                portalDelinkSuccessMessage =
                    activity.getString(R.string.device_delinked_successfully, displayName)
            }

            logger.debug("Device detach: navigating to device list")
            navigateToDeviceList()
            portalDelinkSuccessMessage?.let { message ->
                withContext(AppDispatchers.MAIN) {
                    activity.showPortalDelinkSuccessDialog(message)
                }
            }
        } finally {
            withContext(AppDispatchers.MAIN) {
                FcmSecureFlowCoordinator.releaseLoaderHold()
                activity.showLoading(false, "")
            }
        }
    }

    /** Clears default-card and payment-app prefs tied to the detached payment app instance. */
    private fun clearDetachPreferences(paymentAppInstanceId: String?) {
        paymentAppInstanceId?.takeIf { it.isNotBlank() }?.let { pid ->
            StorageRepository.clearString(PreferenceKey.deviceKey(pid))
            StorageRepository.saveString(PreferenceKey.PAYMENT_APP_INSTANCE_ID, pid)
        }
    }

    /** Notifies [com.infineon.secora.wallet.ui.fragment.AvailableDeviceFragment] that BLE cleanup finished. */
    private fun postDetachCompleted(seId: String, bleAddress: String) {
        EventBus.post(
            ACTION_DEVICE_DETACH_COMPLETED,
            mapOf(
                BundleKey.DEVICE_SE_ID to seId,
                BundleKey.DEVICE_BLE_ADDRESS to bleAddress.takeIf { it.isNotBlank() }
            )
        )
    }

    /** Posts an event to open the wearable device list. */
    private fun navigateToDeviceList() {
        EventBus.post(ACTION_OPEN_DEVICE_LIST)
    }
}
