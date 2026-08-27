// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: AddCardAdapterDb.kt displays a list of saved payment cards in a RecyclerView.
 * It shows card details, images, and status, and handles click and long-press actions for each card.
 **/
package com.infineon.secora.wallet.adapter

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.client.data.models.card.common.CardDetails
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.ItemWalletCardBinding
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.utils.constants.Constants
import com.infineon.secora.wallet.utils.constants.Constants.DEFAULT_COLOR

/**
 * Adapter class for displaying a list of cards from DB in a RecyclerView.
 *
 * It supports displaying Mastercard and Visa cards with nickname, card art (image), and expiration status.
 * It also handles click and long-press events on each card item.
 *
 * @property cardDataList The list of card data to display.
 * @property onItemClicked Callback triggered when a card is tapped.
 * @property onItemLongPress Callback triggered when a card is long pressed.
 * @property imageMap A mapping of card art asset IDs to Base64-encoded image data.
 */
class AddCardAdapterDb(
    private val cardDataList: List<CardDetails>,
    private val onItemClicked: (Int, String) -> Unit,
    private var onItemLongPress: (Int, String) -> Unit,
    private val imageMap: MutableMap<String, Bitmap>
) : RecyclerView.Adapter<AddCardAdapterDb.MyViewHolder>() {

    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)

    /**
     * Creates and returns a new ViewHolder for each card item.
     *
     * @param parent The parent view group.
     * @param viewType The view type of the new View.
     * @return A new instance of [MyViewHolder].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemWalletCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MyViewHolder(binding)
    }

    /**
     * Binds the card data to each item in the RecyclerView.
     *
     * - Sets up card info, expiration date, nickname, and status.
     * - Loads Base64-encoded card art using Glide.
     * - Handles click and long-press events on the card view.
     *
     * @param holder The ViewHolder containing views to update.
     * @param position The position of the item in the data list.
     */
    override fun onBindViewHolder(
        holder: MyViewHolder,
        position: Int
    ) {
        val cardData = cardDataList[position]

        setupCardInfo(holder, cardData)
        setupNicknameAndDefault(holder, cardData)
        setupCardStatus(holder, cardData)
        setupCardImage(holder, cardData, position)
    }

    /**
     * Sets up the card information to be displayed in the ViewHolder.
     *
     * This method formats and binds card details such as the masked card number,
     * card type (Visa/Mastercard), and expiration date to the corresponding UI element.
     *
     * @param holder The ViewHolder containing the card info TextView.
     * @param cardData The card data object containing card type, suffix, and expiration date.
     */
    private fun setupCardInfo(holder: MyViewHolder, cardData: CardDetails) {
        val expiryDate = cardData.cardExpiry.toString()
        var formattedExpiryDate = expiryDate

        if (!expiryDate.contains(Constants.SLASH)) {
            formattedExpiryDate = expiryDate.take(2) + Constants.SLASH + expiryDate.substring(2, 4)
        }

        val context = holder.itemView.context

        val brand = when (cardData.pnoType) {
            Constants.PNO_MDES -> context.getString(R.string.master_card)
            Constants.PNO_VTS -> context.getString(R.string.visa)
            Constants.PNO_AMEX -> context.getString(R.string.card_brand_amex)
            else -> ""
        }

        holder.binding.tvCardInfo.text = context.getString(
            R.string.card_masked_format,
            brand,
            cardData.dpanSuffix,
            formattedExpiryDate
        )
    }

    /**
     * Sets up the nickname and default card indicator for the given card item.
     *
     * - Looks up the nickname for the card from the `nicknameMap` using a key that matches
     *   both payment app instance ID and DPAN suffix.
     * - Displays either the nickname or a masked DPAN as fallback.
     * - Checks if the card is set as the default card and updates the UI accordingly.
     *
     * @param holder The view holder containing card UI components.
     * @param cardData The card data used to populate the view.
     */
    private fun setupNicknameAndDefault(holder: MyViewHolder, cardData: CardDetails) {
        holder.binding.tvNickName.text = cardData.cardNickname ?: ""
        holder.binding.tvDefault.visibility = View.GONE
        val context = holder.itemView.context

        if (cardData.cardStatus.equals(context.getString(R.string.text_suspended), ignoreCase = true)) {
            return
        }

        val paymentId = StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID)
        val defaultTokenRef = StorageRepository.readString(PreferenceKey.deviceKey(paymentId))

        if (cardData.digitizationReferenceNumber == defaultTokenRef) {
            holder.binding.tvDefault.visibility = View.VISIBLE
            holder.binding.tvDefault.text = context.getString(R.string.text_default)
        } else {
            holder.binding.tvDefault.visibility = View.GONE
        }
    }

    /**
     * Sets up the card status text in the view.
     *
     * Displays the card status in the corresponding TextView.
     *
     * @param holder The view holder containing card UI components.
     * @param cardData The card data whose status is to be displayed.
     */
    private fun setupCardStatus(holder: MyViewHolder, cardData: CardDetails) {
        val status = cardData.cardStatus
        holder.binding.tvStatus.text = status
        cardData.cardStatus = status
    }

    /**
     * Configures the card image and background color based on the available assets.
     *
     * Loads card art from `imageMap` if available, otherwise applies a default color scheme.
     * Also set up click listeners for each card item.
     *
     * @param holder The view holder containing card UI components.
     * @param cardData The card data providing image or color information.
     * @param position The adapter position used for click listener callbacks.
     */
    private fun setupCardImage(holder: MyViewHolder, cardData: CardDetails, position: Int) {
        val colors = getCardColors(cardData)
        val assetId = cardData.cardAssetId
        val base64 = assetId?.takeIf { it.isNotBlank() }?.let { imageMap[it] }

        // Match AddCardAdapter: reset recycled state so Glide/tint/background do not hide card art.
        Glide.with(holder.itemView.context).clear(holder.binding.imgCard)
        holder.binding.imgCard.setImageDrawable(null)
        holder.binding.imgCard.background = null
        ImageViewCompat.setImageTintList(holder.binding.imgCard, null)
        holder.binding.imgCard.clearColorFilter()

        if (base64 != null) {
            loadCardImage(holder, assetId)
        } else {
            setupDefaultCardImage(holder, colors)
        }

        setupClickListeners(holder, position)
    }

    /**
     * Safely converts a nullable String to a color Int.
     * Returns the default color if the string is null, empty, or invalid.
     */
    private fun String?.toSafeColor(default: Int = Color.WHITE): Int {
        val colorString = if (!this.isNullOrBlank()) this else DEFAULT_COLOR
        return runCatching { colorString.toColorInt() }.getOrDefault(default)
    }

    /**
     * Returns a Pair of background and foreground colors for a card.
     * Always returns non-null Int values.
     */
    private fun getCardColors(cardData: CardDetails): Pair<Int, Int> {
        val backgroundColor = cardData.backGroundColor
        val foregroundColor = cardData.foreGroundColor

        val parsedBgColor = backgroundColor.toSafeColor()
        val parsedFgColor = foregroundColor.toSafeColor()

        return parsedBgColor to parsedFgColor
    }

    /**
     * Applies background and foreground colors to the ImageView representing a card.
     */
    private fun setupDefaultCardImage(holder: MyViewHolder, colors: Pair<Int, Int>) {
        holder.binding.imgCard.setBackgroundColor(colors.first)
        ImageViewCompat.setImageTintList(holder.binding.imgCard, ColorStateList.valueOf(colors.second))
        holder.binding.imgCard.setColorFilter(colors.second, PorterDuff.Mode.SRC_ATOP)
    }

    /**
     * Decodes and loads the base64-encoded card image into the ImageView using Glide.
     *
     * @param holder The view holder containing card UI components.
     * @param assetId The ID used to locate the image data in the image map.
     */
    private fun loadCardImage(holder: MyViewHolder, assetId: String?) {
        assetId?.let { id ->
            imageMap[id]?.let { base64 ->
                try {
                    holder.binding.imgCard.setBackgroundColor(Color.TRANSPARENT)
                    ImageViewCompat.setImageTintList(holder.binding.imgCard, null)
                    holder.binding.imgCard.clearColorFilter()
                    holder.binding.imgCard.setImageBitmap(base64);
                } catch (e: Exception) {
                    logger.noStackTraceLog("LoadCardImage ", e)
                }
            }
        }
    }

    /**
     * Sets up click and long-click listeners for the card item view.
     *
     * Triggers the respective callback methods `onItemClicked` and `onItemLongPress`
     * when the card item is clicked or long-pressed.
     *
     * @param holder The view holder containing the card item view.
     * @param position The adapter position of the current card.
     */
    private fun setupClickListeners(holder: MyViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            onItemClicked(position, cardDataList[position].cardDecision.toString())
        }
        holder.itemView.setOnLongClickListener {
            onItemLongPress(position, cardDataList[position].cardDecision.toString())
            true
        }
    }

    /**
     * Returns the number of items in the adapter.
     *
     * @return The total count of card data items.
     */
    override fun getItemCount(): Int {
        return cardDataList.size
    }

    /**
     * ViewHolder class for holding views of each card item.
     *
     * @param binding The inflated view binding representing a card row.
     */
    class MyViewHolder(val binding: ItemWalletCardBinding) : RecyclerView.ViewHolder(binding.root)
}
