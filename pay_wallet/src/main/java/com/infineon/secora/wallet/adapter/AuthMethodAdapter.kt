// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: AuthMethodAdapter.kt displays a list of authentication methods in a RecyclerView.
 * it supports click.
 **/
package com.infineon.secora.wallet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.client.data.models.common.AuthenticationMethod
import com.infineon.secora.wallet.databinding.ItemAuthMethodBinding
import com.infineon.secora.wallet.utils.IDVType

/**
 * Adapter class for displaying a list of cards in a RecyclerView.
 *
 * It supports displaying Mastercard and Visa cards with nickname, card art (image), and expiration status.
 * It also handles click and long-press events on each card item.
 *
 * @property authenticationMethods The list of method data to display.
 * @property onItemClicked Callback triggered when a card is tapped.
 */
class AuthMethodAdapter(
    private val authenticationMethods: MutableList<AuthenticationMethod>,
    private val onItemClicked: (Int, String) -> Unit
) : RecyclerView.Adapter<AuthMethodAdapter.MyViewHolder>() {
    /**
     * Creates and returns a new ViewHolder for each card item.
     *
     * @param parent The parent view group.
     * @param viewType The view type of the new View.
     * @return A new instance of [MyViewHolder].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemAuthMethodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    /**
     * Binds the card data to each item in the RecyclerView.
     *
     * - Sets up method name.
     * - Handles click events on the card view.
     *
     * @param holder The ViewHolder containing views to update.
     * @param position The position of the item in the data list.
     */
    override fun onBindViewHolder(
        holder: MyViewHolder,
        position: Int
    ) {
        val cardData = authenticationMethods[position]
        if (cardData.type.equals(IDVType.EMV_3DS.idvType)) {
            holder.binding.tvMethodName.text = cardData.type
            holder.binding.tvMethodNumber.visibility = View.GONE
        } else {
            holder.binding.tvMethodNumber.visibility = View.VISIBLE
            holder.binding.tvMethodName.text = holder.itemView.context.getString(R.string.text_card_info_type, cardData.type)
            holder.binding.tvMethodNumber.text = holder.itemView.context.getString(R.string.text_card_info_value, cardData.value)
        }
        setupClickListeners(holder, position)
    }


    /**
     * Returns the number of items in the adapter.
     *
     * @return The total count of auth method data items.
     */
    override fun getItemCount(): Int {
        return authenticationMethods.size
    }

    /**
     * ViewHolder class for holding views of each card item.
     *
     * @param binding The inflated viewbinding representing a card row.
     */
    class MyViewHolder(val binding: ItemAuthMethodBinding) : RecyclerView.ViewHolder(binding.root)

    /**
     * Sets up click listeners for the auth method item view.
     *
     * Triggers the respective callback methods `onItemClicked`
     * when the card item is clicked.
     *
     * @param holder The view holder containing the method item view.
     * @param position The adapter position of the current method.
     */
    private fun setupClickListeners(holder: MyViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            onItemClicked(position, authenticationMethods[position].value.toString())
        }
    }

}