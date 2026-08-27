// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: ConfirmWalletFragment.kt manages the confirmation of the user’s wallet PIN.
 * It compares the entered PIN with the previously created one and, if they match, saves it securely in preferences
 * and navigates to the device list screen
 **/
package com.infineon.secora.wallet.ui.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.FragmentWalletPinBinding
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.ui.widget.AsteriskPasswordTransformationMethod
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.helper.UIHelper

/**
 * **ConfirmWalletFragment**
 *
 * This fragment is responsible for confirming the wallet PIN created by the user.
 * It verifies whether the PIN entered matches the one created earlier.
 * Upon successful confirmation, the user is navigated to the Device List screen.
 *
 * Key responsibilities:
 * - Displays a PIN confirmation UI.
 * - Verifies entered PIN with the previously created one.
 * - Prevents back navigation during confirmation.
 * - Handles soft keyboard visibility and input focus.
 */
class ConfirmWalletFragment : BaseFragment() {
    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)
    private lateinit var activity: MainActivity
    private lateinit var binding: FragmentWalletPinBinding
    private lateinit var backCallback: OnBackPressedCallback

    /**
     * Called after the view is created.
     *
     * Sets up UI behavior including
     *
     * @param view The root view of the fragment.
     * @param savedInstanceState The previously saved instance state, if any.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.binding.toolbar.profileIcon.visibility = View.GONE
        showKeyboard(binding.etBox1)
        UIHelper.setupOtpBoxes(listOf(binding.etBox1, binding.etBox2, binding.etBox3, binding.etBox4))
        UIHelper.disablePasteOnInputs(
            requireContext(),
            binding.etBox1,
            binding.etBox2,
            binding.etBox3,
            binding.etBox4
        )
        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                logger.debug("Back press blocked")
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
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
        binding.tvTitle.text = getString(R.string.confirm_wallet_pin)
        binding.btnPin.text = getString(R.string.confirm_pin)
        setupEdit()
        initListeners()
        dismissKeyboardOnTap(requireActivity(), binding.root)
        return binding.root
    }

    /**
     * initListeners is used to set the click listener on the button.
     * If create and confirm pin matches, then navigate to device list fragment
     *  to show error message
     */
    private fun initListeners() {
        binding.btnPin.setOnClickListener {
            binding.btnPin.isEnabled = false // Disable button to prevent spamming
            val box1 = binding.etBox1.text.toString().trim()
            val box2 = binding.etBox2.text.toString().trim()
            val box3 = binding.etBox3.text.toString().trim()
            val box4 = binding.etBox4.text.toString().trim()
            val conformOtp = box1 + box2 + box3 + box4
            val createOtp = arguments?.getString(BundleKey.CREATE_OTP).toString()
            when (conformOtp) {
                "" -> {
                    showToast(resources.getString(R.string.text_empty_wallet_pin))
                }

                createOtp -> {
                    StorageRepository.saveString(PreferenceKey.WALLET_PIN, conformOtp)
                    Handler(Looper.getMainLooper()).post {
                        findNavController().navigate(R.id.deviceListFragment)
                    }
                }

                else -> {
                    val otpFields =
                        listOf(binding.etBox1, binding.etBox2, binding.etBox3, binding.etBox4)
                    otpFields.forEach { it.text?.clear() }
                    otpFields.first().requestFocus()
                    showToast(resources.getString(R.string.wallet_pin_mismatch))
                }
            }

            // Re-enable the button after a short delay (e.g., 1 second)
            Handler(Looper.getMainLooper()).postDelayed({
                binding.btnPin.isEnabled = true
            }, 1000)
        }
    }

    /**
     * Configures the PIN input fields to display asterisks ('*') instead of numeric characters.
     *
     * Enhances privacy and security during PIN entry.
     */
    private fun setupEdit() {
        binding.etBox1.transformationMethod = AsteriskPasswordTransformationMethod()
        binding.etBox2.transformationMethod = AsteriskPasswordTransformationMethod()
        binding.etBox3.transformationMethod = AsteriskPasswordTransformationMethod()
        binding.etBox4.transformationMethod = AsteriskPasswordTransformationMethod()
    }
}
