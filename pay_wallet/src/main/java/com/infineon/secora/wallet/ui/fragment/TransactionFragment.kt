// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: TransactionFragment.kt displays the user's transaction history and manages card-related actions
 * such as deletion and synchronization with the backend and secure element.
 **/
package com.infineon.secora.wallet.ui.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.adapter.DeviceDetailTransactionAdapter
import com.infineon.secora.wallet.client.data.models.DeleteScriptBase
import com.infineon.secora.wallet.client.data.models.GetPendingResponse
import com.infineon.secora.wallet.client.data.models.common.TokenTransactionResponse
import com.infineon.secora.wallet.client.data.models.common.TransactionDetails
import com.infineon.secora.wallet.client.data.models.common.TransactionHistoryResponse
import com.infineon.secora.wallet.client.util.AppDispatchers
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.DialogCommonMessageBinding
import com.infineon.secora.wallet.databinding.FragmentWalletTransactionBinding
import com.infineon.secora.wallet.utils.helper.showSecure
import com.infineon.secora.wallet.domain.walletsdk.WalletRepository
import com.infineon.secora.wallet.domain.wearable.ble.script.ScriptHandler
import com.infineon.secora.wallet.firebase.AppEvent
import com.infineon.secora.wallet.firebase.EventBus
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.models.TransactionItem
import com.infineon.secora.wallet.models.TransactionListItem
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.utils.CommonResponse
import com.infineon.secora.wallet.utils.helper.PendingDeleteTaskResponseHelper
import com.infineon.secora.wallet.utils.helper.ScriptDataParser
import com.infineon.secora.wallet.utils.helper.TransactionDataFormatter
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_FORCE_REFRESH_TXN
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_TOGGLE
import com.infineon.secora.wallet.utils.constants.Constants.DELETED_CARD
import com.infineon.secora.wallet.utils.constants.Constants.PNO_VTS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionFragment(val pnoType: String) : BaseFragment() {

    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)
    private lateinit var binding: FragmentWalletTransactionBinding
    private lateinit var activityRef: MainActivity
    private var seId: String? = null
    private var paymentAppInstanceId: String? = null
    private var transactionAdapter: DeviceDetailTransactionAdapter? = null
    private var isCardDeleted = false
    private var isFetchingTransactions = false

    /**
     * Handles refresh events received from EventBus.
     */
    private suspend fun handleRefreshEvent(event: AppEvent) {
        try {
            if (event.action == ACTION_FORCE_REFRESH_TXN && !isCardDeleted) {
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(200)
                    if (!isFetchingTransactions) {
                        getTransactionHistory(pnoType)
                    }
                }
                return
            }
            val msgType = event.getStringExtra(BundleKey.MSG_TYPE)
            if (msgType == "null") {
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
                    if (isFragmentVisibleToUser() && !isFetchingTransactions) {
                        getTransactionHistory(pnoType)
                    }
                }
            } else if (msgType == DELETED_CARD) {
                // Handled centrally by MainActivity → FcmDeletedCardHandler (all screens).
            }
        } catch (e: Exception) {
            logger.debug("Event handling exception: ${e.message}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletTransactionBinding.inflate(inflater, container, false)
        activityRef = requireActivity() as MainActivity

        transactionAdapter = DeviceDetailTransactionAdapter()

        binding.rvTransactionList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            if (!isFetchingTransactions) {
                getTransactionHistory(pnoType)
            }
        }

        seId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)
        paymentAppInstanceId = StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID)

        return binding.root
    }

    /**
     * Called after the Fragment's view is created.
     *
     * Starts collecting EventBus events using the viewLifecycleOwner to ensure the
     * collection is tied to the View lifecycle (not the Fragment lifecycle).
     * repeatOnLifecycle(STARTED) automatically handles start/stop/restart during
     * configuration changes, backstack navigation, and view recreation without
     * requiring manual Job management in onResume/onPause.
     *
     * Also triggers an initial transaction history load when the view is ready.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                EventBus.events.collect { event ->
                    if (event.action == ACTION_TOGGLE || event.action == ACTION_FORCE_REFRESH_TXN) {
                        handleRefreshEvent(event)
                    }
                }
            }
        }

    }

    /**
     * Called when the Fragment resumes.
     * Triggers fetching of transaction history to ensure UI is updated.
     */
    override fun onResume() {
        super.onResume()
        if (!isFetchingTransactions) {
            getTransactionHistory(pnoType)
        }
    }

    private fun isFragmentVisibleToUser(): Boolean {
        return isAdded && view != null && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    /**
     * Fetches transaction history for the currently digitized card.
     * Runs in IO thread, shows loader, and triggers SDK fetch.
     */
    private fun getTransactionHistory(pnoType: String) {
        viewLifecycleOwner.lifecycleScope.launch(AppDispatchers.IO) {
            if (isLoginOlderThanSessionExpiryDuration()) {
                navigateToLoginScreen()
                return@launch
            }
            isFetchingTransactions = true
            showLoader()

            val paymentId = StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID)
            val digitizationRef = StorageRepository.readString(PreferenceKey.DIGITIZATION_REFERENCE_NUMBER)
            val seId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)

            logger.debug("digitizationReferenceNumber: $digitizationRef")
            logger.debug("seID: $seId")

            val sdkResult = WalletRepository.fetchTransactionHistory(
                context = activityRef.applicationContext,
                paymentAppInstanceId = paymentId,
                digitizationReferenceNumber = digitizationRef, pnoType
            )
            isFetchingTransactions = false
            if (!isFragmentValid()) return@launch

            viewLifecycleOwner.lifecycleScope.launch(AppDispatchers.MAIN) {

                if (sdkResult.isSuccess) {
                    if (!isFragmentVisibleToUser()) return@launch
                    hideLoader()
                    sdkResult.response?.let { transactionHistoryResponse ->
                        handleSuccessResponse(
                            transactionHistoryResponse,
                            digitizationRef
                        )
                    }
                } else {
                    hideLoader()
                    showEmptyState()
                    handleSessionOrError(sdkResult.errorMessage)
                }
            }
        }
    }

    /**
     * Processes the SDK response and updates the UI accordingly.
     *
     * @param response         The transaction history response from SDK.
     * @param digitizationRef  The digitization reference number to validate transactions.
     */
    private suspend fun handleSuccessResponse(
        response: TransactionHistoryResponse, digitizationRef: String?
    ) {
        val isMatched = checkDigitizationReferenceNumberMatched(response, digitizationRef)
        if (!isMatched) {
            showEmptyState()
            return
        }

        val transactions = extractTransactions(response, digitizationRef)
        if (transactions.isEmpty()) {
            showEmptyState()
        } else {
            showTransactionList(transactions)
        }
    }

    /**
     * Checks if fragment is currently added and its view exists.
     *
     * @return True if fragment is valid; False otherwise.
     */
    private fun isFragmentValid() = isAdded && view != null


    /**
     * Extracts and filters transactions based on card type (Visa/VTS vs MDES).
     *
     * @param response         The transaction history response from SDK.
     * @param digitizationRef  The digitization reference number to filter transactions.
     * @return A grouped list of transactions by date.
     */
    private fun extractTransactions(
        response: TransactionHistoryResponse, digitizationRef: String?
    ): List<TransactionListItem> {

        val filteredList = if (isVisaTransactionHistory()) {
            response.transactionDetails.filter { it.digitizationReferenceNumber == digitizationRef }
                .let { convertTransactionDetailsToTransactionItem(it) }
        } else {
            response.tokenTransactions.filter { it.digitizationReferenceNumber == digitizationRef }
                .let { convertTokenTransactionResponseToTransactionItem(it) }
        }

        return groupTransactionsByDate(filteredList)
    }

    /**
     * Displays the grouped transaction list in RecyclerView.
     *
     * @param groupedList List of grouped transaction items.
     */
    private fun showTransactionList(groupedList: List<TransactionListItem>) {
        binding.rvTransactionList.post {
            transactionAdapter?.submitList(groupedList)
            binding.rvTransactionList.visibility = View.VISIBLE
            binding.txtNoHistory.visibility = View.GONE
        }
    }

    /**
     * Shows or hides the loader from the activity.
     */
    private suspend fun showLoader() {
        withContext(AppDispatchers.MAIN) {
            activityRef.showLoading(true, getString(R.string.text_please_wait))
        }
    }

    /**
     * Safely hides the loader and resets swipe-to-refresh state.
     */
    private suspend fun hideLoader() {
        withContext(AppDispatchers.MAIN) {
            try {
                if (isCardDeleted) {
                    activityRef.showLoading(true, getString(R.string.text_please_wait))
                    isCardDeleted = false
                } else {
                    activityRef.showLoading(false, "")
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            } catch (e: Exception) {
                logger.debug("Error hiding loader: ${e.message}")
            }
        }
    }

    /**
     * Checking whether the provided digitizationReferenceNumber exists in the transaction history.
     *
     * For Visa/VTS, the match is checked inside `transactionDetails`. For MDES, the match is checked
     * inside `tokenTransactions`.
     *
     * @param response The transaction history response containing card-specific data.
     * @param digitizationReferenceNumber The reference number to match.
     * @return True if the reference number is found, otherwise false.
     */
    private fun checkDigitizationReferenceNumberMatched(
        response: TransactionHistoryResponse,
        digitizationReferenceNumber: String?
    ): Boolean {
        return if (isVisaTransactionHistory()) {
            response.transactionDetails.any { it.digitizationReferenceNumber == digitizationReferenceNumber }
        } else {
            response.tokenTransactions.any { it.digitizationReferenceNumber == digitizationReferenceNumber }
        }
    }

    /**
     * Returns whether transaction history should be read from Visa/VTS [TransactionDetails].
     *
     * @return True when the current card uses the VTS payment network.
     */
    private fun isVisaTransactionHistory(): Boolean = pnoType == PNO_VTS

    /**
     * Displays the empty state when there are no transactions.
     * Hides the transaction RecyclerView and shows the "No History" text.
     */
    private fun showEmptyState() {
        binding.rvTransactionList.visibility = View.GONE
        binding.txtNoHistory.visibility = View.VISIBLE
    }

    /**
     * Groups a list of TransactionItem by transaction date.
     * Adds a header item for each date and orders transactions in descending timestamp.
     *
     * @param transactions List of transactions to group.
     * @return A list of TransactionListItem containing headers and transactions.
     */
    private fun groupTransactionsByDate(transactions: List<TransactionItem>): List<TransactionListItem> {
        return transactions.sortedByDescending { it.timeStamp }
            .groupBy { TransactionDataFormatter.convertDate(it.timeStamp) }.flatMap { (date, list) ->
                listOf(TransactionListItem.Header(date)) + list.map {
                    TransactionListItem.Transaction(
                        it
                    )
                }
            }
    }

    /**
     * Converts a list of TransactionDetails into TransactionItem objects.
     *
     * @param transactionDetails List of transaction details to convert.
     * @return List of TransactionItem objects.
     */
    private fun convertTransactionDetailsToTransactionItem(transactionDetails: List<TransactionDetails>): List<TransactionItem> {
        return transactionDetails.map { details ->
            TransactionItem(
                merchantName = details.merchantName.toString(),
                timeStamp = details.transactionDate.toString(),
                amount = details.amount.toString(),
                currencyCode = details.currencyCode.toString()
            )
        }
    }

    /**
     * Converts a list of TokenTransactionResponse into TransactionItem objects.
     *
     * @param transactionDetails List of TokenTransactionResponse to convert.
     * @return List of TransactionItem objects.
     */
    private fun convertTokenTransactionResponseToTransactionItem(transactionDetails: List<TokenTransactionResponse>): List<TransactionItem> {
        return transactionDetails.map { details ->
            TransactionItem(
                merchantName = details.merchantName.toString(),
                timeStamp = details.transactionTimeStamp.toString(),
                amount = details.amount.toString(),
                currencyCode = details.currencyCode.toString()
            )
        }
    }

    /** Deletes a card using SecoraWalletSDK and updates its status.
     *
     * @param activity MainActivity instance used for UI operations.
     * @param tokenRefNumber Card's token reference number.
     * @param paymentId Payment app instance ID.
     * @param pnoType PNO type (VTS/MDES).
     */
    fun deleteCard(
        activity: MainActivity,
        tokenRefNumber: String,
        paymentId: String,
        pnoType: String
    ) {
        activity.showLoading(true, activity.getString(R.string.text_please_wait))
        fetchSequenceNumberFromDevice(
            onRetrieved = { currentSequenceCounter ->
                logger.debug("currentSequenceCounter : $currentSequenceCounter")
                handleGetPendingTaskFlow(activity, tokenRefNumber, paymentId, pnoType, currentSequenceCounter)
            },
            onFailed = { })
    }

    private fun handleGetPendingTaskFlow(
        activity: MainActivity,
        tokenRefNumber: String,
        paymentId: String,
        pnoType: String,
        currentSequenceCounter: String
    ) {

        // Capture safe references
        val safeActivity = activity
        val safeContext = activity.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            val safeDigitizeRef =
                tokenRefNumber.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            logger.debug("TransactionFragment : Calling Pending Task :")

            val sdkResult = WalletRepository.getPendingTask(
                context = safeContext,
                seId = seId.toString(),
                digitizationReferenceNumber = safeDigitizeRef.toString(),
                currentSequenceCounter = currentSequenceCounter
            )
            if (sdkResult.isSuccess) {
                sdkResult.response?.let { getPendingResponse ->
                    handleSuccessPendingResponse(
                        getPendingResponse,
                        safeActivity,
                        tokenRefNumber,
                        paymentId,
                        pnoType
                    )
                }
            } else {
                safeActivity.showLoading(false, "")
                handleSessionOrError(sdkResult.errorMessage) {
                    statusDialog(safeActivity, sdkResult.errorMessage)
                }
            }
        }
    }

    /**
     * Handles the success response for a pending transaction request.
     *
     * This method verifies the [GetPendingResponse.statusMessage]. If the response indicates
     * a successful status, it retrieves the delete script list and triggers execution with
     * retry logic using [executeDeleteScriptWithRetry].
     *
     * If the response is not successful, it stops the loading indicator on the provided [activity].
     *
     * @param response The pending response containing status and delete script details.
     * @param activity The [MainActivity] instance used to update UI elements such as loading state.
     * @param tokenRefNumber The token reference number associated with the transaction.
     * @param paymentId The unique identifier for the payment.
     * @param pnoType The type of PNO (Payment Network Operator) associated with the transaction.
     */
    private fun handleSuccessPendingResponse(
        response: GetPendingResponse,
        activity: MainActivity,
        tokenRefNumber: String,
        paymentId: String,
        pnoType: String
    ) {
        logger.debug("TransactionFragment : Pending Task response.statusMessage : ${response.statusMessage}")
        if (response.statusMessage.equals(CommonResponse.SUCCESS.response)) {
            val deleteList = response.deleteScriptList
            logger.debug("TransactionFragment : Pending Task deleteList isNotEmpty : ${deleteList.isNotEmpty()}")
            logger.debug("TransactionFragment : Pending Task deleteList size : ${deleteList.size}")

            if (deleteList.isEmpty()) {
                activity.showLoading(false, "")
                CardListFragment.shouldForceApiRefresh = true
                return
            }

            // Execute scripts sequentially (one list entry at a time), one retry per script
            executeDeleteScriptWithRetry(
                response, activity, tokenRefNumber, paymentId, pnoType,
                scriptIndex = 0,
                attemptIndex = 0
            )
        } else if (PendingDeleteTaskResponseHelper.isNoPendingDeleteTask(response)) {
            activity.showLoading(false, "")
            CardListFragment.shouldForceApiRefresh = true
        } else {
            activity.showLoading(false, "")
        }
    }

    private data class TxnPendingDeleteFlowParams(
        val response: GetPendingResponse,
        val activity: MainActivity,
        val tokenRefNumber: String,
        val paymentId: String,
        val pnoType: String,
        val attemptIndex: Int,
    )

    /**
     * Executes delete scripts from [response] sequentially, with one BLE retry per script.
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
        val deleteList = response.deleteScriptList
        val total = deleteList.size
        if (deleteList.isEmpty() || scriptIndex >= total) {
            finishTxnPendingDeleteScriptsOnMain(activity)
            return
        }

        val jsonBytes = extractJsonBytes(response, scriptIndex)
        if (jsonBytes == null || jsonBytes.isEmpty()) {
            handleTxnInvalidDeleteScriptJson(
                activity, response, tokenRefNumber, paymentId, pnoType, scriptIndex, attemptIndex
            )
            return
        }

        val scriptHandler = ScriptHandler(requireContext(), object : ScriptHandler.Callbacks {
            override fun showLoading(show: Boolean, msg: String) {
                // TO DO
            }

            override fun showToast(message: String) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }

            override fun updateLogs(message: String) {
                logger.debug("ScriptHandler $message")
            }
        })

        scriptHandler.deleteScript(jsonBytes).thenAccept { success ->
            if (!isAdded) return@thenAccept
            processTxnDeleteScriptThenAcceptResult(
                success,
                deleteList,
                scriptIndex,
                total,
                TxnPendingDeleteFlowParams(
                    response,
                    activity,
                    tokenRefNumber,
                    paymentId,
                    pnoType,
                    attemptIndex
                )
            )
        }
    }

    private fun finishTxnPendingDeleteScriptsOnMain(activity: MainActivity) {
        activity.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            activity.showLoading(true, "")
            deleteDialog(
                activity,
                getString(R.string.text_card_deleted_successfully)
            )
        }
    }

    private fun handleTxnInvalidDeleteScriptJson(
        activity: MainActivity,
        response: GetPendingResponse,
        tokenRefNumber: String,
        paymentId: String,
        pnoType: String,
        scriptIndex: Int,
        attemptIndex: Int
    ) {
        activity.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            if (attemptIndex == 0) {
                executeDeleteScriptWithRetry(
                    response, activity, tokenRefNumber, paymentId, pnoType,
                    scriptIndex, 1
                )
                return@runOnUiThread
            }
            deleteDialog(
                activity,
                "Unable to delete please clear the memory."
            )
        }
    }

    private fun processTxnDeleteScriptThenAcceptResult(
        success: Boolean,
        deleteList: List<DeleteScriptBase>,
        scriptIndex: Int,
        total: Int,
        flow: TxnPendingDeleteFlowParams
    ) {
        val activity = flow.activity
        val response = flow.response
        val tokenRefNumber = flow.tokenRefNumber
        val paymentId = flow.paymentId
        val pnoType = flow.pnoType
        val attemptIndex = flow.attemptIndex

        activity.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            if (success) {
                acknowledgeSinglePendingDeleteScript(
                    deleteList[scriptIndex],
                    activity,
                    seId.toString(),
                    tokenRefNumber
                )
                val next = scriptIndex + 1
                if (next < total) {
                    executeDeleteScriptWithRetry(
                        response, activity, tokenRefNumber, paymentId, pnoType, next, 0
                    )
                    return@runOnUiThread
                }
                activity.showLoading(true, "")
                deleteDialog(
                    activity,
                    getString(R.string.text_card_deleted_successfully)
                )
                return@runOnUiThread
            }
            if (attemptIndex == 0) {
                executeDeleteScriptWithRetry(
                    response, activity, tokenRefNumber, paymentId, pnoType, scriptIndex, 1
                )
                return@runOnUiThread
            }
            deleteDialog(
                activity,
                "Unable to delete please clear the memory."
            )
        }
    }

    private fun acknowledgeSinglePendingDeleteScript(
        script: DeleteScriptBase,
        activity: MainActivity,
        seId: String,
        tokenRefNumber: String
    ) {
        val scriptId = script.scriptId ?: return
        val digitizeRef = script.digitizationReferenceNumber
            ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?: tokenRefNumber
        acknowledgePendingTask(activity, seId, scriptId, digitizeRef)
    }

    /**
     * Extracts and decodes the JSON bytes from a delete script response.
     *
     * Handles double Base64 decoding if required.
     *
     * @param response GetPendingResponse containing delete script.
     * @param scriptIndex Index into [GetPendingResponse.deleteScriptList].
     * @return Decoded JSON bytes or null if extraction fails.
     */
    private fun extractJsonBytes(response: GetPendingResponse, scriptIndex: Int): ByteArray? {
        return try {
            val scriptData = response.deleteScriptList.getOrNull(scriptIndex)?.scriptData ?: return null
            ScriptDataParser.decodeToJsonBytes(scriptData)
        } catch (e: Exception) {
            logger.noStackTraceLog("ExecuteJsonBytes ", e)
            null
        }
    }

    /**
     * Displays a custom delete confirmation dialog with a message.
     *
     * @param context Context for dialog creation.
     * @param message Message to display in the dialog.
     */
    fun deleteDialog(context: Context, message: String?) {
        (context as? Activity)?.runOnUiThread {
            val dialogViewBinding = DialogCommonMessageBinding.inflate(layoutInflater)
            val alertDialog = Dialog(context)
            alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            alertDialog.setContentView(dialogViewBinding.root)
            alertDialog.setCancelable(false)

            dialogViewBinding.txtMessage.text = message
            dialogViewBinding.txtCancel.visibility = View.GONE
            dialogViewBinding.txtOK.setOnClickListener {
                alertDialog.dismiss()
                if (message.equals(getString(R.string.text_card_deleted_successfully)) && (context is FragmentActivity)) {
                    val navController =
                        (context.supportFragmentManager.primaryNavigationFragment?.findNavController())
                    navController?.popBackStack()

                }
            }
            alertDialog.showSecure()
        }
    }

    /**
     * Sends acknowledgement for a pending task in SecoraWalletSDK.
     *
     * @param activity MainActivity for showing loading UI.
     * @param seId SE ID of the task.
     * @param scriptId ID of the executed script.
     * @param digitizeRef Digitization reference number.
     */
    private fun acknowledgePendingTask(
        activity: MainActivity, seId: String, scriptId: Int, digitizeRef: String
    ) {
        lifecycleScope.launch {
            if (activity.isFinishing) return@launch
            activity.showLoading(true, getString(R.string.scanning))
            val sdkResult = WalletRepository.acknowledgePendingTask(
                context = activity,
                seId = seId,
                scriptId = scriptId,
                digitizeRef = digitizeRef
            )
            activity.showLoading(false, "")
            if (!sdkResult.isSuccess) {
                handleSessionOrError(sdkResult.errorMessage)
            }
        }
    }
}
