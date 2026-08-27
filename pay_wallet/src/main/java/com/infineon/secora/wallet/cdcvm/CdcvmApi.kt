package com.infineon.secora.wallet.cdcvm

import android.content.Context
import android.util.Log
import com.infineon.secora.wallet.client.util.AppDispatchers
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wearable.SecoraWearableSDK
import com.infineon.secora.wearable.cdcvm.CdcvmCardDeletionHandler
import com.infineon.secora.wearable.cdcvm.CdcvmException
import com.infineon.secora.wearable.cdcvm.CvmState
import com.infineon.secora.wearable.cdcvm.SkyfallBodyStatus
import com.infineon.secora.wearable.cdcvm.VerifyPasscodeResult
import com.infineon.secora.wearable.protocolapi.IAsyncProtocol
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.coroutines.resume

/**
 * App-module entry point for CDCVM.
 *
 * Every call is a `suspend` function returning a [CdcvmOutcome], so callers never have to handle a
 * raw [CompletableFuture] or map status words themselves. Failures arrive as outcome values rather
 * than exceptions, because a wrong passcode is an ordinary result here, not an error.
 *
 * The wearable is identified by its SE ID. It is read from preferences by default, so screens do not
 * have to thread it through; pass [deviceId] explicitly when acting on a device other than the
 * currently selected one. The SE ID also keys the persisted passcode challenge, so it must be the
 * same value at setup and at verify time.
 *
 * Calls must not be issued concurrently for one device: each opens its own secure channel, and the
 * applet serves one channel at a time.
 *
 * ```
 * lifecycleScope.launch {
 *     when (val outcome = CdcvmApi.verifyPasscode(requireContext(), "1937")) {
 *         is CdcvmOutcome.Success -> unlockPayments()
 *         is CdcvmOutcome.WrongPasscode -> showRetries(outcome.remainingRetries)
 *         CdcvmOutcome.Blocked -> navigateToRecovery()
 *         else -> showError(outcome.describe())
 *     }
 * }
 * ```
 */
object CdcvmApi {

    /** Logcat tag for body-status diagnostics; matches the SDK's `wearable-sdk` tag for one filter. */
    private const val BODY_LOG_TAG = "wearable-sdk"

    /** Upper bound on a single body-status read, so a silent transport never stalls the poll loop. */
    private const val BODY_STATUS_TIMEOUT_MS = 3_000L

    /**
     * Reads the CVM state. Backs the chip refresh, the add-card gate and the Payment Authorisation
     * poll.
     *
     * A device with no CVM provisioned is reported as [CdcvmOutcome.Success] carrying an
     * uninitialised [CvmState], not as a failure — the caller's next step is to offer setup.
     *
     * @return [CdcvmOutcome.Success] with [CdcvmOutcome.Success.state] populated.
     */
    suspend fun readState(context: Context, deviceId: String? = null): CdcvmOutcome =
        run(context, deviceId) { ctx, protocol, id ->
            manager().readCvmState(ctx, protocol, id)
        }

    /**
     * Runs the VerifyDevice and ClaimDevice onboarding steps: validates CERT.SD.ECKA, presents the
     * host credentials and writes the recovery PUK.
     *
     * Safe to call repeatedly — it is skipped once the device is marked onboarded. That marker is
     * cleared by [resetCvm], which is therefore how onboarding gets re-tested.
     */
    suspend fun onboard(context: Context, deviceId: String? = null): CdcvmOutcome =
        run(context, deviceId) { ctx, protocol, id ->
            manager().onboardDevice(ctx, protocol, id)
        }

    /**
     * UC-01 Setup. Provisions a device passcode and persists the challenge the applet stored.
     *
     * The passcode is checked against the policy before any command is sent, so a weak value comes
     * back as [CdcvmOutcome.PolicyRejected] without touching the secure element.
     */
    suspend fun setupPasscode(
        context: Context,
        passcode: String,
        deviceId: String? = null
    ): CdcvmOutcome = run(context, deviceId) { ctx, protocol, id ->
        manager().setupDevicePasscode(ctx, protocol, id, passcode)
    }

