// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: TransactionItem.kt is a data model class used to display UI in DeviceDetailTransactionAdapter.
 **/
package com.infineon.secora.wallet.models

import com.infineon.secora.wallet.utils.helper.TransactionDataFormatter

data class TransactionItem(
    val merchantName: String,
    val timeStamp: String,
    val amount: String,
    private val currencyCode: String
) {
    val formattedTimeStamp: String
        get() = TransactionDataFormatter.convertTxnTimeToDevice(timeStamp)

    val formattedAmount: String
        get() = TransactionDataFormatter.formatAmountWithCurrency(
            amount = amount,
            currencyCode = currencyCode
        )
}
