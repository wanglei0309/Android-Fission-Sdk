// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: DeviceDetailTransactionAdapter.kt displays a list of transactions grouped by date headers.
 * It binds transaction details like merchant name, time, and amount with currency symbols.
 **/
package com.infineon.secora.wallet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.infineon.secora.wallet.databinding.ItemWalletTransactionHeaderBinding
import com.infineon.secora.wallet.databinding.ItemWalletTransactionSingleRowBinding
import com.infineon.secora.wallet.models.TransactionItem
import com.infineon.secora.wallet.models.TransactionListItem

/**
 * RecyclerView adapter used to display device transaction history
 * grouped by date.
 *
 * This adapter supports multiple view types:
 * 1. Date headers – used to visually separate transactions by day.
 * 2. Transaction items – representing individual token transactions.
 *
 * It extends [ListAdapter] to efficiently handle list updates
 * using DiffUtil, ensuring smooth UI updates during data refresh.
 */
class DeviceDetailTransactionAdapter :
    ListAdapter<TransactionListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TRANSACTION = 1
    }

    /**
     * Determines the view type for the given adapter position.
     *
     * @param position Adapter position
     * @return Integer representing the view type (header or transaction)
     */
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TransactionListItem.Header -> VIEW_TYPE_HEADER
            is TransactionListItem.Transaction -> VIEW_TYPE_TRANSACTION
        }
    }

    /**
     * Creates the appropriate ViewHolder based on the provided view type.
     *
     * @param parent The parent ViewGroup
     * @param viewType The view type returned by [getItemViewType]
     * @return RecyclerView.ViewHolder for header or transaction item
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemWalletTransactionHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderViewHolder(binding)
            }

            else -> {
                val binding = ItemWalletTransactionSingleRowBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                TransactionViewHolder(binding)
            }
        }
    }

    /**
     * Binds data to the corresponding ViewHolder based on item type.
     *
     * @param holder ViewHolder instance
     * @param position Adapter position
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TransactionListItem.Header ->
                (holder as HeaderViewHolder).bind(item.date)

            is TransactionListItem.Transaction ->
                (holder as TransactionViewHolder).bind(item.data)
        }
    }

    /**
     * ViewHolder responsible for displaying transaction date headers.
     *
     * @param binding Inflated header layout view binding
     */
    class HeaderViewHolder(val binding: ItemWalletTransactionHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(date: String) {
            binding.tvDate.text = date
        }
    }

    /**
     * ViewHolder responsible for displaying individual transaction details.
     *
     * @param binding Inflated transaction row layout view binding
     */
    class TransactionViewHolder(val binding: ItemWalletTransactionSingleRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds transaction data to the UI components.
         *
         * @param transaction Token transaction response model
         */
        fun bind(transaction: TransactionItem) {
            binding.tvTransactionName.text = transaction.merchantName
            binding.tvTime.text = transaction.formattedTimeStamp
            binding.tvAmount.text = transaction.formattedAmount
        }
    }
}
