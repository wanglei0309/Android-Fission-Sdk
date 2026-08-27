// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: BluetoothStateReceiver.kt is a broadcast receiver that listens for Bluetooth connection and disconnection events.
 * It updates the global connection status in BluetoothStateManager whenever a Bluetooth device connects
 * or disconnects from the system.
 **/
package com.infineon.secora.wallet.domain.wearable.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import com.infineon.secora.wallet.PayExternalLaunch
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger

/**
 * [BluetoothStateReceiver] is a BroadcastReceiver that listens for Bluetooth connection
 * and disconnection events and updates the [BluetoothStateManager].
 *
 * It listens specifically for:
 * - [BluetoothDevice.ACTION_ACL_CONNECTED] → A Bluetooth device has connected.
 * - [BluetoothDevice.ACTION_ACL_DISCONNECTED] → A Bluetooth device has disconnected.
 *
 * ⚠️ Requires the `BLUETOOTH_CONNECT` permission on Android 12 (API 31) and above.
 *
 * Usage:
 * - Register this receiver in your `Application` or `Activity
 *   with `ACTION_ACL_CONNECTED` and `ACTION_ACL_DISCONNECTED`.
 * - The connection state is stored in [BluetoothStateManager] for global access.
 */
class BluetoothStateReceiver : BroadcastReceiver() {

    /**
     * Called when the system broadcasts a Bluetooth connection/disconnection event.
     *
     * @param context The application or activity context.
     * @param intent  The broadcast intent containing the Bluetooth event details.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val action = intent.action

        when (action) {
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
                return
            }

            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val extras = intent.extras ?: return
                val device: BluetoothDevice? =
                    BundleCompat.getParcelable(
                        extras,
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    )
                device?.let {
                    val seId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID).takeIf { id -> id.isNotBlank() }
                    BluetoothStateManager.addConnectedDevice(it.address, seId)
                }
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val extras = intent.extras ?: return
                val device: BluetoothDevice? =
                    BundleCompat.getParcelable(
                        extras,
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    )
                device?.let {
                    if (PayExternalLaunch.shouldIgnoreHostAclDisconnect(it.address)) {
                        getApplicationLogger("BluetoothStateReceiver").debug(
                            "Host launch: ignore transient ACL disconnect for ${it.address}"
                        )
                        return@let
                    }
                    val logger = getApplicationLogger("BluetoothStateReceiver")
                    try {
                        logger.info("Device disconnected: ${it.address}")
                    } catch (_: SecurityException) {
                        logger.info("Device disconnected")
                    }
                    BluetoothStateManager.removeConnectedDevice(it.address)
                }
            }
        }
    }
}