    /**
     * UC-02 Verify. Unlocks payments when the passcode matches.
     *
     * A wrong passcode returns [CdcvmOutcome.WrongPasscode] with the remaining attempt count; once
     * they are exhausted the result becomes [CdcvmOutcome.Blocked].
     */
    suspend fun verifyPasscode(
        context: Context,
        passcode: String,
        deviceId: String? = null
    ): CdcvmOutcome = run(context, deviceId) { ctx, protocol, id ->
        manager().verifyDevicePasscode(ctx, protocol, id, passcode)
    }

    /**
     * UC-07 Change. Replaces the device passcode in a single command that also checks the old one.
     *
     * A wrong [currentPasscode] returns [CdcvmOutcome.WrongPasscode].
     */
    suspend fun changePasscode(
        context: Context,
        currentPasscode: String,
        newPasscode: String,
        deviceId: String? = null
    ): CdcvmOutcome = run(context, deviceId) { ctx, protocol, id ->
        manager().changeDevicePasscode(ctx, protocol, id, currentPasscode, newPasscode)
    }

    /**
     * UC-09 Reset. Removes the CVM and clears the stored challenge and onboarding marker.
     *
     * Pass [deleteAllCards] to have the payment cards removed first, which is the correct order —
     * deleting the CVM first would leave cards on a device that can no longer verify a cardholder.
     * The default handler deletes nothing, for callers that already removed them.
     */
    suspend fun resetCvm(
        context: Context,
        deviceId: String? = null,
        deleteAllCards: CdcvmCardDeletionHandler = CdcvmCardDeletionHandler.none()
    ): CdcvmOutcome = run(context, deviceId) { ctx, protocol, id ->
        manager().resetDeviceCvm(ctx, protocol, id, deleteAllCards)
    }

    /**
     * Reads the Skyfall on-body / off-body status. Backs the body chip and the off-body gate on the
     * card list.
     *
     * This is a device-level BLE status frame, not a CDCVM APDU, so it neither selects the applet
     * nor opens a secure channel. When no wearable is connected, or the device does not surface the
     * status feed, the result is [SkyfallBodyStatus.unavailable]; callers treat that as "unknown",
     * not "off-body".
     */
    suspend fun readBodyStatus(context: Context, deviceId: String? = null): SkyfallBodyStatus =
        withContext(AppDispatchers.IO) {
            val protocol = BluetoothStateManager.activeProtocol
                ?: run {
                    Log.i(BODY_LOG_TAG, "[CDCVM] Body status skipped: no active BLE protocol")
                    return@withContext SkyfallBodyStatus.unavailable()
                }

            try {
                val value = withTimeoutOrNull(BODY_STATUS_TIMEOUT_MS) {
                    protocol.transceiveBodyStatus().awaitOutcome()
                } ?: run {
                    Log.i(BODY_LOG_TAG, "[CDCVM] Body status timed out after ${BODY_STATUS_TIMEOUT_MS}ms")
                    return@withContext SkyfallBodyStatus.unavailable()
                }
                SkyfallBodyStatus.parse(value).also {
                    Log.i(BODY_LOG_TAG, "[CDCVM] Body status = $it")
                }
            } catch (throwable: Throwable) {
                Log.i(BODY_LOG_TAG, "[CDCVM] Body status error: ${throwable.message}")
                SkyfallBodyStatus.unavailable()
            }
        }

    /**
     * Resolves the SE ID of the device a CDCVM call would act on, or `null` when none is known.
     *
     * Useful for deciding whether to show a CDCVM affordance at all.
     */
    fun activeDeviceId(): String? =
        StorageRepository.readString(PreferenceKey.DEVICE_SE_ID).trim().takeIf { it.isNotEmpty() }

    /**
     * `true` when a wearable is connected and its SE ID is known, so a CDCVM call can proceed.
     */
    fun isReady(): Boolean =
        activeDeviceId() != null &&
            BluetoothStateManager.isConnected &&
            BluetoothStateManager.activeProtocol != null

    private fun manager() = SecoraWearableSDK.getInstance().cdcvmManager

