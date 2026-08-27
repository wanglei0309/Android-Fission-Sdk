// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

package com.infineon.secora.wallet.ui.fragment

import androidx.annotation.StringRes
import com.infineon.secora.wallet.R

/**
 * One step of a wearable payment passcode flow.
 *
 * All six steps render the same screen — heading, message, four digit boxes, one action — so they
 * share [WearablePasscodeFragment] and differ only in their copy and in what submitting does.
 *
 * @property titleRes Heading shown above the message.
 * @property messageRes Explanation shown above the digit boxes.
 * @property actionRes Label of the primary button.
 * @property collectsNewPasscode `true` when the entered value becomes the device passcode, so it
 *   has to satisfy the passcode policy before it is accepted.
 */
enum class WearablePasscodeStep(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
    @StringRes val actionRes: Int,
    val collectsNewPasscode: Boolean
) {
    /** UC-01 first screen: choose a passcode. No APDU is sent. */
    SETUP(
        R.string.wearable_passcode_setup_title,
        R.string.wearable_passcode_setup_message,
        R.string.wearable_passcode_continue,
        collectsNewPasscode = true
    ),

    /** UC-01 second screen: re-enter, then provision it on the secure element. */
    CONFIRM_SETUP(
        R.string.wearable_passcode_confirm_title,
        R.string.wearable_passcode_confirm_message,
        R.string.wearable_passcode_confirm_action,
        collectsNewPasscode = false
    ),

    /** UC-02: verify the passcode to unlock payments. */
    VERIFY(
        R.string.wearable_passcode_verify_title,
        R.string.wearable_passcode_verify_message,
        R.string.wearable_passcode_verify_action,
        collectsNewPasscode = false
    ),

    /** UC-07 first screen: the passcode currently provisioned. */
    CHANGE_CURRENT(
        R.string.wearable_passcode_change_current_title,
        R.string.wearable_passcode_change_current_message,
        R.string.wearable_passcode_continue,
        collectsNewPasscode = false
    ),

    /** UC-07 second screen: choose the replacement. */
    CHANGE_NEW(
        R.string.wearable_passcode_change_new_title,
        R.string.wearable_passcode_change_new_message,
        R.string.wearable_passcode_continue,
        collectsNewPasscode = true
    ),

    /** UC-07 third screen: re-enter the replacement, then send CHANGE PASSCODE. */
    CHANGE_CONFIRM(
        R.string.wearable_passcode_change_confirm_title,
        R.string.wearable_passcode_change_confirm_message,
        R.string.wearable_passcode_change_action,
        collectsNewPasscode = false
    );

    /**
     * `true` when this step talks to the secure element rather than just advancing the flow.
     */
    val sendsCommand: Boolean
        get() = this == CONFIRM_SETUP || this == VERIFY || this == CHANGE_CONFIRM
}
