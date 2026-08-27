// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: UIHelper.kt is a helper object providing common utility functions for UI related functionality.
 **/
package com.infineon.secora.wallet.utils.helper

import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.ActionMode
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.infineon.secora.wallet.R

object UIHelper {

    /**
     * Shows or hides a simple loading spinner overlaid on the provided view.
     *
     * @param view Root view to attach or remove the loader.
     * @param context Activity or fragment context.
     * @param show true to display loader, false to hide.
     */
    fun showLoading(view: View, context: Context, show: Boolean) {
        val rootLayout =
            view.findViewById<ViewGroup>(android.R.id.content) // Root layout of the activity
        val loadingId = R.id.loadingIcon
        var loadingIcon = rootLayout.findViewById<ProgressBar>(loadingId)
        if (show) {
            loadingIcon = ProgressBar(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                isIndeterminate = true
            }
            rootLayout.addView(loadingIcon) // Add to the root layout

            loadingIcon.visibility = View.VISIBLE
        } else {
            loadingIcon?.visibility = View.GONE
        }
    }

    /**
     * Hides the keyboard from the given view.
     */
    fun hideKeyboard(view: View, context: Context) {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Shows the keyboard and focuses on the provided view.
     */
    fun showKeyboard(context: Context, view: View) {
        // Request focus
        view.requestFocus()
        // Open keyboard
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Sets up OTP input behavior to move to the next box automatically.
     */
    fun setupOtpInput(
        currentBox: EditText,
        nextBox: EditText
    ) {
        currentBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 1) {
                    nextBox.requestFocus()
                }
                if (currentBox.selectionStart != currentBox.text.length) {
                    currentBox.setSelection(currentBox.text.length) // Ensure the cursor is at the end
                }
            }
        })
        currentBox.setOnClickListener {
            currentBox.setSelection(currentBox.text.length)
        }
        currentBox.setOnClickListener {
            currentBox.post {
                if (currentBox.selectionStart != currentBox.text.length) {
                    currentBox.setSelection(currentBox.text.length)
                }
            }
        }
    }

    /**
     * Sets up a group of OTP input boxes (EditTexts) to behave as a single input field.
     *
     * @param allBoxes List of EditTexts representing the OTP input fields.
     */
    fun setupOtpBoxes(allBoxes: List<EditText>) {
        if (allBoxes.isEmpty()) return

        allBoxes.forEach { box ->
            setupBoxProperties(box)
            setupBoxClickAndTouch(box, allBoxes)
            setupTextChangeListener(box, allBoxes)
            setupKeyListener(box, allBoxes)
        }

        showKeyboardOnFirstBox(allBoxes)
    }

    /**
     * Sets basic properties for an OTP box.
     */
    private fun setupBoxProperties(box: EditText) {
        box.filters = arrayOf(InputFilter.LengthFilter(1))
        box.isCursorVisible = false
        box.isFocusableInTouchMode = true
    }

    /**
     * Sets click and touch behavior to focus the first empty box.
     */
    private fun setupBoxClickAndTouch(box: EditText, allBoxes: List<EditText>) {
        val focusToFirstEmpty = {
            val target = allBoxes.firstOrNull { it.text.isNullOrEmpty() } ?: allBoxes.last()
            target.requestFocus()
            target.setSelection(target.text.length)
        }

        box.setOnClickListener { focusToFirstEmpty() }
    }

    /**
     * Sets a text change listener to move focus forward or hide keyboard when done.
     */
    private fun setupTextChangeListener(box: EditText, allBoxes: List<EditText>) {
        box.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) return

                val nextBox = allBoxes.firstOrNull { it.text.isNullOrEmpty() }
                if (nextBox != null) {
                    nextBox.requestFocus()
                } else {
                    hideKeyboard(box)
                }
            }
        })
    }

    /**
     * Handles backspace navigation in OTP input.
     */
    private fun setupKeyListener(box: EditText, allBoxes: List<EditText>) {
        box.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                val lastFilled = allBoxes.lastOrNull { !it.text.isNullOrEmpty() }
                lastFilled?.apply {
                    setText("")
                    requestFocus()
                    if (allBoxes.all { it.text.isNullOrEmpty() }) {
                        hideKeyboard(v)
                    }
                }
                return@setOnKeyListener true
            }
            false
        }
    }

    /**
     * Shows the keyboard on the first box when initializing.
     */
    private fun showKeyboardOnFirstBox(allBoxes: List<EditText>) {
        allBoxes.firstOrNull()?.let { firstBox ->
            firstBox.requestFocus()
            val imm =
                firstBox.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(firstBox, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    /**
     * Hides the soft keyboard from the given view.
     */
    private fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Prevents paste, text selection, autofill, and clipboard suggestions on given EditTexts.
     * Clears clipboard content on focus/click and blocks multi-character input (paste).
     * Useful for secure fields like PIN or OTP inputs.
     * Requires API level 28 (Android 9+).
     *
     * @param editTexts One or more EditText views to secure.
     */
    fun disablePasteOnInputs(context: Context, vararg editTexts: EditText) {
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        for (editText in editTexts) {
            editText.isLongClickable = false
            editText.setTextIsSelectable(false)

            editText.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            editText.setAutofillHints(null)

            editText.customSelectionActionModeCallback = object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?) = false
                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) =
                    false

                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) =
                    false

                override fun onDestroyActionMode(mode: ActionMode?) = Unit
            }

            editText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) clipboard.clearPrimaryClip()
            }

            editText.setOnClickListener {
                clipboard.clearPrimaryClip()
            }

            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
                    Unit

                override fun afterTextChanged(s: Editable?) {
                    val text = s.toString()
                    if (text.length > 1) {
                        editText.setText("")
                    }
                }
            })
        }
    }
}