// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: CommonResponse.kt is an enum class that defines standard response statuses used across the app
 * such as success, expired access token, and missing script.
 * Each enum constant holds a corresponding string value for consistent API response handling.
 **/
package com.infineon.secora.wallet.utils

/**
 * Enum class representing common responses used throughout the application,
 * typically for identifying API or operation status messages in a standardized way.
 *
 * @property response The string representation of the response, which can be used for
 * display or comparison against actual API responses.
 */
enum class CommonResponse(val response: String) {
    SUCCESS("SUCCESS"),
    USER_SESSION_EXPIRED("USER_SESSION_EXPIRED"),
    DSEMS_SCRIPT_NOT_FOUND("DSEMS Script not found"),
    TOKEN_CONNECTOR_SERVICE_WEARABLE_0001("TOKEN_CONNECTOR_SERVICE_WEARABLE_0001");

    /**
     * Returns the string value of the response.
     *
     * @return The value of the [response] property.
     */
    override fun toString(): String {
        return response
    }
}