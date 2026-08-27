// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT
package com.infineon.secora.wallet.domain.walletsdk

import com.infineon.secora.wallet.client.data.models.GetPendingResponse
import com.infineon.secora.wallet.client.data.models.common.FetchAssetResponseBody
import com.infineon.secora.wallet.client.data.models.common.PaymentDeviceResponseBody
import com.infineon.secora.wallet.client.data.models.common.UpdateTransactionNotificationResponse
import com.infineon.secora.wallet.client.data.models.common.TransactionHistoryResponse
import com.infineon.secora.wallet.utils.CommonResponse

/**
 * Validates the payment device response structure to ensure required fields are not empty.
 *
 * @return True if the response is invalid; false otherwise.
 */
fun PaymentDeviceResponseBody.isInvalid(): Boolean {
    return statusMessage.isNullOrEmpty() || (paymentDeviceLists.isNotEmpty() && paymentDeviceLists.any {
        it.seId.isNullOrEmpty() || it.deviceModel.isNullOrEmpty() || it.paymentAppInstanceId.isNullOrEmpty() || it.deviceName.isNullOrEmpty()
    })
}

/**
 * Checks if the response from fetchAsset is invalid or incomplete.
 *
 * @return True if the response is invalid, false otherwise.
 */
fun FetchAssetResponseBody.isInvalid(): Boolean {
    return statusMessage.isNullOrEmpty() || mediaContents.isEmpty() || mediaContents.firstOrNull()?.data.isNullOrEmpty()
}

/**
 * Checks whether the response is invalid based on status message or delete script list.
 *
 * A response is considered invalid if the [statusMessage] is null or empty,
 * or if the [deleteScriptList] is empty.
 *
 * @return `true` if the status message is null/empty or delete script list is empty,
 *         otherwise `false`.
 */
fun GetPendingResponse.isInvalidStatusMessageOrEmptyScript(): Boolean {
    return statusMessage.isNullOrEmpty() || deleteScriptList.isEmpty()
}

/**
 * Validates whether the SDK response is non-null and successful.
 *
 * @param response The SDK transaction history response.
 * @return True if valid; False otherwise.
 */
fun TransactionHistoryResponse.isValidResponse(): Boolean {
    return !statusMessage.isNullOrEmpty() && statusMessage == CommonResponse.SUCCESS.response
}
/**
 * Validates whether the SDK response is non-null and successful.
 *
 * @return True if valid; False otherwise.
 */
fun UpdateTransactionNotificationResponse.isValidResponse(): Boolean {
    return !statusMessage.isNullOrEmpty() && statusMessage == CommonResponse.SUCCESS.response
}

