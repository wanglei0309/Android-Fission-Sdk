// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: DeviceDetachBleCleanup.kt disconnects BLE, removes system bond, and clears local pairing
 * for a detached wearable so it can reappear in the scan list after an FCM device-detach notification.
 **/
package com.infineon.secora.wallet.domain.devicedetach

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wearable.SecoraWearableSDK

object DeviceDetachBleCleanup {

    private val logger: ApplicationLogger =
        getApplicationLogger(DeviceDetachBleCleanup::class.java.simpleName)

    /** Outcome of BLE cleanup for a detached device. */
    data class Result(val seId: String, val bleAddress: String)

    /**
     * Disconnects BLE, unbonds, and clears pairing prefs for [seId] only.
     * Other paired devices are untouched.
     *
     * @param context          Application or activity context.
     * @param seId             Secure element ID of the detached device.
     * @param bleAddressHint   Optional MAC from [DeviceDetachTargetResolver] or active GATT when prefs were cleared.
     * @return Cleanup result, or null when [seId] is blank.
     */
    fun cleanup(context: Context, seId: String, bleAddressHint: String = ""): Result? {
        val trimmedSeId = seId.trim()
        if (trimmedSeId.isEmpty()) return null

        val bleAddress = resolveBleAddress(trimmedSeId, bleAddressHint)
        logger.debug("Device detach BLE cleanup seId=$trimmedSeId address=$bleAddress")
        disconnectBleForAddress(bleAddress)

        if (bleAddress.isNotEmpty()) {
            BluetoothStateManager.removeConnectedDevice(bleAddress)
            disconnectAndUnbond(context, bleAddress)
        } else {
            BluetoothStateManager.disconnectActiveProtocol()
        }

        removePairedDevice(trimmedSeId)
        if (StorageRepository.readString(PreferenceKey.DEVICE_SE_ID).trim() == trimmedSeId) {
            StorageRepository.clearString(key = PreferenceKey.DEVICE_SE_ID)
        }

        val selectedDeviceAddress = StorageRepository.readString(PreferenceKey.SELECTED_DEVICE_ADDRESS)
        if (bleAddress.isNotEmpty() &&
            normalizeAddress(selectedDeviceAddress) == normalizeAddress(bleAddress)
        ) {
            StorageRepository.clearString(key = PreferenceKey.SELECTED_DEVICE_ADDRESS)
        }

        return Result(trimmedSeId, bleAddress)
    }

    /**
     * Resolves the BLE address for the given secure element ID.
     * Returns the saved BLE address from preferences, falls back to the provided hint,
     * or uses the currently connected device address if available.
     */
    private fun resolveBleAddress(seId: String, hint: String): String {
        val fromPrefs = StorageRepository.readString(PreferenceKey.bleAddressKey(seId)).trim()
        if (fromPrefs.isNotEmpty()) return fromPrefs

        if (hint.isNotBlank()) return hint.trim()
        return BluetoothStateManager.activeProtocol
            ?.bluetoothDevice
            ?.address
            ?.trim()
            .orEmpty()
    }

    /**
     * Disconnects the active BLE device for the given BLE address.
     * Verifies whether the active device matches the target address before disconnecting.
     * Clears the active BLE protocol after the disconnect process is completed.
     */
    private fun disconnectBleForAddress(bleAddress: String) {
        val active = BluetoothStateManager.activeProtocol
        if (active == null) return

        val activeNorm = normalizeAddress(active.bluetoothDevice.address)
        val targetNorm = normalizeAddress(bleAddress)

        val shouldDisconnect = bleAddress.isEmpty() ||
            (activeNorm != null && targetNorm != null && activeNorm == targetNorm)

        if (!shouldDisconnect) {
            return
        }

        try {
            SecoraWearableSDK.getInstance().getInterface().disconnectBLEDevice(active)
            logger.debug("Device detach cleanup: disconnected active BLE protocol")
        } catch (e: Exception) {
            logger.debug("Device detach cleanup: error disconnecting protocol: ${e.message}")
        }
        BluetoothStateManager.setActiveProtocol(null)
    }

    /**
     * Disconnects and removes the bond for the specified BLE device address.
     * Validates permission and device address before attempting bond removal.
     * Handles cleanup safely if the device is available for unpairing.
     */
    private fun disconnectAndUnbond(context: Context, deviceAddress: String) {
        if (!hasBluetoothConnectPermission(context)) {
            logger.debug("Device detach cleanup: BLUETOOTH_CONNECT not granted, skipping bond removal")
            return
        }
        if (!BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
            logger.debug("Device detach cleanup: invalid address $deviceAddress")
            return
        }
        try {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            val device = adapter?.getRemoteDevice(deviceAddress) ?: return
            SecoraWearableSDK.getInstance().getInterface().removeBond(device)
            logger.debug("Device detach cleanup: bond removed for $deviceAddress")
        } catch (e: SecurityException) {
            logger.debug("Device detach cleanup: SecurityException removing bond: ${e.message}")
        } catch (e: IllegalArgumentException) {
            logger.debug("Device detach cleanup: invalid address removing bond: ${e.message}")
        }
    }

    /**
     * Removes the given secure element ID from the paired device list.
     * Updates the stored paired device entries and clears the saved BLE address for the device.
     */
    private fun removePairedDevice(seId: String) {
        val set = getPairedSeIds().toMutableSet()
        set.remove(seId)
        StorageRepository.saveString(PreferenceKey.PAIRED_SE_IDS, set.joinToString(","))
        StorageRepository.clearString(PreferenceKey.bleAddressKey(seId))
        logger.debug("Device detach cleanup: PAIRED_DEVICE_REMOVED seId=$seId")
    }

    /**
     * Retrieves all stored paired secure element IDs from preferences.
     * Returns the list of valid paired IDs as a set.
     */
    private fun getPairedSeIds(): Set<String> {
        val raw = StorageRepository.readString(PreferenceKey.PAIRED_SE_IDS)
        if (raw.isBlank()) return emptySet()

        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    /**
     * Checks whether the Bluetooth connect permission is granted.
     * Returns true if permission is available, otherwise false.
     */
    private fun hasBluetoothConnectPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Normalizes the BLE address into a standard format for comparison.
     * Removes spaces and separators, and converts the value to uppercase.
     */
    private fun normalizeAddress(address: String?): String? =
        address?.trim()?.uppercase()?.replace(":", "")?.replace("-", "")?.takeIf { it.isNotEmpty() }
}
