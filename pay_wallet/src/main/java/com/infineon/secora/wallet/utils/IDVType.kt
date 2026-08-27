package com.infineon.secora.wallet.utils

enum class IDVType(val idvType: String) {

    /**
     * Call generateOTP
     * Used when the user opts for OTP through SMS for verification.
     */
    OTP_SMS("OTP_SMS"),

    /**
     * Call generateOTP
     * Used when the user opts for OTP through email for verification.
     */
    OTP_EMAIL("OTP_EMAIL"),

    /**
     * Used when the user opts for OTP through online banking for verification.
     */
    OTP_ONLINE_BANKING("OTP_ONLINE_BANKING"),

    /**
     * Call generateOTP
     * Used when the user opts for OTP through cardholder number.
     */
    TEXT_TO_CARDHOLDER_NUMBER("TEXT_TO_CARDHOLDER_NUMBER"),

    /**
     * Call generateOTP
     * Used when the user opts for OTP through cardholder address.
     */
    EMAIL_TO_CARDHOLDER_ADDRESS("EMAIL_TO_CARDHOLDER_ADDRESS"),

    /**
     * Call generateOTP
     * Used when the user opts for OTP through outbound call.
     */
    OUTBOUND_CALL("OUTBOUND_CALL"),

    /**
     * Call generateOTP
     * Used when the user opts for OTP through mobile phone number.
     */
    MASKED_MOBILE_PHONE_NUMBER("MASKED_MOBILE_PHONE_NUMBER"),

    /**
     * Launch Browser
     * Used when the user opts for emv 3ds verification.
     */
    EMV_3DS("EMV_3DS"),

    /**
     * Launch Browser
     * Used when user want to visit customer website.
     */
    CARDHOLDER_TO_VISIT_WEBSITE("CARDHOLDER_TO_VISIT_WEBSITE"),

    /**
     * Dial number
     * Used when the user opts for customer service verification.
     */
    CUSTOMER_SERVICE("CUSTOMER_SERVICE"),

    /**
     *
     * Dial number
     * Used when the user opts for cardholder to call automated services.
     */
    CARDHOLDER_TO_CALL_AUTOMATED_NUMBER("CARDHOLDER_TO_CALL_AUTOMATED_NUMBER"),

    /**
     * Dial number
     * Used when the user opts for cardholder to call manned services.
     */
    CARDHOLDER_TO_CALL_MANNED_NUMBER("CARDHOLDER_TO_CALL_MANNED_NUMBER"),

    /**
     * Launch intent
     * Used when the user opts for app-to-app verification.
     */
    APP_TO_APP("APP_TO_APP"),

    /**
     * Launch intent
     * Used when the user opts for cardholder to use issuer mobile app.
     */
    CARDHOLDER_TO_USE_ISSUER_MOBILE_APP("CARDHOLDER_TO_USE_ISSUER_MOBILE_APP"),
}