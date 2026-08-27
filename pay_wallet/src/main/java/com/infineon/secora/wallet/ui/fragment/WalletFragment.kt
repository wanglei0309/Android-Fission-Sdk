// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: WalletFragment.kt manages the wallet PIN entry screen, providing OTP-style input behavior where focus
 * moves automatically between PIN boxes and supports backspace navigation for a smooth user experience.
 **/
package com.infineon.secora.wallet.ui.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.databinding.FragmentWalletPinBinding
import com.infineon.secora.wallet.ui.home.MainActivity

/**
 * [WalletFragment] handles the wallet PIN entry screen.
 *
 * Layout used: [R.layout.fragment_wallet_pin]
 */
class WalletFragment : Fragment(R.layout.fragment_wallet_pin) {
    private lateinit var activity: MainActivity

    /**
     * Called after the fragment's view has been created.
     * Initializes EditText boxes, sets up OTP-style input behavior,
     * and manages backspace navigation between boxes.
     *
     * @param view The view returned by [onCreateView].
     * @param savedInstanceState Saved instance state bundle, if any.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity = (requireActivity() as MainActivity)
        activity.binding.toolbar.profileIcon.visibility = View.GONE
        val walletPinBinding = FragmentWalletPinBinding.bind(view)

        setupOtpInput(walletPinBinding.etBox1, walletPinBinding.etBox2)
        setupOtpInput(walletPinBinding.etBox2, walletPinBinding.etBox3)
        setupOtpInput(walletPinBinding.etBox3, walletPinBinding.etBox4)

        handleBackspace(walletPinBinding.etBox2, walletPinBinding.etBox1)
        handleBackspace(walletPinBinding.etBox3, walletPinBinding.etBox2)
        handleBackspace(walletPinBinding.etBox4, walletPinBinding.etBox3)
    }

    /**
     * Configures OTP-like behavior by automatically shifting focus
     * from the current input box to the next one when a character is entered.
     *
     * @param currentBox The current [EditText] where the user is typing.
     * @param nextBox The next [EditText] to move focus to after one character is entered.
     */
    private fun setupOtpInput(currentBox: EditText, nextBox: EditText) {
        currentBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 1) {
                    nextBox.requestFocus()
                }
            }
        })
    }

    /**
     * Handles backspace key behavior by moving focus back to the previous input box
     * when the current box is empty and the user presses the delete key.
     *
     * @param currentBox The current [EditText] being monitored.
     * @param previousBox The [EditText] to move focus to when backspace is pressed on an empty box.
     */
    private fun handleBackspace(currentBox: EditText, previousBox: EditText) {
        currentBox.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN && currentBox.text.isEmpty()) {
                previousBox.requestFocus()
                true
            } else {
                false
            }
        }
    }
}
