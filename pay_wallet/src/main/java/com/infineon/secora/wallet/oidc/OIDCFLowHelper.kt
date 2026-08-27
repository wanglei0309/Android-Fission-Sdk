// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: Utility helper for OIDC and Cognito authentication flows.
 *
 * Provides:
 * - Authentication flow status constants
 * - JWT ID token parsing utilities
 * - User profile extraction from token claims
 * - Local storage of user account information
 *
 * This helper is primarily used to process authentication
 * responses and manage user information obtained from ID tokens.
 */

package com.infineon.secora.wallet.oidc

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.EditText
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.client.operations.common.logger.Logger
import com.infineon.secora.wallet.client.util.AppDispatchers
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.utils.StringUtils.isEmptyString
import com.infineon.secora.wallet.databinding.DialogCommonMessageBinding
import com.infineon.secora.wallet.utils.constants.JsonKey.GOOGLE_EMAIL
import com.infineon.secora.wallet.utils.constants.JsonKey.GOOGLE_NAME
import com.infineon.secora.wallet.utils.constants.JsonKey.GOOGLE_PICTURE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

object OIDCFLowHelper {

    private val logger: Logger =
        Logger.getNewLogger(OIDCFLowHelper::class.java.name)

    const val MFA_REQUIRED = "MFA Required"
    const val CONFIRM_WITH_PASSWORD = "Confirm with New Password"
    const val FAILED = "Failed"
    const val TOKEN_NOT_FOUND = "Token Not Found"
    const val MICROSOFT_INIT_FAILED = "MSAL application not initialized yet."
    private val PASSWORD_REGEX =
        Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$")

    enum class OidcLoginType(string: String) {
        COGNITO_REGISTER("Register"),
        COGNITO_LOGIN("Login"),
        COGNITO_FORGOT_PWD("Forget Password"),
        GOOGLE_LOGIN("Google Login"),
        MICROSOFT_LOGIN("Microsoft Login"),
        NONE("None")
    }

    /**
     * Extracts user account information from a JWT ID token.
     *
     * Decodes the token payload, retrieves user profile attributes,
     * and stores them locally for application use.
     *
     * Extracted attributes:
     * - User name
     * - Profile image URL
     * - Email address
     *
     * @param idToken JWT ID token returned by the authentication provider.
     * @return User email address if successfully extracted; otherwise an empty string.
     */
    fun extractAccountDetails(
        idToken: String
    ): String {
        return try {
            val payload = idToken.split(".").getOrNull(1) ?: return ""

            val decodedBytes = android.util.Base64.decode(
                payload,
                android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
            )

            val payloadJson = JSONObject(
                String(decodedBytes, Charsets.UTF_8)
            )

            StorageRepository.saveString(
                PreferenceKey.USER_NAME,
                payloadJson.optString(GOOGLE_NAME)
            )

            StorageRepository.saveString(
                PreferenceKey.PROFILE_IMAGE,
                payloadJson.optString(GOOGLE_PICTURE)
            )

            payloadJson.optString(GOOGLE_EMAIL)

        } catch (e: Exception) {
            logger.debug("Failed to extract email from ID token: ${e.message}")
            ""
        }
    }

    /**
     * Validates password and repeat password fields according to defined rules.
     * Displays appropriate error dialogs if any rule fails.
     *
     * @param context Context used to show dialogs.
     * @param password The password entered by the user.
     * @param repeatPassword The repeated password to confirm.
     * @param etPassword EditText reference of the password input field.
     * @param etRepeatPassword EditText reference of the repeat password input field.
     * @return True if input is valid; false otherwise.
     */
    fun validateInput(
        context: Context,
        password: String,
        repeatPassword: String,
        etPassword: EditText,
        etRepeatPassword: EditText
    ): Boolean {

        val passwordText = password.trim { it <= ' ' }
        val confirmPasswordText = repeatPassword.trim { it <= ' ' }

        val isPasswordEmpty = isEmptyString(passwordText)
        val isConfirmPasswordEmpty = isEmptyString(confirmPasswordText)
        val isValidPass = isValidPassword(context, passwordText, false)

        when {
            isPasswordEmpty -> {
                statusDialog(context, context.getString(R.string.text_enter_password))
                etPassword.requestFocus()
                return false
            }

            !isValidPass -> {
                statusDialog(context, context.getString(R.string.error_valid_pass))
                etPassword.requestFocus()
                return false
            }

            isConfirmPasswordEmpty -> {
                statusDialog(context, context.getString(R.string.error_empty_repeat_pass))
                etRepeatPassword.requestFocus()
                return false
            }

            passwordText != confirmPasswordText -> {
                statusDialog(context, context.getString(R.string.error_passwords_do_not_match))
                etRepeatPassword.requestFocus()
                return false
            }
        }
        return true
    }

