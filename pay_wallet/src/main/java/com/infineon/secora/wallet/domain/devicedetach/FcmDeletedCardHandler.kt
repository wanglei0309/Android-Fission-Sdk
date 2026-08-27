// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: FcmDeletedCardHandler.kt handles FCM "Card Deleted" notifications from any screen:
 * local card cleanup, pending delete scripts on the SE, and card-list refresh/navigation when appropriate.
 **/
package com.infineon.secora.wallet.domain.devicedetach

import com.infineon.secora.wallet.client.data.models.DeleteScriptResponse
import com.infineon.secora.wallet.client.util.AppDispatchers
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.ui.fragment.CardListFragment
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.utils.helper.DigitizationDeleteFlowGate
import com.infineon.secora.wallet.utils.helper.FcmBleConnectionGate
import com.infineon.secora.wallet.utils.helper.FcmBleConnectionGate.GateResult
import com.infineon.secora.wallet.utils.helper.FcmSecureFlowCoordinator
import com.infineon.secora.wallet.utils.helper.FcmSecureFlowCoordinator.FlowKind
import com.infineon.secora.wallet.utils.helper.ManualDeviceDelinkGate
import com.infineon.secora.wallet.utils.helper.PendingDeleteTaskResponseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

object FcmDeletedCardHandler {

    private val logger: ApplicationLogger =
        getApplicationLogger(FcmDeletedCardHandler::class.java.simpleName)

    private val isHandling = AtomicBoolean(false)

    /**
     * Entry point for FCM card-deleted handling from [com.infineon.secora.wallet.ui.home.MainActivity].
     * Deduplicates concurrent notifications and defers refresh until the flow lock is released.
     *
     * @param activity                     Host activity for UI, SDK, and navigation.
     * @param scope                        Coroutine scope (typically [androidx.lifecycle.lifecycleScope]).
     * @param digitizationReferenceNumber  Card digitization reference from FCM [com.infineon.secora.wallet.utils.Constants.ENTITY_ID].
     */
    fun handle(
        activity: MainActivity,
        scope: CoroutineScope,
        digitizationReferenceNumber: String?
    ) {
        val entityId = digitizationReferenceNumber?.trim().orEmpty()
        if (entityId.isEmpty() || entityId.equals("null", ignoreCase = true)) {
            logger.debug("FCM card deleted: missing entityId, ignoring")
            return
        }

        if (DigitizationDeleteFlowGate.shouldSkipCardListPendingDeleteDuplicate(entityId)) {
            logger.debug("FCM card deleted: Terms flow owns ref=$entityId, skipping pending delete")
            requestCardListRefresh(activity, entityId)
            return
        }

        if (!isHandling.compareAndSet(false, true)) {
            logger.debug("FCM card deleted: already handling, ignoring duplicate")
            return
        }

        scope.launch {
            try {
                val seId = StorageRepository.readString(key = PreferenceKey.DEVICE_SE_ID).trim()
                FcmSecureFlowCoordinator.awaitPortalBatchCoalesce(seId)

                FcmSecureFlowCoordinator.runSerialized(FlowKind.CARD_DELETED) {
                    processDeletedCard(activity, entityId)
                }
                // Refresh after this flow releases the coordinator lock (and after Device Detach
                // when both notifications arrive from a portal detach).
                withContext(AppDispatchers.MAIN) {
                    requestCardListRefresh(activity, entityId)
                }
            } finally {
                isHandling.set(false)
            }
        }
    }

    /**
     * Deletes local card data, fetches pending delete scripts, and runs them when BLE is available.
     * The reconnect dialog is shown only after getPending returns scripts and BLE is disconnected.
     */
    private suspend fun processDeletedCard(activity: MainActivity, entityId: String): Boolean {
        logger.debug("FCM card deleted flow started entityId=$entityId")

        val seId = StorageRepository.readString(key = PreferenceKey.DEVICE_SE_ID).trim()
        if (skipCardDeleteForManualDelinkOrPortalDetach(activity, entityId, seId)) {
            return true
        }

        val bleAddress = resolveBleAddressForSeId(activity, seId)
        runLocalCleanupAndPendingScripts(activity, entityId, seId, bleAddress)
        return true
    }

