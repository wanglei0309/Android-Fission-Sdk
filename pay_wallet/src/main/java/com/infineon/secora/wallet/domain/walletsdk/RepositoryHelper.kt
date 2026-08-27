// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: RepositoryHelper.kt helper class for WalletRepository.
 **/
package com.infineon.secora.wallet.domain.walletsdk

import com.infineon.secora.wallet.MyApplication
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.client.data.models.common.DigitizeResponseBody
import com.infineon.secora.wallet.client.data.models.common.UserResponseBody
import com.infineon.secora.wallet.client.data.models.prepse.PrepSeResponseBody
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.utils.constants.Constants.PNO_MDES
import com.infineon.secora.wallet.utils.constants.Constants.PNO_VTS
import com.infineon.secora.wallet.utils.constants.Constants.SUCCESS
import com.infineon.secora.wallet.utils.StringUtils.isEmptyString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object RepositoryHelper {

    /**
     * Creates a successful [WalletSdkResult] with the provided status message and optional response data.
     *
     * @param statusMessage A message describing the success state.
     * @param response Optional data to be returned as part of the success result.
     * @param T The type of the response payload.
     *
     * @return A [WalletSdkResult] instance representing a successful state.
     */
    fun <T : Any> getSuccessResponse(statusMessage: String, response: T? = null): WalletSdkResult<T> {
        val statusMsg = if (isEmptyString(statusMessage)) {
            MyApplication.appContext.getString(R.string.something_went_wrong)
        } else {
            statusMessage
        }
        return WalletSdkResult(isLoading = false, isSuccess = true, statusMessage = statusMsg, response = response)
    }

    /**
     * Returns a user-friendly error message based on the given response.
     *
     * If the response is of type [String], it is returned directly.
     * Otherwise, a default localized error message is retrieved from resources.
     *
     * @param response The response object which may contain an error message.
     * @return A readable error message string.
     */
    fun <T : Any?> getErrorMessage(response: T): String {
        return if (response is String) {
            response
        } else {
            MyApplication.appContext.getString(R.string.something_went_wrong)
        }
    }

    /**
     * Creates an error [WalletSdkResult] with a validated error message.
     *
     * If the provided [errorMessage] is null, empty, or invalid, a default
     * fallback message (`something_went_wrong`) is used instead.
     *
     * This ensures that the SDK always returns a meaningful error message
     * to the caller.
     *
     * @param errorMessage A message describing the error state.
     * @param T The expected type of the response payload (unused in error case).
     *
     * @return A [WalletSdkResult] instance representing a failure state
     * with a non-empty error message.
     */
    fun <T : Any> getErrorResponse(errorMessage: String): WalletSdkResult<T> {
        val errorMsg = if (isEmptyString(errorMessage)) {
            MyApplication.appContext.getString(R.string.something_went_wrong)
        } else {
            errorMessage
        }

        return WalletSdkResult(isLoading = false, isSuccess = false, errorMessage = errorMsg)
    }

    /**
     * Stores user data retrieved from the Google login response into shared preferences.
     *
     * @param response The [UserResponseBody] containing user details.
     */
    fun storeGoogleUserData(email: String, response: UserResponseBody) {
        if (SUCCESS != response.statusMessage) {
            return
        }

        if (!isEmptyString(email)) {
            StorageRepository.saveString(PreferenceKey.EMAIL_ID, email)
        }
        response.userId?.let { userId ->
            StorageRepository.saveString(PreferenceKey.USER_ID, userId)
        }

        val savedEmail = StorageRepository.readString(PreferenceKey.EMAIL_ID)
        val savedProfile = StorageRepository.readString(PreferenceKey.PROFILE_IMAGE)
        val userName = StorageRepository.readString(PreferenceKey.USER_NAME)
        if (userName.trim().isEmpty()) {
            StorageRepository.saveString(PreferenceKey.USER_NAME, savedEmail)
        }
        savedProfile.let { profile ->
            StorageRepository.saveString(PreferenceKey.PROFILE_IMAGE, profile)
        }

        StorageRepository.saveString(PreferenceKey.WALLET_ID, response.walletAppInstanceId.toString())
        response.jwtAuthenticationToken?.takeIf { it.isNotBlank() }?.let { token ->
            StorageRepository.saveString(PreferenceKey.JWT_TOKEN, token)
        }
        response.refreshToken?.takeIf { it.isNotBlank() }?.let { token ->
            StorageRepository.saveString(PreferenceKey.REFRESH_TOKEN, token)
        }
    }

    /**
     * Saves required Prep SE response to local preference storage.
     *
     * @param response The [PrepSeResponseBody].
     */
    fun storePrepSeData(response: PrepSeResponseBody) {
        StorageRepository.saveString(
            PreferenceKey.DIGI_REFERENCE_NUMBER_CANCEL,
            response.digitizationReferenceNumber.toString()
        )

        response.apsdAid?.takeIf { it.isNotBlank() }?.let { apsdAid ->
            StorageRepository.saveString(
                key = PreferenceKey.spsdAppletInstanceAidKey(response.digitizationReferenceNumber.toString()),
                value = apsdAid
            )

            val cardType = when (response.pnoType) {
                PNO_MDES -> MyApplication.appContext.getString(R.string.master_card_)
                PNO_VTS -> MyApplication.appContext.getString(R.string.visa)
                else -> ""
            }
            StorageRepository.saveString(
                key = PreferenceKey.aidCardTypeKey(response.digitizationReferenceNumber.toString()),
                value = cardType
            )
        }

        response.appletInstanceAids?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { aid ->
            StorageRepository.saveString(
                key = PreferenceKey.cardAidKey(response.digitizationReferenceNumber.toString()),
                value = aid
            )
        }
    }

    /**
     * Stores required response [DigitizeResponseBody] parameter in success case.
     * Saves the available step-up authentication methods (like OTP or biometric) into the local database for later use.
     * Saves the card decision (APPROVED / REQUIRES_VERIFICATION / REJECTED) in shared preferences for persistent storage.
     */
    fun storeDigitizeResponseData(digitizeResponse: DigitizeResponseBody) {
        if (SUCCESS != digitizeResponse.statusMessage) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            StorageRepository.saveString(PreferenceKey.CARD_DECISION, digitizeResponse.decision.toString())
            digitizeResponse.panUniqueReference?.trim()?.takeIf { it.isNotEmpty() }?.let { panUniqueReference ->
                StorageRepository.saveString(PreferenceKey.PAN_UNIQUE_REFERENCE, panUniqueReference)
            }
        }
    }
}