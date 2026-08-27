// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: TransactionListItem.kt is a sealed class representing items in a transaction list,
 * containing two types: Header for displaying date sections and Transaction for showing individual transaction details.
 **/
package com.infineon.secora.wallet.models

/**
 * A sealed class representing different types of items that can appear
 * in a transaction list — either a header or a transaction item.
 *
 * This design allows the list to contain multiple view types
 * while ensuring exhaustive handling in `when` expressions.
 */
sealed class TransactionListItem {

    /**
     * Represents a header item in the transaction list, showing a date for the transactions.
     *
     * @property date The date string displayed as the header.
     */
    data class Header(val date: String) : TransactionListItem()

    /**
     * Represents an actual transaction entry in the list.
     *
     * @property data The transaction details encapsulated in a [TransactionItem] object.
     */
    data class Transaction(val data: TransactionItem) : TransactionListItem()
}