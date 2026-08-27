// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: Constants.kt is a centralized object that stores all fixed values and configuration keys used across the application
 * such as network details, OTP and PIN settings, intent keys, UI flags, and feature toggles like biometric authentication.
 **/
package com.infineon.secora.wallet.utils.constants

/**
 * Object that holds constant configuration values used throughout the application,
 * especially for networking or connection purposes.
 */
object Constants {

    const val PIN_LENGTH: Int = 4
    const val PIN_DELAY: Long = 1000
    const val FIRST_ATTEMPT = "first attempt"
    const val RETRY_ATTEMPT = "retry attempt"
    const val COLOR_WHITE = "#FFFFFF"
    const val COLOR_LOGO_BG = "#7393B3"
    const val DEFAULT_CARD_CHANGE = "DEFAULT_CARD_CHANGED"
    const val FAILED_TO_PARSE_GOOGLE_CREDENTIALS = "Failed to parse Google credentials"
    const val CANCEL = "cancel"
    const val NO_CREDENTIAL = "no credential"
    const val INTERRUPTED = "interrupted"
    const val INVALID = "invalid"
    const val SIGN_IN_FAILED = "Sign-in failed: "
    const val SIGN_IN_CANCELLED = "Sign-in cancelled"
    const val SIGN_IN_INTERRUPTED = "Sign-in interrupted"
    const val INVALID_CREDENTIAL_TYPE = "Invalid credential type"
    const val ACTION_FORCE_REFRESH_TXN = "ACTION_FORCE_REFRESH_TXN"
    const val PPSE_VISA = "ppse_visa"
    const val TOKEN_IS_ALREADY_IN_DELETED_STATE = "Token is already in Deleted state."
    const val NO_PENDING_TASK = "No pending task"
    const val SCRIPT_DATA_NULL_OR_EMPTY = "scriptData is null or empty"
    const val UNABLE_TO_DELETE_MESSAGE = "Unable to delete, please clear the memory."
    const val DEFAULT_COLOR = "#FFFFFF"
    const val MASK_CHAR = '*'
    const val AVATAR_BLUE_COLOR = "#0A8276"
    const val SUCCESS_MESSAGE = "SUCCESS"
    const val NFC_DEVICE_MODEL = "SECORA Connect X Kit NFC"
    const val GIVEN_DEVICE_MODEL_IS_NOT_PRESENT_OR_DO_NOT_SUPPORT =
        "Given device model is not present or do not support"
    const val ISO_UTC_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"
    const val DEVICE_TIME_PATTERN_24H = "HH:mm"
    const val UTC_TIMEZONE = "UTC"
    const val CPLC: String = "CPLC"
    const val SCRIPT: String = "SCRIPT"
    const val DELETE_SCRIPT: String = "DELETE_SCRIPT"
    const val DELETE_SCRIPT_CLEAR_DEFAULT: String = "DELETE_SCRIPT_CLEAR_DEFAULT"
    const val PPSE: String = "PPSE"
    const val INTERNAL_ERROR_CARD_DELETED: String =
        "Card deleted on device, but an internal error occurred while updating app state"
    const val UNABLE_TO_DELETE: String = "Unable to delete please clear the memory."
    const val CONNECTION_TIMED_OUT: String = "BLE connection timed out (device did not respond in time)"
    const val CONNECTION_FAILED: String = "BLE connection failed"

    ///////////////////////////////////////////////////////////

    const val ACTIVE: String = "1"
    const val INACTIVE: String = "0"
    const val SUCCESS: String = "SUCCESS"
    const val FAILED: String = "FAILED"
    const val SUCCESS_VALUE: String = "0"
    const val PNO_VTS: String = "VTS"
    const val PNO_MDES: String = "MDES"
    const val PNO_AMEX: String = "AMEX"
    const val ACTIVE_STATUS = "ACTIVE"
    const val SUSPEND_STATUS = "SUSPENDED"
    const val DELETED_STATUS = "DELETED"
    const val PENDING_STATUS = "PENDING"
    const val DEFAULT_CARD_TOKEN_REF = "default_card_token_ref"
    const val PENDING = "Pending"
    const val REQUIRE_ADDITIONAL_AUTHENTICATION = "REQUIRE_ADDITIONAL_AUTHENTICATION"
    const val SECORA = "SECORA"
    const val SLASH = "/"

    ////////////////////////////////////////////////////////////////////////////////

