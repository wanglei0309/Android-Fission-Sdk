// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: PaymentDeviceAdapter.kt displays a list of connected or available payment devices.
 * It binds device details, handles image loading, and manages click and long-press events for each item.
 **/
package com.infineon.secora.wallet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.cdcvm.WearableHandState
import com.infineon.secora.wallet.databinding.ItemDevicesScannedDeviceBinding
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.models.PaymentDeviceList
import com.infineon.secora.wallet.utils.constants.Constants.NFC_DEVICE_MODEL

/**
 * RecyclerView Adapter for displaying a list of payment devices.
 *
 * This adapter displays device names and associated images (decoded from Base64),
 * and handles click and long-click actions on each item.
 *
 * @property devices A list of [PaymentDeviceList] representing the devices to be displayed.
 * @property onItemClicked A callback invoked when an item is clicked, providing the paymentAppInstanceId and position.
 * @property onItemLongPress A callback invoked when an item is long-pressed, providing the position and device name.
 */
class PaymentDeviceAdapter(
    private val devices: List<PaymentDeviceList>,
    private val onItemClicked: (String, Int) -> Unit,
    private var onItemLongPress: (Int, String) -> Unit,
    private var onButtonClick: (String, Int) -> Unit
) : RecyclerView.Adapter<PaymentDeviceAdapter.PaymentDeviceViewHolder>() {

    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)

    /**
     * ViewHolder class that holds the binding for a single device item layout.
     *
     * @property binding The ViewBinding for the layout [ItemDevicesScannedDeviceBinding].
     */
    inner class PaymentDeviceViewHolder(val binding: ItemDevicesScannedDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClicked(
                        devices[position].paymentAppInstanceId.toString(),
                        position
                    )
                } // Pass the clicked item to the callback
            }

            binding.nfcImageView.setOnClickListener {
                val position = bindingAdapterPosition

                if (position != RecyclerView.NO_POSITION) {
                    onButtonClick(
                        devices[position].walletAppInstanceId.toString(),
                        position
                    )
                }
            }

            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongPress(
                        position,
                        devices[position].deviceName.toString()
                    )
                }
                true  // Return true to indicate that the long press event is handled
            }
        }

        /**
         * Binds device details and updates the UI based on the device accessibility state.
         *
         * Displays the device name, loads the associated device image, and renders either
         * the connected or registration state depending on whether the device is currently accessible.
         *
         * @param paymentDevice [PaymentDeviceList] Device information used to populate and update the item view.
         */
        fun bind(paymentDevice: PaymentDeviceList) {
            binding.textDeviceName.text = paymentDevice.deviceName.toString()
            bindImage(paymentDevice)

            if (paymentDevice.isDeviceAccessible) {
                bindConnectedState(paymentDevice)
            } else {
                bindRegistrationState()
            }
        }

        /**
         * Binds the device image to the view.
         * @param paymentDevice Device containing the image data.
         */
        private fun bindImage(paymentDevice: PaymentDeviceList) {
            binding.cardImageView.setImageBitmap(null)
            paymentDevice.data?.let { bitmap ->
                try {
                    binding.cardImageView.setImageBitmap(bitmap)
                } catch (e: IllegalArgumentException) {
                    logger.noStackTraceLog("onBindViewHolder", e)
                }
            }
        }

        /**
         * Binds the device connection state
         *
         * @param paymentDevice Device used to determine the connection status and interaction state.
         */
        private fun bindConnectedState(paymentDevice: PaymentDeviceList) {
            val context = itemView.context.applicationContext
            // Green only when Bluetooth is on and this device is connected; black when BT is off or device disconnected.
            val showConnected = BluetoothStateManager.isBluetoothTurnedOn(context)
                && BluetoothStateManager.isDeviceConnected(paymentDevice.seId, context)
            val bluetoothIcon = if (showConnected) {
                R.drawable.ic_bluetooth_connected
            } else {
                R.drawable.ic_bluetooth_disconnected
            }

            val isNfc = paymentDevice.deviceName?.contains(NFC_DEVICE_MODEL) == true
            if (isNfc) {
                binding.nfcImageView.setImageResource(R.drawable.icon_nfc)
            } else {
                binding.nfcImageView.setImageResource(bluetoothIcon)
            }

            // Hand icon: only for the connected BLE wearable; hidden when BT off / disconnected / NFC.
            renderHand(if (showConnected && !isNfc) WearableHandState.handFor(paymentDevice.seId) else WearableHandState.Hand.HIDDEN)

            binding.nfcImageView.isClickable = false
            itemView.isClickable = true
        }

        /**
         * Applies the on-body / off-body / verified hand icon, or hides it.
         *
         * @param hand Resolved hand state for this row.
         */
        private fun renderHand(hand: WearableHandState.Hand) {
            when (hand) {
                WearableHandState.Hand.HIDDEN -> binding.imgHand.visibility = View.GONE
                WearableHandState.Hand.OFF_BODY -> showHand(R.drawable.ic_hand_off_body)
                WearableHandState.Hand.ON_BODY -> showHand(R.drawable.ic_hand_on_body)
                WearableHandState.Hand.VERIFIED -> showHand(R.drawable.ic_hand_verified)
            }
        }

        private fun showHand(drawableRes: Int) {
            binding.imgHand.setImageResource(drawableRes)
            binding.imgHand.visibility = View.VISIBLE
        }

        /**
         * Binds the registration state UI and enables device registration action.
         */
        private fun bindRegistrationState() {
            binding.nfcImageView.setImageResource(R.drawable.user_registration_icon)
            binding.imgHand.visibility = View.GONE
            binding.nfcImageView.isClickable = true
            itemView.isClickable = false
        }
    }

    /**
     * Creates a new [PaymentDeviceViewHolder] for the given view type.
     *
     * @param parent The parent ViewGroup.
     * @param viewType The view type of the new View.
     * @return A new instance of [PaymentDeviceViewHolder].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentDeviceViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemDevicesScannedDeviceBinding.inflate(inflater, parent, false)
        return PaymentDeviceViewHolder(binding)
    }

    /**
     * Binds the device data to the corresponding.
     *
     * @param holder The [PaymentDeviceViewHolder] instance to bind data to.
     * @param position The position of the item in the list.
     */
    override fun onBindViewHolder(holder: PaymentDeviceViewHolder, position: Int) {
        holder.bind(paymentDevice = devices[position])
    }

    /**
     * Returns the number of items in the adapter.
     *
     * @return The size of the [devices] list.
     */
    override fun getItemCount(): Int = devices.size
}
