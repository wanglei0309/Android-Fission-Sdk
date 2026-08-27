// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: EventBus.kt
 *
 * A simple event bus implementation using Kotlin SharedFlow to replace
 * the deprecated LocalBroadcastManager. Provides a lightweight, lifecycle-aware
 * way to communicate between components.
 */
package com.infineon.secora.wallet.firebase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Represents an event with an action and optional data payload.
 *
 * @property action The action identifier for the event (similar to Intent action)
 * @property data Optional map of key-value pairs for event data
 */
data class AppEvent(
    val action: String,
    val data: Map<String, String?> = emptyMap()
) {
    /**
     * Gets a string extra from the event data.
     *
     * @param key The key to look up
     * @return The value associated with the key, or null if not found
     */
    fun getStringExtra(key: String): String? = data[key]
}

/**
 * A singleton event bus for broadcasting events across the application.
 *
 * This replaces the deprecated LocalBroadcastManager with a modern,
 * coroutine-based approach using SharedFlow.
 *
 * Usage:
 * ```
 * // Sending an event
 * EventBus.post(AppEvent(ACTION_LISTENER))
 *
 * // Sending an event with data
 * EventBus.post(AppEvent(
 *     action = ACTION_TOGGLE,
 *     data = mapOf(MSG_TYPE to "someType", ENTITY_ID to "someId")
 * ))
 *
 * // Subscribing to events (in a coroutine scope)
 * EventBus.events.collect { event ->
 *     when (event.action) {
 *         ACTION_LISTENER -> handleRefresh()
 *         ACTION_TOGGLE -> handleToggle(event.getStringExtra(MSG_TYPE))
 *     }
 * }
 * ```
 */
object EventBus {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _events = MutableSharedFlow<AppEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )

    /**
     * A SharedFlow of events that can be collected to receive events.
     */
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    /**
     * Posts an event to all subscribers.
     *
     * @param event The event to broadcast
     */
    fun post(event: AppEvent) {
        scope.launch {
            _events.emit(event)
        }
    }

    /**
     * Posts an event with just an action (no data).
     *
     * @param action The action identifier
     */
    fun post(action: String) {
        post(AppEvent(action))
    }

    /**
     * Posts an event with an action and data map.
     *
     * @param action The action identifier
     * @param data Map of key-value pairs for event data
     */
    fun post(action: String, data: Map<String, String?>) {
        post(AppEvent(action, data))
    }
}

