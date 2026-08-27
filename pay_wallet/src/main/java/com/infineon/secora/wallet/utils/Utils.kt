// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: Utils.kt is a helper object providing common utility functions for validation, formatting, and UI handling.
 * It manages tasks like OTP setup, keyboard control, NFC checks, date formatting, and secure input protection.
 **/
package com.infineon.secora.wallet.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import com.infineon.secora.wallet.client.data.models.card.common.CardDetails
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.domain.wearable.ble.script.ScriptExecutionResult
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.constants.Constants
import com.infineon.secora.wearable.scriptloader.JsonScriptLoader.ApduExecutionResult

/**
 *
 * A utility object providing commonly used helper functions and constants across the app.
 */
object Utils {

    /**
     * Builds the bundle with card information for fragment navigation.
     */
    fun prepareBundleWithCardDetails(card: CardDetails): Bundle {
        return Bundle().apply {
            putString(BundleKey.PNO_TYPE, card.pnoType)
            putString(BundleKey.CARD_STATUS, card.cardStatus)
            putString(BundleKey.PAN_SUFFIX, card.dpanSuffix)
            putString(BundleKey.CARD_NICK_NAME, card.cardNickname)
            putString(BundleKey.CUSTOM_URL, card.customUrl)
            putString(BundleKey.CONTACT_NUMBER, card.contactNumber)
            putString(BundleKey.PRIVACY_POLICY_URL, card.privacyPolicyURL)
            putString(BundleKey.CONTACT_WEBSITE, card.contactWebsite)
            putString(BundleKey.TERMS_AND_CONDITIONS_URL, card.termsAndConditionsURL)
            putString(BundleKey.CONTACT_EMAIL, card.contactEmail)
            putString(BundleKey.ASSET_ID, card.cardAssetId)
            putString(BundleKey.EXP_DATE_PRINTED_IND, card.expDatePrintedInd)
            putString(BundleKey.CARD_EXP_DATE, card.cardExpiry)
            putInt(BundleKey.DEFAULT_TAB_INDEX, 0)
            putBoolean(BundleKey.NAVIGATION_FROM_NOTIFICATION, true)
        }
    }

    /**
     * Checks whether the device has an active internet connection via Wi-Fi or mobile data.
     *
     * @return `true` if either Wi-Fi or mobile data is connected, `false` otherwise.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Extracts TLV tags 85 and 86 from APDU responses in the execution result.
     * Parses each hex response, scans TLV data, and returns the full TLV strings
     * for tag 85 and tag 86 (if present) as a Pair.
     */
    fun generateTag85And86(executionResult: ScriptExecutionResult): Pair<String?, String?> {

        var tag85: String? = null
        var tag86: String? = null

        executionResult.apduResults.forEach { apdu ->

            val response = apdu.hexResponse ?: return@forEach

            val cleanHex = response
                .replace(" ", "")
                .replace("\n", "")
                .uppercase()

            var index = 0

            while (index < cleanHex.length - 4) {

                val tag = cleanHex.substring(index, index + 2)
                val lengthHex = cleanHex.substring(index + 2, index + 4)
                val length = lengthHex.toInt(16) * 2

                val tlvEnd = index + 4 + length
                if (tlvEnd > cleanHex.length) break

                val fullTLV = cleanHex.substring(index, tlvEnd)

                when (tag) {
                    Constants.TAG_85 -> tag85 = fullTLV
                    Constants.TAG_86 -> tag86 = fullTLV
                }

                index = tlvEnd
            }
        }

        return Pair(tag85, tag86)
    }

    /**
     * Extracts sdScript hex from INSTALL script APDU sequence 8.
     * Strips the trailing 9000 status word when present.
     *
     * @param apduResults APDU execution results from the INSTALL script run.
     * @return Uppercase sdScript hex without the trailing 9000 status word, or null when sequence 8 is absent or empty.
     */
    fun extractSdScript(apduResults: List<ApduExecutionResult>): String? {
        val seq8Result = apduResults.find { it.apduCommandId == "8" } ?: return null
        val raw = seq8Result.hexResponse?.replace("\\s+".toRegex(), "")?.uppercase().orEmpty()
        if (raw.isBlank()) return null
        val sdScript = if (raw.endsWith("9000")) raw.dropLast(4) else raw
        return sdScript.takeIf { it.isNotBlank() }
    }

    /**
     * Checks whether the user is currently not logged in.
     *
     * A user is considered not logged in if either:
     * - the saved email ID is empty, or
     * - the JWT authentication token is empty.
     *
     * @return `true` if the user is not logged in, otherwise `false`.
     */
    fun isUserNotLoggedIn(): Boolean {
        val email = StorageRepository.readString(PreferenceKey.EMAIL_ID)
        val jwtToken = StorageRepository.readString(PreferenceKey.JWT_TOKEN)
        return (email.isEmpty() || jwtToken.isEmpty())
    }
}
