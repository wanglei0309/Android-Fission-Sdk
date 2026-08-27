// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT
package com.infineon.secora.wallet.domain.wearable.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.os.BundleCompat
import com.infineon.secora.wallet.PayExternalLaunch
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey

/**
 * Centralized UI state observer for Bluetooth-related changes.
 *
 * This object listens to system Bluetooth broadcast events and notifies
 * registered UI listeners so screens can react (e.g., refresh UI,
 * enable/disable actions, reconnect flows).
 *
 * Responsibilities:
 * - Listen for Bluetooth adapter state changes
 * - Track device ACL connection / disconnection events
 * - Propagate changes to registered UI observers
 */
object BluetoothUiStateManager {

    private val listeners = mutableSetOf<() -> Unit>()
    private var receiverRegistered = false

    /** Bridge runnable so any connection state change (add/remove device) triggers icon/UI update. */
    private val onConnectionStateChangedRunnable = Runnable { notifyListeners() }

    /**
     * BroadcastReceiver listening for system Bluetooth events.
     * It updates the shared BluetoothStateManager and notifies UI listeners.
     */
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {

                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_OFF -> {
                            BluetoothStateManager.setBluetoothEnabled(false)
                            BluetoothStateManager.clearAllConnectedDevices()
                        }

                        BluetoothAdapter.STATE_ON -> {
                            BluetoothStateManager.setBluetoothEnabled(true)
                        }
                    }
                    notifyListeners()
                }

                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = intent.extras?.let {
                        BundleCompat.getParcelable(
                            it,
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    }
                    val seId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID).takeIf { id -> id.isNotBlank() }
                    device?.let { BluetoothStateManager.addConnectedDevice(it.address, seId) }
                    notifyListeners()
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = intent.extras?.let {
                        BundleCompat.getParcelable(
                            it,
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    }
                    device?.let {
                        if (!PayExternalLaunch.shouldIgnoreHostAclDisconnect(it.address)) {
                            BluetoothStateManager.removeConnectedDevice(it.address)
                        }
                    }
                    notifyListeners()
                }
            }
        }
    }

    /**
     * Registers a UI listener for Bluetooth state updates.
     *
     * - Adds the listener to the internal set
     * - Lazily registers the BroadcastReceiver once
     * - When the first listener is added, also subscribes to [BluetoothStateManager] so that
     *   any call to [BluetoothStateManager.addConnectedDevice] / [BluetoothStateManager.removeConnectedDevice]
     *   (e.g. from GATT disconnect or AvailableDeviceFragment) triggers an icon/UI update.
     *
     * @param context Context used to register the receiver (applicationContext is used internally)
     * @param listener Callback invoked when Bluetooth state changes
     */
    fun register(context: Context, listener: () -> Unit) {
        listeners.add(listener)

        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
            context.applicationContext.registerReceiver(bluetoothReceiver, filter)
            receiverRegistered = true
        }

        // When first listener is added, bridge so any connection state change (e.g. from disconnect path) updates icon
        if (listeners.size == 1) {
            BluetoothStateManager.addOnConnectionStateChanged(onConnectionStateChangedRunnable)
        }
    }

    /**
     * Unregisters a previously registered UI listener.
     *
     * NOTE:
     * The BroadcastReceiver remains registered as long as at least
     * one listener is present. This avoids frequent register/unregister churn.
     *
     * @param listener The listener to remove
     */
    fun unregister(listener: () -> Unit) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            BluetoothStateManager.removeOnConnectionStateChanged(onConnectionStateChangedRunnable)
        }
    }

    /**
     * Notifies all registered listeners of a Bluetooth state change.
     *
     * This method should be lightweight as it may be triggered
     * frequently by system broadcasts.
     */
    private fun notifyListeners() {
        listeners.forEach { it.invoke() }
    }
}
