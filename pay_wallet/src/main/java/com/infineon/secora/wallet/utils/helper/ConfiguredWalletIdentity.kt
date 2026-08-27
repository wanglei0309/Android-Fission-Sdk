// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

package com.infineon.secora.wallet.utils.helper

import android.content.Context
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.domain.wearable.ble.script.ScriptHandler

/**
 * Infineon demo 对齐：OEM / wearable / SE_TYPE_GROUP 来自 [configuration.json] 与
 * assets/config.properties 硬编码；CPLC 只用于读设备 SE ID，不参与 PrepSe 的 cplcIcTypeIdAndSeGroupId。
 */
object ConfiguredWalletIdentity {

    const val OEM_ID = ScriptHandler.TITAN_OEM_ID
    const val OEM_NAME = ScriptHandler.TITAN_OEM_NAME
    const val SE_ID = ScriptHandler.TITAN_SE_ID
    const val WEARABLE_ID = ScriptHandler.TITAN_WEARABLE_ID

    data class Identity(
        val oemId: String = OEM_ID,
        val oemName: String = OEM_NAME,
        val seId: String = SE_ID,
        val wearableId: String = WEARABLE_ID,
        val seTypeGroup: String = ""
    )

    fun load(context: Context): Identity =
        Identity(seTypeGroup = readPersistedSeTypeGroup(context))

    fun hasConfiguredOem(@Suppress("UNUSED_PARAMETER") context: Context): Boolean = true

    fun registrationOemId(
        @Suppress("UNUSED_PARAMETER") context: Context,
        @Suppress("UNUSED_PARAMETER") fetchedOemIdHex: String = ""
    ): String = OEM_ID

    /** TITAN 硬编码 SE type group（config.properties: SE_TYPE_GROUP=DE81-3502）。 */
    fun configuredSeTypeGroup(context: Context): String {
        val fromAssets = PropertiesManager.getPropertyFromAssets(
            context,
            PropertiesManager.PropertyKey.SE_TYPE_GROUP
        )
        return fromAssets.ifBlank { "DE81-$OEM_ID" }
    }

    fun formatSeTypeGroup(icTypeHex: String, seGroupIdHex: String): String? {
        val icType = icTypeHex.trim().uppercase()
        val seGroup = seGroupIdHex.trim().uppercase()
        return if (icType.isNotEmpty() && seGroup.isNotEmpty()) "$icType-$seGroup" else null
    }

    /** Demo：PrepSe / saveOEMDetails 始终用硬编码 SE_TYPE_GROUP，不用 CPLC icType-seGroup。 */
    fun registrationSeTypeGroup(
        context: Context,
        @Suppress("UNUSED_PARAMETER") fetchedIcTypeHex: String = "",
        @Suppress("UNUSED_PARAMETER") fetchedSeGroupIdHex: String = ""
    ): String = readPersistedSeTypeGroup(context)

    fun registrationWearableId(
        @Suppress("UNUSED_PARAMETER") context: Context,
        @Suppress("UNUSED_PARAMETER") fetchedWearableModelIdHex: String = ""
    ): String = WEARABLE_ID

    /**
     * 设备配对 / CPLC 回调：只写 TITAN OEM 常量，不把 CPLC icType-seGroup 写入 prefs。
     */
    fun persistForRegistration(
        context: Context,
        @Suppress("UNUSED_PARAMETER") fetchedOemIdHex: String = "",
        @Suppress("UNUSED_PARAMETER") fetchedIcTypeHex: String = "",
        @Suppress("UNUSED_PARAMETER") fetchedSeGroupIdHex: String = "",
        @Suppress("UNUSED_PARAMETER") fetchedWearableModelIdHex: String = ""
    ) {
        seedHardcodedIdentity(context)
    }

    fun readPersistedSeTypeGroup(context: Context): String {
        val raw = StorageRepository.readString(PreferenceKey.CPLC_SE_TYPE_GROUP).trim()
        if (raw.isNotEmpty()) return raw
        return configuredSeTypeGroup(context)
    }

    fun readPersistedOemId(@Suppress("UNUSED_PARAMETER") context: Context): String = OEM_ID

    /** 写入 TITAN OEM / wearable，并持久化 config 中的 SE_TYPE_GROUP。 */
    fun seedHardcodedIdentity(context: Context) {
        ScriptHandler.applyTitanHardcodedIdentity(context)
        StorageRepository.saveString(
            PreferenceKey.CPLC_SE_TYPE_GROUP,
            configuredSeTypeGroup(context)
        )
    }
}
