// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: Coordinates AWS Cognito authentication operations using Amplify Auth.
 *
 * This class provides APIs for:
 * - User sign-in and sign-out
 * - User registration and account confirmation
 * - Password reset workflows
 * - Session validation
 * - JWT token retrieval
 * - Verification code resend operations
 *
 * It handles Cognito authentication state transitions and exposes
 * success/failure callbacks for UI flow management.
 */
package com.infineon.secora.wallet.oidc

import android.content.Context
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.cognito.exceptions.service.CodeMismatchException
import com.amplifyframework.auth.cognito.exceptions.service.UserNotConfirmedException
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.step.AuthResetPasswordStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.core.Amplify
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.client.operations.common.logger.Logger
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.oidc.OIDCFLowHelper.extractAccountDetails

object CognitoSignInFlowCoordinator {
    private val logger: Logger =
        Logger.getNewLogger(CognitoSignInFlowCoordinator::class.java.name)

    /**
     * Authenticates a user using username and password credentials.
     *
     * Handles Cognito sign-in challenges such as:
     * - SMS MFA verification
     * - New password requirement
     * - Successful authentication
     *
     * @param username User email or username.
     * @param password User password.
     * @param onSuccess Invoked when sign-in completes successfully.
     * @param onFailure Invoked when authentication fails or additional steps are required.
     */
    fun signIn(username : String, password : String,
               onSuccess: (Boolean) -> Unit,
               onFailure: (String?) -> Unit) {

        Amplify.Auth.signIn(
            username,
            password,
            { result ->
                logger.debug("SignedIn :: result.isSignedIn : ${result.isSignedIn}")
                logger.debug("SignedIn :: result.nextStep : ${result.nextStep}")
                when (result.nextStep.signInStep) {

                    AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE -> {
                        // Show OTP screen
                        onFailure(OIDCFLowHelper.MFA_REQUIRED)
                    }

                    AuthSignInStep.CONFIRM_SIGN_IN_WITH_NEW_PASSWORD -> {
                        // Show new password screen
                        onFailure(OIDCFLowHelper.CONFIRM_WITH_PASSWORD)
                    }

                    AuthSignInStep.DONE -> {
                        onSuccess(true)
                    }

                    else -> {
                        onFailure(OIDCFLowHelper.FAILED)
                    }
                }

            },
            { error ->
                logger.debug("SignedIn :: error : $error")
                if (error is UserNotConfirmedException) {
                    onSuccess(false)
                    return@signIn
                }
                onFailure(error.message)
            }
        )
    }

    /**
     * Registers a new user account in Cognito.
     *
     * Creates the user and initiates the email verification process.
     *
     * @param username User email address.
     * @param password User password.
     * @param onSuccess Invoked when sign-up request is successful.
     * @param onFailure Invoked when registration fails.
     */
    fun signUp(username : String, password : String,
               onSuccess: (Boolean) -> Unit,
               onFailure: (String?) -> Unit) {
        val options = AuthSignUpOptions.builder()
            .userAttribute(
                    AuthUserAttributeKey.email(),
                    username
            )
            .build()

        Amplify.Auth.signUp(
            username,
            password,
            options,
            { result ->
                logger.debug("SignedUp :: result.isSignUpComplete : ${result.isSignUpComplete}")
                logger.debug("SignedUp :: result.userId : ${result.userId}")
                logger.debug("SignedUp :: result.nextStep : ${result.nextStep}")
                onSuccess(true)
            },
            { error ->
                logger.debug("SignedUp :: error : $error")
                onFailure(error.message)
            }
        )
    }

