// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: AsteriskPasswordTransformationMethod.kt is a custom text transformation class that replaces all characters in
 * an input field with asterisks while preserving the original text length.
 * It’s typically used to mask sensitive input like PINs or passwords.
 **/
package com.infineon.secora.wallet.ui.widget

import android.text.method.PasswordTransformationMethod
import android.view.View
import com.infineon.secora.wallet.utils.constants.Constants.MASK_CHAR

/**
 * A custom [PasswordTransformationMethod] that displays all characters as asterisks ('*')
 * regardless of the actual input. This class is typically used to mask password inputs
 * for fields where asterisks are preferred over dots or other default characters.
 *
 */
class AsteriskPasswordTransformationMethod : PasswordTransformationMethod() {

    /**
     * Returns a [CharSequence] where each character is replaced with an asterisk '*'.
     *
     * @param source The original input CharSequence.
     * @param view The associated [View] (usually an EditText).
     * @return A [CharSequence] displaying asterisks instead of actual characters.
     */
    override fun getTransformation(source: CharSequence, view: View): CharSequence {
        return object : PasswordCharSequence(source) {}
    }

    /**
     * A [CharSequence] wrapper that transforms each visible character into an asterisk ('*'),
     * preserving only the length of the original input.
     *
     * @property source The original character sequence to be masked.
     */
    open class PasswordCharSequence(
        private val source: CharSequence
    ) : CharSequence by source {

        override fun get(index: Int): Char {
            index.hashCode()
            return MASK_CHAR
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            return PasswordCharSequence(source.subSequence(startIndex, endIndex))
        }
    }
}
