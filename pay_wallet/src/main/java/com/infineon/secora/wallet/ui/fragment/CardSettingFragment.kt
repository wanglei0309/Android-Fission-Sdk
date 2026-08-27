// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: SettingFragment.kt is a comprehensive card management screen that handles all wallet card configuration
 * including activating, suspending, deleting, and setting default cards, as well as notification preferences.
 **/
package com.infineon.secora.wallet.ui.fragment

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.client.data.models.AcknowledgeResponse
import com.infineon.secora.wallet.client.data.models.DefaultCardResponse
import com.infineon.secora.wallet.client.data.models.DeleteScriptBase
import com.infineon.secora.wallet.client.data.models.GetPendingResponse
import com.infineon.secora.wallet.client.data.models.common.UpdateTransactionNotificationResponse
import com.infineon.secora.wallet.client.data.models.common.UpdateCardStatusResponse
import com.infineon.secora.wallet.client.operations.middleware.callbacks.UiCallback
import com.infineon.secora.wallet.client.util.AppDispatchers
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.FragmentWalletCardSettingBinding
import com.infineon.secora.wallet.utils.helper.showSecure
import com.infineon.secora.wallet.domain.walletsdk.WalletRepository
import com.infineon.secora.wallet.domain.walletsdk.WalletSdkResult
import com.infineon.secora.wallet.domain.walletsdk.isInvalidStatusMessageOrEmptyScript
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.domain.wearable.ble.script.ScriptHandler
import com.infineon.secora.wallet.firebase.AppEvent
import com.infineon.secora.wallet.firebase.EventBus
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.utils.CommonResponse
import com.infineon.secora.wallet.utils.helper.NfcScriptExecutionTracker
import com.infineon.secora.wallet.utils.helper.ScriptDataParser
import com.infineon.secora.wallet.utils.Utils.isNetworkAvailable
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.constants.Constants
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_TOGGLE
import com.infineon.secora.wallet.utils.constants.Constants.ACTIVATE_CARD
import com.infineon.secora.wallet.utils.constants.Constants.ACTIVE_STATUS
import com.infineon.secora.wallet.utils.constants.Constants.DEFAULT_CARD_CHANGE
import com.infineon.secora.wallet.utils.constants.Constants.DELETED_CARD
import com.infineon.secora.wallet.utils.constants.Constants.DELETED_STATUS
import com.infineon.secora.wallet.utils.constants.Constants.INTERNAL_ERROR_CARD_DELETED
import com.infineon.secora.wallet.utils.constants.Constants.NO_PENDING_TASK
import com.infineon.secora.wallet.utils.constants.Constants.PENDING_STATUS
import com.infineon.secora.wallet.utils.constants.Constants.PPSE
import com.infineon.secora.wallet.utils.constants.Constants.DELETE_SCRIPT
import com.infineon.secora.wallet.utils.constants.Constants.DELETE_SCRIPT_CLEAR_DEFAULT
import com.infineon.secora.wallet.utils.constants.Constants.SUSPEND_CARD
import com.infineon.secora.wallet.utils.constants.Constants.SUSPEND_STATUS
import com.infineon.secora.wallet.utils.constants.Constants.TOKEN_IS_ALREADY_IN_DELETED_STATE
import com.infineon.secora.wallet.utils.constants.Constants.UNABLE_TO_DELETE
import com.infineon.secora.wearable.SecoraWearableSDK
import com.infineon.secora.wearable.apdu.ApduResponsesItem
import com.infineon.secora.wearable.nfc.ScriptExecutionCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * SettingFragment : It handles the setting related feature implementation
 *
 * @property pnoType
 * @property cardStatus
 * @property panSuffix
 */