    /**
     * Confirms user registration using the verification code
     * received through email or SMS.
     *
     * Attempts automatic sign-in when supported by Cognito.
     *
     * @param context context.
     * @param username User email or username.
     * @param confirmationCode Verification code received by the user.
     * @param onSuccess Invoked when confirmation succeeds.
     * @param onFailure Invoked when confirmation fails.
     */
    fun confirmSignUp(context : Context, username : String, confirmationCode : String,
                      onSuccess: (Boolean) -> Unit,
                      onFailure: (String?) -> Unit) {
        Amplify.Auth.confirmSignUp(
            username,
            confirmationCode,
            { result ->
                logger.debug("ConfirmSignedUp :: result.isSignUpComplete : ${result.isSignUpComplete}")
                logger.debug("ConfirmSignedUp :: result.userId : ${result.userId}")
                logger.debug("ConfirmSignedUp :: result.nextStep : ${result.nextStep}")
                when (result.nextStep.signUpStep) {

                    AuthSignUpStep.COMPLETE_AUTO_SIGN_IN -> {

                        Amplify.Auth.autoSignIn(
                            { signInResult ->
                                logger.debug("Auto SignIn :: signInResult.isSignedIn : ${signInResult.isSignedIn}")
                                logger.debug("Auto SignIn :: Auto SignIn signInResult.nextStep : ${signInResult.nextStep}")

                                when (signInResult.nextStep.signInStep) {

                                    AuthSignInStep.DONE -> {
                                        logger.debug("Auto SignIn :: success")
                                        onSuccess(true)
                                    }

                                    AuthSignInStep.CONTINUE_SIGN_IN_WITH_FIRST_FACTOR_SELECTION -> {
                                        onSuccess(false)
                                    }

                                    else -> {
                                        onFailure(OIDCFLowHelper.FAILED)
                                    }
                                }
                            },
                            { error ->
                                logger.debug("Auto SignIn :: error : $error")
                                onFailure(error.message)
                            }
                        )
                    }

                    AuthSignUpStep.DONE -> {
                        // Sign-up complete
                        onSuccess(true)
                    }

                    else -> {
                        onFailure(OIDCFLowHelper.FAILED)
                    }
                }
            },
            { error ->
                logger.debug("ConfirmSignedUp :: error : $error")
                if (error is CodeMismatchException) {
                    val errorMessage = context.getString(R.string.verification_code_invalid)
                    onFailure(errorMessage)
                } else {
                    onFailure(error.message)
                }
            }
        )
    }

    /**
     * Initiates the password reset process.
     *
     * Requests Cognito to send a verification code to the user's
     * registered email or phone number.
     *
     * @param username User email or username.
     * @param onSuccess Invoked when the reset code is successfully sent.
     * @param onFailure Invoked when the reset request fails.
     */
    fun resetPassword(username : String, onSuccess: (Boolean) -> Unit,
                      onFailure: (String?) -> Unit) {
        Amplify.Auth.resetPassword(
            username,
            { result ->
                logger.debug("Reset Password :: result.nextStep : ${result.nextStep}")
                logger.debug("Reset Password :: result.isPasswordReset : ${result.isPasswordReset}")

                when (result.nextStep.resetPasswordStep) {

                    AuthResetPasswordStep.CONFIRM_RESET_PASSWORD_WITH_CODE -> {

                        val details = result.nextStep.codeDeliveryDetails

                        val destination = details?.destination
                        val medium = details?.deliveryMedium

                        logger.debug("Reset Password :: Code sent to $destination via $medium")
                        onSuccess(true)
                        // Navigate to OTP + New Password screen
                    }

                    AuthResetPasswordStep.DONE -> {
                        logger.debug("Reset Password :: Password reset complete")
                        onSuccess(false)
                    }
                     else -> {
                         onFailure(OIDCFLowHelper.FAILED)
                     }
                }

            },
            { error ->
                logger.debug("Reset Password :: error : $error")
                onFailure(error.message)
            }
        )
    }

    /**
     * Completes the password reset flow using the verification code
     * and a new password.
     *
     * @param context context.
     * @param username User email or username.
     * @param newPassword New password to be set.
     * @param confirmationCode Verification code received by the user.
     * @param onSuccess Invoked when password reset succeeds.
     * @param onFailure Invoked when password reset fails.
     */
    fun confirmResetPassword(context : Context, username : String, newPassword : String, confirmationCode : String, onSuccess: (Boolean) -> Unit,
                             onFailure: (String?) -> Unit) {
        Amplify.Auth.confirmResetPassword(
            username,
            newPassword,
            confirmationCode,
            {
                logger.debug("Confirm Reset Password :: Success")
                onSuccess(true)
            },
            {error ->
                logger.debug("Confirm Reset Password :: error : $error")
                if (error is CodeMismatchException) {
                    val errorMessage = context.getString(R.string.verification_code_invalid)
                    onFailure(errorMessage)
                } else {
                    onFailure(error.message)
                }

            }
        )
    }

