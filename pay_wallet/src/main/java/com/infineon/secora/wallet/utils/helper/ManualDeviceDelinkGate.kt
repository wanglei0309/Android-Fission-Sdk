// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: ManualDeviceDelinkGate.kt suppresses redundant post-delink FCM handling
 * (Device Detach Update, Card Deleted, etc.) when the user already delinked from the app.
 **/
package com.infineon.secora.wallet.utils.helper

/**
 * Coordinates in-app manual device delink with server-driven FCM notifications.
 * The backend may still send detach/card-deleted FCMs after a client-initiated deleteDevice call.
 */
object ManualDeviceDelinkGate {

    private const val TTL_MS = 120_000L

    @Volatile
    private var lastManualDelinkSeId: String? = null

    @Volatile
    private var lastManualDelinkPaymentAppInstanceId: String? = null

    @Volatile
    private var markedAtMs: Long = 0L

    /**
     * Call when deleteDevice / delink succeeds from the app (not portal-only detach).
     */
    fun markManualDelinkCompleted(seId: String, paymentAppInstanceId: String? = null) {
        val trimmedSeId = seId.trim()
        if (trimmedSeId.isEmpty()) return
        lastManualDelinkSeId = trimmedSeId
        lastManualDelinkPaymentAppInstanceId =
            paymentAppInstanceId?.trim()?.takeIf { it.isNotEmpty() }
        markedAtMs = System.currentTimeMillis()
    }

    /**
     * Returns true when an incoming post-delink FCM should be ignored because the user
     * already started or completed the same delink locally.
     *
     * The gate stays active until [TTL_MS] so multiple FCM types (Device Detach, Card Deleted)
     * for the same delink are all suppressed without showing reconnect prompts.
     */
    fun shouldSkipFcmDeviceDetach(
        seId: String?,
        paymentAppInstanceId: String? = null
    ): Boolean = shouldSkipPostManualDelinkFcm(seId, paymentAppInstanceId)

    /**
     * Returns true when an incoming post-delink FCM should be ignored because the user
     * already started or completed the same delink locally.
     *
     * Matches on SE ID or payment app instance ID within [TTL_MS].
     *
     * @param seId                    Secure element ID from the FCM payload, if any.
     * @param paymentAppInstanceId  Payment app instance ID from the FCM payload, if any.
     */
    fun shouldSkipPostManualDelinkFcm(
        seId: String?,
        paymentAppInstanceId: String? = null
    ): Boolean {
        if (isExpired()) {
            clear()
            return false
        }

        val notifiedSeId = seId?.trim().orEmpty()
        val notifiedPid = paymentAppInstanceId?.trim().orEmpty()
        val markedSeId = lastManualDelinkSeId?.trim().orEmpty()
        if (markedSeId.isEmpty()) return false

        val matchesSeId =
            notifiedSeId.isNotEmpty() &&
                notifiedSeId.equals(markedSeId, ignoreCase = true)
        val markedPid = lastManualDelinkPaymentAppInstanceId?.trim().orEmpty()
        val matchesPid =
            notifiedPid.isNotEmpty() &&
                markedPid.isNotEmpty() &&
                notifiedPid == markedPid

        return matchesSeId || matchesPid
    }

    /**
     * Returns true when the manual-delink gate has exceeded [TTL_MS] since [markManualDelinkCompleted].
     */
    private fun isExpired(): Boolean {
        val markedAt = markedAtMs
        return markedAt == 0L || System.currentTimeMillis() - markedAt > TTL_MS
    }

    /**
     * Clears stored manual-delink markers after expiry.
     */
    private fun clear() {
        lastManualDelinkSeId = null
        lastManualDelinkPaymentAppInstanceId = null
        markedAtMs = 0L
    }
}
