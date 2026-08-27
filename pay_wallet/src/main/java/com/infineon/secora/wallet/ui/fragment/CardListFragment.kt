// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: CardListFragment.kt managing the list of provisioned payment cards associated with a device.
 * It integrates deeply with the SecoraWalletSDK and
 * the local database to ensure real-time synchronization between stored card data, the backend, and the user interface.
 **/
package com.infineon.secora.wallet.ui.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.BuildConfig
import com.infineon.secora.wallet.adapter.AddCardAdapter
import com.infineon.secora.wallet.adapter.AddCardAdapterDb
import com.infineon.secora.wallet.cdcvm.BodyPresenceTracker
import com.infineon.secora.wallet.cdcvm.CdcvmApi
import com.infineon.secora.wallet.cdcvm.WearableStatus
import com.infineon.secora.wallet.cdcvm.WearableStatusChips
import com.infineon.secora.wallet.cdcvm.WearableStatusMonitor
import com.infineon.secora.wallet.client.data.models.AcknowledgeResponse
import com.infineon.secora.wallet.client.data.models.DeleteScriptBase
import com.infineon.secora.wallet.client.data.models.DeleteScriptResponse
import com.infineon.secora.wallet.client.data.models.GetPendingResponse
import com.infineon.secora.wallet.client.data.models.capturecard.CapturedCardDetail
import com.infineon.secora.wallet.client.data.models.card.common.CardDetails
import com.infineon.secora.wallet.client.data.models.common.CardList
import com.infineon.secora.wallet.client.data.models.common.CheckEligibilityResponseBody
import com.infineon.secora.wallet.client.data.models.common.DeleteScript
import com.infineon.secora.wallet.client.data.models.common.DeleteScriptList
import com.infineon.secora.wallet.client.data.models.common.FetchAssetResponseBody
import com.infineon.secora.wallet.client.data.models.common.GetProvisionCardResponse
import com.infineon.secora.wallet.client.data.models.common.UpdateCardStatusResponse
import com.infineon.secora.wallet.client.data.models.prepse.PrepSeResponseBody
import com.infineon.secora.wallet.client.data.models.prepse.ScriptItem
import com.infineon.secora.wallet.client.operations.middleware.callbacks.UiCallback
import com.infineon.secora.wallet.client.util.AppDispatchers
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.DialogCommonMessageBinding
import com.infineon.secora.wallet.databinding.FragmentWalletCardListBinding
import com.infineon.secora.wallet.utils.helper.showSecure
import com.infineon.secora.wallet.domain.walletsdk.WalletRepository
import com.infineon.secora.wallet.domain.walletsdk.WalletSdkResult
import com.infineon.secora.wallet.domain.walletsdk.isInvalid
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothUiStateManager
import com.infineon.secora.wallet.domain.wearable.ble.script.ScriptExecutionResult
import com.infineon.secora.wallet.domain.wearable.ble.script.ScriptHandler
import com.infineon.secora.wallet.firebase.AppEvent
import com.infineon.secora.wallet.firebase.EventBus
import com.infineon.secora.wallet.PayHostFssSync
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.ui.fragment.WearableSettingFragment.Companion.isCardDeletionInProgress
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.utils.CommonResponse
import com.infineon.secora.wallet.utils.helper.CasdCertificateHelper
import com.infineon.secora.wallet.utils.helper.DigitizationDeleteFlowGate
import com.infineon.secora.wallet.utils.helper.SequenceCounterHelper
import com.infineon.secora.wallet.utils.helper.FcmSecureFlowCoordinator
import com.infineon.secora.wallet.utils.ImageUtils
import com.infineon.secora.wallet.utils.helper.NfcScriptExecutionTracker
import com.infineon.secora.wallet.utils.helper.PendingDeleteScriptExecutionGate
import com.infineon.secora.wallet.utils.helper.PendingDeleteTaskResponseHelper
import com.infineon.secora.wallet.utils.helper.ScriptDataParser
import com.infineon.secora.wallet.utils.helper.SecureElementScriptCoordinator
import com.infineon.secora.wallet.utils.Utils
import com.infineon.secora.wallet.utils.Utils.isNetworkAvailable
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.constants.Constants
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_CARD
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_DEVICE_STATUS_UPDATE
import com.infineon.secora.wallet.utils.constants.Constants.DEFAULT_CARD_CHANGE
import com.infineon.secora.wallet.utils.constants.Constants.DELETED_CARD
import com.infineon.secora.wallet.utils.constants.Constants.DELETED_STATUS
import com.infineon.secora.wallet.utils.constants.Constants.ONBOARDING_FETCH_INSTALL_SCRIPT_0002
import com.infineon.secora.wallet.utils.constants.Constants.PENDING
import com.infineon.secora.wallet.utils.constants.Constants.PNO_MDES
import com.infineon.secora.wallet.utils.constants.Constants.PNO_VTS
import com.infineon.secora.wallet.utils.constants.Constants.PPSE
import com.infineon.secora.wallet.utils.constants.Constants.REQUIRE_ADDITIONAL_AUTHENTICATION
import com.infineon.secora.wallet.utils.constants.Constants.DELETE_SCRIPT
import com.infineon.secora.wallet.utils.constants.Constants.NO_PENDING_TASK
import com.infineon.secora.wallet.utils.constants.Constants.SCRIPT
import com.infineon.secora.wallet.utils.constants.Constants.SCRIPT_DATA_NULL_OR_EMPTY
import com.infineon.secora.wallet.utils.constants.Constants.SUCCESS_MESSAGE
import com.infineon.secora.wallet.utils.constants.Constants.TAG_85
import com.infineon.secora.wallet.utils.constants.Constants.TAG_86
import com.infineon.secora.wallet.utils.constants.Constants.TOKEN_IS_ALREADY_IN_DELETED_STATE
import com.infineon.secora.wallet.utils.constants.JsonKey
import com.infineon.secora.wearable.SecoraWearableSDK
import com.infineon.secora.wearable.apdu.ApduResponsesItem
import com.infineon.secora.wearable.cdcvm.CvmState
import com.infineon.secora.wearable.nfc.ScriptExecutionCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/**
 * CardListFragment: This class is used for displaying the list of cards
 *
 */
