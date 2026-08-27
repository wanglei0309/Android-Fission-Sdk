// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: PendingDeleteTaskResponseHelper.kt interprets get-pending-task API responses for delete flows
 * across device detach, FCM card deleted, and manual card deletion screens.
 **/
package com.infineon.secora.wallet.utils.helper

import com.infineon.secora.wallet.client.data.models.GetPendingResponse
import com.infineon.secora.wallet.utils.constants.Constants

object PendingDeleteTaskResponseHelper {

    /**
     * Returns true when the backend reports no pending delete task for the request.
     *
     * @param response Get-pending-task response from the wallet SDK.
     */
    fun isNoPendingDeleteTask(response: GetPendingResponse): Boolean {
        if (response.statusCode == Constants.GET_DEVICE_PENDING_TASK_EMPTY_CODE) return true
        return response.statusMessage?.contains("No pending task", ignoreCase = true) == true
    }

    /**
     * Returns true when an error or status message indicates no pending delete task.
     *
     * @param message Status or error message from get-pending-task or script execution.
     */
    fun isNoPendingDeleteTaskMessage(message: String?): Boolean {
        if (message.isNullOrBlank()) return false
        return message.contains("No pending task", ignoreCase = true)
    }
}
