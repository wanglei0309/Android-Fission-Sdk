// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: AddCardAdapter.kt displays a list of payment cards in a RecyclerView.
 * It shows card details, images, and status, and supports click, long-press, and delete actions for each card.
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
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.client.data.models.common.CardList
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.ItemWalletCardBinding
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.utils.StringUtils.isEmptyString
import com.infineon.secora.wallet.utils.constants.Constants
import com.infineon.secora.wallet.utils.constants.Constants.DEFAULT_COLOR
import com.infineon.secora.wallet.utils.constants.Constants.PNO_MDES
import com.infineon.secora.wallet.utils.constants.Constants.PNO_VTS

/**
 * Adapter class for displaying a list of cards in a RecyclerView.
 *
 * It supports displaying Mastercard and Visa cards with nickname, card art (image), and expiration status.
 * It also handles click and long-press events on each card item.
 *
 * @property cardDataList The list of card data to display.
 * @property onItemClicked Callback triggered when a card is tapped.* @property onItemLongPress Callback triggered when a card is long pressed.
 * @property nicknameMap A mapping of card identifiers to user-defined nicknames.
 * @property imageMap A mapping of card art asset IDs to Base64-encoded image data.
 */
class AddCardAdapter(
    private val cardDataList: MutableList<CardList>,
    private val onItemClicked: (Int, String) -> Unit,
    private var onItemLongPress: (Int, String) -> Unit,
    private var nicknameMap: Map<Pair<String, String>, String> = emptyMap(),
    private val imageMap: MutableMap<String?, Bitmap?>
) : RecyclerView.Adapter<AddCardAdapter.MyViewHolder>() {

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
    private fun setupCardInfo(holder: MyViewHolder, cardData: CardList) {
        val expiryDate = cardData.expirationDate.toString()
        var formattedExpiryDate = expiryDate

        if (!expiryDate.contains(Constants.SLASH)) {
            formattedExpiryDate = expiryDate.take(2) + Constants.SLASH + expiryDate.substring(2, 4)
        }

        val context = holder.itemView.context

        val brand = when (cardData.pnoType) {
            PNO_MDES -> context.getString(R.string.master_card)
            PNO_VTS -> context.getString(R.string.visa)
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
    private fun setupNicknameAndDefault(holder: MyViewHolder, cardData: CardList) {
        val nickname = nicknameMap.entries.find {
            it.key.first == cardData.paymentAppInstanceId &&
                it.key.second.endsWith(cardData.dpanSuffix ?: "")
        }?.value

        holder.binding.tvNickName.text = nickname ?: ""
        val context = holder.itemView.context

        val paymentId = StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID)
        val defaultTokenRef = StorageRepository.readString(PreferenceKey.deviceKey(paymentId))

        val isDefaultCard =
            cardData.digitizationReferenceNumber == defaultTokenRef

        val isSuspended =
            cardData.status.equals(context.getString(R.string.text_suspended), ignoreCase = true)

        if (isDefaultCard && !isSuspended) {
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
    private fun setupCardStatus(holder: MyViewHolder, cardData: CardList) {
        val status = cardData.status
        holder.binding.tvStatus.text = status
        cardData.status = status
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
    private fun setupCardImage(holder: MyViewHolder, cardData: CardList, position: Int) {
        val assetId = getAssetId(cardData)
        val colors = getCardColors(cardData)

        // Clear previous image/background
        holder.binding.imgCard.setImageDrawable(null)
        holder.binding.imgCard.background = null
        ImageViewCompat.setImageTintList(holder.binding.imgCard, null)
        holder.binding.imgCard.clearColorFilter()

        val base64Image = assetId?.let { imageMap[it] }
        if (base64Image != null) {
            holder.binding.imgCard.setImageBitmap(base64Image);
        } else {
            setupDefaultCardImage(holder, colors)
        }
        setupClickListeners(holder, position)
    }

    /**
     * Retrieves the asset ID (card art) based on the payment network type.
     *
     * Supports both Mastercard (MDES) and Visa (VTS) configurations.
     *
     * @param cardData The card data used to extract the asset ID.
     * @return The asset ID string if available, otherwise null.
     */
    private fun getAssetId(cardData: CardList): String? {
        return when (cardData.pnoType) {
            PNO_MDES -> cardData.productConfig?.cardBackgroundCombinedAssetId
            PNO_VTS -> cardData.cardMetaData?.cardData
                ?.firstOrNull { it.contentType == "digitalCardArt" }
                ?.guid

            else -> null
        }
    }

    /**
     * Converts a nullable string to a color integer safely.
     *
     * If the string is null, empty, or not a valid color, returns [default].
     * By default, [default] is Color.WHITE.
     *
     * @receiver Nullable string representing a color (e.g., "#FF0000").
     * @param default Fallback color if parsing fails or string is null/empty.
     * @return Parsed color as an Int, or [default] on failure.
     */
    private fun String?.toParsedColor(default: Int = Color.WHITE): Int =
        runCatching {
            if (!isEmptyString(this)) this!!.toColorInt() else DEFAULT_COLOR.toColorInt()
        }.getOrDefault(default)

    /**
     * Determines the background and foreground colors for a card.
     *
     * Defaults to white if the provided color strings are null or invalid.
     *
     * @param cardData The card data containing color configuration.
     * @return A pair of colors (backgroundColor, foregroundColor) as Int values.
     */
    private fun getCardColors(cardData: CardList): Pair<Int, Int> {
        val foregroundColor = when (cardData.pnoType) {
            PNO_MDES -> cardData.productConfig?.foregroundColor
            PNO_VTS -> cardData.cardMetaData?.foregroundColor
            else -> null
        }

        val backgroundColor = when (cardData.pnoType) {
            PNO_MDES -> cardData.productConfig?.backgroundColor
            PNO_VTS -> cardData.cardMetaData?.backgroundColor
            else -> null
        }

        return backgroundColor.toParsedColor() to foregroundColor.toParsedColor()
    }

    /**
     * Applies a simple color-based card placeholder if no image is available.
     *
     * Uses color drawables to visually represent the card background and tint.
     *
     * @param holder The view holder containing card UI components.
     * @param colors A pair of colors (backgroundColor, foregroundColor).
     */
    private fun setupDefaultCardImage(holder: MyViewHolder, colors: Pair<Int, Int>) {
        holder.binding.imgCard.setBackgroundColor(colors.first)
        ImageViewCompat.setImageTintList(
            holder.binding.imgCard,
            ColorStateList.valueOf(colors.second)
        )
        holder.binding.imgCard.setColorFilter(colors.second, PorterDuff.Mode.SRC_ATOP)
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
     * @param binding The inflated viewbinding representing a card row.
     */
    class MyViewHolder(val binding: ItemWalletCardBinding) : RecyclerView.ViewHolder(binding.root)

    /**
     * Updates the existing nickname map by clearing current entries and inserting values from the new map.
     * Ensures that only the latest nickname data is stored in memory.
     */
    fun updateNicknameMap(newMap: Map<Pair<String, String>, String>) {
        nicknameMap = newMap.toMap()
        notifyDataSetChanged()
    }
}