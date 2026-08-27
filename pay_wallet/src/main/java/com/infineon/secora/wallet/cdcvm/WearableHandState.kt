// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

package com.infineon.secora.wallet.cdcvm

/**
 * Resolved hand-icon state for the wearable rows in the device list.
 *
 * Body and verification state only exist for the one wearable that currently holds the BLE
 * connection, so this holder keys the resolved [Hand] by that device's SE ID. A row whose SE ID does
 * not match the active device — a disconnected device, or any row while Bluetooth is off — resolves
 * to [Hand.HIDDEN].
 *
 * The device-list poller ([com.infineon.secora.wallet.ui.fragment.AvailableDeviceFragment]) writes
 * it; [com.infineon.secora.wallet.adapter.PaymentDeviceAdapter] reads it while binding.
 */
object WearableHandState {

    /**
     * Which hand icon a row shows.
     *
     * - [HIDDEN]: Bluetooth off, or this device is not the connected one, or state unknown.
     * - [OFF_BODY]: connected, wearable off the wrist.
     * - [ON_BODY]: connected, worn, cardholder not verified (payments locked).
     * - [VERIFIED]: connected, worn, cardholder verified (payments unlocked).
     */
    enum class Hand { HIDDEN, OFF_BODY, ON_BODY, VERIFIED }

    @Volatile
    private var activeSeId: String? = null

    @Volatile
    private var hand: Hand = Hand.HIDDEN

    /** Records the resolved [hand] for the currently connected wearable [seId]. */
    fun update(seId: String, hand: Hand) {
        activeSeId = seId
        this.hand = hand
    }

    /** Clears the state, e.g. when no wearable is connected. */
    fun clear() {
        activeSeId = null
        hand = Hand.HIDDEN
    }

    /** Resolved hand for [seId]; [Hand.HIDDEN] unless it is the connected wearable. */
    fun handFor(seId: String?): Hand =
        if (!seId.isNullOrEmpty() && seId == activeSeId) hand else Hand.HIDDEN
}
