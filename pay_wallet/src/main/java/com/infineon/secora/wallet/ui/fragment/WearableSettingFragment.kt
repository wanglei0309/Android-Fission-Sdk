// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

package com.infineon.secora.wallet.ui.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.cdcvm.CdcvmApi
import com.infineon.secora.wallet.cdcvm.WearableStatus
import com.infineon.secora.wallet.cdcvm.WearableStatusChips
import com.infineon.secora.wallet.cdcvm.WearableStatusMonitor
import com.infineon.secora.wallet.client.data.models.DeleteScriptBase
import com.infineon.secora.wallet.client.data.models.card.common.CardDetails
import com.infineon.secora.wallet.client.data.models.common.UpdateCardStatusResponse
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.DialogCommonMessageBinding
import com.infineon.secora.wallet.databinding.FragmentWearableSettingBinding
import com.infineon.secora.wallet.domain.walletsdk.WalletRepository
import com.infineon.secora.wallet.domain.walletsdk.WalletSdkResult
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.utils.CommonResponse
import com.infineon.secora.wallet.utils.Utils.isNetworkAvailable
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.constants.Constants.DELETED_STATUS
import com.infineon.secora.wallet.utils.constants.Constants.NO_PENDING_TASK
import com.infineon.secora.wallet.utils.constants.Constants.TOKEN_IS_ALREADY_IN_DELETED_STATE
import com.infineon.secora.wallet.utils.helper.showSecure
import com.infineon.secora.wearable.cdcvm.CvmState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Wearable configuration: the settings that belong to the device rather than to a card.
 *
 * Card settings stay with [DeviceDetailFragment]. This screen owns the cardholder verification
 * method — setting, changing and clearing the wearable payment passcode.
 *
 * The rows shown depend on the CVM state read from the secure element, so a device without a
 * passcode offers setup while a provisioned one offers change and clear. State is re-read in
 * [onResume] so returning from a passcode flow reflects what just happened.
 */
