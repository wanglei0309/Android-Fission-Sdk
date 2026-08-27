// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

package com.infineon.secora.wallet.cdcvm

import com.infineon.secora.wearable.cdcvm.SkyfallBodyStatus

/**
 * Debounces the noisy Skyfall body-status feed into a stable [Presence] for the UI and the
 * off-body gate.
 *
 * The wearable can report a spurious off-body sample while still worn, so flipping an established
 * on-body state to off-body requires [offBodyThreshold] consecutive off-body samples (default 2, per
 * the CDCVM technical reference). Any on-body sample resets the counter. A transient unavailable
 * sample keeps the last known presence rather than dropping to unknown, so a single missed poll does
 * not blank the chip.
 *
 * Not thread-safe: feed it from a single thread (the main thread on the card list).
 */
class BodyPresenceTracker(private val offBodyThreshold: Int = DEFAULT_OFF_BODY_THRESHOLD) {

    /** Resolved body presence after debounce. */
    enum class Presence { ON_BODY, OFF_BODY, UNKNOWN }

    private var current: Presence = Presence.UNKNOWN
    private var consecutiveOffBodySamples = 0

    /** Latest resolved presence without feeding a new sample. */
    val presence: Presence get() = current

    /**
     * Feeds one poll [status] and returns the resolved presence.
     *
     * - Unavailable sample: keep the last known presence (or [Presence.UNKNOWN] if never known).
     * - On-body sample: resolve to [Presence.ON_BODY] and reset the off-body counter.
     * - Off-body sample: resolve immediately unless currently on-body, in which case wait for
     *   [offBodyThreshold] consecutive off-body samples before flipping.
     */
    fun update(status: SkyfallBodyStatus): Presence {
        if (!status.isAvailable) {
            return current
        }
        if (status.isOnBody) {
            consecutiveOffBodySamples = 0
            current = Presence.ON_BODY
            return current
        }
        consecutiveOffBodySamples++
        if (current != Presence.ON_BODY || consecutiveOffBodySamples >= offBodyThreshold) {
            current = Presence.OFF_BODY
        }
        return current
    }

    /** Clears state, e.g. on BLE disconnect. */
    fun reset() {
        current = Presence.UNKNOWN
        consecutiveOffBodySamples = 0
    }

    private companion object {
        const val DEFAULT_OFF_BODY_THRESHOLD = 2
    }
}
