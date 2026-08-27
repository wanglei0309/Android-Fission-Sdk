// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: StorageRepository.kt wraps Application database and SharedPreference usage and
 * exposes the suspend function which will safely execute on background thread
 */
package com.infineon.secora.wallet.data.local

import android.content.Context
import com.infineon.secora.wallet.MyApplication
import com.infineon.secora.wallet.client.data.models.card.common.CardDetails
import com.infineon.secora.wallet.client.data.models.common.CardList
import com.infineon.secora.wallet.client.data.models.common.GetProvisionCardResponse
import com.infineon.secora.wallet.client.util.AppDispatchers
import com.infineon.secora.wallet.data.local.hostdb.HostCardCache
import com.infineon.secora.wallet.data.local.preference.AppPreferenceStorage
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import kotlinx.coroutines.withContext

/**
 * Use this class for data storing and retrieving.
 * This can be called from UI screen instead of calling AppPreferenceStorage or Utils directly
 */
object StorageRepository {

    private val preferenceStorage by lazy {
        AppPreferenceStorage.getInstance(MyApplication.appContext)
    }

    /**
     * Saves a string value associated with the given string key.
     * Use this only if necessary dynamic key scenarios
     *
     * @param key The key used to identify the stored value.
     * @param value The string value to be stored.
     */
    fun saveString(key: String, value: String) {
        preferenceStorage.setPreferenceForString(key = key, value = value)
    }

    /**
     * Retrieves the string value associated with the given string key.
     * Use this only if necessary dynamic key scenarios
     *
     * @param key The key used to identify the stored value.
     * @return The stored string value, or an empty string if no value exists.
     */
    fun readString(key: String): String {
        return preferenceStorage.getPreferenceForString(key = key)
    }

    /**
     * Clears the stored string value for the given preference key
     * by saving an empty string.
     *
     * @param key The preference key whose value should be cleared.
     */
    fun clearString(key: String) {
        preferenceStorage.setPreferenceForString(key = key, value = "")
    }

    /**
     * Saves a boolean value associated with the given preference key.
     *
     * @param key The preference key used to identify the stored value.
     * @param value The boolean value to be stored.
     */
    fun saveBoolean(key: String, value: Boolean) {
        preferenceStorage.setPreferenceForBoolean(key = key, value = value)
    }

    /**
     * Retrieves the boolean value associated with the given preference key.
     *
     * @param key The preference key used to identify the stored value.
     * @return The stored boolean value. Returns false if no value exists.
     */
    fun readBoolean(key: String): Boolean {
        return preferenceStorage.getPreferenceForBoolean(key = key)
    }

    /**
     * Returns all saved nicknames for the active payment app instance.
     *
     * @param context The application or activity context.
     * @return Map keyed by (paymentAppInstanceId, dpanSuffix) with nickname values.
     */
    suspend fun getNicknameMap(context: Context): Map<Pair<String, String>, String> = withContext(AppDispatchers.IO) {
        val paymentAppInstanceId = readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID)
        if (paymentAppInstanceId.isBlank()) return@withContext emptyMap()

