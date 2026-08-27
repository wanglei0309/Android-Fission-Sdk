// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: ConfirmWalletPinFragment.kt verifies the wallet PIN entered by the user by matching it with the previously entered PIN.
 * If the PINs match, it saves the PIN securely and navigates to the device list screen.
 **/
package com.infineon.secora.wallet.ui.wallet

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.navigation.fragment.findNavController
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.FragmentWalletConfirmPinBinding
import com.infineon.secora.wallet.ui.fragment.BaseFragment
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.constants.Constants.PIN_DELAY

/**
 * ConfirmWalletPinFragment : Fragment responsible for confirming the wallet PIN entered by the user.
 *
 * This screen compares the re-entered PIN against the initially entered one.
 * If matched, the PIN is saved and navigation proceeds. If not, an error is shown.
 */
class ConfirmWalletPinFragment : BaseFragment() {

    private lateinit var binding: FragmentWalletConfirmPinBinding
    private var enteredPin: String? = null
    private var oldPin: String? = null
    private lateinit var activity: MainActivity

    /**
     * Called when the fragment is created.
     *
     * - Inflates the view binding
     * - Read PIN values passed via intent extras
     * - Sets up the listener for confirming the PIN
     *
     * @param savedInstanceState the previously saved state of the fragment, if any
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentWalletConfirmPinBinding.inflate(layoutInflater)
        requireActivity().setContentView(binding.root)
        activity = (requireActivity() as MainActivity)
        activity.binding.toolbar.profileIcon.visibility = View.GONE

        // Get pin data from bundle
        val bundle = requireActivity().intent.extras
        if (bundle != null && !bundle.isEmpty) {
            enteredPin = bundle.getString(BundleKey.PIN_VALUE)
            oldPin = bundle.getString(BundleKey.OLD_PIN)
        }

        if (binding.txtConfirmPinEntry.text != null) {
            binding.txtConfirmPinEntry.setAnimateText(true)
            binding.txtConfirmPinEntry.setOnPinEnteredListener { str ->
                if (str.toString().length == 4) {
                    if (str.toString() == enteredPin) {
                        StorageRepository.saveString(PreferenceKey.WALLET_PIN, enteredPin.toString())
                        Handler(Looper.getMainLooper()).post {
                            findNavController().navigate(R.id.deviceListFragment)
                        }
                    } else {
                        binding.txtConfirmPinEntry.isError = true
                        showToast(
                            resources.getString(R.string.error_failure),
                        )
                        binding.txtConfirmPinEntry.postDelayed({
                            binding.txtConfirmPinEntry.text = null
                        }, PIN_DELAY)
                    }
                }
            }
        }
    }
}