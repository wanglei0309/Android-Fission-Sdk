// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: VerifyCodeFragment.kt manages User confirmation verification code capturing  during the Registration, Login and Forgot Password Flow.
 **/
package com.infineon.secora.wallet.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.databinding.FragmentVerifyCodeBinding
import com.infineon.secora.wallet.oidc.CognitoSignInFlowCoordinator
import com.infineon.secora.wallet.oidc.OIDCFLowHelper.OidcLoginType
import com.infineon.secora.wallet.oidc.OIDCFLowHelper.isValidVerificationCode
import com.infineon.secora.wallet.oidc.OIDCFLowHelper.maskEmail
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.ui.widget.AsteriskPasswordTransformationMethod
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.helper.UIHelper

class VerifyCodeFragment : BaseFragment() {
    private lateinit var binding: FragmentVerifyCodeBinding
    private lateinit var activity: MainActivity
    private lateinit var userName: String
    private lateinit var operationType: String

    /**
     * onCreateView method is used to inflate the layout for this fragment
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentVerifyCodeBinding.inflate(inflater, container, false)
        activity = (requireActivity() as MainActivity)
        userName = arguments?.getString(BundleKey.USERNAME).toString()
        operationType = arguments?.getString(BundleKey.OPERATION_TYPE).toString()

        activity.binding.toolbar.profileIcon.visibility = View.GONE
        UIHelper.showKeyboard(requireContext(), binding.etVerificationBox1)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        validationForVerificationCode()
        initListeners()
        startTimer()
        binding.tvConfirmationUsername.setText(maskEmail(userName))
        clearVerificationCodeData()
        dismissKeyboardOnTap(requireActivity(), binding.root)
        return binding.root
    }

    /**
     *  onViewCreated method is used to initialize the views for this fragment
     *
     * @param view
     * @param savedInstanceState
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.binding.toolbar.profileIcon.visibility = View.GONE
        showKeyboard(binding.etVerificationBox1)
        UIHelper.disablePasteOnInputs(
            requireContext(),
            binding.etVerificationBox1,
            binding.etVerificationBox2,
            binding.etVerificationBox3,
            binding.etVerificationBox4,
            binding.etVerificationBox5,
            binding.etVerificationBox6
        )

    }

    private fun initListeners() {

        binding.btnConfirm.setOnClickListener {

            val verificationCode = getVerificationCodeData()
            if (!isValidVerificationCode(verificationCode)) {
                val errorMessage = getString(R.string.verification_code_invalid)
                clearVerificationCodeData()
                statusDialog(requireContext(), errorMessage) {
                    binding.etVerificationBox1.postDelayed({
                        showKeyboard(binding.etVerificationBox1)
                    }, 100)
                }
                return@setOnClickListener
            }
            if (operationType == OidcLoginType.COGNITO_FORGOT_PWD.name) {
                val bundle = Bundle().apply {
                    putString(BundleKey.USERNAME, userName)
                    putString(BundleKey.VERIFICATION_CODE, verificationCode)
                }
                clearVerificationCodeData()
                findNavController().navigate(
                    R.id.confirmPasswordFragment, bundle
                )

                return@setOnClickListener
            }
            handleCognitoUserConfirmationOidcFlow(userName, verificationCode)
        }

        binding.tvResendVerificationCode.setOnClickListener {
            resendVerificationCode()
        }

    }

    /**
     * Collects and concatenates the six OTP input values into a single verification code.
     *
     * @return Six-digit verification code.
     */
    private fun getVerificationCodeData(): String {
        val otpBoxes = listOf(
            binding.etVerificationBox1.text.toString().trim(),
            binding.etVerificationBox2.text.toString().trim(),
            binding.etVerificationBox3.text.toString().trim(),
            binding.etVerificationBox4.text.toString().trim(),
            binding.etVerificationBox5.text.toString().trim(),
            binding.etVerificationBox6.text.toString().trim()
        )

        return otpBoxes.joinToString("")
    }

    /**
     * Clears all OTP input fields.
     */
    private fun clearVerificationCodeData() {
        binding.etVerificationBox1.text?.clear()
        binding.etVerificationBox2.text?.clear()
        binding.etVerificationBox3.text?.clear()
        binding.etVerificationBox4.text?.clear()
        binding.etVerificationBox5.text?.clear()
        binding.etVerificationBox6.text?.clear()
    }

    /**
     * Configures OTP input fields, masking behavior, and automatic focus navigation.
     */
    private fun validationForVerificationCode() {
        val etBox1 = binding.etVerificationBox1
        val etBox2 = binding.etVerificationBox2
        val etBox3 = binding.etVerificationBox3
        val etBox4 = binding.etVerificationBox4
        val etBox5 = binding.etVerificationBox5
        val etBox6 = binding.etVerificationBox6

        etBox1.transformationMethod = AsteriskPasswordTransformationMethod()
        etBox2.transformationMethod = AsteriskPasswordTransformationMethod()
        etBox3.transformationMethod = AsteriskPasswordTransformationMethod()
        etBox4.transformationMethod = AsteriskPasswordTransformationMethod()
        etBox5.transformationMethod = AsteriskPasswordTransformationMethod()
        etBox6.transformationMethod = AsteriskPasswordTransformationMethod()

        UIHelper.setupOtpInput(etBox1, etBox2)
        UIHelper.setupOtpBoxes(listOf(etBox1, etBox2, etBox3, etBox4, etBox5, etBox6))
    }

    /**
     * Confirms a Cognito user account using the supplied verification code.
     *
     * @param userName User account identifier.
     * @param confirmationCode Verification code received by the user.
     */
    private fun handleCognitoUserConfirmationOidcFlow(userName: String, confirmationCode: String) {
        activity.showLoading(true, getString(R.string.text_please_wait))
        CognitoSignInFlowCoordinator.confirmSignUp(requireContext(),
            userName, confirmationCode, {
                activity.runOnUiThread {
                    activity.showLoading(false, "")
                    statusDialog(requireActivity(), getString(R.string.user_confirmed_popup_text))

                    findNavController().navigate(
                        R.id.loginFragment
                    )
                }
            },
            { errorMessage ->
                activity.runOnUiThread {
                    activity.showLoading(false, "")

                    val verificationCodeErrorMessage = getString(R.string.verification_code_invalid)
                    clearVerificationCodeData()
                    if (errorMessage.equals(verificationCodeErrorMessage)) {
                        statusDialog(requireContext(), errorMessage) {
                            binding.etVerificationBox1.postDelayed({
                                showKeyboard(binding.etVerificationBox1)
                            }, 100)
                        }
                    } else {
                        statusDialog(requireActivity(), errorMessage)
                    }

                }
            })
    }

    /**
     * Resends the verification code for the currently displayed account.
     */
    private fun resendVerificationCode() {
        activity.showLoading(true, getString(R.string.text_please_wait))
        handleResendConfirmationCodeFlow(
            userName, {
                activity.runOnUiThread {
                    startTimer()
                    updateVerificationCodeButtonState(false)
                    activity.showLoading(false, "")
                    statusDialog(requireActivity(), getString(R.string.verification_code_sent_popup_text))
                }
            },
            { errorMessage ->
                activity.runOnUiThread {
                    activity.showLoading(false, "")
                    statusDialog(requireActivity(), errorMessage)
                }
            })
    }

    private fun updateVerificationCodeButtonState(state : Boolean) {
        binding.tvResendVerificationCode.isEnabled = state
    }

    private fun startTimer() {
        prepareOtp(binding.tvTimer, true, onTimeOut = {
            updateVerificationCodeButtonState(true)
        })
    }
}