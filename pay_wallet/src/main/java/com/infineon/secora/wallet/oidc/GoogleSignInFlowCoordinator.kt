// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: Coordinates Google authentication using Android Credential Manager
 * and Google Identity Services.
 *
 * This class is responsible for:
 * - Initiating Google Sign-In
 * - Handling credential retrieval
 * - Processing Google ID tokens
 * - Extracting user account information
 * - Persisting authentication tokens
 * - Handling sign-in errors and configuration issues
 *
 * The coordinator provides a callback-based interface for integrating
 * Google authentication flows with application UI components.
 */
package com.infineon.secora.wallet.oidc

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.client.operations.common.logger.Logger
import com.infineon.secora.wallet.client.util.AppDispatchers
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.oidc.OIDCFLowHelper.extractAccountDetails
import com.infineon.secora.wallet.utils.constants.Constants.CANCEL
import com.infineon.secora.wallet.utils.constants.Constants.INTERRUPTED
import com.infineon.secora.wallet.utils.constants.Constants.INVALID
import com.infineon.secora.wallet.utils.constants.Constants.INVALID_CREDENTIAL_TYPE
import com.infineon.secora.wallet.utils.constants.Constants.NO_CREDENTIAL
import com.infineon.secora.wallet.utils.constants.Constants.SIGN_IN_CANCELLED
import com.infineon.secora.wallet.utils.constants.Constants.SIGN_IN_FAILED
import com.infineon.secora.wallet.utils.constants.Constants.SIGN_IN_INTERRUPTED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object GoogleSignInFlowCoordinator {

    private val logger: Logger =
        Logger.getNewLogger(GoogleSignInFlowCoordinator::class.java.name)

    /**
     * Launches the Google Sign-In flow using Credential Manager.
     *
     * Requests a Google account credential and retrieves a Google ID token
     * upon successful authentication.
     *
     * @param context Activity or application context used for credential requests.
     * @param onSuccess Invoked with the authenticated user's email address.
     * @param onFailure Invoked when sign-in fails or is cancelled.
     */
    fun loginWithGoogle(
        context: Context,
        onSuccess: (String) -> Unit,
        onFailure: (String?) -> Unit
    ) {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(
                context.getString(R.string.default_web_client_id)
            )
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(AppDispatchers.MAIN).launch {
            runCatching {
                credentialManager.getCredential(context, request)
            }.onSuccess { response ->
                handleGoogleSignInSuccess(
                    context,
                    response,
                    onSuccess,
                    onFailure
                )
            }.onFailure { throwable ->
                handleGoogleSignInFailure(
                    context,
                    throwable,
                    onFailure
                )
            }
        }
    }

    /**
     * Processes a successful Google credential response.
     *
     * Extracts the Google ID token, stores it locally, retrieves
     * user account details from the token claims, and returns
     * the authenticated email address.
     *
     * @param context Application context.
     * @param response Credential Manager authentication response.
     * @param onSuccess Invoked with the authenticated user's email address.
     * @param onFailure Invoked when credential parsing fails.
     */
    private fun handleGoogleSignInSuccess(
        context: Context,
        response: GetCredentialResponse,
        onSuccess: (String) -> Unit,
        onFailure: (String?) -> Unit
    ) {
        try {
            val credential = response.credential

            val googleCredential =
                GoogleIdTokenCredential.createFrom(credential.data)

            val idToken = googleCredential.idToken


            StorageRepository.saveString(
                PreferenceKey.JWT_TOKEN,
                idToken
            )

            val email = extractAccountDetails(idToken)

            logger.debug("Google Sign-In successful")
            onSuccess(email)

        } catch (e: GoogleIdTokenParsingException) {
            logger.error("Failed to parse Google credential :${e.message}")
            onFailure(
                context.getString(
                    R.string.text_failed_to_parse_credentials
                )
            )
        }
    }

    /**
     * Handles Google Sign-In failures and converts platform-specific
     * exceptions into user-friendly error messages.
     *
     * Supports detection of common failure scenarios such as:
     * - User cancellation
     * - Missing credentials
     * - Configuration errors
     * - Invalid credential types
     * - Interrupted authentication flows
     *
     * @param context Application context.
     * @param e Exception thrown during the sign-in process.
     * @param onFailure Invoked with a descriptive error message.
     */
    private fun handleGoogleSignInFailure(
        context: Context,
        e: Throwable,
        onFailure: (String?) -> Unit
    ) {
        logger.debug("Google Sign-In failed: ${e.message}")
        logger.debug("Google Sign-In exception type: ${e.javaClass.simpleName}")
        logger.debug("Google Sign-In exception: ${e.toString()}")
        logger.debug("Google Sign-In stack trace: ${e.stackTraceToString()}")

            val errorMessage = when (e) {
                is GetCredentialException -> {
                    val errorMsg = e.message ?: ""
                    val errorType = e.javaClass.simpleName
                    logger.debug("GetCredentialException type: $errorType, message: $errorMsg")

                    when {
                        errorMsg.contains(CANCEL, ignoreCase = true) -> SIGN_IN_CANCELLED
                        errorMsg.contains(NO_CREDENTIAL, ignoreCase = true) ||
                            errorMsg.contains("no credentials", ignoreCase = true) ||
                            errorMsg.contains("credentials available", ignoreCase = true) ||
                            errorMsg.contains("28433", ignoreCase = true) ||
                            errorMsg.contains("Cannot find a matching credential", ignoreCase = true) -> {
                            // Error code 28433 indicates configuration issue
                            val packageName = context.packageName
                            "Google Sign-In Configuration Error\n\n" +
                                "This usually means:\n" +
                                "1. SHA-1 fingerprint not registered\n" +
                                "   Get SHA-1: keytool -list -v -keystore <keystore> -alias <alias>\n" +
                                "   Register at: Google Cloud Console > APIs & Services > Credentials\n\n" +
                                "2. Package name mismatch\n" +
                                "   Current: $packageName\n" +
                                "   Must match Google Cloud Console\n\n" +
                                "3. No Google account on device\n" +
                                "   Add account in Settings > Accounts\n\n" +
                                "Error: $errorMsg"
                        }

                        errorMsg.contains(INTERRUPTED, ignoreCase = true) -> SIGN_IN_INTERRUPTED
                        errorMsg.contains(INVALID, ignoreCase = true) -> INVALID_CREDENTIAL_TYPE
                        else -> "$SIGN_IN_FAILED $errorMsg"
                    }
                }

                else -> "$SIGN_IN_FAILED ${e.message}"
            }
        onFailure(errorMessage)
    }
}