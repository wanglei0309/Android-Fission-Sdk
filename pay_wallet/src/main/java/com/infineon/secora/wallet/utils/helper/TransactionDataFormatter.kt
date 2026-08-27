// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: TransactionFormatter.kt is a helper object used to format transaction related data
 **/
package com.infineon.secora.wallet.utils.helper

import com.infineon.secora.wallet.MyApplication
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.utils.constants.Constants.DEVICE_TIME_PATTERN_24H
import com.infineon.secora.wallet.utils.constants.Constants.ISO_UTC_PATTERN
import com.infineon.secora.wallet.utils.constants.Constants.UTC_TIMEZONE
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TransactionDataFormatter {

    private val CURRENCY_SYMBOLS = mapOf(
        "840" to "$",
        "276" to "€",
        "978" to "€",
        "356" to "₹"
    )

    /**
     * Returns the symbol for the given ISO currency code.
     *
     * @param currencyCode ISO currency code.
     * @return The corresponding currency symbol, or the original code if unsupported.
     */
    fun getCurrencySymbol(currencyCode: String): String {
        return CURRENCY_SYMBOLS[currencyCode.trimStart('0')] ?: currencyCode
    }

    /**
     * Converts an ISO-8601 timestamp to a formatted date string.
     *
     * The input date is parsed using [OffsetDateTime] and formatted to
     * the pattern `EEE dd MMM` (e.g., `WED 11 MAR`).
     *
     * Note:
     * - Requires Android API level 26 (Oreo) or higher
     * - Returns an empty string if the input value is `"null"`
     *
     * @param input ISO-8601 formatted date-time string
     * @return Formatted date string in uppercase, or empty string if invalid
     */
    fun convertDate(input: String?): String {
        if (input.isNullOrEmpty() || input == "null") return ""

        return try {
            // Parse server time (UTC or with offset)
            val parsed = OffsetDateTime.parse(input)

            // Convert to device timezone
            val deviceZoned = parsed.atZoneSameInstant(ZoneId.systemDefault())

            val formatter = DateTimeFormatter.ofPattern("EEE dd MMM", Locale.ENGLISH)
            deviceZoned.format(formatter).uppercase()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Converts a transaction timestamp (UTC ISO string or epoch millis)
     * to device local time in 24-hour format (HH:mm).
     *
     * - Reflects device timezone changes automatically
     * - No AM/PM shown
     */
    fun convertTxnTimeToDevice(timestamp: String?): String {
        if (timestamp.isNullOrEmpty()) return ""

        return try {
            // Try ISO UTC format first
            val utcFormat = SimpleDateFormat(ISO_UTC_PATTERN, Locale.getDefault())
            utcFormat.timeZone = TimeZone.getTimeZone(UTC_TIMEZONE)

            val date = utcFormat.parse(timestamp)

            // Device local time (24-hour format)
            val deviceFormat = SimpleDateFormat(DEVICE_TIME_PATTERN_24H, Locale.getDefault())
            deviceFormat.timeZone = TimeZone.getDefault()

            deviceFormat.format(date!!)
        } catch (e: Exception) {
            try {
                // Fallback: epoch millis
                val date = Date(timestamp.toLong())

                val sdf = SimpleDateFormat(DEVICE_TIME_PATTERN_24H, Locale.getDefault())
                sdf.timeZone = TimeZone.getDefault()

                sdf.format(date)
            } catch (e2: Exception) {
                timestamp
            }
        }
    }

    /**
     * Formats an amount with its corresponding currency symbol.
     *
     * @param amount The transaction amount.
     * @param currencyCode ISO currency code used to resolve the currency symbol.
     * @return Formatted amount string with currency symbol.
     */
    fun formatAmountWithCurrency(amount: String, currencyCode: String): String {
        return MyApplication.appContext.getString(
            R.string.formatted_two_values,
            amount,
            getCurrencySymbol(currencyCode)
        )
    }
}