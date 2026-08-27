// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: PendingDeleteScriptExecutor.kt fetches pending delete scripts from the wallet SDK,
 * runs them sequentially over BLE, and acknowledges each script for device-detach and card-deleted flows.
 **/
package com.infineon.secora.wallet.domain.devicedetach

import android.app.Activity
import com.infineon.secora.wallet.client.data.models.DeleteScriptResponse
import com.infineon.secora.wallet.domain.walletsdk.WalletRepository
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.domain.wearable.ble.script.ScriptHandler
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.utils.CommonResponse
import com.infineon.secora.wallet.utils.helper.PendingDeleteScriptExecutionGate
import com.infineon.secora.wallet.utils.helper.PendingDeleteTaskResponseHelper
import com.infineon.secora.wallet.utils.helper.ScriptDataParser
import com.infineon.secora.wallet.utils.helper.SecureElementScriptCoordinator
import com.infineon.secora.wallet.utils.helper.SequenceCounterHelper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object PendingDeleteScriptExecutor {

    private val logger: ApplicationLogger =
        getApplicationLogger(PendingDeleteScriptExecutor::class.java.simpleName)

    /** Outcome of a pending delete script run. */
    sealed class Result {
        data object NoScripts : Result()
        data class ScriptsExecuted(val scriptCount: Int) : Result()
        data class Failed(val message: String) : Result()
    }

    /** Outcome of a getPending fetch without script execution. */
    sealed class FetchResult {
        data object NoScripts : FetchResult()
        data class HasScripts(val scripts: List<DeleteScriptResponse>) : FetchResult()
        data class Failed(val message: String) : FetchResult()
    }

    /**
     * Fetches pending delete tasks and executes delete scripts on the secure element.
     * Requires BLE to be connected before calling getPending (device-detach flow).
     * Shows loading via [onLoading] unless the caller owns the loader for the full flow.
     *
     * @param activity                     Host activity for SDK and script execution.
     * @param seId                         Secure element ID of the target device.
     * @param digitizationReferenceNumber  Optional card reference to scope the pending task.
     * @param onLoading                    Called with true/false around the pending-task fetch and script run.
     * @return [Result.NoScripts], [Result.ScriptsExecuted], or [Result.Failed].
     */
    suspend fun run(
        activity: MainActivity,
        seId: String,
        digitizationReferenceNumber: String? = null,
        onLoading: (Boolean) -> Unit = { activity.showLoading(it, "") }
    ): Result {
        if (!BluetoothStateManager.isDeviceConnected(seId, activity)) {
            logger.debug(
                "Pending delete: target BLE not connected, skipping getPending and scripts seId=$seId"
            )
            return Result.NoScripts
        }

        onLoading(true)
        return try {
            when (val fetchResult = fetchPendingDeleteScripts(
                activity = activity,
                seId = seId,
                digitizationReferenceNumber = digitizationReferenceNumber
            )) {
                is FetchResult.NoScripts -> Result.NoScripts
                is FetchResult.Failed -> Result.Failed(fetchResult.message)
                is FetchResult.HasScripts -> {
                    if (!BluetoothStateManager.isDeviceConnected(seId, activity)) {
                        logger.debug(
                            "Pending delete: BLE disconnected after getPending, skipping script execution seId=$seId"
                        )
                        Result.NoScripts
                    } else {
                        executeScripts(
                            activity = activity,
                            seId = seId,
                            scripts = fetchResult.scripts,
                            onLoading = { /* loader owned by run() */ }
                        )
                    }
                }
            }
        } finally {
            onLoading(false)
        }
    }

    /**
     * Calls getPending without requiring BLE (FCM card-deleted flow).
     * The caller decides when to prompt for reconnect before [executeScripts].
     *
     * @param activity                     Host activity for SDK calls.
     * @param seId                         Secure element ID of the target device.
     * @param digitizationReferenceNumber  Optional card reference to scope the pending task.
     * @param onLoading                    Called with true/false around the getPending API call.
     */
    suspend fun fetchPendingScripts(
        activity: MainActivity,
        seId: String,
        digitizationReferenceNumber: String? = null,
        onLoading: (Boolean) -> Unit = { activity.showLoading(it, "") }
    ): FetchResult {
        onLoading(true)
        return try {
            fetchPendingDeleteScripts(
                activity = activity,
                seId = seId,
                digitizationReferenceNumber = digitizationReferenceNumber
            )
        } finally {
            onLoading(false)
        }
    }

    /**
     * Runs pre-fetched delete scripts over BLE and acknowledges each script.
     * Requires the target wearable to be connected before calling.
     *
     * @param activity   Host activity for SDK and script execution.
     * @param seId       Secure element ID of the target device.
     * @param scripts    Delete scripts returned from [fetchPendingScripts].
     * @param onLoading  Called with true/false around script execution.
     */
    suspend fun executeScripts(
        activity: MainActivity,
        seId: String,
        scripts: List<DeleteScriptResponse>,
        onLoading: (Boolean) -> Unit = { activity.showLoading(it, "") }
    ): Result {
        if (scripts.isEmpty()) return Result.NoScripts
        if (!BluetoothStateManager.isDeviceConnected(seId, activity)) {
            logger.debug(
                "Pending delete: target BLE not connected, skipping script execution seId=$seId"
            )
            return Result.NoScripts
        }

        onLoading(true)
        return try {
            SecureElementScriptCoordinator.awaitIdle()
            if (PendingDeleteScriptExecutionGate.isInProgress(seId)) {
                logger.debug("Pending delete skipped; already running for seId=$seId")
                return Result.NoScripts
            }
            if (!PendingDeleteScriptExecutionGate.tryBegin(seId)) {
                return Result.NoScripts
            }
            try {
                executeDeleteScripts(activity, seId, scripts)
            } finally {
                PendingDeleteScriptExecutionGate.end(seId)
            }
        } finally {
            onLoading(false)
        }
    }

    /**
     * Resolves pending delete scripts from the wallet SDK without executing them.
     */
    private suspend fun fetchPendingDeleteScripts(
        activity: MainActivity,
        seId: String,
        digitizationReferenceNumber: String?
    ): FetchResult {
        val sequenceCounter = SequenceCounterHelper.resolveSequenceCounter(activity)
        logger.debug("Pending delete fetch: sequenceCounter=$sequenceCounter seId=$seId")

        val pendingResult = WalletRepository.getPendingTask(
            context = activity,
            seId = seId,
            digitizationReferenceNumber = digitizationReferenceNumber,
            currentSequenceCounter = sequenceCounter
        )

        if (!pendingResult.isSuccess) {
            return FetchResult.Failed(pendingResult.errorMessage)
        }

        val response = pendingResult.response
        if (response == null || response.statusMessage.isNullOrBlank()) {
            return FetchResult.Failed(
                pendingResult.errorMessage.ifBlank { "Empty pending task response" }
            )
        }

        if (response.statusMessage != CommonResponse.SUCCESS.response) {
            if (digitizationReferenceNumber != null &&
                PendingDeleteTaskResponseHelper.isNoPendingDeleteTask(response)
            ) {
                logger.debug("Pending delete: no task for ref (already deleted on SE)")
                return FetchResult.NoScripts
            }
            return FetchResult.Failed(response.statusMessage.orEmpty())
        }

        if (response.deleteScriptList.isEmpty()) {
            logger.debug("Pending delete: deleteScriptList empty")
            return FetchResult.NoScripts
        }

        return FetchResult.HasScripts(response.deleteScriptList)
    }

    /**
     * Runs each delete script over BLE and acknowledges successful executions.
     *
     * @param activity  Host activity for SDK acknowledge calls.
     * @param seId      Secure element ID of the target device.
     * @param scripts   Pending delete scripts from getPending.
     * @return [Result.ScriptsExecuted] with the number of scripts that ran successfully.
     */
    private suspend fun executeDeleteScripts(
        activity: MainActivity,
        seId: String,
        scripts: List<DeleteScriptResponse>
    ): Result {
        val scriptHandler = ScriptHandler(activity.applicationContext, createSilentCallbacks())
        var executedCount = 0

        for ((index, script) in scripts.withIndex()) {
            val scriptData = script.scriptData
            if (scriptData.isNullOrBlank()) {
                logger.debug("Device detach script ${index + 1}: empty scriptData, skipping execution")
                acknowledgeIfPossible(activity, seId, script)
                continue
            }

            val jsonBytes = ScriptDataParser.decodeToJsonBytes(scriptData)
            if (jsonBytes == null) {
                logger.debug("Device detach script ${index + 1}: could not decode scriptData")
                acknowledgeIfPossible(activity, seId, script)
                continue
            }

            val success = runDeleteScript(scriptHandler, jsonBytes)
            logger.debug("Device detach script ${index + 1}/${scripts.size} success=$success")
            if (success) {
                executedCount++
                acknowledgeIfPossible(activity, seId, script)
            }
        }

        return Result.ScriptsExecuted(executedCount)
    }

    /**
     * Executes a single delete script via [ScriptHandler].
     *
     * @param scriptHandler  Handler bound to the host activity context.
     * @param jsonBytes      Decoded script payload.
     * @return `true` when deleteScript completes without error.
     */
    private suspend fun runDeleteScript(
        scriptHandler: ScriptHandler,
        jsonBytes: ByteArray
    ): Boolean = suspendCancellableCoroutine { continuation ->
        scriptHandler.deleteScript(jsonBytes).whenComplete { success, throwable ->
            if (continuation.isActive) {
                continuation.resume(success == true && throwable == null)
            }
        }
    }

    /**
     * Acknowledges a pending delete task when script id and digitization ref are present.
     *
     * @param activity  Context for the acknowledge API call.
     * @param seId      Secure element ID of the target device.
     * @param script    Executed or skipped script entry from getPending.
     */
    private suspend fun acknowledgeIfPossible(
        activity: Activity,
        seId: String,
        script: DeleteScriptResponse
    ) {
        val scriptId = script.scriptId ?: return
        val digitizeRef = script.digitizationReferenceNumber?.takeIf { it.isNotBlank() } ?: return
        val result = WalletRepository.acknowledgePendingTask(
            context = activity,
            seId = seId,
            scriptId = scriptId,
            digitizeRef = digitizeRef
        )
        if (!result.isSuccess) {
            logger.debug("Device detach acknowledge failed: ${result.errorMessage}")
        }
    }

    /**
     * Script handler callbacks that log only, so FCM flows own loading and toasts.
     */
    private fun createSilentCallbacks() = object : ScriptHandler.Callbacks {
        override fun showLoading(show: Boolean, msg: String) = Unit
        override fun showToast(message: String) = Unit
        override fun updateLogs(message: String) {
            logger.debug("PendingDeleteScriptExecutor: $message")
        }
    }
}
