// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: SecureElementScriptCoordinator.kt tracks in-flight BLE/NFC secure-element scripts app-wide
 * so FCM detach/delete flows can await idle before fetching or running pending delete scripts.
 **/
package com.infineon.secora.wallet.utils.helper

import kotlinx.coroutines.delay
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

object SecureElementScriptCoordinator {

    private val activeScriptOperations = AtomicInteger(0)

    private const val IDLE_POLL_MS = 50L
    private const val MAX_IDLE_WAIT_MS = 180_000L

    /** Increments the active script count when a BLE/NFC script starts. */
    fun onScriptStarted() {
        activeScriptOperations.incrementAndGet()
    }

    /** Decrements the active script count when a BLE/NFC script finishes. */
    fun onScriptFinished() {
        val remaining = activeScriptOperations.decrementAndGet()
        if (remaining < 0) {
            activeScriptOperations.set(0)
        }
    }

    /** Returns true while at least one secure-element script is in progress. */
    fun isScriptRunning(): Boolean = activeScriptOperations.get() > 0

    /**
     * Clears a stuck active count after a forced timeout/disconnect when any APDU left the script future hanging.
     */
    fun forceResetActiveScripts() {
        activeScriptOperations.set(0)
    }

    /**
     * Suspends until no BLE/NFC script reported via [onScriptStarted]/[onScriptFinished] is active,
     * or until [MAX_IDLE_WAIT_MS] elapses.
     */
    suspend fun awaitIdle() {
        var waitedMs = 0L
        while (isScriptRunning() && waitedMs < MAX_IDLE_WAIT_MS) {
            delay(IDLE_POLL_MS)
            waitedMs += IDLE_POLL_MS
        }
    }

    /**
     * Wraps a [java.util.concurrent.CompletableFuture] so script start/finish are tracked until completion.
     *
     * @param future CompletableFuture returned by [com.infineon.secora.wallet.use_cases.cards.ScriptHandler].
     * @return The same future, with completion hooked to decrement the active count.
     */
    fun <T> trackFuture(future: CompletableFuture<T>): CompletableFuture<T> {
        onScriptStarted()
        return future.whenComplete { _, _ -> onScriptFinished() }
    }
}
