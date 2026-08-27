// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

package com.infineon.secora.wallet.cdcvm

import com.infineon.secora.wearable.cdcvm.CvmState

/**
 * A single resolved snapshot of the wearable's CDCVM-relevant state, produced by
 * [WearableStatusMonitor] and consumed by the status chips (card list, device detail, wearable
 * settings) and the device-list hand icon.
 *
 * @property connected `true` when a BLE wearable is connected and its SE ID is known.
 * @property isNfc `true` for an NFC device, where the body/CVM concepts do not apply.
 * @property presence Debounced on-body / off-body state.
 * @property verified `true` when the cardholder is currently verified (payments unlocked).
 * @property cvmState The raw CVM state read, or `null` when unavailable; lets a screen drive extra
 *   behaviour (e.g. the card list's passcode CTA and setup gate) without re-reading.
 */
data class WearableStatus(
    val connected: Boolean,
    val isNfc: Boolean,
    val presence: BodyPresenceTracker.Presence,
    val verified: Boolean,
    val cvmState: CvmState?
) {
    /** No wearable reachable (disconnected or NFC): chips/hand should show their inactive state. */
    val unreachable: Boolean get() = isNfc || !connected
}