class CardSettingFragment(val pnoType: String, var cardStatus: String, val panSuffix: String) :
    BaseFragment() {

    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)
    private var isOn = true
    private var isDefaultCardOn = false
    private var isTransactionNotificationEnabled = false
    private lateinit var binding: FragmentWalletCardSettingBinding
    private var tokenRefNumber: String? = null
    private var paymentId: String? = null
    private var seId: String? = null
    private var cardDeletionInProgress: Boolean = false
    private var toggleInProgress = false
    private var isNicknameDialogOpen = false

    /**
     * Handles suspend, activate, and delete card events:
     * - Database updates for card status changes.
     * - UI updates based on the current card state.
     * - Card deletion handling.
     * - Forces API refresh in CardListFragment after any state change.
     *
     * All operations are wrapped in a try-catch block to prevent crashes
     * due to unexpected event data.
     */
    private suspend fun handleRefreshEvent(event: AppEvent) {
        try {
            val msgType = event.getStringExtra(BundleKey.MSG_TYPE)
            if (msgType == "null") return

            when (msgType) {

                SUSPEND_CARD -> {
                    StorageRepository.updateLocalCardStatus(
                        requireContext(),
                        tokenRefNumber,
                        getString(R.string.text_suspended)
                    )

                    updateScreenActive()
                    cardStatus = SUSPEND_STATUS
                    logger.debug("handleRefreshEvent: SUSPEND → disabling default button")

                    binding.defaultCardBtn.isEnabled = false
                    binding.defaultCardBtn.isClickable = false
                    binding.defaultCardBtn.isChecked = false
                    CardListFragment.shouldForceApiRefresh = true
                }

                ACTIVATE_CARD -> {
                    StorageRepository.updateLocalCardStatus(
                        requireContext(),
                        tokenRefNumber,
                        getString(R.string.text_active)
                    )
                    updateScreenSuspend()
                    cardStatus = ACTIVE_STATUS
                    logger.debug("handleRefreshEvent: ACTIVE → enabling default button")

                    binding.defaultCardBtn.isEnabled = true
                    binding.defaultCardBtn.isClickable = true
                    binding.defaultCardBtn.isChecked = true
                    CardListFragment.shouldForceApiRefresh = true
                }

                DELETED_CARD -> {
                    // Handled centrally by MainActivity → FcmDeletedCardHandler (all screens).
                }
            }

        } catch (e: Exception) {
            logger.debug("Exception" + e.message)
        }
    }

    /**
     * Handles deleted card events received from refresh notifications.
     * Removes the card locally, triggers portal deletion flow,
     * and safely navigates back to the card list if BLE reconnect is cancelled.
     * Also updates card deletion state and forces card list API refresh.
     */
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
        logger.debug("SettingsFragment : onCreateView")
        binding = FragmentWalletCardSettingBinding.inflate(inflater, container, false)
        paymentId = StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID)
        tokenRefNumber = StorageRepository.readString(PreferenceKey.DIGITIZATION_REFERENCE_NUMBER)
        isTransactionNotificationEnabled = StorageRepository.readBoolean(PreferenceKey.updateTransactionNotificationKey(tokenRefNumber.toString()))
        logger.debug(":: card.digitizationReferenceNumber.toString() : ${tokenRefNumber.toString()}")
        logger.debug(":: isTransactionNotificationEnabled : ${isTransactionNotificationEnabled}")

        updateNotificationToggle()
        when {
            cardStatus.equals(ACTIVE_STATUS, ignoreCase = true) -> {
                isOn = true
                updateScreenSuspend()

                logger.debug("onCreateView: ACTIVE → enabling default button")
                binding.defaultCardBtn.isEnabled = true
                binding.defaultCardBtn.isClickable = true
            }

            cardStatus.equals(PENDING_STATUS, ignoreCase = true) -> {
                binding.tvPending1.visibility = View.VISIBLE
                binding.tvPending2.visibility = View.VISIBLE
                binding.clCard.visibility = View.GONE
            }

            cardStatus.equals(SUSPEND_STATUS, ignoreCase = true) -> {
                isOn = false
                updateScreenActive()

                logger.debug("onCreateView: SUSPEND → disabling default button")
                binding.defaultCardBtn.isEnabled = false
                binding.defaultCardBtn.isClickable = false
                binding.defaultCardBtn.isChecked = false
            }

            else -> {
                isOn = false
                updateScreenActive()
            }
        }
        binding.customSwitch1.setOnClickListener {
            if (toggleInProgress) {
                logger.debug("Toggle ignored: action already in progress")
                return@setOnClickListener
            }

            toggleInProgress = true
            updateToggle()
        }

        binding.defaultCardBtn.setOnClickListener {
            handleDefaultButtonClick()
        }
        refreshDefaultToggle()

        binding.customSwitch3.setOnClickListener {
            if (!isNetworkAvailable(requireContext())) {
                confirmDataDialog(getString(R.string.data_enable))
                return@setOnClickListener
            }
            val message = if (isTransactionNotificationEnabled) {
                getString(R.string.disable_payment_notifications)
            } else {
                getString(R.string.enable_payment_notifications)
            }

            statusDialogListener(
                requireContext(),
                message,
                okListener = {
                    val currentTokenRef = tokenRefNumber?.takeIf { it.isNotBlank() }
                    updateTransactionNotificationToggle(
                        requireActivity() as MainActivity,
                        paymentId.toString(),
                        currentTokenRef.toString(),
                        !isTransactionNotificationEnabled
                    )
                },
                cancelListener = {

                }
            )
        }

        binding.removeCard.setOnClickListener {

            if (!isNetworkAvailable(requireContext())) {
                confirmDataDialog(getString(R.string.data_enable))
                return@setOnClickListener
            }

            if (cardDeletionInProgress) {
                logger.debug("RemoveCard ignored: deletion already in progress")
                return@setOnClickListener
            }

            handleRemoveCardClickEvent()
        }

        binding.updateNickname.setOnClickListener {
            if (isNicknameDialogOpen) {
                logger.debug("UpdateNickname ignored: dialog already open")
                return@setOnClickListener
            }

            lifecycleScope.launch {
                updateNicknameDialog()
            }
        }
        binding.defaultCardBtn.setOnClickListener {
            handleDefaultButtonClick()
        }

        refreshDefaultToggle()
        seId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)
        return binding.root
    }

    /**
     * Handles the default card button click.
     * Prevents action if the selected card is already default (for NFC) or button is disabled.
     * Ensures BLE/NFC readiness before triggering the default card update.
     */
    private fun handleDefaultButtonClick() {
        if (isNFC()) {
            val defaultTokenRef = StorageRepository.readString(PreferenceKey.deviceKey(paymentId.toString()))

            if (tokenRefNumber == defaultTokenRef) {
                logger.debug("Blocked: NFC default card click")
                return
            }
        }
        if (!binding.defaultCardBtn.isEnabled) {
            logger.debug("Click ignored: button disabled")
            return
        }
        binding.defaultCardBtn.isChecked = isDefaultCardOn
        if (!isNFC()) {
            ensureBleConnectedThenRun(
                onConnected = {
                    handleDefaultBtnClickListener()
                }
            )
        } else {
            ensureNfcReadyThenRun(
                onConnected = {
                    handleDefaultBtnClickListener()
                }
            )
        }
    }

    private fun handleRemoveCardClickEvent() {
        if (isLoginOlderThanSessionExpiryDuration()) {
            navigateToLoginScreen()
            return
        }
        if (!isNFC()) {
            val isBluetoothOn =
                BluetoothStateManager.isBluetoothTurnedOn(requireContext())

            if (!isBluetoothOn) {
                statusDialog(requireContext(), getString(R.string.bluetooth_not_turned_on))
                return
            }
        }
        updateCardDeletionStatus(true)
        if (!isNFC()) {
            ensureBleConnectedThenRun(
                onConnected = {
                    statusDialogListener(
                        requireContext(),
                        if (
                            tokenRefNumber == StorageRepository.readString(PreferenceKey.deviceKey(paymentId.toString()))
                        ) {
                            getString(R.string.deleting_default_card)
                        } else {
                            getString(R.string.text_delete_card)
                        },
                        okListener = {
                            logger.debug("SettingsFragment : removeCard : cardDeletionInProgress : $cardDeletionInProgress")
                            logger.debug("SettingsFragment : removeCard : tokenRefNumber : ${tokenRefNumber.toString()}")
                            deleteCard(
                                requireActivity() as MainActivity,
                                DELETED_STATUS,
                                tokenRefNumber.toString()
                            )
                        },
                        cancelListener = {
                            updateCardDeletionStatus(false)
                            logger.debug("RemoveCard unlocked after dialog cancel")
                        }
                    )
                },

                onCancelled = {
                    updateCardDeletionStatus(false)
                }
            )
        } else {
            ensureNfcReadyThenRun(
                onConnected = {
                    statusDialogListener(
                        requireContext(),
                        if (
                            tokenRefNumber == StorageRepository.readString(
                                PreferenceKey.deviceKey(
                                    paymentId.toString()
                                )
                            )
                        ) {
                            getString(R.string.deleting_default_card)
                        } else {
                            getString(R.string.text_delete_card)
                        },
                        okListener = {
                            logger.debug("SettingsFragment : removeCard : cardDeletionInProgress : $cardDeletionInProgress")
                            logger.debug("SettingsFragment : removeCard : tokenRefNumber : ${tokenRefNumber.toString()}")
                            deleteCard(
                                requireActivity() as MainActivity,
                                DELETED_STATUS,
                                tokenRefNumber.toString()
                            )
                        },
                        cancelListener = {
                            updateCardDeletionStatus(false)
                            logger.debug("RemoveCard unlocked after dialog cancel")
                        }
                    )
                }
            )
        }
    }

    /**
     * Called after the Fragment's view has been created.
     *
     * This method starts collecting EventBus events in a lifecycle-aware way
     * using the viewLifecycleOwner. The collection is tied to the View lifecycle
     * instead of the Fragment lifecycle to avoid leaks and ensure correct
     * behavior during view recreation (e.g., rotation, backstack navigation).
     *
     * repeatOnLifecycle(STARTED) automatically:
     * - Starts collecting when the view is visible.
     * - Stops collecting when the view is not visible.
     * - Restarts collection when the view becomes visible again.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                EventBus.events.collect { event ->
                    if (event.action == ACTION_TOGGLE) {
                        handleRefreshEvent(event)
                    }
                }
            }
        }
    }

    /**
     * handleDefaultBtnClickListener(): It handles the default button click event.
     *
     */
    private fun handleDefaultBtnClickListener() {

        if (isLoginOlderThanSessionExpiryDuration()) {
            navigateToLoginScreen()
            return
        }

        if (!isNetworkAvailable(requireContext())) {
            binding.defaultCardBtn.isChecked = false
            confirmDataDialog(getString(R.string.data_enable))
            return
        }

        if (isDefaultCardOn) {
            binding.defaultCardBtn.isChecked = true
            return
        }
        if (!isNFC()) {
            val isBluetoothOn = BluetoothStateManager.isBluetoothTurnedOn(requireContext())

            if (!isBluetoothOn || !BluetoothStateManager.isConnected) {
                binding.defaultCardBtn.isChecked = false

                statusDialog(
                    requireContext(),
                    if (!isBluetoothOn)
                        getString(R.string.bluetooth_not_turned_on)
                    else
                        getString(R.string.bluetooth_not_connected)
                )
                return
            }
        }

        if (cardStatus == SUSPEND_STATUS) {
            binding.defaultCardBtn.isChecked = false
            statusDialog(requireActivity(), getString(R.string.suspend_default))
            return
        }

        binding.defaultCardBtn.isChecked = false

        statusDialogListener(
            requireContext(),
            getString(R.string.confirm_make_default_card),
            okListener = {
                isDefaultCardOn = true
                binding.defaultCardBtn.isChecked = true
                updateDefaultCardToggle(requireActivity() as MainActivity)
            }, cancelListener = {
                binding.defaultCardBtn.isChecked = false
            })
    }

    /**
     * Updates the UI to reflect the "suspend" state for the card.
     *
     * This method changes the card status text to indicate suspension,
     * updates the "On" text view with a selected background and primary color,
     * and resets the "Off" text view to a transparent background with black text color.
     */
    private fun updateScreenSuspend() {
        (requireActivity() as MainActivity).showLoading(false, "")
        binding.tvCardStatus.text = getString(R.string.text_suspend_card)
        binding.onText.setBackgroundResource(R.drawable.bg_on_selected)
        binding.onText.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.white)
        )
        binding.offText.setBackgroundColor(Color.TRANSPARENT)
        binding.offText.setTextColor(Color.BLACK)
    }

    /**
     * Called when the fragment becomes visible and actively running.
     *
     * Resets transient UI flags (such as in-progress deletion state) so the
     * settings actions are available after returning to the screen.
     *
     * @see androidx.fragment.app.Fragment.onResume
     */
    override fun onResume() {
        super.onResume()
        logger.debug("SettingsFragment : onResume")
        updateCardDeletionStatus(false)
    }

    /**
     * Updates the status of cardDeletion process.
     *
     */
    private fun updateCardDeletionStatus(status: Boolean) {
        cardDeletionInProgress = status
        binding.removeCard.isEnabled = !status
    }

    /**
     * Updates the UI to reflect the "active" state for the card.
     *
     * This method changes the card status text to indicate activation,
     * updates the "Off" text view with a selected background and primary color,
     * and resets the "On" text view to a transparent background with black text color.
     */
    private fun updateScreenActive() {
        (requireActivity() as MainActivity).showLoading(false, "")
        binding.tvCardStatus.text = getString(R.string.text_activate_card)
        binding.offText.setBackgroundResource(R.drawable.bg_on_selected)
        binding.offText.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.white)
        )
        binding.onText.setBackgroundColor(Color.TRANSPARENT)
        binding.onText.setTextColor(Color.BLACK)
    }

    /**
     * updateToggle(): It handles the toggle for card status
     *
     */
    private fun updateToggle() {
        val previousIsOn = isOn

        val message = if (isOn) {
            getString(R.string.make_card_suspend)
        } else {
            getString(R.string.make_card_active)
        }

        statusDialogListener(
            requireContext(),
            message,
            okListener = {
                if (!isNetworkAvailable(requireContext())) {
                    confirmDataDialog(getString(R.string.data_enable))
                    toggleInProgress = false
                    return@statusDialogListener
                }

                if (isOn) {
                    updateStatus(requireActivity() as MainActivity, SUSPEND_STATUS, false)
                    binding.onText.setBackgroundResource(R.drawable.bg_on_selected)
                    binding.onText.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.white)
                    )
                    binding.offText.setBackgroundColor(Color.TRANSPARENT)
                    binding.offText.setTextColor(Color.BLACK)
//                    isOn = false
                } else {
                    updateStatus(requireActivity() as MainActivity, ACTIVE_STATUS, true)
                    binding.offText.setBackgroundResource(R.drawable.bg_on_selected)
                    binding.offText.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.white)
                    )
                    binding.onText.setBackgroundColor(Color.TRANSPARENT)
                    binding.onText.setTextColor(Color.BLACK)
                }
                toggleInProgress = false
            },
            cancelListener = {
                isOn = previousIsOn
                toggleInProgress = false
            }
        )
    }

    /**
     * updateDefaultCardToggle(): It handles the toggle for default card
     *
     */
    fun updateDefaultCardToggle(activity: MainActivity) {
        val currentTokenRef = tokenRefNumber?.takeIf { it.isNotBlank() }
        if (currentTokenRef == null) {
            logger.debug("updateDefaultCardToggle: missing digitization ref")
            isDefaultCardOn = false
            binding.defaultCardBtn.isChecked = false
            refreshDefaultToggle()
            statusDialog(requireActivity(), getString(R.string.failed_to_set_default_card))
            return
        }
        val deviceKey = PreferenceKey.deviceKey(paymentId.toString())
        val savedDefaultTokenRef = StorageRepository.readString(deviceKey)

        lifecycleScope.launch {
            if (isDefaultCardOn) {
                handleDefaultOn(
                    activity,
                    currentTokenRef,
                    savedDefaultTokenRef,
                    deviceKey
                )
            } else {
                handleDefaultOff(savedDefaultTokenRef, currentTokenRef, deviceKey)
            }

            refreshDefaultToggle()
        }
    }

    /**
     * Handles the flow when the default card toggle is turned ON.
     * Extracted from main method to reduce cognitive complexity.
     * Logic remains exactly the same as original.
     */
    private suspend fun handleDefaultOn(
        activity: MainActivity,
        currentTokenRef: String,
        savedDefaultTokenRef: String?,
        deviceKey: String
    ) {
        val card = StorageRepository.getLocalCardListForStatusApi(
            activity,
            currentTokenRef,
            paymentId.toString()
        )
        val ppseFileName = card?.ppseFileName

        val scriptHandler = createScriptHandler()
        logger.debug(
            "handleDefaultOn: starting PPSE flow (card=${card != null}, ppse=$ppseFileName)"
        )

        val aid = StorageRepository.readString(PreferenceKey.cardAidKey(currentTokenRef))
            .takeIf { it.isNotBlank() }
            ?: StorageRepository.readString(PreferenceKey.spsdAppletInstanceAidKey(currentTokenRef))
        val cardType = StorageRepository.readString(PreferenceKey.aidCardTypeKey(currentTokenRef))

        if (isNFC()) {
            activity.showLoading(true, getString(R.string.text_please_wait))
            showNfcSheet(parentFragmentManager, onCancelClick = {
                activity.showLoading(false, "")
                refreshDefaultToggle()
            })

            NfcScriptExecutionTracker.onNfcScriptStarted()
            SecoraWearableSDK.getInstance().getInterface().executeNfcOperation(
                requireActivity(),
                PPSE,
                null,
                aid,
                cardType,
                object : ScriptExecutionCallback {

                    override fun onSeidDetected(
                        seid: String?, tagId: String?,
                        icTypeHex: String?, oemIdHex: String?,
                        seGroupIdHex: String?, wearableModelIdHex: String?
                    ) {
                        // This callback is not required for the default card NFC flow.
                    }

                    override fun onApduProgress(request: String?, response: String?) {
                        // APDU progress is not required for default card NFC flow.
                    }

                    override fun onSuccess(
                        responseItems: MutableList<ApduResponsesItem>?,
                        completed: Boolean
                    ) {
                        if (!completed) return
                        NfcScriptExecutionTracker.onNfcScriptFinished()

                        hideNfcSheet()
                        activity.showLoading(true, getString(R.string.text_please_wait))
                        setCardAsDefaultApiCall(
                            activity,
                            currentTokenRef,
                            savedDefaultTokenRef,
                            deviceKey
                        )
                    }

                    override fun onError(error: String?) {
                        NfcScriptExecutionTracker.onNfcScriptFinished()
                        requireActivity().runOnUiThread {
                            hideNfcSheet()
                            activity.showLoading(false, "")
                            refreshDefaultToggle()
                        }
                    }
                }
            )
        } else {
            activity.showLoading(true, getString(R.string.text_please_wait))
            val otherCardAppletInstanceAids = resolveOtherCardAppletInstanceAidsFromLocal(
                activity,
                paymentId.toString(),
                currentTokenRef
            )
            executePPSE(
                activity,
                scriptHandler,
                currentTokenRef,
                savedDefaultTokenRef,
                deviceKey,
                otherCardAppletInstanceAids
            )
        }
    }

    /**
     * Handles the flow when the default card toggle is turned OFF.
     * Extracted for readability and lower cognitive complexity.
     */
    private fun handleDefaultOff(
        savedDefaultTokenRef: String?,
        currentTokenRef: String,
        deviceKey: String
    ) {
        if (savedDefaultTokenRef == currentTokenRef) {
            StorageRepository.clearString(deviceKey)
        }

        binding.defaultCardBtn.isChecked = isDefaultCardOn
        notifyDefaultChanged()
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
            requireContext(),
            object : ScriptHandler.Callbacks {
                override fun showLoading(show: Boolean, msg: String) {
                    // Intentionally left blank because loading UI is handled elsewhere
                    // and ScriptHandler requires this callback to be implemented.
                }

                override fun showToast(message: String) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }

                override fun updateLogs(message: String) {
                    logger.debug("ScriptHandler: $message")
                }
            }
        )
    }

    /**
     * Executes a PPSE Script and handles the result callbacks.
     * This method preserves the original full flow including:
     * - Updating UI
     * - Toggling default card state
     * - Saving user preference
     * - Showing dialogs/logs
     * - Broadcasting default card change
     */
    private fun executePPSE(
        activity: MainActivity,
        scriptHandler: ScriptHandler,
        currentTokenRef: String,
        savedDefaultTokenRef: String?,
        deviceKey: String,
        otherCardAppletInstanceAids: List<String> = emptyList()
    ) {
        val aid = StorageRepository.readString(PreferenceKey.cardAidKey(currentTokenRef))
            .takeIf { it.isNotBlank() }
            ?: StorageRepository.readString(PreferenceKey.spsdAppletInstanceAidKey(currentTokenRef))
        val cardType = StorageRepository.readString(PreferenceKey.aidCardTypeKey(currentTokenRef))
        logger.debug(":: aid : $aid")
        logger.debug(":: cardType : $cardType")
        logger.debug(":: otherCardAppletInstanceAids : $otherCardAppletInstanceAids")

        scriptHandler.executePPSEScript(aid, cardType, otherCardAppletInstanceAids).thenAccept { retrySuccess ->
            requireActivity().runOnUiThread {
                if (retrySuccess) {
                    setCardAsDefaultApiCall(
                        activity, currentTokenRef,
                        savedDefaultTokenRef,
                        deviceKey
                    )
                } else {
                    activity.showLoading(false, "")
                    statusDialog(requireActivity(), getString(R.string.failed_to_set_default_card))
                }
            }
        }.exceptionally { retryThrowable ->
            requireActivity().runOnUiThread {
                isDefaultCardOn = false
                binding.defaultCardBtn.isChecked = isDefaultCardOn
                activity.showLoading(false, "")

                logger.debug("PPSE: Retry failed, exception=${retryThrowable.message}, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected}")
                statusDialog(requireActivity(), "PPSE: failed")
            }
            null
        }
    }

    /**
     * Resolves applet instance AIDs for other provisioned cards from local storage.
     *
     * Reads the local card list for the given payment app instance, excludes the current card,
     * and looks up each remaining card's applet instance AID from stored preferences.
     *
     * @param context Android context used to read local card data.
     * @param paymentAppInstanceId Payment app instance identifier used to load the local card list.
     * @param currentTokenRef Token reference of the card being set as default, which is excluded from the result.
     * @return A deduplicated list of applet instance AIDs for other provisioned cards, or an empty list if none are found.
     */
    private suspend fun resolveOtherCardAppletInstanceAidsFromLocal(
        context: Context,
        paymentAppInstanceId: String,
        currentTokenRef: String
    ): List<String> {
        return StorageRepository.getUiCardListFromLocalDb(context, paymentAppInstanceId)
            .asSequence()
            .mapNotNull { it.digitizationReferenceNumber }
            .filter { it != currentTokenRef }
            .mapNotNull { tokenRef ->
                StorageRepository.readString(PreferenceKey.cardAidKey(tokenRef)).takeIf { it.isNotBlank() }
                    ?: StorageRepository.readString(PreferenceKey.spsdAppletInstanceAidKey(tokenRef))
                        .takeIf { it.isNotBlank() }
            }
            .distinct()
            .toList()
    }

    /**
     * Sets the selected card as the default card using the SecoraWalletSDK API.
     *
     * Performs the operation on the IO dispatcher and updates the UI on the main
     * thread. On success, updates the default card state, persists the token
     * reference if needed, and notifies listeners. On failure, shows an error dialog.
     *
     * @param activity Host activity used for showing/hiding loading indicators.
     * @param currentTokenRef Token reference of the card to set as default.
     * @param savedDefaultTokenRef Previously stored default token reference.
     * @param preferenceStorage Preference manager used to save token references.
     * @param deviceKey Key under which the default token reference is stored.
     */
    private fun setCardAsDefaultApiCall(
        activity: MainActivity, currentTokenRef: String,
        savedDefaultTokenRef: String?,
        deviceKey: String
    ) {
        CoroutineScope(AppDispatchers.IO).launch {

            val sdkResult = WalletRepository.setCardAsDefault(
                context = activity,
                digitizationReferenceNumber = tokenRefNumber.toString()
            )

            viewLifecycleOwner.lifecycleScope.launch(AppDispatchers.MAIN) {
                activity.showLoading(false, "")
                if (sdkResult.isSuccess) {
                    sdkResult.response?.let { defaultCardResponse ->
                        handleSuccessSetCardAsDefault(
                            defaultCardResponse,
                            currentTokenRef,
                            savedDefaultTokenRef,
                            deviceKey
                        )
                    }
                } else {
                    handleSetDefaultCardErrorResponse(sdkResult.errorMessage)
                }
            }
        }
    }

    /**
     * Handles the response after attempting to set a card as the default card.
     *
     * If the response indicates success, this function:
     * - Updates the UI to reflect the default card selection state
     * - Shows a success dialog to the user
     * - Persists the new default token reference if it differs from the previously saved value
     * - Notifies listeners about the default card change
     *
     * If the response indicates failure, a failure dialog is displayed.
     *
     * @param response The API response containing the status of the default card operation.
     * @param currentTokenRef The token reference of the card being set as default.
     * @param savedDefaultTokenRef The previously saved default card token reference, if any.
     * @param preferenceStorage Storage used to persist the default card token reference.
     * @param deviceKey The key used to store and retrieve preferences for the current device.
     */
    private fun handleSuccessSetCardAsDefault(
        response: DefaultCardResponse,
        currentTokenRef: String,
        savedDefaultTokenRef: String?,
        deviceKey: String
    ) {
        if (response.statusMessage == CommonResponse.SUCCESS.response) {
            isDefaultCardOn = true
            binding.defaultCardBtn.isChecked = true
            binding.defaultCardBtn.isEnabled = false
            binding.defaultCardBtn.isClickable = false
            statusDialog(requireActivity(), getString(R.string.default_card_has_set))
            binding.defaultCardBtn.isChecked = isDefaultCardOn

            if (savedDefaultTokenRef != currentTokenRef) {
                StorageRepository.saveString(deviceKey, currentTokenRef)
            }
            notifyDefaultChanged()
        } else {
            isDefaultCardOn = false
            binding.defaultCardBtn.isChecked = false
            refreshDefaultToggle()
            statusDialog(requireActivity(), getString(R.string.failed_to_set_default_card))
        }
    }

    /**
     * Handles the error usecase of set default card response.
     *
     */
    private fun handleSetDefaultCardErrorResponse(errorMsg: String) {
        isDefaultCardOn = false
        binding.defaultCardBtn.isChecked = false
        refreshDefaultToggle()
        handleSessionOrError(errorMsg) {
            statusDialog(requireActivity(), errorMsg)
        }
    }

    /**
     * Posts an event to notify that default card state has changed,
     * and triggers a forced refresh for screens displaying card list.
     */
    private fun notifyDefaultChanged() {
        CardListFragment.shouldForceApiRefresh = true
        EventBus.post(DEFAULT_CARD_CHANGE)
    }

    /**
     * Clears the stored default-card token when the deleted card was the default so the card list
     * can promote another card on the next refresh.
     */
    private suspend fun clearSavedDefaultIfDeleted(deletedTokenRef: String) {
        val pid = paymentId?.toString()?.trim().orEmpty()
        val cardCountBeforeDelete = if (pid.isNotEmpty() && isAdded) {
            StorageRepository.getUiCardListFromLocalDb(requireContext(), pid).size
        } else {
            0
        }
        if (CardListFragment.onDefaultCardDeleted(deletedTokenRef, cardCountBeforeDelete)) {
            logger.debug("Cleared saved default after deleting default card ref=$deletedTokenRef")
        }
    }

    /**
     * Returns true when [tokenRef] is the currently saved default card for this payment instance.
     * Used to decide whether to run clear-default APDU ({@code 80 D2 05 00 01 00}) after delete.
     */
    private fun shouldClearDefaultOnDelete(tokenRef: String): Boolean {
        val defaultRef = StorageRepository.readString(
            PreferenceKey.deviceKey(paymentId.toString())
        ).trim()
        return tokenRef.isNotBlank() &&
            defaultRef.isNotEmpty() &&
            tokenRef.trim().equals(defaultRef, ignoreCase = true)
    }

    /**
     * Refreshes the UI toggle to reflect whether the current card is set as the default card.
     *
     * This method compares the currently selected token reference (`tokenRefNumber`) with the
     * saved default card token reference stored in `Storage` under the key `DEFAULT_CARD_TOKEN_REF`.
     *
     * - If the current card is the default card, it updates the UI to highlight the "On" toggle state.
     * - Otherwise, it highlights the "Off" toggle state.
     *
     * The toggle states are visually represented by changing the background and text colors
     * of the `onText2` and `offText2` views.
     *
     * Assumes `binding` is initialized and contains `onText2` and `offText2` TextViews.
     *
     */
    private fun refreshDefaultToggle() {
        val defaultTokenRef = StorageRepository.readString(PreferenceKey.deviceKey(paymentId.toString()))
        val currentTokenRef = tokenRefNumber

        val isSuspend = cardStatus.equals(SUSPEND_STATUS, ignoreCase = true)
        isDefaultCardOn = !isSuspend && currentTokenRef != null && currentTokenRef == defaultTokenRef
        binding.defaultCardBtn.isChecked = isDefaultCardOn
        binding.defaultCardBtn.isEnabled = !isDefaultCardOn && !isSuspend
        binding.defaultCardBtn.isClickable = !isDefaultCardOn && !isSuspend
        logger.debug("refreshDefaultToggle: isDefault=$isDefaultCardOn, isSuspend=$isSuspend")
    }

    /**
     * updateNotificationToggle(): It handles the toggle for notification
     *
     */
    fun updateNotificationToggle() {
        if (isTransactionNotificationEnabled) {
            binding.onText3.setBackgroundResource(R.drawable.bg_on_selected)
            binding.onText3.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.white)
            )
            binding.offText3.setBackgroundColor(Color.TRANSPARENT)
            binding.offText3.setTextColor(Color.BLACK)
        } else {
            binding.offText3.setBackgroundResource(R.drawable.bg_on_selected)
            binding.offText3.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.white)
            )
            binding.onText3.setBackgroundColor(Color.TRANSPARENT)
            binding.onText3.setTextColor(Color.BLACK)
        }
    }

    /**
     * Handles the toggle for transaction notification receiving status.
     *
     */
    private fun updateTransactionNotificationToggle(activity: MainActivity,
                                                    paymentAppInstanceId: String,
                                                    digitizationReferenceNumber: String,
                                                    enabled : Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            if (activity.isFinishing) return@launch
            activity.showLoading(true,getString(R.string.text_please_wait))
            val sdkResult = WalletRepository.updateTransactionNotification(
                context = activity,
                paymentAppInstanceId = paymentAppInstanceId,
                digitizationReferenceNumber = digitizationReferenceNumber,
                enabled = enabled.toString()
            )

            if (sdkResult.isSuccess) {
                sdkResult.response?.let { updateTransactionNotificationResponse ->
                    handleUpdateTransactionNotificationStatusResponse(activity, digitizationReferenceNumber, enabled, updateTransactionNotificationResponse)
                }
            } else {
                activity.runOnUiThread {
                    if (activity.isFinishing) return@runOnUiThread
                    activity.showLoading(false, "")

                    handleSessionOrError(sdkResult.errorMessage) {
                        statusDialog(activity, sdkResult.errorMessage)
                    }
                }
            }
        }
    }


    /**
     * Handles the toggle for transaction notification receiving status update api response.
     *
     */
    private fun handleUpdateTransactionNotificationStatusResponse(activity: MainActivity, digitizationReferenceNumber: String, enabled : Boolean, updateTransactionNotificationResponse : UpdateTransactionNotificationResponse) {

        activity.runOnUiThread {
            if (activity.isFinishing) return@runOnUiThread
            activity.showLoading(false, "")
            if (updateTransactionNotificationResponse.enabled == enabled.toString()) {
                isTransactionNotificationEnabled = enabled
                StorageRepository.saveBoolean(
                    key = PreferenceKey.updateTransactionNotificationKey(digitizationReferenceNumber),
                    value = isTransactionNotificationEnabled
                )
                updateNotificationToggle()
            } else {
                statusDialog(activity, "Status Confirmation mismatch")
            }
        }
    }

    /**
     * updateStatus(): It handles the api call for updating card status
     *
     * @param activity
     * @param status
     */
    private fun updateStatus(activity: MainActivity, status: String, isOn: Boolean) {
        val connected = BluetoothStateManager.isConnected
        activity.showLoading(true, getString(R.string.text_please_wait))
        if (!isNFC() && !connected) {
            executeUpdateCardStatus(
                activity = activity,
                status = status,
                isOn = isOn,
                connected = false,
                currentSequenceCounter = ""
            )
            return
        }
        fetchSequenceNumberFromDevice(
            onRetrieved = { currentSequenceCounter ->
                executeUpdateCardStatus(
                    activity = activity,
                    status = status,
                    isOn = isOn,
                    connected = isNFC() || connected,
                    currentSequenceCounter = currentSequenceCounter
                )
            },
            onFailed = {
                activity.runOnUiThread {
                    if (!activity.isFinishing) {
                        activity.showLoading(false, "")
                    }
                    toggleInProgress = false
                }
            }
        )
    }
    private fun executeUpdateCardStatus(
        activity: MainActivity,
        status: String,
        isOn: Boolean,
        connected: Boolean,
        currentSequenceCounter: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (activity.isFinishing) return@launch
            val selectedCard = StorageRepository.getLocalCardListForStatusApi(
                activity,
                tokenRefNumber.toString(),
                paymentId.toString()
            )

            val deviceDetails = DeviceDetails(
                connected = connected,
                currentSequenceCounter = currentSequenceCounter
            )

            val sdkResult = WalletRepository.updateCardStatus(
                context = activity,
                cardStatus = status,
                cardList = selectedCard,
                paymentAppInstanceId = paymentId.toString(),
                digitizationReferenceNumber = tokenRefNumber.toString(),
                pnoType = pnoType,
                deviceDetails = deviceDetails
            )

            if (sdkResult.isSuccess) {
                sdkResult.response?.let { updateCardStatusResponse ->
                    activity.runOnUiThread {
                        if (!activity.isFinishing) {
                            handleUpdateStatusResponse(
                                updateCardStatusResponse,
                                activity,
                                isOn
                            )
                        }
                    }
                }

            }else {
                activity.runOnUiThread {
                    if (activity.isFinishing) return@runOnUiThread
                    activity.showLoading(false, "")

                    handleSessionOrError(sdkResult.errorMessage) {
                        statusDialog(activity, sdkResult.errorMessage)
                    }
                }
            }
        }
    }

    /**
     * Handles the server response received after attempting to update a card's status.
     *
     * @param response The [UpdateCardStatusResponse] object containing the result of the API call.
     * The target card status being applied (e.g., ACTIVE or SUSPENDED).
     * @param activity The current [MainActivity] instance used for UI and context access.
     */
    private fun handleUpdateStatusResponse(
        response: UpdateCardStatusResponse,
        activity: MainActivity,
        isOn: Boolean
    ) {
        when {

            response.statusMessage?.equals(CommonResponse.SUCCESS.response) == true -> {
                this@CardSettingFragment.isOn = isOn
                activity.showLoading(false, "")
                showStatusUpdatedDialog(getString(R.string.updated_successfully))
            }

            response.statusMessage == getString(R.string.card_suspended_please_contact_admin) -> {
                activity.showLoading(false, "")
                showStatusUpdatedDialog(getString(R.string.card_suspended_message))
            }

            response.statusMessage == getString(R.string.token_status_is_already_updated_by_oem) -> {
                activity.showLoading(false, "")
                showStatusUpdatedDialog(getString(R.string.token_status_update_not_available))
            }

            response.statusMessage != CommonResponse.SUCCESS.response -> {
                activity.showLoading(false, "")
                showStatusUpdatedDialog(response.statusMessage)
            }
        }
    }

    /**
     * deleteCard(): It handles the api call for deleting card
     *
     * @param activity
     * @param status
     */
    fun deleteCard(
        activity: MainActivity,
        status: String,
        tokenRefNumber: String,
        showErrorDialog: Boolean = true
    ) {
        alertDialog?.dismiss()
        activity.showLoading(true, getString(R.string.text_please_wait))

        logger.debug("SettingsFragment : deleteCard : tokenRefNumber : $tokenRefNumber")
        logger.debug("SettingsFragment : deleteCard : this.tokenRefNumber : ${this.tokenRefNumber.toString()}")
        logger.debug("SettingsFragment : deleteCard : status : $status")

        fetchSequenceNumberFromDevice(
            onRetrieved = { currentSequenceCounter ->
                logger.debug("currentSequenceCounter : $currentSequenceCounter")
                handleUpdateCardStatusFLow(activity, status, tokenRefNumber, showErrorDialog, currentSequenceCounter)
            },
            onFailed = {

            })

    }

    private fun handleUpdateCardStatusFLow(
        activity: MainActivity,
        status: String,
        tokenRefNumber: String,
        showErrorDialog: Boolean = true,
        currentSequenceCounter: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
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
                pnoType = pnoType,
                deviceDetails = deviceDetails
            )

            when {
                sdkResult.isSuccess ->
                    dispatchUpdateCardStatusSuccess(activity, sdkResult, tokenRefNumber, showErrorDialog)

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
                    activity.showLoading(false, "")
                    updateCardDeletionStatus(false)
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
            handleSessionOrError(sdkResult.errorMessage) {
                statusDialog(activity, sdkResult.errorMessage)
                updateCardDeletionStatus(false)
            }
        }
    }

    /**
     * deleteCardFromPortal(): It handles the api call for deleting card from portal.
     *
     * @param activity
     */
    fun deleteCardFromPortal(
        activity: MainActivity,
        tokenRefNumber: String,
        showErrorDialog: Boolean = true,
        onBleCancelled: (() -> Unit)? = null
    ) {
        alertDialog?.dismiss()
        activity.showLoading(true, getString(R.string.text_please_wait))
        fetchSequenceNumberFromDevice(
            onRetrieved = { currentSequenceCounter ->
                logger.debug("currentSequenceCounter : $currentSequenceCounter")
                handleDeleteCardFromPortalGetPendingTaskFlow(
                    activity,
                    tokenRefNumber,
                    showErrorDialog,
                    currentSequenceCounter
                )
            },
            onFailed = {
                updateCardDeletionStatus(false)
            },
            onCancelled = onBleCancelled
        )

    }

    private fun handleDeleteCardFromPortalGetPendingTaskFlow(
        activity: MainActivity,
        tokenRefNumber: String,
        showErrorDialog: Boolean,
        currentSequenceCounter: String
    ) {

        // Capture safe references to activity and context
        val safeContext = activity.applicationContext

        CoroutineScope(Dispatchers.IO).launch {

            val safeDigitizeRef =
                tokenRefNumber.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            logger.debug("SettingsFragment : Calling Pending Task :")

            val sdkResult = WalletRepository.getPendingTask(
                context = safeContext,
                seId = seId.toString(),
                digitizationReferenceNumber = safeDigitizeRef.toString(),
                currentSequenceCounter = currentSequenceCounter
            )
            if (!isAdded) return@launch

            if (sdkResult.isSuccess) {
                sdkResult.response?.let { getPendingResponse ->
                    if (getPendingResponse.isInvalidStatusMessageOrEmptyScript()) {
                        logger.debug("SettingsFragment : Pending Task Empty :")
                        activity.runOnUiThread {
                            activity.showLoading(false, "")
                            updateCardDeletionStatus(false)
                        }
                        return@launch
                    }

                    logger.debug("SettingsFragment : Pending Task response.statusMessage : ${getPendingResponse.statusMessage}")
                    val deleteList = getPendingResponse.deleteScriptList
                    logger.debug("SettingsFragment : Pending Task deleteList  : $deleteList")
                    handleDeleteCardResponse(
                        getPendingResponse.statusMessage,
                        tokenRefNumber,
                        getPendingResponse.deleteScriptList,
                        activity,
                        showErrorDialog
                    )
                }
            } else {
                activity.showLoading(false, "")
                handleSessionOrError(sdkResult.errorMessage) {
                    updateCardDeletionStatus(false)
                    statusDialog(activity, sdkResult.errorMessage)
                }
            }
        }
    }

    /**
     * Handles the API response after attempting to delete a card.
     *
     * The [UpdateCardStatusResponse] received from the delete card API.
     * @param activity The current [MainActivity] instance used for UI interactions and context access.
     * @param showErrorDialog A flag that determines whether to show an error dialog on failure.
     */
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
                if (!isNFC())
                    executeDeleteScriptWithRetry(
                        deleteScriptsList,
                        tokenRefNumber,
                        activity,
                        scriptIndex = 0,
                        attemptIndex = 0
                    )
                else
                    executeDeleteScriptWithRetryNFC(
                        deleteScriptsList,
                        tokenRefNumber,
                        activity,
                        scriptIndex = 0,
                        attemptIndex = 0
                    )

            }

            statusMessage?.contains(TOKEN_IS_ALREADY_IN_DELETED_STATE) == true -> {
                activity.showLoading(true, getString(R.string.text_please_wait))
            }

            statusMessage?.contains(NO_PENDING_TASK, ignoreCase = true) == true ||
                (statusMessage == CommonResponse.SUCCESS.response && deleteScriptsList.isNullOrEmpty()) -> {
                lifecycleScope.launch {
                    activity.showLoading(false, "")
                    updateCardDeletionStatus(false)
                    clearSavedDefaultIfDeleted(tokenRefNumber)
                    StorageRepository.deleteLocalCardByDigitizeRef(requireContext(), tokenRefNumber)
                    CardListFragment.shouldForceApiRefresh = true
                }
            }

            else -> {
                activity.showLoading(true, getString(R.string.text_please_wait))
                if (showErrorDialog) {
                    deleteDialog(
                        activity,
                        getString(R.string.text_card_deleted_error)
                    )
                }
            }
        }
    }

    /**
     * Executes delete scripts sequentially (one [scriptData] blob per list item), with one BLE retry per script.
     *
     * @param scriptIndex Index into [deleteScriptsList] for the script currently being run.
     * @param attemptIndex 0 = first attempt for that script, 1 = single retry after failure.
     */
    private fun executeDeleteScriptWithRetry(
        deleteScriptsList: List<DeleteScriptBase>?,
        tokenRefNumber: String,
        activity: MainActivity,
        scriptIndex: Int,
        attemptIndex: Int
    ) {
        if (!isAdded) return
        val fragmentContext = context ?: return
        val mainActivity = activity

        if (deleteScriptsList.isNullOrEmpty()) {
            handleDeleteSuccess(mainActivity, tokenRefNumber)
            return
        }

        if (scriptIndex >= deleteScriptsList.size) {
            handleDeleteSuccess(mainActivity, tokenRefNumber)
            return
        }

        val totalScripts = deleteScriptsList.size
        val jsonBytes = extractJsonBytes(deleteScriptsList, scriptIndex)
        if (jsonBytes == null || jsonBytes.isEmpty()) {
            logger.debug(
                "delete card: scriptData missing or invalid (script ${scriptIndex + 1}/$totalScripts)"
            )
            handleRetryOrFailure(
                deleteScriptsList,
                tokenRefNumber,
                mainActivity,
                scriptIndex,
                attemptIndex
            )
            return
        }

        StorageRepository.saveString(PreferenceKey.INSTALL_SCRIPT, jsonBytes.contentToString())
        val scriptHandler = createScriptHandler(fragmentContext)
        val clearDefaultCard = shouldClearDefaultOnDelete(tokenRefNumber)

        scriptHandler.deleteScript(jsonBytes, clearDefaultCard = clearDefaultCard).thenAccept { success ->
            if (!isAdded) return@thenAccept

            logger.debug(
                "delete card: Success=$success, clearDefaultCard=$clearDefaultCard, " +
                    "BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected} " +
                    "(script ${scriptIndex + 1}/$totalScripts, attempt ${attemptIndex + 1})"
            )

            try {
                processBleDeleteScriptThenAccept(
                    success,
                    deleteScriptsList,
                    scriptIndex,
                    attemptIndex,
                    tokenRefNumber,
                    mainActivity
                )
            } catch (e: Exception) {
                reportBleDeleteScriptPostProcessingFailure(e, mainActivity, totalScripts, scriptIndex, attemptIndex)
            }
        }.exceptionally { throwable ->
            if (!isAdded) return@exceptionally null
            logger.debug(
                "delete card: Exception during script execution=${throwable.message}, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected} " +
                    "(script ${scriptIndex + 1}/$totalScripts, attempt ${attemptIndex + 1})"
            )

            handleRetryOrFailure(
                deleteScriptsList,
                tokenRefNumber,
                mainActivity,
                scriptIndex,
                attemptIndex
            )
            null
        }
    }

    private fun processBleDeleteScriptThenAccept(
        success: Boolean,
        deleteScriptsList: List<DeleteScriptBase>,
        scriptIndex: Int,
        attemptIndex: Int,
        tokenRefNumber: String,
        mainActivity: MainActivity
    ) {
        if (success) {
            StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)
            val current = deleteScriptsList[scriptIndex]
            acknowledgeSingleDeleteScript(current, mainActivity, seId.toString(), tokenRefNumber)
            val nextIndex = scriptIndex + 1
            if (nextIndex < deleteScriptsList.size) {
                executeDeleteScriptWithRetry(
                    deleteScriptsList,
                    tokenRefNumber,
                    mainActivity,
                    scriptIndex = nextIndex,
                    attemptIndex = 0
                )
            } else {
                handleDeleteSuccess(mainActivity, tokenRefNumber)
            }
            return
        }
        handleRetryOrFailure(
            deleteScriptsList,
            tokenRefNumber,
            mainActivity,
            scriptIndex,
            attemptIndex
        )
    }

    private fun reportBleDeleteScriptPostProcessingFailure(
        e: Exception,
        mainActivity: MainActivity,
        totalScripts: Int,
        scriptIndex: Int,
        attemptIndex: Int
    ) {
        logger.debug(
            "delete card: Post-processing exception=${e.message}, BluetoothStateManager.isConnected=${BluetoothStateManager.isConnected} " +
                "(script ${scriptIndex + 1}/$totalScripts, attempt ${attemptIndex + 1})"
        )
        mainActivity.runOnUiThread {
            mainActivity.showLoading(false, "")
            deleteDialog(
                mainActivity,
                INTERNAL_ERROR_CARD_DELETED
            )
        }
    }

    /**
     * NFC variant of [executeDeleteScriptWithRetry]: runs each script in [deleteScriptsList] in order.
     */
    private fun executeDeleteScriptWithRetryNFC(
        deleteScriptsList: List<DeleteScriptBase>?,
        tokenRefNumber: String,
        activity: MainActivity,
        scriptIndex: Int,
        attemptIndex: Int
    ) {
        if (!isAdded) return

        val mainActivity = activity
        if (deleteScriptsList.isNullOrEmpty()) {
            handleDeleteSuccess(mainActivity, tokenRefNumber)
            return
        }

        if (scriptIndex >= deleteScriptsList.size) {
            handleDeleteSuccess(mainActivity, tokenRefNumber)
            return
        }

        val totalScripts = deleteScriptsList.size
        val jsonBytes = extractJsonBytes(deleteScriptsList, scriptIndex)
        if (jsonBytes == null || jsonBytes.isEmpty()) {
            logger.debug(
                "delete card (NFC): scriptData missing or invalid (script ${scriptIndex + 1}/$totalScripts)"
            )
            handleRetryOrFailure(
                deleteScriptsList,
                tokenRefNumber,
                mainActivity,
                scriptIndex,
                attemptIndex
            )
            return
        }

        StorageRepository.saveString(PreferenceKey.INSTALL_SCRIPT, jsonBytes.contentToString())

        showNfcSheet(parentFragmentManager, onCancelClick = {
            activity.showLoading(false, "")
            findNavController().popBackStack()
        })
        NfcScriptExecutionTracker.onNfcScriptStarted()
        val nfcDeleteOperation =
            if (shouldClearDefaultOnDelete(tokenRefNumber)) {
                DELETE_SCRIPT_CLEAR_DEFAULT
            } else {
                DELETE_SCRIPT
            }
        SecoraWearableSDK.getInstance().getInterface().executeNfcOperation(
            requireActivity(),
            nfcDeleteOperation,
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
                }

                override fun onSuccess(
                    responseItems: MutableList<ApduResponsesItem>?,
                    completed: Boolean
                ) {
                    processNfcDeleteScriptOnSuccess(
                        completed,
                        deleteScriptsList,
                        scriptIndex,
                        tokenRefNumber,
                        mainActivity
                    )
                }

                override fun onError(error: String) {
                    NfcScriptExecutionTracker.onNfcScriptFinished()
                    if (!isAdded) return
                    handleRetryOrFailure(
                        deleteScriptsList,
                        tokenRefNumber,
                        mainActivity,
                        scriptIndex,
                        attemptIndex
                    )
                }
            })
    }

    /**
     * Handles NFC delete-script [ScriptExecutionCallback.onSuccess]: ignores progress callbacks,
     * then advances to the next script or finishes deletion.
     */
    private fun processNfcDeleteScriptOnSuccess(
        completed: Boolean,
        deleteScriptsList: List<DeleteScriptBase>,
        scriptIndex: Int,
        tokenRefNumber: String,
        mainActivity: MainActivity
    ) {
        if (!completed) {
            logger.debug("Delete NFC script progress callback received, waiting for completion")
            return
        }
        NfcScriptExecutionTracker.onNfcScriptFinished()
        if (!isAdded) return

        hideNfcSheet()
        StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)
        val current = deleteScriptsList[scriptIndex]

        acknowledgeSingleDeleteScript(current, mainActivity, seId.toString(), tokenRefNumber)
        val nextIndex = scriptIndex + 1
        if (nextIndex < deleteScriptsList.size) {
            executeDeleteScriptWithRetryNFC(
                deleteScriptsList,
                tokenRefNumber,
                mainActivity,
                scriptIndex = nextIndex,
                attemptIndex = 0
            )
        } else {
            handleDeleteSuccess(mainActivity, tokenRefNumber)
        }
    }

    /**
     * Creates and returns a ScriptHandler instance with required callbacks.
     *
     * Handles toast display and log updates while delegating loading UI
     * handling to the main activity.
     *
     * @param context Context used to initialize the ScriptHandler.
     * @return Configured ScriptHandler instance.
     */
    private fun createScriptHandler(context: Context): ScriptHandler {
        return ScriptHandler(
            context,
            object : ScriptHandler.Callbacks {
                override fun showLoading(show: Boolean, msg: String) {
                    // Loading is handled by the main activity, no action needed here
                }

                override fun showToast(message: String) {
                    if (!isAdded) return
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

                override fun updateLogs(message: String) {
                    // Updating log into main activity, no action needed here
                    logger.debug("ScriptHandler$message")
                }
            }
        )
    }

    /**
     * Handles successful card deletion response.
     *
     * - Stops loading indicator.
     * - Shows success dialog.
     * - Acknowledges delete script success.
     * - Fetches pending tasks with SUCCESS status.
     *
     * @param mainActivity Host activity used for UI operations.
     */
    private fun handleDeleteSuccess(
        mainActivity: MainActivity,
        tokenRefNumber: String
    ) {
        if (!isAdded) return

        lifecycleScope.launch {
            clearSavedDefaultIfDeleted(tokenRefNumber)
            mainActivity.showLoading(true, "")
            StorageRepository.removeNicknameForCard(requireContext(), paymentId.toString(), panSuffix)

            deleteDialog(
                mainActivity,
                mainActivity.getString(R.string.text_card_deleted_successfully)
            )

            logger.debug("seId---delete2->${seId}")

            getPendingTask(
                mainActivity,
                seId.toString(),
                tokenRefNumber
            )
        }
    }

    /**
     * Handles retry logic and final failure for delete card operation.
     *
     * - Retries delete script execution once on first failure.
     * - Clears stored install script on final failure.
     * - Displays failure dialog and updates pending task status.
     *
     * Response received from delete attempt.
     * @param mainActivity Host activity for UI interactions.
     */
    private fun handleRetryOrFailure(
        deleteScriptsList: List<DeleteScriptBase>?,
        tokenRefNumber: String,
        mainActivity: MainActivity,
        scriptIndex: Int,
        attemptIndex: Int
    ) {
        if (!isAdded) return

        if (attemptIndex == 0) {
            logger.debug("Delete script attempt failed, retrying same script once...")
            if (!isNFC())
                executeDeleteScriptWithRetry(
                    deleteScriptsList,
                    tokenRefNumber,
                    mainActivity,
                    scriptIndex = scriptIndex,
                    attemptIndex = 1
                )
            else
                executeDeleteScriptWithRetryNFC(
                    deleteScriptsList,
                    tokenRefNumber,
                    mainActivity,
                    scriptIndex = scriptIndex,
                    attemptIndex = 1
                )
            return
        }

        StorageRepository.clearString(key = PreferenceKey.INSTALL_SCRIPT)
        logger.debug("delete card else: Final failure after retry")

        deleteDialog(
            mainActivity,
            UNABLE_TO_DELETE
        )
        getPendingTask(
            mainActivity,
            seId.toString(),
            tokenRefNumber
        )
    }

    /**
     * Extracts and decodes delete-script payload bytes (Base64, possibly nested) for the wearable SDK.
     * Uses [ScriptDataParser] so JSON roots that are arrays (e.g. `[{"apdu":...}]`) are handled like other fragments.
     *
     * @param scriptIndex Index into [deleteScriptsList] for multi-step delete flows from the backend.
     */
    private fun extractJsonBytes(deleteScriptsList: List<DeleteScriptBase>?, scriptIndex: Int): ByteArray? {
        return try {
            val scriptData = deleteScriptsList?.getOrNull(scriptIndex)?.scriptData
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
     * - On success, processes any `deleteScriptList` from response.
     * - If tasks exist, invokes [acknowledgePendingTask] with the first scriptId and digitizeRef.
     * - On error or empty response, stops the loading and optionally shows a message.
     */
    private fun getPendingTask(
        activity: MainActivity,
        seId: String,
        digitizeRef: String
    ) {
        activity.showLoading(true, getString(R.string.text_please_wait))
        logger.debug("getPendingTask seid--->${seId}")

        fetchSequenceNumberFromDevice(
            onRetrieved = { currentSequenceCounter ->
                logger.debug("currentSequenceCounter : $currentSequenceCounter")
                handleGetPendingTaskFlow(activity, seId, digitizeRef, currentSequenceCounter)
            },
            onFailed = {

            })

    }

    private fun handleGetPendingTaskFlow(
        activity: MainActivity,
        seId: String,
        digitizeRef: String,
        currentSequenceCounter: String
    ) {

        lifecycleScope.launch {
            if (activity.isFinishing) return@launch
            // Use [activity] for Context, not [requireContext]: after [getPendingTask] suspends and
            // resumes, or when this runs from a posted BLE callback, the fragment may already be detached.
            val sdkResult = WalletRepository.getPendingTask(
                context = activity,
                seId = seId,
                digitizationReferenceNumber = digitizeRef,
                currentSequenceCounter = currentSequenceCounter
            )

            if (sdkResult.isSuccess) {
                if (!this@CardSettingFragment.isAdded) return@launch
                sdkResult.response?.let { getPendingResponse ->
                    handleGetPendingTaskResponse(getPendingResponse, activity, seId)
                }
            } else {
                activity.showLoading(false, "")
                handleSessionOrError(sdkResult.errorMessage)
            }
        }
    }

    /**
     * Processes the [GetPendingResponse] and decides further actions based on the status message.
     *
     * @param response The pending task response.
     * @param activity The [MainActivity] instance.
     * @param seId The Secure Element ID.
     */
    private fun handleGetPendingTaskResponse(
        response: GetPendingResponse,
        activity: MainActivity,
        seId: String
    ) {
        if (!isAdded) return
        when (response.statusMessage) {
            CommonResponse.SUCCESS.response -> {
                handleSuccessResponse(response, activity, seId)
            }

            else -> {
                activity.showLoading(false, "")
                logger.info("Status message: ${response.statusMessage}")
            }
        }
    }

    /**
     * Handles successful pending task retrieval by extracting script details
     * and acknowledging the pending task.
     *
     * @param response The [GetPendingResponse] containing script information.
     * @param activity The [MainActivity] instance.
     * @param seId The Secure Element ID.
     */
    private fun handleSuccessResponse(
        response: GetPendingResponse,
        activity: MainActivity,
        seId: String
    ) {
        if (!isAdded) return
        if (response.deleteScriptList.isNotEmpty()) {
            val scriptId = response.deleteScriptList[0].scriptId
            val digitizeRef = response.deleteScriptList[0].digitizationReferenceNumber
            activity.let { act ->
                scriptId?.let { it1 ->
                    acknowledgePendingTask(
                        act,
                        seId,
                        it1,
                        digitizeRef.toString()
                    )
                }
            }
        } else {
            activity.showLoading(false, "")
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
        activity: MainActivity,
        seId: String,
        scriptId: Int,
        digitizeRef: String
    ) {
        if (!isAdded) return

        lifecycleScope.launch {
            if (activity.isFinishing) return@launch
            activity.showLoading(true, getString(R.string.scanning))
            val sdkResult = WalletRepository.acknowledgePendingTask(
                context = activity,
                seId = seId,
                scriptId = scriptId,
                digitizeRef = digitizeRef
            )
            dispatchAcknowledgePendingTaskResult(activity, sdkResult)
        }
    }

    private fun dispatchAcknowledgePendingTaskResult(
        activity: MainActivity,
        sdkResult: WalletSdkResult<AcknowledgeResponse>
    ) {
        if (sdkResult.isSuccess) {
            if (!isAdded) return
            activity.showLoading(false, "")

            sdkResult.response?.let { acknowledgeResponse ->
                activity.runOnUiThread {
                    if (isAdded) handleAcknowledgeResponse(acknowledgeResponse)
                }
            }
            return
        }
        if (isAdded) activity.showLoading(false, "")
        handleSessionOrError(sdkResult.errorMessage)
    }

    /**
     * Handles the response received after acknowledging a pending task or card update action.
     *
     * @param response The [AcknowledgeResponse] object containing the server response details.
     */
    private fun handleAcknowledgeResponse(response: AcknowledgeResponse) {
        when (response.statusMessage) {
            CommonResponse.SUCCESS.response -> {
                // Success case - no action needed
            }

            else -> {
                logger.info("Status message: ${response.statusMessage}")
            }
        }
    }

    /**
     * update nickname dialog
     * show dialog to update card nickname
     *
     */
    private suspend fun updateNicknameDialog() {
        isNicknameDialogOpen = true
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.view_wallet_add_card_nickname, null)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.setPadding(15, 35, 15, 35)

        val msg = dialogView.findViewById<TextView>(R.id.tv_card_name_msg)
        val updateButton = dialogView.findViewById<Button>(R.id.btn_add_name)
        val btnCancel = dialogView.findViewById<Button>(R.id.add_card_btn_cancel)
        val etCardHolderName = dialogView.findViewById<EditText>(R.id.et_card_holder_name)
        val paymentAppInstanceId = StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID)
        val currentNickname =
            StorageRepository.getNicknameForCard(paymentAppInstanceId, panSuffix, requireContext()).orEmpty()
        etCardHolderName.setText(currentNickname)
        etCardHolderName.setSelection(etCardHolderName.text.length)

        msg.text = requireContext().getString(R.string.update_nickname_msg)
        updateButton.text = requireContext().getString(R.string.btn_update)

        btnCancel.visibility = View.VISIBLE
        updateButton.setOnClickListener {
            if (!isNetworkAvailable(requireContext())) {
                confirmDataDialog(getString(R.string.data_enable))
                return@setOnClickListener
            }

            val nickName = etCardHolderName.text.toString().trim()
            val paymentAppInstanceId = StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID)

            lifecycleScope.launch {

                if (nickName.isNotEmpty() && isNicknameDuplicate(
                        nickName,
                        paymentAppInstanceId,
                        panSuffix
                    )
                ) {
                    showAlertDialog(
                        getString(R.string.text_secora_wallet),
                        getString(R.string.nickname_already_assigned)
                    )
                    return@launch
                }

                // Save nickname if not duplicate
                StorageRepository.saveNicknameForCard(
                    requireContext(),
                    paymentAppInstanceId,
                    panSuffix,
                    nickName
                )
                StorageRepository.updateLocalCardNicknameByDpan(
                    requireContext(),
                    paymentAppInstanceId,
                    panSuffix,
                    nickName
                )

                StorageRepository.readString(PreferenceKey.DIGITIZATION_REFERENCE_NUMBER).trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { dRef ->
                        StorageRepository.setLocalUserCardNickname(requireContext(), dRef, nickName)
                    }

                setFragmentResult(Constants.REQUEST_KEY, Bundle().apply {
                    putString(BundleKey.NICK_NAME, nickName)
                })
                alertDialog.dismiss()
            }
        }

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.setOnDismissListener {
            isNicknameDialogOpen = false
        }

        etCardHolderName.addTextChangedListener(object : TextWatcher {
            var previousText: String = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                previousText = s.toString()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed during text change, validation is handled in afterTextChanged
            }

            override fun afterTextChanged(s: Editable?) {
                validateNicknameInput(etCardHolderName, s, this)
            }
        })
        etCardHolderName.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        alertDialog.showSecure()
    }

    /**
     * Validates the nickname entered in the EditText and allows only letters, numbers,
     * spaces, underscores, and hyphens. If invalid characters are detected, they are
     * filtered out and an error message is shown to the user.
     */
    private fun validateNicknameInput(
        etCardHolderName: EditText,
        s: Editable?,
        watcher: TextWatcher
    ) {
        s?.let {
            val nickname = it.toString()
            val allowedPattern = Regex("^[a-zA-Z0-9 _-]*$")

            if (!allowedPattern.matches(nickname)) {
                val filtered = nickname.filter { ch ->
                    ch.isLetterOrDigit() || ch == '_' || ch == '-' || ch == ' '
                }

                if (filtered != nickname) {
                    etCardHolderName.removeTextChangedListener(watcher)
                    etCardHolderName.setText(filtered)
                    etCardHolderName.setSelection(filtered.length)
                    etCardHolderName.addTextChangedListener(watcher)

                    statusDialog(requireActivity(), getString(R.string.invalid_nickname))
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
    fun deleteDialog(context: Context, message: String?) {
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
            updateCardDeletionStatus(false)
            btnPasswordInfoOk.setOnClickListener {
                alertDialog.dismiss()
                if (context is FragmentActivity) {
                    val navController = (context.supportFragmentManager
                        .primaryNavigationFragment
                        ?.findNavController())
                    navController?.popBackStack()
                }
            }
            alertDialog.showSecure()
        }
    }

    /**
     * Shows a simple status update dialog with a custom message.
     *
     * @param message The status message to display.
     */
    private fun showStatusUpdatedDialog(message: String?) {
        if (!isAdded) return
        (activity as? Activity)?.runOnUiThread {
            (activity as? MainActivity)?.showLoading(false, "")

            val dialogView =
                LayoutInflater.from(requireContext()).inflate(R.layout.dialog_common_message, null)
            val alertDialog =
                AlertDialog.Builder(requireContext()).setView(dialogView).create()

            val txtTitle = dialogView.findViewById<TextView>(R.id.txtTitle)
            val txtMessage = dialogView.findViewById<TextView>(R.id.txtMessage)
            val txtOK = dialogView.findViewById<TextView>(R.id.txtOK)
            val txtCancel = dialogView.findViewById<TextView>(R.id.txtCancel)

            txtTitle.text = getString(R.string.text_secora_wallet)
            txtMessage.text = message

            txtOK.text = getString(R.string.ok)
            txtCancel.visibility = View.GONE

            txtOK.setOnClickListener {
                alertDialog.dismiss()
            }

            alertDialog.showSecure()
        }
    }

    /**
     * Checks whether a given nickname is already associated with the provided payment instance.
     *
     * @param nickname The new nickname entered by the user.
     * @param paymentAppInstanceId The unique payment instance ID to check against.
     * @return `true` if the nickname already exists for the given payment instance, otherwise `false`.
     */
    private suspend fun isNicknameDuplicate(
        nickname: String,
        paymentAppInstanceId: String,
        cardId: String
    ): Boolean {
        return StorageRepository.isNicknameDuplicate(
            context = requireContext(),
            nickname = nickname,
            paymentAppInstanceId = paymentAppInstanceId,
            cardId = cardId
        )
    }

    /**
     * Displays a general-purpose alert dialog with a title and message.
     *
     * @param title The title of the dialog.
     * @param message The content message to display inside the dialog.
     */
    private fun showAlertDialog(title: String, message: String) {
        (activity as? Activity)?.runOnUiThread {
            val alertDialog = Dialog(requireContext())
            alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            alertDialog.setContentView(R.layout.dialog_common_message)
            alertDialog.setCancelable(false)

            val btnOk = alertDialog.findViewById<TextView>(R.id.txtOK)
            val tvTitle = alertDialog.findViewById<TextView>(R.id.txtTitle)
            val txtMessage = alertDialog.findViewById<TextView>(R.id.txtMessage)
            val txtCancel = alertDialog.findViewById<TextView>(R.id.txtCancel)

            tvTitle.text = title
            txtMessage.text = message
            txtCancel.visibility = View.GONE

            btnOk.setOnClickListener {
                alertDialog.dismiss()
            }
            alertDialog.showSecure()
        }
    }

    /**
     * Notifies the backend after one delete script from the list has completed on-device.
     * Uses [tokenRefNumber] when the script entry has no digitization reference (e.g. ISD-only step).
     */
    private fun acknowledgeSingleDeleteScript(
        script: DeleteScriptBase,
        activity: MainActivity,
        seId: String,
        tokenRefNumber: String
    ) {
        try {
            script.scriptId?.let { scriptId ->
                val digitizeRef = script.digitizationReferenceNumber
                    ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    ?: tokenRefNumber
                acknowledgePendingTask(activity, seId, scriptId, digitizeRef)
            }
        } catch (e: Exception) {
            logger.debug("Error acknowledging delete script success: ${e.message}")
        }
    }
}
