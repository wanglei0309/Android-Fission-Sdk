// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: CreateWalletFragment.kt allows users to set up a new wallet PIN by entering a 4-digit code.
 * Once the PIN is entered, it navigates to the confirmation screen to verify the PIN;
 * if incomplete, it prompts the user with an error message.
 **/
package com.infineon.secora.wallet.ui.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.databinding.DialogCommonMessageBinding
import com.infineon.secora.wallet.databinding.FragmentWalletPinBinding
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.ui.widget.AsteriskPasswordTransformationMethod
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.helper.UIHelper
import com.infineon.secora.wallet.utils.helper.showSecure
import com.infineon.secora.wallet.utils.helper.WalletPinValidator

/**
 *CreateWalletFragment is used to create the wallet pin
 */
class CreateWalletFragment : BaseFragment() {

    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)
    private lateinit var activity: MainActivity
    private lateinit var binding: FragmentWalletPinBinding
    private lateinit var backCallback: OnBackPressedCallback

    /**
     * Called immediately after the fragment's view is created.
     * This method initializes the UI, manages visibility states,
     * handles back press behavior, and sets up input field restrictions.
     *
     * @param view The root view returned by `onCreateView`.
     * @param savedInstanceState Previously saved instance state, if any.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.binding.toolbar.profileIcon.visibility = View.GONE
        activity.showLoading(false, "")
        showKeyboard(binding.etBox1)
        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                logger.debug("Back press blocked")
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        UIHelper.disablePasteOnInputs(
            requireContext(),
            binding.etBox1,
            binding.etBox2,
            binding.etBox3,
            binding.etBox4
        )
    }

    /**
     * Inflates the layout and initializes the view components.
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
        UIHelper.showKeyboard(requireContext(), binding.etBox1)
        binding.tvTitle.text = getString(R.string.create_wallet_pin)
        binding.btnPin.text = getString(R.string.create_pin)
        setupEdit()
        setupUI()
        initListeners()
        dismissKeyboardOnTap(requireActivity(), binding.root)
        return binding.root
    }

    /**
     * Prompts biometric authentication to access the wallet.
     *
     * Invokes [bioMetric] using the fragment's context to start the biometric
     * authentication flow for wallet access.
     *
     * Call this from the UI thread and ensure the fragment is attached when invoked.
     *
     * @return Unit
     * @throws IllegalStateException if a valid Context cannot be obtained.
     * @see bioMetric
     */
    fun promptBiometricForWalletAccess() {
        bioMetric(requireContext())
    }

    /**
     * Initializes the UI components related to OTP input fields.
     */
    private fun setupUI() {
        val boxes = listOf(binding.etBox1, binding.etBox2, binding.etBox3, binding.etBox4)
        UIHelper.setupOtpBoxes(boxes)
    }

    /**
     * setupEdit is used to hide the pin in astrix form
     */
    private fun setupEdit() {
        binding.etBox1.transformationMethod = AsteriskPasswordTransformationMethod()
        binding.etBox2.transformationMethod = AsteriskPasswordTransformationMethod()
        binding.etBox3.transformationMethod = AsteriskPasswordTransformationMethod()
        binding.etBox4.transformationMethod = AsteriskPasswordTransformationMethod()
    }

    /**
     * initListeners() handles the button click event.
     * if otp length is 4, then navigate to confirm wallet pin fragment
     */
    private fun initListeners() {
        binding.btnPin.setOnClickListener {
            val otp =
                binding.etBox1.text.toString().trim() + binding.etBox2.text.toString()
                    .trim() + binding.etBox3.text.toString().trim() + binding.etBox4.text.toString()
                    .trim()
            if (otp.length != 4) {
                showToast(getString(R.string.text_empty_wallet_pin))
                return@setOnClickListener
            }
            if (WalletPinValidator.isWeakPin(otp)) {
                showWeakPinAlert()
                return@setOnClickListener
            }
            val bundle = Bundle().apply {
                putString(BundleKey.CREATE_OTP, otp)
            }
            findNavController().navigate(R.id.confirmwalletpin, bundle)
        }
    }

    private fun showWeakPinAlert() {
        activity.runOnUiThread {
            val dialogViewBinding = DialogCommonMessageBinding.inflate(layoutInflater)
            val alertDialog = Dialog(requireContext())
            alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            alertDialog.setContentView(dialogViewBinding.root)
            alertDialog.setCancelable(false)

            dialogViewBinding.txtTitle.text = getString(R.string.app_name)
            dialogViewBinding.txtMessage.text = getString(R.string.weak_wallet_pin_message)
            dialogViewBinding.txtCancel.visibility = View.GONE

            dialogViewBinding.txtOK.setOnClickListener {
                alertDialog.dismiss()
                clearPinFields()
            }
            alertDialog.showSecure()
        }
    }

    private fun clearPinFields() {
        binding.etBox1.text?.clear()
        binding.etBox2.text?.clear()
        binding.etBox3.text?.clear()
        binding.etBox4.text?.clear()
        UIHelper.showKeyboard(requireContext(), binding.etBox1)
        binding.etBox1.requestFocus()
    }
}

