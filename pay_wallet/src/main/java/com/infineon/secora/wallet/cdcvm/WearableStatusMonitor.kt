// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

package com.infineon.secora.wallet.cdcvm

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothUiStateManager
import com.infineon.secora.wallet.ui.fragment.WearableSettingFragment.Companion.isCardDeletionInProgress
import com.infineon.secora.wallet.utils.constants.Constants
import com.infineon.secora.wallet.utils.helper.SecureElementScriptCoordinator
import com.infineon.secora.wearable.cdcvm.CvmState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shared wearable-status poller used by every screen that shows the CDCVM status (the card list,
 * device detail and wearable settings chips, and the device-list hand icon).
 *
 * While its owner is started it polls every [intervalMs]: the Bluetooth connection state, the
 * Skyfall on-body / off-body status, and — when connected — GET CVM STATE for the verified flag. It
 * also re-polls immediately when the Bluetooth connection state changes. Each snapshot is delivered
 * to [onUpdate] on the main thread as a [WearableStatus].
 *
 * A poll is skipped while [canPoll] is false (default: a secure-element script is running), so the
 * periodic GET CVM STATE never interleaves with a register / delink / install / add-card exchange.
 * Screens that run their own BLE flows can pass a stricter predicate.
 *
 * @property onUpdate Called on the main thread with each resolved [WearableStatus].
 * @property canPoll Guard evaluated before every read; `false` skips the tick and keeps the last state.
 * @property attemptOnboard When `true`, a device reporting no passcode is onboarded before the state
 *   is reported (matches the card list's behaviour); most screens leave this `false`.
 * @property intervalMs Poll cadence in milliseconds.
 */
class WearableStatusMonitor(
    private val onUpdate: (WearableStatus) -> Unit,
    private val canPoll: () -> Boolean = { !SecureElementScriptCoordinator.isScriptRunning() },
    private val attemptOnboard: Boolean = false,
    private val intervalMs: Long = DEFAULT_INTERVAL_MS
) {

    private val bodyTracker = BodyPresenceTracker(offBodyThreshold = 1)
    private var trackedSeId: String? = null
    private var polling = false
    private var attachedOwner: LifecycleOwner? = null
    private var appContext: Context? = null

    /**
     * Starts polling tied to [owner]'s STARTED state; stops and restarts automatically with the
     * lifecycle. Call once (e.g. from `onViewCreated`) with the view lifecycle owner.
     */
    fun attach(owner: LifecycleOwner, context: Context) {
        val appContext = context.applicationContext
        this.attachedOwner = owner
        this.appContext = appContext
        val listener: () -> Unit = { owner.lifecycleScope.launch { pollOnce(appContext) } }
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                BluetoothUiStateManager.register(appContext, listener)
                try {
                    while (true) {
                        pollOnce(appContext)
                        delay(intervalMs)
                    }
                } finally {
                    BluetoothUiStateManager.unregister(listener)
                }
            }
        }
    }

    /**
     * Requests an immediate one-shot poll off the periodic cadence (e.g. after a connect or a device
     * clear). No-op until [attach] has been called.
     */
    fun requestPoll() {
        val owner = attachedOwner ?: return
        val context = appContext ?: return
        owner.lifecycleScope.launch { pollOnce(context) }
    }

    /** Reads one snapshot and reports it; overlapping calls (loop + Bluetooth edge) are coalesced. */
    private suspend fun pollOnce(context: Context) {
        if (polling || isCardDeletionInProgress) return
        polling = true
        try {
            val nfc = isNfc()
//            val connected = BluetoothStateManager.isConnected
            val selectedSeId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)
            val connected = BluetoothStateManager.isBluetoothTurnedOn(context)
                && BluetoothStateManager.isDeviceConnected(selectedSeId, context)

            val seId = CdcvmApi.activeDeviceId()

            if (nfc || !connected || seId == null) {
                bodyTracker.reset()
                trackedSeId = null
                onUpdate(
                    WearableStatus(
                        connected = connected && !nfc,
                        isNfc = nfc,
                        presence = BodyPresenceTracker.Presence.UNKNOWN,
                        verified = false,
                        cvmState = null
                    )
                )
                return
            }

            // Busy: keep the last reported state rather than contending with an in-flight exchange.
            if (!canPoll()) return

            if (seId != trackedSeId) {
                bodyTracker.reset()
                trackedSeId = seId
            }

            val presence = bodyTracker.update(CdcvmApi.readBodyStatus(context))
            var state = readCvmState(context)
            if (attemptOnboard && state?.needsPasscodeSetup() == true) {
                if (CdcvmApi.onboard(context).isSuccess) {
                    state = readCvmState(context)
                }
            }
            onUpdate(
                WearableStatus(
                    connected = true,
                    isNfc = false,
                    presence = presence,
                    verified = state?.isAuthenticated == true,
                    cvmState = state
                )
            )
        } finally {
            polling = false
        }
    }

    private suspend fun readCvmState(context: Context): CvmState? =
        (CdcvmApi.readState(context) as? CdcvmOutcome.Success)?.state

    private fun isNfc(): Boolean =
        runCatching {
            StorageRepository.readString(PreferenceKey.DEVICE_NAME).contains(Constants.NFC_DEVICE_MODEL)
        }.getOrDefault(false)

    private companion object {
        const val DEFAULT_INTERVAL_MS = 8_000L
    }
}