    /**
     * Validates email input field for non-empty and correct format.
     *
     * @param context Context used to show dialogs.
     * @param etEmail EditText containing email input.
     * @return True if the email is valid; false otherwise.
     */
    fun validateEmailCredentials(
        context: Context,
        etEmail: EditText
    ): Boolean {

        val emailID = etEmail.text.toString().trim { it <= ' ' }
        val isValidEmail = isValidEmail(emailID)
        val isEmailEmpty = isEmptyString(emailID)

        when {
            isEmailEmpty -> {
                statusDialog(context, context.getString(R.string.prompt_email_address))
                etEmail.requestFocus()
                return false
            }

            !isValidEmail -> {
                statusDialog(context, context.getString(R.string.error_valid_email))
                etEmail.requestFocus()
                return false
            }
        }

        return true
    }

    /**
     * Validates an email address using Android email pattern matching.
     *
     * @param email Email address to validate.
     * @return True if the email address is valid.
     */
    private fun isValidEmail(email: String): Boolean {
        val isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

        return isValid
    }

    /**
     * Validates a password against application password requirements.
     *
     * @param context Context used to show dialogs.
     * @param password Password to validate.
     * @return True if the password satisfies all requirements.
     */
    fun isValidPassword(context: Context, password: String, displayError : Boolean = true): Boolean {
        val isValid = PASSWORD_REGEX.matches(password)
        if (displayError && !isValid) {
            handlePasswordValidationFailedCase(context, password)
        }
        return isValid
    }

    /**
     * Displays an appropriate validation error message for an invalid password.
     *
     * @param context Context used to show dialogs.
     * @param password Invalid password value.
     */
    private fun handlePasswordValidationFailedCase(context: Context, password: String) {
        //    Minimum 8 characters
        //    1 uppercase
        //    1 lowercase
        //    1 number
        //    1 special character

        val errorMessage = when {
            password.length < 8 ->
                context.getString(R.string.password_invalid_characters)

            !password.any { it.isUpperCase() } ->
                context.getString(R.string.password_invalid_uppercase)

            !password.any { it.isLowerCase() } ->
                context.getString(R.string.password_invalid_lowercase)

            !password.any { it.isDigit() } ->
                context.getString(R.string.password_invalid_number)

            else -> context.getString(R.string.password_invalid)
        }
        statusDialog(context, errorMessage)
    }

    /**
     * Validates a six-digit verification code.
     *
     * @param verificationCode Verification code to validate.
     * @return True if the code is valid.
     */
    fun isValidVerificationCode(verificationCode: String): Boolean {
        return verificationCode.matches(Regex("^\\d{6}$"))
    }

    /**
     * Displays a status dialog with a message and a dismissible "OK" button.
     *
     * @param context Context used to show dialogs.
     * @param message The status message to be shown.
     */
    private fun statusDialog(context: Context, message: String?) {
        CoroutineScope(AppDispatchers.MAIN).launch {
            val dialogViewBinding = DialogCommonMessageBinding.inflate(LayoutInflater.from(context))
            val alertDialog = Dialog(context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(dialogViewBinding.root)
                setCancelable(false)
            }
            dialogViewBinding.txtMessage.text = message
            dialogViewBinding.txtCancel.visibility = View.GONE

            dialogViewBinding.txtOK.setOnClickListener {
                alertDialog.dismiss()
            }

            alertDialog.show()
        }
    }

    /**
     * masks the email address.
     *
     * @param email Email address to be masked.
     * @return Masked Email address.
     */
    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email

      return "${parts[0].first()}***@${parts[1].first()}***"
    }
}