class WearableSettingFragment : BaseFragment() {

    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)
    private lateinit var binding: FragmentWearableSettingBinding
    private lateinit var activity: MainActivity

    /** Shared status pills (Bluetooth / on-body / payment). */
    private lateinit var statusChips: WearableStatusChips
    private var paymentAppInstanceId: String? = null
    private var seId: String? = null
    private var cardListDetail: List<CardDetails> = ArrayList()
    private lateinit var selectedCardForDelete: CardDetails
    private var currentPosition: Int = 0
    /** Shared poller driving the pills and the passcode rows from the live CVM state. */
    private val statusMonitor = WearableStatusMonitor(onUpdate = ::onWearableStatus)
    companion object {
        var isCardDeletionInProgress: Boolean = false
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWearableSettingBinding.inflate(inflater, container, false)
        activity = requireActivity() as MainActivity
        activity.binding.toolbar.toolbarTitle.text = getString(R.string.wearable_setting_title)
        statusChips = WearableStatusChips(binding.statusChips)
        paymentAppInstanceId = StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID)
        seId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)

        binding.deviceDetails.textDeviceName.text =
            StorageRepository.readString(PreferenceKey.DEVICE_NAME).ifBlank {
                getString(R.string.app_name)
            }

        binding.rowSetupPasscode.setOnClickListener {
            openPasscodeFlow(WearablePasscodeStep.SETUP)
        }

        getWearableImage(
            StorageRepository.readString(PreferenceKey.DEVICE_IMAGE)
        )

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.cardListFragment)
                }
            })

        return binding.root
    }

    /**
     *  getWearableImage(): method is used to get the wearable image and displayed using Glide library
     *
     * @param image
     */
    fun getWearableImage(image: String) {
        try {
            val imageBytes = Base64.decode(image, Base64.DEFAULT)
            Glide.with(requireContext()).load(imageBytes).into(binding.deviceDetails.cardImageView)
        } catch (e: IllegalArgumentException) {
            logger.noStackTraceLog("GetWearableImage ", e)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusMonitor.attach(viewLifecycleOwner, requireContext())
    }

    override fun onResume() {
        super.onResume()
        statusMonitor.requestPoll()
    }

    /**
     * Renders the shared status pills and updates which passcode rows are offered from each polled
     * [status]. A disconnected/NFC device shows the "not connected" status line and hides the pills.
     */
    private fun onWearableStatus(status: WearableStatus) {
        statusChips.setVisible(!status.isNfc)
        statusChips.render(status)
        if (status.isNfc) return
        if (!status.connected) {
            showStatus(getString(R.string.wearable_passcode_not_connected))
            setStatusChipsConnectEnabled(true)
            return
        }
        setStatusChipsConnectEnabled(false)
        applyState(status.cvmState)
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
     * Shows setup or change depending on whether a passcode exists, and reports the current
     * verification status.
     */
    private fun applyState(state: CvmState?) {
        if (state == null) {
            showStatus(getString(R.string.wearable_passcode_state_unknown))
            return
        }

        val provisioned = !state.needsPasscodeSetup()
        binding.rowSetupPasscode.visibility = View.GONE
        binding.rowChangePasscode.visibility = View.VISIBLE
        binding.rowClearDevice.visibility = View.VISIBLE

        if(state.needsPasscodeSetup()) {
            binding.rowChangePasscode.setOnClickListener {

                val dialogBinding = DialogCommonMessageBinding.inflate(layoutInflater)
                val dialog = Dialog(requireContext())
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setContentView(dialogBinding.root)
                dialog.setCancelable(true)
                dialogBinding.txtCancel.visibility = View.GONE

                dialogBinding.txtTitle.text = getString(R.string.dialog_title)
                dialogBinding.txtMessage.text = getString(R.string.wearable_clear_device_no_passcode_set_change_passcode)
                dialogBinding.txtOK.setOnClickListener {
                    dialog.dismiss()
                }
                dialog.showSecure()

            }

        } else {
            binding.rowChangePasscode.setOnClickListener {
                openPasscodeFlow(WearablePasscodeStep.CHANGE_CURRENT)
            }

        }

        if (state.isAuthenticated) {
            binding.rowClearDevice.setOnClickListener { confirmClearDevice() }
        } else {
            binding.rowClearDevice.setOnClickListener {

                val dialogBinding = DialogCommonMessageBinding.inflate(layoutInflater)
                val dialog = Dialog(requireContext())
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setContentView(dialogBinding.root)
                dialog.setCancelable(true)
                dialogBinding.txtCancel.visibility = View.GONE
                val okText = if (state.needsPasscodeSetup()) {
                    getString(R.string.ok)
                } else {
                    getString(R.string.unlock_payments)
                }

                val messageText = if (state.needsPasscodeSetup()) {
                    getString(R.string.wearable_clear_device_no_passcode_set)
                } else {
                    getString(R.string.wearable_clear_device_payment_locked)
                }
                dialogBinding.txtTitle.text = getString(R.string.dialog_title)
                dialogBinding.txtOK.text = okText
                dialogBinding.txtMessage.text = messageText
                dialogBinding.txtOK.setOnClickListener {
                    dialog.dismiss()
                    if (!state.needsPasscodeSetup()) {
                        findNavController().navigate(
                            R.id.wearablePasscodeFragment,
                            WearablePasscodeFragment.argsFor(WearablePasscodeStep.VERIFY, navigateToSettings = true))
                    }
                }
                dialog.showSecure()

            }
        }


//
//        binding.rowSetupPasscode.visibility = if (provisioned) View.GONE else View.VISIBLE
//        binding.rowChangePasscode.visibility = if (provisioned) View.VISIBLE else View.GONE
//        // Clearing only makes sense once there is something on the device to clear.
//        binding.rowClearDevice.visibility = if (provisioned) View.VISIBLE else View.GONE

        showStatus(
            when {
                !provisioned -> getString(R.string.wearable_passcode_status_not_set)
                state.isBlocked -> getString(R.string.wearable_passcode_status_blocked)
                state.isAuthenticated -> getString(R.string.wearable_passcode_status_verified)
                else -> getString(
                    R.string.wearable_passcode_status_needs_verification,
                    state.remainingRetries
                )
            }
        )
    }

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
     * Asks before clearing, because UC-09 removes the passcode and the cards and cannot be undone.
     */
    private fun confirmClearDevice() {
        if (CdcvmApi.activeDeviceId() == null) {
            showToast(getString(R.string.wearable_passcode_not_connected))
            return
        }

        val dialogBinding = DialogCommonMessageBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(false)

        dialogBinding.txtTitle.text = getString(R.string.dialog_title)
        dialogBinding.txtMessage.text = getString(R.string.wearable_clear_device_confirm)
        dialogBinding.txtOK.setOnClickListener {
            dialog.dismiss()
            fetchCards(requireContext())
        }
        dialogBinding.txtCancel.setOnClickListener { dialog.dismiss() }
        dialog.showSecure()
    }

    /**
     * Runs UC-09.
     *
     * No card-deletion handler is passed yet, so this clears the cardholder verification method
     * only. Wiring card deletion in is a separate step that needs the wallet backend calls.
     */
    private fun clearDevice() {
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

    private fun showStatus(message: String) {
        binding.tvPasscodeStatus.text = message
        binding.tvPasscodeStatus.visibility = View.INVISIBLE
    }

    private fun fetchCards(safeContext: Context) {

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
                            deleteCardFlow()
                        } else {
                            clearDevice()
                        }

                    } else {
                        activity.showLoading(false, "")
                        logger.debug("Payment Instance Id not available")
                    }
                }
            }
        )
    }

    private fun deleteCardFlow() {
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
                    deleteCardFlow()
                } else {
                    isCardDeletionInProgress = false
                    clearDevice()

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
                    deleteDialog(
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
            btnPasswordInfoOk.setOnClickListener {
                alertDialog.dismiss()
            }
            alertDialog.showSecure()
        }
    }

}
