// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: BluetoothScanAdapter.kt displays a list of nearby Bluetooth devices in a RecyclerView.
 * It binds each device’s name and handles click actions to connect or select a device.
 **/
package com.infineon.secora.wallet.adapter

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresPermission
import androidx.recyclerview.widget.RecyclerView
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.databinding.ItemDevicesBluetoothDeviceBinding
import com.infineon.secora.wallet.databinding.ItemDevicesBluetoothEmptyStateBinding
import com.infineon.secora.wallet.models.BluetoothDeviceUiModel

/**
 * RecyclerView adapter for displaying a list of nearby or bonded Bluetooth devices.
 *
 * This adapter keeps a local mutable list and updates the UI synchronously
 * to avoid RecyclerView inconsistency during rapid scan/empty-state transitions.
 *
 * Responsibilities:
 * - Display the Bluetooth device name in each row.
 * - Handle item click events and propagate the selected [BluetoothDevice]
 *   through a callback.
 * - Update the list deterministically using [submitList].
 *
 * Key implementation details:
 * - Supports two row types: device row and empty-state row.
 * - Device name access requires [Manifest.permission.BLUETOOTH_CONNECT]
 *   on Android 12 (API 31) and above.
 *
 * Permissions:
 * - The caller must ensure that BLUETOOTH_CONNECT permission is granted
 *   before submitting the list or binding items.
 *
 * @param onClick Callback invoked when a Bluetooth device item is clicked.
 * The selected [BluetoothDevice] is passed to the caller.
 */
class BluetoothScanAdapter(
    private val onClick: (BluetoothDeviceUiModel) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var emptyStateMessage: String? = null
    private val devices = mutableListOf<BluetoothDeviceUiModel>()

    companion object {
        private const val VIEW_TYPE_DEVICE = 0
        private const val VIEW_TYPE_EMPTY_STATE = 1
    }

    /**
     * ViewHolder responsible for binding a single Bluetooth device
     * to the corresponding RecyclerView row.
     */
    inner class DeviceViewHolder(private val binding: ItemDevicesBluetoothDeviceBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onClick(devices[position])
                }
            }
        }

        /**
         * Binds Bluetooth device data to the UI.
         *
         * @param device Bluetooth device to display.
         *
         * @throws SecurityException if BLUETOOTH_CONNECT permission is missing.
         */
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        fun bind(device: BluetoothDeviceUiModel) {
            binding.textDeviceName.text = device.name
        }
    }

    inner class EmptyStateViewHolder(private val binding: ItemDevicesBluetoothEmptyStateBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener(null)
        }

        /**
         * Binds the empty-state message when no scanned devices are available.
         *
         * @param emptyMessage Optional custom message shown in the empty row.
         */
        fun bind(emptyMessage: String?) {
            binding.textEmptyMessage.text = emptyMessage ?: itemView.context.getString(R.string.no_secora_device_found)
        }
    }

    /**
     * Sets the message used by the empty-state row.
     *
     * @param message Optional custom empty-state message.
     */
    fun setEmptyStateMessage(message: String?) {
        emptyStateMessage = message
        if (devices.isEmpty()) {
            // Avoid fine-grained notify on synthetic empty row with ListAdapter diff updates.
            notifyDataSetChanged()
        }
    }

    /**
     * Replaces the current scanned device list and refreshes the adapter.
     *
     * @param list Latest scanned devices to display.
     */
    fun submitList(list: List<BluetoothDeviceUiModel>) {
        devices.clear()
        devices.addAll(list)
        notifyDataSetChanged()
    }

    /**
     * Inflates the item layout and creates a new [DeviceViewHolder].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_EMPTY_STATE -> {
                val binding = ItemDevicesBluetoothEmptyStateBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )

                EmptyStateViewHolder(binding)
            }

            else -> {
                val binding = ItemDevicesBluetoothDeviceBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )

                DeviceViewHolder(binding)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (devices.isEmpty()) VIEW_TYPE_EMPTY_STATE else VIEW_TYPE_DEVICE
    }

    /**
     * Binds the Bluetooth device at the given position to the ViewHolder.
     *
     * @param holder ViewHolder to bind.
     * @param position Adapter position.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EmptyStateViewHolder -> holder.bind(emptyStateMessage)
            is DeviceViewHolder -> holder.bind(devices[position])
        }
    }

    override fun getItemCount(): Int = if (devices.isEmpty()) 1 else devices.size
}
