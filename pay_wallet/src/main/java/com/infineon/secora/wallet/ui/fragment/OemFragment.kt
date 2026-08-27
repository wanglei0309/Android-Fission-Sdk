// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: OemFragment.kt allows users to input and save OEM details such as OEM ID and SE Type.
 * It validates the inputs, saves them via SecoraWalletSDK, and navigates back to the login screen.
 **/
package com.infineon.secora.wallet.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.databinding.FragmentWalletOemInfoBinding
import com.infineon.secora.wallet.domain.walletsdk.WalletRepository
import com.infineon.secora.wallet.ui.home.MainActivity
import kotlinx.coroutines.launch

/**
 * Fragment responsible for capturing and saving OEM-specific details from the user.
 *
 * This fragment provides input fields for entering the OEM ID and SE Type Group, and includes:
 * - Validation for required fields
 * - Save functionality to persist data in shared preferences
 * - Navigation to the Login screen on successful save or cancellation
 *
 * @see R.id.loginFragment to navigate after actions
 */
class OemFragment : BaseFragment() {

    private lateinit var binding: FragmentWalletOemInfoBinding
    private lateinit var activity: MainActivity

    /**
     * Called to create and return the view hierarchy associated with the HomeFragment.
     *
     * @param inflater The LayoutInflater used to inflate views in the fragment.
     * @param container The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState Previously saved state (if any).
     * @return The root View of the fragment layout.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletOemInfoBinding.inflate(inflater, container, false)
        activity = (requireActivity() as MainActivity)
        binding.btnSave.setOnClickListener {
            if (binding.editTextOemId.text.toString() == "") {
                showToast(getString(R.string.enter_oem_id))
                return@setOnClickListener
            }
            if (binding.editTextSeType.text.toString() == "") {
                showToast(getString(R.string.enter_set_type_info))
                return@setOnClickListener
            }

            saveOemDetails(
                oemId = binding.editTextOemId.text.toString(),
                infineonSalesCodeAndGroup = binding.editTextSeType.text.toString()
            )
        }
        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
            activity.binding.toolbar.profileIcon.visibility = View.VISIBLE
        }

        return binding.root
    }

    private fun saveOemDetails(oemId: String, infineonSalesCodeAndGroup: String) {
        lifecycleScope.launch {
            if (!::activity.isInitialized || activity.isFinishing) return@launch
            WalletRepository.saveOEMDetails(
                context = activity.applicationContext,
                oemId = oemId,
                infineonSalesCodeAndGroup = infineonSalesCodeAndGroup
            )
            findNavController().navigate(R.id.loginFragment)
        }
    }
}