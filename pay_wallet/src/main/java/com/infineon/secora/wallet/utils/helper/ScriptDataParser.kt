// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: ScriptDataParser handles script data parsing.
 **/
package com.infineon.secora.wallet.utils.helper

import android.util.Base64
import com.infineon.secora.wallet.BuildConfig
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.utils.constants.JsonKey
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

object ScriptDataParser {
    private val logger by lazy {
        ApplicationLogger.getApplicationLogger("ScriptDataParser")
    }

    /**
     * Decodes script data that may be Base64 once or twice,
     * and normalizes both JSON object and array roots.
     */
    fun decodeToJsonBytes(scriptData: String?): ByteArray? {
        if (scriptData.isNullOrBlank()) {
            logger.debug("decodeToJsonBytes: input is null/blank")
            return null
        }

        val firstPass = decodeBase64ToString(scriptData) ?: return null
        val firstNormalized = normalizeJson(firstPass)
        if (firstNormalized != null) {
            logger.debug("decodeToJsonBytes: parsed single-pass payload")
            return firstNormalized.toByteArray(Charsets.UTF_8)
        }

        val secondPass = decodeBase64ToString(firstPass) ?: return null
        val secondNormalized = normalizeJson(secondPass)
        if (secondNormalized != null) {
            logger.debug("decodeToJsonBytes: parsed double-pass payload")
        } else {
            logger.debug("decodeToJsonBytes: failed to parse payload")
        }
        return secondNormalized?.toByteArray(Charsets.UTF_8)
    }

    private fun decodeBase64ToString(value: String): String? {
        return try {
            String(Base64.decode(value.trim(), Base64.DEFAULT), Charsets.UTF_8)
        } catch (e: Exception) {
            if (BuildConfig.ENABLE_APP_LOGS) {
                logger.debug("decodeBase64ToString: base64 decode failed | ${e.message}")
            }
            null
        }
    }

    private fun normalizeJson(decoded: String): String? {
        return try {
            when (val parsed = JSONTokener(decoded.trim()).nextValue()) {
                is JSONObject -> {
                    logger.debug("normalizeJson: root type=object")
                    parsed.toString()
                }
                is JSONArray -> {
                    logger.debug("normalizeJson: root type=array")
                    JSONObject().apply {
                        put(JsonKey.APDU_LIST, parsed)
                    }.toString()
                }
                else -> null
            }
        } catch (e: Exception) {
            if (BuildConfig.ENABLE_APP_LOGS) {
                logger.debug("normalizeJson: invalid json | ${e.message}")
            }
            null
        }
    }
}