    /**
     * Retrieves the current authenticated user's ID token and
     * extracts account information from it.
     *
     * The token is stored locally for future authenticated requests.
     *
     * @param onSuccess Returns the authenticated user's email.
     * @param onFailure Invoked when token retrieval fails.
     */
    fun fetchTokenDetails(onSuccess: (String) -> Unit,
        onFailure: (String?) -> Unit) {
        Amplify.Auth.fetchAuthSession(
            { session ->

                val cognitoSession =
                    session as AWSCognitoAuthSession


                val idToken =
                    cognitoSession.userPoolTokensResult.value?.idToken

                logger.debug("fetchAuthSession :: cognitoSession.userPoolTokensResult : ${cognitoSession.userPoolTokensResult}")
                logger.debug("fetchAuthSession :: cognito id : $idToken")
                if (idToken == null) {
                    onFailure(OIDCFLowHelper.TOKEN_NOT_FOUND)
                    return@fetchAuthSession
                }
                StorageRepository.saveString(
                    PreferenceKey.JWT_TOKEN,
                    idToken
                )

                val email = extractAccountDetails(idToken)

                logger.debug("fetchAuthSession :: Cognito Sign-In successful")
                onSuccess(email)

            },
            { error ->
                logger.debug("fetchAuthSession :: error : $error")
                onFailure(error.message)
            }
        )
    }

    /**
     * Signs the current user out of Cognito.
     *
     * Performs a global sign-out to invalidate active sessions
     * across all devices.
     *
     * @param onSuccess Invoked when sign-out completes successfully.
     * @param onFailure Invoked when sign-out fails.
     */
    fun signOut(
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit
    ) {
        val options = AuthSignOutOptions.builder()
            .globalSignOut(true)
            .build()
        Amplify.Auth.signOut(options) { result ->

            logger.debug("SignOut :: result : ${result}")
            when (result) {

                is AWSCognitoAuthSignOutResult.CompleteSignOut -> {
                    onSuccess()
                }

                is AWSCognitoAuthSignOutResult.PartialSignOut -> {
                    onSuccess()
                }

                is AWSCognitoAuthSignOutResult.FailedSignOut -> {
                    onFailure(result.exception.message)
                }
            }
        }
    }

    /**
     * Checks whether a valid Cognito user session currently exists.
     *
     * @param onSuccess Returns true when the user is authenticated.
     * @param onFailure Invoked when session validation fails.
     */
    fun checkForUserSession(onSuccess: (Boolean) -> Unit,
                          onFailure: (String?) -> Unit) {
        Amplify.Auth.fetchAuthSession(
            { session ->
                onSuccess(session.isSignedIn)
                logger.debug("User Session :: session.isSignedIn : ${session.isSignedIn}")

            },
            { error ->
                logger.debug("User Session :: error : $error")
                onFailure(error.message)
            }
        )
    }

    /**
     * Resends the account verification code for users who have not
     * completed the registration confirmation process.
     *
     * @param username User email or username.
     * @param onSuccess Invoked when the verification code is resent.
     * @param onFailure Invoked when the resend operation fails.
     */
    fun resendConfirmationCode(username: String, onSuccess: (Boolean) -> Unit,
                          onFailure: (String?) -> Unit) {
        Amplify.Auth.resendSignUpCode(
            username,
            { result ->
                // OTP sent again
                 logger.debug("Resend Code :: Destination: ${result.destination}")
                 logger.debug("Resend Code :: Medium: ${result.deliveryMedium}")
                 logger.debug("Resend Code :: Attribute: ${result.attributeName}")
                onSuccess(true)
            },
            { error ->
                logger.debug("Resend Code :: error : $error")
                onFailure(error.message)
            }
        )
    }
}