// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: PreferenceKey.kt is the key mapping used to store and fetch data in SharedPreference
 */
package com.infineon.secora.wallet.data.local.preference

import com.infineon.secora.wallet.utils.constants.Constants.DEFAULT_CARD_TOKEN_REF

object PreferenceKey {

    ////////////////////////// Static keys //////////////////////////
    const val JWT_TOKEN = "JWT_TOKEN"
    const val FCM_TOKEN = "FCM_TOKEN"
    const val IS_COGNITO_TOKEN_FETCHED = "IS_COGNITO_TOKEN_FETCHED"
    const val REFRESH_TOKEN = "REFRESH_TOKEN"
    const val EMAIL_ID = "EMAIL_ID"
    const val USER_ID = "USER_ID"
    const val WALLET_ID = "WALLET_ID"
    const val WALLET_PIN = "WALLET_PIN"
    const val PNO_TYPE = "PNO_TYPE"
    const val USER_NAME = "USER_NAME"
    const val PAN_UNIQUE_REFERENCE = "PAN_UNIQUE_REFERENCE"
    const val PROFILE_IMAGE = "PROFILE_IMAGE"
    const val IS_GOOGLE_LOG_IN = "IS_GOOGLE_LOG_IN"
    const val SPSD_APPLET_INSTANCE_AID = "SPSD_APPLET_INSTANCE_AID"
    const val CARD_AID = "CARD_AID"
    const val AID_CARD_TYPE = "AID_CARD_TYPE"
    const val TRANSACTION_NOTIFICATION_STATE = "TRANSACTION_NOTIFICATION_STATE"
    const val WEARABLE_MODEL_ID = "WEARABLE_MODEL_ID"
    const val CPLC_OEM_ID = "CPLC_OEM_ID"
    const val CPLC_LAST_FETCHED_OEM_ID = "CPLC_LAST_FETCHED_OEM_ID"
    const val CPLC_SE_TYPE_GROUP = "CPLC_SE_TYPE_GROUP"
    const val DIGI_REFERENCE_NUMBER_CANCEL = "DIGI_REFERENCE_NUMBER_CANCEL"
    const val CARD_DECISION = "CARD_DECISION"
    const val PAYMENT_APP_INSTANCE_ID = "PAYMENT_APP_INSTANCE_ID"
    const val DEVICE_SE_ID = "DEVICE_SE_ID"
    const val DEVICE_NAME = "DEVICE_NAME"
    const val DEVICE_IMAGE = "DEVICE_IMAGE"
    const val SELECTED_DEVICE_ADDRESS = "SELECTED_DEVICE_ADDRESS"
    const val INSTALL_SCRIPT = "INSTALL_SCRIPT"
    const val PAIRED_SE_IDS = "PAIRED_SE_IDS"
    const val DIGITIZATION_REFERENCE_NUMBER = "DIGITIZATION_REFERENCE_NUMBER"
    const val TERMS_DATA = "TERMS_DATA"
    const val DEFAULT_CARD_ID = "DEFAULT_CARD_ID"
    const val OTP_EXPIRE_TIME = "OTP_EXPIRE_TIME"
    const val LOGIN_DATE = "LOGIN_DATE"
    const val BACK_PRESSED_FLAG = "BACK_PRESSED_FLAG"
    /** 从 Fission 宿主 demo 跳入开卡流程时为 true，退出钱包页后清除。 */
    const val HOST_LAUNCH_ACTIVE = "HOST_LAUNCH_ACTIVE"
    const val HAS_REQUESTED_NEARBY_DEVICES_PERMISSION = "HAS_REQUESTED_NEARBY_DEVICES_PERMISSION"
    const val THREE_DES_AUTH_TIME = "THREE_DES_AUTH_TIME"

    ////////////////////////// Dynamic keys //////////////////////////

    fun bleAddressKey(seId: String) = "BLE_ADDRESS_$seId"

    fun paymentAppSeIdKey(paymentAppInstanceId: String) = "SE_ID_FOR_PAYMENT_APP_$paymentAppInstanceId"

    fun deviceKey(paymentAppInstanceId: String) = "${DEFAULT_CARD_TOKEN_REF}_$paymentAppInstanceId"

    fun spsdAppletInstanceAidKey(digitizationReferenceNumber: String) =
        "${digitizationReferenceNumber}_${SPSD_APPLET_INSTANCE_AID}"

    fun cardAidKey(digitizationReferenceNumber: String) =
        "${digitizationReferenceNumber}_$CARD_AID"

    fun aidCardTypeKey(digitizationReferenceNumber: String) = "${digitizationReferenceNumber}_${AID_CARD_TYPE}"
    fun updateTransactionNotificationKey(digitizationReferenceNumber: String) = "${digitizationReferenceNumber}_${TRANSACTION_NOTIFICATION_STATE}"
}
