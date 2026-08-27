package com.infineon.secora.wallet.models

import android.graphics.Bitmap

data class PaymentDeviceList(
    var seId: String? = null,
    var paymentAppInstanceId: String? = null,
    var deviceModel: String? = null,
    var walletAppInstanceId: String? = null,
    var deviceName: String? = null,
    var status: String? = null,
    var wearableDeviceModelId: String? = null,
    var data: Bitmap? = null,
    var isDeviceAccessible: Boolean = true
)
