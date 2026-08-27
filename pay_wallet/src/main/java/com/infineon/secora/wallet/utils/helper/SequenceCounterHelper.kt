// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: SequenceCounterHelper.kt resolves the sequence counter required for get-pending-task calls,
 * reading it from the connected BLE wearable or using a fallback for NFC/disconnected devices.
 **/
package com.infineon.secora.wallet.utils.helper

import android.content.Context
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.domain.wearable.ble.script.ScriptHandler
import com.infineon.secora.wallet.utils.constants.Constants.NFC_DEVICE_MODEL
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume

object SequenceCounterHelper {

    private const val SEQUENCE_COUNTER_ASSET = "sequence-counter.json"
    private const val FALLBACK_SEQUENCE_COUNTER = "161"

    /**
     * Returns true when the linked device name indicates an NFC wearable model.
     */
    fun isNfcDevice(): Boolean {
        return try {
            StorageRepository.readString(PreferenceKey.DEVICE_NAME).contains(NFC_DEVICE_MODEL)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Reads sequence counter from the connected wearable when possible; otherwise returns [FALLBACK_SEQUENCE_COUNTER].
     */
    suspend fun resolveSequenceCounter(context: Context): String {
        if (isNfcDevice() || !BluetoothStateManager.isConnected) {
            return FALLBACK_SEQUENCE_COUNTER
        }
        val requestBytes = loadSequenceCounterRequest(context) ?: return FALLBACK_SEQUENCE_COUNTER
        return readSequenceCounterBle(context, requestBytes)
    }

    /**
     * Loads the sequence counter request data from the asset file.
     * Validates the JSON content and returns it as a byte array if successful.
     * Returns null if the file is invalid or cannot be read.
     */
    private fun loadSequenceCounterRequest(context: Context): ByteArray? {
        return try {
            context.assets.open(SEQUENCE_COUNTER_ASSET).use { input ->
                val bytes = input.readBytes()
                JSONObject(String(bytes, StandardCharsets.UTF_8))
                bytes
            }
        } catch (_: JSONException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Reads the sequence counter value over BLE using the provided request data.
     * Executes the script, extracts the sequence counter from the response, and returns a fallback value if the read fails.
     * Resumes with the resolved sequence counter once the operation is completed.
     */
    private suspend fun readSequenceCounterBle(context: Context, requestBytes: ByteArray): String =
        suspendCancellableCoroutine { continuation ->
            val scriptHandler = ScriptHandler(context, null)
            scriptHandler.executeScript(requestBytes)
                .whenComplete { executionResult, throwable ->
                    if (throwable != null) {
                        if (continuation.isActive) {
                            continuation.resume(FALLBACK_SEQUENCE_COUNTER)
                        }
                        return@whenComplete
                    }
                    val counter = parseSequenceCounter(executionResult?.apduResults?.getOrNull(1)?.hexResponse)
                    if (continuation.isActive) {
                        continuation.resume(counter ?: FALLBACK_SEQUENCE_COUNTER)
                    }
                }
        }

    /**
     * Parses the sequence counter value from the given hex response.
     * Extracts the required bytes and converts them into a decimal string.
     * Returns null if the response is invalid or cannot be parsed.
     */
    private fun parseSequenceCounter(hexResponse: String?): String? {
        if (hexResponse.isNullOrBlank()) return null
        val parts = hexResponse.trim().split(" ")
        if (parts.size != 7) return null
        return try {
            (parts[2] + parts[3] + parts[4]).toInt(16).toString()
        } catch (_: Exception) {
            null
        }
    }
}
