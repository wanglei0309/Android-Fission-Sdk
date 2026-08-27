// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: DeviceIntegrity.kt detects common indicators of a rooted or compromised Android
 * environment so the wallet can record integrity status during startup.
 **/
package com.infineon.secora.wallet.utils.helper

import android.os.Build
import java.io.File

/**
 * DeviceIntegrity: Lightweight checks for rooted / modified device images.
 *
 * Detection is intentionally non-blocking for callers; use [isDeviceRooted] to inspect status.
 */
object DeviceIntegrity {

    private val suPaths = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/system/bin/failsafe/su",
        "/system/sd/xbin/su"
    )

    /**
     * Returns true when common root indicators are present on the device.
     */
    @JvmStatic
    fun isDeviceRooted(): Boolean {
        return hasTestKeys() || hasSuBinary()
    }

    /**
     * Returns true when [Build.TAGS] contains "test-keys", which typically indicates
     * a non-production or engineering build.
     */
    private fun hasTestKeys(): Boolean {
        val tags = Build.TAGS
        return tags != null && tags.contains("test-keys")
    }

    /**
     * Returns true when a known `su` binary or Superuser APK path exists on the device.
     */
    private fun hasSuBinary(): Boolean {
        return suPaths.any { path -> File(path).exists() }
    }
}
