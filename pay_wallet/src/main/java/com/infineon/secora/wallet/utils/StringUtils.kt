// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: StringUtils.kt is a Utility object providing String related utility functions.
 **/
package com.infineon.secora.wallet.utils

object StringUtils {

    /**
     * Returns true if the given string is null or empty after trimming spaces.
     */
    fun isEmptyString(str: CharSequence?): Boolean {
        return str == null || str.toString().trim { it <= ' ' }.isEmpty() || str == "null"
    }

    /**
     * Removes extra whitespace from the string.
     *
     * This function:
     * - Replaces multiple consecutive whitespace characters with a single space
     * - Trims leading and trailing spaces
     *
     * @return A string with normalized spacing
     */
    fun String.removeExtraSpaces(): String {
        return this.replace(Regex("\\s+"), " ").trim()
    }
}