    /**
     * Skips FCM card-delete processing when the user already delinked manually or portal detach is active.
     * Removes the local card row before returning.
     *
     * @param activity Host activity for local DB cleanup.
     * @param entityId Digitization reference of the deleted card.
     * @param seId     Secure element ID of the target device.
     * @return `true` when the flow should stop after local cleanup.
     */
    private suspend fun skipCardDeleteForManualDelinkOrPortalDetach(
        activity: MainActivity,
        entityId: String,
        seId: String
    ): Boolean {
        if (seId.isEmpty()) return false
        if (ManualDeviceDelinkGate.shouldSkipPostManualDelinkFcm(seId, null)) {
            StorageRepository.deleteLocalCardByDigitizeRef(activity, entityId)
            logger.debug("FCM card deleted: skipped — manual delink already handled seId=$seId")
            return true
        }
        if (FcmSecureFlowCoordinator.isPortalDeviceDetachActive(seId)) {
            StorageRepository.deleteLocalCardByDigitizeRef(activity, entityId)
            logger.debug(
                "FCM card deleted: portal device detach active, skipping reconnect/scripts seId=$seId"
            )
            return true
        }
        return false
    }

    /**
     * Resolves the BLE MAC for [seId] from the detach resolver or stored preferences.
     *
     * @param activity Host activity for SDK resolution and prefs.
     * @param seId     Secure element ID of the target device.
     */
    private fun resolveBleAddressForSeId(activity: MainActivity, seId: String): String {
        if (seId.isEmpty()) return ""
        return DeviceDetachTargetResolver.resolve(activity, null, seId)?.bleAddress.orEmpty()
            .ifBlank {
                StorageRepository.readString(key = PreferenceKey.bleAddressKey(seId)).trim()
            }
    }

    /**
     * Deletes the local card row, calls getPending, and runs delete scripts when scripts exist.
     * Shows the BLE reconnect dialog only after getPending returns scripts and the wearable is disconnected.
     */
    private suspend fun runLocalCleanupAndPendingScripts(
        activity: MainActivity,
        entityId: String,
        seId: String,
        bleAddress: String
    ) {
        FcmSecureFlowCoordinator.acquireLoaderHold()
        try {
            val paymentId = StorageRepository.readString(key = PreferenceKey.PAYMENT_APP_INSTANCE_ID).trim()
            val cardCountBeforeDelete = if (paymentId.isNotEmpty()) {
                StorageRepository.getUiCardListFromLocalDb(activity, paymentId).size
            } else {
                0
            }
            CardListFragment.onDefaultCardDeleted(entityId, cardCountBeforeDelete)
            StorageRepository.deleteLocalCardByDigitizeRef(activity, entityId)
            if (seId.isEmpty()) {
                logger.debug("FCM card deleted: no seId, local cleanup only")
                return
            }

            when (
                val fetchResult = PendingDeleteScriptExecutor.fetchPendingScripts(
                    activity = activity,
                    seId = seId,
                    digitizationReferenceNumber = entityId,
                    onLoading = { /* loader owned by this handler */ }
                )
            ) {
                is PendingDeleteScriptExecutor.FetchResult.NoScripts -> {
                    logger.debug("FCM card deleted: no scripts from getPending")
                }

                is PendingDeleteScriptExecutor.FetchResult.Failed -> {
                    logPendingDeleteFetchFailure(fetchResult.message)
                }

                is PendingDeleteScriptExecutor.FetchResult.HasScripts -> {
                    runPendingScriptsWhenBleReady(
                        activity = activity,
                        seId = seId,
                        bleAddress = bleAddress,
                        scripts = fetchResult.scripts
                    )
                }
            }
        } finally {
            withContext(AppDispatchers.MAIN) {
                FcmSecureFlowCoordinator.releaseLoaderHold()
                activity.showLoading(false, "")
            }
        }
    }

