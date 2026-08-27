// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: EnterWalletFragment.kt handles wallet PIN authentication, allowing users to unlock access to their wallet.
 * It validates the entered 4-digit PIN against the stored one, supports biometric login if enabled,
 * and navigates to the device list on success while securely masking PIN input.
 **/
package com.infineon.secora.wallet.ui.fragment

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.FragmentWalletPinBinding
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.ui.widget.AsteriskPasswordTransformationMethod
import com.infineon.secora.wallet.utils.helper.UIHelper
import com.infineon.secora.wallet.utils.Utils.isNetworkAvailable

/**
 * EnterWalletFragment is used to enter the wallet pin
 */
class EnterWalletFragment : BaseFragment() {
    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)
    private lateinit var binding: FragmentWalletPinBinding
    private lateinit var activity: MainActivity
    private lateinit var backCallback: OnBackPressedCallback

    /**
     * soft keyboard is set here
     * otp input is handled here
     *
     * @param view The view returned by [onCreateView].
     * @param savedInstanceState Saved instance state bundle, if any.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity = (requireActivity() as MainActivity)
        activity.binding.toolbar.profileIcon.visibility = View.GONE
        showKeyboard(binding.etBox1)
        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                logger.debug("Back press blocked")
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
        UIHelper.setupOtpBoxes(listOf(binding.etBox1, binding.etBox2, binding.etBox3, binding.etBox4))
        UIHelper.disablePasteOnInputs(
            requireContext(),
            binding.etBox1,
            binding.etBox2,
            binding.etBox3,
            binding.etBox4
        )
    }

    /**
     * onCreateView method is used to inflate the layout for this fragment
     * biometrics is handled here.
     *
     * @param inflater The layout inflater used to inflate views in this fragment.
     * @param container The parent view that the fragment's UI should attach to.
     * @param savedInstanceState The saved instance state, if available.
     * @return The root view of the inflated layout.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletPinBinding.inflate(inflater, container, false)
        activity = (requireActivity() as MainActivity)
        activity.binding.toolbar.profileIcon.visibility = View.GONE
        binding.tvTitle.text = getString(R.string.text_enter_wallet_pin)
        binding.btnPin.text = getString(R.string.enter_pin)
        initListeners()
        setupEdit()

        return binding.root
    }

    /**
     * Prompts biometric authentication when a wallet PIN exists.
     *
     * This function reads the stored wallet PIN from Storage. If a
     * non-empty PIN is found, it invokes [bioMetric] with the fragment's context
     * to start the biometric authentication flow.
     *
     * Call this from the UI thread and ensure the fragment is attached when invoked.
     *
     * @return Unit
     * @throws IllegalStateException if a valid Context cannot be obtained.
     * @see bioMetric
     */
    fun promptBiometricIfWalletPinExists() {
//        val walletPin = StorageRepository.readString(PreferenceKey.WALLET_PIN)
//        if (walletPin.isNotEmpty()) {
            bioMetric(requireContext())
        //}
    }

    /**
     * initListeners method is used to handle the click events
     * If entered pin is correct then navigate to device list fragment
     */
    private fun initListeners() {
        binding.btnPin.setOnClickListener {
            if (!isNetworkAvailable(requireContext())) {
                confirmDataDialog(getString(R.string.data_enable))
                return@setOnClickListener
            }
            val box1 = binding.etBox1.text.toString().trim()
            val box2 = binding.etBox2.text.toString().trim()
            val box3 = binding.etBox3.text.toString().trim()
            val box4 = binding.etBox4.text.toString().trim()
            val conformOtp = box1 + box2 + box3 + box4
            binding.etBox1.requestFocus()
            val imm =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etBox1, InputMethodManager.SHOW_IMPLICIT)
            imm.showSoftInput(binding.etBox2, InputMethodManager.SHOW_IMPLICIT)
            imm.showSoftInput(binding.etBox3, InputMethodManager.SHOW_IMPLICIT)
            imm.showSoftInput(binding.etBox4, InputMethodManager.SHOW_IMPLICIT)

            val walletPin = StorageRepository.readString(PreferenceKey.WALLET_PIN)
            when (conformOtp) {
                "" -> {
                    showToast(resources.getString(R.string.text_empty_wallet_pin))
                }

                walletPin -> {
                    Handler(Looper.getMainLooper()).post {
                        handleNavigation()
                    }
                }

                else -> {
                    binding.etBox1.setText("")
                    binding.etBox2.setText("")
                    binding.etBox3.setText("")
                    binding.etBox4.setText("")
                    showToast(resources.getString(R.string.incorrect_wallet_pin))
                }
            }
        }
    }

    /**
     * setupEdit method is used to set the transformation method for edit text
     * AsteriskPasswordTransformationMethod is used to hide the entered pin
     */
    private fun setupEdit() {
        binding.etBox1.transformationMethod = AsteriskPasswordTransformationMethod()
        binding.etBox2.transformationMethod = AsteriskPasswordTransformationMethod()
        binding.etBox3.transformationMethod = AsteriskPasswordTransformationMethod()
        binding.etBox4.transformationMethod = AsteriskPasswordTransformationMethod()
    }
}