    /**
     * Resolves the transport and device id, runs [block], and maps the result onto a [CdcvmOutcome].
     *
     * The future's value type is left open: the use cases return a `CvmState`, a
     * `VerifyPasscodeResult` or nothing at all, and [toOutcome] tells them apart. Using a wildcard
     * rather than a type parameter also keeps Java's `Void` out of this file.
     */
    private suspend fun run(
        context: Context,
        deviceId: String?,
        block: (Context, IAsyncProtocol, String) -> CompletableFuture<out Any?>
    ): CdcvmOutcome = withContext(AppDispatchers.IO) {
        // Everything here must stay off the main thread. Resolving the device id reads
        // preferences, and building the operation opens the encrypted challenge store, which
        // generates or unwraps an Android Keystore key on first use - seconds of blocking work.
        // Callers typically launch on the main dispatcher, and a suspend function runs on the
        // caller's thread until it actually suspends, so the dispatcher has to be forced here.
        val id = deviceId?.trim()?.takeIf { it.isNotEmpty() }
            ?: activeDeviceId()
            ?: return@withContext CdcvmOutcome.NotConnected("no SE ID is stored for a wearable")

        val protocol = BluetoothStateManager.activeProtocol
            ?: return@withContext CdcvmOutcome.NotConnected("no BLE device is connected")

        try {
            toOutcome(block(context.applicationContext, protocol, id).awaitOutcome())
        } catch (throwable: Throwable) {
            classify(throwable)
        }
    }

    /**
     * Maps whatever a use case produced onto an outcome.
     *
     * A [VerifyPasscodeResult] carries its own rejection, so it must not be reported as a success
     * just because the future completed normally.
     */
    private fun toOutcome(result: Any?): CdcvmOutcome = when (result) {
        is CvmState -> CdcvmOutcome.Success(result)
        is VerifyPasscodeResult -> when {
            result.isSuccess -> CdcvmOutcome.Success()
            result.isBlocked -> CdcvmOutcome.Blocked
            else -> CdcvmOutcome.WrongPasscode(result.remainingRetries)
        }
        else -> CdcvmOutcome.Success()
    }

    /**
     * Awaits a future without blocking the calling thread, unwrapping the completion wrapper so
     * [classify] sees the original [CdcvmException].
     */
    private suspend fun <T> CompletableFuture<T>.awaitOutcome(): T? =
        suspendCancellableCoroutine { continuation ->
            whenComplete { value, throwable ->
                if (!continuation.isActive) return@whenComplete
                if (throwable != null) {
                    continuation.resumeWith(Result.failure(throwable.unwrap()))
                } else {
                    continuation.resume(value)
                }
            }
            continuation.invokeOnCancellation { cancel(true) }
        }

    /**
     * Strips the [CompletionException] that a future chain adds.
     */
    private fun Throwable.unwrap(): Throwable =
        if (this is CompletionException && cause != null) cause!! else this

    /**
     * Maps a failure onto the outcome the UI can act on.
     */
    private fun classify(throwable: Throwable): CdcvmOutcome {
        val error = throwable.unwrap()

        if (error is IllegalArgumentException) {
            // Policy and encoding rejections are raised before anything reaches the applet.
            return CdcvmOutcome.PolicyRejected(error.message ?: "invalid passcode")
        }

        if (error is IllegalStateException && error.message?.contains("connect", true) == true) {
            return CdcvmOutcome.NotConnected(error.message ?: "not connected")
        }

        if (error is CdcvmException) {
            return when {
                error.isWrongPasscode -> CdcvmOutcome.WrongPasscode(error.remainingRetries)
                error.isBlocked -> CdcvmOutcome.Blocked
                error.isCredentialMismatch -> CdcvmOutcome.CredentialMismatch
                error.isUninitialized -> CdcvmOutcome.NotProvisioned
                error.message?.contains("No passcode", true) == true -> CdcvmOutcome.NotProvisioned
                error.message?.contains("challenge stored", true) == true -> CdcvmOutcome.NotProvisioned
                else -> CdcvmOutcome.Failed(
                    error.message ?: "CDCVM operation failed",
                    error.statusWord,
                    error
                )
            }
        }

        return CdcvmOutcome.Failed(error.message ?: error.javaClass.simpleName, null, error)
    }
}
