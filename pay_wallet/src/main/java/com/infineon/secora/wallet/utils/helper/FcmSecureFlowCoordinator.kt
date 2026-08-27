// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: FcmSecureFlowCoordinator.kt serializes FCM-driven secure-element flows (device detach vs card deleted)
 * so only one runs at a time, with [FlowKind.DEVICE_DETACH] taking priority over [FlowKind.CARD_DELETED].
 **/
package com.infineon.secora.wallet.utils.helper

import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object FcmSecureFlowCoordinator {

    private val logger: ApplicationLogger =
        getApplicationLogger(FcmSecureFlowCoordinator::class.java.simpleName)

    private val executionMutex = Mutex()
    private val schedulerLock = Any()

    private val sequence = AtomicLong(0L)

    @Volatile
    private var activeFlowKind: FlowKind? = null

    private val loaderHoldCount = AtomicInteger(0)

    private const val PORTAL_DETACH_TTL_MS = 120_000L
    private const val PORTAL_BATCH_COALESCE_MS = 900L
    private const val PORTAL_BATCH_POLL_MS = 50L
    private const val QUEUE_POLL_MS = 25L

    @Volatile
    private var scheduledDetachSeId: String? = null

    @Volatile
    private var scheduledDetachAtMs: Long = 0L

    private data class WaitEntry(
        val kind: FlowKind,
        val order: Long
    )

    private val waitQueue = mutableListOf<WaitEntry>()

    private val waiterComparator = compareBy<WaitEntry>({ it.kind.priority }, { it.order })

    /**
     * Returns true while a serialized FCM detach/delete flow holds the coordinator lock.
     */
    fun isFlowInProgress(): Boolean = activeFlowKind != null

    /**
     * Increments the loader hold count so [com.infineon.secora.wallet.ui.home.MainActivity.showLoading]
     * ignores external dismiss requests during FCM flows.
     */
    fun acquireLoaderHold() {
        loaderHoldCount.incrementAndGet()
    }

    /**
     * Decrements the loader hold count after an FCM flow finishes showing the loader.
     */
    fun releaseLoaderHold() {
        val remaining = loaderHoldCount.decrementAndGet()
        if (remaining < 0) {
            loaderHoldCount.set(0)
        }
    }

    /**
     * Returns true when at least one FCM handler has acquired the loader hold.
     */
    fun isLoaderHoldActive(): Boolean = loaderHoldCount.get() > 0

    /** Identifies which serialized FCM secure-element flow is active. Lower [priority] runs first. */
    enum class FlowKind(val label: String, val priority: Int) {
        DEVICE_DETACH("Device Detach Update", 0),
        CARD_DELETED("Card Deleted", 1),
    }

    /**
     * Call as soon as a portal Device Detach FCM is received so Card Deleted can defer to it.
     * Preempts an in-flight Card Deleted flow by cancelling the BLE reconnect dialog when needed.
     *
     * @param seId Secure element ID from the detach notification, if present.
     */
    fun markDeviceDetachScheduled(seId: String?) {
        val trimmed = seId?.trim().orEmpty()
        if (trimmed.isEmpty()) return
        scheduledDetachSeId = trimmed
        scheduledDetachAtMs = System.currentTimeMillis()
        logger.debug("Portal device detach scheduled seId=$trimmed")
        if (activeFlowKind == FlowKind.CARD_DELETED) {
            logger.debug("Portal device detach preempting in-flight Card Deleted flow")
            FcmBleConnectionGate.cancelForPortalDeviceDetach(trimmed)
        }
    }

    /**
     * Returns true when a portal Device Detach was recently scheduled for [seId].
     *
     * @param seId Secure element ID to compare against the scheduled detach target.
     */
    fun isPortalDeviceDetachActive(seId: String?): Boolean {
        val trimmed = seId?.trim().orEmpty()
        if (trimmed.isEmpty()) return false
        val marked = scheduledDetachSeId?.trim().orEmpty()
        if (marked.isEmpty()) return false
        if (System.currentTimeMillis() - scheduledDetachAtMs > PORTAL_DETACH_TTL_MS) {
            return false
        }
        return marked.equals(trimmed, ignoreCase = true)
    }

    /**
     * Returns true when a Device Detach flow is running or waiting in the priority queue.
     */
    fun isDeviceDetachFlowActiveOrQueued(): Boolean {
        if (activeFlowKind == FlowKind.DEVICE_DETACH) return true
        synchronized(schedulerLock) {
            return waitQueue.any { it.kind == FlowKind.DEVICE_DETACH }
        }
    }

    /**
     * Card Deleted may arrive before Device Detach in the same portal batch. Wait briefly so
     * Device Detach can register and take priority in the queue.
     *
     * @param seId Secure element ID shared by notifications in the same portal batch.
     */
    suspend fun awaitPortalBatchCoalesce(seId: String) {
        if (seId.isBlank()) {
            delay(PORTAL_BATCH_COALESCE_MS)
            return
        }
        val deadline = System.currentTimeMillis() + PORTAL_BATCH_COALESCE_MS
        while (System.currentTimeMillis() < deadline) {
            if (isPortalDeviceDetachActive(seId) || isDeviceDetachFlowActiveOrQueued()) {
                logger.debug("FCM card deleted: portal batch detected, deferring to device detach")
                return
            }
            delay(PORTAL_BATCH_POLL_MS)
        }
    }

    /**
     * Runs [block] exclusively. [FlowKind.DEVICE_DETACH] waiters run before [FlowKind.CARD_DELETED].
     *
     * @param kind  Device detach or card-deleted flow identifier.
     * @param block Suspend work to run while holding the execution mutex.
     * @return Result of [block].
     */
    suspend fun <T> runSerialized(kind: FlowKind, block: suspend () -> T): T {
        val entry = WaitEntry(kind, sequence.getAndIncrement())
        synchronized(schedulerLock) {
            waitQueue.add(entry)
        }

        logger.debug("FCM flow waiting for turn: ${kind.label}")
        awaitPriorityTurn(entry)

        return executionMutex.withLock {
            synchronized(schedulerLock) {
                waitQueue.remove(entry)
            }
            activeFlowKind = kind
            logger.debug("FCM flow started: ${kind.label}")
            try {
                block()
            } finally {
                activeFlowKind = null
                logger.debug("FCM flow finished: ${kind.label}")
            }
        }
    }

    /**
     * Suspends until [entry] is the highest-priority waiter and the execution mutex is free.
     *
     * @param entry Queue entry registered for the current FCM flow.
     */
    private suspend fun awaitPriorityTurn(entry: WaitEntry) {
        while (true) {
            val isOurTurn = synchronized(schedulerLock) {
                waitQueue.minWithOrNull(waiterComparator) == entry
            }
            if (isOurTurn && !executionMutex.isLocked) {
                return
            }
            delay(QUEUE_POLL_MS)
        }
    }
}