    const val TERMS_CONDITION_ASSET_ID = "a0b72f0e60284f4f9809c9375557a77c"
    const val ACTION_LISTENER = "com.ifx.se.ACTION_REFRESH_FRAGMENT"
    const val ACTION_NAVIGATE_LISTENER = "com.ifx.se.ACTION_NAVIGATE_FRAGMENT"
    const val ACTION_FORCE_LOGOUT = "com.ifx.se.ACTION_FORCE_LOGOUT"
    const val DEVICE_LOGOUT = "DEVICE_LOGOUT"
    const val ACTION_TOGGLE = "com.ifx.se.ACTION_TOGGLE"
    const val ACTION_REGISTRATION_CODE = "com.ifx.se.REGISTRATION_CODE"

    const val REGISTRATION_CODE2 = "registrationCode2"
    const val TOKEN_UNIQUE_REFERENCE_NO = "tokenUniqueReference"
    const val ACTIVATE_CARD = "ACTIVATE_CARD"
    const val SUSPEND_CARD = "SUSPEND_CARD"
    const val DELETED_CARD = "DELETED_CARD"
    const val GET_DEVICE_PENDING_TASK_EMPTY_CODE = "TOKEN_CONNECTOR_SERVICE_GET_DEVICE_PENDING_TASK_0001"
    const val CARD_PROVISION = "CARD_PROVISION"
    const val BIOMETRIC_NOT_AVAILABLE = "Biometric authentication not available"
    const val ACTION_CARD = "com.ifx.se.ACTION_CARD_REFRESH"
    const val BLUETOOTH_NOT_CONNECTED = "Bluetooth not connected"
    const val BLUETOOTH_PERMISSION_DENIED = "Bluetooth permission denied"
    const val BLUETOOTH_PERMISSION_REQUIRE = "Bluetooth permissions required"
    const val BLUETOOTH_PERMISSION_MISSING = "Bluetooth permissions missing"
    const val UNKNOWN_DEVICE = "Unknown Device"
    const val BLUETOOTH_PERMISSION = "Bluetooth permissions required"
    const val BLUETOOTH_SCAN_PERMISSION_REQUIRED = "Bluetooth scan permission required"
    const val DEVICE_LINKED_WITH_ANOTHER_USER_MESSAGE = "Device already linked to another user"
    const val DEVICE_NOT_BELONG_TO_OEM_MESSAGE = "This device does not belong to"
    const val CONTENT_TYPE_DIGITAL_CARD_ART = "digitalCardArt"
    const val ONBOARDING_FETCH_INSTALL_SCRIPT_0002 = "TR_ONBOARDING_FETCH_INSTALL_SCRIPT_0002"
    const val REQUEST_KEY = "requestKey"
    const val DEVICE_STOLEN = "DEVICE_STOLEN"
    const val ACTION_OPEN_DEVICE_LIST = "ACTION_OPEN_DEVICE_LIST"
    const val ACTION_DEVICE_DETACH_COMPLETED = "ACTION_DEVICE_DETACH_COMPLETED"
    const val ACTION_DEVICE_STATUS_UPDATE = "ACTION_DEVICE_STATUS_UPDATE"
    const val TIMEOUT_EXCEPTION = "TimeoutException"
    /** Per-APDU wall-clock budget (any command in the chain can hang, not a specific step). */
    const val BLE_PER_APDU_TIMEOUT_SECONDS = 15L
    /**
     * Extra seconds per script APDU for ISO 7816 GET RESPONSE chaining when the SE returns 61xx.
     */
    const val BLE_GET_RESPONSE_CHAIN_BUDGET_PER_APDU_SECONDS = 45L
    /** Extra headroom after the last APDU (connect/teardown). */
    const val BLE_SCRIPT_TIMEOUT_BUFFER_SECONDS = 30L
    const val BLE_SCRIPT_MIN_TIMEOUT_SECONDS = 45L
    const val BLE_SCRIPT_MAX_TIMEOUT_SECONDS = 180L
    /** Fallback when script JSON cannot be parsed to count APDUs. */
    const val BLE_SCRIPT_OPERATION_TIMEOUT_SECONDS = 120L
    const val BLE_SCRIPT_ALREADY_RUNNING = "A secure element operation is already in progress. Please wait."
    const val TAG_85 = "85"
    const val TAG_86 = "86"
    const val DEFAULT_DEVICE_NAME = "device"
    const val EMAIL_TO_CARDHOLDER_ADDRESS: String = "EMAIL_TO_CARDHOLDER_ADDRESS"
    const val TEXT_TO_CARDHOLDER_NUMBER: String = "TEXT_TO_CARDHOLDER_NUMBER"
    const val IS_MULTI_COMPANION_ENABLED: Boolean = true
    const val DEVICE_RESUME_UPDATE: String = "DEVICE_RESUME_UPDATE"
    const val DEVICE_SUSPEND_UPDATE: String = "DEVICE_SUSPEND_UPDATE"
}
