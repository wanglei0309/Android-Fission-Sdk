// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: This class provides DiffUtil callbacks for efficiently updating the transaction list in RecyclerView.
 * The overridden methods are invoked internally by ListAdapter during list updates and are not called directly.
 **/
package com.infineon.secora.wallet.adapter

import androidx.recyclerview.widget.DiffUtil
import com.infineon.secora.wallet.models.TransactionListItem

/**
 * DiffUtil callback for calculating differences between items in the
 * transaction list RecyclerView.
 *
 * This callback supports multiple view types represented by the sealed class
 * [TransactionListItem], such as headers and transaction items.
 *
 * Comparison strategy:
 * - Header items are identified uniquely by their date.
 * - Transaction items are identified using a combination of
 *   transaction timestamp and transaction amount.
 * - Items of different types are always treated as different.
 *
 * Using DiffUtil allows the RecyclerView to efficiently update only the
 * changed items instead of redrawing the entire list.
 */
class DiffCallback : DiffUtil.ItemCallback<TransactionListItem>() {

    /**
     * Determines whether two items represent the same logical entity.
     *
     * This method is used to decide whether an item has been moved,
     * updated, or replaced.
     *
     * @param oldItem Item from the previous list.
     * @param newItem Item from the new list.
     * @return true if both items represent the same entity, false otherwise.
     */
    override fun areItemsTheSame(
        oldItem: TransactionListItem,
        newItem: TransactionListItem
    ): Boolean {
        return when (oldItem) {
            is TransactionListItem.Header if newItem is TransactionListItem.Header ->
                oldItem.date == newItem.date

            is TransactionListItem.Transaction if newItem is TransactionListItem.Transaction ->
                oldItem.data.timeStamp == newItem.data.timeStamp &&
                    oldItem.data.amount == newItem.data.amount

            else -> false
        }
    }

    /**
     * Determines whether the contents of two items are exactly the same.
     *
     * This method is called only if [areItemsTheSame] returns true.
     * If this method returns false, the item will be re-bound to update
     * the UI with new content.
     *
     * @param oldItem Item from the previous list.
     * @param newItem Item from the new list.
     * @return true if the contents are identical, false otherwise.
     */
    override fun areContentsTheSame(
        oldItem: TransactionListItem,
        newItem: TransactionListItem
    ): Boolean = oldItem == newItem
}