    /**
     * Runs pending delete scripts when BLE is already connected, or prompts the user to reconnect first.
     * On decline or reconnect failure, skips script execution; the card list refresh runs afterward.
     */
    private suspend fun runPendingScriptsWhenBleReady(
        activity: MainActivity,
        seId: String,
        bleAddress: String,
        scripts: List<DeleteScriptResponse>
    ) {
        if (BluetoothStateManager.isDeviceConnected(seId, activity)) {
            logPendingDeleteResult(
                PendingDeleteScriptExecutor.executeScripts(
                    activity = activity,
                    seId = seId,
                    scripts = scripts,
                    onLoading = { /* loader owned by this handler */ }
                )
            )
            return
        }

        when (FcmBleConnectionGate.ensureBleConnected(activity, seId, bleAddress)) {
            GateResult.UserDeclinedReconnect -> {
                logger.debug("FCM card deleted: user tapped No on reconnect dialog, skipping scripts")
            }

            GateResult.Aborted, GateResult.Failed -> {
                logBleGateEndedWithoutScripts(seId)
            }

            GateResult.Proceed -> {
                if (BluetoothStateManager.isDeviceConnected(seId, activity)) {
                    logPendingDeleteResult(
                        PendingDeleteScriptExecutor.executeScripts(
                            activity = activity,
                            seId = seId,
                            scripts = scripts,
                            onLoading = { /* loader owned by this handler */ }
                        )
                    )
                } else {
                    logger.debug(
                        "FCM card deleted: BLE not connected after reconnect, skipping scripts seId=$seId"
                    )
                }
            }
        }
    }

    /**
     * Logs a failed getPending response for FCM card-delete flows.
     */
    private fun logPendingDeleteFetchFailure(message: String) {
        if (PendingDeleteTaskResponseHelper.isNoPendingDeleteTaskMessage(message)) {
            logger.debug("FCM card deleted: no pending task (already deleted on SE)")
        } else {
            logger.debug("FCM card deleted getPending failed: $message")
        }
    }

    /**
     * Logs why the BLE gate ended without running pending delete scripts.
     */
    private fun logBleGateEndedWithoutScripts(seId: String) {
        if (FcmSecureFlowCoordinator.isPortalDeviceDetachActive(seId)) {
            logger.debug("FCM card deleted: preempted by portal device detach seId=$seId")
        } else {
            logger.debug("FCM card deleted: BLE gate ended without reconnect")
        }
    }

    /**
     * Logs the outcome of [PendingDeleteScriptExecutor.executeScripts] for FCM card-delete flows.
     *
     * @param result Result from pending delete script execution.
     */
    private fun logPendingDeleteResult(result: PendingDeleteScriptExecutor.Result) {
        when (result) {
            is PendingDeleteScriptExecutor.Result.NoScripts -> {
                logger.debug("FCM card deleted: no scripts / already removed on SE")
            }

            is PendingDeleteScriptExecutor.Result.ScriptsExecuted -> {
                logger.debug("FCM card deleted: executed ${result.scriptCount} script(s)")
            }

            is PendingDeleteScriptExecutor.Result.Failed -> {
                if (PendingDeleteTaskResponseHelper.isNoPendingDeleteTaskMessage(result.message)) {
                    logger.debug("FCM card deleted: no pending task (already deleted on SE)")
                } else {
                    logger.debug("FCM card deleted pending/scripts failed: ${result.message}")
                }
            }
        }
    }

    /**
     * Triggers card-list API refresh and pops device detail when the deleted card is on screen.
     * Must run on the main thread after [FcmSecureFlowCoordinator] releases the lock.
     */
    private fun requestCardListRefresh(activity: MainActivity, entityId: String) {
        CardListFragment.shouldForceApiRefresh = true
        activity.signalCardListRefreshAfterFcmDeleted()
        activity.navigateToCardListAfterFcmCardDeletedIfOnDetailForCard(entityId)
        logger.debug("FCM card deleted: requested card list refresh")
    }
}
