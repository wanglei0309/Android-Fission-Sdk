// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: WalletRepository.kt calls and handles all apis of SecoraWalletSdk.
 **/
package com.infineon.secora.wallet.domain.walletsdk

import android.content.Context
import com.infineon.secora.wallet.MyApplication
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.client.data.models.AcknowledgeResponse
import com.infineon.secora.wallet.client.data.models.DefaultCardResponse
import com.infineon.secora.wallet.client.data.models.GetPendingResponse
import com.infineon.secora.wallet.client.data.models.LogoutResponse
import com.infineon.secora.wallet.client.data.models.capturecard.CapturedCardDetail
import com.infineon.secora.wallet.client.data.models.capturecard.ScreenConfiguration
import com.infineon.secora.wallet.client.data.models.card.common.CardDetails
import com.infineon.secora.wallet.client.data.models.common.AuthenticationMethod
import com.infineon.secora.wallet.client.data.models.common.CardList
import com.infineon.secora.wallet.client.data.models.common.CheckEligibilityResponseBody
import com.infineon.secora.wallet.client.data.models.common.DeleteDeviceResponse
import com.infineon.secora.wallet.client.data.models.common.DigitizeResponse
import com.infineon.secora.wallet.client.data.models.common.DigitizeResponseBody
import com.infineon.secora.wallet.client.data.models.common.GetActivationResponse
import com.infineon.secora.wallet.client.data.models.common.FetchAssetResponseBody
import com.infineon.secora.wallet.client.data.models.common.GetProvisionCardResponse
import com.infineon.secora.wallet.client.data.models.common.NotifyProvisionResponse
import com.infineon.secora.wallet.client.data.models.common.NotifyProvisionStatusResponse
import com.infineon.secora.wallet.client.data.models.common.PaymentDeviceResponseBody
import com.infineon.secora.wallet.client.data.models.common.UpdateTransactionNotificationResponse
import com.infineon.secora.wallet.client.data.models.common.TransactionHistoryResponse
import com.infineon.secora.wallet.client.data.models.common.UpdateCardStatusResponse
import com.infineon.secora.wallet.client.data.models.common.UserResponseBody
import com.infineon.secora.wallet.client.data.models.common.VerifyActivationResponse
import com.infineon.secora.wallet.client.data.models.devices.RegisterPaymentDeviceResponse
import com.infineon.secora.wallet.client.data.models.prepse.PrepSeResponseBody
import com.infineon.secora.wallet.client.data.models.provision.ApduResponsesItem
import com.infineon.secora.wallet.client.operations.common.logger.Logger
import com.infineon.secora.wallet.client.operations.middleware.callbacks.UiCallback
import com.infineon.secora.wallet.client.operations.middleware.interfaces.CheckEligibilityParams
import com.infineon.secora.wallet.client.operations.middleware.interfaces.PrepSeScriptsParams
import com.infineon.secora.wallet.client.operations.middleware.interfaces.UpdateCardStatusParams
import com.infineon.secora.wallet.client.operations.middleware.service.SecoraWalletSDK
import com.infineon.secora.wallet.client.util.AppDispatchers
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wearable.protocolapi.IAsyncProtocol
import com.infineon.secora.wallet.domain.walletsdk.RepositoryHelper.getErrorMessage
import com.infineon.secora.wallet.domain.walletsdk.RepositoryHelper.getErrorResponse
import com.infineon.secora.wallet.domain.walletsdk.RepositoryHelper.getSuccessResponse
import com.infineon.secora.wallet.domain.walletsdk.RepositoryHelper.storeDigitizeResponseData
import com.infineon.secora.wallet.domain.walletsdk.RepositoryHelper.storeGoogleUserData
import com.infineon.secora.wallet.domain.walletsdk.RepositoryHelper.storePrepSeData
import com.infineon.secora.wallet.ui.fragment.BaseFragment
import com.infineon.secora.wallet.utils.constants.Constants.SUCCESS_MESSAGE
import com.infineon.secora.wallet.utils.helper.ConfiguredWalletIdentity
import com.infineon.secora.wallet.utils.hostedui.HostedUILanguage
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object WalletRepository {

    private val logger = Logger.getNewLogger(WalletRepository::class.java.name.toString())

    private val walletSdk by lazy {
        SecoraWalletSDK.getInstance()
    }

    /**
     * Initializes the SDK with the application context.
     *
     * Network base URLs come from `assets/config.properties` via the SDK’s
     * [com.infineon.secora.wallet.client.util.PropertiesLoader] after properties are loaded.
     */
    fun initializeWalletSdk(context: Context) {
        walletSdk?.initialize(context.applicationContext)
    }

    /**
     * Returns card row from the app database, or fetches the provision list via the SDK (no SDK DB access),
     * persists it with [StorageRepository.saveProvisionedCardsToLocalDb], and re-reads — for notification / deep-link when Room was cleared.
     */
    suspend fun getLocalCardDetailsOrRefreshFromProvisionApi(
        context: Context,
        digitizationReferenceNumber: String
    ): CardDetails = withContext(AppDispatchers.IO) {
        StorageRepository.getCardByDigitizationReferenceNumber(context, digitizationReferenceNumber)
            ?.let { return@withContext it }

        val paymentAppInstanceId = StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID)
        if (paymentAppInstanceId.isBlank()) return@withContext CardDetails()

        val result = fetchProvisionedCards(context, paymentAppInstanceId)
        if (result.isSuccess && result.response != null) {
            StorageRepository.saveProvisionedCardsToLocalDb(
                context,
                paymentAppInstanceId,
                result.response
            )
        }
        StorageRepository.getCardByDigitizationReferenceNumber(context, digitizationReferenceNumber) ?: CardDetails()
    }

    /**
     * Performs user login and returns the result as [WalletSdkResult].
     *
     * Suspends until the SDK callback responds. On success, stores user data if
     * status indicates success; otherwise returns an error result.
     *
     * @param email User email used for storing data on successful login.
     * @param idToken Google / Cognito ID token for the user-service login Bearer header (SDK stores only for that request chain).
     * @param fcmToken FCM registration token required to send notifications from backend server to the application.
     * @return Success with [UserResponseBody] or error with message.
     */
    suspend fun userLogin(
        email: String = "",
        idToken: String? = null,
        fcmToken : String
    ): WalletSdkResult<UserResponseBody> =
        suspendCancellableCoroutine { continuation ->
            walletSdk?.userLogin(idToken = idToken, fcmToken = fcmToken, callback = object : UiCallback {
                override fun <T : Any?> onSuccess(response: T) {
                    super.onSuccess(response)
                    if (response is UserResponseBody) {
                        storeGoogleUserData(email, response)
                        continuation.resume(getSuccessResponse(response.statusMessage.toString(), response))
                    } else {
                        continuation.resume(getErrorResponse(MyApplication.appContext.getString(R.string.no_response_data_received)))
                    }
                }

                override fun onError(errorNbr: Int, errorMsg: String) {
                    continuation.resume(getErrorResponse(errorMsg))
                }
            })
        }

    /**
     * Performs user logout and returns the result as [WalletSdkResult].
     *
     * Suspends until the SDK callback responds. On success, stores user data if
     * status indicates success; otherwise returns an error result.
     *
     * @return Success with [LogoutResponse] or error with message.
     */
    suspend fun userLogout(): WalletSdkResult<LogoutResponse> =
        suspendCancellableCoroutine { continuation ->
            walletSdk?.userLogout(callback = object : UiCallback {
                override fun <T : Any?> onSuccess(response: T) {
                    super.onSuccess(response)
                    if (response is LogoutResponse) {
                        continuation.resume(getSuccessResponse(response.statusMessage.toString(), response))
                    } else {
                        continuation.resume(getErrorResponse(MyApplication.appContext.getString(R.string.no_response_data_received)))
                    }
                }

                override fun onError(errorNbr: Int, errorMsg: String) {
                    continuation.resume(getErrorResponse(errorMsg))
                }
            })
        }

    /**
     * Registers a payment device and returns the result as [WalletSdkResult].
     *
     * Suspends until the SDK callback responds. Returns success when the device
     * is registered successfully, otherwise returns an error result.
     *
     * @param context Context used for SDK operation and error messages.
     * @param device Device identifier to be registered.
     * @param seId Secure element ID associated with the device.
     * @return Success with no data or error with message.
     */
    suspend fun registerPaymentDevice(
        context: Context,
        device: String,
        seId: String,
        wearableModelIdHex: String = ""
    ): WalletSdkResult<Unit> =
        suspendCancellableCoroutine { continuation ->
            ConfiguredWalletIdentity.seedHardcodedIdentity(context)
            val wearableForRegister = wearableModelIdHex.ifBlank {
                ConfiguredWalletIdentity.WEARABLE_ID
            }

            walletSdk?.registerPaymentDevice(
                context = context,
                device = device,
                seId = seId,
                wearableModelIdHex = wearableForRegister,
                callback = object : UiCallback {
                    override fun <T : Any?> onSuccess(ret: T) {
                        super.onSuccess(ret)
                        if (ret is RegisterPaymentDeviceResponse) {
                            continuation.resume(getSuccessResponse(ret.statusMessage.toString()))
                        } else {
                            continuation.resume(getErrorResponse(context.getString(R.string.unable_register) + device))
                        }
                    }

                    override fun onError(errorNbr: Int, errorMsg: String) {
                        continuation.resume(getErrorResponse(errorMsg))
                    }
                })
        }

    /**
     * Fetches registered payment devices and returns the result as [WalletSdkResult].
     *
     * Suspends until the SDK callback responds. Returns success with
     * [PaymentDeviceResponseBody] when valid, otherwise returns an error result.
     *
     * @param context Context used for SDK operation and error messages.
     * @return Success with device data or error with message.
     */
    suspend fun fetchPaymentDevices(context: Context): WalletSdkResult<PaymentDeviceResponseBody> =
        suspendCancellableCoroutine { continuation ->
            walletSdk?.fetchPaymentDevices(
                context = context,
                callback = object : UiCallback {
                    override fun <T : Any?> onSuccess(ret: T) {
                        super.onSuccess(ret)
                        val response = ret as? PaymentDeviceResponseBody
                        if (response == null) {
                            continuation.resume(getErrorResponse(context.getString(R.string.something_went_wrong)))
                        } else if (response.isInvalid()) {
                            continuation.resume(getErrorResponse(response.statusMessage.toString()))
                        } else {
                            continuation.resume(getSuccessResponse(response.statusMessage.toString(), response))
                        }
                    }

                    override fun onError(errorNbr: Int, errorMsg: String) {
                        continuation.resume(getErrorResponse(errorMsg))
                    }
                })

        }

    /**
     * Removes a payment device using the wallet SDK.
     * Suspends until the SDK responds and converts the callback result into a [WalletSdkResult].
     *
     * @param context Android context required by the SDK.
     * @param paymentAppInstanceId Unique payment app instance ID.
     * @param seId Secure Element ID of the device.
     * @param connected Whether the device is currently connected.
     *
     * @return Success with [DeleteDeviceResponse] if removal succeeds,
     * or error result if the operation fails or response is invalid.
     */
    suspend fun removePaymentDevice(
        context: Context,
        paymentAppInstanceId: String,
        seId: String,
        connected: Boolean,
        currentSequenceCounter: String
    ): WalletSdkResult<DeleteDeviceResponse> = suspendCancellableCoroutine { continuation ->
        walletSdk?.removePaymentDevice(
            context = context,
            paymentAppInstanceId = paymentAppInstanceId,
            seId = seId,
            connected = connected,
            currentSequenceCounter = currentSequenceCounter,
            uiCallback = object : UiCallback {
                override fun <T : Any?> onSuccess(response: T) {
                    if (response is DeleteDeviceResponse) {
                        continuation.resume(getSuccessResponse(response.statusMessage.toString(), response))
                    } else {
                        continuation.resume(getErrorResponse(context.getString(R.string.no_response_data_received)))
                    }
                }

                override fun <T : Any?> onError(response: T) {
                    val errorMessage = getErrorMessage(response)
                    continuation.resume(getErrorResponse(errorMessage))
                }
            })
    }

    /**
     * Fetches pending tasks from the wallet SDK.
     *
     * Suspends until the SDK responds and converts the callback result
     * into a [WalletSdkResult]. User and wallet identifiers are retrieved from local storage.
     *
     * @param context Android context required by the SDK.
     * @param seId Secure Element ID of the device.
     * @param digitizationReferenceNumber Optional reference number for filtering tasks.
     *
     * @return Success with [GetPendingResponse] if data is received,
     * or error result if the operation fails or response is invalid.
     */
    suspend fun getPendingTask(
        context: Context,
        seId: String,
        digitizationReferenceNumber: String? = null,
        currentSequenceCounter: String
    ): WalletSdkResult<GetPendingResponse> = suspendCancellableCoroutine { continuation ->

        walletSdk?.getPendingTask(
            context = context,
            seId = seId,
            userId = StorageRepository.readString(PreferenceKey.USER_ID),
            walletAppInstanceId = StorageRepository.readString(PreferenceKey.WALLET_ID),
            digitizationReferenceNumber = digitizationReferenceNumber,
            currentSequenceCounter = currentSequenceCounter,
            uiCallback = object : UiCallback {
                override fun <T : Any?> onSuccess(response: T) {
                    super.onSuccess(response)
                    if (response is GetPendingResponse) {
                        continuation.resume(getSuccessResponse(response.statusMessage.toString(), response))
                    } else {
                        continuation.resume(getErrorResponse(context.getString(R.string.something_went_wrong)))
                    }
                }

                override fun <T : Any?> onError(response: T) {
                    super.onError(response)
                    val errorMessage = getErrorMessage(response)
                    continuation.resume(getErrorResponse(errorMessage))
                }
            })
    }

    /**
     * Retrieves pending task(s) from Wallet SDK for the given SE and user context.
     *
     * @param context context
     * @param seId Secure Element identifier
     * @param digitizationReferenceNumber Optional task reference
     * @param uiCallback Callback for SDK response
     */
    fun getPendingTask(
        context: Context,
        seId: String,
        digitizationReferenceNumber: String? = null,
        currentSequenceCounter: String,
        uiCallback: UiCallback?
    ) {

        walletSdk?.getPendingTask(
            context = context,
            seId = seId,
            userId = StorageRepository.readString(PreferenceKey.USER_ID),
            walletAppInstanceId = StorageRepository.readString(PreferenceKey.WALLET_ID),
            digitizationReferenceNumber = digitizationReferenceNumber,
            currentSequenceCounter = currentSequenceCounter,
            uiCallback = uiCallback
        )
    }

    /**
     * Acknowledges a pending task via the wallet SDK.
     *
     * Suspends until the SDK responds and converts the callback result into a [WalletSdkResult].
     *
     * @param context Android context required by the SDK.
     * @param seId Secure Element ID of the device.
     * @param scriptId Identifier of the script/task to acknowledge.
     * @param digitizeRef Digitization reference number.
     * @param status Status to be sent (defaults to success).
     *
     * @return Success with [UpdateCardStatusResponse] if acknowledgement succeeds,
     * or error result if the operation fails or response is invalid.
     */
    suspend fun acknowledgePendingTask(
        context: Context,
        seId: String,
        scriptId: Int,
        digitizeRef: String,
        status: String = SUCCESS_MESSAGE
    ): WalletSdkResult<AcknowledgeResponse> = suspendCancellableCoroutine { continuation ->
        walletSdk?.acknowledgePendingTask(
            context = context,
            seId = seId,
            scriptId = scriptId,
            digitizeref = digitizeRef,
            status = status,
            uiCallback = object : UiCallback {
                override fun <T : Any?> onSuccess(response: T) {
                    super.onSuccess(response)
                    if (response is AcknowledgeResponse) {
                        continuation.resume(getSuccessResponse(response.statusMessage.toString(), response))
                    } else {
                        continuation.resume(getErrorResponse(context.getString(R.string.something_went_wrong)))
                    }
                }

                override fun <T : Any?> onError(response: T) {
                    super.onError(response)
                    val errorMessage = getErrorMessage(response)
                    continuation.resume(getErrorResponse(errorMessage))
                }
            })
    }

    /**
     * Saves OEM details in the wallet SDK.
     *
     * @param context Android context required by the SDK.
     * @param oemId OEM identifier.
     * @param infineonSalesCodeAndGroup Infineon sales code and group.
     */
    suspend fun saveOEMDetails(
        context: Context,
        oemId: String,
        infineonSalesCodeAndGroup: String
    ) = withContext(AppDispatchers.IO) {
        walletSdk?.saveOEMDetails(
            context = context,
            oemId = oemId,
            infineonSalesCodeAndGroup = infineonSalesCodeAndGroup
        )
    }

    /**
     * Demo 对齐：后台把 config.properties 中的 TITAN OEM / SE_TYPE_GROUP 同步进 wallet SDK。
     */
    suspend fun prefetchHardcodedOemDetailsIfNeeded(context: Context) {
        syncOemDetailsFromPreferences(context)
    }

    /**
     * 发卡 / Unlock 前把 assets 硬编码 OEM 与 SE_TYPE_GROUP 同步进 wallet SDK（与 Infineon demo 一致）。
     */
    suspend fun syncOemDetailsFromPreferences(context: Context) = withContext(AppDispatchers.IO) {
        ConfiguredWalletIdentity.seedHardcodedIdentity(context)
        val seTypeGroup = ConfiguredWalletIdentity.readPersistedSeTypeGroup(context)
        logger.info(
            "syncOemDetailsFromPreferences: oemId=${ConfiguredWalletIdentity.OEM_ID} " +
                "seTypeGroup=$seTypeGroup"
        )
        saveOEMDetails(
            context = context,
            oemId = ConfiguredWalletIdentity.OEM_ID,
            infineonSalesCodeAndGroup = seTypeGroup
        )
    }

    /**
     * @param registrationCode part of mtf registration code.
     * @param digitizeReferenceNumber Digitization reference number.
     */
    suspend fun registrationDetailsToMTF(registrationCode : String, digitizeReferenceNumber : String) = withContext(AppDispatchers.IO) {
        walletSdk?.registrationDetailsToMTF(
            registrationCode = registrationCode,
            digitizeReferenceNumber = digitizeReferenceNumber
        )
    }

    /**
     * Fetches provisioned cards from the wallet SDK.
     *
     * Suspends until the SDK responds and converts the callback result into a [WalletSdkResult].
     *
     * @param context Android context required by the SDK.
     * @param paymentAppInstanceId Unique payment app instance ID.
     *
     * @return Success with [GetProvisionCardResponse] if data is received,
     * or error result if the operation fails or response is invalid.
     */
    suspend fun fetchProvisionedCards(
        context: Context,
        paymentAppInstanceId: String
    ): WalletSdkResult<GetProvisionCardResponse> = suspendCancellableCoroutine { continuation ->
        walletSdk?.fetchProvisionedCards(
            context = context,
            paymentAppInstanceId = paymentAppInstanceId,
            uiCallback = object : UiCallback {
                override fun <T : Any?> onSuccess(ret: T) {
                    if (ret is GetProvisionCardResponse) {
                        continuation.resume(getSuccessResponse(ret.statusMessage.toString(), ret))
                    } else {
                        continuation.resume(getErrorResponse(context.getString(R.string.no_response_data_received)))
                    }
                }

                override fun onError(errorNbr: Int, errorMsg: String) {
                    continuation.resume(getErrorResponse(errorMsg))
                }
            })
    }

    /**
     * Sets the screen configuration for the Wallet SDK based on the selected language.
     *
     * This function switches the execution to the IO dispatcher and applies the
     * corresponding [ScreenConfiguration] retrieved from the provided [HostedUILanguage].
     *
     * @param context The application or activity context required to initialize the configuration.
     * @param language The selected [HostedUILanguage] used to derive the screen configuration.
     *
     */
    suspend fun setScreenConfiguration(context: Context, language: HostedUILanguage) = withContext(AppDispatchers.IO) {
        walletSdk?.setScreenConfiguration(context = context, screenConfiguration = language.getScreenConfiguration())
    }

    /**
     * Captures card details via the wallet SDK.
     *
     * Suspends until the SDK responds and converts the callback result into a [WalletSdkResult].
     *
     * @param context Android context required by the SDK.
     *
     * @return Success with [CapturedCardDetail] if capture succeeds,
     * or error result if the operation fails.
     */
    suspend fun captureCardDetails(context: Context, paymentAppInstanceId: String): WalletSdkResult<CapturedCardDetail> =
        suspendCancellableCoroutine { continuation ->
            walletSdk?.captureCardDetails(context = context, paymentAppInstanceId = paymentAppInstanceId, callback = object : UiCallback {
                override fun <T : Any?> onSuccess(ret: T) {
                    if (ret is CapturedCardDetail) {
                        logger.debug("captureCardDetails onSuccess, CapturedCardDetail : $ret")
                        continuation.resume(getSuccessResponse(statusMessage = "Success", response = ret))
                    } else {
                        continuation.resume(getErrorResponse(context.getString(R.string.something_went_wrong)))
                    }
                }

                override fun onError(errorNbr: Int, errorMsg: String?) {
                    super.onError(errorNbr, errorMsg)
                    logger.error("onError : $errorMsg")
                    continuation.resume(getErrorResponse(errorMsg.toString()))
                }
            })
        }

    /**
     * Prepares the secure element via the wallet SDK.
     * Suspends until the SDK responds and converts the callback result into a [WalletSdkResult].
     *
     * @param context Android context required by the SDK.
     * @param seId Secure Element ID.
     * @param paymentAppInstanceId Payment app instance ID.
     * @param device Device identifier.
     *
     * @return Success with [PrepSeResponseBody] if preparation succeeds,
     * or error result if the operation fails or response is invalid.
     */
    suspend fun prepareSecureElement(
        context: Context,
        seId: String,
        paymentAppInstanceId: String,
        device: String,
        wearableModelIdHex: String = "",
        currentSequenceCounter: String
    ): WalletSdkResult<PrepSeResponseBody> = suspendCancellableCoroutine { continuation ->
        walletSdk?.prepareSecureElement(
            context = context,
            seId = seId,
            paymentAppInstanceId = paymentAppInstanceId,
            device = device,
            wearableModelIdHex = wearableModelIdHex,
            currentSequenceCounter = currentSequenceCounter,
            callback = object : UiCallback {
                override fun <T : Any?> onSuccess(ret: T) {
                    super.onSuccess(ret)
                    if (ret is PrepSeResponseBody) {
                        storePrepSeData(ret)
                        continuation.resume(getSuccessResponse(ret.statusMessage.toString(), ret))
                    } else {
                        continuation.resume(getErrorResponse(context.getString(R.string.no_response_data_received)))
                    }
                }

                override fun <T : Any?> onError(response: T) {
                    super.onError(response)
                    val errorMessage = getErrorMessage(response)
                    continuation.resume(getErrorResponse(errorMessage))
                }
            })
    }

    /**
     * Checks card eligibility via the wallet SDK.
     * Suspends until the SDK responds and converts the callback result into a [WalletSdkResult].
     *
     * @param context Android context required by the SDK.
     * @param paymentAppInstanceId Payment app instance ID.
     * @param digitizationReferenceNumber Digitization reference number.
     * @param tlvA6 TLV A6 value from the INSTALL script execution.
     * @param tag85 tag85 value from the INSTALL script execution.
     * @param tag86 tag86 value from the INSTALL script execution.
     * @param casdPkCertificate CASD public-key certificate for MDES BLE flows, or null when not required.
     *
     * @return Success with [CheckEligibilityResponseBody] if eligible data is received,
     * or error result if the operation fails or response is invalid.
     */
    suspend fun checkEligibility(
        context: Context,
        paymentAppInstanceId: String,
        digitizationReferenceNumber: String,
        tlvA6: String?,
        tag85: String?,
        tag86: String?,
        casdPkCertificate: String? = null
    ): WalletSdkResult<CheckEligibilityResponseBody> = suspendCancellableCoroutine { continuation ->
        walletSdk?.checkEligibility(
            params = CheckEligibilityParams(
                context = context,
                paymentAppInstanceId = paymentAppInstanceId,
                digitizationReferenceNumber = digitizationReferenceNumber,
                tlvA6 = tlvA6,
                tag85 = tag85,
                tag86 = tag86,
                casdPkCertificate = casdPkCertificate,
                callback = object : UiCallback {
                override fun <T : Any?> onSuccess(response: T) {
                    super.onSuccess(response)
                    if (response is CheckEligibilityResponseBody) {
                        continuation.resume(getSuccessResponse(response.statusMessage.toString(), response))
                    } else {
                        continuation.resume(getErrorResponse(context.getString(R.string.no_response_data_received)))
                    }
                }

                override fun <T : Any?> onError(response: T) {
                    super.onError(response)
                    val errorMessage = getErrorMessage(response)
                    continuation.resume(getErrorResponse(errorMessage))
                }
            }
            )
        )
    }

    /**
     * Fetches SE preparation scripts after eligibility verification.
     */
    suspend fun fetchPrepSeScripts(
        context: Context,
        paymentAppInstanceId: String,
        digitizationReferenceNumber: String,
        currentSequenceCounter: String
    ): WalletSdkResult<PrepSeResponseBody> = suspendCancellableCoroutine { continuation ->
        walletSdk?.fetchPrepSeScripts(
            params = PrepSeScriptsParams(
                context = context,
                paymentAppInstanceId = paymentAppInstanceId,
                digitizationReferenceNumber = digitizationReferenceNumber,
                currentSequenceCounter = currentSequenceCounter,
                callback = object : UiCallback {
                    override fun <T : Any?> onSuccess(response: T) {
                        super.onSuccess(response)
                        if (response is PrepSeResponseBody) {
                            storePrepSeData(response)
                            continuation.resume(getSuccessResponse(response.statusMessage.toString(), response))
                        } else {
                            continuation.resume(getErrorResponse(context.getString(R.string.no_response_data_received)))
                        }
                    }

                    override fun <T : Any?> onError(response: T) {
                        super.onError(response)
                        val errorMessage = getErrorMessage(response)
                        continuation.resume(getErrorResponse(errorMessage))
                    }
                }
            )
        )
    }

    /**
     * Fetches asset from the wallet SDK.
     * Suspends until the SDK responds and converts the callback result into a [WalletSdkResult].
     *
     * @param context Android context required by the SDK.
     * @param paymentAppInstanceId Payment app instance ID.
     * @param digitizationReferenceNumber Digitization reference number.
     * @param assetId Asset identifier (defaults to terms & conditions asset).
     * @param isCardImage Indicates whether the requested asset is a card image.
     *
     * @return Success with [FetchAssetResponseBody] if data is received,
     * or error result if the operation fails or response is invalid.
     */
    suspend fun fetchAsset(
        context: Context,
        paymentAppInstanceId: String,
        digitizationReferenceNumber: String,
        assetId: String,
        isCardImage: Boolean = false
    ): WalletSdkResult<FetchAssetResponseBody> = suspendCancellableCoroutine { continuation ->
        walletSdk?.fetchAsset(
            context = context,
            paymentAppInstanceId = paymentAppInstanceId,
            digitizationReferenceNumber = digitizationReferenceNumber,
            assetId = assetId,
            isCardImage = isCardImage,
            uiCallback = object : UiCallback {
                override fun <T : Any?> onSuccess(response: T) {
                    super.onSuccess(response)
                    if (response is FetchAssetResponseBody) {
                        continuation.resume(getSuccessResponse(response.statusMessage.toString(), response))
                    } else {
                        continuation.resume(getErrorResponse(context.getString(R.string.no_response_data_received)))
                    }
                }

                override fun <T : Any?> onError(response: T) {
                    super.onError(response)
                    val errorMessage = getErrorMessage(response)
                    continuation.resume(getErrorResponse(errorMessage))
                }
            }
        )
    }

    /**
     * Digitizes a card via the wallet SDK and persists digitize response data on success.
     *
     * @param context Android context for the SDK and local storage.
     * @param paymentAppInstanceId Payment app instance identifier.
     * @param digitizationReferenceNumber Digitization reference for this card flow.
     * @param clientWalletAccountEmailAddress Wallet account email; defaults to the value stored in app preferences.
     * @param sdScript Combined sdScript hex from INSTALL script sequence 8, or null when not available.
     * @param casdCert CASD public-key certificate TLV as uppercase hex for VTS flows, or null when not required.
     * @return Success with [DigitizeResponseBody] or an error [WalletSdkResult].
     */
    suspend fun digitizeCard(
        context: Context,
        paymentAppInstanceId: String,
        digitizationReferenceNumber: String,
        clientWalletAccountEmailAddress: String? = null,
        sdScript: String? = null,
        casdCert: String? = null
    ): WalletSdkResult<DigitizeResponseBody> = suspendCancellableCoroutine { continuation ->
        val email = clientWalletAccountEmailAddress?.takeIf { it.isNotBlank() }
            ?: StorageRepository.readString(PreferenceKey.EMAIL_ID)
        logger.debug("digitizeCard: clientWalletAccountEmailAddress=$email")
        walletSdk?.digitizeCard(
            context = context,
            paymentAppInstanceId = paymentAppInstanceId,
            digitizationReferenceNumber = digitizationReferenceNumber,
            clientWalletAccountEmailAddress = email,
            sdScript = sdScript,
            casdCert = casdCert,
            callback = object : UiCallback {
                override fun <T : Any> onSuccess(response: T) {
                    super.onSuccess(response)
                    if (response is DigitizeResponseBody) {
                        storeDigitizeResponseData(digitizeResponse = response)
                        continuation.resume(getSuccessResponse(response.statusMessage.toString(), response))
                    } else {
                        continuation.resume(getErrorResponse(context.getString(R.string.no_response_data_received)))
                    }
                }

                override fun <T : Any?> onError(response: T) {
                    super.onError(response)
                    if (response is DigitizeResponse) {
                        continuation.resume(getErrorResponse(response.statusMessage.toString()))
                    } else {
                        val errorMessage = getErrorMessage(response)
                        continuation.resume(getErrorResponse(errorMessage))
                    }
                }
            })
    }

    /**
     * Creates a [UiCallback] for handling provision responses.
     *
     * Resumes the given [CancellableContinuation] with a success result
     * if the response is valid, or an error result otherwise.
     *
     * @param continuation Continuation to be resumed with the result.
     *
     * @return Configured [UiCallback] instance.
     */
    private fun createProvisionCallback(continuation: CancellableContinuation<WalletSdkResult<NotifyProvisionResponse>>): UiCallback =
        object : UiCallback {
            override fun <T : Any?> onSuccess(response: T) {
                super.onSuccess(response)
                if (response is NotifyProvisionResponse) {
                    continuation.resume(getSuccessResponse(response.statusMessage.toString(), response))
                } else {
                    continuation.resume(getErrorResponse(MyApplication.appContext.getString(R.string.no_response_data_received)))
                }
            }

            override fun <T : Any?> onError(response: T) {
                super.onError(response)
                val errorMessage = getErrorMessage(response)
                continuation.resume(getErrorResponse(errorMessage))
            }
        }

    /**
     * Creates a [UiCallback] for handling provision responses.
     *
     * Resumes the given [CancellableContinuation] with a success result
     * if the response is valid, or an error result otherwise.
     *
     * @param continuation Continuation to be resumed with the result.
     *
     * @return Configured [UiCallback] instance.
     */
    private fun createProvisionStatusCallback(continuation: CancellableContinuation<WalletSdkResult<NotifyProvisionStatusResponse>>): UiCallback =
        object : UiCallback {
            override fun <T : Any?> onSuccess(response: T) {
                super.onSuccess(response)
                if (response is NotifyProvisionStatusResponse) {
                    continuation.resume(getSuccessResponse(response.statusMessage.toString(), response))
                } else {
                    continuation.resume(getErrorResponse(MyApplication.appContext.getString(R.string.no_response_data_received)))
                }
            }

            override fun <T : Any?> onError(response: T) {
                super.onError(response)
                val errorMessage = getErrorMessage(response)
                continuation.resume(getErrorResponse(errorMessage))
            }
        }

    /**
     * Fetches provisioned data from the wallet SDK.
     * Suspends until the SDK responds and converts the callback result into a [WalletSdkResult] using a provision callback.
     *
     * @param context Android context required by the SDK.
     * @param paymentAppInstanceId Payment app instance ID.
     * @param digitizationReferenceNumber Digitization reference number.
     *
     * @return Success with [NotifyProvisionResponse] if data is received,
     * or error result if the operation fails.
     */
    suspend fun fetchProvisionedData(
        context: Context,
        paymentAppInstanceId: String,
        digitizationReferenceNumber: String
    ): WalletSdkResult<NotifyProvisionResponse> = suspendCancellableCoroutine { continuation ->
        walletSdk?.fetchProvisionedData(
            context = context,
            paymentAppInstanceId = paymentAppInstanceId,
            digitizationReferenceNumber = digitizationReferenceNumber,
            callback = createProvisionCallback(continuation)
        )
    }

    /**
     * Notifies provision status to the wallet SDK.
     * Suspends until the SDK responds and converts the callback result into a [WalletSdkResult] using a provision callback.
     *
     * @param context Android context required by the SDK.
     * @param paymentAppInstanceId Payment app instance ID.
     * @param digitizationReferenceNumber Digitization reference number.
     * @param apduResponses Optional APDU response list for provisioning.
     *
     * @return Success with [NotifyProvisionStatusResponse] if notification succeeds,
     * or error result if the operation fails.
     */
    suspend fun notifyProvisionStatus(
        context: Context,
        paymentAppInstanceId: String,
        digitizationReferenceNumber: String,
        apduResponses: List<ApduResponsesItem>? = null
    ): WalletSdkResult<NotifyProvisionStatusResponse> = suspendCancellableCoroutine { continuation ->
        walletSdk?.notifyProvisionStatus(
            context = context,
            paymentAppInstanceId = paymentAppInstanceId,
            digitizationReferenceNumber = digitizationReferenceNumber,
            apduResponses = apduResponses,
            callback = createProvisionStatusCallback(continuation)
        )
    }

    /**
     * Updates card status via the wallet SDK.
     * Suspends until the SDK responds and converts the callback result into a [WalletSdkResult].
     *
     * @param context Android context required by the SDK.
     * @param cardStatus Status to be updated for the card.
     * @param cardList Card details to be updated (nullable).
     * @param paymentAppInstanceId Payment app instance ID.
     * @param digitizationReferenceNumber Digitization reference number.
     * @param pnoType PNO type identifier.
     * @param deviceDetails Device details like  connected status and current sequence number.
     *
     * @return Success with [UpdateCardStatusResponse] if update succeeds,
     * or error result if the operation fails or response is invalid.
     */
    suspend fun updateCardStatus(
        context: Context, cardStatus: String, cardList: CardList?, paymentAppInstanceId: String,
        digitizationReferenceNumber: String, pnoType: String, deviceDetails: BaseFragment.DeviceDetails
    ): WalletSdkResult<UpdateCardStatusResponse> = suspendCancellableCoroutine { continuation ->
        walletSdk?.updateCardStatus(
            params = UpdateCardStatusParams(
                context = context,
                cardStatus = cardStatus,
                card = cardList,
                paymentAppInstanceId = paymentAppInstanceId,
                digitizationReferenceNumber = digitizationReferenceNumber,
                pnoType = pnoType,
                connected = deviceDetails.connected,
                currentSequenceCounter = deviceDetails.currentSequenceCounter,
                uiCallback = object : UiCallback {
                    override fun <T : Any?> onSuccess(response: T) {
                        when {
                            response == null -> {
                                continuation.resume(
                                    getErrorResponse(context.getString(R.string.something_went_wrong))
                                )
                            }

                            response is UpdateCardStatusResponse -> {
                                if (response.statusMessage.isNullOrBlank()) {
                                    continuation.resume(
                                        getErrorResponse(context.getString(R.string.something_went_wrong))
                                    )
                                } else {
                                    continuation.resume(
                                        getSuccessResponse(
                                            statusMessage = response.statusMessage.toString(),
                                            response = response
                                        )
                                    )
                                }
                            }

                            else -> {
                                continuation.resume(
                                    getErrorResponse(context.getString(R.string.no_response_data_received))
                                )
                            }
                        }
                    }

                    override fun onError(errorNbr: Int, errorMsg: String?) {
                        super.onError(errorNbr, errorMsg)
                        logger.error("onError : $errorMsg")
                        continuation.resume(getErrorResponse(errorMsg.toString()))
                    }
                }
            )
        )
    }

    /**
     * Sets a card as default via the wallet SDK.
     * Suspends until the SDK responds and converts the callback result into a [WalletSdkResult].
     *
     * @param context Android context required by the SDK.
     * @param digitizationReferenceNumber Digitization reference number.
     *
     * @return Success with [DefaultCardResponse] if update succeeds,
     * or error result if the operation fails or response is invalid.
     */
    suspend fun setCardAsDefault(
        context: Context,
        digitizationReferenceNumber: String
    ): WalletSdkResult<DefaultCardResponse> = suspendCancellableCoroutine { continuation ->
        walletSdk?.setCardAsDefault(
            context = context,
            digitizationReferenceNumber = digitizationReferenceNumber,
            callback = object : UiCallback {
                override fun <T : Any?> onSuccess(response: T) {
                    if (response is DefaultCardResponse) {
                        continuation.resume(
                            getSuccessResponse(
                                statusMessage = response.statusMessage.toString(),
                                response = response
                            )
                        )
                    } else {
                        continuation.resume(getErrorResponse(context.getString(R.string.no_response_data_received)))
                    }
                }

                override fun onError(errorNbr: Int, errorMsg: String) {
                    super.onError(errorNbr, errorMsg)
                    logger.error("onError : $errorMsg")
                    continuation.resume(getErrorResponse(errorMsg))
                }
            })
    }

    /**
     * Fetches transaction history from the wallet SDK.
     * Suspends until the SDK responds and converts the callback result into a [WalletSdkResult].
     *
     * @param context Android context required by the SDK.
     * @param paymentAppInstanceId Payment app instance ID.
     * @param digitizationReferenceNumber Digitization reference number.
     *
     * @return Success with [TransactionHistoryResponse] if data is received,
     * or error result if the operation fails or response is invalid.
     */
    suspend fun fetchTransactionHistory(
        context: Context,
        paymentAppInstanceId: String,
        digitizationReferenceNumber: String,
        pnoType: String
    ): WalletSdkResult<TransactionHistoryResponse> = suspendCancellableCoroutine { continuation ->
        walletSdk?.fetchTransactionHistory(
            context = context,
            paymentAppInstanceId = paymentAppInstanceId,
            digitizationReferenceNumber = digitizationReferenceNumber,
            pnoType,
            callback = object : UiCallback {
                override fun <T : Any?> onSuccess(response: T) {
                    if (response is TransactionHistoryResponse) {
                        if (response.isValidResponse()) {
                            continuation.resume(
                                getSuccessResponse(
                                    statusMessage = response.statusMessage.toString(),
                                    response = response
                                )
                            )
                            return
                        }
                        continuation.resume(getErrorResponse(response.statusMessage.toString()))
                    } else {
                        continuation.resume(getErrorResponse(context.getString(R.string.no_response_data_received)))
                    }
                }

                override fun onError(errorNbr: Int, errorMsg: String) {
                    logger.error("onError : $errorMsg")
                    continuation.resume(getErrorResponse(errorMsg))
                }
            })
    }

    /**
     * Creates a [UiCallback] for handling activation responses.
     * Resumes the provided continuation with a success result if the response is valid, or an error result otherwise.
     *
     * @param continuation Continuation to be resumed with the result.
     *
     * @return Configured [UiCallback] instance.
     */
    private fun createGetActivationCallback(continuation: CancellableContinuation<WalletSdkResult<GetActivationResponse>>): UiCallback =
        object : UiCallback {
            override fun <T : Any?> onSuccess(response: T) {
                super.onSuccess(response)
                if (response is GetActivationResponse) {
                    continuation.resume(getSuccessResponse(response.statusMessage.toString(), response))
                } else {
                    continuation.resume(getErrorResponse(MyApplication.appContext.getString(R.string.no_response_data_received)))
                }
            }

            override fun <T : Any?> onError(response: T) {
                super.onError(response)
                val errorMessage = getErrorMessage(response)
                continuation.resume(getErrorResponse(errorMessage))
            }
        }

    /**
     * Creates a [UiCallback] for handling activation verification responses.
     * Resumes the provided continuation with a success result if the response is valid, or an error result otherwise.
     *
     * @param continuation Continuation to be resumed with the result.
     *
     * @return Configured [UiCallback] instance.
     */
    private fun createVerifyActivationCallback(continuation: CancellableContinuation<WalletSdkResult<VerifyActivationResponse>>): UiCallback =
        object : UiCallback {
            override fun <T : Any?> onSuccess(response: T) {
                super.onSuccess(response)
                if (response is VerifyActivationResponse) {
                    continuation.resume(getSuccessResponse(response.statusMessage.toString(), response))
                } else {
                    continuation.resume(getErrorResponse(MyApplication.appContext.getString(R.string.no_response_data_received)))
                }
            }

            override fun <T : Any?> onError(response: T) {
                super.onError(response)
                val errorMessage = getErrorMessage(response)
                continuation.resume(getErrorResponse(errorMessage))
            }
        }

    /**
     * Generates OTP via the wallet SDK.
     * Suspends until the SDK responds and converts the callback result into a [WalletSdkResult] using an activation callback.
     *
     * @param context Android context required by the SDK.
     * @param paymentAppInstanceId Payment app instance ID.
     * @param referenceNumber Reference number for OTP generation.
     *
     * @return Success with [GetActivationResponse] if OTP generation succeeds,
     * or error result if the operation fails.
     */
    suspend fun generateOTP(
        context: Context,
        paymentAppInstanceId: String,
        referenceNumber: String,
        authenticationMethod: AuthenticationMethod
    ): WalletSdkResult<GetActivationResponse> = suspendCancellableCoroutine { continuation ->
        walletSdk?.generateOTP(
            context = context,
            paymentAppInstanceId = paymentAppInstanceId,
            referenceNumber = referenceNumber,
            authenticationMethod = authenticationMethod,
            callback = createGetActivationCallback(continuation)
        )
    }

    /**
     * Verifies OTP via the wallet SDK.
     * Suspends until the SDK responds and converts the callback result into a [WalletSdkResult] using an activation callback.
     *
     * @param context Android context required by the SDK.
     * @param paymentAppInstanceId Payment app instance ID.
     * @param referenceNumber Reference number used for OTP.
     * @param authMethod Authentication method used for verification.
     * @param authCode OTP/authentication code to verify.
     *
     * @return Success with [VerifyActivationResponse] if verification succeeds,
     * or error result if the operation fails.
     */
    suspend fun verifyOTP(
        context: Context,
        paymentAppInstanceId: String,
        referenceNumber: String,
        authCode: String
    ): WalletSdkResult<VerifyActivationResponse> = suspendCancellableCoroutine { continuation ->
        walletSdk?.verifyOTP(
            context = context,
            paymentAppInstanceId = paymentAppInstanceId,
            referenceNumber = referenceNumber,
            authCode = authCode,
            callback = createVerifyActivationCallback(continuation)
        )
    }

    /**
     * Updates Transaction Notification receiving status.
     * Suspends until the SDK responds and converts the callback result into a [WalletSdkResult].
     *
     * @param context Android context required by the SDK.
     * @param paymentAppInstanceId Payment app instance ID.
     * @param digitizationReferenceNumber Digitization reference number.
     * @param enabled transaction notification receiving status.
     *
     * @return Success with [UpdateTransactionNotificationResponse] if data is received,
     * or error result if the operation fails or response is invalid.
     */
    suspend fun updateTransactionNotification(
        context: Context,
        paymentAppInstanceId: String,
        digitizationReferenceNumber: String,
        enabled : String
    ): WalletSdkResult<UpdateTransactionNotificationResponse> = suspendCancellableCoroutine { continuation ->
        walletSdk?.updateTransactionNotification(
            context = context,
            paymentAppInstanceId = paymentAppInstanceId,
            digitizationReferenceNumber = digitizationReferenceNumber,
            enabled,
            callback = object : UiCallback {
                override fun <T : Any?> onSuccess(response: T) {
                    if (response is UpdateTransactionNotificationResponse) {
                        if (response.isValidResponse()) {
                            continuation.resume(
                                getSuccessResponse(
                                    statusMessage = response.statusMessage.toString(),
                                    response = response
                                )
                            )
                            return
                        }
                        continuation.resume(getErrorResponse(response.statusMessage.toString()))
                    } else {
                        continuation.resume(getErrorResponse(context.getString(R.string.no_response_data_received)))
                    }
                }

                override fun onError(errorNbr: Int, errorMsg: String) {
                    logger.error("onError : $errorMsg")
                    continuation.resume(getErrorResponse(errorMsg))
                }
            })
    }
}