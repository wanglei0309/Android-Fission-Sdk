// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: TermsConditionFragment.kt is the  final stage of the card digitization process.
 * It manages the complete flow from accepting terms digitizing the card
 * to synchronizing with the backend and maintaining the secure element.
 **/
package com.infineon.secora.wallet.ui.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.PayHostFssSync
import com.infineon.secora.wallet.adapter.AuthMethodAdapter
import com.infineon.secora.wallet.client.data.models.DeleteScriptBase
import com.infineon.secora.wallet.client.data.models.GetPendingResponse
import com.infineon.secora.wallet.client.data.models.capturecard.CapturedCardDetail
import com.infineon.secora.wallet.client.data.models.common.ApduCommands
import com.infineon.secora.wallet.client.data.models.common.AuthenticationMethod
import com.infineon.secora.wallet.client.data.models.common.DigitizeResponseBody
import com.infineon.secora.wallet.client.data.models.common.GetActivationResponse
import com.infineon.secora.wallet.client.data.models.common.NotifyProvisionResponse
import com.infineon.secora.wallet.client.data.models.common.UpdateCardStatusResponse
import com.infineon.secora.wallet.client.data.models.common.VerifyActivationResponse
import com.infineon.secora.wallet.client.data.models.provision.ApduResponsesItem
import com.infineon.secora.wallet.client.operations.middleware.callbacks.UiCallback
import com.infineon.secora.wallet.client.util.AppDispatchers
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.DialogCommonMessageBinding
import com.infineon.secora.wallet.databinding.FragmentWalletTermsConditionsBinding
import com.infineon.secora.wallet.utils.helper.showSecure
import com.infineon.secora.wallet.domain.walletsdk.WalletRepository
import com.infineon.secora.wallet.domain.walletsdk.WalletSdkResult
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothUiStateManager
import com.infineon.secora.wallet.domain.wearable.ble.script.ScriptHandler
import com.infineon.secora.wallet.firebase.AppEvent
import com.infineon.secora.wallet.firebase.EventBus
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.ui.widget.AsteriskPasswordTransformationMethod
import com.infineon.secora.wallet.utils.CommonResponse
import com.infineon.secora.wallet.utils.IDVType
import com.infineon.secora.wallet.utils.helper.DigitizationDeleteFlowGate
import com.infineon.secora.wallet.utils.helper.NfcScriptExecutionTracker
import com.infineon.secora.wallet.utils.helper.ScriptDataParser
import com.infineon.secora.wallet.utils.helper.SequenceCounterHelper
import com.infineon.secora.wallet.utils.StringUtils.isEmptyString
import com.infineon.secora.wallet.utils.helper.UIHelper
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_CARD
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_DEVICE_STATUS_UPDATE
import com.infineon.secora.wallet.utils.constants.Constants.CARD_PROVISION
import com.infineon.secora.wallet.utils.constants.Constants.DELETED_STATUS
import com.infineon.secora.wallet.utils.constants.Constants.EMAIL_TO_CARDHOLDER_ADDRESS
import com.infineon.secora.wallet.utils.constants.Constants.FAILED
import com.infineon.secora.wallet.utils.constants.Constants.PENDING
import com.infineon.secora.wallet.utils.constants.Constants.PNO_MDES
import com.infineon.secora.wallet.utils.constants.Constants.PNO_VTS
import com.infineon.secora.wallet.utils.helper.CasdCertificateHelper
import com.infineon.secora.wallet.utils.constants.Constants.DELETE_SCRIPT
import com.infineon.secora.wallet.utils.constants.Constants.SCRIPT
import com.infineon.secora.wallet.utils.constants.Constants.SUCCESS
import com.infineon.secora.wallet.utils.constants.Constants.SUCCESS_MESSAGE
import com.infineon.secora.wallet.utils.constants.Constants.SUCCESS_VALUE
import com.infineon.secora.wallet.utils.constants.Constants.TEXT_TO_CARDHOLDER_NUMBER
import com.infineon.secora.wallet.utils.constants.JsonKey
import com.infineon.secora.wearable.SecoraWearableSDK
import com.infineon.secora.wearable.nfc.ScriptExecutionCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * TermsConditionFragment : It handles the terms and conditions implementation
 *
 */
