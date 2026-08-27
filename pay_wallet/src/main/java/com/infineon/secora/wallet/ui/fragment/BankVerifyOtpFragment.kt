// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: BankVerifyOTP.kt is a fragment that manages the OTP verification UI for bank-related operations.
 * It initializes OTP input fields with masked characters, handles focus transitions between boxes,
 * and starts an OTP timer to manage code resend functionality.
 **/
package com.infineon.secora.wallet.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.infineon.secora.wallet.databinding.FragmentWalletVerifyOtpBinding
import com.infineon.secora.wallet.ui.widget.AsteriskPasswordTransformationMethod
import com.infineon.secora.wallet.utils.helper.UIHelper.setupOtpBoxes

/**
 *BankVerifyOTP class is used to verify otp
 */
class BankVerifyOtpFragment : BaseFragment() {

    private lateinit var binding: FragmentWalletVerifyOtpBinding

    /**
     * onCreateView method is used to inflate the layout
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletVerifyOtpBinding.inflate(inflater, container, false)
        initListeners()
        return binding.root
    }

    /**
     * initListeners method is used to initialize the listeners
     * It is used to set the click listeners on the views
     * OTP Timer is started here
     */
    private fun initListeners() {
        binding.tvRequestCode.setOnClickListener {
            prepareOtp(binding.tvTimer, false)
        }
    }

    /**
     * onViewCreated method is used to initialize the views and handle the Otp box focus movement
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEdit()
        setupOtpBoxes(
            listOf(
                binding.etBox1,
                binding.etBox2,
                binding.etBox3,
                binding.etBox4,
                binding.etBox5,
                binding.etBox6
            )
        )
        prepareOtp(binding.tvTimer, false)
    }

    /**
     * setupEdit method is used to show the entered otp digits in asterisks
     */
    private fun setupEdit() {
        binding.etBox1.transformationMethod = AsteriskPasswordTransformationMethod()
        binding.etBox2.transformationMethod = AsteriskPasswordTransformationMethod()
        binding.etBox3.transformationMethod = AsteriskPasswordTransformationMethod()
        binding.etBox4.transformationMethod = AsteriskPasswordTransformationMethod()
        binding.etBox5.transformationMethod = AsteriskPasswordTransformationMethod()
        binding.etBox6.transformationMethod = AsteriskPasswordTransformationMethod()
    }
}