        HostCardCache.readForPaymentApp(context, paymentAppInstanceId)
            .mapNotNull { card ->
                val dPan = card.dpanSuffix?.trim().orEmpty()
                val nickname = card.cardNickname?.trim().orEmpty()
                if (dPan.isNotEmpty() && nickname.isNotEmpty()) (paymentAppInstanceId to dPan) to nickname else null
            }.toMap()
    }

    /**
     * Checks if a nickname is already assigned to another card in the same payment app instance.
     *
     * @param context The application or activity context.
     * @param nickname The nickname to validate.
     * @param paymentAppInstanceId The payment app instance ID scope.
     * @param cardId The current card identifier (excluded from duplicate check).
     * @return True if duplicate nickname exists, false otherwise.
     */
    suspend fun isNicknameDuplicate(
        context: Context,
        nickname: String,
        paymentAppInstanceId: String,
        cardId: String
    ): Boolean = withContext(AppDispatchers.IO) {
        val targetNickname = nickname.trim()
        val targetCardId = cardId.trim()
        if (targetNickname.isEmpty() || paymentAppInstanceId.isBlank()) return@withContext false

        HostCardCache.readForPaymentApp(context, paymentAppInstanceId).any { card ->
            val dPan = card.dpanSuffix?.trim().orEmpty()
            val existingNickname = card.cardNickname?.trim().orEmpty()
            dPan.isNotEmpty() && dPan != targetCardId && existingNickname.equals(targetNickname, ignoreCase = true)
        }
    }

    /**
     * Returns the saved nickname for a card under the given payment app instance.
     *
     * @param paymentAppInstanceId The payment app instance ID scope.
     * @param cardId The target card identifier (DPAN suffix).
     * @param context The application or activity context.
     * @return Nickname if found, otherwise null.
     */
    suspend fun getNicknameForCard(paymentAppInstanceId: String, cardId: String, context: Context): String? =
        withContext(AppDispatchers.IO) {
            val targetCardId = cardId.trim()
            if (targetCardId.isEmpty() || paymentAppInstanceId.isBlank()) return@withContext null

            HostCardCache.readForPaymentApp(context, paymentAppInstanceId)
                .firstOrNull { it.dpanSuffix?.trim().orEmpty() == targetCardId }
                ?.cardNickname
        }

    /**
     * Saves or updates nickname mapping for a card in app-owned local storage.
     *
     * @param context The application or activity context.
     * @param paymentAppInstanceId The payment app instance ID scope.
     * @param cardId The target card identifier (DPAN suffix).
     * @param nickname The nickname value to persist.
     */
    suspend fun saveNicknameForCard(context: Context, paymentAppInstanceId: String, cardId: String, nickname: String) =
        withContext(AppDispatchers.IO) {
            HostCardCache.updateCardNicknameByDpan(context, paymentAppInstanceId, cardId, nickname)
        }

    /**
     * Removes nickname mapping for a card from app-owned local storage.
     *
     * @param context The application or activity context.
     * @param paymentAppInstanceId The payment app instance ID scope.
     * @param cardId The card identifier (DPAN suffix or digitization reference).
     */
    suspend fun removeNicknameForCard(context: Context, paymentAppInstanceId: String, cardId: String) =
        withContext(AppDispatchers.IO) {
            val targetCardId = cardId.trim()
            if (targetCardId.isEmpty() || paymentAppInstanceId.isBlank()) return@withContext

            HostCardCache.removeUserNicknameForCard(context, paymentAppInstanceId, targetCardId)
            HostCardCache.updateCardNicknameByDpan(context, paymentAppInstanceId, targetCardId, "")
        }

    /**
     * Persists provision API response into app local card storage.
     *
     * @param context The application or activity context.
     * @param paymentAppInstanceId The payment app instance ID scope.
     * @param response Provision response to cache.
     */
    suspend fun saveProvisionedCardsToLocalDb(
        context: Context,
        paymentAppInstanceId: String,
        response: GetProvisionCardResponse?
    ) = withContext(AppDispatchers.IO) {
        HostCardCache.saveProvisionResponse(context, paymentAppInstanceId, response)
    }

    /**
     * Returns local card list for a payment app instance.
     *
     * @param context The application or activity context.
     * @param paymentAppInstanceId The payment app instance ID scope.
     * @return List of locally cached cards.
     */
    suspend fun getUiCardListFromLocalDb(context: Context, paymentAppInstanceId: String): List<CardDetails> =
        withContext(AppDispatchers.IO) {
            HostCardCache.readForPaymentApp(context, paymentAppInstanceId)
        }

    /**
     * Clears app local card cache.
     *
     * @param context The application or activity context.
     * @param clearUserNicknames True to clear nickname mappings as well.
     */
    suspend fun clearAllLocalCardData(context: Context, clearUserNicknames: Boolean = true) =
        withContext(AppDispatchers.IO) {
            HostCardCache.clearAll(context, clearUserNicknames)
        }

    /**
     * Saves user nickname keyed by digitization reference.
     *
     * @param context The application or activity context.
     * @param digitizationReferenceNumber The card digitization reference number.
     * @param nickname The nickname to persist.
     */
    suspend fun setLocalUserCardNickname(context: Context, digitizationReferenceNumber: String, nickname: String) =
        withContext(AppDispatchers.IO) {
            HostCardCache.setUserNicknameForDigitizationRef(context, digitizationReferenceNumber, nickname)
        }

    /**
     * Clears local cards for one payment app instance.
     *
     * @param context The application or activity context.
     * @param paymentAppInstanceId The payment app instance ID to clear.
     */
    suspend fun clearLocalCardsForPaymentApp(context: Context, paymentAppInstanceId: String) =
        withContext(AppDispatchers.IO) {
            HostCardCache.clearPaymentApp(context, paymentAppInstanceId)
        }

    /**
     * Updates local card status by digitization reference.
     *
     * @param context The application or activity context.
     * @param tokenRefNum The card digitization reference.
     * @param cardStatus New card status value.
     */
    suspend fun updateLocalCardStatus(context: Context, tokenRefNum: String?, cardStatus: String?) =
        withContext(AppDispatchers.IO) {
            HostCardCache.updateCardStatus(context, tokenRefNum, cardStatus)
        }

    /**
     * Deletes a local card row by digitization reference.
     *
     * @param context The application or activity context.
     * @param digitizationReferenceNumber The card digitization reference to remove.
     */
    suspend fun deleteLocalCardByDigitizeRef(context: Context, digitizationReferenceNumber: String) =
        withContext(AppDispatchers.IO) {
            HostCardCache.deleteByDigitizationRef(context, digitizationReferenceNumber)
        }

    /**
     * Builds card payload for status API from local storage.
     *
     * @param context The application or activity context.
     * @param digitizationReferenceNumber The target card digitization reference.
     * @param paymentAppInstanceId Payment app instance ID scope.
     * @return Local [CardList] payload or null if no matching card is found.
     */
    suspend fun getLocalCardListForStatusApi(
        context: Context,
        digitizationReferenceNumber: String,
        paymentAppInstanceId: String
    ): CardList? = withContext(AppDispatchers.IO) {
        HostCardCache.cardListForStatusApi(context, digitizationReferenceNumber, paymentAppInstanceId)
    }

    /**
     * Returns local card image payload by asset ID.
     *
     * @param context The application or activity context.
     * @param assetId The card asset ID.
     * @return Card details containing image fields when found.
     */
    suspend fun getLocalCardImageByAssetId(context: Context, assetId: String): CardDetails =
        withContext(AppDispatchers.IO) {
            HostCardCache.findImageByAssetId(context, assetId)
        }

    /**
     * Updates local card image fields for a card reference.
     *
     * @param context The application or activity context.
     * @param digitizationReferenceNumber The card digitization reference.
     * @param base64Image Base64 image value.
     * @param height Optional image height.
     * @param width Optional image width.
     */
    suspend fun mergeLocalCardImage(
        context: Context,
        digitizationReferenceNumber: String,
        base64Image: String,
        height: String? = null,
        width: String? = null
    ) = withContext(AppDispatchers.IO) {
        HostCardCache.mergeCardImage(context, digitizationReferenceNumber, base64Image, height, width)
    }

    /**
     * Updates local card nickname by payment app and DPAN suffix.
     *
     * @param context The application or activity context.
     * @param paymentAppInstanceId The payment app instance ID scope.
     * @param dPanSuffix The target card DPAN suffix.
     * @param nickname The nickname value to set.
     */
    suspend fun updateLocalCardNicknameByDpan(
        context: Context,
        paymentAppInstanceId: String,
        dPanSuffix: String,
        nickname: String
    ) = withContext(AppDispatchers.IO) {
        HostCardCache.updateCardNicknameByDpan(context, paymentAppInstanceId, dPanSuffix, nickname)
    }

    /**
     * Reads one cached card by digitization reference.
     *
     * @param context Context used to access host DB.
     * @param digitizationReferenceNumber Digitization reference number.
     * @return Card details when found, otherwise null.
     */
    suspend fun getCardByDigitizationReferenceNumber(context: Context, digitizationReferenceNumber: String) =
        withContext(AppDispatchers.IO) {
            HostCardCache.getByDigitizationRef(context, digitizationReferenceNumber)
        }
}