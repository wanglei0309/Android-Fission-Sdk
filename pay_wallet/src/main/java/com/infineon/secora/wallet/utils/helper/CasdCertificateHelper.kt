// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Reads the CASD Certificate Store from the connected secure element for wallet APIs.
 */
package com.infineon.secora.wallet.utils.helper

import android.content.Context
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.domain.wearable.ble.script.ScriptRunner
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.utils.constants.Constants.PNO_MDES
import com.infineon.secora.wearable.util.CasdCertificates
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object CasdCertificateHelper {

    private val logger: ApplicationLogger = getApplicationLogger(CasdCertificateHelper::class.java.simpleName)

    /**
     * Reads the MDES or VTS CASD certificate from the connected BLE wearable.
     *
     * @param context The application context.
     * @param pnoType Payment network type ([PNO_MDES] or VTS).
     * @return Uppercase hex TLV for the requested PNO type, or null when BLE is unavailable or the read fails.
     */
    suspend fun fetchCasdCertificate(context: Context, pnoType: String): String? {
        val seId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID).takeIf { it.isNotBlank() }
        if (seId == null) {
            logger.debug("Skipping CASD fetch: no DEVICE_SE_ID")
            return null
        }
        if (!BluetoothStateManager.isDeviceConnected(seId, context)) {
            logger.debug("Skipping CASD fetch: BLE not connected for seId=$seId")
            return null
        }

        val protocol = BluetoothStateManager.activeProtocol ?: run {
            logger.debug("Skipping CASD fetch: no active BLE protocol")
            return null
        }

        return try {
            val certificates = readCasdCertificatesBle(context, protocol) ?: return null
            val casdHex = when (pnoType) {
                PNO_MDES -> certificates.mdesCasdHex
                else -> certificates.vtsCasdHex
            }
            if (casdHex.isBlank()) {
                logger.debug("CASD fetch returned empty certificate for pnoType=$pnoType")
                null
            } else {
                logger.debug("CASD certificate fetched for $pnoType (${casdHex.length / 2} bytes)")
                casdHex.uppercase()
            }
        } catch (e: Exception) {
            logger.debug("CASD fetch failed: ${e.message}")
            null
        }
    }

    /**
     * Reads MDES and VTS CASD certificates from the connected BLE wearable.
     *
     * @param context The application context used for script execution.
     * @param protocol Active BLE protocol instance for secure-element communication.
     * @return Parsed [CasdCertificates], or null when the read fails or is cancelled.
     */
    private suspend fun readCasdCertificatesBle(
        context: Context,
        protocol: com.infineon.secora.wearable.protocolapi.IAsyncProtocol
    ): CasdCertificates? = suspendCancellableCoroutine { continuation ->
        ScriptRunner()
            .fetchCasdCertificates(context, protocol)
            .whenComplete { certificates, throwable ->
                if (throwable != null) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                    return@whenComplete
                }
                if (continuation.isActive) {
                    continuation.resume(certificates)
                }
            }
    }
}
