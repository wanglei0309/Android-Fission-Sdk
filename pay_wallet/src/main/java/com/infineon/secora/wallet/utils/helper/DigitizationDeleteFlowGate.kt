// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: DigitizationDeleteFlowGate.kt Prevents duplicate secure-element delete script execution when
 * the user cancels on terms while a deleted-card event triggers card list pending delete for the same digitization reference.
 **/
package com.infineon.secora.wallet.utils.helper

/**
 * Coordinates [com.infineon.secora.wallet.ui.fragment.TermsConditionFragment] (updateCardStatus + local scripts)
 * with [com.infineon.secora.wallet.ui.fragment.CardListFragment] pending-task delete when a
 * [com.infineon.secora.wallet.utils.constants.Constants.DELETED_CARD] event targets the same digitization reference.
 */
object DigitizationDeleteFlowGate {

    /**
     * When [TermsConditionFragment] pops after a successful client-side delete, it sets this key
     * to `true` on the card list destination's [androidx.lifecycle.SavedStateHandle] so
     * [com.infineon.secora.wallet.ui.fragment.CardListFragment] can refresh.
     */
    const val POST_TERMS_DELETE_REFRESH_KEY = "post_terms_client_delete_refresh"

    /**
     * Set on the card list [androidx.lifecycle.SavedStateHandle] after FCM delete scripts finish
     * so [com.infineon.secora.wallet.ui.fragment.CardListFragment] refreshes from the API.
     */
    const val POST_FCM_CARD_DELETED_REFRESH_KEY = "post_fcm_card_deleted_refresh"

    @Volatile
    private var termsClientDeleteDigitizationRef: String? = null

    /**
     * Marks that [com.infineon.secora.wallet.ui.fragment.TermsConditionFragment] is running a client-side delete
     * for [digitizationReferenceNumber], so duplicate FCM card-deleted pending deletes are skipped.
     *
     * @param digitizationReferenceNumber Card digitization reference being deleted.
     */
    fun markTermsClientDeleteStarted(digitizationReferenceNumber: String) {
        val ref = digitizationReferenceNumber.trim()
        if (ref.isEmpty()) return
        termsClientDeleteDigitizationRef = ref
    }

    /** Clears the in-flight Terms client-delete digitization reference. */
    fun clearTermsClientDelete() {
        termsClientDeleteDigitizationRef = null
    }

    /**
     * Returns true when an FCM card-deleted event targets the same card as an active Terms client delete.
     *
     * @param entityId Digitization reference from the FCM payload.
     */
    fun shouldSkipCardListPendingDeleteDuplicate(entityId: String?): Boolean {
        val id = entityId?.trim().orEmpty()
        if (id.isEmpty()) return false
        val active = termsClientDeleteDigitizationRef ?: return false
        return active == id
    }
}
