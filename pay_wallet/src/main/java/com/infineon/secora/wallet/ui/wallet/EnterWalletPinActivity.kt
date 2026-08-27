// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: EnterWalletPinActivity.kt verifies the user’s wallet PIN or biometric authentication before granting access.
 * checks the entered PIN against stored data, supports fingerprint/face authentication,
 * and navigates to the home screen upon successful verification.
 **/
package com.infineon.secora.wallet.ui.wallet

import android.os.Bundle
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.infineon.secora.wallet.BuildConfig
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.FragmentWalletEnterPinBinding
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.ui.fragment.BaseFragment
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.constants.Constants.PIN_DELAY
import com.infineon.secora.wallet.utils.constants.Constants.PIN_LENGTH
import com.infineon.secora.wallet.utils.helper.ScreenCaptureProtection
import com.infineon.secora.wallet.ui.widget.PinEntryEditText

/**
 * Activity responsible for handling wallet PIN entry and biometric authentication.
 *
 * This class extends [BaseFragment] and manages user authentication by:
 * - Allowing manual PIN entry using [PinEntryEditText]
 * - Optionally authenticating using biometrics (fingerprint/face)
 * - Navigating to the home screen upon successful authentication
 *
 */
class EnterWalletPinActivity : BaseFragment() {

    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)
    private lateinit var binding: FragmentWalletEnterPinBinding
    private var tokenRefNumber: String? = null

    /**
     * Called when the activity is first created.
     * - Initializes view binding.
     * - Retrieves the token reference number from intent extras.
     * - Set up PIN entry validation logic.
     * - Initiates biometric authentication if available.
     *
     * @param savedInstanceState the saved instance state from a previous configuration (if any)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentWalletEnterPinBinding.inflate(layoutInflater)
        ScreenCaptureProtection.apply(requireActivity())
        requireActivity().setContentView(binding.root)

        // Get tokenRefNum data from bundle
        val bundle: Bundle? = requireActivity().intent.extras
        if (bundle != null && !bundle.isEmpty) {
            tokenRefNumber = bundle.getString(BundleKey.TOKEN_REFERENCE_NUMBER)
        }

        if (binding.txtWalletPinEntry.text != null) {
            binding.txtWalletPinEntry.setAnimateText(true)
            binding.txtWalletPinEntry.setOnPinEnteredListener { str ->
                if (str.toString().length == PIN_LENGTH) {
                    val walletPin = StorageRepository.readString(PreferenceKey.WALLET_PIN)
                    if (walletPin == binding.txtWalletPinEntry.text.toString()) {
                        navigateToHomeScreen()
                    } else {
                        binding.txtWalletPinEntry.isError = true
                        showToast(
                            resources.getString(R.string.wallet_pin_mismatch),
                        )
                        binding.txtWalletPinEntry.postDelayed({
                            binding.txtWalletPinEntry.text = null
                        }, PIN_DELAY)
                    }
                }
            }
        }

        if (BuildConfig.ENABLE_BIOMETRIC) {
            if (isBiometricAvailable()) {
                authenticateWithBiometrics(requireActivity())
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.text_biometric_not_available),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Checks if biometric authentication (fingerprint/face) is available and enabled on the device.
     *
     * @return `true` if biometric authentication can be used, otherwise `false`
     */
    private fun isBiometricAvailable(): Boolean {
        if (!BuildConfig.ENABLE_BIOMETRIC) return false
        val biometricManager = BiometricManager.from(requireContext())
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    /**
     * Prompts the user for biometric authentication using [BiometricPrompt].
     *
     * @param activity The hosting [FragmentActivity] used to display the biometric prompt.
     *
     * Handles three outcomes:
     * - **Success:** Navigates to the home screen.
     * - **Failure:** Displays an authentication failed message.
     * - **Error:** Displays the error reason.
     */
    private fun authenticateWithBiometrics(activity: FragmentActivity) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt =
            BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(activity, getString(R.string.text_authentication_succeeded), Toast.LENGTH_SHORT)
                        .show()
                    navigateToHomeScreen()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(activity, getString(R.string.text_authentication_failed), Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    val errorMessage = getString(R.string.text_authentication_error, errString)
                    Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT)
                        .show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.text_biometric_authentication))
            .setSubtitle(getString(R.string.text_use_biometric))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Navigates the user to the home screen after successful authentication.
     */
    private fun navigateToHomeScreen() {
        logger.info("navigateToHomeScreen")
    }
}