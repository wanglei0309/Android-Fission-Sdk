// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: DeviceDetachTargetResolver.kt resolves which wearable is being detached from FCM payload
 * fields and local pairing storage, including payment-app-to-SE mappings and BLE address hints.
 **/
package com.infineon.secora.wallet.domain.devicedetach

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.utils.constants.Constants.DEFAULT_DEVICE_NAME

object DeviceDetachTargetResolver {

    private val logger: ApplicationLogger =
        getApplicationLogger(DeviceDetachTargetResolver::class.java.simpleName)

    /** Resolved detach target: SE ID, optional payment app instance ID, and BLE address hint. */
    data class Target(
        val seId: String,
        val paymentAppInstanceId: String?,
        val bleAddress: String
    )

    /**
     * Persists paymentAppInstanceId → seId whenever the user links or selects a wearable.
     *
     * @param context                Application or activity context.
     * @param paymentAppInstanceId   Payment application instance ID.
     * @param seId                   Secure element ID of the linked device.
     */
    fun savePaymentAppToSeIdMapping(
        context: Context,
        paymentAppInstanceId: String?,
        seId: String
    ) {
        val pid = paymentAppInstanceId?.trim().orEmpty()
        val sid = seId.trim()
        if (pid.isEmpty() || sid.isEmpty()) return

        StorageRepository.saveString(PreferenceKey.paymentAppSeIdKey(pid), sid)
        logger.debug("Saved paymentApp→seId mapping pid=$pid seId=$sid")
    }

    /**
     * Resolves the device targeted by a device-detach FCM notification.
     * BLE MAC is read from `BLE_ADDRESS_<seId>` or the active GATT session — never hardcoded.
     *
     * @param context                Application or activity context.
     * @param paymentAppInstanceId   From FCM when present; null when the backend omits it.
     * @param seIdFromNotification   Optional SE ID from the FCM data payload.
     * @return [Target] when a device can be resolved, or null otherwise.
     */
    fun resolve(
        context: Context,
        paymentAppInstanceId: String?,
        seIdFromNotification: String? = null
    ): Target? {
        val pid = paymentAppInstanceId?.trim().orEmpty().takeIf { it.isNotEmpty() && !it.equals("null", true) }
        val notifiedSeId = seIdFromNotification?.trim().orEmpty().takeIf { it.isNotEmpty() }

        val seId = when {
            !notifiedSeId.isNullOrEmpty() -> notifiedSeId
            pid != null -> resolveSeIdForPaymentApp(pid)
            else -> StorageRepository.readString(PreferenceKey.DEVICE_SE_ID).trim()
        }.takeIf { it.isNotEmpty() } ?: resolveSeIdFromActiveBle()

        if (seId.isNullOrEmpty()) {
            logger.debug("Device detach resolve: no seId (pid=$pid)")
            return null
        }

        val bleAddress = resolveBleAddress(seId)
        logger.debug("Device detach resolve: seId=$seId pid=$pid bleAddress=$bleAddress")
        return Target(seId = seId, paymentAppInstanceId = pid, bleAddress = bleAddress)
    }

    /**
     * Resolves the secure element ID for the given payment app instance ID.
     * Returns the mapped SE ID from preferences or falls back to the stored device SE ID when applicable.
     * Returns an empty string if no matching SE ID is found.
     */
    private fun resolveSeIdForPaymentApp(paymentAppInstanceId: String): String {
        StorageRepository.readString(PreferenceKey.paymentAppSeIdKey(paymentAppInstanceId)).trim()
            .takeIf { it.isNotEmpty() }
            ?.let { return it }

        val storedPaymentId = StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID).trim()
        if (storedPaymentId == paymentAppInstanceId) {
            return StorageRepository.readString(PreferenceKey.DEVICE_SE_ID).trim()
        }
        return ""
    }

    /** When FCM has no pid, infer seId from whichever device is connected over GATT. */
    private fun resolveSeIdFromActiveBle(): String? {
        val activeAddress = BluetoothStateManager.activeProtocol
            ?.bluetoothDevice
            ?.address
            ?.trim()
            .orEmpty()
        if (activeAddress.isEmpty()) return null

        val activeNorm = normalizeAddress(activeAddress)
        for (seId in getPairedSeIds()) {
            val stored = StorageRepository.readString(PreferenceKey.bleAddressKey(seId)).trim()
            if (stored.isNotEmpty() && normalizeAddress(stored) == activeNorm) {
                return seId
            }
        }
        return null
    }

    /**
     * Resolves the BLE address for the given secure element ID.
     * Retrieves the address from preferences, selected device details, or the active BLE connection.
     * Returns an empty string if no matching BLE address is found.
     */
    private fun resolveBleAddress(seId: String): String {
        val fromPrefs = StorageRepository.readString(PreferenceKey.bleAddressKey(seId)).trim()
        if (fromPrefs.isNotEmpty()) return fromPrefs

        val selected = StorageRepository.readString(PreferenceKey.SELECTED_DEVICE_ADDRESS).trim()
        val deviceSeId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID).trim()
        if (selected.isNotEmpty() && deviceSeId == seId) {
            return selected
        }

        val active = BluetoothStateManager.activeProtocol?.bluetoothDevice?.address?.trim().orEmpty()
        if (active.isNotEmpty() && resolveSeIdFromActiveBle() == seId) {
            return active
        }
        return ""
    }

    /**
     * Retrieves all paired secure element IDs stored in preferences.
     * Returns the valid IDs as a set after removing empty values.
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
     * Normalizes the BLE address into a consistent format for comparison.
     * Trims the value, removes separators, and converts it to uppercase.
     */
    private fun normalizeAddress(address: String?): String? =
        address?.trim()?.uppercase()?.replace(":", "")?.replace("-", "")?.takeIf { it.isNotEmpty() }

    /**
     * Resolves a user-facing device label for portal detach success messaging.
     * Prefer stored [DEVICE_NAME], then the bonded BLE name, then a short SE suffix fallback.
     */
    fun resolveDisplayName(context: Context, seId: String, bleAddress: String = ""): String {
        val trimmedSeId = seId.trim()
        val storedSeId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID).trim()
        val storedName = StorageRepository.readString(PreferenceKey.DEVICE_NAME).trim()
        if (storedName.isNotEmpty() &&
            (storedSeId.isEmpty() || storedSeId.equals(trimmedSeId, ignoreCase = true))
        ) {
            return storedName
        }

        val bleName = resolveBleDeviceName(context, bleAddress)
        if (!bleName.isNullOrBlank()) return bleName

        val seSuffix = trimmedSeId.takeLast(6)
        return seSuffix.ifEmpty { DEFAULT_DEVICE_NAME }
    }

    private fun resolveBleDeviceName(context: Context, bleAddress: String): String? {
        val address = bleAddress.trim()
        if (address.isEmpty()) return null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        return try {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return null
            adapter.getRemoteDevice(address).name?.trim()?.takeIf { it.isNotEmpty() }
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