class CardListFragment : BaseFragment() {

    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)
    private lateinit var binding: FragmentWalletCardListBinding
    private var nonDefaultCardCount = 0
    private var paymentAppInstanceId: String? = null
    private var cardList: MutableList<CardList> = ArrayList()
    private var cardListDetail: List<CardDetails> = ArrayList()
    private lateinit var activity: MainActivity
    private val imageMap: MutableMap<String?, Bitmap?> = mutableMapOf()
    private var isBleFlowInProgress = false
    private var isNavigatingToAddCard = false

    private var scriptIdleObserverJob: Job? = null

    /** Shared status pills (Bluetooth / on-body / payment). */
    private lateinit var statusChips: WearableStatusChips

    /** Shared poller driving the pills, the passcode CTA and the setup gate. */
    private val statusMonitor = WearableStatusMonitor(
        onUpdate = ::onWearableStatus,
        canPoll = { !isTransportOwnedByFlow() },
        attemptOnboard = true
    )

    /** Latest body presence, used by the Verify CTA's off-body gate. */
    private var lastPresence: BodyPresenceTracker.Presence = BodyPresenceTracker.Presence.UNKNOWN

    /** True while no wearable passcode is provisioned: the card list and add-card are gated off. */
    private var passcodeSetupRequired = false
    private var devicelocked = false

    /** Thread-safe: card art is written from asset-fetch IO and read on the main thread in the list adapter. */
    private var dbImageMap: MutableMap<String, Bitmap> = ConcurrentHashMap()
    private lateinit var adapter: AddCardAdapter
    private var seId: String? = null
    private var isDeletingFromPortal = false
    private var scriptDSEM = false
    private var digitizationReferenceNumber: String = ""
    private var isDeleteFlow = false

    companion object {
        var isCardAdded: Boolean = false
        var shouldForceApiRefresh = false
        private var pendingAutoPromoteRemainingAfterDefaultDelete = false

        /**
         * Clears the stored default-card token when [deletedTokenRef] was the default.
         * When the wallet had exactly two cards before deletion, marks the remaining card for
         * optional auto-promotion on the next card-list refresh.
         *
         * @return `true` when the saved default reference was cleared.
         */
        fun onDefaultCardDeleted(deletedTokenRef: String, cardCountBeforeDelete: Int): Boolean {
            if (deletedTokenRef.isBlank() || deletedTokenRef.equals("null", ignoreCase = true)) return false
            val paymentId = StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID).trim()
            if (paymentId.isEmpty()) return false
            val deviceKey = PreferenceKey.deviceKey(paymentId)
            val savedDefaultRef = StorageRepository.readString(deviceKey).trim()
            if (savedDefaultRef.isEmpty() ||
                !savedDefaultRef.equals(deletedTokenRef.trim(), ignoreCase = true)
            ) {
                return false
            }
            StorageRepository.clearString(deviceKey)
            shouldForceApiRefresh = true
            if (cardCountBeforeDelete == 2) {
                pendingAutoPromoteRemainingAfterDefaultDelete = true
            }
            return true
        }

        /**
         * Consumes and returns whether the next card-list refresh should auto-promote the sole
         * remaining card after the default card was deleted from a two-card wallet.
         */
        private fun consumePendingAutoPromoteRemainingAfterDefaultDelete(): Boolean {
            val pending = pendingAutoPromoteRemainingAfterDefaultDelete
            pendingAutoPromoteRemainingAfterDefaultDelete = false
            return pending
        }
    }

    /**
     * Job for collecting events from EventBus.
     */
    private var eventCollectorJob: Job? = null
    private var cardDetails: CapturedCardDetail? = null
    private var getPending: Boolean = false

    private var getPendingTaskFromPrepSeFailure = false;
    private var prepSeFailureStatusMessage = "";
    private var isNotificationPending = false

    /** True from Add Payment Card tap until [resetAddCardFlowState]; independent of [isNotificationPending]. */
    private var isAddCardFlowActive = false

    private var backgroundPendingDeleteJob: Job? = null

    /** While eligibility / asset fetch runs, ignore FCM card refreshes (replaces former routing flag). */
    private var suppressFcmCardRefresh = false

    /**
     * Returns true when an FCM detach/delete flow or eligibility fetch owns the loader
     * and this fragment must not dismiss it or refresh from stale FCM events.
     */
    private fun isFcmSecureFlowControllingLoader(): Boolean =
        suppressFcmCardRefresh ||
            FcmSecureFlowCoordinator.isFlowInProgress() ||
            FcmSecureFlowCoordinator.isLoaderHoldActive()

    /** True while add-card provisioning (pending task, capture, prepSE, or install script) owns the loader. */
    private fun isAddCardLoaderOwner(): Boolean =
        isAddCardFlowActive || isAddCardProvisioningFlow()

    /** Hides the loader only when no secure or add-card flow is controlling it. */
    private fun dismissLoaderIfAllowed() {
        if (!isFcmSecureFlowControllingLoader() && !isAddCardLoaderOwner()) {
            activity.showLoading(false, "")
        }
    }

    /**
     * Handles refresh-related events from EventBus.
     *
     * - Safely ignores events if the fragment is not attached.
     * - Executes UI updates on the main thread.
     * - Handles deleted card events by fetching card details,
     *   determining the PNO type, and triggering card deletion.
     * - Refreshes the view after processing the event.
     */
    private fun handleRefreshEvent(event: AppEvent) {
        if (!isAdded) {
            logger.info("Fragment not attached to activity, ignoring event")
            return
        }
        if (suppressFcmCardRefresh) {
            return
        }

        try {
            val msgType = event.getStringExtra(BundleKey.MSG_TYPE)
            if (::activity.isInitialized && !activity.isFinishing) {
                handleMessageType(msgType)
            }
        } catch (e: IndexOutOfBoundsException) {
            logger.debug("Exception :- $e")
        }
    }

    /**
     * Handles non-deleted-card FCM message types by refreshing the card list UI.
     * Card-deleted events are handled centrally by [com.infineon.secora.wallet.domain.devicedetach.FcmDeletedCardHandler].
     *
     * @param msgType Message type from the FCM payload.
     */
    private fun handleMessageType(msgType: String?) {
        if (!::activity.isInitialized || !isAdded) return

        if (msgType == null || msgType == "null") {
            return
        }

        if (msgType == DELETED_CARD) {
            // Handled centrally by MainActivity → FcmDeletedCardHandler (all screens).
            return
        }

        initView()
    }

    /**
     * Card was already removed on the SE (e.g. Device Detach ran delete scripts first).
     * Used by manual delete flow only — no error dialog.
     */
    private suspend fun handleCardAlreadyDeletedOnSe(activity: MainActivity, tokenRefNumber: String) {
        logger.debug("CardListFragment: no pending delete task — card already removed on SE")
        isDeleteFlow = false
        isDeletingFromPortal = false
        activity.showLoading(false, "")
        if (tokenRefNumber.isNotBlank() && !tokenRefNumber.equals("null", ignoreCase = true)) {
            clearSavedDefaultIfDeleted(tokenRefNumber)
            StorageRepository.deleteLocalCardByDigitizeRef(activity, tokenRefNumber)
        }
        shouldForceApiRefresh = true
    }

    /**
     * Handles default card change events.
     * When triggered, it refreshes the RecyclerView adapter to update the UI.
     */
    private fun handleDefaultCardChange() {
        logger.debug("Default card changed - refreshing adapter")
        shouldForceApiRefresh = true
        initView()
    }

    /**
     * onViewCreated(): Initialize the view elements and set up listeners
     *
     * @param view
     * @param savedInstanceState
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbarProfile(requireActivity() as AppCompatActivity)
        updateBluetoothIconState()
        observePostDeleteRefreshRequests()
        statusChips = WearableStatusChips(binding.statusChips)
        statusMonitor.attach(viewLifecycleOwner, requireContext())
    }

    /**
     * Renders the shared status pills and drives the passcode CTA, setup gate, connect affordance and
     * "+" add-card visibility from each polled [status] (the shared monitor replaces the old
     * per-screen refresh loop). Onboarding, when a device has no passcode, is handled by the monitor
     * ([WearableStatusMonitor.attemptOnboard]).
     */
    private fun onWearableStatus(status: WearableStatus) {
        if (!::binding.isInitialized || !::statusChips.isInitialized) return
        lastPresence = status.presence

        // NFC: CDCVM is meaningless, so hide the pills and CTA but keep add-card available.
        if (status.isNfc) {
            statusChips.setVisible(false)
            binding.btnPasscodeCta.visibility = View.GONE
            binding.btnPasscodeCta.setOnClickListener(null)
            binding.imgAddCard.visibility = View.VISIBLE
            setStatusChipsConnectEnabled(false)
            return
        }

        statusChips.setVisible(true)
        statusChips.render(status)

        if (!status.connected) {
            // Bluetooth off / disconnected: hide the CTA and "+"; tapping the pills runs connect.
            binding.btnPasscodeCta.visibility = View.GONE
            binding.btnPasscodeCta.setOnClickListener(null)
            binding.imgAddCard.visibility = View.GONE
            setStatusChipsConnectEnabled(true)
            return
        }

        // Connected: the pills are status only; drive the CTA + setup gate from the CVM state.
        setStatusChipsConnectEnabled(false)
        updatePasscodeCta(status.cvmState)
    }

    /** True while a secure / add-card / connect flow owns the transport; the periodic read must wait. */
    private fun isTransportOwnedByFlow(): Boolean =
        isBleFlowInProgress || isAddCardLoaderOwner() || isFcmSecureFlowControllingLoader() ||
            SecureElementScriptCoordinator.isScriptRunning()

    /**
     * One-shot refresh after Terms client delete or FCM card-deleted scripts (while this screen is visible).
     */
    private fun observePostDeleteRefreshRequests() {
        val entry = try {
            findNavController().getBackStackEntry(R.id.cardListFragment)
        } catch (_: IllegalArgumentException) {
            return
        }
        val handle = entry.savedStateHandle

        handle.getLiveData<Boolean>(
            DigitizationDeleteFlowGate.POST_TERMS_DELETE_REFRESH_KEY
        ).observe(viewLifecycleOwner) { shouldRefresh ->
            if (shouldRefresh == true) {
                handle[DigitizationDeleteFlowGate.POST_TERMS_DELETE_REFRESH_KEY] = false
                refreshCardListFromServer()
            }
        }

        handle.getLiveData<Boolean>(
            DigitizationDeleteFlowGate.POST_FCM_CARD_DELETED_REFRESH_KEY
        ).observe(viewLifecycleOwner) { shouldRefresh ->
            if (shouldRefresh == true) {
                handle[DigitizationDeleteFlowGate.POST_FCM_CARD_DELETED_REFRESH_KEY] = false
                refreshCardListFromServer()
            }
        }
    }

    /**
     * Clears stale local cache and reloads provisioned cards from the API.
     */
    private fun refreshCardListFromServer() {
        if (!isAdded) return
        if (isFcmSecureFlowControllingLoader() || isAddCardLoaderOwner()) {
            logger.debug("Deferring refreshCardListFromServer until secure or add-card flow completes")
            shouldForceApiRefresh = true
            if (isFcmSecureFlowControllingLoader()) {
                scheduleRefreshWhenFcmFlowIdle()
            }
            return
        }

        shouldForceApiRefresh = false
        lifecycleScope.launch {
            StorageRepository.clearAllLocalCardData(requireContext(), clearUserNicknames = false)
        }

        initView()
    }

    /** Retries [refreshCardListFromServer] once serialized FCM flows (e.g. Device Detach) finish. */
    private fun scheduleRefreshWhenFcmFlowIdle() {
        viewLifecycleOwner.lifecycleScope.launch {
            var attempts = 0
            while (isAdded && isFcmSecureFlowControllingLoader() && attempts < 120) {
                delay(100)
                attempts++
            }
            if (!isAdded || !shouldForceApiRefresh) return@launch
            if (isFcmSecureFlowControllingLoader()) {
                logger.debug("Card list refresh still blocked by FCM flow after wait")
                return@launch
            }
            logger.debug("Card list refresh resuming after FCM secure flow completed")
            refreshCardListFromServer()
        }
    }

    /**
     * onCreateView(): It handles to inflate of the layout
     * refresh functionality with swipe refresh layout is also handled
     * If card is present in DB then show the list from DB,
     * or, if card is not present in DB ie if DB is empty then show the list from API
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletCardListBinding.inflate(inflater, container, false)
        setupActivityAndPreferences()
        setupDeviceInfo()
        setupListenersAndReceivers()
        setupBackPressedCallback()
        lifecycleScope.launch {
            handleCardDataInitialization()
        }

        setupSwipeRefreshListener()
        setupWearableSettingsEntry()
        return binding.root
    }


    /**
     * Exposes the wearable configuration entry point.
     *
     * The passcode use cases are driven by the bottom CTA (see [onWearableStatus]) and the
     * wearable configuration screen; the gear in the device row opens the latter. It is hidden in
     * the shared device row by default, so this screen opts into showing it.
     */
    private fun setupWearableSettingsEntry() {
        binding.selectedDevice.imgDeviceSettings.visibility = View.VISIBLE
        binding.selectedDevice.imgDeviceSettings.setOnClickListener {
            findNavController().navigate(R.id.wearableSettingFragment)
        }
    }

    /**
     * Makes the status-chip row the connect affordance while Bluetooth is off: tapping it runs the
     * standard BLE connect flow and, once connected, re-reads the CVM state so the passcode CTA
     * reappears. When [enabled] is false the row is inert status only.
     */
    private fun setStatusChipsConnectEnabled(enabled: Boolean) {
        if (!::statusChips.isInitialized) return
        val row = statusChips.root
        if (enabled) {
            row.isClickable = true
            row.setOnClickListener {
                ensureBleConnectedThenRun(
                    onConnected = { statusMonitor.requestPoll() },
                    blePromptNotRequired = true
                )
            }
        } else {
            row.setOnClickListener(null)
            row.isClickable = false
        }
    }

    /**
     * Points the bottom CTA at what the [state] calls for:
     * - no passcode provisioned → Set passcode, opens the setup flow;
     * - passcode set but not verified → Verify passcode, opens the verify flow;
     * - already verified → Payment unlocked, a non-actionable status label.
     *
     * A null state (read failed) hides the button, since there is nothing actionable and the chips
     * already report the failure.
     */
    private fun updatePasscodeCta(state: CvmState?) {
        if (!::binding.isInitialized) return
        // Gate the card list / add-card while no passcode is provisioned (null state = leave as-is).
        passcodeSetupRequired = state?.needsPasscodeSetup() == true
        devicelocked = state?.isBlocked == true
        applyPasscodeSetupGate()
        if (state == null) {
            binding.btnPasscodeCta.visibility = View.GONE
            binding.btnPasscodeCta.setOnClickListener(null)
            return
        }
        when {
            state.isAuthenticated -> {
                // Payments are already unlocked; no bottom action is needed, so hide the button.
                binding.btnPasscodeCta.visibility = View.GONE
                binding.btnPasscodeCta.setOnClickListener(null)
                return
            }

            state.needsPasscodeSetup() -> {
                binding.btnPasscodeCta.setText(R.string.card_list_cta_set_passcode)
                binding.btnPasscodeCta.setOnClickListener {
                    openPasscodeFlow(WearablePasscodeStep.SETUP)
                }
            }
            state.isBlocked -> {
                binding.btnPasscodeCta.setText(R.string.card_list_cta_clear_wallet)
                binding.btnPasscodeCta.setOnClickListener {
                    resetWearableWallet()
                }
            }

            else -> {
                binding.btnPasscodeCta.setText(R.string.card_list_cta_verify_passcode)
                binding.btnPasscodeCta.setOnClickListener {
                    // UC-02 off-body gate: refuse verification while the wearable is off the wrist.
                    if (lastPresence == BodyPresenceTracker.Presence.OFF_BODY) {
                        statusDialog(activity, getString(R.string.wearable_off_body_verify_blocked))
                    } else {
                        openPasscodeFlow(WearablePasscodeStep.VERIFY)
                    }
                }
            }
        }
        binding.btnPasscodeCta.visibility = View.VISIBLE
    }

    /**
     * Applies the setup gate: while [passcodeSetupRequired] the card list, empty-state and the "+"
     * add-card control are hidden and a "setup wearable passcode" message is shown; otherwise the
     * message is hidden and the "+" is restored (the card-fetch logic manages the list itself).
     */
    private fun applyPasscodeSetupGate() {
        if (!::binding.isInitialized) return
        if (passcodeSetupRequired) {
            binding.tvSetupPasscodeMessage.visibility = View.VISIBLE
            binding.tvDeviceLockedMessage.visibility = View.GONE
            binding.imgAddCard.visibility = View.GONE
            binding.tvTitle.visibility = View.INVISIBLE
            binding.tvNoCards.visibility = View.GONE
            binding.recyclerView.visibility = View.GONE
            binding.swipeRefreshLayout.isRefreshing = false
        } else if (devicelocked) {
            binding.tvDeviceLockedMessage.visibility = View.VISIBLE
            binding.tvSetupPasscodeMessage.visibility = View.GONE
            binding.imgAddCard.visibility = View.GONE
            binding.tvTitle.visibility = View.INVISIBLE
            binding.tvNoCards.visibility = View.GONE
            binding.recyclerView.visibility = View.GONE
            binding.swipeRefreshLayout.isRefreshing = false
        } else {
            binding.tvDeviceLockedMessage.visibility = View.GONE
            binding.tvSetupPasscodeMessage.visibility = View.GONE
            binding.imgAddCard.visibility = View.VISIBLE

            val showConnected = BluetoothStateManager.isBluetoothTurnedOn(requireContext())
                && BluetoothStateManager.isDeviceConnected(seId, requireContext())

            binding.tvTitle.visibility = if(showConnected) {
                View.VISIBLE
            } else  View.INVISIBLE
        }
    }

    /**
     * Opens the wearable passcode flow at [step].
     *
     * The screens own the input, validation and secure-element calls, so nothing else is needed
     * here. Requires an SE ID, which keys the persisted passcode challenge.
     */
    private fun openPasscodeFlow(step: WearablePasscodeStep) {
        if (CdcvmApi.activeDeviceId() == null) {
            showToast(getString(R.string.wearable_passcode_not_connected))
            return
        }
        findNavController().navigate(
            R.id.wearablePasscodeFragment,
            WearablePasscodeFragment.argsFor(step)
        )
    }

    /**
     * Initializes activity reference, enables back navigation in the toolbar,
     * and loads necessary preferences like PaymentAppInstanceId and SE ID.
     * Also updates the screen state in preferences.
     */
    private fun setupActivityAndPreferences() {
        activity = requireActivity() as MainActivity
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        paymentAppInstanceId = StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID)
        seId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)
    }

    /**
     * Loads the device name and image from shared preferences
     * and updates the corresponding UI elements.
     */
    private fun setupDeviceInfo() {
        binding.selectedDevice.textDeviceName.text = StorageRepository.readString(PreferenceKey.DEVICE_NAME)
        getWearableImage(
            StorageRepository.readString(PreferenceKey.DEVICE_IMAGE)
        )
    }

    /**
     * Sets up event listeners and starts collecting events from EventBus
     * for card-related actions (like card refresh).
     */
    private fun setupListenersAndReceivers() {
        initListeners()
        updateAddCardInteractiveState()
        eventCollectorJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                EventBus.events.collect { event ->
                    when (event.action) {
                        ACTION_DEVICE_STATUS_UPDATE-> handleSuspendNotification(event)
                        ACTION_CARD -> handleRefreshEvent(event)
                        DEFAULT_CARD_CHANGE -> handleDefaultCardChange()
                    }
                }
            }
        }
    }

    /**
     * Overrides the default system back-press behavior.
     * Navigates the user back to the device list screen when back is pressed.
     */
    private fun setupBackPressedCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!isNetworkAvailable(requireContext())) {
                        confirmDataDialog(getString(R.string.data_enable))
                        return
                    }
                    findNavController().navigate(R.id.deviceListFragment)
                }
            })
    }

    /**
     * Initializes card data by fetching it from the local database.
     * Based on the data state, it decides whether to reinitialize the view,
     * load card images, or clear invalid data.
     */
    private suspend fun handleCardDataInitialization() {
        cardListDetail = StorageRepository.getUiCardListFromLocalDb(requireContext(), paymentAppInstanceId!!)

        if (shouldForceApiRefresh) {
            if (isFcmSecureFlowControllingLoader()) {
                logger.debug("Deferring card list refresh until FCM secure flow completes")
                return
            }
            StorageRepository.clearAllLocalCardData(requireContext(), clearUserNicknames = false)
            shouldForceApiRefresh = false
            initView()
            return
        }

        if (isCardAdded) {
            initView()
            isCardAdded = false
        } else if (cardListDetail.isNotEmpty()) {
            loadCardImagesFromDatabase()
            setupCardListAdapter()
        } else {
            StorageRepository.clearLocalCardsForPaymentApp(requireContext(), paymentAppInstanceId!!)
            initView()
        }
    }

    /**
     * Loads all card images from the local database and
     * stores them in a map for quick access by the adapter.
     */
    private suspend fun loadCardImagesFromDatabase() {
        for (card in cardListDetail) {
            val cardData =
                StorageRepository.getLocalCardImageByAssetId(requireContext(), card.cardAssetId.toString())
            if (!cardData.cardImage.isNullOrEmpty() && (!card.cardAssetId.isNullOrEmpty())) {
                dbImageMap[card.cardAssetId!!] = ImageUtils.base64ToBitmap(cardData.cardImage!!)
            }
        }
    }

    /**
     * Configures the RecyclerView adapter to display the list of cards.
     * Also makes the wallet image and title visible.
     */
    private fun setupCardListAdapter() {
        try {
            binding.tvSetupPasscodeMessage.visibility = View.GONE
            binding.tvTitle.visibility = View.VISIBLE
            binding.recyclerView.adapter = createCardAdapter()
            binding.recyclerView.visibility = View.VISIBLE
        } catch (e: IndexOutOfBoundsException) {
            logger.debug("Exception during card data processing: $e")
            binding.recyclerView.visibility = View.GONE
        }
    }

    /**
     * Creates and returns an [AddCardAdapterDb] for the card list RecyclerView.
     *
     * Wires item click, long-press, and delete callbacks and applies persisted card images from [dbImageMap].
     *
     * @return Configured [AddCardAdapterDb] bound to [cardListDetail].
     */
    private fun createCardAdapter(): AddCardAdapterDb {
        return AddCardAdapterDb(
            cardListDetail, onItemClicked = { position, _ ->
                handleCardItemClick(position)
            }, onItemLongPress = { _, _ -> initListeners() }, imageMap = dbImageMap
        )
    }

    /**
     * Handles click events for card list items.
     * If a card is pending authentication, navigates to pending screen,
     * otherwise navigates to the detailed view.
     */
    private fun handleCardItemClick(position: Int) {
        try {
            if (isLoginOlderThanSessionExpiryDuration()) {
                navigateToLoginScreen()
                return
            }
            val card = cardListDetail.getOrNull(position) ?: return

            if (card.cardStatus == PENDING &&
                card.cardDecision == REQUIRE_ADDITIONAL_AUTHENTICATION
            ) {
                navigateToPending(card)
            } else {
                navigateToCardDetail(position)
            }
        } catch (e: IndexOutOfBoundsException) {
            logger.debug("Exception :- $e")
        }
    }

    /**
     * Sets up swipe-to-refresh behavior.
     * When triggered, it clears all card data from the database and reloads the view.
     */
    private fun setupSwipeRefreshListener() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    StorageRepository.clearAllLocalCardData(requireContext(), clearUserNicknames = false)
                    imageMap.clear()
                    dbImageMap.clear()
                    initView()
                } catch (e: IndexOutOfBoundsException) {
                    logger.debug("Exception during swipe refresh: $e")
                }
            }
        }
    }

    /**
     * Navigates to the card detail screen with the selected card’s information.
     * Passes all relevant card details as a Bundle to the destination fragment.
     */
    private fun navigateToCardDetail(position: Int) {
        val bundle = Bundle().apply {
            putString(BundleKey.DEVICE_NAME, binding.selectedDevice.textDeviceName.text.toString())
            putString(BundleKey.PNO_TYPE, cardListDetail[position].pnoType)
            putString(BundleKey.CARD_STATUS, cardListDetail[position].cardStatus)
            putString(BundleKey.PAN_SUFFIX, cardListDetail[position].dpanSuffix)
            putString(BundleKey.CARD_NICK_NAME, cardListDetail[position].cardNickname)
            putString(BundleKey.CUSTOM_URL, cardListDetail[position].customUrl)
            putString(BundleKey.CONTACT_NUMBER, cardListDetail[position].contactNumber)
            putString(BundleKey.PRIVACY_POLICY_URL, cardListDetail[position].privacyPolicyURL)
            putString(BundleKey.CONTACT_WEBSITE, cardListDetail[position].contactWebsite)
            putString(BundleKey.TERMS_AND_CONDITIONS_URL, cardListDetail[position].termsAndConditionsURL)
            putString(BundleKey.CONTACT_EMAIL, cardListDetail[position].contactEmail)
            putString(BundleKey.ASSET_ID, cardListDetail[position].cardAssetId)
            putString(BundleKey.EXP_DATE_PRINTED_IND, cardListDetail[position].expDatePrintedInd)
            putString(BundleKey.CARD_EXP_DATE, cardListDetail[position].cardExpiry)
            putInt(BundleKey.DEFAULT_TAB_INDEX, 0)
        }

        StorageRepository.saveString(
            PreferenceKey.DIGITIZATION_REFERENCE_NUMBER,
            cardListDetail[position].digitizationReferenceNumber.toString()
        )
        cardListDetail[position].paymentAppInstanceId?.takeIf { it.isNotBlank() }?.let { pid ->
            StorageRepository.saveString(PreferenceKey.PAYMENT_APP_INSTANCE_ID, pid)
        }

        findNavController().navigate(
            R.id.detailFragment, bundle
        )
    }

    /**
     * Navigates to the "Terms & Conditions" screen for cards
     * that are in a pending state (CardList version).
     */
    private fun navigateToPending(card: CardList) {
        if (!isAdded || view == null) return
        logger.debug("card.authenticationMethods : ${card.authenticationMethods.size}")
        if (card.authenticationMethods.isEmpty()) return
        val bundle = Bundle().apply {
            putString(BundleKey.PAYMENT_APP_INSTANCE_ID, card.paymentAppInstanceId)
            putString(BundleKey.DEVICE_NAME, binding.selectedDevice.textDeviceName.text.toString())
            putString(BundleKey.DIGITIZATION_REFERENCE_NUMBER, card.digitizationReferenceNumber)
            putString(BundleKey.PNO_TYPE, card.pnoType)
            putString(BundleKey.D_PAN_SUFFIX, card.dpanSuffix)
            putString(BundleKey.PENDING_STATUS, PENDING)
        }
        findNavController().navigate(R.id.termsFragment, bundle)
    }

    /**
     * Navigates the user to the Terms screen with pending card details.
     *
     * @param card The [CardDetails] object containing the card information to pass along.
     */
    private fun navigateToPending(card: CardDetails) {
        if (!isAdded || view == null) return
        logger.debug("card.authenticationMethods : ${card.authenticationMethods.size}")
        if (card.authenticationMethods.isEmpty()) return
        val bundle = Bundle().apply {
            putString(BundleKey.PAYMENT_APP_INSTANCE_ID, paymentAppInstanceId)
            putString(BundleKey.DEVICE_NAME, binding.selectedDevice.textDeviceName.text.toString())
            putString(BundleKey.DIGITIZATION_REFERENCE_NUMBER, card.digitizationReferenceNumber)
            putString(BundleKey.PNO_TYPE, card.pnoType)
            putString(BundleKey.D_PAN_SUFFIX, card.dpanSuffix)
            putString(BundleKey.PENDING_STATUS, PENDING)
        }

        findNavController().navigate(R.id.termsFragment, bundle)
    }

    /**
     * initView(): It handles the API call to fetch the list of cards
     * If success, call the asset api to fetch the card image
     *
     */
    private fun initView() {
        if (isLoginOlderThanSessionExpiryDuration()) {
            DigitizationDeleteFlowGate.clearTermsClientDelete()
            navigateToLoginScreen()
            return
        }
        if (!isNetworkAvailable(requireContext())) {
            DigitizationDeleteFlowGate.clearTermsClientDelete()
            confirmDataDialog(getString(R.string.data_enable))
            return
        }
        if (isFcmSecureFlowControllingLoader() || isAddCardLoaderOwner()) {
            logger.debug("Skipping card list fetch while secure or add-card flow is in progress")
            if (::binding.isInitialized) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
            return
        }
        initializeCardData()
        // Default layout is VISIBLE with no adapter when DB is empty; avoid "No adapter attached" layout passes.
        if (binding.recyclerView.adapter == null) {
            binding.recyclerView.visibility = View.GONE
        }
        activity.showLoading(true, getString(R.string.text_fetching_cards))
        lifecycleScope.launch(AppDispatchers.IO) {
            try {
                if (!isAdded) return@launch
                paymentAppInstanceId?.let {
                    fetchProvisionedCards(it)
                }
            } finally {
                withContext(AppDispatchers.MAIN) {
                    DigitizationDeleteFlowGate.clearTermsClientDelete()
                }
            }
        }
    }

    /**
     * Initializes or resets all card-related data structures.
     * Clears existing lists, resets counters, and prepares new objects
     * before fetching or processing any card data.
     */
    private fun initializeCardData() {
        cardList.clear()
        imageMap.clear()
        dbImageMap.clear()
        nonDefaultCardCount = 0
    }

    /**
     * Initiates the process to fetch all provisioned cards associated with
     * the given Payment App Instance ID.
     *
     * @param paymentAppInstanceId The unique identifier for the payment app instance.
     */
    private suspend fun fetchProvisionedCards(paymentAppInstanceId: String) {
        val sdkResult = WalletRepository.fetchProvisionedCards(
            context = activity,
            paymentAppInstanceId = paymentAppInstanceId
        )

        if (sdkResult.isSuccess) {
            val provisionCardResponse = sdkResult.response
            if (provisionCardResponse != null) {
                handleFetchCardsSuccess(provisionCardResponse)
            } else {
                activity.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    binding.swipeRefreshLayout.isRefreshing = false
                    dismissLoaderIfAllowed()
                    val msg = getString(R.string.something_went_wrong)
                    logger.error("Error loading card list: empty response body")
                    if (!isFcmSecureFlowControllingLoader()) {
                        handleSessionOrError(msg) { statusDialog(activity, msg) }
                    }
                }
            }
        } else {
            dismissLoaderIfAllowed()
            logger.error("Error loading card list: ${sdkResult.errorMessage}")
            if (!isFcmSecureFlowControllingLoader()) {
                handleSessionOrError(sdkResult.errorMessage)
            }
        }
    }

    /**
     * Handles successful retrieval of provisioned cards from the SDK.
     *
     * @param response The response object received[GetProvisionCardResponse] from the SDK containing provisioned card data.
     */
    private suspend fun handleFetchCardsSuccess(response: GetProvisionCardResponse) {
        try {
            if (!isAdded) return
            val safeContext = context ?: return
            withContext(AppDispatchers.MAIN) {
                binding.swipeRefreshLayout.isRefreshing = false
                if (!isFcmSecureFlowControllingLoader()) {
                    activity.showLoading(true, getString(R.string.text_please_wait))
                }
                handleFetchCardsResponse(response, safeContext)
            }
        } catch (e: IndexOutOfBoundsException) {
            logger.debug("Exception during card list processing: $e")
        }
    }

    /**
     * Processes the [GetProvisionCardResponse] received from the SDK
     * and determines whether to handle a successful response or an expired access token.
     *
     * @param response The response containing card data and status.
     * @param safeContext A non-null context used for further operations.
     */
    private suspend fun handleFetchCardsResponse(response: GetProvisionCardResponse, safeContext: Context) {
        when {
            response.statusMessage?.equals(
                CommonResponse.SUCCESS.response,
                ignoreCase = true
            ) == true -> {
                handleSuccessfulCardResponse(response, safeContext)
            }

            else -> {
                // SDK may call onSuccess with a body such as empty cardList + "IO Exception. Please try again."
                val msg = response.statusMessage?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.something_went_wrong)
                if (isFcmSecureFlowControllingLoader() || isAddCardLoaderOwner()) {
                    logger.debug("Suppressing provision error during secure/add-card flow: $msg")
                    return
                }
                activity.showLoading(false, "")
                logger.error("Provision card list rejected: $msg")
                handleSessionOrError(msg) { statusDialog(safeContext, msg) }
            }
        }
    }

    /**
     * Handles successful card responses by checking whether the response contains cards.
     * If cards exist, it initializes the UI and adapters; otherwise, it shows an empty state.
     *
     * @param response The SDK response containing a list of provisioned cards.
     * @param safeContext The application context.
     */
    private suspend fun handleSuccessfulCardResponse(
        response: GetProvisionCardResponse, safeContext: Context
    ) {
        if (response.cardList.isNotEmpty()) {
            handleNonEmptyCardList(response, safeContext)
        } else {
            if (isDeletingFromPortal) {
                dismissLoaderIfAllowed()
            }
            handleEmptyCardList()
        }
    }

    /**
     * Handles the case where one or more provisioned cards exist.
     * Sets up the UI, initializes the card list adapter, and loads card images.
     *
     * @param response The SDK response containing provisioned cards.
     * @param safeContext The context used for UI updates.
     */
    private fun handleNonEmptyCardList(response: GetProvisionCardResponse, safeContext: Context) {
        logger.debug("response.cardList.size : ${response.cardList.size}")
        var isDefaultCardAvailable = false
        for (card in response.cardList) {
            StorageRepository.saveString(
                key = PreferenceKey.spsdAppletInstanceAidKey(card.digitizationReferenceNumber.toString()),
                value = card.apsdAid.toString()
            )
            card.appletInstanceAids?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { aid ->
                StorageRepository.saveString(
                    key = PreferenceKey.cardAidKey(card.digitizationReferenceNumber.toString()),
                    value = aid
                )
            }
            val cardType = when (card.pnoType) {
                PNO_MDES -> getString(R.string.master_card_)
                PNO_VTS -> getString(R.string.visa)
                else -> ""
            }

            logger.debug(":: card.digitizationReferenceNumber.toString() : ${card.digitizationReferenceNumber.toString()}")
            logger.debug(":: isTransactionNotificationEnabled : ${card.isTransactionNotificationEnabled}")

            StorageRepository.saveBoolean(
                key = PreferenceKey.updateTransactionNotificationKey(card.digitizationReferenceNumber.toString()),
                value = card.isTransactionNotificationEnabled
            )

            StorageRepository.saveString(
                key = PreferenceKey.aidCardTypeKey(card.digitizationReferenceNumber.toString()),
                value = cardType
            )
            if (!isDefaultCardAvailable && card.cardAsDefault.equals("true", ignoreCase = true)) {
                val paymentId = StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID)
                StorageRepository.saveString(
                    key = PreferenceKey.deviceKey(paymentId),
                    value = card.digitizationReferenceNumber.toString()
                )
                isDefaultCardAvailable = true
                logger.debug("Default Card : card.digitizationReferenceNumber : ${card.digitizationReferenceNumber}")
            }
        }
        val navigatedAfterAddCardFlow = arguments?.getBoolean(BundleKey.NAVIGATED_AFTER_ADDCARD_FLOW) == true
        logger.debug(":: navigatedAfterAddCardFlow : $navigatedAfterAddCardFlow")
        arguments?.remove(BundleKey.NAVIGATED_AFTER_ADDCARD_FLOW)
        logger.debug("isDefaultCardAvailable : $isDefaultCardAvailable")

        val cardToPromoteAsDefault = resolveCardToPromoteAsDefault(
            response = response,
            isDefaultCardAvailable = isDefaultCardAvailable
        )
        logger.debug(
            "setAsDefault : ${cardToPromoteAsDefault != null}, " +
                "promoteRef : ${cardToPromoteAsDefault?.digitizationReferenceNumber}"
        )
        val seId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)

        if (cardToPromoteAsDefault != null &&
            (isNFC() || (BluetoothStateManager.isBluetoothTurnedOn(safeContext)
                && BluetoothStateManager.isDeviceConnected(seId, safeContext)))
        ) {
            checkAndSetCardAsDefault(response, safeContext, cardToPromoteAsDefault)
        } else {
            cardListUiUpdateFlow(response, safeContext)
        }
    }

    /**
     * Returns the card that should be promoted to default via PPSE + set-default API, or null when
     * no automatic promotion is needed.
     *
     * - Single card with no backend default (`cardAsDefault`): auto-set on load or after add-card.
     * - Default deleted leaving exactly one card from a 2-card wallet: promote the remaining card
     *   when [BuildConfig.ENABLE_AUTO_PROMOTE_REMAINING_DEFAULT_CARD] is true.
     * - Default deleted with 2+ cards remaining: no automatic promotion.
     */
    private fun resolveCardToPromoteAsDefault(
        response: GetProvisionCardResponse,
        isDefaultCardAvailable: Boolean
    ): CardList? {
        if (response.cardList.isEmpty() || isDefaultCardAvailable) return null

        val remainingRefs = response.cardList.mapNotNull { card ->
            card.digitizationReferenceNumber?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        }

        if (remainingRefs.size == 1) {
            if (consumePendingAutoPromoteRemainingAfterDefaultDelete()) {
                if (BuildConfig.ENABLE_AUTO_PROMOTE_REMAINING_DEFAULT_CARD) {
                    logger.debug(
                        "Promoting remaining card after default deleted from 2-card wallet ref=${remainingRefs.first()}"
                    )
                    return response.cardList[0]
                }
                logger.debug(
                    "Auto-promote disabled; not promoting remaining card after default delete ref=${remainingRefs.first()}"
                )
                return null
            }
            logger.debug("Promoting sole remaining card as default ref=${remainingRefs.first()}")
            return response.cardList[0]
        }

        return null
    }

    /**
     * Clears the stored default-card token when the deleted card was the default so the list refresh
     * can promote another card.
     */
    private fun clearSavedDefaultIfDeleted(deletedTokenRef: String) {
        if (onDefaultCardDeleted(deletedTokenRef, cardList.size)) {
            logger.debug("Cleared saved default after deleting default card ref=$deletedTokenRef")
        }
    }

    /**
     * Updates the UI elements for displaying the list of cards.
     * Stores the default card ID in preferences and makes the wallet UI visible.
     *
     * @param response The response containing card details.
     */
    private fun setupCardListUI(response: GetProvisionCardResponse) {
        StorageRepository.saveString(
            PreferenceKey.DEFAULT_CARD_ID,
            response.cardList[0].digitizationReferenceNumber.toString()
        )
        binding.tvSetupPasscodeMessage.visibility = View.GONE
        binding.tvTitle.visibility = View.VISIBLE
        binding.tvNoCards.visibility = View.GONE
        cardList = response.cardList
    }

    /**
     * Creates and sets up the RecyclerView adapter for displaying the list of cards.
     *
     * @param response The response containing the list of provisioned cards.
     * @param safeContext The application context.
     */
    private suspend fun setupCardAdapter(response: GetProvisionCardResponse, safeContext: Context) {
        val nicknameMap = StorageRepository.getNicknameMap(requireContext())
        adapter = AddCardAdapter(
            response.cardList,
            onItemClicked = { position, _ -> handleCardClick(response, position, safeContext) },
            onItemLongPress = { _, _ -> initListeners() },
            nicknameMap,
            imageMap = imageMap
        )
        binding.recyclerView.adapter = adapter
        binding.recyclerView.visibility = View.VISIBLE
    }

    /**
     * Handles click events on a specific card in the list.
     *
     * @param response The response containing the clicked card details.
     * @param position The position of the clicked card in the list.
     * @param safeContext The application context.
     */
    private fun handleCardClick(
        response: GetProvisionCardResponse, position: Int, safeContext: Context
    ) {
        if (isLoginOlderThanSessionExpiryDuration()) {
            navigateToLoginScreen()
            return
        }
        if (response.cardList[position].status == PENDING && response.cardList[position].cardDecision == REQUIRE_ADDITIONAL_AUTHENTICATION) {
            navigateToPending(response.cardList[position])
        } else {
            navigateToCardDetailFromResponse(response, position, safeContext)
        }
    }

    /**
     * Navigates to the card detail fragment for a selected card.
     * Prepares a data bundle with complete card information and saves the reference number.
     *
     * @param response The response containing card list data.
     * @param position The position of the selected card.
     * @param safeContext The context used for preference operations.
     */
    private fun navigateToCardDetailFromResponse(
        response: GetProvisionCardResponse, position: Int, safeContext: Context
    ) {
        lifecycleScope.launch {
            val cardNickName = StorageRepository.getNicknameForCard(
                paymentAppInstanceId!!, cardList[position].dpanSuffix.toString(), safeContext
            )
            val bundle = createCardDetailBundle(response, position, cardNickName.toString())
            StorageRepository.saveString(
                PreferenceKey.DIGITIZATION_REFERENCE_NUMBER,
                response.cardList[position].digitizationReferenceNumber.toString()
            )
            findNavController().navigate(R.id.detailFragment, bundle)
        }
    }

    /**
     * Creates a [Bundle] object containing all relevant details about the selected card.
     * This data is used when navigating to the card detail screen.
     *
     * @param response The SDK response containing card list data.
     * @param position The selected card position.
     * @param cardNickName The nickname assigned to the card.
     * @return A fully prepared [Bundle] with card information.
     */
    private fun createCardDetailBundle(
        response: GetProvisionCardResponse, position: Int, cardNickName: String
    ): Bundle {
        return Bundle().apply {
            putBasicCardInfo(response, position, cardNickName)
            putCardMetadata(response, position)
            putCardSpecificInfo(response, position)
        }
    }

    /**
     * Extension function to populate a [Bundle] with basic card information
     * extracted from the given [GetProvisionCardResponse].
     *
     * @param response The API response containing the list of cards that can be provisioned.
     * @param position The index of the selected card in the response list.
     * @param cardNickName The nickname assigned to the card by the user.
     */
    private fun Bundle.putBasicCardInfo(
        response: GetProvisionCardResponse, position: Int, cardNickName: String
    ) {
        putString(BundleKey.DEVICE_NAME, binding.selectedDevice.textDeviceName.text.toString())
        putString(BundleKey.PNO_TYPE, response.cardList[position].pnoType)
        putString(BundleKey.CARD_STATUS, response.cardList[position].status)
        putString(BundleKey.PAN_SUFFIX, response.cardList[position].dpanSuffix)
        putString(BundleKey.CARD_NICK_NAME, cardNickName)
        putString(BundleKey.CUSTOM_URL, response.cardList[position].productConfig?.customerServiceUrl)
        putString(BundleKey.EXP_DATE_PRINTED_IND, response.cardList[position].tokenInfo?.expDatePrintedInd)
        putString(BundleKey.CARD_EXP_DATE, response.cardList[position].expirationDate)
    }

    /**
     * Extension function to populate a [Bundle] with card metadata details
     * extracted from the given [GetProvisionCardResponse].
     *
     * @param response The API response containing a list of provisional cards.
     * @param position The index of the selected card within the response list.
     */
    private fun Bundle.putCardMetadata(response: GetProvisionCardResponse, position: Int) {
        putString(BundleKey.CONTACT_NUMBER, response.cardList[position].cardMetaData?.contactNumber)
        putString(BundleKey.PRIVACY_POLICY_URL, response.cardList[position].cardMetaData?.privacyPolicyURL)
        putString(BundleKey.CONTACT_WEBSITE, response.cardList[position].cardMetaData?.contactWebsite)
        putString(
            BundleKey.TERMS_AND_CONDITIONS_URL,
            response.cardList[position].cardMetaData?.termsAndConditionsURL
        )
        putString(BundleKey.CONTACT_EMAIL, response.cardList[position].cardMetaData?.contactEmail)
    }

    /**
     * Extension function to populate a [Bundle] with card network–specific information
     * based on the card’s product number type (PNO type).
     *
     * @param response The API response containing the list of cards that can be provisioned.
     * @param position The index of the selected card in the response list.
     */
    private fun Bundle.putCardSpecificInfo(response: GetProvisionCardResponse, position: Int) {
        when (response.cardList[position].pnoType) {
            PNO_MDES -> putMDESCardInfo(response, position)
            PNO_VTS -> putVTSCardInfo(response, position)
        }
    }

    /**
     * Extension function to populate a [Bundle] with Mastercard (MDES)–specific
     * configuration and contact information.
     *
     * @param response The API response containing the list of cards that can be provisioned.
     * @param position The index of the selected card in the response list.
     */
    private fun Bundle.putMDESCardInfo(response: GetProvisionCardResponse, position: Int) {
        putString(
            BundleKey.CONTACT_NUMBER, response.cardList[position].productConfig?.customerServicePhoneNumber
        )
        putString(BundleKey.PRIVACY_POLICY_URL, response.cardList[position].productConfig?.privacyPolicyURL)
        putString(BundleKey.CONTACT_WEBSITE, response.cardList[position].productConfig?.customerServiceUrl)
        putString(
            BundleKey.TERMS_AND_CONDITIONS_URL,
            response.cardList[position].productConfig?.termsAndConditionsUrl
        )
        putString(BundleKey.CONTACT_EMAIL, response.cardList[position].productConfig?.customerServiceEmail)
        putString(
            BundleKey.ASSET_ID, response.cardList[position].productConfig?.cardBackgroundCombinedAssetId
        )
    }

    /**
     * Extension function to populate a [Bundle] with Visa (VTS)–specific
     * digital card art information.
     *
     * @param response The API response containing the list of cards that can be provisioned.
     * @param position The index of the selected card in the response list.
     */
    private fun Bundle.putVTSCardInfo(response: GetProvisionCardResponse, position: Int) {
        putString(
            BundleKey.ASSET_ID,
            response.cardList[position].cardMetaData?.cardData?.firstOrNull { it.contentType == "digitalCardArt" }?.guid
        )
    }

    /**
     * Loads digital card images for all cards in the given [GetProvisionCardResponse].
     *
     * @param response The API response containing the list of cards to process.
     */
    private fun loadCardImages(response: GetProvisionCardResponse) {
        val cardsWithImages = response.cardList.filter { getAssetIdForCard(it) != null }

        if (cardsWithImages.isEmpty()) {
            dismissLoaderIfAllowed()
            return
        }

        var loadedCount = 0
        val totalImages = cardsWithImages.size

        for ((_, card) in response.cardList.withIndex()) {
            val assetId = getAssetIdForCard(card)
            if (assetId != null) {
                getCardImage(assetId, card.digitizationReferenceNumber) {
                    loadedCount++
                    if (loadedCount >= totalImages && ::activity.isInitialized && !activity.isFinishing) {
                        activity.runOnUiThread {
                            dismissLoaderIfAllowed()
                            binding.recyclerView.adapter?.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieves the asset ID for a given [CardList] item based on its product number type.
     *
     * @param card The card whose asset ID is to be retrieved.
     * @return The card’s background asset ID (for MDES) or digital card art GUID (for VTS),
     * or `null` if not available.
     */
    private fun getAssetIdForCard(card: CardList): String? {
        return when (card.pnoType) {
            PNO_MDES -> card.productConfig?.cardBackgroundCombinedAssetId
            PNO_VTS -> card.cardMetaData?.cardData?.firstOrNull { it.contentType == "digitalCardArt" }?.guid
            else -> null
        }
    }

    /**
     * Handles the case when the card list is empty.
     *
     * Updates the UI to hide card-related views and display a "no cards" message.
     * This method ensures a clean and user-friendly empty state.
     */
    private suspend fun handleEmptyCardList() {
        paymentAppInstanceId?.takeIf { it.isNotBlank() }?.let { pid ->
            StorageRepository.clearLocalCardsForPaymentApp(activity.applicationContext, pid)
        }
        isDeletingFromPortal = false
        binding.recyclerView.adapter = null
        binding.recyclerView.visibility = View.GONE
        binding.tvTitle.visibility = View.INVISIBLE
        if (passcodeSetupRequired || devicelocked) {
            // No passcode yet: show the setup message instead of the "no cards" empty state.
            applyPasscodeSetupGate()
        } else {
            val showConnected = BluetoothStateManager.isBluetoothTurnedOn(requireContext())
                && BluetoothStateManager.isDeviceConnected(seId, requireContext())

            binding.tvTitle.visibility = if(showConnected) {
                View.VISIBLE
            } else  View.INVISIBLE
            binding.tvNoCards.visibility = View.VISIBLE
        }
        dismissLoaderIfAllowed()
    }

    /**
     * initListeners(): It handles the click listeners of the buttons
     *
     */
    private fun initListeners() {
        // Adding a card is the "+" in the card list header. The bottom button is now the CDCVM CTA
        // (see [onWearableStatus]), so it no longer triggers the add-card flow.
        binding.imgAddCard.setOnClickListener { onAddCardClicked() }
    }

    /**
     * Handles Add Payment Card button clicks for NFC and BLE provisioning flows.
     */
    private fun onAddCardClicked() {
        logger.debug("Add Card clicked")
        if (passcodeSetupRequired) {
            // Cannot add a card before the wearable passcode is set up.
            statusDialog(activity, getString(R.string.card_list_setup_passcode_message))
            return
        }
        val isNfcFlow = isNFC()
        if (shouldIgnoreAddCardClick(isNfcFlow)) return
        if (!isNfcFlow && !BluetoothStateManager.isBluetoothTurnedOn(requireContext())) {
            logger.debug("Add Card: Mobile Bluetooth is turned OFF")
            statusDialog(activity, getString(R.string.bluetooth_not_turned_on))
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            isAddCardFlowActive = true
            updateAddCardInteractiveState()
            PayHostFssSync.onAddCardStarting()
            delay(50)
            if (!isAdded || activity.isFinishing) {
                resetAddCardFlowState()
                PayHostFssSync.onAddCardFailed()
                return@launch
            }
            prepareAddCardFlow()
            if (handleAddCardPreconditions(isNfcFlow)) {
                PayHostFssSync.onAddCardFailed()
                return@launch
            }
            if (startNfcAddCardFlow(isNfcFlow)) return@launch
            startBleAddCardFlow()
        }
    }

    /**
     * Returns whether an Add Payment Card click should be ignored due to an in-flight flow.
     *
     * @param isNfcFlow `true` when the linked device is an NFC wearable.
     * @return `true` when the click must be ignored.
     */
    private fun shouldIgnoreAddCardClick(isNfcFlow: Boolean): Boolean {
        if (!isNfcFlow && isBleFlowInProgress) {
            logger.debug("Ignoring click - BLE connection flow already in progress")
            return true
        }
        if (isNavigatingToAddCard) {
            logger.debug("Ignoring click - add card navigation already in progress")
            return true
        }
        if (isAddCardFlowActive) {
            logger.debug("Ignoring click - add card flow already in progress")
            return true
        }
        return false
    }

    /** Keeps the "+" add-card control interactive; pending delete scripts run without blocking it. */
    private fun updateAddCardInteractiveState() {
        if (!::binding.isInitialized) return
        binding.imgAddCard.isEnabled = true
        binding.imgAddCard.alpha = 1f
    }

    /**
     * Resets add-card button state when secure-element script tracking is not observed.
     */
    private fun observeSecureElementScriptIdle() {
        scriptIdleObserverJob?.cancel()
        scriptIdleObserverJob = null
        updateAddCardInteractiveState()
    }

    /**
     * Initializes UI and flow state when the user starts adding a payment card.
     *
     * Marks the add-card flow as active, enables notification handling for pending tasks,
     * updates interactive controls, and shows the add-card loading indicator.
     */
    private fun prepareAddCardFlow() {
        isAddCardFlowActive = true
        isNotificationPending = true
        suppressFcmCardRefresh = true
        updateAddCardInteractiveState()
        showAddCardFlowLoading()
    }

    /**
     * Validates session and NFC card-count preconditions before starting add-card provisioning.
     *
     * @param isNfcFlow `true` when the linked device is an NFC wearable.
     * @return `true` when preconditions failed and the add-card flow was aborted.
     */
    private fun handleAddCardPreconditions(isNfcFlow: Boolean): Boolean {
        if (isLoginOlderThanSessionExpiryDuration()) {
            resetAddCardFlowState()
            navigateToLoginScreen()
            return true
        }
        if (isNfcFlow && binding.tvNoCards.isGone) {
            resetAddCardFlowState()
            statusDialog(activity, getString(R.string.nfc_multiple_cards_error_msg))
            return true
        }
        return false
    }

    /**
     * Starts the NFC add-card flow by fetching pending tasks directly.
     *
     * @param isNfcFlow `true` when the linked device is an NFC wearable.
     * @return `true` when the NFC flow was started.
     */
    private fun startNfcAddCardFlow(isNfcFlow: Boolean): Boolean {
        if (!isNfcFlow) return false
        logger.debug("NFC flow → directly calling getPendingTask")
        cardDetails = null
        getPendingTask(activity, seId ?: "", "")
        return true
    }

    /**
     * Ensures BLE is connected, then fetches pending tasks for the BLE add-card flow.
     */
    private fun startBleAddCardFlow() {
        isBleFlowInProgress = true
        updateAddCardInteractiveState()
        ensureBleConnectedThenRun(
            onConnected = {
                isBleFlowInProgress = false
                updateAddCardInteractiveState()
                if (isNavigatingToAddCard) {
                    logger.debug("Navigation already triggered, skipping")
                    return@ensureBleConnectedThenRun
                }
                logger.debug("BLE connected → proceeding")
                cardDetails = null
                getPendingTask(activity, seId ?: "", "")
            },
            onCancelled = {
                logger.debug("User cancelled BLE connection dialog")
                isBleFlowInProgress = false
                PayHostFssSync.onAddCardFailed()
                resetAddCardFlowState()
            }
        )
    }

    /** True while Add Payment Card is fetching pending tasks / running cleanup scripts on BLE. */
    private fun isAddCardProvisioningFlow(): Boolean =
        isNotificationPending && !isDeleteFlow && !isNFC()

    /**
     * Shows the add-card loading overlay while pending tasks or cleanup scripts run.
     */
    private fun showAddCardFlowLoading() {
        if (!::activity.isInitialized || activity.isFinishing) return
        activity.showLoading(true, getString(R.string.text_please_wait))
    }

    /**
     * Hides the add-card loading overlay and restores button interactivity.
     */
    private fun dismissAddCardFlowLoading() {
        if (!::activity.isInitialized || activity.isFinishing) return
        isBleFlowInProgress = false
        activity.showLoading(false, "")
        updateAddCardInteractiveState()
    }

    /** Clears add-card flow flags and hides any loader left from getPendingTask / delete scripts. */
    private fun resetAddCardFlowState() {
        isAddCardFlowActive = false
        isNotificationPending = false
        isNavigatingToAddCard = false
        suppressFcmCardRefresh = false
        backgroundPendingDeleteJob?.cancel()
        backgroundPendingDeleteJob = null
        dismissAddCardFlowLoading()
    }

    /**
     * Ends a failed add-card attempt after backend cleanup, without disturbing an active
     * get-pending / provisioning flow where [isNotificationPending] is still true.
     */
    private fun finishAbortedAddCardFlowIfNeeded(): Boolean {
        if (!isAddCardFlowActive || isAddCardProvisioningFlow()) return false
        resetAddCardFlowState()
        if (shouldForceApiRefresh) {
            refreshCardListFromServer()
        }
        return true
    }

    /**
     * Runs getPending delete scripts without blocking Add Payment Card or showing loaders.
     * Waits for any in-flight registration/FCM deletes first so BLE is not contended.
     * Capture-card UI can proceed while this coroutine is queued on [awaitIdle].
     */
    private fun runAddCardPendingDeleteScriptsInBackground(
        deleteScriptsList: List<DeleteScriptBase>
    ) {
        if (deleteScriptsList.isEmpty()) return
        isBleFlowInProgress = false
        updateAddCardInteractiveState()
        backgroundPendingDeleteJob?.cancel()
        backgroundPendingDeleteJob = viewLifecycleOwner.lifecycleScope.launch {
            SecureElementScriptCoordinator.awaitIdle()
            if (!isAdded) return@launch
            val activeSeId = seId?.trim().orEmpty()
            if (activeSeId.isEmpty()) return@launch
            if (PendingDeleteScriptExecutionGate.isInProgress(activeSeId)) {
                logger.debug(
                    "Background pending delete skipped; already running for seId=$activeSeId"
                )
                return@launch
            }
            if (!PendingDeleteScriptExecutionGate.tryBegin(activeSeId)) return@launch
            try {
                executeDeleteScriptWithRetry(
                    deleteScriptsList = deleteScriptsList,
                    tokenRefNumber = "",
                    retryCount = 0,
                    runInBackground = true
                )
            } finally {
                releasePendingDeleteExecutionGate()
            }
        }
    }

    /**
     * Captures card details from the wallet SDK and continues the add-card navigation flow.
     */
    private fun captureCardDetails() {
        lifecycleScope.launch {
            if (activity.isFinishing) return@launch
            dismissAddCardFlowLoading()
            val sdkResult = WalletRepository.captureCardDetails(context = activity, paymentAppInstanceId = paymentAppInstanceId.toString())
            if (sdkResult.isSuccess) {
                sdkResult.response?.let { capturedCardDetail ->
                    StorageRepository.saveString(PreferenceKey.PNO_TYPE, capturedCardDetail.pnoType)
                    cardDetails = capturedCardDetail
                    logger.debug("captureCardDetails onSuccess, CapturedCardDetail : $cardDetails")
                    cardDetails?.let { handleAddCardClick(it) }
                }
            } else {
                logger.error("onError : ${sdkResult.errorMessage}")
                if (!::activity.isInitialized || activity.isFinishing) return@launch
                activity.runOnUiThread { resetAddCardFlowState() }
            }
        }
    }

    /**
     *  getWearableImage(): method is used to get the wearable image and displayed using Glide library
     *
     * @param image
     */
    fun getWearableImage(image: String) {
        try {
            val imageBytes = Base64.decode(image, Base64.DEFAULT)
            Glide.with(requireContext()).load(imageBytes).into(binding.selectedDevice.cardImageView)
        } catch (e: IllegalArgumentException) {
            logger.noStackTraceLog("GetWearableImage ", e)
        }
    }

    /**
     * Fetches the card image asynchronously based on the provided asset ID and digitization reference number.
     * Shows and hides loading indicators appropriately, updates the image map and notifies the adapter of changes.
     *
     * @param assetId The asset ID to fetch the image for.
     * @param digitizationRefNumber The digitization reference number related to the card.
     * @param onComplete Callback when image fetch completes (success or failure).
     */
    private fun getCardImage(
        assetId: String,
        digitizationRefNumber: String?,
        onComplete: () -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val dRefForSdk = digitizationRefNumber?.trim().orEmpty()
            val ctx = activity.applicationContext
            val sdkResult = WalletRepository.fetchAsset(
                context = ctx,
                paymentAppInstanceId = paymentAppInstanceId.toString(),
                digitizationReferenceNumber = dRefForSdk,
                assetId = assetId,
                isCardImage = true
            )

            if (sdkResult.isSuccess) {
                sdkResult.response?.let { fetchAssetResponseBody ->
                    if (!fetchAssetResponseBody.isInvalid() && fetchAssetResponseBody.statusMessage == CommonResponse.SUCCESS.response) {
                        fetchAssetResponseBody.mediaContents.firstOrNull()?.data?.let { base64Image ->
                            val bitmapImage = ImageUtils.base64ToBitmap(base64Image)
                            imageMap[assetId] = bitmapImage
                            digitizationRefNumber?.trim()?.takeIf { it.isNotEmpty() }?.let { ref ->
                                StorageRepository.mergeLocalCardImage(ctx, ref, base64Image)
                            }
                            // AddCardAdapterDb binds dbImageMap, not imageMap — keep them in sync after fetch.
                            dbImageMap[assetId] = bitmapImage
                            notifyCardImageLoaded(assetId)
                        }
                    }
                }
                onComplete()
            } else {
                handleSessionOrError(sdkResult.errorMessage) {
                    onComplete()
                }
            }
        }
    }

    /** Refresh list rows as each asset arrives so art appears without waiting for every fetch to finish. */
    private fun notifyCardImageLoaded(assetId: String) {
        if (!::activity.isInitialized || activity.isFinishing) return
        activity.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            val rvAdapter =
                binding.recyclerView.adapter as? AddCardAdapterDb ?: return@runOnUiThread
            val idx = cardListDetail.indexOfFirst { it.cardAssetId == assetId }
            if (idx >= 0) {
                rvAdapter.notifyItemChanged(idx)
            } else {
                rvAdapter.notifyDataSetChanged()
            }
        }
    }

    /**
     * Called when the fragment becomes visible to the user and is actively running.
     *
     * This method performs the following actions:
     * 1. Retrieves the default card's token reference from preferences (`DEFAULT_CARD_TOKEN_REF`)
     *    and prepares for any adapter updates or UI refreshes related to it.
     *    *(Actual update logic for the adapter is to be implemented where indicated.)*
     *
     * 2. Refreshes local card and nickname-backed UI state when returning from child screens.
     *
     * This method is typically used to restore UI
     * when returning to this fragment from another screen (like settings).
     *
     * @see androidx.fragment.app.Fragment.onResume
     */
    override fun onResume() {
        super.onResume()
        if (!isAddCardFlowActive) {
            isBleFlowInProgress = false
        }
        isNavigatingToAddCard = false
        updateAddCardInteractiveState()
        observeSecureElementScriptIdle()
        // Reflects a passcode that was set, verified or cleared while this screen was away.
        statusMonitor.requestPoll()
        if (shouldForceApiRefresh && !isAddCardLoaderOwner()) {
            refreshCardListFromServer()
        }
        lifecycleScope.launch {
            if (view != null && binding.recyclerView.adapter is AddCardAdapterDb &&
                !paymentAppInstanceId.isNullOrBlank()
            ) {
                cardListDetail =
                    StorageRepository.getUiCardListFromLocalDb(requireContext(), paymentAppInstanceId!!)
                binding.recyclerView.adapter = createCardAdapter()
            }
            if (::adapter.isInitialized && binding.recyclerView.adapter === adapter) {
                val updatedNicknameMap = StorageRepository.getNicknameMap(requireContext()).toMutableMap()
                adapter.updateNicknameMap(updatedNicknameMap)
            }
        }
    }

    /**
     * Cleans up when the Fragment view is destroyed by cancelling event collection.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        suppressFcmCardRefresh = false
        eventCollectorJob?.cancel()
        eventCollectorJob = null
        scriptIdleObserverJob?.cancel()
        scriptIdleObserverJob = null
        backgroundPendingDeleteJob?.cancel()
        backgroundPendingDeleteJob = null
    }

    /**
     * Deletes a provisioned card from the wallet and updates its status using the SecoraWalletSDK.
     *
     * @param activity The [MainActivity] instance used for UI context and callbacks.
     * The new status to be applied to the card (e.g., "DELETED").
     * @param tokenRefNumber The unique digitization reference number for the card.
     * @param paymentId The payment application instance ID associated with the card.
     * @param pnoType The card product type (e.g., MDES for Mastercard, VTS for Visa).
     */
    fun deleteCard(
        activity: MainActivity, tokenRefNumber: String, paymentId: String, pnoType: String
    ) {
        alertDialog?.dismiss()
        cardDetails = null
        activity.showLoading(true, getString(R.string.text_please_wait))

        CoroutineScope(Dispatchers.IO).launch {
            requestPendingTaskForDeleteCard(activity, tokenRefNumber, paymentId, pnoType)
        }
    }

    private suspend fun requestPendingTaskForDeleteCard(
        activity: MainActivity,
        tokenRefNumber: String,
        paymentId: String,
        pnoType: String
    ) {

        fetchSequenceNumberFromDevice(
            onRetrieved = { currentSequenceCounter ->
                logger.debug("currentSequenceCounter : $currentSequenceCounter")
                lifecycleScope.launch {
                    handleDeleteCardGetpendingTask(activity, tokenRefNumber, paymentId, pnoType, currentSequenceCounter)
                }
            },
            onFailed = {
                activity.runOnUiThread {
                    if (!activity.isFinishing) {
                        activity.showLoading(false, "")
                    }
                    isDeleteFlow = false
                    isDeletingFromPortal = false
                }
            })

    }

    private suspend fun handleDeleteCardGetpendingTask(
        activity: MainActivity,
        tokenRefNumber: String,
        paymentId: String,
        pnoType: String,
        currentSequenceCounter: String
    ) {
        val seIdForRequest = resolveSeIdForPendingTask()
        val safeDigitizeRef =
            tokenRefNumber.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        logger.debug("CardListFragment : Calling Pending Task :")
        val sdkResult = WalletRepository.getPendingTask(
            context = activity,
            seId = seIdForRequest,
            digitizationReferenceNumber = safeDigitizeRef.toString(),
            currentSequenceCounter = currentSequenceCounter
        )

        if (sdkResult.isSuccess) {
            handleDeleteCardPendingTaskSuccess(
                sdkResult.response,
                activity,
                tokenRefNumber,
                paymentId,
                pnoType
            )
        } else {
            isDeleteFlow = false
            activity.showLoading(false, "")
            handleSessionOrError(sdkResult.errorMessage) {
                statusDialog(activity, sdkResult.errorMessage)
            }
        }
    }

    /**
     * Resolves the secure element ID for pending-task API calls.
     *
     * @return Fragment seId when set, otherwise the stored device seId preference.
     */
    private fun resolveSeIdForPendingTask(): String {
        return seId?.takeIf { it.isNotBlank() }
            ?: StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)
    }

    private suspend fun handleDeleteCardPendingTaskSuccess(
        response: GetPendingResponse?,
        activity: MainActivity,
        tokenRefNumber: String,
        paymentId: String,
        pnoType: String
    ) {
        if (response == null || response.statusMessage.isNullOrEmpty()) {
            isDeleteFlow = false
            logger.debug("CardListFragment : Pending Task Empty :")
            activity.showLoading(false, "")
            return
        }

        logger.debug("CardListFragment : Pending Task response.statusMessage : ${response.statusMessage}")
        when {
            response.statusMessage == CommonResponse.SUCCESS.response -> {
                val deleteList = response.deleteScriptList
                logger.debug("CardListFragment : Pending Task deleteList isNotEmpty : ${deleteList.isNotEmpty()}")
                logger.debug("CardListFragment : Pending Task deleteList size : ${deleteList.size}")
                if (deleteList.isEmpty() && isDeletingFromPortal) {
                    handleCardAlreadyDeletedOnSe(activity, tokenRefNumber)
                    return
                }
                if (!isNFC()) {
                    executeDeleteScriptWithRetry(
                        response,
                        activity,
                        tokenRefNumber,
                        paymentId,
                        pnoType,
                        scriptIndex = 0,
                        attemptIndex = 0
                    )
                } else {
                    executeDeleteScriptWithRetryNFC(
                        response.deleteScriptList,
                        tokenRefNumber,
                        scriptIndex = 0,
                        attemptIndex = 0,
                        pendingContext = NfcPendingDeleteFlowContext(
                            response,
                            activity,
                            paymentId,
                            pnoType
                        )
                    )
                }
            }

            PendingDeleteTaskResponseHelper.isNoPendingDeleteTask(response) -> {
                handleCardAlreadyDeletedOnSe(activity, tokenRefNumber)
            }

            else -> {
                isDeleteFlow = false
                activity.showLoading(false, "")
                deleteDialog(activity, getString(R.string.text_card_deleted_error))
            }
        }
    }

    /**
     * Executes delete scripts from a pending-task response sequentially (one list entry at a time),
     * with one BLE retry per script ([attemptIndex] 0 then 1).
     */
    private fun executeDeleteScriptWithRetry(
        response: GetPendingResponse,
        activity: MainActivity,
        tokenRefNumber: String,
        paymentId: String,
        pnoType: String,
        scriptIndex: Int,
        attemptIndex: Int
    ) {
        // Resolve seId from prefs when fragment's seId is null so we never send literal "null" in Acknowledge request
        val seIdToUse = resolveSeIdForPendingTask()
        val deleteList = response.deleteScriptList
        val totalScripts = deleteList.size
        if (deleteList.isEmpty() || scriptIndex >= totalScripts) {
            handleDeleteScriptSuccess(activity, seIdToUse, tokenRefNumber)
            return
        }

        if (scriptIndex == 0 && attemptIndex == 0 &&
            !PendingDeleteScriptExecutionGate.tryBegin(seIdToUse)
        ) {
            logger.debug(
                "delete card: pending delete already running for seId=$seIdToUse; skipping"
            )
            isDeleteFlow = false
            activity.showLoading(false, "")
            return
        }

        val jsonBytes = extractJsonBytes(response, scriptIndex)
        if (jsonBytes == null || jsonBytes.isEmpty()) {
            logger.debug(
                "delete card: scriptData missing (script ${scriptIndex + 1}/$totalScripts)"
            )
            handleDeleteScriptFailure(
                scriptIndex, attemptIndex, response, activity, tokenRefNumber, paymentId, pnoType
            )
            return
        }

        StorageRepository.saveString(PreferenceKey.INSTALL_SCRIPT, jsonBytes.contentToString())
        val scriptHandler = ScriptHandler(
            activity, object : ScriptHandler.Callbacks {
                override fun showLoading(show: Boolean, msg: String) {
                    // Empty implementation - loading is handled by the parent fragment's showLoading method
                }

                override fun showToast(message: String) {
                    Toast.makeText(
                        activity.applicationContext, message, Toast.LENGTH_SHORT
                    ).show()
                }

                override fun updateLogs(message: String) {
                    logger.debug("ScriptHandler$message")
                }
            })
        scriptHandler.deleteScript(jsonBytes).thenAccept { success ->
            logger.debug(
                "delete card: Success=$success, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected} " +
                    "(script ${scriptIndex + 1}/$totalScripts, attempt ${attemptIndex + 1})"
            )
            if (success) {
                clearInstallScriptPreference()
                acknowledgeDeleteScriptSuccess(
                    listOf(deleteList[scriptIndex]),
                    activity,
                    seIdToUse,
                    tokenRefNumber
                )
                val next = scriptIndex + 1
                if (next < totalScripts) {
                    executeDeleteScriptWithRetry(
                        response, activity, tokenRefNumber, paymentId, pnoType, next, 0
                    )
                } else {
                    handleDeleteScriptSuccess(activity, seIdToUse, tokenRefNumber)
                }
            } else {
                handleDeleteScriptFailure(
                    scriptIndex, attemptIndex, response, activity, tokenRefNumber, paymentId, pnoType
                )
            }
        }.exceptionally { throwable ->
            logger.debug(
                "delete card: Exception=${throwable.message}, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected} " +
                    "(script ${scriptIndex + 1}/$totalScripts, attempt ${attemptIndex + 1})"
            )
            handleDeleteScriptFailure(
                scriptIndex, attemptIndex, response, activity, tokenRefNumber, paymentId, pnoType
            )
            null
        }
    }

    /**
     * Handles successful completion of all delete scripts in a pending-task response.
     *
     * @param activity        Host activity for dialogs and follow-up pending-task calls.
     * @param seIdToUse       Secure element ID used for acknowledgement and refresh.
     * @param tokenRefNumber  Digitization reference number for the deleted card.
     */
    private fun handleDeleteScriptSuccess(
        activity: MainActivity,
        seIdToUse: String,
        tokenRefNumber: String
    ) {
        clearSavedDefaultIfDeleted(tokenRefNumber)
        PendingDeleteScriptExecutionGate.end(seIdToUse)
        activity.showLoading(true, "")
        clearInstallScriptPreference()
        deleteDialog(activity, getString(R.string.text_card_deleted_successfully))

        logger.debug("seId---delete2->$seIdToUse")
        if (!isBleFlowInProgress || isNFC()) {
            logger.debug("Calling getPendingTask after delete success")
            getPendingTask(activity, seIdToUse, tokenRefNumber)
        } else {
            logger.debug("Skipping duplicate getPendingTask call")
        }
    }

    /**
     * Retries or finalizes failure for one delete script in a pending-task response.
     *
     * @param scriptIndex     Index of the current delete script in the response list.
     * @param attemptIndex    Zero-based attempt index for the current script.
     * @param response        Pending-task response containing delete scripts.
     * @param activity        Host activity for dialogs and loading UI.
     * @param tokenRefNumber  Digitization reference number for the deleted card.
     * @param paymentId       Payment app instance ID for follow-up API calls.
     * @param pnoType         Payment network operator type (MDES or VTS).
     */
    private fun handleDeleteScriptFailure(
        scriptIndex: Int,
        attemptIndex: Int,
        response: GetPendingResponse,
        activity: MainActivity,
        tokenRefNumber: String,
        paymentId: String,
        pnoType: String
    ) {
        if (attemptIndex == 0) {
            logger.debug("Delete script first attempt failed, retrying same script once...")
            executeDeleteScriptWithRetry(
                response, activity, tokenRefNumber, paymentId, pnoType, scriptIndex, 1
            )
            return
        }
        clearInstallScriptPreference()
        logger.debug("delete card: Final failure after retry")

        PendingDeleteScriptExecutionGate.end(
            seId?.takeIf { it.isNotBlank() }
                ?: StorageRepository.readString(key = PreferenceKey.DEVICE_SE_ID)
        )
        isDeleteFlow = false
        deleteDialog(activity, getString(R.string.text_unable_to_delete))
        activity.showLoading(false, "")
    }

    /**
     * Resolves the secure element ID used for pending delete-script execution gates.
     *
     * @return Fragment seId when set, otherwise the stored device seId preference.
     */
    private fun resolveSeIdForPendingDelete(): String =
        seId?.trim().orEmpty().ifEmpty {
            StorageRepository.readString(key = PreferenceKey.DEVICE_SE_ID).trim()
        }

    /**
     * Releases the per-seId gate that prevents concurrent pending delete script execution.
     */
    private fun releasePendingDeleteExecutionGate() {
        val id = resolveSeIdForPendingDelete()
        if (id.isNotEmpty()) {
            PendingDeleteScriptExecutionGate.end(id)
        }
    }

    /**
     * Clears the temporary install/delete script payload stored in preferences.
     */
    private fun clearInstallScriptPreference() {
        StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)
    }

    /**
     * Extracts and decodes JSON data from the `deleteScriptList` field in the
     * [UpdateCardStatusResponse] object.
     *
     * @param response The [UpdateCardStatusResponse] containing script data for card operations.
     * @return A UTF-8 encoded byte array representing the decoded JSON object, or `null`
     * if decoding fails or data is missing.
     */
    private fun extractJsonBytes(response: GetPendingResponse, scriptIndex: Int): ByteArray? {
        return try {
            val scriptData = response.deleteScriptList.getOrNull(scriptIndex)?.scriptData
            if (scriptData.isNullOrEmpty()) {
                logger.debug("scriptData is null or empty")
                return null
            }

            ScriptDataParser.decodeToJsonBytes(scriptData)?.also {
                val decodedString = String(it, Charsets.UTF_8)
                logger.debug("Decoded scriptData -> $decodedString")
            }
        } catch (e: Exception) {
            logger.noStackTraceLog("ExtractJsonBytes ", e)
            null
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
     * - On success, processes any `deleteScriptList` from response (BLE execution + per-script acknowledge).
     * - On error or empty response, stops the loading and optionally shows a message.
     */
    private fun getPendingTask(
        activity: MainActivity, seId: String, digitizeRef: String
    ) {
        fetchSequenceNumberFromDevice(
            onRetrieved = { currentSequenceCounter ->
                logger.debug("currentSequenceCounter : $currentSequenceCounter")
                handleGetPendingTaskFlow(activity, seId, digitizeRef, currentSequenceCounter)
            },
            onFailed = {
                if (isAddCardFlowActive) {
                    resetAddCardFlowState()
                    statusDialog(
                        activity,
                        getString(R.string.failed_to_add_card_bluetooth_connection_lost)
                    )
                }
            }
        )
    }

    /**
     * Shows the appropriate loading UI while the pending-task API request is in flight.
     *
     * Uses the add-card flow loader during card provisioning; otherwise shows the standard
     * activity loading overlay.
     *
     * @param activity The [MainActivity] used to display loading when not in add-card provisioning.
     */
    private fun showGetPendingTaskLoading(activity: MainActivity) {
        if (isAddCardProvisioningFlow()) {
            showAddCardFlowLoading()
        } else {
            activity.showLoading(true, getString(R.string.text_please_wait))
        }
    }

    /**
     * Handles a failed `getPendingTask` API response.
     *
     * Resets delete-flow and add-card state as needed, dismisses loading UI, and routes
     * session-expiry or other errors through [handleSessionOrError].
     *
     * @param activity The [MainActivity] used to hide loading on failure.
     * @param errorMessage The error message returned from the SDK.
     */
    private fun handleGetPendingTaskFailure(activity: MainActivity, errorMessage: String) {
        isDeleteFlow = false
        val fromPrepSeFailure = getPendingTaskFromPrepSeFailure
        if (getPendingTaskFromPrepSeFailure) {
            getPendingTaskFromPrepSeFailure = false
        }
        if (isAddCardFlowActive && !fromPrepSeFailure) {
            resetAddCardFlowState()
        } else if (isAddCardProvisioningFlow()) {
            dismissAddCardFlowLoading()
        } else {
            activity.showLoading(false, "")
        }
        handleSessionOrError(errorMessage)
    }

    /**
     * Invokes the pending-task API and dispatches success or failure handling.
     *
     * Shows loading, calls [WalletRepository.getPendingTask], then processes the response
     * on success or delegates to [handleGetPendingTaskFailure] on error.
     *
     * @param activity The [MainActivity] context for loading and UI callbacks.
     * @param seId The Secure Element ID of the connected device.
     * @param digitizeRef The digitization reference number of the card.
     * @param currentSequenceCounter Sequence counter obtained from the wearable.
     */
    private fun handleGetPendingTaskFlow(
        activity: MainActivity,
        seId: String,
        digitizeRef: String,
        currentSequenceCounter: String
    ) {
        lifecycleScope.launch {
            if (activity.isFinishing) return@launch
            showGetPendingTaskLoading(activity)
            logger.debug("getPendingTask seid--->${seId}")

            val sdkResult = WalletRepository.getPendingTask(
                context = activity,
                seId = seId,
                digitizationReferenceNumber = digitizeRef,
                currentSequenceCounter = currentSequenceCounter
            )

            if (sdkResult.isSuccess) {
                handleGetPendingTaskSuccess(sdkResult.response, activity, seId)
            } else {
                handleGetPendingTaskFailure(activity, sdkResult.errorMessage)
            }
        }
    }

    /**
     * Handles a successful response from the `getPendingTask` API call.
     *
     * @param response The response object[GetPendingResponse] received from the SDK.
     * @param activity The [MainActivity] used for updating UI elements.
     * @param seId The Secure Element ID associated with the request.
     */
    private fun handleGetPendingTaskSuccess(
        response: GetPendingResponse?, activity: MainActivity, seId: String
    ) {
        isDeletingFromPortal = false
        val pendingDeleteScriptsPending = shouldRunPendingDeleteScripts(response)
        if (!pendingDeleteScriptsPending && !isAddCardProvisioningFlow()) {
            activity.showLoading(false, "")
        }
        if (response != null) {
            if (response.statusMessage.isNullOrEmpty()) {
                val fromPrepSeFailure = getPendingTaskFromPrepSeFailure
                if (getPendingTaskFromPrepSeFailure) {
                    getPendingTaskFromPrepSeFailure = false
                }
                isDeleteFlow = false
                if (isAddCardFlowActive && !fromPrepSeFailure) {
                    resetAddCardFlowState()
                }
                return
            }
            activity.runOnUiThread {
                handleGetPendingTaskResponse(response, activity, seId)
            }
        } else {
            isDeleteFlow = false
            if (getPendingTaskFromPrepSeFailure) {
                getPendingTaskFromPrepSeFailure = false
            }
        }
    }

    /**
     * Evaluates the `GetPendingResponse` status and routes to the appropriate handler.
     *
     * @param response The parsed [GetPendingResponse] from the SDK.
     * @param activity The [MainActivity] context for UI interactions.
     * @param seId The Secure Element ID linked to the task.
     */
    private fun handleGetPendingTaskResponse(
        response: GetPendingResponse, activity: MainActivity, seId: String
    ) {
        if (response.statusMessage == CommonResponse.SUCCESS.response) {
            handleGetPendingTaskSuccessResponseFlow(response, activity, seId)
            return
        }
        if (isAddCardProvisioningFlow() && PendingDeleteTaskResponseHelper.isNoPendingDeleteTask(response)) {
            handleGetPendingTaskSuccessResponseFlow(response, activity, seId)
            return
        }
        handleGetPendingTaskNonSuccessResponse(response)
    }

    /**
     * Returns whether a successful get-pending response includes BLE delete scripts to execute.
     *
     * @param response The get-pending API response, or null.
     */
    private fun shouldRunPendingDeleteScripts(response: GetPendingResponse?): Boolean {
        return response != null &&
            response.statusMessage == CommonResponse.SUCCESS.response &&
            response.deleteScriptList.isNotEmpty() &&
            !isNFC()
    }

    /**
     * Routes a successful get-pending response to add-card navigation, background cleanup,
     * or sequential delete-script execution depending on flow state and transport.
     *
     * @param response The successful get-pending API response.
     * @param activity The host [MainActivity].
     * @param seId Secure Element ID of the linked device.
     */
    private fun handleGetPendingTaskSuccessResponseFlow(
        response: GetPendingResponse,
        activity: MainActivity,
        seId: String
    ) {
        if (getPendingTaskFromPrepSeFailure) {
            getPendingTaskFromPrepSeFailure = false
        }
        if (response.deleteScriptList.isEmpty()) {
            isDeleteFlow = false
            if (isAddCardProvisioningFlow()) {
                navigateAddToCard()
            }
            return
        }
        if (!isNFC()) {
            if (PendingDeleteScriptExecutionGate.isInProgress(seId)) {
                logger.debug(
                    "getPending delete scripts skipped; already running for seId=$seId"
                )
                if (isAddCardProvisioningFlow()) {
                    navigateAddToCard()
                }
                return
            }
            if (isAddCardProvisioningFlow()) {
                runAddCardPendingDeleteScriptsInBackground(response.deleteScriptList)
                navigateAddToCard()
                return
            }
            executeDeleteScriptWithRetry(response.deleteScriptList, "", 0)
            return
        }
        executePendingTaskScriptNfc(response, activity, seId)
    }

    /**
     * Starts the NFC pending-task delete-script chain from the first script index.
     *
     * @param response The get-pending response containing delete scripts.
     * @param activity The host [MainActivity].
     * @param seId Secure Element ID of the linked device.
     */
    private fun executePendingTaskScriptNfc(
        response: GetPendingResponse,
        activity: MainActivity,
        seId: String
    ) {
        runPrepSePendingDeleteNfcScript(response, activity, seId, scriptIndex = 0, attemptIndex = 0)
    }

    /**
     * Runs each NFC delete script from a prep-SE pending response in order (with one retry per script).
     */
    private fun runPrepSePendingDeleteNfcScript(
        response: GetPendingResponse,
        activity: MainActivity,
        seId: String,
        scriptIndex: Int,
        attemptIndex: Int
    ) {
        val list = response.deleteScriptList
        if (list.isEmpty()) {
            hideNfcSheet()
            navigateAddToCard()
            return
        }
        if (scriptIndex >= list.size) {
            hideNfcSheet()
            navigateAddToCard()
            return
        }

        val scriptItem = list[scriptIndex]
        val scriptData = getScriptDataFromItem(scriptItem)
        val jsonBytes = scriptData?.let { extractJsonBytes(it) }

        if (jsonBytes == null || jsonBytes.isEmpty()) {
            logger.debug("prep se NFC: invalid script data at index $scriptIndex (attempt ${attemptIndex + 1})")
            if (attemptIndex == 0) {
                runPrepSePendingDeleteNfcScript(response, activity, seId, scriptIndex, 1)
            } else {
                hideNfcSheet()
                navigateAddToCard()
            }
            return
        }

        showNfcSheet(parentFragmentManager, onCancelClick = { navigateAddToCard() })
        NfcScriptExecutionTracker.onNfcScriptStarted()
        SecoraWearableSDK.getInstance().getInterface().executeNfcOperation(
            activity,
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
                    responseItems: MutableList<ApduResponsesItem>?,
                    completed: Boolean
                ) {
                    if (!completed) return
                    NfcScriptExecutionTracker.onNfcScriptFinished()
                    hideNfcSheet()
                    val scriptBase = scriptItem as? DeleteScriptBase
                    scriptBase?.let {
                        acknowledgeDeleteScriptSuccess(listOf(it), activity, seId, "")
                    }
                    val next = scriptIndex + 1
                    if (next < list.size) {
                        runPrepSePendingDeleteNfcScript(response, activity, seId, next, 0)
                    } else {
                        navigateAddToCard()
                    }
                }

                override fun onError(error: String) {
                    NfcScriptExecutionTracker.onNfcScriptFinished()
                    hideNfcSheet()
                    if (attemptIndex == 0) {
                        runPrepSePendingDeleteNfcScript(response, activity, seId, scriptIndex, 1)
                    } else {
                        navigateAddToCard()
                        logger.debug("onError: $error")
                    }
                }
            }
        )
    }

    /**
     * Handles non-success get-pending responses by showing prep-SE failure UI or continuing
     * add-card navigation when appropriate.
     *
     * @param response The get-pending API response with a non-success status.
     */
    private fun handleGetPendingTaskNonSuccessResponse(response: GetPendingResponse) {
        if (shouldShowPrepSeFailureDialog(response)) return
        if (isDeleteFlow) {
            isDeleteFlow = false
            logger.debug("Skipping navigation after delete flow")
            return
        }
        if (!isNFC() && isNavigatingToAddCard) {
            logger.debug("Already navigating → skipping duplicate navigation")
            return
        }

        if (!isNFC()) {
            isBleFlowInProgress = false
        }
        if (!isAddCardProvisioningFlow()) {
            activity.showLoading(false, "")
        }

        isNotificationPending = false
        logger.debug("Navigating to Add Card screen")

        cardDetails?.let {
            handleAddCardClick(it)
        } ?: navigateAddToCard()
    }

    /**
     * Shows the stored prep-SE failure message when get-pending confirms no pending tasks remain.
     *
     * @param response The get-pending API response.
     * @return `true` when the prep-SE failure dialog was shown and further handling should stop.
     */
    private fun shouldShowPrepSeFailureDialog(response: GetPendingResponse): Boolean {
        if (!getPendingTaskFromPrepSeFailure) return false
        getPendingTaskFromPrepSeFailure = false
        if (response.statusCode != Constants.GET_DEVICE_PENDING_TASK_EMPTY_CODE) return false
        resetAddCardFlowState()
        statusDialog(activity, prepSeFailureStatusMessage)
        return true
    }

    /**
     * Extracts scriptData from a script item using reflection
     * @param scriptItem The script item object
     * @return The scriptData string or null if not found
     */
    private fun getScriptDataFromItem(scriptItem: Any): String? {
        return when (scriptItem) {
            is DeleteScriptList -> scriptItem.scriptData
            is DeleteScriptResponse -> scriptItem.scriptData
            is DeleteScript -> scriptItem.scriptData
            is ScriptItem -> scriptItem.scriptData
            else -> null
        }
    }

    /**
     * Decodes a Base64-encoded JSON string into a byte array.
     * Handles nested Base64 layers and verifies JSON format.
     * Returns null if decoding fails.
     */
    private fun extractJsonBytes(scriptData: String): ByteArray? {
        return try {
            if (scriptData.isEmpty()) {
                logger.debug("scriptData is null or empty")
                return null
            }

            ScriptDataParser.decodeToJsonBytes(scriptData)?.also {
                val decodedString = String(it, Charsets.UTF_8)
                logger.debug("Decoded scriptData -> $decodedString")
            }

        } catch (e: Exception) {
            logger.noStackTraceLog("ExtractJsonBytes ", e)
            null
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
     * - Otherwise, shows a user-friendly.
     */
    private fun acknowledgePendingTask(
        activity: MainActivity, seId: String, scriptId: Int, digitizeRef: String
    ) {

        lifecycleScope.launch {
            if (activity.isFinishing) return@launch
            val sdkResult = WalletRepository.acknowledgePendingTask(
                context = activity,
                seId = seId,
                scriptId = scriptId,
                digitizeRef = digitizeRef
            )

            if (sdkResult.isSuccess) {
                handleAcknowledgePendingTaskSuccess(sdkResult.response, activity)
            } else {
                activity.showLoading(false, "")
                handleSessionOrError(sdkResult.errorMessage)
            }
        }
    }

    /**
     * Handles a successful response from the "Acknowledge Pending Task" API.
     *
     * @param response The response object[AcknowledgeResponse] returned from the SDK.
     * @param activity The [MainActivity] instance used for UI updates.
     */
    private fun handleAcknowledgePendingTaskSuccess(
        response: AcknowledgeResponse?,
        activity: MainActivity
    ) {
        isDeletingFromPortal = false
        if (response != null) {
            if (response.statusMessage.isNullOrEmpty()) {
                activity.showLoading(false, "")
                return
            }
            activity.runOnUiThread {
                handleAcknowledgePendingTaskResponse(response)
            }
        } else {
            activity.showLoading(false, "")
        }
    }

    /**
     * Handles the processed acknowledgment task response.
     *
     * @param response The [AcknowledgeResponse] received from the SDK.
     */
    private fun handleAcknowledgePendingTaskResponse(response: AcknowledgeResponse) {
        when (response.statusMessage) {
            CommonResponse.SUCCESS.response -> {
                // Success case - no action needed
                if (getPending) {
                    cardDetails?.let { checkForDuplicateCard(it) }
                }
            }

            else -> {
                logger.info("Status message: ${response.statusMessage}")
            }
        }
    }

    /**
     * Displays a non-cancelable dialog with a custom message.
     *
     * @param context The [Context] used to create and show the dialog.
     * @param message The message text to display inside the dialog.
     */
    fun deleteDialog(context: Context, message: String?) {
        (context as? Activity)?.runOnUiThread {

            val dialogViewBinding = DialogCommonMessageBinding.inflate(
                LayoutInflater.from(context)
            )
            val alertDialog = Dialog(context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(dialogViewBinding.root)
                setCancelable(false)
            }

            dialogViewBinding.txtTitle.text = context.getString(R.string.text_secora_wallet)
            dialogViewBinding.txtMessage.text = message
            dialogViewBinding.txtCancel.visibility = View.GONE
            dialogViewBinding.txtOK.setOnClickListener {
                alertDialog.dismiss()
            }
            alertDialog.showSecure()
        }
    }

    /**
     * Promotes [targetCard] to default by running PPSE on the wearable and calling set-default API.
     *
     * @param response Provisioned card list used to refresh UI after promotion.
     * @param safeContext Application context for SDK calls.
     * @param targetCard The card to promote as default.
     */
    private fun checkAndSetCardAsDefault(
        response: GetProvisionCardResponse,
        safeContext: Context,
        targetCard: CardList
    ) {
        val paymentId = StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID)
        val deviceKey = PreferenceKey.deviceKey(paymentId)
        val savedDefaultTokenRef = StorageRepository.readString(deviceKey)
        val targetTokenRef = targetCard.digitizationReferenceNumber?.toString() ?: return
        handleDefaultOn(
            response,
            safeContext,
            activity,
            targetTokenRef,
            savedDefaultTokenRef,
            deviceKey
        )
    }

    /**
     * Handles the flow when the default card toggle is turned ON.
     * Extracted from the main method to reduce cognitive complexity.
     * Logic remains exactly the same as the original.
     */
    private fun handleDefaultOn(
        response: GetProvisionCardResponse,
        safeContext: Context,
        activity: MainActivity,
        targetTokenRef: String,
        savedDefaultTokenRef: String?,
        deviceKey: String
    ) {
        val scriptHandler = createScriptHandler()
        activity.showLoading(true, getString(R.string.text_please_wait))
        executePPSE(
            response,
            safeContext,
            activity,
            scriptHandler,
            targetTokenRef,
            savedDefaultTokenRef,
            deviceKey
        )
    }

    /**
     * Executes a PPSE Script and handles the result callbacks.
     * This method preserves the original full flow, including:
     * - Updating UI
     * - Toggling default card state
     * - Saving user preference
     * - Showing dialogs/logs
     * - Broadcasting default card change
     */
    private fun executePPSE(
        response: GetProvisionCardResponse,
        safeContext: Context,
        activity: MainActivity,
        scriptHandler: ScriptHandler,
        targetTokenRef: String,
        savedDefaultTokenRef: String?,
        deviceKey: String
    ) {
        val currentTokenRef = targetTokenRef
        val aid = StorageRepository.readString(PreferenceKey.cardAidKey(currentTokenRef))
            .takeIf { it.isNotBlank() }
            ?: StorageRepository.readString(PreferenceKey.spsdAppletInstanceAidKey(currentTokenRef))
        val cardType = StorageRepository.readString(PreferenceKey.aidCardTypeKey(currentTokenRef))
        val otherCardAppletInstanceAids = resolveOtherCardAppletInstanceAids(response.cardList, currentTokenRef)
        logger.debug(":: aid : $aid")
        logger.debug(":: cardType : $cardType")
        logger.debug(":: otherCardAppletInstanceAids : $otherCardAppletInstanceAids")

        if (!isNFC()) {
            scriptHandler.executePPSEScript(aid, cardType, otherCardAppletInstanceAids)
                .thenAccept { retrySuccess ->
                    activity.runOnUiThread {
                        if (retrySuccess) {
                            setCardAsDefaultApiCall(
                                response,
                                safeContext,
                                activity,
                                currentTokenRef,
                                savedDefaultTokenRef,
                                deviceKey
                            )
                        } else {
                            activity.showLoading(false, "")
                            logger.debug("Script Failed : failed_to_set_default_card")
                            cardListUiUpdateFlow(response, safeContext)
                        }
                    }
                }.exceptionally { retryThrowable ->
                    activity.runOnUiThread {
                        logger.debug("Script Failed : exception")
                        activity.showLoading(true, getString(R.string.text_please_wait))
                        logger.debug("PPSE: Retry failed, exception=${retryThrowable.message}, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
                        cardListUiUpdateFlow(response, safeContext)
                    }
                    null
                }
        } else {
            showNfcSheet(parentFragmentManager, onCancelClick = {
                cardListUiUpdateFlow(response, safeContext)
            })
            NfcScriptExecutionTracker.onNfcScriptStarted()
            SecoraWearableSDK.getInstance().getInterface().executeNfcOperation(
                activity,
                PPSE,
                null,
                aid,
                cardType,
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
                        responseItems: MutableList<ApduResponsesItem>?,
                        completed: Boolean
                    ) {
                        if (!completed) {
                            logger.debug("PPSE NFC progress callback received, waiting for completion")
                            return
                        }
                        NfcScriptExecutionTracker.onNfcScriptFinished()
                        hideNfcSheet()
                        setCardAsDefaultApiCall(
                            response,
                            safeContext,
                            activity,
                            currentTokenRef,
                            savedDefaultTokenRef,
                            deviceKey
                        )
                    }

                    override fun onError(error: String?) {
                        NfcScriptExecutionTracker.onNfcScriptFinished()
                        activity.runOnUiThread {
                            hideNfcSheet()
                            logger.debug("Script Failed : exception")
                            activity.showLoading(true, getString(R.string.text_please_wait))
                            logger.debug("PPSE: Retry failed, exception=${error}, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
                            cardListUiUpdateFlow(response, safeContext)
                        }
                    }

                })
        }
    }

    /**
     * Resolves first applet instance AID for every card except the one being set as default.
     */
    private fun resolveOtherCardAppletInstanceAids(
        cardList: List<CardList>,
        currentTokenRef: String
    ): List<String> {
        return cardList
            .asSequence()
            .filter { it.digitizationReferenceNumber?.toString() != currentTokenRef }
            .mapNotNull { card ->
                card.appletInstanceAids?.firstOrNull()?.takeIf { it.isNotBlank() }
            }
            .distinct()
            .toList()
    }

    /**
     * Calls the wallet SDK set-default API and refreshes the card list on success or error.
     *
     * @param provisionCardResponse Provisioned card list for UI refresh.
     * @param safeContext Application context for SDK calls.
     * @param activity The host [MainActivity].
     * @param currentTokenRef Digitization reference of the card being set as default.
     * @param savedDefaultTokenRef Previously saved default-card token reference.
     * @param deviceKey Preference key used to persist the default-card token reference.
     */
    private fun setCardAsDefaultApiCall(
        provisionCardResponse: GetProvisionCardResponse,
        safeContext: Context,
        activity: MainActivity,
        currentTokenRef: String,
        savedDefaultTokenRef: String?,
        deviceKey: String
    ) {
        CoroutineScope(AppDispatchers.IO).launch {
            val sdkResult = WalletRepository.setCardAsDefault(
                context = safeContext,
                digitizationReferenceNumber = currentTokenRef
            )

            viewLifecycleOwner.lifecycleScope.launch(AppDispatchers.MAIN) {
                if (sdkResult.isSuccess) {
                    sdkResult.response?.let { defaultCardResponse ->
                        activity.showLoading(true, getString(R.string.text_please_wait))
                        if (!defaultCardResponse.statusMessage.isNullOrEmpty() &&
                            defaultCardResponse.statusMessage == CommonResponse.SUCCESS.response &&
                            savedDefaultTokenRef != currentTokenRef
                        ) {
                            StorageRepository.saveString(deviceKey, currentTokenRef)
                            EventBus.post(DEFAULT_CARD_CHANGE)
                        }
                        cardListUiUpdateFlow(provisionCardResponse, safeContext)
                    }
                } else {
                    handleSessionOrError(sdkResult.errorMessage) {
                        cardListUiUpdateFlow(provisionCardResponse, safeContext)
                    }
                }
            }
        }
    }

    /**
     * Persists provisioned cards locally and rebuilds the card-list UI, adapter, and images.
     *
     * @param provisionCardResponse Provisioned card list from the backend.
     * @param safeContext Application context for database and SDK operations.
     */
    private fun cardListUiUpdateFlow(
        provisionCardResponse: GetProvisionCardResponse,
        safeContext: Context
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val pid = paymentAppInstanceId?.takeIf { it.isNotBlank() }
            if (pid != null) {
                StorageRepository.saveProvisionedCardsToLocalDb(safeContext, pid, provisionCardResponse)
                cardListDetail = StorageRepository.getUiCardListFromLocalDb(safeContext, pid)
                loadCardImagesFromDatabase()
            }
            setupCardListUI(provisionCardResponse)
            if (pid != null) {
                setupCardListAdapter()
            } else {
                setupCardAdapter(provisionCardResponse, safeContext)
            }
            loadCardImages(provisionCardResponse)
        }
    }

    /**
     * Creates and returns a [ScriptHandler] instance with required callbacks.
     *
     * Uses [requireContext] for initialization. Handles toast display and log updates while
     * delegating loading UI to the main activity (the [showLoading] callback is intentionally empty).
     *
     * @return Configured [ScriptHandler] instance for PPSE and related flows.
     */
    private fun createScriptHandler(): ScriptHandler {
        return ScriptHandler(
            requireContext(), object : ScriptHandler.Callbacks {
                override fun showLoading(show: Boolean, msg: String) {
                    // Intentionally left blank because the loading UI is handled elsewhere
                    // and ScriptHandler requires this callback to be implemented.
                }

                override fun showToast(message: String) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }

                override fun updateLogs(message: String) {
                    logger.debug("ScriptHandler: $message")
                }
            })
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
                // The status pills refresh via WearableStatusMonitor's own Bluetooth listener.
            }
        }
    }

    /**
     * Lifecycle callback:
     * Registers Bluetooth UI state listener when Fragment becomes visible.
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
     * Handles the logic executed when "Add Card" is clicked.
     * Performs various pre-checks before proceeding
     *
     * If all checks pass, it proceeds to verify duplicate cards.
     */
    private fun handleAddCardClick(capturedCardDetail: CapturedCardDetail) {
        when {
            !isNetworkAvailable(requireContext()) -> {
                confirmDataDialog(
                    resources.getString(R.string.data_enable)
                )
            }

            !isNFC() && !isBluetoothReadyForWearable() -> {
                statusDialog(
                    requireContext(), getString(R.string.bluetooth_not_turned_on)
                )
            }

            !isNFC() && !BluetoothStateManager.isDeviceConnected(seId, requireContext()) -> {
                ensureBleConnectedThenRun(
                    onConnected = {
                        checkForDuplicateCard(capturedCardDetail)
                    }
                )
            }

            else -> {
                checkForDuplicateCard(capturedCardDetail)
            }
        }
    }

    /**
     * Checks if the entered card number already exists in the saved card list.
     */
    private fun checkForDuplicateCard(capturedCardDetail: CapturedCardDetail) {
        CoroutineScope(Dispatchers.IO).launch {
            val isDuplicate = cardList.any { cardNum ->
                cardNum.dpanSuffix == capturedCardDetail.maskedCardNumber.takeLast(4)
            }

            if (isDuplicate) {
                activity.runOnUiThread {
                    if (isAddCardFlowActive) {
                        resetAddCardFlowState()
                    }
                    statusDialog(requireContext(), getString(R.string.duplicate_card))
                }
            } else {
                activity.showLoading(true, getString(R.string.text_please_wait))
                logger.debug("prepSEApiCall: Initial BluetoothStateManager.isConnected = ${BluetoothStateManager.isConnected}")

                fetchSequenceNumberFromDevice(
                    onRetrieved = { currentSequenceCounter ->
                        handlePrepSeFlow(capturedCardDetail, currentSequenceCounter)
                    },
                    onFailed = {
                        activity.showLoading(false, "")
                        if (isAddCardFlowActive) {
                            resetAddCardFlowState()
                        }
                    })

            }
        }
    }

    /**
     * Continues add-card provisioning by calling prepare-secure-element with wearable metadata.
     *
     * @param capturedCardDetail Card details captured from the wallet SDK.
     * @param currentSequenceCounter Sequence counter read from the secure element.
     */
    private fun handlePrepSeFlow(capturedCardDetail: CapturedCardDetail, currentSequenceCounter: String) {
        lifecycleScope.launch {
            if (activity.isFinishing) return@launch
            logger.debug("currentSequenceCounter : $currentSequenceCounter")
            WalletRepository.syncOemDetailsFromPreferences(activity)
            val wearableName = StorageRepository.readString(PreferenceKey.DEVICE_NAME)
            val wearableModelIdHex = StorageRepository.readString(PreferenceKey.WEARABLE_MODEL_ID)

            callPrepareSecureElement(
                capturedCardDetail = capturedCardDetail,
                wearableName = wearableName,
                wearableModelIdHex = wearableModelIdHex,
                currentSequenceCounter = currentSequenceCounter
            )
        }
    }

    /**
     * Initiates secure element preparation for the given card and wearable.
     */
    private suspend fun callPrepareSecureElement(
        capturedCardDetail: CapturedCardDetail,
        wearableName: String,
        wearableModelIdHex: String = "",
        currentSequenceCounter: String
    ) {
        val sdkResult = WalletRepository.prepareSecureElement(
            context = activity,
            seId = seId ?: "",
            paymentAppInstanceId = paymentAppInstanceId ?: "",
            device = wearableName,
            wearableModelIdHex = wearableModelIdHex,
            currentSequenceCounter = currentSequenceCounter
        )

        if (sdkResult.isSuccess) {
            handlePrepareSecureElementSuccess(sdkResult.response, capturedCardDetail)
        } else {
            logger.debug("prepareSecureElement: Error, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
            activity.showLoading(false, "")
            handleSessionOrError(sdkResult.errorMessage)
        }
    }

    /**
     * Handles a successful response from the Prepare Secure Element (Prep SE) API call.
     *
     * This function processes the response, saves necessary data, and prepares the script for installation.
     *
     * @param response The raw API response object[PrepSeResponseBody].
     * @param capturedCardDetail The [CapturedCardDetail] being processed.
     */
    private fun handlePrepareSecureElementSuccess(
        response: PrepSeResponseBody?, capturedCardDetail: CapturedCardDetail
    ) {
        if (response == null) {
            logger.debug("prepareSecureElement: Null response, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
            showStatusAndStopLoading(getString(R.string.no_response_data_received))
            return
        }
        if (!response.statusMessage.equals(SUCCESS_MESSAGE, ignoreCase = true)){
            showStatusAndStopLoading(response.statusMessage.toString())
            return
        }
        logger.debug("prepareSecureElement: Success, status=${response.statusMessage}, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
        response.digitizationReferenceNumber?.toString()?.takeIf { it.isNotBlank() }?.let {
            digitizationReferenceNumber = it
        }

        if (handleOnboardingFetchInstallScriptIfNeeded(response)) return

        if (response.statusMessage.isNullOrEmpty()) {
            showStatusAndStopLoading(getString(R.string.no_response_data_received))
            return
        }

        if (isVisaPno(response.pnoType)) {
            runPrepareSecureElementResponseOnUiThread(
                response, createScriptHandler(), null, capturedCardDetail
            )
            return
        }

        val jsonString = extractJsonBytes(response)
        if (jsonString == null) {
            showStatusAndStopLoading(getString(R.string.no_response_data_received))
            return
        }
        StorageRepository.saveString(PreferenceKey.INSTALL_SCRIPT, jsonString.contentToString())
        runPrepareSecureElementResponseOnUiThread(
            response, createScriptHandler(), jsonString, capturedCardDetail
        )
    }

    /**
     * Handles the onboarding status that defers install-script delivery to get-pending.
     *
     * When [PrepSeResponseBody.statusCode] is [ONBOARDING_FETCH_INSTALL_SCRIPT_0002], the backend
     * has not returned an install script in the prep-SE response. This method flags the flow as
     * get-pending driven, records the failure message for later UI, and invokes [getPendingTask]
     * to retrieve the script asynchronously.
     *
     * @param response The [PrepSeResponseBody] from the prepare-SE API.
     * @return `true` when the onboarding branch was taken and normal prep-SE handling must stop.
     */
    private fun handleOnboardingFetchInstallScriptIfNeeded(response: PrepSeResponseBody): Boolean {
        if (!response.statusCode.equals(ONBOARDING_FETCH_INSTALL_SCRIPT_0002)) return false
        getPending = true
        getPendingTaskFromPrepSeFailure = true
        prepSeFailureStatusMessage = response.statusMessage.toString()
        isNotificationPending = false
        getPendingTask(activity, seId ?: "", "")
        return true
    }

    /**
     * Dispatches [handlePrepareSecureElementResponse] on the main thread when the activity is active.
     *
     * Prep-SE callbacks may arrive off the UI thread; script execution and navigation must run on
     * the main looper. No-op if the fragment activity is not initialized or is finishing.
     *
     * @param response The [PrepSeResponseBody] from the prepare-SE API.
     * @param scriptHandler [ScriptHandler] used for BLE install-script execution.
     * @param jsonString Decoded install-script JSON bytes, or `null` for Visa flows that skip local decoding.
     * @param capturedCardDetail The [CapturedCardDetail] being provisioned.
     */
    private fun runPrepareSecureElementResponseOnUiThread(
        response: PrepSeResponseBody,
        scriptHandler: ScriptHandler,
        jsonString: ByteArray?,
        capturedCardDetail: CapturedCardDetail,
    ) {
        if (::activity.isInitialized && !activity.isFinishing) {
            activity.runOnUiThread {
                handlePrepareSecureElementResponse(
                    response, scriptHandler, jsonString, capturedCardDetail
                )
            }
        }
    }

    /**
     * Hides the loading indicator and displays a status dialog with the given message.
     */
    private fun showStatusAndStopLoading(message: String) {
        activity.showLoading(false, "")
        logger.debug("showStatusAndStopLoading: message=$message, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
        if (isAddCardFlowActive) {
            PayHostFssSync.onAddCardFailed()
            resetAddCardFlowState()
        }
        statusDialog(activity, message)
    }

    /**
     * Extracts and decodes the JSON installation script bytes from the [PrepSeResponseBody].
     *
     * The script data inside the Prep SE response is typically Base64-encoded.
     * In some cases, it may be double-encoded (Base64 within Base64), so this method
     * performs an additional decoding pass if necessary.
     *
     * @param response The [PrepSeResponseBody] containing Base64-encoded script data.
     * @return A UTF-8 encoded [ByteArray] representing the decoded JSON script,
     *         or `null` if decoding or parsing fails.
     */
    private fun extractJsonBytes(response: PrepSeResponseBody): ByteArray? {
        return try {
            logger.debug("extractJsonBytes--->Prepse script Response: ${Gson().toJson(response.script)}")
            val scriptData = Gson().toJson(response.script?.get(0)?.scriptData)

            val scriptData1 = response.script?.get(0)?.scriptData
            if (scriptData.isNullOrEmpty()) {
                logger.debug(SCRIPT_DATA_NULL_OR_EMPTY)
                return null
            }
            val normalizedBytes = ScriptDataParser.decodeToJsonBytes(scriptData1) ?: return null
            val decodedString = String(normalizedBytes, Charsets.UTF_8)
            logger.debug("Decoded scriptData -> $decodedString")

            val parsed = JSONTokener(decodedString).nextValue()
            if (parsed is JSONObject) {
                val ppseFileName = parsed.optString(JsonKey.PPSE_FILE_NAME, "")
                if (ppseFileName.isNotEmpty()) {
                    logger.debug("PPSE_file_Name: $ppseFileName")
                }
                parsed.remove(JsonKey.PPSE_FILE_NAME)
                parsed.remove(JsonKey.SCRIPT_NAME)
                parsed.remove(JsonKey.OEM_NAME)
                parsed.remove(JsonKey.AP_NAME)
                parsed.remove(JsonKey.SE_GROUP_NAME)
                parsed.remove(JsonKey.TLV_A6)
                parsed.remove(JsonKey.INSTALL_INSTANCE_AIDS)
                parsed.toString().toByteArray(Charsets.UTF_8)
            } else {
                normalizedBytes
            }
        } catch (e: Exception) {
            logger.noStackTraceLog("ExpectJsonBytes ", e)
            null
        }
    }

    /**
     * Handles the response from the Prepare Secure Element (Prep SE) API call.
     *
     * Depending on the response status, this method routes control to appropriate handlers such as
     * executing the DSEMS script, handling access token expiration, or showing an error message.
     *
     * @param response The [PrepSeResponseBody] returned from the Prep SE API.
     * @param scriptHandler The [ScriptHandler] used to execute DSEMS scripts.
     * @param jsonString The JSON byte array representing the installation script.
     * @param capturedCardDetail The [CapturedCardDetail] being processed.
     */
    private fun handlePrepareSecureElementResponse(
        response: PrepSeResponseBody,
        scriptHandler: ScriptHandler,
        jsonString: ByteArray?,
        capturedCardDetail: CapturedCardDetail,
    ) {
        if (response.statusCode == CommonResponse.TOKEN_CONNECTOR_SERVICE_WEARABLE_0001.response) {
            handlePrepSeWearableTokenExpired()
            return
        }

        when (response.statusMessage) {
            CommonResponse.SUCCESS.response -> handlePrepSeSuccessStatus(
                response, scriptHandler, jsonString, capturedCardDetail
            )

            CommonResponse.DSEMS_SCRIPT_NOT_FOUND.response -> {
                logger.debug("prepareSecureElement: DSEMS_SCRIPT_NOT_FOUND, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
                showStatusAndStopLoading(response.statusMessage!!)
            }

            else -> {
                logger.debug("prepareSecureElement: Unknown status=${response.statusMessage}, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
                showStatusAndStopLoading(response.statusMessage.toString())
            }
        }
    }

    /**
     * Handles wearable access-token expiry returned during prepare-SE.
     *
     * Dismisses the loader, resets add-card state when applicable, and routes to the suspend-device
     * notification flow so the user can reconnect the wearable.
     */
    private fun handlePrepSeWearableTokenExpired() {
        activity.showLoading(false, "")
        if (isAddCardFlowActive) {
            resetAddCardFlowState()
        }
        handleSuspendNotification(
            AppEvent(
                "", mapOf(
                    BundleKey.DEVICE_NAME to StorageRepository.readString(PreferenceKey.DEVICE_NAME)
                )
            )
        )
    }

    /**
     * Routes a successful prepare-SE response by payment network and transport.
     *
     * Visa (VTS) skips install-script execution here and proceeds directly to eligibility check.
     * Mastercard (MDES) runs the install script over NFC or BLE via [executeNfcInstallScript] or
     * [handleSuccessResponse] respectively.
     *
     * @param response The successful [PrepSeResponseBody] from prepare-SE.
     * @param scriptHandler [ScriptHandler] for BLE script execution.
     * @param jsonString Decoded install-script JSON bytes for MDES BLE flows.
     * @param capturedCardDetail The [CapturedCardDetail] being provisioned.
     */
    private fun handlePrepSeSuccessStatus(
        response: PrepSeResponseBody,
        scriptHandler: ScriptHandler,
        jsonString: ByteArray?,
        capturedCardDetail: CapturedCardDetail,
    ) {
        if (isVisaPno(response.pnoType)) {
            proceedToCheckEligibilityFromPrepSe(response, capturedCardDetail)
            return
        }
        if (!isNFC()) {
            handleSuccessResponse(scriptHandler, jsonString, response, capturedCardDetail)
        } else {
            executeNfcInstallScript(response, jsonString, capturedCardDetail)
        }
    }

    /**
     * Executes the MDES install script over NFC and continues the add-card flow on completion.
     *
     * Shows the NFC sheet, streams APDU progress for logging, and on success extracts TLV tags 85/86
     * before calling [handleAddMCMSuccess]. Failures route to [handleAddMCMFailure].
     *
     * @param response The [PrepSeResponseBody] from the prepare-SE API.
     * @param jsonString Decoded install-script JSON bytes to send to the secure element.
     * @param capturedCardDetail The [CapturedCardDetail] being provisioned.
     */
    private fun executeNfcInstallScript(
        response: PrepSeResponseBody,
        jsonString: ByteArray?,
        capturedCardDetail: CapturedCardDetail,
    ) {
        val nfcApduResponses = mutableListOf<String>()
        showNfcSheet(parentFragmentManager, onCancelClick = {
            findNavController().navigate(R.id.cardListFragment)
        })
        NfcScriptExecutionTracker.onNfcScriptStarted()
        SecoraWearableSDK.getInstance().getInterface().executeNfcOperation(
            activity,
            SCRIPT,
            jsonString,
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
                    handleNfcCplcCallback(
                        seid,
                        tagId,
                        icTypeHex,
                        oemIdHex,
                        seGroupIdHex,
                        wearableModelIdHex
                    )
                }

                override fun onApduProgress(request: String?, response: String?) {
                    logger.debug("APDU REQUEST=$request \n RESPONSE=$response")
                    if (!response.isNullOrBlank()) {
                        nfcApduResponses.add(response)
                    }
                }

                override fun onSuccess(
                    responseItems: MutableList<ApduResponsesItem>?,
                    completed: Boolean
                ) {
                    if (!completed) {
                        logger.debug("NFC addMCM progress callback received, waiting for completion")
                        return
                    }
                    NfcScriptExecutionTracker.onNfcScriptFinished()
                    hideNfcSheet()
                    val (tag85, tag86) = extractTag85And86FromHexResponses(nfcApduResponses)
                    handleAddMCMSuccess(
                        response,
                        capturedCardDetail,
                        response.tlvA6,
                        tag85,
                        tag86,
                        sdScript = null
                    )
                }

                override fun onError(error: String?) {
                    NfcScriptExecutionTracker.onNfcScriptFinished()
                    logger.debug("NFC executeScript onError: $error")
                    handleAddMCMFailure(response)
                }
            })
    }

    /**
     * Processes the result of add-card MCM script execution on the UI thread.
     *
     * On success, extracts tag 85/86 and continues with eligibility check; on failure,
     * shows the add-MCM error dialog.
     *
     * @param executionResult The [ScriptExecutionResult] from [ScriptHandler.executeScript].
     * @param response The [PrepSeResponseBody] from the prepare-SE API.
     * @param capturedCardDetail The [CapturedCardDetail] being provisioned.
     */
    private fun onAddCardScriptExecutionComplete(
        executionResult: ScriptExecutionResult,
        response: PrepSeResponseBody,
        capturedCardDetail: CapturedCardDetail
    ) {
        if (!isAdded) return
        logger.debug(
            "executeScript: Success=${executionResult.success}, " +
                "BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}"
        )
        isBleFlowInProgress = false
        if (executionResult.success) {
            val (tag85, tag86) = Utils.generateTag85And86(executionResult)
            handleAddMCMSuccess(
                response,
                capturedCardDetail,
                response.tlvA6,
                tag85,
                tag86,
                sdScript = executionResult.sdScript
            )
        } else {
            handleAddMCMFailure(response)
        }
    }

    /**
     * Executes the DSEMS installation script and handles the result of execution.
     *
     * Waits for secure-element scripts to finish, runs the install script over BLE, and
     * delegates UI-thread handling to [onAddCardScriptExecutionComplete] or [handleAddMCMException].
     *
     * @param scriptHandler The [ScriptHandler] responsible for executing the installation script.
     * @param jsonString The byte array representing the installation script data.
     * @param response The [PrepSeResponseBody] object containing API response data.
     * @param capturedCardDetail The [CapturedCardDetail] being processed.
     */
    private fun handleSuccessResponse(
        scriptHandler: ScriptHandler,
        jsonString: ByteArray?,
        response: PrepSeResponseBody,
        capturedCardDetail: CapturedCardDetail
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            SecureElementScriptCoordinator.awaitIdle()
            if (!isAdded || activity.isFinishing) return@launch
            activity.showLoading(true, getString(R.string.text_please_wait))
            scriptHandler.executeScript(jsonString).thenAccept { executionResult ->
                activity.runOnUiThread {
                    onAddCardScriptExecutionComplete(executionResult, response, capturedCardDetail)
                }
            }.exceptionally { throwable ->
                activity.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    isBleFlowInProgress = false
                    handleAddMCMException(throwable, response)
                }
                null
            }
        }
    }

    /**
     * Parses APDU response hex strings and extracts TLV tags 85 and 86 when present.
     *
     * @param responses Raw APDU response hex strings from script execution.
     * @return Pair of tag-85 and tag-86 TLV hex values, either may be null.
     */
    private fun extractTag85And86FromHexResponses(responses: List<String>): Pair<String?, String?> {
        var tag85: String? = null
        var tag86: String? = null

        responses.forEach { rawResponse ->
            val cleanHex = rawResponse
                .replace(" ", "")
                .replace("\n", "")
                .uppercase()

            var index = 0
            while (index < cleanHex.length - 4) {
                val tag = cleanHex.substring(index, index + 2)
                val lengthHex = cleanHex.substring(index + 2, index + 4)
                val length = lengthHex.toIntOrNull(16)?.times(2) ?: break
                val tlvEnd = index + 4 + length

                if (tlvEnd > cleanHex.length) {
                    break
                }

                val fullTlv = cleanHex.substring(index, tlvEnd)
                when (tag) {
                    TAG_85 -> tag85 = fullTlv
                    TAG_86 -> tag86 = fullTlv
                }

                index = tlvEnd
            }
        }

        return Pair(tag85, tag86)
    }

    /**
     * Handles an MCM (Mobile Card Management) script execution failure by showing an error dialog.
     *
     * @param response The [PrepSeResponseBody] representing the failed response data.
     */
    private fun handleAddMCMFailure(
        response: PrepSeResponseBody,
        throwable: Throwable? = null
    ) {
        clearInstallScriptPreference()
        logger.debug("addMCM: Failed, showing error dialog, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
        activity.showLoading(false, "")
        val digitizationRef = response.digitizationReferenceNumber.toString()
        val pnoType = response.pnoType.toString()
        if (!isNFC()) {
            handleAddMCMFailureBle(response, throwable, digitizationRef, pnoType)
            return
        }
        showAddMCMFailureDialog(digitizationRef, pnoType)
    }

    /**
     * Shows the add-MCM failure dialog and resets add-card UI state when the flow is active.
     *
     * @param digitizationRef Digitization reference number used for cleanup on confirm.
     * @param pnoType Payment network operator type (MDES or VTS).
     */
    private fun showAddMCMFailureDialog(digitizationRef: String, pnoType: String) {
        PayHostFssSync.onAddCardFailed()
        if (isAddCardFlowActive) {
            resetAddCardFlowState()
        }
        deleteDialog(getString(R.string.error_adding_mcm), digitizationRef, pnoType)
    }

    /**
     * Handles BLE-specific install-script (add-MCM) failures with reconnect and cleanup paths.
     *
     * Transport errors prompt a connection-lost dialog; APDU status-word errors show the failure
     * dialog immediately; transient disconnects offer a BLE reconnect prompt before retrying via
     * [retryAddMCMScriptAfterBleReconnect].
     *
     * @param response The [PrepSeResponseBody] from the prepare-SE API.
     * @param throwable The execution error, if any.
     * @param digitizationRef Digitization reference number used for cleanup on confirm.
     * @param pnoType Payment network operator type (MDES or VTS).
     */
    private fun handleAddMCMFailureBle(
        response: PrepSeResponseBody,
        throwable: Throwable?,
        digitizationRef: String,
        pnoType: String,
    ) {
        if (ScriptHandler.isBleTransportError(throwable)) {
            showProvisionScriptBleConnectionLostDialog {
                finishCardListAddCardAbortCleanup()
                abortAddCardCleanupWithoutBle(DELETED_STATUS, digitizationRef, pnoType)
            }
            return
        }
        val seId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)
        val bleStillConnected = !seId.isNullOrBlank() &&
            BluetoothStateManager.isDeviceConnected(seId, activity)
        if (bleStillConnected ||
            throwable == null ||
            ScriptHandler.isScriptApduStatusWordError(throwable)
        ) {
            showAddMCMFailureDialog(digitizationRef, pnoType)
            return
        }
        resetAddCardFlowState()
        showProvisionBleReconnectPrompt(
            onConnected = { retryAddMCMScriptAfterBleReconnect(response, digitizationRef, pnoType) },
            onDeclined = {
                finishCardListAddCardAbortCleanup()
                abortAddCardCleanupWithoutBle(DELETED_STATUS, digitizationRef, pnoType)
            }
        )
    }

    /**
     * Retries the MDES install script over BLE after the user reconnects the wearable.
     *
     * Re-extracts script bytes from [response] and delegates to [handleSuccessResponse]. Aborts the
     * add-card flow when card details or script data are unavailable.
     *
     * @param response The [PrepSeResponseBody] from the original prepare-SE call.
     * @param digitizationRef Digitization reference number used for cleanup on abort.
     * @param pnoType Payment network operator type (MDES or VTS).
     */
    private fun retryAddMCMScriptAfterBleReconnect(
        response: PrepSeResponseBody,
        digitizationRef: String,
        pnoType: String,
    ) {
        val detail = cardDetails
        if (detail == null) {
            finishCardListAddCardAbortCleanup()
            abortAddCardCleanupWithoutBle(DELETED_STATUS, digitizationRef, pnoType)
            return
        }
        activity.showLoading(true, getString(R.string.text_please_wait))
        val jsonString = extractJsonBytes(response)
        if (jsonString == null) {
            finishCardListAddCardAbortCleanup()
            abortAddCardCleanupWithoutBle(DELETED_STATUS, digitizationRef, pnoType)
            return
        }
        handleSuccessResponse(createScriptHandler(), jsonString, response, detail)
    }

    /**
     * Continues to eligibility check directly after prepSE when no install script is executed (Visa/VTS).
     */
    private fun proceedToCheckEligibilityFromPrepSe(
        response: PrepSeResponseBody,
        capturedCardDetail: CapturedCardDetail,
    ) {
        lifecycleScope.launch {
            if (activity.isFinishing) return@launch

            val sdkResult = WalletRepository.checkEligibility(
                context = activity,
                paymentAppInstanceId = paymentAppInstanceId ?: "",
                digitizationReferenceNumber = response.digitizationReferenceNumber.toString(),
                tlvA6 = response.tlvA6,
                tag85 = null,
                tag86 = null,
                casdPkCertificate = null,
            )
            if (sdkResult.isSuccess) {
                handleCheckEligibilitySuccess(
                    sdkResult.response,
                    capturedCardDetail,
                    response.digitizationReferenceNumber.toString(),
                    sdScript = null,
                    pnoType = response.pnoType.toString()
                )
            } else {
                activity.showLoading(false, "")
                finishAbortedAddCardFlowIfNeeded()
                logger.debug("checkEligibility: Error after Visa prepSE, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
                handleSessionOrError(sdkResult.errorMessage)
            }
        }
    }

    /**
     * Handles a successful MCM (Mobile Card Management) script execution and initiates an eligibility check.
     *
     * @param response The [PrepSeResponseBody] containing digitization and card details.
     * @param capturedCardDetail The [CapturedCardDetail] being processed.
     * @param tlvA6 TLV A6 value from the INSTALL script execution.
     * @param tag85 tag85 value from the INSTALL script execution.
     * @param tag86 tag86 value from the INSTALL script execution.
     * @param sdScript Combined sdScript hex from INSTALL script sequence 8, or null when not available.
     */
    private fun handleAddMCMSuccess(
        response: PrepSeResponseBody,
        capturedCardDetail: CapturedCardDetail,
        tlvA6: String?,
        tag85: String?,
        tag86: String?,
        sdScript: String? = null,
    ) {
        clearInstallScriptPreference()

        lifecycleScope.launch {
            if (activity.isFinishing) return@launch

            val pnoType = response.pnoType.toString()
            val casdPkCertificate = if (pnoType == PNO_MDES && !isNFC()) {
                CasdCertificateHelper.fetchCasdCertificate(activity.applicationContext, PNO_MDES)
            } else {
                null
            }
            if (pnoType == PNO_MDES && !isNFC() && casdPkCertificate.isNullOrBlank()) {
                activity.showLoading(false, "")
                handleAddMCMFailure(response)
                return@launch
            }

            val sdkResult = WalletRepository.checkEligibility(
                context = activity,
                paymentAppInstanceId = paymentAppInstanceId ?: "",
                digitizationReferenceNumber = response.digitizationReferenceNumber.toString(),
                tlvA6 = tlvA6,
                tag85 = tag85,
                tag86 = tag86,
                casdPkCertificate = casdPkCertificate,
            )
            if (sdkResult.isSuccess) {
                handleCheckEligibilitySuccess(
                    sdkResult.response,
                    capturedCardDetail,
                    response.digitizationReferenceNumber.toString(),
                    sdScript,
                    pnoType
                )
            } else {
                activity.showLoading(false, "")
                finishAbortedAddCardFlowIfNeeded()
                logger.debug("checkEligibility: Error, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
                handleSessionOrError(sdkResult.errorMessage)
            }
        }
    }

    /**
     * Handles success response from Check Eligibility API.
     * If the response is successful, proceed with fetching assets.
     * Otherwise, show error dialog or log appropriate error.
     *
     * @param response Check eligibility response from the wallet SDK.
     * @param capturedCardDetail Card details captured during add-card flow.
     * @param digitizationReferenceNumberDelete Digitization reference used for cleanup on failure.
     * @param sdScript Combined sdScript hex from INSTALL script sequence 8, passed through to digitization.
     */
    private fun handleCheckEligibilitySuccess(
        response: CheckEligibilityResponseBody?,
        capturedCardDetail: CapturedCardDetail,
        digitizationReferenceNumberDelete: String,
        sdScript: String? = null,
        pnoType: String? = null,
    ) {
        if (response != null) {
            suppressFcmCardRefresh = true
            logger.debug("checkEligibility: Success, status=${response.statusMessage}, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")

            if (response.statusMessage.equals(SUCCESS_MESSAGE, ignoreCase = true)) {
                digitizationReferenceNumber = response.digitizationReferenceNumber.toString()
                proceedToPrepSeScripts(
                    response,
                    capturedCardDetail,
                    digitizationReferenceNumberDelete,
                    sdScript,
                    pnoType ?: response.pnoType.toString()
                )
            } else {
                suppressFcmCardRefresh = false
                handleEligibilityFailure(
                    response, digitizationReferenceNumberDelete, response.pnoType.toString()
                )
            }
        } else {
            if (!finishAbortedAddCardFlowIfNeeded()) {
                activity.showLoading(false, "")
            }
            logger.debug("checkEligibility: Null response, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
        }
    }

    /**
     * Handles an exception thrown during MCM script execution.
     *
     * @param throwable The exception encountered during script execution.
     * @param response The [PrepSeResponseBody] associated with the operation.
     */
    private fun handleAddMCMException(throwable: Throwable, response: PrepSeResponseBody) {
        logger.debug("addMCM: Exception=${throwable.message}, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
        handleAddMCMFailure(response, throwable)
    }

    /**
     * Displays a non-cancelable confirmation dialog for deleting a card.
     * Once the user confirms, triggers the card deletion process.
     *
     * @param message The message to show in the dialog.
     * @param digitizationReferenceNumberDelete The reference number of the card to delete.
     */
    fun deleteDialog(
        message: String, digitizationReferenceNumberDelete: String, pnoType: String
    ) {
        if (!::activity.isInitialized || activity.isFinishing) return
        activity.runOnUiThread {
            if (activity.isFinishing) return@runOnUiThread
            val currentActivity = activity
            if (!currentActivity.isFinishing) {
                val dialogViewBinding = DialogCommonMessageBinding.inflate(currentActivity.layoutInflater)
                val alertDialog = Dialog(currentActivity).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setContentView(dialogViewBinding.root)
                    setCancelable(false)
                }

                dialogViewBinding.txtTitle.text = getString(R.string.text_secora_wallet)
                dialogViewBinding.txtMessage.text = message
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
                    isNotificationPending = false
                    deleteCard(
                        DELETED_STATUS, digitizationReferenceNumberDelete, pnoType
                    )
                    alertDialog.dismiss()
                }

                alertDialog.showSecure()
            }
        }
    }

    /**
     * Deletes a payment card from the wearable device and updates its status on the backend.
     *
     * @param status The new status to set for the card (e.g., "DELETED").
     * @param tokenRefNumber The digitization reference number of the card.
     * @param pnoType The payment network type associated with the card.
     */
    fun deleteCard(
        status: String, tokenRefNumber: String, pnoType: String
    ) {
        alertDialog?.dismiss()
        activity.showLoading(true, getString(R.string.text_please_wait))

        fetchSequenceNumberForProvisionAbort(
            onRetrieved = { currentSequenceCounter ->
                logger.debug("currentSequenceCounter : $currentSequenceCounter")
                handleUpdateCardStatusFLow(
                    status, tokenRefNumber, pnoType, currentSequenceCounter, skipSeScriptsWhenDisconnected = false
                )
            },
            onProceedWithoutBle = {
                abortAddCardCleanupWithoutBle(status, tokenRefNumber, pnoType)
            }
        )
    }

    /**
     * Backend-only cleanup after a failed add-card attempt when BLE cannot be restored.
     */
    private fun abortAddCardCleanupWithoutBle(
        status: String,
        tokenRefNumber: String,
        pnoType: String
    ) {
        if (!::activity.isInitialized || activity.isFinishing) return
        activity.showLoading(true, getString(R.string.text_please_wait))
        lifecycleScope.launch {
            val counter = SequenceCounterHelper.resolveSequenceCounter(activity)
            handleUpdateCardStatusFLow(
                status, tokenRefNumber, pnoType, counter, skipSeScriptsWhenDisconnected = true
            )
        }
    }

    /**
     * Releases add-card flow state and hides loading after a failed provisioning attempt is aborted.
     */
    private fun finishCardListAddCardAbortCleanup() {
        releasePendingDeleteExecutionGate()
        resetAddCardFlowState()
        if (::activity.isInitialized && !activity.isFinishing) {
            activity.showLoading(false, "")
        }
    }

    /**
     * Updates card status on the backend and optionally runs returned delete scripts on the SE.
     *
     * @param status New card status (for example DELETED).
     * @param tokenRefNumber Digitization reference number of the card.
     * @param pnoType Payment network operator type.
     * @param currentSequenceCounter Sequence counter from the wearable.
     * @param skipSeScriptsWhenDisconnected When true, skips SE delete scripts if BLE is unavailable.
     */
    private fun handleUpdateCardStatusFLow(
        status: String,
        tokenRefNumber: String,
        pnoType: String,
        currentSequenceCounter: String,
        skipSeScriptsWhenDisconnected: Boolean = false
    ) {

        // Run database operation and API call on background thread to avoid UI lag.
        lifecycleScope.launch(AppDispatchers.IO) {
            if (activity.isFinishing) return@launch

            val selectedCard = StorageRepository.getLocalCardListForStatusApi(
                activity,
                tokenRefNumber,
                paymentAppInstanceId ?: ""
            )
            val deviceDetails = DeviceDetails(
                connected = isNFC() || BluetoothStateManager.isConnected,
                currentSequenceCounter = currentSequenceCounter
            )

            val sdkResult = WalletRepository.updateCardStatus(
                context = activity,
                cardStatus = status,
                cardList = selectedCard,
                paymentAppInstanceId = paymentAppInstanceId ?: "",
                digitizationReferenceNumber = tokenRefNumber,
                pnoType = pnoType,
                deviceDetails = deviceDetails
            )
            if (sdkResult.isSuccess) {
                handleUpdateCardStatusSuccess(
                    sdkResult.response, tokenRefNumber, skipSeScriptsWhenDisconnected
                )
            } else {
                activity.runOnUiThread {
                    if (activity.isFinishing) return@runOnUiThread
                    if (skipSeScriptsWhenDisconnected) {
                        finishCardListAddCardAbortCleanup()
                    } else if (!finishAbortedAddCardFlowIfNeeded()) {
                        activity.showLoading(false, "")
                    }
                    handleSessionOrError(sdkResult.errorMessage) {
                        statusDialog(activity, sdkResult.errorMessage)
                    }
                }
            }
        }
    }

    /**
     * Handles a successful response from the UpdateCardStatus API.
     *
     * If the status indicates success, triggers execution of the delete script; otherwise, handles token expiration or errors.
     *
     * @param response The API response object[UpdateCardStatusResponse].
     * @param tokenRefNumber The digitization reference number of the updated card.
     */
    private fun handleUpdateCardStatusSuccess(
        response: UpdateCardStatusResponse?,
        tokenRefNumber: String,
        skipSeScriptsWhenDisconnected: Boolean = false
    ) {
        if (response == null || response.statusMessage.isNullOrEmpty()) {
            if (skipSeScriptsWhenDisconnected) {
                finishCardListAddCardAbortCleanup()
            } else if (!finishAbortedAddCardFlowIfNeeded()) {
                activity.showLoading(false, "")
            }
            return
        }

        when (response.statusMessage) {
            CommonResponse.SUCCESS.response -> {
                if (skipSeScriptsWhenDisconnected || shouldSkipSeDeleteScriptAndAcknowledgement()) {
                    finishCardListAddCardAbortCleanup()
                    return
                }
                if (!isNFC()) {
                    executeDeleteScriptWithRetry(response.deleteScriptList, tokenRefNumber, 0)
                } else {
                    executeDeleteScriptWithRetryNFC(
                        response.deleteScriptList,
                        tokenRefNumber,
                        scriptIndex = 0,
                        attemptIndex = 0
                    )
                }
            }

            else -> {
                if (skipSeScriptsWhenDisconnected) {
                    finishCardListAddCardAbortCleanup()
                } else if (!finishAbortedAddCardFlowIfNeeded()) {
                    activity.showLoading(false, "")
                }
            }
        }
    }

    /**
     * Executes delete scripts sequentially using the provided index.
     *
     * @param deleteScriptsList The delete scripts to run in order.
     * @param tokenRefNumber The token reference number (kept for flow compatibility).
     * @param retryCount Current script index to execute.
     */
    private fun executeDeleteScriptWithRetry(
        deleteScriptsList: List<DeleteScriptBase>?,
        tokenRefNumber: String,
        retryCount: Int,
        runInBackground: Boolean = false
    ) {
        if (retryCount == 0 && !runInBackground) {
            val activeSeId = resolveSeIdForPendingDelete()
            if (activeSeId.isNotEmpty() &&
                !PendingDeleteScriptExecutionGate.tryBegin(activeSeId)
            ) {
                logger.debug(
                    "executeDeleteScriptWithRetry skipped; gate busy for seId=$activeSeId"
                )
                onSequentialDeleteScriptsUnavailable(runInBackground)
                return
            }
        }

        val scripts = resolveSequentialDeleteScripts(deleteScriptsList, retryCount, runInBackground) ?: return

        if (!runInBackground) {
            showAddCardFlowLoadingIfProvisioning()
        }

        val currentScript = scripts[retryCount]
        val jsonBytes = extractAndPersistSequentialDeleteScript(currentScript.scriptData)
        if (jsonBytes == null || jsonBytes.isEmpty()) {
            onSequentialDeleteScriptInvalidPayload(runInBackground)
            return
        }

        dispatchSequentialDeleteScript(
            jsonBytes, currentScript, scripts, tokenRefNumber, retryCount, runInBackground
        )
    }

    /**
     * Validates the delete script list and index; handles terminal states when invalid.
     *
     * @return The script list to execute, or null when the flow should stop.
     */
    private fun resolveSequentialDeleteScripts(
        deleteScriptsList: List<DeleteScriptBase>?,
        retryCount: Int,
        runInBackground: Boolean = false
    ): List<DeleteScriptBase>? {
        if (deleteScriptsList.isNullOrEmpty()) {
            onSequentialDeleteScriptsUnavailable(runInBackground)
            return null
        }
        if (retryCount >= deleteScriptsList.size) {
            onSequentialDeleteScriptsCompleted(runInBackground)
            return null
        }
        return deleteScriptsList
    }

    /**
     * Shows the add-card loading overlay only while add-card provisioning owns the flow.
     */
    private fun showAddCardFlowLoadingIfProvisioning() {
        if (isAddCardProvisioningFlow()) {
            showAddCardFlowLoading()
        }
    }

    /**
     * Decodes a delete-script payload and stores it in preferences for execution.
     *
     * @param scriptData Base64-encoded delete script from the backend.
     * @return Decoded script bytes, or null when decoding fails.
     */
    private fun extractAndPersistSequentialDeleteScript(scriptData: String?): ByteArray? {
        val jsonBytes = extractJsonBytesRetry(scriptData)
        StorageRepository.saveString(PreferenceKey.INSTALL_SCRIPT, jsonBytes.contentToString())
        return jsonBytes
    }

    /**
     * Creates a [ScriptHandler] for sequential delete-script execution.
     *
     * When [suppressUi] is true, toast and loading callbacks are no-ops so background
     * delete flows do not disturb the active UI.
     *
     * @param suppressUi When true, suppresses toast and loading callbacks (background deletes).
     * @return Configured [ScriptHandler] for chained delete-script execution.
     */
    private fun createSequentialDeleteScriptHandler(suppressUi: Boolean = false): ScriptHandler {
        return ScriptHandler(
            activity, object : ScriptHandler.Callbacks {
                override fun showLoading(show: Boolean, msg: String) {
                    if (!suppressUi && isAddCardProvisioningFlow() && show) {
                        showAddCardFlowLoading()
                    }
                }

                override fun showToast(message: String) {
                    if (!suppressUi) {
                        Toast.makeText(activity.applicationContext, message, Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun updateLogs(message: String) {
                    logger.debug("ScriptHandler$message")
                }
            })
    }

    /**
     * Executes one delete script and routes the result to the sequential delete handlers.
     *
     * @param jsonBytes Decoded delete-script payload.
     * @param currentScript Script metadata for acknowledgement.
     * @param deleteScriptsList Full ordered list of delete scripts.
     * @param tokenRefNumber Digitization reference number of the card being deleted.
     * @param retryCount Index of the script currently executing.
     * @param runInBackground When true, runs without blocking add-card UI.
     */
    private fun dispatchSequentialDeleteScript(
        jsonBytes: ByteArray,
        currentScript: DeleteScriptBase,
        deleteScriptsList: List<DeleteScriptBase>,
        tokenRefNumber: String,
        retryCount: Int,
        runInBackground: Boolean = false
    ) {
        val scriptHandler = createSequentialDeleteScriptHandler(suppressUi = runInBackground)
        scriptHandler.deleteScript(jsonBytes).thenAccept { success ->
            logger.debug(
                "delete card: Success=$success (script ${retryCount + 1}/${deleteScriptsList.size})"
            )
            handleSequentialDeleteScriptResult(
                success, currentScript, deleteScriptsList, tokenRefNumber, retryCount, runInBackground
            )
        }.exceptionally { throwable ->
            onSequentialDeleteScriptExecutionError(
                throwable, retryCount, deleteScriptsList.size, runInBackground, tokenRefNumber
            )
            null
        }
    }

    /**
     * Handles exceptions thrown while executing a sequential delete script.
     *
     * @param throwable The execution exception.
     * @param retryCount Index of the script that failed.
     * @param totalScripts Total number of scripts in the sequence.
     * @param runInBackground When true, logs only and does not interrupt add-card UI.
     * @param tokenRefNumber Digitization reference used for add-card abort cleanup.
     */
    private fun onSequentialDeleteScriptExecutionError(
        throwable: Throwable,
        retryCount: Int,
        totalScripts: Int,
        runInBackground: Boolean = false,
        tokenRefNumber: String = ""
    ) {
        logger.debug("delete card: Exception=${throwable.message} (script ${retryCount + 1}/$totalScripts)")
        scriptDSEM = false
        clearInstallScriptPreference()
        logger.debug("delete card: Failure while executing scripts sequentially")
        if (!runInBackground) {
            releasePendingDeleteExecutionGate()
        }
        if (!::activity.isInitialized || activity.isFinishing) return
        activity.runOnUiThread {
            if (runInBackground) {
                logger.debug("Background pending delete script failed; add-card flow already continued")
                return@runOnUiThread
            }
            if (!isNFC() && (isAddCardFlowActive || isAddCardProvisioningFlow())) {
                promptAddCardBleReconnectAfterScriptFailure(tokenRefNumber)
                return@runOnUiThread
            }
            statusDialog(activity, Constants.UNABLE_TO_DELETE_MESSAGE)
            activity.showLoading(false, "")
            if (isDeleteFlow) {
                isDeleteFlow = false
                return@runOnUiThread
            }
            if (isNotificationPending) {
                navigateAddToCard()
            }
        }
    }

    /**
     * Handles the case where no delete scripts are available to execute.
     *
     * @param runInBackground When true, skips UI changes because add-card already continued.
     */
    private fun onSequentialDeleteScriptsUnavailable(runInBackground: Boolean = false) {
        isDeleteFlow = false
        logger.debug("executeDeleteScriptWithRetry: deleteScriptsList is null or empty")
        if (!runInBackground) {
            releasePendingDeleteExecutionGate()
        }
        if (runInBackground) return
        if (finishAbortedAddCardFlowIfNeeded()) return
        if (isAddCardProvisioningFlow()) {
            dismissAddCardFlowLoading()
        } else {
            activity.showLoading(false, "")
        }
    }

    /**
     * Handles completion of all sequential delete scripts and resumes add-card flow if needed.
     *
     * @param runInBackground When true, skips UI changes because add-card already continued.
     */
    private fun onSequentialDeleteScriptsCompleted(runInBackground: Boolean = false) {
        logger.debug("executeDeleteScriptWithRetry: all scripts executed")
        if (!runInBackground) {
            releasePendingDeleteExecutionGate()
        }
        if (runInBackground) return
        if (isAddCardProvisioningFlow()) {
            showAddCardFlowLoading()
        } else {
            activity.showLoading(false, "")
        }
        if (isDeleteFlow) {
            isDeleteFlow = false
            if (!isAddCardProvisioningFlow()) {
                activity.showLoading(false, "")
            }
            finishAbortedAddCardFlowIfNeeded()
            return
        }

        if (isNotificationPending) {
            navigateAddToCard()
            return
        }

        finishAbortedAddCardFlowIfNeeded()
    }

    /**
     * Handles an invalid or empty delete-script payload at the current sequence index.
     *
     * @param runInBackground When true, skips UI changes because add-card already continued.
     */
    private fun onSequentialDeleteScriptInvalidPayload(runInBackground: Boolean = false) {
        isDeleteFlow = false
        logger.debug("executeDeleteScriptWithRetry: jsonBytes is null or empty, skipping execution")
        if (!runInBackground) {
            releasePendingDeleteExecutionGate()
        }
        if (runInBackground) return
        if (::activity.isInitialized && !activity.isFinishing) {
            activity.runOnUiThread {
                activity.showLoading(false, "")
            }
        }
    }

    /**
     * Dispatches a completed delete-script execution to success or failure handlers.
     *
     * @param success Whether script execution succeeded on the secure element.
     * @param currentScript Script metadata for acknowledgement.
     * @param deleteScriptsList Full ordered list of delete scripts.
     * @param tokenRefNumber Digitization reference number of the card.
     * @param retryCount Index of the script that just finished.
     * @param runInBackground When true, runs without blocking add-card UI.
     */
    private fun handleSequentialDeleteScriptResult(
        success: Boolean,
        currentScript: DeleteScriptBase,
        deleteScriptsList: List<DeleteScriptBase>,
        tokenRefNumber: String,
        retryCount: Int,
        runInBackground: Boolean = false
    ) {
        if (!::activity.isInitialized || activity.isFinishing) return
        if (!runInBackground && isAddCardProvisioningFlow()) {
            showAddCardFlowLoading()
        }
        // Proceed as soon as BLE reports script success; a fixed 9s wait was only blocking UX.
        activity.runOnUiThread {
            if (success) {
                onSequentialDeleteScriptSuccess(
                    currentScript,
                    deleteScriptsList,
                    tokenRefNumber,
                    retryCount,
                    runInBackground
                )
            } else {
                onSequentialDeleteScriptFailure(
                    retryCount, deleteScriptsList.size, runInBackground, tokenRefNumber
                )
            }
        }
    }

    /**
     * Acknowledges a successful delete script and advances to the next script in the sequence.
     *
     * @param currentScript Script metadata that completed successfully.
     * @param deleteScriptsList Full ordered list of delete scripts.
     * @param tokenRefNumber Digitization reference number of the card.
     * @param retryCount Index of the script that just succeeded.
     * @param runInBackground When true, runs without blocking add-card UI.
     */
    private fun onSequentialDeleteScriptSuccess(
        currentScript: DeleteScriptBase,
        deleteScriptsList: List<DeleteScriptBase>,
        tokenRefNumber: String,
        retryCount: Int,
        runInBackground: Boolean = false
    ) {
        scriptDSEM = true
        clearInstallScriptPreference()
        if (!shouldSkipSeDeleteScriptAndAcknowledgement()) {
            acknowledgeDeleteScriptSuccess(
                listOf(currentScript),
                activity,
                seId ?: "",
                tokenRefNumber
            )
        }
        executeDeleteScriptWithRetry(
            deleteScriptsList, tokenRefNumber, retryCount + 1, runInBackground
        )
    }

    /**
     * Handles a failed delete-script step and prompts BLE reconnect during add-card abort flows.
     *
     * @param retryCount Index of the script that failed.
     * @param totalScripts Total number of scripts in the sequence.
     * @param runInBackground When true, logs only and does not interrupt add-card UI.
     * @param tokenRefNumber Digitization reference used for add-card abort cleanup.
     */
    private fun onSequentialDeleteScriptFailure(
        retryCount: Int,
        totalScripts: Int,
        runInBackground: Boolean = false,
        tokenRefNumber: String = ""
    ) {
        scriptDSEM = false
        clearInstallScriptPreference()
        logger.debug("delete card: Failed (script ${retryCount + 1}/$totalScripts)")
        if (runInBackground) {
            logger.debug("Background pending delete script step failed; add-card flow already continued")
            return
        }
        releasePendingDeleteExecutionGate()
        if (!isNFC() && (isAddCardFlowActive || isAddCardProvisioningFlow())) {
            promptAddCardBleReconnectAfterScriptFailure(tokenRefNumber)
            return
        }
        statusDialog(activity, Constants.UNABLE_TO_DELETE_MESSAGE)
        if (isDeleteFlow) {
            isDeleteFlow = false
            finishAbortedAddCardFlowIfNeeded()
            return
        }
        if (isNotificationPending) {
            navigateAddToCard()
            return
        }
        finishAbortedAddCardFlowIfNeeded()
    }

    /**
     * Prompts BLE reconnect after a delete-script failure during add-card provisioning abort.
     *
     * @param tokenRefNumber Digitization reference used for backend cleanup on decline.
     */
    private fun promptAddCardBleReconnectAfterScriptFailure(tokenRefNumber: String) {
        val digitizationRef = tokenRefNumber.ifBlank { digitizationReferenceNumber }
        val pnoType = cardDetails?.pnoType?.takeIf { it.isNotBlank() } ?: PNO_MDES
        showProvisionBleReconnectPrompt(
            onConnected = {
                if (digitizationRef.isNotBlank()) {
                    deleteCard(DELETED_STATUS, digitizationRef, pnoType)
                } else {
                    finishCardListAddCardAbortCleanup()
                }
            },
            onDeclined = {
                finishCardListAddCardAbortCleanup()
                if (digitizationRef.isNotBlank()) {
                    abortAddCardCleanupWithoutBle(DELETED_STATUS, digitizationRef, pnoType)
                }
            }
        )
    }

    /**
     * Extracts and decodes the JSON delete script bytes from an [UpdateCardStatusResponse].
     *
     * This method behaves similarly to [extractJsonBytes], but is used during the card deletion process.
     * It decodes the delete script returned from the backend, performing double Base64 decoding if needed.
     * @return A UTF-8 encoded [ByteArray] representing the decoded delete script JSON,
     *         or `null` if decoding fails or no script data is found.
     */
    private fun extractJsonBytesRetry(scriptData: String?): ByteArray? {
        return try {
            if (scriptData.isNullOrEmpty()) {
                logger.debug(SCRIPT_DATA_NULL_OR_EMPTY)
                return null
            }

            ScriptDataParser.decodeToJsonBytes(scriptData)?.also {
                val decodedString = String(it, Charsets.UTF_8)
                logger.debug("Decoded scriptData -> $decodedString")
            }
        } catch (e: Exception) {
            logger.noStackTraceLog("ExtractJsonBytesRetry ", e)
            null
        }
    }

    /**
     * Acknowledges the successful execution of a delete script
     * @param activity The MainActivity instance
     * @param seId The Secure Element ID
     */
    private fun acknowledgeDeleteScriptSuccess(
        deleteScriptsList: List<DeleteScriptBase>?,
        activity: MainActivity,
        seId: String,
        tokenRefNumberFallback: String = ""
    ) {
        try {
            if (!deleteScriptsList.isNullOrEmpty()) {
                val script = deleteScriptsList[0]
                script.scriptId?.let { scriptId ->
                    val digitizeRef = script.digitizationReferenceNumber
                        ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                        ?: tokenRefNumberFallback
                    acknowledgePendingTask(activity, seId, scriptId, digitizeRef)
                }
            }
        } catch (e: Exception) {
            logger.debug("Error acknowledging delete script success: ${e.message}")
        }
    }

    /**
     * Handles failure case of eligibility check.
     * Displays error dialog and stops loading indicator.
     */
    private fun handleEligibilityFailure(
        response: CheckEligibilityResponseBody,
        digitizationReferenceNumberDelete: String,
        pnoType: String
    ) {
        suppressFcmCardRefresh = false
        logger.debug("CheckEligibility: Failed, showing error dialog, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
        activity.showLoading(false, "")
        deleteDialog(response.statusMessage.toString(), digitizationReferenceNumberDelete, pnoType)
    }

    /**
     * Calls se-preparations/scripts after eligibility and routes to script execution or asset fetch.
     */
    private fun proceedToPrepSeScripts(
        eligibilityResponse: CheckEligibilityResponseBody,
        capturedCardDetail: CapturedCardDetail,
        digitizationReferenceNumberDelete: String,
        sdScript: String? = null,
        pnoType: String,
    ) {
        lifecycleScope.launch {
            if (activity.isFinishing) return@launch
            WalletRepository.syncOemDetailsFromPreferences(activity)
            val currentSequenceCounter = SequenceCounterHelper.resolveSequenceCounter(activity)
            val sdkResult = WalletRepository.fetchPrepSeScripts(
                context = activity,
                paymentAppInstanceId = paymentAppInstanceId ?: "",
                digitizationReferenceNumber = eligibilityResponse.digitizationReferenceNumber.toString(),
                currentSequenceCounter = currentSequenceCounter
            )
            if (sdkResult.isSuccess) {
                val scriptsResponse = sdkResult.response
                if (scriptsResponse != null) {
                    handleFetchPrepSeScriptsResponse(
                        scriptsResponse,
                        eligibilityResponse,
                        capturedCardDetail,
                        digitizationReferenceNumberDelete,
                        sdScript,
                        pnoType
                    )
                } else {
                    handlePrepSeScriptsApiError(getString(R.string.no_response_data_received))
                }
            } else {
                logger.debug("fetchPrepSeScripts: Error, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
                activity.showLoading(false, "")
                handleSessionOrError(sdkResult.errorMessage)
            }
        }
    }

    /**
     * Routes se-preparations/scripts API responses using the same rules as [handlePrepareSecureElementSuccess]
     * and [handlePrepareSecureElementResponse] (get-pending on install-script fetch, suspend wearable, etc.).
     */
    private fun handleFetchPrepSeScriptsResponse(
        scriptsResponse: PrepSeResponseBody,
        eligibilityResponse: CheckEligibilityResponseBody,
        capturedCardDetail: CapturedCardDetail,
        digitizationReferenceNumberDelete: String,
        sdScript: String? = null,
        pnoType: String,
    ) {
        if (scriptsResponse.statusCode.equals(ONBOARDING_FETCH_INSTALL_SCRIPT_0002)) {
            getPending = true
            getPendingTaskFromPrepSeFailure = true
            prepSeFailureStatusMessage = scriptsResponse.statusMessage.toString()
            isNotificationPending = false
            getPendingTask(activity, seId ?: "", "")
            return
        }

        if (scriptsResponse.statusMessage.isNullOrEmpty()) {
            handlePrepSeScriptsApiError(getString(R.string.no_response_data_received))
            return
        }

        when (scriptsResponse.statusCode) {
            CommonResponse.TOKEN_CONNECTOR_SERVICE_WEARABLE_0001.response -> {
                activity.showLoading(false, "")
                if (isAddCardFlowActive) {
                    resetAddCardFlowState()
                }
                handleSuspendNotification(
                    AppEvent(
                        "", mapOf(
                            BundleKey.DEVICE_NAME to StorageRepository.readString(PreferenceKey.DEVICE_NAME)
                        )
                    )
                )
                return
            }
        }

        when (scriptsResponse.statusMessage) {
            CommonResponse.SUCCESS.response -> {
                handlePrepSeScriptsSuccess(
                    scriptsResponse,
                    eligibilityResponse,
                    capturedCardDetail,
                    digitizationReferenceNumberDelete,
                    sdScript,
                    pnoType
                )
            }

            CommonResponse.DSEMS_SCRIPT_NOT_FOUND.response -> {
                logger.debug(
                    "fetchPrepSeScripts: DSEMS_SCRIPT_NOT_FOUND, " +
                        "BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}"
                )
                handlePrepSeScriptsApiError(scriptsResponse.statusMessage!!)
            }

            else -> {
                logger.debug(
                    "fetchPrepSeScripts: Unknown status=${scriptsResponse.statusMessage}, " +
                        "BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}"
                )
                handlePrepSeScriptsApiError(scriptsResponse.statusMessage.toString())
            }
        }
    }

    /**
     * Shows a status dialog for se-preparations/scripts API failures (mirrors prepSE API error handling).
     */
    private fun handlePrepSeScriptsApiError(message: String) {
        suppressFcmCardRefresh = false
        showStatusAndStopLoading(message)
    }

    /**
     * Handles a successful se-preparations/scripts response.
     * Visa executes the returned script; Mastercard skips script execution.
     */
    private fun handlePrepSeScriptsSuccess(
        scriptsResponse: PrepSeResponseBody,
        eligibilityResponse: CheckEligibilityResponseBody,
        capturedCardDetail: CapturedCardDetail,
        digitizationReferenceNumberDelete: String,
        existingSdScript: String? = null,
        pnoType: String,
    ) {
        if (isVisaPno(pnoType)) {
            val jsonString = extractJsonBytes(scriptsResponse)
            if (jsonString == null) {
                handlePrepSeScriptsFailure(
                    getString(R.string.no_response_data_received),
                    digitizationReferenceNumberDelete,
                    pnoType
                )
                return
            }
            StorageRepository.saveString(PreferenceKey.INSTALL_SCRIPT, jsonString.contentToString())
            if (!isNFC()) {
                executePostEligibilityScript(
                    createScriptHandler(),
                    jsonString,
                    eligibilityResponse,
                    capturedCardDetail,
                    digitizationReferenceNumberDelete,
                    pnoType
                )
            } else {
                executePostEligibilityNfcScript(
                    jsonString,
                    eligibilityResponse,
                    capturedCardDetail,
                    digitizationReferenceNumberDelete,
                    pnoType
                )
            }
            return
        }
        fetchAssetForTermsAndConditions(eligibilityResponse, capturedCardDetail, existingSdScript)
    }

    /**
     * Executes the post-eligibility Visa install script over BLE.
     *
     * Waits for any in-flight secure-element scripts, runs the script via [ScriptHandler], and
     * delegates UI-thread completion to [onPostEligibilityScriptExecutionComplete] or
     * [handlePostEligibilityScriptException] on failure.
     *
     * @param scriptHandler [ScriptHandler] used to execute the script.
     * @param jsonString Decoded install-script JSON bytes.
     * @param eligibilityResponse Successful check-eligibility response driving the next step.
     * @param capturedCardDetail The [CapturedCardDetail] being provisioned.
     * @param digitizationReferenceNumberDelete Digitization reference used for cleanup on failure.
     * @param pnoType Payment network operator type (MDES or VTS).
     */
    private fun executePostEligibilityScript(
        scriptHandler: ScriptHandler,
        jsonString: ByteArray?,
        eligibilityResponse: CheckEligibilityResponseBody,
        capturedCardDetail: CapturedCardDetail,
        digitizationReferenceNumberDelete: String,
        pnoType: String,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            SecureElementScriptCoordinator.awaitIdle()
            if (!isAdded || activity.isFinishing) return@launch
            activity.showLoading(true, getString(R.string.text_please_wait))
            scriptHandler.executeScript(jsonString).thenAccept { executionResult ->
                activity.runOnUiThread {
                    onPostEligibilityScriptExecutionComplete(
                        executionResult,
                        eligibilityResponse,
                        capturedCardDetail,
                        digitizationReferenceNumberDelete,
                        pnoType
                    )
                }
            }.exceptionally { throwable ->
                activity.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    isBleFlowInProgress = false
                    handlePostEligibilityScriptException(
                        throwable,
                        eligibilityResponse,
                        capturedCardDetail,
                        jsonString,
                        digitizationReferenceNumberDelete,
                        pnoType
                    )
                }
                null
            }
        }
    }

    /**
     * Executes the post-eligibility Visa install script over NFC.
     *
     * Shows the NFC sheet, runs the script via the wearable SDK, and on success proceeds to
     * [fetchAssetForTermsAndConditions]. Failures route to [handlePrepSeScriptsFailure].
     *
     * @param jsonString Decoded install-script JSON bytes to send to the secure element.
     * @param eligibilityResponse Successful check-eligibility response driving the next step.
     * @param capturedCardDetail The [CapturedCardDetail] being provisioned.
     * @param digitizationReferenceNumberDelete Digitization reference used for cleanup on failure.
     * @param pnoType Payment network operator type (MDES or VTS).
     */
    private fun executePostEligibilityNfcScript(
        jsonString: ByteArray?,
        eligibilityResponse: CheckEligibilityResponseBody,
        capturedCardDetail: CapturedCardDetail,
        digitizationReferenceNumberDelete: String,
        pnoType: String,
    ) {
        val nfcApduResponses = mutableListOf<String>()
        showNfcSheet(parentFragmentManager, onCancelClick = {
            findNavController().navigate(R.id.cardListFragment)
        })
        NfcScriptExecutionTracker.onNfcScriptStarted()
        SecoraWearableSDK.getInstance().getInterface().executeNfcOperation(
            activity,
            SCRIPT,
            jsonString,
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
                    handleNfcCplcCallback(
                        seid,
                        tagId,
                        icTypeHex,
                        oemIdHex,
                        seGroupIdHex,
                        wearableModelIdHex
                    )
                }

                override fun onApduProgress(request: String?, response: String?) {
                    if (!response.isNullOrBlank()) {
                        nfcApduResponses.add(response)
                    }
                }

                override fun onSuccess(
                    responseItems: MutableList<ApduResponsesItem>?,
                    completed: Boolean
                ) {
                    if (!completed) return
                    NfcScriptExecutionTracker.onNfcScriptFinished()
                    hideNfcSheet()
                    clearInstallScriptPreference()
                    fetchAssetForTermsAndConditions(eligibilityResponse, capturedCardDetail, sdScript = null)
                }

                override fun onError(error: String?) {
                    NfcScriptExecutionTracker.onNfcScriptFinished()
                    hideNfcSheet()
                    clearInstallScriptPreference()
                    handlePrepSeScriptsFailure(
                        resolveScriptExecutionErrorMessage(error),
                        digitizationReferenceNumberDelete,
                        pnoType
                    )
                }
            })
    }

    /**
     * Processes the result of post-eligibility Visa script execution on the UI thread.
     *
     * On success, forwards the optional [ScriptExecutionResult.sdScript] to
     * [fetchAssetForTermsAndConditions]; on failure, shows the scripts failure dialog via
     * [handlePrepSeScriptsFailure].
     *
     * @param executionResult Result from [ScriptHandler.executeScript].
     * @param eligibilityResponse Successful check-eligibility response driving the next step.
     * @param capturedCardDetail The [CapturedCardDetail] being provisioned.
     * @param digitizationReferenceNumberDelete Digitization reference used for cleanup on failure.
     * @param pnoType Payment network operator type (MDES or VTS).
     */
    private fun onPostEligibilityScriptExecutionComplete(
        executionResult: ScriptExecutionResult,
        eligibilityResponse: CheckEligibilityResponseBody,
        capturedCardDetail: CapturedCardDetail,
        digitizationReferenceNumberDelete: String,
        pnoType: String,
    ) {
        if (!isAdded) return
        clearInstallScriptPreference()
        isBleFlowInProgress = false
        if (executionResult.success) {
            fetchAssetForTermsAndConditions(
                eligibilityResponse,
                capturedCardDetail,
                executionResult.sdScript
            )
        } else {
            handlePrepSeScriptsFailure(
                getString(R.string.error_adding_mcm),
                digitizationReferenceNumberDelete,
                pnoType
            )
        }
    }

    /**
     * Handles exceptions thrown during post-eligibility Visa script execution.
     *
     * Clears the cached install script, dismisses the loader, and routes BLE failures to
     * [handlePostEligibilityScriptFailureBle] or shows [showPostEligibilityScriptFailureDialog]
     * for NFC.
     *
     * @param throwable The exception encountered during script execution.
     * @param eligibilityResponse Successful check-eligibility response driving the next step.
     * @param capturedCardDetail The [CapturedCardDetail] being provisioned.
     * @param jsonString Install-script bytes used for BLE retry after reconnect.
     * @param digitizationReferenceNumberDelete Digitization reference used for cleanup on failure.
     * @param pnoType Payment network operator type (MDES or VTS).
     */
    private fun handlePostEligibilityScriptException(
        throwable: Throwable,
        eligibilityResponse: CheckEligibilityResponseBody,
        capturedCardDetail: CapturedCardDetail,
        jsonString: ByteArray?,
        digitizationReferenceNumberDelete: String,
        pnoType: String,
    ) {
        clearInstallScriptPreference()
        logger.debug(
            "postEligibilityScript: Exception=${throwable.message}, " +
                "BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}"
        )
        activity.showLoading(false, "")
        if (!isNFC()) {
            handlePostEligibilityScriptFailureBle(
                throwable,
                eligibilityResponse,
                capturedCardDetail,
                jsonString,
                digitizationReferenceNumberDelete,
                pnoType
            )
            return
        }
        showPostEligibilityScriptFailureDialog(digitizationReferenceNumberDelete, pnoType)
    }

    /**
     * Shows the post-eligibility script failure dialog and resets add-card UI state when active.
     *
     * @param digitizationReferenceNumberDelete Digitization reference used for cleanup on confirm.
     * @param pnoType Payment network operator type (MDES or VTS).
     */
    private fun showPostEligibilityScriptFailureDialog(
        digitizationReferenceNumberDelete: String,
        pnoType: String,
    ) {
        if (isAddCardFlowActive) {
            resetAddCardFlowState()
        }
        deleteDialog(getString(R.string.error_adding_mcm), digitizationReferenceNumberDelete, pnoType)
    }

    /**
     * Handles BLE-specific post-eligibility script failures with reconnect and cleanup paths.
     *
     * Mirrors [handleAddMCMFailureBle]: transport errors show a connection-lost dialog, APDU errors
     * show the failure dialog immediately, and transient disconnects offer reconnect before retrying
     * via [retryPostEligibilityScriptAfterBleReconnect].
     *
     * @param throwable The execution error from script execution.
     * @param eligibilityResponse Successful check-eligibility response driving the next step.
     * @param capturedCardDetail The [CapturedCardDetail] being provisioned.
     * @param jsonString Install-script bytes used for BLE retry after reconnect.
     * @param digitizationReferenceNumberDelete Digitization reference used for cleanup on failure.
     * @param pnoType Payment network operator type (MDES or VTS).
     */
    private fun handlePostEligibilityScriptFailureBle(
        throwable: Throwable,
        eligibilityResponse: CheckEligibilityResponseBody,
        capturedCardDetail: CapturedCardDetail,
        jsonString: ByteArray?,
        digitizationReferenceNumberDelete: String,
        pnoType: String,
    ) {
        if (ScriptHandler.isBleTransportError(throwable)) {
            showProvisionScriptBleConnectionLostDialog {
                finishCardListAddCardAbortCleanup()
                abortAddCardCleanupWithoutBle(DELETED_STATUS, digitizationReferenceNumberDelete, pnoType)
            }
            return
        }
        val seId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)
        val bleStillConnected = !seId.isNullOrBlank() &&
            BluetoothStateManager.isDeviceConnected(seId, activity)
        if (bleStillConnected || ScriptHandler.isScriptApduStatusWordError(throwable)) {
            showPostEligibilityScriptFailureDialog(digitizationReferenceNumberDelete, pnoType)
            return
        }
        showProvisionBleReconnectPrompt(
            onConnected = {
                retryPostEligibilityScriptAfterBleReconnect(
                    eligibilityResponse,
                    capturedCardDetail,
                    jsonString,
                    digitizationReferenceNumberDelete,
                    pnoType
                )
            },
            onDeclined = {
                finishCardListAddCardAbortCleanup()
                abortAddCardCleanupWithoutBle(DELETED_STATUS, digitizationReferenceNumberDelete, pnoType)
            }
        )
    }

    /**
     * Retries the post-eligibility Visa install script over BLE after reconnect.
     *
     * Aborts the add-card flow when [jsonString] is null; otherwise re-invokes
     * [executePostEligibilityScript] with a fresh [ScriptHandler].
     *
     * @param eligibilityResponse Successful check-eligibility response driving the next step.
     * @param capturedCardDetail The [CapturedCardDetail] being provisioned.
     * @param jsonString Install-script bytes to resend to the secure element.
     * @param digitizationReferenceNumberDelete Digitization reference used for cleanup on abort.
     * @param pnoType Payment network operator type (MDES or VTS).
     */
    private fun retryPostEligibilityScriptAfterBleReconnect(
        eligibilityResponse: CheckEligibilityResponseBody,
        capturedCardDetail: CapturedCardDetail,
        jsonString: ByteArray?,
        digitizationReferenceNumberDelete: String,
        pnoType: String,
    ) {
        if (jsonString == null) {
            finishCardListAddCardAbortCleanup()
            abortAddCardCleanupWithoutBle(DELETED_STATUS, digitizationReferenceNumberDelete, pnoType)
            return
        }
        activity.showLoading(true, getString(R.string.text_please_wait))
        executePostEligibilityScript(
            createScriptHandler(),
            jsonString,
            eligibilityResponse,
            capturedCardDetail,
            digitizationReferenceNumberDelete,
            pnoType
        )
    }

    /**
     * Maps low-level script loader errors (e.g. StatusWordError) to a user-friendly message.
     */
    private fun resolveScriptExecutionErrorMessage(error: String?): String {
        if (error.isNullOrBlank()) {
            return getString(R.string.error_adding_mcm)
        }
        if (error.contains("StatusWordError", ignoreCase = true) ||
            error.contains("returned status word 0x", ignoreCase = true) ||
            error.contains("Error during JSON script loading", ignoreCase = true)
        ) {
            return getString(R.string.error_adding_mcm)
        }
        return error
    }

    /**
     * Handles post-eligibility script execution or scripts-API failures during add-card.
     *
     * Dismisses the loader, re-enables FCM card refresh, and shows a delete confirmation dialog so
     * the user can clean up the partially provisioned card.
     *
     * @param message User-facing error message to display.
     * @param digitizationReferenceNumberDelete Digitization reference used for cleanup on confirm.
     * @param pnoType Payment network operator type (MDES or VTS).
     */
    private fun handlePrepSeScriptsFailure(
        message: String,
        digitizationReferenceNumberDelete: String,
        pnoType: String,
    ) {
        suppressFcmCardRefresh = false
        activity.showLoading(false, "")
        deleteDialog(message, digitizationReferenceNumberDelete, pnoType)
    }

    /**
     * Returns whether the given PNO type identifies a Visa (VTS) card.
     *
     * @param pnoType Payment network operator type from the API response.
     * @return `true` when [pnoType] equals [PNO_VTS].
     */
    private fun isVisaPno(pnoType: String?): Boolean = pnoType == PNO_VTS

    /**
     * Fetches Terms & Conditions assets required before proceeding.
     *
     * @param response Check eligibility response containing the terms asset ID.
     * @param capturedCardDetail Card details captured during add-card flow.
     * @param sdScript Combined sdScript hex from INSTALL script sequence 8, passed through to digitization.
     */
    private fun fetchAssetForTermsAndConditions(
        response: CheckEligibilityResponseBody,
        capturedCardDetail: CapturedCardDetail,
        sdScript: String? = null,
    ) {
        lifecycleScope.launch {
            if (activity.isFinishing) return@launch
            val sdkResult = WalletRepository.fetchAsset(
                context = activity,
                paymentAppInstanceId = paymentAppInstanceId ?: "",
                assetId = response.termsAndConditionsAssetId.toString(),
                digitizationReferenceNumber = response.digitizationReferenceNumber.toString()
            )
            if (sdkResult.isSuccess) {
                val body = sdkResult.response
                if (body != null) {
                    logger.debug("fetchAsset: Success, status=${response.statusMessage}, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
                    if (::activity.isInitialized && !activity.isFinishing) {
                        activity.runOnUiThread {
                            processFetchAssetResponse(body, capturedCardDetail, sdScript)
                        }
                    }
                } else {
                    suppressFcmCardRefresh = false
                    activity.showLoading(false, "")
                }
            } else {
                logger.debug("fetchAsset: Error, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
                suppressFcmCardRefresh = false
                activity.showLoading(false, "")
                handleSessionOrError(sdkResult.errorMessage)
            }
        }
    }

    /**
     * Processes asset response and navigates to Terms screen if available.
     *
     * @param response Fetch asset response containing terms media content.
     * @param capturedCardDetail Card details captured during add-card flow.
     * @param sdScript Combined sdScript hex from INSTALL script sequence 8, passed through to digitization.
     */
    private fun processFetchAssetResponse(
        response: FetchAssetResponseBody,
        capturedCardDetail: CapturedCardDetail,
        sdScript: String? = null,
    ) {
        suppressFcmCardRefresh = false
        try {
            if (response.mediaContents.isNotEmpty()) {
                navigateToTermsFragment(response, capturedCardDetail, sdScript)
            } else {
                activity.showLoading(false, "")
                statusDialog(activity, response.statusMessage)
            }
        } catch (_: Exception) {
            logger.debug("fetchAsset: Error decoding data, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
        }
    }

    /**
     * Navigates user to Terms and Conditions screen with decoded data.
     *
     * @param response Fetch asset response containing terms media content.
     * @param capturedCardDetail Card details captured during add-card flow.
     * @param sdScript Combined sdScript hex from INSTALL script sequence 8, forwarded in navigation arguments.
     */
    private fun navigateToTermsFragment(
        response: FetchAssetResponseBody,
        capturedCardDetail: CapturedCardDetail,
        sdScript: String? = null,
    ) {
        resetAddCardFlowState()
        val data: ByteArray = Base64.decode(response.mediaContents[0].data, Base64.DEFAULT)
        val termsAndCondition = String(data, StandardCharsets.UTF_8)
        StorageRepository.saveString(PreferenceKey.TERMS_DATA, termsAndCondition)

        val bundle = Bundle().apply {
            putSerializable(BundleKey.CAPTURED_CARD_DETAIL, capturedCardDetail)
            putString(BundleKey.PAYMENT_APP_INSTANCE_ID, paymentAppInstanceId)
            putString(BundleKey.DIGITIZATION_REFERENCE_NUMBER, digitizationReferenceNumber)
            sdScript?.let { putString(BundleKey.SD_SCRIPT, it) }
        }
        findNavController().navigate(R.id.termsFragment, bundle)
    }

    /**
     * Navigates the add-card flow to card capture, ensuring BLE is connected first on BLE wearables.
     */
    private fun navigateAddToCard() {
        if (!isAdded) return
        if (isNavigatingToAddCard) {
            logger.debug("navigateAddToCard: already in progress, skipping")
            return
        }
        isNavigatingToAddCard = true
        if (!isNFC()) {
            ensureBleConnectedThenRun(
                onConnected = {
                    captureCardDetails()
                },
                onCancelled = {
                    logger.debug("User cancelled BLE reconnect before capture card")
                    isNavigatingToAddCard = false
                    resetAddCardFlowState()
                }
            )
        } else {
            captureCardDetails()
        }
    }

    private data class NfcPendingDeleteFlowContext(
        val pendingResponse: GetPendingResponse,
        val pendingActivity: MainActivity,
        val paymentId: String,
        val pnoType: String,
    )

    /**
     * Resolves the Secure Element ID used for NFC delete-script execution.
     */
    private fun resolveSeIdForNfcDeleteExecution(): String {
        return seId?.takeIf { it.isNotBlank() }
            ?: StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)
    }

    /**
     * Finishes NFC delete flow when the script list is empty.
     *
     * @param pendingContext Optional pending-task context for portal-driven deletes.
     * @param seIdResolved Secure Element ID used for acknowledgement.
     * @param tokenRefNumber Digitization reference number of the deleted card.
     */
    private fun finishNfcDeleteWhenListEmpty(
        pendingContext: NfcPendingDeleteFlowContext?,
        seIdResolved: String,
        tokenRefNumber: String
    ) {
        if (pendingContext != null) {
            handleDeleteScriptSuccess(
                pendingContext.pendingActivity,
                seIdResolved,
                tokenRefNumber
            )
        } else {
            isDeleteFlow = false
            activity.showLoading(false, "")
        }
    }

    /**
     * Finishes NFC delete flow after the last script index has been processed.
     *
     * @param pendingContext Optional pending-task context for portal-driven deletes.
     * @param seIdResolved Secure Element ID used for acknowledgement.
     * @param tokenRefNumber Digitization reference number of the deleted card.
     */
    private fun finishNfcDeleteWhenIndexPastEnd(
        pendingContext: NfcPendingDeleteFlowContext?,
        seIdResolved: String,
        tokenRefNumber: String
    ) {
        if (pendingContext != null) {
            handleDeleteScriptSuccess(
                pendingContext.pendingActivity,
                seIdResolved,
                tokenRefNumber
            )
        } else {
            scriptDSEM = true
            isDeleteFlow = false
            activity.runOnUiThread { activity.showLoading(false, "") }
            onSequentialDeleteScriptsCompleted()
        }
    }

    /**
     * Handles invalid NFC delete-script payload by retrying or finishing the delete flow.
     *
     * @param scriptIndex Index of the script with invalid payload.
     * @param attemptIndex Current retry attempt for the script.
     * @param pendingContext Optional pending-task context for portal-driven deletes.
     * @param tokenRefNumber Digitization reference number of the deleted card.
     */
    private fun handleNfcDeleteInvalidPayload(
        scriptIndex: Int,
        attemptIndex: Int,
        pendingContext: NfcPendingDeleteFlowContext?,
        tokenRefNumber: String
    ) {
        if (pendingContext != null) {
            handleDeleteScriptFailure(
                scriptIndex,
                attemptIndex,
                pendingContext.pendingResponse,
                pendingContext.pendingActivity,
                tokenRefNumber,
                pendingContext.paymentId,
                pendingContext.pnoType
            )
        } else {
            isDeleteFlow = false
            logger.debug("executeDeleteScriptWithRetryNFC: jsonBytes is null or empty")
            if (::activity.isInitialized && !activity.isFinishing) {
                activity.runOnUiThread {
                    activity.showLoading(false, "")
                }
            }
        }
    }

    /**
     * Schedules execution of the next NFC delete script in the sequence.
     *
     * @param deleteScriptsList Full ordered list of delete scripts.
     * @param tokenRefNumber Digitization reference number of the deleted card.
     * @param nextIndex Index of the next script to execute.
     * @param pendingContext Optional pending-task context for portal-driven deletes.
     */
    private fun scheduleNextNfcDeleteScript(
        deleteScriptsList: List<DeleteScriptBase>,
        tokenRefNumber: String,
        nextIndex: Int,
        pendingContext: NfcPendingDeleteFlowContext?
    ) {
        executeDeleteScriptWithRetryNFC(
            deleteScriptsList,
            tokenRefNumber,
            scriptIndex = nextIndex,
            attemptIndex = 0,
            pendingContext = pendingContext
        )
    }

    /**
     * Completes a standalone NFC sequential delete flow and resumes add-card handling if needed.
     */
    private fun completeNfcSequentialDeleteFlow() {
        scriptDSEM = true
        isDeleteFlow = false
        activity.showLoading(false, "")
        onSequentialDeleteScriptsCompleted()
    }

    /**
     * Completes a portal-driven NFC pending delete after the final script succeeds.
     *
     * @param pendingContext Pending-task context for the delete flow.
     * @param seIdResolved Secure Element ID used for acknowledgement.
     * @param tokenRefNumber Digitization reference number of the deleted card.
     */
    private fun completeNfcPendingDeleteAfterLastScript(
        pendingContext: NfcPendingDeleteFlowContext,
        seIdResolved: String,
        tokenRefNumber: String
    ) {
        isDeleteFlow = false
        handleDeleteScriptSuccess(
            pendingContext.pendingActivity,
            seIdResolved,
            tokenRefNumber
        )
    }

    /**
     * Handles NFC delete-script execution errors with one retry per script when appropriate.
     *
     * @param deleteScriptsList Full ordered list of delete scripts.
     * @param tokenRefNumber Digitization reference number of the deleted card.
     * @param scriptIndex Index of the script that failed.
     * @param attemptIndex Current retry attempt for the script.
     * @param pendingContext Optional pending-task context for portal-driven deletes.
     */
    private fun handleNfcDeleteScriptExecutionError(
        deleteScriptsList: List<DeleteScriptBase>,
        tokenRefNumber: String,
        scriptIndex: Int,
        attemptIndex: Int,
        pendingContext: NfcPendingDeleteFlowContext?
    ) {
        if (pendingContext != null) {
            handleDeleteScriptFailure(
                scriptIndex,
                attemptIndex,
                pendingContext.pendingResponse,
                pendingContext.pendingActivity,
                tokenRefNumber,
                pendingContext.paymentId,
                pendingContext.pnoType
            )
            return
        }
        if (attemptIndex == 0) {
            logger.debug("First attempt failed, retrying...")
            executeDeleteScriptWithRetryNFC(
                deleteScriptsList,
                tokenRefNumber,
                scriptIndex = scriptIndex,
                attemptIndex = 1
            )
            return
        }
        scriptDSEM = false
        isDeleteFlow = false
        clearInstallScriptPreference()
        logger.debug("delete card: Final failure after retry")
        statusDialog(activity, Constants.UNABLE_TO_DELETE_MESSAGE)
        activity.showLoading(false, "")
    }

    /**
     * Builds the NFC [ScriptExecutionCallback] used for sequential delete-script execution.
     *
     * @param deleteScriptsList Full ordered list of delete scripts.
     * @param tokenRefNumber Digitization reference number of the deleted card.
     * @param scriptIndex Index of the script currently executing.
     * @param attemptIndex Current retry attempt for the script.
     * @param pendingContext Optional pending-task context for portal-driven deletes.
     * @param seIdResolved Secure Element ID used for acknowledgement.
     * @param totalScripts Total number of scripts in the sequence.
     */
    private fun nfcDeleteScriptExecutionCallback(
        deleteScriptsList: List<DeleteScriptBase>,
        tokenRefNumber: String,
        scriptIndex: Int,
        attemptIndex: Int,
        pendingContext: NfcPendingDeleteFlowContext?,
        seIdResolved: String,
        totalScripts: Int
    ): ScriptExecutionCallback {
        return object : ScriptExecutionCallback {
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
                responseItems: MutableList<ApduResponsesItem>?,
                completed: Boolean
            ) {
                if (!completed) {
                    logger.debug("Delete NFC progress callback received, waiting for completion")
                    return
                }
                NfcScriptExecutionTracker.onNfcScriptFinished()
                if (!isAdded) return
                activity.runOnUiThread {
                    hideNfcSheet()
                    clearInstallScriptPreference()
                    val current = deleteScriptsList[scriptIndex]
                    acknowledgeDeleteScriptSuccess(
                        listOf(current),
                        activity,
                        seIdResolved,
                        tokenRefNumber
                    )
                    val next = scriptIndex + 1
                    when {
                        next < totalScripts -> scheduleNextNfcDeleteScript(
                            deleteScriptsList,
                            tokenRefNumber,
                            next,
                            pendingContext
                        )

                        pendingContext != null -> completeNfcPendingDeleteAfterLastScript(
                            pendingContext,
                            seIdResolved,
                            tokenRefNumber
                        )

                        else -> completeNfcSequentialDeleteFlow()
                    }
                }
            }

            override fun onError(error: String) {
                NfcScriptExecutionTracker.onNfcScriptFinished()
                activity.runOnUiThread {
                    hideNfcSheet()
                    handleNfcDeleteScriptExecutionError(
                        deleteScriptsList,
                        tokenRefNumber,
                        scriptIndex,
                        attemptIndex,
                        pendingContext
                    )
                }
            }
        }
    }

    /**
     * NFC delete scripts: runs each entry in [deleteScriptsList] in order (with one retry per script).
     * When [pendingContext] is non-null, completion follows the get-pending-task flow;
     * otherwise the update-card-status sequential flow ([onSequentialDeleteScriptsCompleted]) is used.
     */
    private fun executeDeleteScriptWithRetryNFC(
        deleteScriptsList: List<DeleteScriptBase>?,
        tokenRefNumber: String,
        scriptIndex: Int,
        attemptIndex: Int,
        pendingContext: NfcPendingDeleteFlowContext? = null
    ) {
        val seIdResolved = resolveSeIdForNfcDeleteExecution()
        if (deleteScriptsList.isNullOrEmpty()) {
            finishNfcDeleteWhenListEmpty(pendingContext, seIdResolved, tokenRefNumber)
            return
        }
        val totalScripts = deleteScriptsList.size
        if (scriptIndex >= totalScripts) {
            finishNfcDeleteWhenIndexPastEnd(pendingContext, seIdResolved, tokenRefNumber)
            return
        }
        val jsonBytes = extractJsonBytesRetry(deleteScriptsList[scriptIndex].scriptData)
        StorageRepository.saveString(PreferenceKey.INSTALL_SCRIPT, jsonBytes.contentToString())

        if (jsonBytes == null || jsonBytes.isEmpty()) {
            handleNfcDeleteInvalidPayload(scriptIndex, attemptIndex, pendingContext, tokenRefNumber)
            return
        }
        showNfcSheet(parentFragmentManager)
        NfcScriptExecutionTracker.onNfcScriptStarted()
        SecoraWearableSDK.getInstance().getInterface().executeNfcOperation(
            activity,
            DELETE_SCRIPT,
            jsonBytes,
            null,
            null,
            nfcDeleteScriptExecutionCallback(
                deleteScriptsList,
                tokenRefNumber,
                scriptIndex,
                attemptIndex,
                pendingContext,
                seIdResolved,
                totalScripts
            )
        )
    }

    private lateinit var selectedCardForDelete: CardDetails
    private var currentPosition: Int = 0

    private fun resetWearableWallet() {
        ensureBleConnectedThenRun(
            onConnected = {

                val dialogBinding = DialogCommonMessageBinding.inflate(layoutInflater)
                val dialog = Dialog(requireContext())
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setContentView(dialogBinding.root)
                dialog.setCancelable(false)

                dialogBinding.txtTitle.text = getString(R.string.dialog_title)
                dialogBinding.txtMessage.text = getString(R.string.wearable_clear_device_confirm)
                dialogBinding.txtOK.setOnClickListener {
                    dialog.dismiss()
                    resetFlowFetchCards(requireContext())
                }
                dialogBinding.txtCancel.setOnClickListener { dialog.dismiss() }
                dialog.showSecure()
            }
        )
    }
    private fun resetFlowFetchCards(safeContext: Context) {

        ensureBleConnectedThenRun(
            onConnected = {
                activity.showLoading(true, getString(R.string.wearable_passcode_in_progress))
                lifecycleScope.launch {

                    val pid = paymentAppInstanceId?.takeIf { it.isNotBlank() }
                    if (pid != null) {
                        cardListDetail = StorageRepository.getUiCardListFromLocalDb(safeContext, pid)
                        if(cardListDetail.isNotEmpty()) {
                            currentPosition = 0
                            selectedCardForDelete = cardListDetail[currentPosition]
                            resetWalletDeleteCardFlow()
                        } else {
                            clearWearableDevice()
                        }

                    } else {
                        activity.showLoading(false, "")
                        logger.debug("Payment Instance Id not available")
                    }
                }
            }
        )
    }

    /**
     * Runs UC-09.
     *
     * No card-deletion handler is passed yet, so this clears the cardholder verification method
     * only. Wiring card deletion in is a separate step that needs the wallet backend calls.
     */
    private fun clearWearableDevice() {
        ensureBleConnectedThenRun(
            onConnected = {
                activity.showLoading(true, getString(R.string.wearable_passcode_in_progress))
                lifecycleScope.launch {
                    val outcome = CdcvmApi.resetCvm(requireContext())
                    activity.showLoading(false, "")
                    logger.debug("Wearable settings: clear device - ${outcome.describe()}")
//                    if (outcome.isSuccess) {
//                        showToast(getString(R.string.wearable_clear_device_success))
//                    } else {
//                        showToast(outcome.describe())
//                    }
                    statusMonitor.requestPoll()
                }
            }
        )
    }


    private fun resetWalletDeleteCardFlow() {
        fetchSequenceNumberFromDevice(
            onRetrieved = { currentSequenceCounter ->
                logger.debug("currentSequenceCounter : $currentSequenceCounter")
                handleUpdateCardStatusFLow(activity, false, currentSequenceCounter)
            },
            onFailed = {

            })
    }



    private fun handleUpdateCardStatusFLow(
        activity: MainActivity,
        showErrorDialog: Boolean = true,
        currentSequenceCounter: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (activity.isFinishing) return@launch
            val selectedCard = StorageRepository.getLocalCardListForStatusApi(
                activity,
                selectedCardForDelete.digitizationReferenceNumber.toString(),
                paymentAppInstanceId.toString()
            )

            isCardDeletionInProgress = true

            val deviceDetails = DeviceDetails(
                connected = isNFC() || BluetoothStateManager.isConnected,
                currentSequenceCounter = currentSequenceCounter
            )
            val sdkResult = WalletRepository.updateCardStatus(
                context = activity,
                cardStatus = DELETED_STATUS,
                cardList = selectedCard,
                paymentAppInstanceId = paymentAppInstanceId.toString(),
                digitizationReferenceNumber = selectedCardForDelete.digitizationReferenceNumber.toString(),
                pnoType = selectedCardForDelete.pnoType.toString(),
                deviceDetails = deviceDetails
            )

            when {
                sdkResult.isSuccess ->
                    dispatchUpdateCardStatusSuccess(activity, sdkResult, selectedCardForDelete.digitizationReferenceNumber.toString(), showErrorDialog)

                else ->
                    dispatchUpdateCardStatusFailure(activity, sdkResult)
            }
        }
    }

    private fun dispatchUpdateCardStatusSuccess(
        activity: MainActivity,
        sdkResult: WalletSdkResult<UpdateCardStatusResponse>,
        tokenRefNumber: String,
        showErrorDialog: Boolean
    ) {
        val body = sdkResult.response ?: return
        if (body.statusMessage.isNullOrEmpty()) {
            activity.runOnUiThread {
                if (!activity.isFinishing) {
                    isCardDeletionInProgress = false
                    activity.showLoading(false, "")
                }
            }
            return
        }
        activity.runOnUiThread {
            if (!activity.isFinishing) {
                activity.showLoading(true, getString(R.string.text_please_wait))
                handleDeleteCardResponse(
                    body.statusMessage,
                    tokenRefNumber,
                    body.deleteScriptList,
                    activity,
                    showErrorDialog
                )
            }
        }
    }

    private fun dispatchUpdateCardStatusFailure(
        activity: MainActivity,
        sdkResult: WalletSdkResult<UpdateCardStatusResponse>
    ) {
        activity.runOnUiThread {
            if (activity.isFinishing) return@runOnUiThread
            activity.showLoading(false, "")

            isCardDeletionInProgress = false
            handleSessionOrError(sdkResult.errorMessage) {
                statusDialog(activity, sdkResult.errorMessage)
            }
        }
    }

    private fun handleDeleteCardResponse(
        statusMessage: String?,
        tokenRefNumber: String,
        deleteScriptsList: List<DeleteScriptBase>?,
        activity: MainActivity,
        showErrorDialog: Boolean
    ) {
        if (!isAdded) return
        when {
            statusMessage.equals(CommonResponse.SUCCESS.response) -> {
                // Execute script with retry logic
                activity.showLoading(true, getString(R.string.text_please_wait))
                if (cardListDetail.size > (currentPosition + 1)) {
                    currentPosition++
                    selectedCardForDelete = cardListDetail[currentPosition]
                    resetWalletDeleteCardFlow()
                } else {
                    isCardDeletionInProgress = false
                    clearWearableDevice()

                }
            }

            statusMessage?.contains(TOKEN_IS_ALREADY_IN_DELETED_STATE) == true -> {
                activity.showLoading(true, getString(R.string.text_please_wait))
            }

            statusMessage?.contains(NO_PENDING_TASK, ignoreCase = true) == true ||
                (statusMessage == CommonResponse.SUCCESS.response && deleteScriptsList.isNullOrEmpty()) -> {
                lifecycleScope.launch {
                    activity.showLoading(false, "")
                    StorageRepository.deleteLocalCardByDigitizeRef(requireContext(), tokenRefNumber)
                    CardListFragment.shouldForceApiRefresh = true
                }
            }

            else -> {
                activity.showLoading(true, getString(R.string.text_please_wait))
                if (showErrorDialog) {
                    resetWalletDeleteDialog(
                        activity,
                        getString(R.string.text_card_deleted_error)
                    )
                }
            }
        }
    }

    /**
     * Displays a custom delete confirmation dialog and navigates back upon confirmation.
     *
     * @param context The context used to create and display the dialog.
     * @param message The message to be displayed in the dialog.
     */
    fun resetWalletDeleteDialog(context: Context, message: String?) {
        (context as? Activity)?.runOnUiThread {
            val alertDialog = Dialog(context)
            alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            alertDialog.setContentView(R.layout.dialog_common_message)
            alertDialog.setCancelable(false)

            val btnPasswordInfoOk = alertDialog.findViewById<TextView>(R.id.txtOK)
            val tvTitle = alertDialog.findViewById<TextView>(R.id.txtTitle)
            tvTitle.text = context.getString(R.string.text_secora_wallet)

            val txtMessage = alertDialog.findViewById<TextView>(R.id.txtMessage)
            txtMessage.text = message

            val txtCancel = alertDialog.findViewById<TextView>(R.id.txtCancel)
            txtCancel.visibility = View.GONE
            btnPasswordInfoOk.setOnClickListener {
                alertDialog.dismiss()
            }
            alertDialog.showSecure()
        }
    }


}
