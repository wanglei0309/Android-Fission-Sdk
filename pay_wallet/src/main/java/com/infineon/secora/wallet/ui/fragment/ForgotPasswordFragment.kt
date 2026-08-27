// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: ForgotPasswordFragment.kt manages user validation and verification code generation flow required for the forgot password flow.
 *
 **/
package com.infineon.secora.wallet.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.databinding.FragmentForgotPasswordBinding
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.oidc.CognitoSignInFlowCoordinator
import com.infineon.secora.wallet.oidc.OIDCFLowHelper.OidcLoginType
import com.infineon.secora.wallet.oidc.OIDCFLowHelper.validateEmailCredentials
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.utils.constants.BundleKey

class ForgotPasswordFragment : BaseFragment() {
    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)
    private lateinit var binding: FragmentForgotPasswordBinding
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
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
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

        binding.btnSendOtp.setOnClickListener {
            if (!validateEmailCredentials(requireContext(), binding.etResetPasswordUsername)) {
                return@setOnClickListener
            }
            resetPasswordFlow()
        }
    }

    /**
     * Initiates the Cognito password reset process and requests
     * a verification code.
     */
    private fun resetPasswordFlow() {
        val userName = binding.etResetPasswordUsername.text.toString()
        activity.showLoading(true, getString(R.string.text_please_wait))
        CognitoSignInFlowCoordinator.resetPassword(
            userName, {
                activity.runOnUiThread {
                    activity.showLoading(false, "")
                    statusDialog(requireActivity(), getString(R.string.verification_code_sent_popup_text))
                    val bundle = Bundle().apply {
                        putString(BundleKey.USERNAME, userName)
                        putString(BundleKey.OPERATION_TYPE, OidcLoginType.COGNITO_FORGOT_PWD.name)
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

}