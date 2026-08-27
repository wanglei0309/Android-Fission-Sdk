// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: RegisterFragment.kt manages user registration flow, once sign up completed then the flow will be navigated to sign up confirmation screen.
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
import com.infineon.secora.wallet.databinding.FragmentRegisterBinding
import com.infineon.secora.wallet.oidc.CognitoSignInFlowCoordinator
import com.infineon.secora.wallet.oidc.OIDCFLowHelper
import com.infineon.secora.wallet.oidc.OIDCFLowHelper.validateEmailCredentials
import com.infineon.secora.wallet.oidc.OIDCFLowHelper.validateInput
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.utils.constants.BundleKey

class RegisterFragment : BaseFragment() {
    private lateinit var binding: FragmentRegisterBinding
    private lateinit var activity: MainActivity

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
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        activity = (requireActivity() as MainActivity)
        activity.binding.toolbar.profileIcon.visibility = View.GONE
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
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

        binding.btnRegister.setOnClickListener {
            handleRegisterButtonClickEvent()
        }

        binding.btnNavigateToLogin.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.ivRegisterToggleEye.setOnClickListener {
            handleRegisterPasswordVisibilityToggle()
        }
        binding.ivRepeatPwdToggleEye.setOnClickListener {
            handleRegisterRepeatPasswordVisibilityToggle()
        }

        binding.ivPasswordRequirements.setOnClickListener {
            statusDialog(requireContext(), getString(R.string.text_password_requirements))
        }
    }

    /**
     * Validates registration form inputs and initiates the Cognito
     * user registration flow when all validations pass.
     */
    private fun handleRegisterButtonClickEvent() {
        if (!validateEmailCredentials(requireContext(), binding.etRegisterUsername)) {
            return
        }
        if (!validateInput(
                requireContext(), binding.etRegisterPassword.text.toString(),
                binding.etRepeatPassword.text.toString(),
                binding.etRegisterPassword,
                binding.etRepeatPassword
            )
        ) {
            return
        }
        handleCognitoRegisterOidcFlow()
    }

    /**
     * Starts the Cognito Register flow after validating any existing user session.
     */
    private fun handleCognitoRegisterOidcFlow() {
        activity.showLoading(true, getString(R.string.text_please_wait))
        validateUserSession({
            proceedWithCognitoRegistrationFlow()
        })
    }


    /**
     * Registers a new Cognito user account using the registration form data.
     */
    private fun proceedWithCognitoRegistrationFlow() {
        val userName = binding.etRegisterUsername.text.toString()
        val password = binding.etRegisterPassword.text.toString()
        val repeatPassword = binding.etRepeatPassword.text.toString()
        if (!validateEmailCredentials(requireContext(), binding.etRegisterUsername)) {
            activity.showLoading(false, "")
            return
        }
        if (!validateInput(
                requireContext(),
                password,
                repeatPassword,
                binding.etRegisterPassword,
                binding.etRepeatPassword
            )
        ) {
            activity.showLoading(false, "")
            return
        }
        CognitoSignInFlowCoordinator.signUp(
            userName, password, {
                activity.runOnUiThread {
                    activity.showLoading(false, "")
                    statusDialog(requireActivity(), getString(R.string.verification_code_sent_popup_text))
                    val bundle = Bundle().apply {
                    putString(BundleKey.USERNAME, userName)
                    putString(BundleKey.OPERATION_TYPE, OIDCFLowHelper.OidcLoginType.COGNITO_REGISTER.name)
                    }
                    findNavController().navigate(
                        R.id.verifyCodeFragment, bundle
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

    /**
     * Toggles visibility of the registration password field and updates
     * the visibility icon while preserving the cursor position.
     */
    private fun handleRegisterPasswordVisibilityToggle() {

        isPasswordVisible = !isPasswordVisible

        if (isPasswordVisible) {
            binding.etRegisterPassword.transformationMethod =
                HideReturnsTransformationMethod.getInstance()
            binding.ivRegisterToggleEye.setImageResource(R.drawable.eye_open)
        } else {
            binding.etRegisterPassword.transformationMethod =
                PasswordTransformationMethod.getInstance()
            binding.ivRegisterToggleEye.setImageResource(R.drawable.eye_slash)
        }

        // Keep cursor at end
        binding.etRegisterPassword.setSelection(
            binding.etRegisterPassword.text?.length ?: 0
        )
    }

    /**
     * Toggles visibility of the registration repeat password field and updates
     * the visibility icon while preserving the cursor position.
     */
    private fun handleRegisterRepeatPasswordVisibilityToggle() {

        isPasswordVisible = !isPasswordVisible

        if (isPasswordVisible) {
            binding.etRepeatPassword.transformationMethod =
                HideReturnsTransformationMethod.getInstance()
            binding.ivRepeatPwdToggleEye.setImageResource(R.drawable.eye_open)
        } else {
            binding.etRepeatPassword.transformationMethod =
                PasswordTransformationMethod.getInstance()
            binding.ivRepeatPwdToggleEye.setImageResource(R.drawable.eye_slash)
        }

        // Keep cursor at end
        binding.etRepeatPassword.setSelection(
            binding.etRepeatPassword.text?.length ?: 0
        )
    }

}