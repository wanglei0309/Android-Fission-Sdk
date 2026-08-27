// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: WalletSdkResult is a data class which is provided from WalletRepository to UI layer
 **/
package com.infineon.secora.wallet.domain.walletsdk

data class WalletSdkResult<T>(
    var isLoading: Boolean,
    var isSuccess: Boolean,
    var statusMessage: String = "", // Will be filled up in case of success callback
    var errorMessage: String = "", // // Will be filled up in case of error callback or empty response in success callback
    var response: T? = null // Will be filled up with the response received in success callback.
)