class TermsConditionFragment : BaseFragment() {

    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)
    private lateinit var binding: FragmentWalletTermsConditionsBinding
    private var paymentId: String? = null
    private var tokenRefNumber: String? = null
    private var decision: String? = null
    private var authCode: String? = null
    private lateinit var activity: MainActivity
    private var maskedNum: String? = null
    private var seId: String? = null
    private var digitizationReferenceNumberCancel: String? = null
    private var checkCancelBtn: Boolean = false
    private var cardProvisionDataApiTriggered: Boolean = false

    private var capturedCardDetail: CapturedCardDetail? = null
    private var selectedMethod: AuthenticationMethod? = null
    private var eventCollectorJob: Job? = null
    private val isScriptRunning = AtomicBoolean(false)

    /**
     * Represents the available OTP authentication methods.
     *
     * Note:
     * - In Yellow Flow, only two OTP methods are supported: Mobile and Email.
     * - This enum is used to track the currently active OTP method and
     *   to enable seamless switching (e.g., via "Try Another Method")
     *   without showing redundant selection screens.
     */

    enum class OtpMethod {
        MOBILE,
        EMAIL
    }

    /**
     * Initializes the activity reference after the view is created.
     * Safely casts the attached activity to MainActivity for further use.
     *
     * @param view The view returned by [onCreateView].
     * @param savedInstanceState Saved instance state bundle, if any.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity = (requireActivity() as MainActivity)
        activity.showLoading(false, "")
        updateBluetoothIconState()
    }

    /**
     * onCreateView method is used to inflate the layout for this fragment
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletTermsConditionsBinding.inflate(inflater, container, false)
        activity = requireActivity() as MainActivity

        digitizationReferenceNumberCancel = StorageRepository.readString(PreferenceKey.DIGI_REFERENCE_NUMBER_CANCEL)
        dismissKeyboardOnTap(requireActivity(), binding.llNickName.root)
        dismissKeyboardOnTap(requireActivity(), binding.root)

        capturedCardDetail = arguments?.getSerializable(BundleKey.CAPTURED_CARD_DETAIL, CapturedCardDetail::class.java)
        maskedNum = capturedCardDetail?.maskedCardNumber

        val cardType =
            if (pnoType() == PNO_MDES) getString(R.string.master_card) else getString(R.string.visa)
        binding.llSuccess.tvCard.text = getString(R.string.formatted_two_values, cardType, maskedNum)

        val status = arguments?.getString(BundleKey.PENDING_STATUS)
        logger.debug("status: $status")
        handleAuthMethodForPendingStatus(status)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (status == PENDING) {
                        findNavController().popBackStack()
                        return
                    }

                    if (!isNFC()) {
                        checkCancelBtn = true
                        deleteCard(
                            requireActivity() as MainActivity,
                            DELETED_STATUS,
                            digitizationReferenceNumberCancel.orEmpty()
                        )
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.text_navigate_back_prohibited),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
        setupUI()
        validationForBankAuth()
        initListeners()
        setupListenersAndReceivers()
        return binding.root
    }

    /**
     * Shows OTP selection UI for a pending card and loads available authentication methods.
     *
     * @param pendingStatus Card pending status from navigation arguments.
     */
    private fun handleAuthMethodForPendingStatus(pendingStatus: String?) {
        if (PENDING == pendingStatus) {
            lifecycleScope.launch {

                val pno = arguments?.getString(BundleKey.PNO_TYPE)
                val dPanSuffix = arguments?.getString(BundleKey.D_PAN_SUFFIX).orEmpty()
                val digitizationReferenceNumber = arguments?.getString(BundleKey.DIGITIZATION_REFERENCE_NUMBER).toString()
                val card = StorageRepository.getCardByDigitizationReferenceNumber(requireContext(), digitizationReferenceNumber)

                binding.llSuccess.tvCard.text =
                    if (pno == PNO_MDES)
                        getString(R.string.card_mastercard, dPanSuffix)
                    else
                        getString(R.string.card_visa, dPanSuffix)

                activity.showLoading(false, "")
                binding.llSelectOtp.root.visibility = View.VISIBLE
                setupAuthMethod(card?.authenticationMethods ?: arrayListOf())
            }
        }
    }

    /**
     * Subscribes to card-provision refresh events from the app event bus.
     */
    private fun setupListenersAndReceivers() {

        eventCollectorJob = viewLifecycleOwner.lifecycleScope.launch {
                EventBus.events.collect { event ->
                    when (event.action) {
                        ACTION_CARD -> handleRefreshEvent(event)
                        ACTION_DEVICE_STATUS_UPDATE -> handleSuspendNotification(event)
                    }
                }
        }
    }

    /**
     * Handles card-provision-related event from EventBus.
     */
    private fun handleRefreshEvent(event: AppEvent) {
        if (!isAdded) {
            logger.info("Fragment not attached to activity, ignoring event")
            return
        }
        val msgType = event.getStringExtra(BundleKey.MSG_TYPE)
        requireActivity().runOnUiThread {
            logger.debug("cardProvisionDataApiTriggered : $cardProvisionDataApiTriggered")
            logger.debug("msgType : $msgType")
            if (!cardProvisionDataApiTriggered && msgType == CARD_PROVISION) {
                cardProvisionDataApiTriggered = true

                binding.llSelectOtp.root.visibility = View.GONE
                ContextCompat.getColor(requireContext(), R.color.white)
                    .let { it1 -> binding.loader.progressBar.setBackgroundColor(it1) }
                activity.showLoading(true, getString(R.string.text_verifying_identity))
                if (!isNFC())
                    fetchNotifyProvision()
                else fetchNFCNotifyProvision()
            }
        }
    }

    /**
     * Cleans up when the Fragment view is destroyed by cancelling event collection.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        eventCollectorJob?.cancel()
        eventCollectorJob = null
    }

    /**
     * initListeners() : It handles the click listeners
     *
     */
    private fun initListeners() {
        binding.llNickName.etCardHolderName.addTextChangedListener(object : TextWatcher {
            var previousText: String = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                previousText = s.toString()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val nickname = it.toString()
                    val allowedPattern =
                        Regex("^[a-zA-Z0-9 _-]*$") // allow letters, digits, spaces, _ and -

                    if (!allowedPattern.matches(nickname)) {
                        val filtered = nickname.filter { ch ->
                            ch.isLetterOrDigit() || ch == '_' || ch == '-' || ch == ' '
                        }

                        if (filtered != nickname) {
                            binding.llNickName.etCardHolderName.removeTextChangedListener(this)
                            binding.llNickName.etCardHolderName.setText(filtered)
                            binding.llNickName.etCardHolderName.setSelection(filtered.length)
                            binding.llNickName.etCardHolderName.addTextChangedListener(this)

                            statusDialog(requireActivity(), getString(R.string.invalid_nickname))
                        }
                    }
                }
            }
        })
        binding.llNickName.etCardHolderName.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }

    /**
     * setupUI() : It handles the UI click events
     * It handles the toolbar data
     *
     */
    private fun setupUI() {
        paymentId = StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID)
        seId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)

        getWearableImage(
            StorageRepository.readString(
                PreferenceKey.DEVICE_IMAGE
            )
        )

        binding.selectedDevice.textDeviceName.text =
            StorageRepository.readString(PreferenceKey.DEVICE_NAME)
        tokenRefNumber = arguments?.getString(BundleKey.DIGITIZATION_REFERENCE_NUMBER).toString()
        val termsData = StorageRepository.readString(PreferenceKey.TERMS_DATA)
        binding.tvInfo.text = termsData
        logger.debug("Terms_Data: ${binding.tvInfo.text}")
        val status = arguments?.getString(BundleKey.PENDING_STATUS)
        logger.debug("Pending :: status: $status")
        hideTermsAndConditionViewForPendingCard(status)
        setListeners()
    }

    /**
     * Wires terms, OTP, nickname, and navigation click listeners for this screen.
     */
    private fun setListeners() {

        binding.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.scrollView.post {
                    binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }
                binding.llAccept.visibility = View.VISIBLE
                return@setOnCheckedChangeListener
            }
            binding.llAccept.visibility = View.GONE
        }

        binding.btnAccept.setOnClickListener {
            handleAcceptButtonClickEvent()
        }

        binding.btnCancel.setOnClickListener {
            handleCancelButtonClickEvent()
        }

        binding.llNickName.btnAddName.setOnClickListener {
            lifecycleScope.launch {
                handleAddNickNameEvent()
            }
        }

        binding.llSuccess.btnDone.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean(BundleKey.NAVIGATED_AFTER_ADDCARD_FLOW, true)
            }
            findNavController().popBackStack()
            findNavController().navigate(R.id.cardListFragment, bundle)
        }

        binding.llBank.tvRequestCode.setOnClickListener {
            clearOtpBoxes()
            prepareOtp(binding.llBank.tvTimer, false)
            updateOtpUiAndRestart(selectedMethod!!)
            Toast.makeText(activity,getString(R.string.otp_send), Toast.LENGTH_SHORT).show()
        }

        binding.llBank.btnVerifyCode.setOnClickListener {
            val otpBoxes = listOf(
                binding.llBank.etBox1.text.toString().trim(),
                binding.llBank.etBox2.text.toString().trim(),
                binding.llBank.etBox3.text.toString().trim(),
                binding.llBank.etBox4.text.toString().trim(),
                binding.llBank.etBox5.text.toString().trim(),
                binding.llBank.etBox6.text.toString().trim()
            )

            val otp = otpBoxes.joinToString("")

            if (otp.length == 6) {
                binding.llBank.root.visibility = View.GONE
                ContextCompat.getColor(requireContext(), R.color.white)
                    .let { it1 -> binding.loader.progressBar.setBackgroundColor(it1) }
                activity.showLoading(true, getString(R.string.text_verifying_identity))
                verifyOTP(otp)
            } else {
                showToast(getString(R.string.text_valid_otp))
            }
        }

        binding.llBank.tvTryOtherMethod.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }


    /**
     * Initializes and configures the RecyclerView that displays
     * available authentication methods (e.g., Email, Mobile OTP).
     *
     * - Sets a LinearLayoutManager for vertical list display
     * - Attaches AuthMethodAdapter with authentication methods data
     * - Handles item click events via callback
     *
     * @param digitizeResponse Response object containing authentication methods
     */
    private fun setupAuthMethod(authenticationMethods: ArrayList<AuthenticationMethod>) {
        binding.llSelectOtp.recyclerViewAuthMethod.layoutManager = LinearLayoutManager(activity)
        binding.llSelectOtp.recyclerViewAuthMethod.adapter = AuthMethodAdapter(
            authenticationMethods,
            { position, _ -> handleAuthMethodClick(authenticationMethods, position) })
    }
    /**
     * Handles user selection of an authentication method item.
     *
     * Determines whether the selected method is Email or Mobile OTP,
     * updates the current OTP method accordingly, and navigates to
     * the OTP screen while triggering OTP generation.
     *
     * @param authenticationMethods available authentication methods
     * @param position Position of the selected item in the list
     */
    private fun handleAuthMethodClick(
        authenticationMethods: ArrayList<AuthenticationMethod>, position: Int
    ) {
        val authenticationMethod = authenticationMethods[position]
        selectedMethod = authenticationMethod
        val type = IDVType.entries.firstOrNull { it.idvType == authenticationMethod.key }
        when (type) {
            IDVType.EMV_3DS, IDVType.CARDHOLDER_TO_VISIT_WEBSITE -> {
                val status = arguments?.getString(BundleKey.PENDING_STATUS)
                val isUrlExpired = isThreeDESAuthOlderThan15Minutes()
                logger.debug("status: $status")
                logger.debug("isUrlExpired: $isUrlExpired")
                if (status == PENDING || isUrlExpired) {
                    handleThreeDesExpiryFlow(authenticationMethod)
                } else {
                    launchIntent(Intent.ACTION_VIEW, authenticationMethod.value)

                    binding.llSelectOtp.root.visibility = View.GONE
                    ContextCompat.getColor(requireContext(), R.color.white)
                        .let { it1 -> binding.loader.progressBar.setBackgroundColor(it1) }
                    activity.showLoading(true, getString(R.string.text_verifying_identity))
                    handleVerifyOtpSuccessResponseFlow(45000)
                }
            }

            IDVType.OTP_SMS, IDVType.OTP_EMAIL, IDVType.OTP_ONLINE_BANKING, IDVType.TEXT_TO_CARDHOLDER_NUMBER,
            IDVType.EMAIL_TO_CARDHOLDER_ADDRESS, IDVType.OUTBOUND_CALL, IDVType.MASKED_MOBILE_PHONE_NUMBER -> {
                showOtpScreenAndGenerate(authenticationMethod)
            }

            IDVType.CUSTOMER_SERVICE, IDVType.CARDHOLDER_TO_CALL_AUTOMATED_NUMBER, IDVType.CARDHOLDER_TO_CALL_MANNED_NUMBER -> {
               launchIntent(Intent.ACTION_DIAL,"tel:${authenticationMethod.value}")
            }

            IDVType.APP_TO_APP, IDVType.CARDHOLDER_TO_USE_ISSUER_MOBILE_APP -> {
                launchIntent(Intent.ACTION_VIEW,"market://details?id=${authenticationMethod.value}")
            }

            else -> {
                showOtpScreenAndGenerate(authenticationMethod)
            }
        }
    }

    /**
     * Regenerates a 3-D Secure activation URL when the stored link is expired or the card is pending.
     *
     * @param authenticationMethod Selected EMV 3DS authentication method.
     */
    private fun handleThreeDesExpiryFlow(authenticationMethod: AuthenticationMethod) {
        activity.showLoading(true, getString(R.string.text_please_wait))
        lifecycleScope.launch {
            authenticationMethod.apply {
                value = ""
            }
            val sdkResult = WalletRepository.generateOTP(
                context = requireContext(),
                paymentAppInstanceId = paymentId.toString(),
                referenceNumber = tokenRefNumber.toString(),
                authenticationMethod = authenticationMethod,
            )

            activity.showLoading(false, "")
            if (sdkResult.isSuccess) {
                sdkResult.response?.let { activationResponse ->
                    if (activationResponse.statusMessage.isNullOrEmpty()) {
                        return@launch
                    }
                    if (activationResponse.statusCode == SUCCESS_VALUE) {
                        handleThreeDesSuccessResponse(activationResponse, sdkResult.statusMessage)
                    }
                }
            } else {
                handleSessionOrError(sdkResult.errorMessage) {
                    activity.runOnUiThread {
                        Toast.makeText(requireContext(), sdkResult.errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * Opens the refreshed 3-D Secure activation URL and continues the verification flow.
     *
     * @param activationResponse SDK activation response containing the 3DS URL.
     * @param statusMessage      Status message returned by the wallet SDK.
     */
    private fun handleThreeDesSuccessResponse(activationResponse : GetActivationResponse, statusMessage : String) {
        if (activationResponse.threeDSActivationURL == null) {
            Toast.makeText(requireContext(),statusMessage, Toast.LENGTH_SHORT).show()
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activationResponse.threeDSActivationURL))
        startActivity(intent)

        binding.llSelectOtp.root.visibility = View.GONE
        ContextCompat.getColor(requireContext(), R.color.white)
            .let { it1 -> binding.loader.progressBar.setBackgroundColor(it1) }
        activity.showLoading(true, getString(R.string.text_verifying_identity))
        handleVerifyOtpSuccessResponseFlow(45000)
    }

    /**
     * Returns whether the last stored 3-D Secure authentication timestamp is older than 15 minutes.
     *
     * @return `true` when a fresh 3DS URL must be requested.
     */
    private fun isThreeDESAuthOlderThan15Minutes(): Boolean {
        val loginTimeStr = StorageRepository.readString(PreferenceKey.THREE_DES_AUTH_TIME)
        if (isEmptyString(loginTimeStr)) return false

        val loginTime = LocalDateTime.parse(loginTimeStr)
        val currentTime = LocalDateTime.now()
        val minutesBetween = ChronoUnit.MINUTES.between(loginTime, currentTime)

        return minutesBetween >= 15
    }

    /**
     * Validates and persists the card nickname entered on the success screen.
     */
    private suspend fun handleAddNickNameEvent() {
        hideKeyboard(binding.llNickName.root)
        val enteredNickname =
            binding.llNickName.etCardHolderName.text.toString().trim()

        activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        CardListFragment.isCardAdded = true

        val paymentAppInstanceId = StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID)
        val suffix = arguments?.getString(BundleKey.D_PAN_SUFFIX)
            ?: maskedNum.toString().takeLast(4)
        if (enteredNickname.isNotEmpty() && isNicknameDuplicate(enteredNickname, paymentAppInstanceId, suffix)) {

            showAlertDialog(
                getString(R.string.text_secora_wallet),
                getString(R.string.nickname_already_assigned)
            )
            return
        }

        StorageRepository.saveNicknameForCard(
            requireContext(),
            paymentAppInstanceId,
            suffix,
            enteredNickname
        )
        StorageRepository.updateLocalCardNicknameByDpan(
            requireContext(),
            paymentAppInstanceId,
            suffix,
            enteredNickname
        )
        arguments?.getString(BundleKey.DIGITIZATION_REFERENCE_NUMBER)?.trim()?.takeIf { it.isNotEmpty() }?.let { dRef ->
            StorageRepository.setLocalUserCardNickname(requireContext(), dRef, enteredNickname)
        }

        binding.llNickName.root.visibility = View.GONE
        binding.llSuccess.root.visibility = View.VISIBLE
    }

    /**
     * Handles Terms Accept: ensures wearable connectivity, then starts card digitization.
     */
    private fun handleAcceptButtonClickEvent() {
        checkCancelBtn = false
        if (!isNFC())
            ensureBleConnectedThenRun(
                onConnected = {
                    executeDigitizeCard()
                }
            )
        else
            ensureNfcReadyThenRun {
                executeDigitizeCard()
            }
        handleBackPressOnAcceptButtonClick()
    }

    /**
     * Handles Terms Cancel: triggers provision-abort cleanup and card deletion when required.
     */
    private fun handleCancelButtonClickEvent() {
        checkCancelBtn = true
        if (!isNFC()) {
            ensureBleConnectedThenRun(
                onConnected = {
                    deleteCard(
                        requireActivity() as MainActivity,
                        DELETED_STATUS,
                        digitizationReferenceNumberCancel.toString()
                    )
                },
                onCancelled = {
                    abortProvisionCleanupWithoutBle(
                        digitizationReferenceNumberCancel.toString()
                    )
                }
            )
        } else {
            ensureNfcReadyThenRun {
                deleteCard(
                    requireActivity() as MainActivity,
                    DELETED_STATUS,
                    digitizationReferenceNumberCancel.toString()
                )
            }

        }
    }

    /**
     * Displays the OTP screen and initiates OTP generation based on the currently
     * selected authentication method (Mobile or Email).
     *
     * Note:
     * - Mobile and Email OTP flows share the same UI.
     * - When switching between methods, the OTP countdown must restart fresh
     *   to avoid resuming a previous timer from SharedPreferences.
     * - The notification text is updated to clearly reflect the active OTP method.
     *
     * This ensures a clear UX when using "Try Another Method" and prevents
     * confusion caused by reused timers or unchanged UI state.
     */
    private fun showOtpScreenAndGenerate(authenticationMethod: AuthenticationMethod) {
        binding.llSelectOtp.root.visibility = View.GONE
        binding.llBank.root.visibility = View.VISIBLE
        updateOtpUiAndRestart(authenticationMethod)
    }

    /**
     * Updates OTP-related UI elements and forcefully restarts the OTP flow.
     *
     * - Updates alert message based on the selected OTP method.
     * - Forces a fresh OTP countdown (ignores any existing stored expiry time).
     * - Triggers OTP generation for the newly selected method.
     *
     * This method is intentionally used when switching OTP methods to ensure
     * a fresh and predictable OTP experience.
     */
    private fun updateOtpUiAndRestart(authenticationMethod: AuthenticationMethod) {
        val messageRes = if (authenticationMethod.type.equals(TEXT_TO_CARDHOLDER_NUMBER)) {
            R.string.bank_alert_msg_mobile
        } else if (authenticationMethod.type.equals(EMAIL_TO_CARDHOLDER_ADDRESS)) {
            R.string.bank_alert_msg_email
        } else {
            R.string.bank_alert_msg_code
        }
        binding.llBank.tvBankNotification.setText(messageRes)
        prepareOtp(binding.llBank.tvTimer, true)
        clearOtpBoxes()
        generateOTP(authenticationMethod)
    }


    /**
     * Hides the Terms & Conditions UI when the card is in a pending state.
     *
     * This is required for the yellow card pending activation flow,
     * where Terms & Conditions should not be shown.
     */
    private fun hideTermsAndConditionViewForPendingCard(status: String?) {
        if (status == PENDING) {
            binding.tvConditions.visibility = View.GONE
            binding.scrollView.visibility = View.GONE
        }
    }

    /**
     * Hides digitization / T&C UI while a card removal runs so the user does not briefly
     * see terms again between delete scripts or during [getPendingTask] sequence-counter reads.
     */
    private fun hideTermsUiForDeletionFlow() {
        binding.tvConditions.visibility = View.GONE
        binding.scrollView.visibility = View.GONE
        binding.llBank.root.visibility = View.GONE
        binding.llSelectOtp.root.visibility = View.GONE
        binding.llNickName.root.visibility = View.GONE
        binding.llSuccess.root.visibility = View.GONE
        binding.selectedDevice.root.visibility = View.GONE
    }

    /**
     * handleBackPressOnAcceptButtonClick() : It handles the hardware back button events.
     *
     */
    private fun handleBackPressOnAcceptButtonClick() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.llBank.root.isVisible) {
                        moveBackToOtpSelectionScreen()
                        return
                    }
                    if (binding.llNickName.root.isVisible) {
                        binding.llSuccess.btnDone.callOnClick()
                        return
                    }

                    checkCancelBtn = true
                    deleteCard(
                        requireActivity() as MainActivity,
                        DELETED_STATUS,
                        digitizationReferenceNumberCancel.toString()
                    )
                }
            }
        )
    }

    /**
     * moveBackToOtpSelectionScreen() : It navigates back to OTP selection screen.
     *
     */
    private fun moveBackToOtpSelectionScreen() {
        binding.llBank.etBox1.text?.clear()
        binding.llBank.etBox2.text?.clear()
        binding.llBank.etBox3.text?.clear()
        binding.llBank.etBox4.text?.clear()
        binding.llBank.etBox5.text?.clear()
        binding.llBank.etBox6.text?.clear()

        StorageRepository.saveString(PreferenceKey.OTP_EXPIRE_TIME, 0.toString())
        binding.llBank.root.visibility = View.GONE
        binding.llSelectOtp.root.visibility = View.VISIBLE
    }

    /**
     * Initiates the card digitization process using the SecoraWallet SDK.
     * This sends the card information to the SDK for digitization and handles callbacks.
     */
    private fun executeDigitizeCard() {
        lifecycleScope.launch {
            if (activity.isFinishing) return@launch

            activity.showLoading(true, getString(R.string.text_requesting_digital_card))
            val clientWalletAccountEmailAddress = StorageRepository.readString(PreferenceKey.EMAIL_ID)

            val digitizationRef = arguments?.getString(BundleKey.DIGITIZATION_REFERENCE_NUMBER).toString()
            val casdCert = if (pnoType() == PNO_VTS && !isNFC()) {
                CasdCertificateHelper.fetchCasdCertificate(activity.applicationContext, PNO_VTS)
            } else {
                null
            }
            if (pnoType() == PNO_VTS && !isNFC() && casdCert.isNullOrBlank()) {
                activity.showLoading(false, "")
                deleteDialog(getString(R.string.error_adding_mcm), digitizationRef)
                return@launch
            }

            val sdkResult = WalletRepository.digitizeCard(
                context = activity,
                paymentAppInstanceId = arguments?.getString(BundleKey.PAYMENT_APP_INSTANCE_ID).toString(),
                digitizationReferenceNumber = digitizationRef,
                clientWalletAccountEmailAddress = clientWalletAccountEmailAddress,
                sdScript = arguments?.getString(BundleKey.SD_SCRIPT),
                casdCert = casdCert,
            )

            handleDigitizeCardSdkResult(sdkResult)
        }
    }

    /**
     * Handles the wallet SDK digitize-card response and routes to success or failure flows.
     *
     * @param sdkResult Result wrapper from [WalletRepository.digitizeCard].
     */
    private fun handleDigitizeCardSdkResult(sdkResult: WalletSdkResult<DigitizeResponseBody>) {
        if (!sdkResult.isSuccess) {
            handleDigitizeFailure(sdkResult.errorMessage)
            return
        }
        val digitizeResponse = sdkResult.response ?: return
        if (SUCCESS != digitizeResponse.statusMessage) {
            handleDigitizeFailure(digitizeResponse.statusMessage)
            return
        }
        tokenRefNumber = digitizeResponse.digitizationReferenceNumber.toString()
        decision = digitizeResponse.decision.toString()
        handleDecisionBasedFlow(digitizeResponse)
    }

    /**
     * Determines the next flow based on the card digitization decision.
     */
    private fun handleDecisionBasedFlow(digitizeResponseBody : DigitizeResponseBody) {
        if (decision == "APPROVED") {
            scheduleNotifyProvision()
        } else {
            StorageRepository.saveString(PreferenceKey.THREE_DES_AUTH_TIME, LocalDateTime.now().toString())
            showOtpSelection(digitizeResponseBody)
        }
    }

    /**
     * Delays the notifyProvision API call by 12 seconds to ensure
     * backend systems have processed the digitization data.
     */
    private fun scheduleNotifyProvision() {
        CoroutineScope(Dispatchers.IO).launch {
            delay(12000)
            if (cardProvisionDataApiTriggered) return@launch
            cardProvisionDataApiTriggered = true
            if (!isNFC())
                fetchNotifyProvision()
            else fetchNFCNotifyProvision()
        }
    }

    /**
     * Displays the OTP selection UI on the main thread.
     * This allows the user to choose a step-up authentication method.
     */
    private fun showOtpSelection(digitizeResponseBody : DigitizeResponseBody) {
        requireActivity().runOnUiThread {
            activity.showLoading(false, "")
            binding.tvConditions.visibility = View.GONE
            binding.scrollView.visibility = View.GONE
            binding.llSelectOtp.root.visibility = View.VISIBLE
            setupAuthMethod(digitizeResponseBody.authenticationMethods?:arrayListOf())
        }
    }

    /**
     * Handles digitization failure scenarios.
     *
     * Runs the same provision-abort cleanup as Cancel (updateCardStatus plus delete scripts).
     * Navigation is handled by [finishTermsProvisionAbortCleanup] after cleanup completes.
     *
     * @param errorMessage Optional error message from the digitize API response.
     */
    private fun handleDigitizeFailure(errorMessage: String? = null) {
        PayHostFssSync.onAddCardFailed()
        checkCancelBtn = true
        val tokenRef = resolveProvisionAbortTokenRef()
        activity.showLoading(false, "")
        if (!errorMessage.isNullOrBlank()) {
            handleSessionOrError(errorMessage) {
                statusDialog(activity, errorMessage)
            }
        }
        if (tokenRef.isBlank()) {
            finishTermsProvisionAbortCleanup("")
            return
        }
        if (!isNFC()) {
            ensureBleConnectedThenRun(
                onConnected = {
                    deleteCard(
                        requireActivity() as MainActivity,
                        DELETED_STATUS,
                        tokenRef
                    )
                },
                onCancelled = {
                    abortProvisionCleanupWithoutBle(tokenRef)
                }
            )
        } else {
            ensureNfcReadyThenRun {
                deleteCard(
                    requireActivity() as MainActivity,
                    DELETED_STATUS,
                    tokenRef
                )
            }
        }
    }

    /**
     * Resolves the digitization reference number used for provision-abort cleanup.
     *
     * @return Token reference from fragment arguments, or the cancel-flow fallback value.
     */
    private fun resolveProvisionAbortTokenRef(): String {
        return arguments?.getString(BundleKey.DIGITIZATION_REFERENCE_NUMBER)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: digitizationReferenceNumberCancel.toString().trim()
    }

    /**
     * fetchNotifyProvision() : It handles the call of fetchProvision API
     *
     */
    private fun fetchNotifyProvision() {
        val safeContext = context ?: return

        paymentId?.let {
            tokenRefNumber?.let { it1 ->

                lifecycleScope.launch {
                    val sdkResult = WalletRepository.fetchProvisionedData(
                        context = safeContext,
                        paymentAppInstanceId = it,
                        digitizationReferenceNumber = it1
                    )
                    if (sdkResult.isSuccess) {
                        sdkResult.response?.let { notifyProvisionResponse ->
                            handleSuccessFlowProvisionedData(notifyProvisionResponse)
                        }
                    } else {
                        activity.showLoading(false, "")
                        handleSessionOrError(sdkResult.errorMessage)
                    }
                }
            }
        }
    }

    /**
     * Continues notify-provision when APDU commands are present; otherwise shows MCM failure UI.
     *
     * @param response Notify-provision response from the wallet SDK.
     */
    private fun handleSuccessFlowProvisionedData(response: NotifyProvisionResponse) {
        val commandGroups = getProvisionApduCommandGroups(response)
        if (commandGroups.isNotEmpty()) {
            executeScriptForProvisionedData(commandGroups)
        } else {
            activity.showLoading(false, "")
            logger.debug("Failed Add MCM else null")
            PayHostFssSync.onAddCardFailed()
            deleteDialog(
                getString(R.string.failed_to_add_mcm), tokenRefNumber.toString()
            )
        }
    }

    /**
     * Executes every notify-provision APDU command group over BLE, in API response order,
     * then posts aggregated APDU results back to the SDK.
     *
     * @param commandGroups Non-empty [ApduCommands] groups from notify-provision.
     */
    private fun executeScriptForProvisionedData(commandGroups: List<ApduCommands>) {
        val tokenRef = tokenRefNumber ?: return

        if (!isScriptRunning.compareAndSet(false, true)) {
            logger.debug("Script already running — ignoring duplicate call")
            return
        }

        logger.debug(
            "Notify provision BLE: starting ${commandGroups.size} script group(s)"
        )
        executeProvisionScriptAtIndexBle(
            commandGroups = commandGroups,
            scriptIndex = 0,
            tokenRef = tokenRef,
            accumulatedResponses = mutableListOf()
        )
    }

    /**
     * Runs one notify-provision APDU group, then continues to the next until all succeed.
     *
     * @param commandGroups Ordered list of APDU command groups to execute.
     * @param scriptIndex Zero-based index of the group currently executing.
     * @param tokenRef Digitization reference for failure UI.
     * @param accumulatedResponses Aggregated APDU responses across groups for notify-status.
     */
    private fun executeProvisionScriptAtIndexBle(
        commandGroups: List<ApduCommands>,
        scriptIndex: Int,
        tokenRef: String,
        accumulatedResponses: MutableList<ApduResponsesItem>
    ) {
        if (scriptIndex >= commandGroups.size) {
            isScriptRunning.set(false)
            StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)
            logger.debug(
                "Notify provision BLE: all ${commandGroups.size} script(s) succeeded, " +
                    "APDU responses: ${accumulatedResponses.size}"
            )
            callNotifyProvisionStatus(accumulatedResponses)
            return
        }

        val commandGroup = commandGroups[scriptIndex]
        val transformed = transformJsonBackFromBase64(commandGroup)
        if (transformed.isEmpty()) {
            isScriptRunning.set(false)
            StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)
            activity.showLoading(false, "")
            logger.debug(
                "Notify provision BLE: empty payload for script ${scriptIndex + 1}/${commandGroups.size}"
            )
            activity.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                handleNotifyProvisionScriptFailure(tokenRef)
            }
            return
        }

        StorageRepository.saveString(PreferenceKey.INSTALL_SCRIPT, transformed.contentToString())
        logger.debug(
            "Notify provision BLE: executing script ${scriptIndex + 1}/${commandGroups.size} " +
                "aid=${commandGroup.appletInstanceAID} lines=${commandGroup.apduLines?.size ?: 0}"
        )

        val scriptHandler = ScriptHandler(
            requireContext(),
            object : ScriptHandler.Callbacks {
                override fun showLoading(show: Boolean, msg: String) {
                    // Loading is owned by the fragment notify-provision flow.
                }

                override fun showToast(message: String) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }

                override fun updateLogs(message: String) {
                    logger.debug("ScriptHandler$message")
                }
            }
        )

        scriptHandler.executeScript(transformed)
            .thenAccept { executionResult ->
                if (executionResult.success) {
                    accumulatedResponses += executionResult.apduResults.map { apduResult ->
                        ApduResponsesItem(
                            apduCommandId = apduResult.apduCommandId,
                            apduCommandResponse = apduResult.apduCommandResponse
                        )
                    }
                    logger.debug(
                        "Notify provision BLE: script ${scriptIndex + 1}/${commandGroups.size} success"
                    )
                    executeProvisionScriptAtIndexBle(
                        commandGroups = commandGroups,
                        scriptIndex = scriptIndex + 1,
                        tokenRef = tokenRef,
                        accumulatedResponses = accumulatedResponses
                    )
                } else {
                    isScriptRunning.set(false)
                    StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)
                    logger.debug(
                        "Notify provision BLE: script ${scriptIndex + 1}/${commandGroups.size} failed"
                    )
                    activity.runOnUiThread {
                        if (!isAdded) return@runOnUiThread
                        handleNotifyProvisionScriptFailure(tokenRef)
                    }
                }
            }
            .exceptionally { throwable ->
                isScriptRunning.set(false)
                StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)
                activity.showLoading(false, "")
                logger.debug(
                    "Notify provision BLE: script ${scriptIndex + 1}/${commandGroups.size} " +
                        "exception: ${throwable?.message}"
                )
                activity.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    handleNotifyProvisionScriptFailure(tokenRef, throwable)
                }
                null
            }
    }

    /**
     * Cleans up state and shows the MCM failure dialog after notify-provision script execution fails.
     * BLE transport loss (e.g. could not write 'Request' characteristic) uses the same alert as
     * [CardListFragment.handleAddMCMFailure]; all other failures show [R.string.failed_to_add_mcm].
     *
     * @param tokenRef Digitization reference number for provision-abort cleanup.
     * @param throwable Optional script execution error from the exceptionally callback.
     */
    private fun handleNotifyProvisionScriptFailure(
        tokenRef: String,
        throwable: Throwable? = null
    ) {
        isScriptRunning.set(false)
        StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)
        activity.showLoading(false, "")
        logger.debug(
            "Failed Add MCM: throwable=${throwable?.message}, " +
                "BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}"
        )
        if (!isNFC() && ScriptHandler.isBleTransportError(throwable)) {
            showProvisionScriptBleConnectionLostDialog {
                abortProvisionCleanupWithoutBle(tokenRef)
            }
            return
        }
        PayHostFssSync.onAddCardFailed()
        deleteDialog(getString(R.string.failed_to_add_mcm), tokenRef)
    }

    /**
     * Sends notify-provision status with executed APDU responses to the wallet SDK.
     *
     * @param apduResponses APDU command IDs and status words from script execution.
     */
    private fun callNotifyProvisionStatus(apduResponses: List<ApduResponsesItem>) {
        CoroutineScope(Dispatchers.IO).launch {
            if (activity.isFinishing) return@launch

            val sdkResult = WalletRepository.notifyProvisionStatus(
                context = activity,
                paymentAppInstanceId = paymentId!!,
                digitizationReferenceNumber = tokenRefNumber!!,
                apduResponses = apduResponses
            )

            if (sdkResult.isSuccess) {
                sdkResult.response?.let { notifyProvisionStatusResponse ->
                    logger.debug("NotifyProvisionResponse Response apduCommands: ${notifyProvisionStatusResponse.apduCommands.toString()}")
                    logger.debug("notify Response ${notifyProvisionStatusResponse.statusMessage.toString()}")

                    activity.runOnUiThread {
                        activity.showLoading(false, "")
                        binding.loader.root.visibility = View.GONE
                        binding.tvConditions.visibility = View.GONE
                        binding.scrollView.visibility = View.GONE
                        binding.llNickName.root.visibility = View.VISIBLE
                        PayHostFssSync.onAddCardSuccess()
                    }
                }
            } else {
                activity.showLoading(false, "")
                PayHostFssSync.onAddCardFailed()
                handleSessionOrError(sdkResult.errorMessage)
            }
        }
    }

    /**
     * validationForBankAuth(): It handles the otp validation for bank auth
     *
     */
    private fun validationForBankAuth() {
        val etBox1 = binding.llBank.etBox1
        val etBox2 = binding.llBank.etBox2
        val etBox3 = binding.llBank.etBox3
        val etBox4 = binding.llBank.etBox4
        val etBox5 = binding.llBank.etBox5
        val etBox6 = binding.llBank.etBox6

        authCode =
            etBox1.text.toString().trim() + etBox2.text.toString().trim() + etBox3.text.toString()
                .trim() + etBox4.text.toString().trim() + etBox5.text.toString()
                .trim() + etBox6.text.toString().trim()

        etBox1.transformationMethod = AsteriskPasswordTransformationMethod()
        etBox2.transformationMethod = AsteriskPasswordTransformationMethod()
        etBox3.transformationMethod = AsteriskPasswordTransformationMethod()
        etBox4.transformationMethod = AsteriskPasswordTransformationMethod()
        etBox5.transformationMethod = AsteriskPasswordTransformationMethod()
        etBox6.transformationMethod = AsteriskPasswordTransformationMethod()

        UIHelper.setupOtpInput(etBox1, etBox2)
        UIHelper.setupOtpBoxes(listOf(etBox1, etBox2, etBox3, etBox4, etBox5, etBox6))
    }

    /**
     * verifyOtp(): It verifies the entered otp by calling verifyActivationCode API
     *
     */
    private fun verifyOTP(otp: String) {
        authCode?.let {

            lifecycleScope.launch {
                if (activity.isFinishing) return@launch
                val sdkResult = WalletRepository.verifyOTP(
                    context = activity,
                    paymentAppInstanceId = paymentId.toString(),
                    referenceNumber = tokenRefNumber.toString(),
                    authCode = otp
                )
                if (sdkResult.isSuccess) {
                    sdkResult.response?.let { verifyActivationResponse ->
                        handleVerifyOtpResponseFlow(verifyActivationResponse)
                    }
                } else {
                    activity.showLoading(false, "")
                    activity.showVerifyingBankLoading(false)
                    handleSessionOrError(sdkResult.errorMessage) {
                        activity.runOnUiThread {
                            handleVerifyOtpErrors(sdkResult.errorMessage)
                        }
                    }
                }
            }
        }
    }

    /**
     * Continues the bank-auth flow after OTP verification succeeds or fails.
     *
     * @param verifyActivationResponse Wallet SDK verify-activation response.
     */
    private fun handleVerifyOtpResponseFlow(verifyActivationResponse : VerifyActivationResponse) {
        if (verifyActivationResponse.statusMessage.isNullOrEmpty()) {
            activity.showLoading(false, "")
            activity.showVerifyingBankLoading(false)
            return
        }
        if (verifyActivationResponse.statusCode == SUCCESS_VALUE) {
            handleVerifyOtpSuccessResponseFlow(12000)
        } else {
            activity.showLoading(false, "")
            activity.showVerifyingBankLoading(false)
            handleVerifyOtpErrors(verifyActivationResponse.statusMessage)
        }
    }

    /**
     * Handles the error usecase of verify otp response.
     *
     */
    private fun handleVerifyOtpErrors(message : String? = "") {
        Toast.makeText(activity,message, Toast.LENGTH_SHORT).show()
        binding.llSelectOtp.root.visibility = View.GONE
        binding.llBank.root.visibility = View.VISIBLE
        clearOtpBoxes()
    }

    /**
     * Schedules notify-provision after successful OTP or 3DS verification.
     */
    private fun handleVerifyOtpSuccessResponseFlow(timeMillis: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(timeMillis)
            if (cardProvisionDataApiTriggered) return@launch
            cardProvisionDataApiTriggered = true

            if (!isNFC())
                fetchNotifyProvision()
            else fetchNFCNotifyProvision()
        }
    }

    /**
     * Generates a One-Time Password (OTP) for the ongoing digitization process.
     * This method calls the SecoraWallet SDK to generate the OTP linked to the current payment ID.
     * It handles both success and error callbacks to update the UI accordingly.
     */
    private fun generateOTP(authenticationMethod: AuthenticationMethod) {
        authCode?.let {
            lifecycleScope.launch {
                if (activity.isFinishing) return@launch
                val sdkResult = WalletRepository.generateOTP(
                    context = activity,
                    paymentAppInstanceId = paymentId.toString(),
                    referenceNumber = tokenRefNumber.toString(),
                    authenticationMethod = authenticationMethod,
                )
                if (sdkResult.isSuccess) {
                    sdkResult.response?.let { activationResponse ->
                        handleGenerateOtpResponseFlow(activationResponse)
                    }
                } else {
                    activity.showVerifyingBankLoading(false)
                    handleSessionOrError(sdkResult.errorMessage) {
                        Toast.makeText(activity,sdkResult.errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * Shows the OTP entry UI after a successful generate-OTP SDK response.
     *
     * @param activationResponse Wallet SDK activation response for the selected method.
     */
    private fun handleGenerateOtpResponseFlow(activationResponse : GetActivationResponse) {
        if (activationResponse.statusMessage.isNullOrEmpty()) {
            activity.showVerifyingBankLoading(false)
            return
        }
        if (activationResponse.statusCode != SUCCESS_VALUE) {
            Toast.makeText(activity,activationResponse.statusMessage, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Decodes a Base64-encoded image string and displays it inside the card image view.
     * Used for showing the wearable card image after digitization or provisioning.
     *
     * @param image The Base64-encoded image string from backend or SDK response.
     */
    fun getWearableImage(image: String) {
        try {
            val imageBytes = Base64.decode(image, Base64.DEFAULT)
            Glide.with(requireContext())
                .load(imageBytes)
                .into(binding.selectedDevice.cardImageView)
        } catch (e: IllegalArgumentException) {
            logger.noStackTraceLog("GetWearableImage ", e)
        }
    }

    /**
     * Fetches any pending card-related tasks (e.g., suspend/delete operations) from the backend.
     *
     * This method checks whether the card has any pending actions that need user acknowledgment or system processing.
     * It makes two variants of API calls based on whether the `digitizeref` is available or not.
     *
     * @param seId The Secure Element ID of the connected device.
     * @param digitizeRef The digitization reference number of the card. If empty or "null", an alternate API is used.
     *
     * - Shows loading while fetching tasks.
     * - On success, processes any `deleteScriptList` from response.
     * - If tasks exist, invokes [acknowledgePendingTask] with the first scriptId and digitizeRef.
     * - On error or empty response, stops the loading and optionally shows a message.
     */
    private fun getPendingTask(seId: String, digitizeRef: String, status: String) {

        fetchSequenceNumberFromDevice(
            onRetrieved = { currentSequenceCounter ->
                logger.debug("currentSequenceCounter : $currentSequenceCounter")
                handleGetPendingTaskFlow(seId, digitizeRef, status, currentSequenceCounter)
            },
            onFailed = {

            })

    }

    /**
     * Processes the wallet SDK pending-task response for terms-screen delete or cleanup flows.
     *
     * @param response      Pending-task response from the wallet SDK.
     * @param seId          Secure element ID for the linked device.
     * @param digitizeRef   Digitization reference number for the card.
     * @param status        Current card or operation status.
     */
    private fun handleGetPendingTaskFlow(
        seId: String,
        digitizeRef: String,
        status: String,
        currentSequenceCounter: String
    ) {
        lifecycleScope.launch {
            val sdkResult = WalletRepository.getPendingTask(
                context = activity,
                seId = seId,
                digitizationReferenceNumber = sanitizeDigitizeRef(digitizeRef),
                currentSequenceCounter = currentSequenceCounter
            )
            if (sdkResult.isSuccess) {
                sdkResult.response?.let { getPendingResponse ->
                    if (getPendingResponse.statusMessage.isNullOrEmpty()) return@launch
                    activity.runOnUiThread {
                        processPendingTaskResponse(getPendingResponse, seId, status)
                    }
                }
            } else {
                handleSessionOrError(sdkResult.errorMessage)
            }
        }
    }

    /**
     * Sanitizes the digitization reference string by checking for blank or invalid values.
     *
     * @param digitizeRef The digitization reference number as a string.
     * @return The sanitized reference number, or null if it’s blank or equals "null".
     */
    private fun sanitizeDigitizeRef(digitizeRef: String): String? {
        return if (digitizeRef.isBlank() || digitizeRef.equals("null", ignoreCase = true)) {
            null
        } else {
            digitizeRef
        }
    }

    /**
     * Processes the `GetPendingResponse` from the SDK.
     * Determines the next step based on the response’s status message.
     *
     * @param response The pending task response.
     * @param seId Secure Element ID.
     * @param status Current status of the card or operation.
     */
    private fun processPendingTaskResponse(
        response: GetPendingResponse,
        seId: String,
        status: String
    ) {
        when (response.statusMessage) {
            CommonResponse.SUCCESS.response -> {
                handleSuccessfulPendingTask(response, seId, status)
            }

            else -> {
                logger.info("Response status message: ${response.statusMessage}")
            }
        }
    }

    /**
     * Handles a successful pending task response when delete scripts are available.
     * Extracts the script details and acknowledges the task to complete the flow.
     *
     * @param response The successful pending task response.
     * @param seId Secure Element ID.
     * @param status Current task status.
     */
    private fun handleSuccessfulPendingTask(
        response: GetPendingResponse,
        seId: String,
        status: String
    ) {
        if (response.deleteScriptList.isNotEmpty()) {
            val scriptId = response.deleteScriptList[0].scriptId
            val digitizeRef = response.deleteScriptList[0].digitizationReferenceNumber
            scriptId?.let { scriptIdInt ->
                acknowledgePendingTask(seId, scriptIdInt, digitizeRef.toString(), status)
            }
        }
    }

    /**
     * Acknowledges a pending task received from [getPendingTask] for a card.
     *
     * This confirms that the pending operation (like deletion) can be completed, as per server request.
     *
     * @param seId The Secure Element ID of the connected device.
     * @param scriptId The script identifier provided by backend indicating which operation to acknowledge.
     * @param digitizeRef The digitization reference number of the card being operated upon.
     *
     * - Sends the acknowledgment to backend via SDK.
     * - Handles success and failure via [UiCallback].
     * - If response is successful, proceed silently.
     * - Otherwise, shows a user-friendly message.
     */
    private fun acknowledgePendingTask(
        seId: String,
        scriptId: Int,
        digitizeRef: String,
        status: String
    ) {
        lifecycleScope.launch {
            if (activity.isFinishing) return@launch
            val sdkResult = WalletRepository.acknowledgePendingTask(
                context = activity,
                seId = seId,
                scriptId = scriptId,
                digitizeRef = digitizeRef,
                status = status
            )
            if (sdkResult.isSuccess) {
                sdkResult.response?.let { acknowledgeResponse ->
                    when (acknowledgeResponse.statusMessage) {
                        CommonResponse.SUCCESS.response -> {
                            // Success case - no action needed
                        }

                        else -> {
                            logger.info("Response status message: ${acknowledgeResponse.statusMessage}")
                            // Handle other response cases
                        }
                    }
                }
            } else {
                handleSessionOrError(sdkResult.errorMessage)
            }
        }
    }

    /**
     * Returns every non-empty APDU command group from the notify-provision response,
     * preserving backend order so all scripts (1, 2, 3, ...) can be executed sequentially.
     *
     * @param response Notify-provision response from the wallet SDK.
     * @return List of [ApduCommands] groups that contain at least one APDU line.
     */
    private fun getProvisionApduCommandGroups(
        response: NotifyProvisionResponse
    ): List<ApduCommands> {
        return response.apduCommands
            .orEmpty()
            .filter { !it.apduLines.isNullOrEmpty() }
    }

    /**
     * Transforms one [ApduCommands] group into the JSON byte array consumed by
     * [com.infineon.secora.wallet.utils.scripthandler.ScriptHandler.executeScript].
     *
     * @param apduCommands One applet APDU command group from notify-provision.
     * @return A UTF-8 encoded JSON byte array ready for SE script execution.
     */
    fun transformJsonBackFromBase64(apduCommands: ApduCommands?): ByteArray {
        if (apduCommands == null || apduCommands.apduLines.isNullOrEmpty()) {
            logger.debug("apduCommands is null or empty, returning empty byte array")
            return ByteArray(0)
        }

        val scriptData = Gson().toJson(apduCommands)
        val input = JSONObject(scriptData)
        val apduList = input.getJSONArray(JsonKey.APDU_LINES)
        val newArray = JSONArray()

        for (i in 0 until apduList.length()) {
            val item = apduList.getJSONObject(i)
            val newItem = JSONObject().apply {
                put(JsonKey.APDU_SEQ_NO, i + 1)
                put(JsonKey.APDU, item.optString(JsonKey.APDU_COMMAND))
                put(JsonKey.COMMAND_DESCRIPTION, "")
            }
            newArray.put(newItem)
        }

        return JSONObject().apply {
            put(JsonKey.APDU_LIST, newArray)
        }.toString().toByteArray(Charsets.UTF_8)
    }

    /**
     * Deletes a digitized card from the wallet both locally and through the SecoraWallet SDK.
     * This method updates the card’s status (e.g., DELETED or DEVICE_STOLEN) and handles
     * retry logic for the delete script execution.
     *
     * @param activity The parent [MainActivity] context for UI operations.
     * @param status The new status for the card (e.g., "DELETED").
     * @param tokenRefNumber The digitization reference number identifying the card.
     */
    fun deleteCard(activity: MainActivity, status: String, tokenRefNumber: String) {
        alertDialog?.dismiss()
        val trimmedRef = tokenRefNumber.trim()
        if (status == DELETED_STATUS && trimmedRef.isNotEmpty()) {
            DigitizationDeleteFlowGate.markTermsClientDeleteStarted(trimmedRef)
        }
        activity.showLoading(true, activity.getString(R.string.text_please_wait))
        if (status == DELETED_STATUS && trimmedRef.isNotEmpty() && isAdded) {
            hideTermsUiForDeletionFlow()
        }

        fetchSequenceNumberForProvisionAbort(
            onRetrieved = { currentSequenceCounter ->
                logger.debug("currentSequenceCounter : $currentSequenceCounter")
                handleUpdateCardStatusFLow(
                    activity, status, tokenRefNumber, currentSequenceCounter, skipSeScriptsWhenDisconnected = false
                )
            },
            onProceedWithoutBle = {
                abortProvisionCleanupWithoutBle(tokenRefNumber, status, activity)
            }
        )
    }

    /**
     * Marks the card deleted on the backend without running SE delete scripts or acknowledgements
     * when BLE is unavailable (same navigation as terms Cancel).
     */
    private fun abortProvisionCleanupWithoutBle(
        tokenRefNumber: String,
        status: String = DELETED_STATUS,
        hostActivity: MainActivity = requireActivity() as MainActivity
    ) {
        if (!isAdded) return
        DigitizationDeleteFlowGate.clearTermsClientDelete()
        checkCancelBtn = true
        hostActivity.showLoading(true, hostActivity.getString(R.string.text_please_wait))
        lifecycleScope.launch {
            val counter = SequenceCounterHelper.resolveSequenceCounter(hostActivity)
            handleUpdateCardStatusFLow(
                hostActivity, status, tokenRefNumber, counter, skipSeScriptsWhenDisconnected = true
            )
        }
    }

    /**
     * Starts update-card-status cleanup by fetching pending delete scripts for the card.
     *
     * @param tokenRefNumber Digitization reference number for provision abort cleanup.
     * @param status         Current card status passed from the cancel/abort flow.
     */
    private fun handleUpdateCardStatusFLow(
        activity: MainActivity,
        status: String,
        tokenRefNumber: String,
        currentSequenceCounter: String,
        skipSeScriptsWhenDisconnected: Boolean = false
    ) {

        // Run database operation and API call on background thread to avoid UI lag.
        // Use [activity] for Context, not [requireContext]: the BLE/script callback can resume
        // after the fragment is detached, in which case requireContext() throws.
        lifecycleScope.launch(AppDispatchers.IO) {
            if (activity.isFinishing) return@launch

            val selectedCard = StorageRepository.getLocalCardListForStatusApi(
                activity,
                tokenRefNumber,
                paymentId.toString()
            )
            val deviceDetails = DeviceDetails(
                connected = isNFC() || BluetoothStateManager.isConnected,
                currentSequenceCounter = currentSequenceCounter
            )

            val sdkResult = WalletRepository.updateCardStatus(
                context = activity,
                cardStatus = status,
                cardList = selectedCard,
                paymentAppInstanceId = paymentId.toString(),
                digitizationReferenceNumber = tokenRefNumber,
                pnoType = pnoType(),
                deviceDetails = deviceDetails
            )

            if (sdkResult.isSuccess) {
                handleUpdateCardStatusResponse(
                    sdkResult.response,
                    tokenRefNumber,
                    skipSeScriptsWhenDisconnected
                )
            } else {
                activity.runOnUiThread {
                    if (activity.isFinishing) return@runOnUiThread
                    if (skipSeScriptsWhenDisconnected) {
                        finishTermsProvisionAbortCleanup(tokenRefNumber)
                        return@runOnUiThread
                    }
                    DigitizationDeleteFlowGate.clearTermsClientDelete()
                    activity.showLoading(false, "")
                    handleSessionOrError(sdkResult.errorMessage) {
                        statusDialog(activity, sdkResult.errorMessage)
                    }
                }
            }
        }
    }


    /**
     * Executes delete scripts returned by update-card-status and acknowledges each step.
     *
     * @param response        Update-card-status response containing delete scripts.
     * @param tokenRefNumber  Digitization reference number for the card being cleaned up.
     * @param scriptIndex     Index of the current delete script in the response list.
     */
    private fun handleUpdateCardStatusResponse(
        updateCardStatusResponse: UpdateCardStatusResponse?,
        tokenRefNumber: String,
        skipSeScriptsWhenDisconnected: Boolean = false
    ) {
        if (updateCardStatusResponse == null) {
            if (skipSeScriptsWhenDisconnected) {
                finishTermsProvisionAbortCleanup(tokenRefNumber)
                return
            }
            DigitizationDeleteFlowGate.clearTermsClientDelete()
            activity.showLoading(false, "")
            return
        }

        when (updateCardStatusResponse.statusMessage) {
            CommonResponse.SUCCESS.response -> {
                if (skipSeScriptsWhenDisconnected || shouldSkipSeDeleteScriptAndAcknowledgement()) {
                    finishTermsProvisionAbortCleanup(tokenRefNumber)
                    return
                }
                // Execute each delete script from updateCardStatus sequentially (BLE or NFC)
                if (!isNFC()) {
                    executeDeleteScriptWithRetry(
                        updateCardStatusResponse, activity, tokenRefNumber, scriptIndex = 0, attemptIndex = 0
                    )
                } else {
                    executeDeleteScriptWithRetryNfc(
                        updateCardStatusResponse, activity, tokenRefNumber, scriptIndex = 0, attemptIndex = 0
                    )
                }
            }

            else -> {
                if (skipSeScriptsWhenDisconnected) {
                    finishTermsProvisionAbortCleanup(tokenRefNumber)
                    return
                }
                DigitizationDeleteFlowGate.clearTermsClientDelete()
                activity.showLoading(false, "")
            }
        }
    }

    /**
     * Runs main-thread follow-up after all terms-screen delete scripts complete.
     *
     * @param activity       Host activity for loading UI and pending-task refresh.
     * @param tokenRefNumber Digitization reference number for provision-abort cleanup.
     * @param response       Update-card-status response that owned the delete scripts.
     */
    private fun termsOnDeleteScriptListCompleteOnMainThread(
        activity: MainActivity,
        tokenRefNumber: String,
        response: UpdateCardStatusResponse
    ) {
        activity.runOnUiThread {
            activity.showLoading(true, activity.getString(R.string.text_please_wait))
            getPendingTask(seId.toString(), tokenRefNumber, SUCCESS)
            if (isAdded) handleDeleteScriptResult(true, response, activity, tokenRefNumber)
            else DigitizationDeleteFlowGate.clearTermsClientDelete()
        }
    }

    /**
     * Creates a [ScriptHandler] configured for terms-screen delete-script execution.
     *
     * @param activity Host activity for BLE script callbacks.
     * @return Script handler used for delete-script execution on the terms screen.
     */
    private fun createTermsDeleteScriptHandler(activity: MainActivity): ScriptHandler {
        return ScriptHandler(
            activity,
            object : ScriptHandler.Callbacks {
                override fun showLoading(show: Boolean, msg: String) {
                    // Empty callback to avoid conflicts with centralized loading
                }

                override fun showToast(message: String) {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                }

                override fun updateLogs(message: String) {
                    logger.debug("ScriptHandler: $message")
                }
            }
        )
    }

    /**
     * Handles successful BLE delete-script completion on the terms screen.
     *
     * @param success        Whether the delete script reported success.
     * @param activity       Host activity for loading UI and follow-up API calls.
     * @param response       Update-card-status response containing remaining scripts.
     * @param deleteList     Delete scripts returned by update-card-status.
     * @param scriptIndex    Index of the script that just completed.
     * @param attemptIndex   Zero-based attempt index for the current script.
     * @param tokenRefNumber Digitization reference number for the card.
     */
    private fun handleTermsBleDeleteHandlerPostedWork(
        success: Boolean,
        activity: MainActivity,
        response: UpdateCardStatusResponse,
        deleteList: List<DeleteScriptBase>,
        scriptIndex: Int,
        attemptIndex: Int,
        tokenRefNumber: String
    ) {
        StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)

        if (success) {
            if (!shouldSkipSeDeleteScriptAndAcknowledgement()) {
                acknowledgeSingleUpdateCardDeleteScript(deleteList[scriptIndex], tokenRefNumber)
            }
            val next = scriptIndex + 1
            if (next < deleteList.size) {
                activity.showLoading(true, activity.getString(R.string.text_deleting_card))
                executeDeleteScriptWithRetry(response, activity, tokenRefNumber, next, 0)
            } else {
                activity.showLoading(true, activity.getString(R.string.text_please_wait))
                getPendingTask(seId.toString(), tokenRefNumber, SUCCESS)
                if (isAdded) {
                    handleDeleteScriptResult(true, response, activity, tokenRefNumber)
                } else {
                    DigitizationDeleteFlowGate.clearTermsClientDelete()
                }
            }
            return
        }
        activity.showLoading(false, "")
        getPendingTask(seId.toString(), tokenRefNumber, FAILED)
        handleDeleteScriptResult(
            false, response, activity, tokenRefNumber, scriptIndex, attemptIndex
        )
    }

    /**
     * Handles BLE delete-script failures on the terms screen, including one retry per script.
     *
     * @param throwable      Error thrown by delete-script execution.
     * @param attemptIndex   Zero-based attempt index for the current script.
     * @param response       Update-card-status response containing delete scripts.
     * @param activity       Host activity for dialogs and loading UI.
     * @param tokenRefNumber Digitization reference number for the card.
     * @param scriptIndex    Index of the failed script.
     */
    private fun handleTermsBleDeleteScriptExceptionally(
        throwable: Throwable,
        attemptIndex: Int,
        response: UpdateCardStatusResponse,
        activity: MainActivity,
        tokenRefNumber: String,
        scriptIndex: Int
    ) {
        logger.debug("delete card: Exception=${throwable.message} (attempt ${attemptIndex + 1})")
        StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)

        if (attemptIndex == 0) {
            handleTheScriptExecutionRepeatedRetryFlow(
                response, activity, tokenRefNumber, scriptIndex, attemptIndex = 1
            )
            return
        }
        logger.debug("delete card: Final failure after retry")
        logger.debug(
            "delete card: Exception=${throwable.message}, " +
                "BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}"
        )
        activity.showLoading(false, "")
        DigitizationDeleteFlowGate.clearTermsClientDelete()
        statusDialog(
            activity,
            activity.getString(R.string.text_unable_to_delete)
        )
        getPendingTask(seId.toString(), tokenRefNumber, SUCCESS_MESSAGE)
        if (isAdded) {
            navigateToPaymentFragment()
        }
    }

    /**
     * Handles successful NFC delete-script completion on the terms screen.
     *
     * @param response       Update-card-status response containing remaining scripts.
     * @param tokenRefNumber Digitization reference number for the card.
     * @param scriptIndex    Index of the script that just completed.
     */
    private fun handleTermsNfcDeleteSuccessPostedWork(
        activity: MainActivity,
        response: UpdateCardStatusResponse,
        deleteList: List<DeleteScriptBase>,
        scriptIndex: Int,
        tokenRefNumber: String
    ) {
        StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)

        if (!shouldSkipSeDeleteScriptAndAcknowledgement()) {
            acknowledgeSingleUpdateCardDeleteScript(deleteList[scriptIndex], tokenRefNumber)
        }
        val next = scriptIndex + 1
        if (next < deleteList.size) {
            activity.showLoading(true, activity.getString(R.string.text_deleting_card))
            executeDeleteScriptWithRetryNfc(response, activity, tokenRefNumber, next, 0)
        } else {
            activity.showLoading(true, activity.getString(R.string.text_please_wait))
            getPendingTask(seId.toString(), tokenRefNumber, SUCCESS)
            if (isAdded) {
                handleDeleteScriptResult(true, response, activity, tokenRefNumber)
            } else {
                DigitizationDeleteFlowGate.clearTermsClientDelete()
            }
        }
    }

    /**
     * Handles NFC delete-script errors on the terms screen.
     *
     * @param error          Error message from NFC script execution.
     * @param response       Update-card-status response containing delete scripts.
     * @param tokenRefNumber Digitization reference number for the card.
     * @param scriptIndex    Index of the failed script.
     * @param attemptIndex   Zero-based attempt index for the current script.
     */
    private fun handleTermsNfcDeleteOnError(
        activity: MainActivity,
        response: UpdateCardStatusResponse,
        deleteList: List<DeleteScriptBase>,
        scriptIndex: Int,
        attemptIndex: Int,
        tokenRefNumber: String,
        error: String
    ) {
        hideNfcSheet()
        logger.debug(
            "delete card NFC: Error=$error (script ${scriptIndex + 1}/${deleteList.size}, attempt ${attemptIndex + 1})"
        )
        StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)

        if (attemptIndex == 0) {
            logger.debug("First NFC attempt failed, retrying same script...")
            executeDeleteScriptWithRetryNfc(
                response,
                activity,
                tokenRefNumber,
                scriptIndex,
                1
            )
            return
        }
        logger.debug("delete card NFC: Final failure")
        activity.showLoading(false, "")
        DigitizationDeleteFlowGate.clearTermsClientDelete()

        statusDialog(
            activity,
            activity.getString(R.string.text_unable_to_delete)
        )

        getPendingTask(
            seId.toString(),
            tokenRefNumber,
            SUCCESS_MESSAGE
        )

        if (isAdded) {
            navigateToPaymentFragment()
        }
    }

    /**
     * Executes delete scripts from [updateCardStatus] response sequentially (one list entry at a time),
     * with one BLE retry per script ([attemptIndex] 0 then 1).
     */
    private fun executeDeleteScriptWithRetry(
        response: UpdateCardStatusResponse,
        activity: MainActivity,
        tokenRefNumber: String,
        scriptIndex: Int,
        attemptIndex: Int
    ) {
        val deleteList = response.deleteScriptList.orEmpty()
        if (deleteList.isEmpty()) {
            termsOnDeleteScriptListCompleteOnMainThread(activity, tokenRefNumber, response)
            return
        }

        if (scriptIndex >= deleteList.size) {
            termsOnDeleteScriptListCompleteOnMainThread(activity, tokenRefNumber, response)
            return
        }

        val jsonBytes = extractJsonBytes1(response, scriptIndex)
        StorageRepository.saveString(PreferenceKey.INSTALL_SCRIPT, jsonBytes.contentToString())

        if (jsonBytes == null || jsonBytes.isEmpty()) {
            activity.runOnUiThread {
                handleDeleteScriptFailure(response, activity, tokenRefNumber, scriptIndex, attemptIndex)
            }
            return
        }

        val scriptHandler = createTermsDeleteScriptHandler(activity)
        activity.showLoading(true, activity.getString(R.string.text_deleting_card))

        scriptHandler.deleteScript(jsonBytes).thenAccept { success ->
            activity.runOnUiThread {
                logger.debug(
                    "delete card: Success=$success (script ${scriptIndex + 1}/${deleteList.size}, attempt ${attemptIndex + 1})"
                )

                Handler(Looper.getMainLooper()).post {
                    handleTermsBleDeleteHandlerPostedWork(
                        success,
                        activity,
                        response,
                        deleteList,
                        scriptIndex,
                        attemptIndex,
                        tokenRefNumber
                    )
                }
            }
        }.exceptionally { throwable ->
            activity.runOnUiThread {
                handleTermsBleDeleteScriptExceptionally(
                    throwable ?: Exception("unknown"),
                    attemptIndex,
                    response,
                    activity,
                    tokenRefNumber,
                    scriptIndex
                )
            }
            null
        }
    }

    /**
     * Retries the same delete script once before finalizing terms-screen delete failure.
     *
     * @param response       Update-card-status response containing delete scripts.
     * @param tokenRefNumber Digitization reference number for the card.
     * @param scriptIndex    Index of the script to retry.
     */
    private fun handleTheScriptExecutionRepeatedRetryFlow(
        response: UpdateCardStatusResponse,
        activity: MainActivity,
        tokenRefNumber: String,
        scriptIndex: Int,
        attemptIndex: Int
    ) {
        logger.debug("First attempt failed, retrying same script...")
        if (!isNFC()) {
            executeDeleteScriptWithRetry(response, activity, tokenRefNumber, scriptIndex, attemptIndex)
        } else {
            executeDeleteScriptWithRetryNfc(response, activity, tokenRefNumber, scriptIndex, attemptIndex)
        }
    }

    /**
     * Handles the result of a delete script execution.
     * If successful, navigate back to the Payment screen.
     * Otherwise, retries or handles failure depending on the attempt index.
     */
    private fun handleDeleteScriptResult(
        success: Boolean,
        response: UpdateCardStatusResponse,
        activity: MainActivity,
        tokenRefNumber: String,
        scriptIndex: Int = 0,
        attemptIndex: Int = 0
    ) {
        if (success) {
            if (!isAdded) return

            lifecycleScope.launch {
                StorageRepository.removeNicknameForCard(requireContext(), paymentId.toString(), tokenRefNumber)
                navigateToPaymentFragment()
            }
        } else {
            handleDeleteScriptFailure(response, activity, tokenRefNumber, scriptIndex, attemptIndex)
        }
    }

    /**
     * Handles a failed delete script execution.
     * Retries the operation once for the same script; if the second attempt also fails,
     * it shows an error dialog and triggers pending task retrieval.
     */
    private fun handleDeleteScriptFailure(
        response: UpdateCardStatusResponse,
        activity: MainActivity,
        tokenRefNumber: String,
        scriptIndex: Int,
        attemptIndex: Int
    ) {
        if (attemptIndex == 0) {
            logger.debug("First attempt failed, retrying...")
            if (!isNFC()) {
                executeDeleteScriptWithRetry(response, activity, tokenRefNumber, scriptIndex, 1)
            } else {
                executeDeleteScriptWithRetryNfc(response, activity, tokenRefNumber, scriptIndex, 1)
            }
        } else {
            logger.debug("Delete card: Final failure after retry")
            StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)

            DigitizationDeleteFlowGate.clearTermsClientDelete()
            statusDialog(
                activity,
                activity.getString(R.string.text_unable_to_delete)
            )
            getPendingTask(seId.toString(), tokenRefNumber, SUCCESS_MESSAGE)
            navigateToPaymentFragment()
        }
    }

    /**
     * Extracts and decodes JSON byte data from a Base64-encoded delete script
     * in an [UpdateCardStatusResponse].
     *
     * @param scriptIndex Index into [UpdateCardStatusResponse.deleteScriptList] for multi-step deletes.
     */
    private fun extractJsonBytes1(response: UpdateCardStatusResponse, scriptIndex: Int): ByteArray? {
        return try {
            val scriptData = response.deleteScriptList?.getOrNull(scriptIndex)?.scriptData
            if (scriptData.isNullOrEmpty()) {
                logger.debug("scriptData is null or empty at index $scriptIndex")
                return null
            }

            ScriptDataParser.decodeToJsonBytes(scriptData)?.also {
                val decodedString = String(it, Charsets.UTF_8)
                logger.debug("Decoded scriptData -> $decodedString")
            }
        } catch (e: Exception) {
            logger.noStackTraceLog("extractJsonBytes1 ", e)
            null
        }
    }

    /**
     * Completes terms-screen provision-abort cleanup and navigates away from the flow.
     *
     * @param tokenRefNumber Digitization reference number for the aborted card.
     */
    private fun finishTermsProvisionAbortCleanup(tokenRefNumber: String) {
        DigitizationDeleteFlowGate.clearTermsClientDelete()
        activity.showLoading(false, "")
        lifecycleScope.launch {
            val counter = SequenceCounterHelper.resolveSequenceCounter(activity)
            handleGetPendingTaskFlow(seId.toString(), tokenRefNumber, SUCCESS_MESSAGE, counter)
            if (isAdded) {
                navigateToPaymentFragment()
            }
        }
    }

    /**
     * Navigates to the payment fragment after successful terms-screen card provisioning.
     */
    private fun navigateToPaymentFragment() {
        if (!isAdded) return
        if (findNavController().currentDestination?.id != R.id.termsFragment) return
        try {
            val nav = findNavController()
            nav.getBackStackEntry(R.id.cardListFragment).savedStateHandle[
                DigitizationDeleteFlowGate.POST_TERMS_DELETE_REFRESH_KEY
            ] = true
            nav.popBackStack()
        } catch (e: Exception) {
            logger.debug("Navigation error: ${e.message}")
        }
    }

    /**
     * Displays a confirmation dialog before deleting a card.
     * The dialog only shows an OK button (no cancel) and executes [deleteCard] upon confirmation.
     *
     * @param message The message displayed inside the dialog.
     * @param digitizationReferenceNumberDelete The reference number of the card to delete.
     */
    fun deleteDialog(
        message: String,
        digitizationReferenceNumberDelete: String
    ) {
        requireActivity().runOnUiThread {
            val currentActivity = requireActivity()
            if (!currentActivity.isFinishing) {
                val dialogViewBinding = DialogCommonMessageBinding.inflate(layoutInflater)
                val alertDialog = Dialog(requireContext()).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setContentView(dialogViewBinding.root)
                    setCancelable(false)
                }

                dialogViewBinding.txtTitle.text = getString(R.string.text_secora_wallet)
                dialogViewBinding.txtMessage.text = message

                // Hide the cancel button
                dialogViewBinding.txtCancel.visibility = View.GONE

                // Center the OK button by adjusting its layout parameters
                dialogViewBinding.txtOK.let { okButton ->
                    val layoutParams = okButton.layoutParams
                    if (layoutParams is LinearLayout.LayoutParams) {
                        layoutParams.gravity = Gravity.CENTER_HORIZONTAL
                        layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
                        layoutParams.setMargins(0, 0, 0, 0)
                        okButton.layoutParams = layoutParams
                    }

                    // Set text alignment to center
                    okButton.gravity = Gravity.CENTER
                }

                dialogViewBinding.txtOK.setOnClickListener {
                    deleteCard(
                        requireActivity() as MainActivity,
                        DELETED_STATUS, digitizationReferenceNumberDelete
                    )
                    alertDialog.dismiss()
                }

                alertDialog.showSecure()
            }
        }
    }

    /**
     * Displays a generic non-cancelable alert dialog with a title and message.
     *
     * @param title The dialog title (e.g., app name or status).
     * @param message The dialog body text.
     */
    fun showAlertDialog(title: String, message: String) {
        (activity as? Activity)?.runOnUiThread {
            val dialogViewBinding = DialogCommonMessageBinding.inflate(layoutInflater)
            val alertDialog = Dialog(requireContext()).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(dialogViewBinding.root)
                setCancelable(false)
            }

            dialogViewBinding.txtTitle.text = title
            dialogViewBinding.txtMessage.text = message
            dialogViewBinding.txtCancel.visibility = View.GONE

            dialogViewBinding.txtOK.setOnClickListener {
                alertDialog.dismiss()
            }
            alertDialog.showSecure()
        }
    }

    /**
     * Checks if a given nickname already exists for the same payment app instance.
     *
     * @param nickname The nickname to check for duplication.
     * @param paymentAppInstanceId The payment app instance ID used to scope the nickname.
     * @return True if the nickname already exists for this instance, false otherwise.
     */
    private suspend fun isNicknameDuplicate(nickname: String, paymentAppInstanceId: String, cardId: String): Boolean {
        return StorageRepository.isNicknameDuplicate(
            context = requireContext(),
            nickname = nickname,
            paymentAppInstanceId = paymentAppInstanceId,
            cardId = cardId
        )
    }

    /**
     * Acknowledges one executed delete script from [UpdateCardStatusResponse] (multi-step delete).
     */
    private fun acknowledgeSingleUpdateCardDeleteScript(script: DeleteScriptBase, tokenRefNumber: String) {
        try {
            script.scriptId?.let { scriptId ->
                val digitizeRef = script.digitizationReferenceNumber
                    ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    ?: tokenRefNumber
                acknowledgePendingTask(
                    seId.toString(),
                    scriptId,
                    digitizeRef,
                    SUCCESS_MESSAGE
                )
            }
        } catch (e: Exception) {
            logger.debug("Error acknowledging delete script success: ${e.message}")
        }
    }

    /**
     * Lifecycle callback:
     * Registers Bluetooth UI state listener when Fragment becomes visible.
     */
    override fun onStart() {
        super.onStart()
        BluetoothUiStateManager.register(requireContext()) {
            if (isAdded) {
                updateBluetoothIconState()
            }
        }
    }

    /**
     * Lifecycle callback:
     * Unregisters Bluetooth UI listener to avoid leaks.
     */
    override fun onStop() {
        super.onStop()
        BluetoothUiStateManager.unregister {
            updateBluetoothIconState()
        }
    }

    /**
     * Updates Bluetooth icon: green when BT is on and the selected device (header) is connected, black when BT off or that device disconnected.
     */
    private fun updateBluetoothIconState() {
        val selectedSeId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)
        val showConnected = BluetoothStateManager.isBluetoothTurnedOn(requireContext())
            && BluetoothStateManager.isDeviceConnected(selectedSeId, requireContext())
        val iconRes = if (isNFC())
            R.drawable.icon_nfc
        else if (showConnected) {
            R.drawable.ic_bluetooth_connected
        } else {
            R.drawable.ic_bluetooth_disconnected
        }
        binding.selectedDevice.imgBluetooth.setImageResource(iconRes)
        binding.selectedDevice.imgBluetooth.clearColorFilter()
    }

    /**
     * Clears all OTP input fields and resets the focus to the first OTP box.
     *
     * This method removes any existing text from the six OTP input fields
     * and ensures the cursor is placed back in the first field for fresh input.
     */
    private fun clearOtpBoxes() {
        val otpBoxes = listOf(
            binding.llBank.etBox1,
            binding.llBank.etBox2,
            binding.llBank.etBox3,
            binding.llBank.etBox4,
            binding.llBank.etBox5,
            binding.llBank.etBox6
        )

        otpBoxes.forEach { it.text?.clear() }
        otpBoxes.first().requestFocus()
    }

    /**
     * Fetches notify-provision data for NFC wearables and continues the MCM script flow.
     */
    private fun fetchNFCNotifyProvision() {
        val safeContext = context ?: return

        paymentId?.let {
            tokenRefNumber?.let { it1 ->

                lifecycleScope.launch {
                    val sdkResult = WalletRepository.fetchProvisionedData(
                        context = safeContext,
                        paymentAppInstanceId = it,
                        digitizationReferenceNumber = it1
                    )
                    if (sdkResult.isSuccess) {
                        sdkResult.response?.let { notifyProvisionResponse ->
                            handleSuccessFlowNFCProvisionedData(notifyProvisionResponse)
                        }
                    } else {
                        activity.showLoading(false, "")
                        handleSessionOrError(sdkResult.errorMessage)
                    }
                }
            }
        }
    }

    /**
     * Continues NFC notify-provision when APDU commands are present; otherwise shows MCM failure UI.
     *
     * @param response Notify-provision response from the wallet SDK.
     */
    private fun handleSuccessFlowNFCProvisionedData(response: NotifyProvisionResponse) {
        val commandGroups = getProvisionApduCommandGroups(response)
        if (commandGroups.isNotEmpty()) {
            executeScriptForNFCProvisionedData(commandGroups)
        } else {
            activity.showLoading(false, "")
            logger.debug("Failed Add MCM else null")
            deleteDialog(
                getString(R.string.failed_to_add_mcm), tokenRefNumber.toString()
            )
        }
    }

    /**
     * Executes every notify-provision APDU group over NFC (API list order), then posts combined results.
     *
     * @param commandGroups Non-empty APDU command groups from notify-provision.
     */
    private fun executeScriptForNFCProvisionedData(commandGroups: List<ApduCommands>) {
        if (!isScriptRunning.compareAndSet(false, true)) {
            logger.debug("NFC script already running — ignoring duplicate call")
            return
        }
        NfcScriptExecutionTracker.onNfcScriptStarted()
        showNfcSheet(parentFragmentManager, onCancelClick = {
            deleteCard(
                requireActivity() as MainActivity,
                DELETED_STATUS,
                digitizationReferenceNumberCancel.orEmpty()
            )
        })
        executeProvisionScriptAtIndexNfc(
            commandGroups = commandGroups,
            scriptIndex = 0,
            accumulatedResponses = mutableListOf()
        )
    }

    /**
     * Runs one notify-provision NFC script group, then continues with the next group on success.
     */
    private fun executeProvisionScriptAtIndexNfc(
        commandGroups: List<ApduCommands>,
        scriptIndex: Int,
        accumulatedResponses: MutableList<ApduResponsesItem>
    ) {
        val group = commandGroups.getOrNull(scriptIndex)
        if (group == null) {
            isScriptRunning.set(false)
            NfcScriptExecutionTracker.onNfcScriptFinished()
            StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)
            requireActivity().runOnUiThread { hideNfcSheet() }
            activity.showLoading(false, "")
            deleteDialog(getString(R.string.failed_to_add_mcm), tokenRefNumber.toString())
            return
        }

        val nfcApduResponses = mutableListOf<ApduResponsesItem>()
        val transformed = transformJsonBackFromBase64(group)
        if (transformed.isEmpty()) {
            isScriptRunning.set(false)
            NfcScriptExecutionTracker.onNfcScriptFinished()
            StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)
            requireActivity().runOnUiThread {
                hideNfcSheet()
                activity.showLoading(false, "")
                logger.debug(
                    "Notify provision NFC: empty payload for script " +
                        "${scriptIndex + 1}/${commandGroups.size}"
                )
                deleteDialog(
                    getString(R.string.failed_to_add_mcm),
                    tokenRefNumber.toString()
                )
            }
            return
        }

        StorageRepository.saveString(PreferenceKey.INSTALL_SCRIPT, transformed.contentToString())
        logger.debug(
            "Notify provision NFC: executing script ${scriptIndex + 1}/${commandGroups.size} " +
                "aid=${group.appletInstanceAID} lines=${group.apduLines?.size ?: 0}"
        )

        SecoraWearableSDK.getInstance().getInterface().executeNfcOperation(
            requireActivity(),
            SCRIPT,
            transformed,
            null,
            null,
            object : ScriptExecutionCallback {
                override fun onSeidDetected(
                    seid: String?,
                    tagId: String?,
                    icTypeHex: String?,
                    oemIdHex: String?,
                    seGroupIdHex: String?,
                    wearableModelIdHex: String?
                ) {
                    handleNfcCplcCallback(seid, tagId, icTypeHex, oemIdHex, seGroupIdHex, wearableModelIdHex)
                    logger.debug("SE_ID: $seid")
                }

                override fun onApduProgress(request: String?, response: String?) {
                    logger.debug("APDU REQUEST=$request \n RESPONSE=$response")
                    if (!request.isNullOrBlank() && !response.isNullOrBlank()) {
                        nfcApduResponses.add(
                            ApduResponsesItem(
                                apduCommandId = (accumulatedResponses.size + nfcApduResponses.size + 1).toString(),
                                apduCommandResponse = response
                            )
                        )
                    }
                }

                override fun onSuccess(
                    responseItems: List<com.infineon.secora.wearable.apdu.ApduResponsesItem>?,
                    completed: Boolean
                ) {
                    if (!completed) {
                        logger.debug("Provision NFC progress callback received, waiting for completion")
                        return
                    }
                    val apduResponses =
                        responseItems.orEmpty().map { sdkItem ->
                            ApduResponsesItem(
                                apduCommandId = sdkItem.apduCommandId,
                                apduCommandResponse = sdkItem.apduCommandResponse
                            )
                        }.ifEmpty {
                            nfcApduResponses.toList()
                        }
                    accumulatedResponses.addAll(apduResponses)
                    logger.debug(
                        "Notify provision NFC script ${scriptIndex + 1}/${commandGroups.size} success; " +
                            "APDU responses this script=${apduResponses.size}, total=${accumulatedResponses.size}"
                    )

                    val nextIndex = scriptIndex + 1
                    if (nextIndex < commandGroups.size) {
                        executeProvisionScriptAtIndexNfc(
                            commandGroups = commandGroups,
                            scriptIndex = nextIndex,
                            accumulatedResponses = accumulatedResponses
                        )
                        return
                    }

                    isScriptRunning.set(false)
                    NfcScriptExecutionTracker.onNfcScriptFinished()
                    StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)
                    requireActivity().runOnUiThread { hideNfcSheet() }
                    callNFCNotifyProvisionStatus(accumulatedResponses.toList())
                }

                override fun onError(error: String?) {
                    isScriptRunning.set(false)
                    NfcScriptExecutionTracker.onNfcScriptFinished()
                    StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)
                    if (::activity.isInitialized && !activity.isFinishing) {
                        activity.runOnUiThread {
                            hideNfcSheet()
                            activity.showLoading(false, "")
                            logger.debug(
                                "Failed Add MCM NFC script ${scriptIndex + 1}/${commandGroups.size}: $error"
                            )
                            deleteDialog(
                                getString(R.string.failed_to_add_mcm),
                                tokenRefNumber.toString()
                            )
                        }
                    }
                }
            })
    }

    /**
     * Sends NFC notify-provision status with executed APDU responses to the wallet SDK.
     *
     * @param apduResponses APDU command IDs and status words from NFC script execution.
     */
    private fun callNFCNotifyProvisionStatus(apduResponses: List<ApduResponsesItem>) {
        CoroutineScope(Dispatchers.IO).launch {
            if (activity.isFinishing) return@launch

            val sdkResult = WalletRepository.notifyProvisionStatus(
                context = activity,
                paymentAppInstanceId = paymentId!!,
                digitizationReferenceNumber = tokenRefNumber!!,
                apduResponses = apduResponses
            )
            if (sdkResult.isSuccess) {
                sdkResult.response?.let { notifyProvisionStatusResponse ->
                    logger.debug("NotifyProvisionResponse Response apduCommands: ${notifyProvisionStatusResponse.apduCommands.toString()}")
                    logger.debug("notify Response ${notifyProvisionStatusResponse.statusMessage.toString()}")

                    activity.runOnUiThread {
                        activity.showLoading(false, "")
                        binding.loader.root.visibility = View.GONE
                        binding.tvConditions.visibility = View.GONE
                        binding.scrollView.visibility = View.GONE
                        binding.llNickName.root.visibility = View.VISIBLE
                        PayHostFssSync.onAddCardSuccess()
                    }
                }
            } else {
                activity.showLoading(false, "")
                PayHostFssSync.onAddCardFailed()
                handleSessionOrError(sdkResult.errorMessage)
            }
        }
    }

    /**
     * NFC: runs each entry in [UpdateCardStatusResponse.deleteScriptList] sequentially,
     * with one retry per script ([attemptIndex] 0 then 1).
     */
    private fun executeDeleteScriptWithRetryNfc(
        response: UpdateCardStatusResponse,
        activity: MainActivity,
        tokenRefNumber: String,
        scriptIndex: Int,
        attemptIndex: Int
    ) {
        val deleteList = response.deleteScriptList.orEmpty()
        if (deleteList.isEmpty()) {
            termsOnDeleteScriptListCompleteOnMainThread(activity, tokenRefNumber, response)
            return
        }

        if (scriptIndex >= deleteList.size) {
            termsOnDeleteScriptListCompleteOnMainThread(activity, tokenRefNumber, response)
            return
        }

        val jsonBytes = extractJsonBytes1(response, scriptIndex)
        StorageRepository.saveString(PreferenceKey.INSTALL_SCRIPT, jsonBytes.contentToString())

        if (jsonBytes == null || jsonBytes.isEmpty()) {
            activity.runOnUiThread {
                handleDeleteScriptFailure(response, activity, tokenRefNumber, scriptIndex, attemptIndex)
            }
            return
        }

        activity.showLoading(true, activity.getString(R.string.text_deleting_card))

        showNfcSheet(parentFragmentManager, onCancelClick = {
            navigateToPaymentFragment()
        })
        NfcScriptExecutionTracker.onNfcScriptStarted()
        SecoraWearableSDK.getInstance().getInterface().executeNfcOperation(
            requireActivity(),
            DELETE_SCRIPT,
            jsonBytes,
            null,
            null,
            object : ScriptExecutionCallback {
                override fun onSeidDetected(
                    seid: String?,
                    tagId: String?,
                    icTypeHex: String?,
                    oemIdHex: String?,
                    seGroupIdHex: String?,
                    wearableModelIdHex: String?
                ) {
                    handleNfcCplcCallback(seid, tagId, icTypeHex, oemIdHex, seGroupIdHex, wearableModelIdHex)
                }

                override fun onApduProgress(request: String?, response: String?) {
                    logger.debug("APDU REQUEST=$request \n RESPONSE=$response")
                }

                override fun onSuccess(
                    responseItems: MutableList<com.infineon.secora.wearable.apdu.ApduResponsesItem>?,
                    completed: Boolean
                ) {
                    if (!completed) {
                        logger.debug("Delete NFC progress callback received, waiting for completion")
                        return
                    }
                    NfcScriptExecutionTracker.onNfcScriptFinished()
                    if (!isAdded) {
                        DigitizationDeleteFlowGate.clearTermsClientDelete()
                        return
                    }
                    activity.runOnUiThread {
                        hideNfcSheet()
                        Handler(Looper.getMainLooper()).post {
                            handleTermsNfcDeleteSuccessPostedWork(
                                activity,
                                response,
                                deleteList,
                                scriptIndex,
                                tokenRefNumber
                            )
                        }
                    }
                }

                override fun onError(error: String) {
                    NfcScriptExecutionTracker.onNfcScriptFinished()
                    activity.runOnUiThread {
                        handleTermsNfcDeleteOnError(
                            activity,
                            response,
                            deleteList,
                            scriptIndex,
                            attemptIndex,
                            tokenRefNumber,
                            error
                        )
                    }
                }
            })
    }
}
