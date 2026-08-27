// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: PendingDeleteScriptExecutionGate.kt prevents overlapping getPending delete-script runs
 * (and duplicate acknowledge calls) across [com.infineon.secora.wallet.ui.fragment.AvailableDeviceFragment],
 * [com.infineon.secora.wallet.ui.fragment.CardListFragment], and device-detach executors for the same SE.
 **/
package com.infineon.secora.wallet.utils.helper

/**
 * Process-wide guard for sequential pending delete script execution per secure-element ID.
 */
object PendingDeleteScriptExecutionGate {

    @Volatile
    private var activeSeId: String? = null

    /**
     * Marks delete-script execution as started for [seId] when no other SE is already running.
     *
     * @return `true` if this caller owns the gate; `false` if another flow is already executing deletes.
     */
    @Synchronized
    fun tryBegin(seId: String): Boolean {
        val id = seId.trim()
        if (id.isEmpty()) return false
        val current = activeSeId
        if (current != null && current != id) return false
        if (current == id) return false
        activeSeId = id
        return true
    }

    /** Clears the gate when delete-script execution finishes for [seId]. */
    @Synchronized
    fun end(seId: String) {
        val id = seId.trim()
        if (id.isEmpty()) return
        if (activeSeId == id) {
            activeSeId = null
        }
    }

    /** Returns true when pending delete scripts are executing for [seId]. */
    @Synchronized
    fun isInProgress(seId: String): Boolean {
        val id = seId.trim()
        if (id.isEmpty()) return false
        return activeSeId == id
    }
}
