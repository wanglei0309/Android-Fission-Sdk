// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: ForgotPasswordFragment.kt manages user to  new password  creation/validation along with verification code validation.
 *
 **/
package com.infineon.secora.wallet.ui.fragment

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.databinding.FragmentConfirmResetPasswordBinding
import com.infineon.secora.wallet.oidc.CognitoSignInFlowCoordinator
import com.infineon.secora.wallet.oidc.OIDCFLowHelper.validateInput
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.utils.constants.BundleKey

class ConfirmPasswordFragment : BaseFragment() {
    private lateinit var binding: FragmentConfirmResetPasswordBinding
    private lateinit var activity: MainActivity
    private lateinit var userName: String
    private lateinit var verificationCode: String

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
        binding = FragmentConfirmResetPasswordBinding.inflate(inflater, container, false)
        activity = (requireActivity() as MainActivity)

        activity.binding.toolbar.profileIcon.visibility = View.GONE
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        userName = arguments?.getString(BundleKey.USERNAME).toString()
        verificationCode = arguments?.getString(BundleKey.VERIFICATION_CODE).toString()

        dismissKeyboardOnTap(requireActivity(), binding.root)
        initListeners()
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
    }

    private fun initListeners() {

        binding.ivResetToggleEye.setOnClickListener {
            handleResetPasswordVisibilityToggle()
        }
        binding.ivRepeatResetToggleEye.setOnClickListener {
            handleRepeatResetPasswordVisibilityToggle()
        }

        binding.btnConfirmReset.setOnClickListener {

            val password = binding.etNewPassword.text.toString()
            val repeatPassword = binding.etRepeatNewPassword.text.toString()
            if (!validateInput(
                    requireContext(),
                    password,
                    repeatPassword,
                    binding.etNewPassword,
                    binding.etRepeatNewPassword
                )
            ) {
                return@setOnClickListener
            }

            resetPasswordConfirmationFlow()
        }
    }

    /**
     * Toggles visibility of the reset password field and updates
     * the visibility icon while preserving the cursor position.
     */
    private fun handleResetPasswordVisibilityToggle() {
        isPasswordVisible = !isPasswordVisible

        if (isPasswordVisible) {
            binding.etNewPassword.transformationMethod =
                HideReturnsTransformationMethod.getInstance()
            binding.ivResetToggleEye.setImageResource(R.drawable.eye_open)
        } else {
            binding.etNewPassword.transformationMethod =
                PasswordTransformationMethod.getInstance()
            binding.ivResetToggleEye.setImageResource(R.drawable.eye_slash)
        }

        // Keep cursor at end
        binding.etNewPassword.setSelection(
            binding.etNewPassword.text?.length ?: 0
        )
    }

    /**
     * Toggles visibility of the reset password field and updates
     * the visibility icon while preserving the cursor position.
     */
    private fun handleRepeatResetPasswordVisibilityToggle() {
        isPasswordVisible = !isPasswordVisible

        if (isPasswordVisible) {
            binding.etRepeatNewPassword.transformationMethod =
                HideReturnsTransformationMethod.getInstance()
            binding.ivRepeatResetToggleEye.setImageResource(R.drawable.eye_open)
        } else {
            binding.etRepeatNewPassword.transformationMethod =
                PasswordTransformationMethod.getInstance()
            binding.ivRepeatResetToggleEye.setImageResource(R.drawable.eye_slash)
        }

        // Keep cursor at end
        binding.etRepeatNewPassword.setSelection(
            binding.etRepeatNewPassword.text?.length ?: 0
        )
    }

    /**
     * Completes the password reset operation using the verification
     * code and newly supplied password.
     */
    private fun resetPasswordConfirmationFlow() {
        activity.showLoading(true, getString(R.string.text_please_wait))
        val newPassword = binding.etNewPassword.text.toString()
        CognitoSignInFlowCoordinator.confirmResetPassword(requireContext(),
            userName, newPassword, verificationCode, {
                activity.runOnUiThread {

                    activity.showLoading(false, "")
                    statusDialog(requireActivity(), getString(R.string.password_reset_success_popup_text))
                    findNavController().navigate(
                        R.id.loginFragment
                    )
                }
            },
            { errorMessage ->
                activity.runOnUiThread {
                    activity.showLoading(false, "")
                    statusDialog(requireActivity(), errorMessage)
                }
            })
    }

}