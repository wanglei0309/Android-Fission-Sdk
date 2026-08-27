// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Coordinates the Microsoft Sign-In authentication flow using the Microsoft Authentication Library (MSAL).
 *
 * Responsibilities:
 * - Initializes the MSAL Single Account application.
 * - Handles interactive user sign-in.
 * - Retrieves and stores the ID token.
 * - Checks whether a Microsoft account session already exists.
 * - Signs out the currently authenticated user.
 *
 * This implementation uses the Single Account Mode provided by MSAL,
 * allowing only one Microsoft account to be signed in at a time.
 */
package com.infineon.secora.wallet.oidc

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.client.operations.common.logger.Logger
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.oidc.OIDCFLowHelper.MICROSOFT_INIT_FAILED
import com.infineon.secora.wallet.oidc.OIDCFLowHelper.extractAccountDetails
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.exception.MsalException

object MicrosoftSignInFlowCoordinator {
    private val logger: Logger =
        Logger.getNewLogger(MicrosoftSignInFlowCoordinator::class.java.name)
    private lateinit var mSingleAccountApp: ISingleAccountPublicClientApplication

    /**
     * Initializes the MSAL Single Account Public Client Application.
     *
     * This method must be called before invoking any authentication-related
     * operations such as login, session validation, or logout.
     *
     * @param context Application or Activity context used to load the MSAL configuration.
     */
    fun init(context: Context) {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            R.raw.msal_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {

                // Fixed the type here to ISingleAccountPublicClientApplication
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    mSingleAccountApp = application
                    logger.debug("Microsoft :: init : onCreated")
                }

                override fun onError(exception: MsalException) {
                    exception.printStackTrace()
                    logger.debug("Microsoft ::  init : onError ${exception.message}")
                }
            }
        )
    }

    /**
     * Starts the Microsoft interactive sign-in flow.
     *
     * Displays the Microsoft authentication UI and requests the configured
     * scopes from the user. Upon successful authentication, the user's ID token
     * is extracted, stored locally, and the email address is returned.
     *
     * Required scopes:
     * - openid
     * - profile
     * - email
     * - User.Read
     *
     * @param activity Activity used to launch the Microsoft authentication UI.
     * @param onSuccess Callback invoked with the authenticated user's email address.
     * @param onFailure Callback invoked when authentication fails or is cancelled.
     */
    fun login(
        activity: FragmentActivity,
        onSuccess: (String) -> Unit,
        onFailure: (String?) -> Unit
    ) {
        if (!::mSingleAccountApp.isInitialized) {
            logger.debug("Microsoft :: login : $MICROSOFT_INIT_FAILED")
            onFailure(MICROSOFT_INIT_FAILED)
            return
        }

        val signInParameters = SignInParameters.builder()
            .withActivity(activity)                // Current Activity Context
            .withLoginHint(null)               // Optional: pre-fill user email
            .withScopes(listOf( "openid",
                "profile",
                "email",
                "User.Read"))       // Must be a List<String> now
            .withCallback(getAuthInteractiveCallback(onSuccess, onFailure))
            .build()

        mSingleAccountApp.signIn(signInParameters)

    }

    /**
     * Creates an authentication callback for handling Microsoft sign-in results.
     *
     * Callback scenarios:
     * - Success: Retrieves and stores the ID token, extracts the user's email,
     *   and invokes the success callback.
     * - Error: Returns the MSAL exception message.
     * - Cancel: Indicates that the user cancelled the authentication flow.
     *
     * @param onSuccess Callback invoked with the authenticated user's email.
     * @param onFailure Callback invoked with an error message.
     * @return AuthenticationCallback implementation used by MSAL.
     */
    private fun getAuthInteractiveCallback(
        onSuccess: (String) -> Unit,
        onFailure: (String?) -> Unit): AuthenticationCallback {
        return object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                val idToken = authenticationResult.account.idToken

                if (idToken == null) {
                    onFailure(OIDCFLowHelper.TOKEN_NOT_FOUND)
                    return
                }
                StorageRepository.saveString(
                    PreferenceKey.JWT_TOKEN,
                    idToken
                )

                val email = extractAccountDetails(idToken)
                onSuccess(email)
            }

            override fun onError(exception: MsalException) {
                exception.printStackTrace()
                logger.debug("Microsoft :: login : onError : ${exception.message}")
                onFailure(exception.message)
            }

            override fun onCancel() {
                logger.debug("Microsoft :: login : onCancel ")
                onFailure("User Cancelled")
            }
        }
    }

    /**
     * Checks whether a Microsoft account is currently signed in.
     *
     * Uses MSAL's account cache to determine whether an active account
     * exists on the device.
     *
     * @param onSuccess Callback invoked with:
     * - true if an active account exists.
     * - false if no account is signed in.
     * @param onFailure Callback invoked when an error occurs while retrieving account information.
     */
    fun checkUserSession(onSuccess: (Boolean) -> Unit,
                             onFailure: (String?) -> Unit) {
        if (!::mSingleAccountApp.isInitialized) {
            logger.debug("Microsoft :: checkUserSession : $MICROSOFT_INIT_FAILED")
            onFailure(MICROSOFT_INIT_FAILED)
            return
        }
        mSingleAccountApp.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                if (activeAccount != null) {
                    logger.debug("Microsoft :: checkUserSession : Loaded existing account: ${activeAccount.username}")
                } else {
                    logger.debug("Microsoft :: checkUserSession : account null")
                }
                onSuccess(activeAccount != null)
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                logger.debug("Microsoft :: checkUserSession : onAccountChanged")
                onFailure("onAccountChanged")
            }

            override fun onError(exception: MsalException) {
                logger.debug("Microsoft :: checkUserSession : Error loading account: ${exception.message}")
                onFailure(exception.message)
            }
        })
    }

    /**
     * Signs out the currently authenticated Microsoft account.
     *
     * Clears the MSAL account session and removes the active account from
     * the application context.
     *
     * @param onSuccess Callback invoked when sign-out completes successfully.
     * @param onFailure Callback invoked if sign-out fails.
     */
    fun logout(onSuccess: (Boolean) -> Unit,
                       onFailure: (String?) -> Unit) {
        if (!::mSingleAccountApp.isInitialized) {
            logger.debug("Microsoft :: logout : $MICROSOFT_INIT_FAILED")
            onFailure(MICROSOFT_INIT_FAILED)
            return
        }
        // Trigger the sign out flow asynchronously
        mSingleAccountApp.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {


            override fun onSignOut() {
                logger.debug("Microsoft :: logout : onSignOut")
                onSuccess(true)
            }

            override fun onError(exception: MsalException) {
                logger.debug("Microsoft :: logout : onError ${exception.message}")
                onFailure(exception.message)
            }
        })

    }

}