// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: PropertiesManager.kt handles the properties file present inside assets folder.
 **/
package com.infineon.secora.wallet.utils.helper

import android.content.Context
import com.infineon.secora.wallet.client.operations.common.logger.Logger
import java.util.Properties

object PropertiesManager {

    private val logger = Logger.getNewLogger(PropertiesManager::class.java.name.toString())

    private const val BASE_URLS_PROPERTIES = "config.properties"

    /**
     * Reads a trimmed string property from `assets/config.properties`.
     *
     * @param context Android context for asset access.
     * @param key     Property key in the file.
     * @return The value, or an empty string if missing or if the file cannot be read.
     */
    fun getPropertyFromAssets(context: Context, key: PropertyKey): String {
        return try {
            val properties = Properties()
            context.assets.open(BASE_URLS_PROPERTIES).use { inputStream ->
                properties.load(inputStream)
            }
            properties.getProperty(key.name)?.trim().orEmpty()
        } catch (e: Exception) {
            logger.noStackTraceLog("Failed to getPropertyFromAssets ", e)
            ""
        }
    }

    /**
     * Returns true when fetched [oemId] does not match [PropertyKey.OEM_ID] from `config.properties`.
     * Blank configured values are treated as no restriction.
     */
    fun isConfiguredWalletOemMismatch(@Suppress("UNUSED_PARAMETER") context: Context, @Suppress("UNUSED_PARAMETER") oemId: String): Boolean = false

    enum class PropertyKey {
        OEM_ID,
        OEM_NAME,
        SE_TYPE_GROUP,
        WEARABLE_ID
    }
}