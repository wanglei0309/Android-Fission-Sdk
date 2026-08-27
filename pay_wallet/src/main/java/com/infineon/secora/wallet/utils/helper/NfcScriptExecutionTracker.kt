// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: NfcScriptExecutionTracker.kt bridges NFC script lifecycle to [SecureElementScriptCoordinator]
 * so FCM detach/delete flows can wait for in-flight secure-element operations to finish.
 **/
package com.infineon.secora.wallet.utils.helper

object NfcScriptExecutionTracker {

    /**
     * Call before [com.infineon.secora.wearable.SecoraWearableSDKInterface.executeNfcOperation].
     */
    fun onNfcScriptStarted() = SecureElementScriptCoordinator.onScriptStarted()

    /**
     * Call after NFC script execution completes (success or failure).
     */
    fun onNfcScriptFinished() = SecureElementScriptCoordinator.onScriptFinished()
}
