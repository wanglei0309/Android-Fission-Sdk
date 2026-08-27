// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: BaseFragment.kt serves as a foundation class for all fragments in the app,
 * providing reusable utility functions, standardized dialogs, permission handling, and common SDK integrations.
 **/
package com.infineon.secora.wallet.ui.fragment

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.infineon.secora.wallet.BuildConfig
import com.infineon.secora.wallet.PayExternalLaunch
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.DialogCommonMessageBinding
import com.infineon.secora.wallet.domain.devicedetach.DeviceDetachTargetResolver
import com.infineon.secora.wallet.domain.walletsdk.WalletRepository
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.domain.wearable.ble.script.ScriptHandler
import com.infineon.secora.wallet.domain.wearable.ble.script.ScriptRunner
import com.infineon.secora.wallet.firebase.AppEvent
import com.infineon.secora.wallet.firebase.FirebaseManager
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.oidc.CognitoSignInFlowCoordinator
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.ui.sheet.NfcScanBottomSheet
import com.infineon.secora.wallet.utils.helper.showSecure
import com.infineon.secora.wallet.utils.CommonResponse
import com.infineon.secora.wallet.utils.StringUtils.isEmptyString
import com.infineon.secora.wallet.utils.Utils
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_DEVICE_STATUS_UPDATE
import com.infineon.secora.wallet.utils.constants.Constants.GIVEN_DEVICE_MODEL_IS_NOT_PRESENT_OR_DO_NOT_SUPPORT
import com.infineon.secora.wallet.utils.constants.Constants.NFC_DEVICE_MODEL
import com.infineon.secora.wallet.utils.constants.Constants.PNO_MDES
import com.infineon.secora.wallet.utils.constants.Constants.PNO_VTS
import com.infineon.secora.wallet.utils.constants.Constants.SCRIPT
import com.infineon.secora.wallet.utils.helper.FcmSecureFlowCoordinator
import com.infineon.secora.wallet.utils.helper.NfcScriptExecutionTracker
import com.infineon.secora.wallet.utils.helper.ConfiguredWalletIdentity
import com.infineon.secora.wallet.utils.helper.SecureElementScriptCoordinator
import com.infineon.secora.wearable.SecoraWearableSDK
import com.infineon.secora.wearable.apdu.ApduResponsesItem
import com.infineon.secora.wearable.nfc.ScriptExecutionCallback
import com.infineon.secora.wearable.protocolapi.IHostSharedBleProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import androidx.core.net.toUri

/**
 * A base fragment providing common utilities such as OTP countdown,
 * keyboard handling, Firebase token initialization, and logging.
 *
 * This class is designed to be extended by other fragments that require
 * reusable features like OTP timing and network status tracking.
 *
 */
open class BaseFragment : Fragment() {

    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)
    private var requestPermission: String? = null
    private lateinit var credentialManager: CredentialManager
    private val otpDurationMillis = 60000L // 60 seconds
    private var countDownTimer: CountDownTimer? = null
    private var isStatusDialogVisible = false
    public var isPasswordVisible = false
    private lateinit var activity: MainActivity
    private val bleReconnectTimeoutHandler = Handler(Looper.getMainLooper())
    private var bleReconnectTimeoutRunnable: Runnable? = null
    private val bleReconnectTimeout = 28_000L
    private val bleReconnectInitialDelayMs = 2_000L
    private var nfcSheet: NfcScanBottomSheet? = null

    var isSeiTsmFlowEnabled = true

    data class DeviceDetails(
        val connected: Boolean,
        val currentSequenceCounter: String
    )
    protected var alertDialog: Dialog? = null

    /**
     * Initializes the activity reference after the view is created.
     * Safely casts the attached activity to MainActivity for further use.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setFilterTouchesWhenObscured(true)
        activity = requireActivity() as MainActivity
    }

    /**
     * Prepares the OTP timer. If the OTP is not expired, resume the remaining countdown.
     * Otherwise, starts a fresh countdown for a new OTP.
     *
     * @param tv The [TextView] where the countdown will be displayed.
     */
    fun prepareOtp(tv: TextView, forceReset: Boolean = false, onTimeOut: (() -> Unit)? = null) {
        val currentTime = System.currentTimeMillis()

        if (forceReset) {
            val newExpireTime = currentTime + otpDurationMillis
            StorageRepository.saveString(PreferenceKey.OTP_EXPIRE_TIME, newExpireTime.toString())
            startOtpCountdown(otpDurationMillis, tv, onTimeOut)
            return
        }

        val expireTimeString = StorageRepository.readString(PreferenceKey.OTP_EXPIRE_TIME)
        var expireTime = 0L
        if (!isEmptyString(expireTimeString)) {
            expireTime = expireTimeString.toLong()
        }

        if (expireTime == 0L || currentTime >= expireTime) {
            // Start fresh countdown
            val newExpireTime = currentTime + otpDurationMillis
            StorageRepository.saveString(PreferenceKey.OTP_EXPIRE_TIME, newExpireTime.toString())
            startOtpCountdown(otpDurationMillis, tv, onTimeOut)
        } else {
            // Resume existing countdown
            startOtpCountdown(expireTime - currentTime, tv, onTimeOut)
        }
    }

    /**
     * Hides the soft keyboard.
     */
    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Starts an OTP countdown timer and updates the given [TextView] every second.
     *
     * @param timeLeftInMillis Remaining time in milliseconds.
     * @param tv The [TextView] where the countdown will be displayed.
     */
    private fun startOtpCountdown(timeLeftInMillis: Long, tv: TextView, onTimeOut: (() -> Unit)?) {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                tv.text = getString(R.string.seconds_remaining, seconds)
            }

            override fun onFinish() {
                tv.text = ""
                if (onTimeOut != null) {
                    onTimeOut()
                }
            }
        }.start()
    }

    /**
     * Called when the fragment is created. Initializes Infineon SDK and fetches the FCM token.
     *
     * @param savedInstanceState Saved fragment state (if any).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WalletRepository.initializeWalletSdk(requireContext().applicationContext)
        credentialManager = CredentialManager.create(requireContext())
    }

    /**
     * Requests focus for the given view and opens the software keyboard.
     *
     * @param view The view to receive focus and trigger the keyboard.
     */
    fun showKeyboard(view: View) {
        view.requestFocus()
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Displays a confirmation dialog prompting the user to enable device settings.
     *
     * The dialog has two options:
     * - "Settings": Opens the Location Source Settings screen.
     * - "Cancel": Dismisses the dialog.
     *
     * Optional message to be displayed in the dialog.
     * Type of alert (currently unused but reserved for future logic).
     */
    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            logger.info("SettingsLauncher Returned from settings with resultCode: ${result.resultCode}")
        }

    /**
     * Shows a confirmation dialog with a custom message.
     *
     * Provides two options:
     * - **Settings** → Opens the location settings screen.
     * - **Cancel** → Dismisses the dialog.
     *
     * @param titleMessage Message to display in the dialog.
     * Type of alert (reserved for future use).
     */
    fun confirmDataDialog(titleMessage: String?) {
        if (!::activity.isInitialized || activity.isFinishing) return
        activity.runOnUiThread {
            val alertDialogBuilder = AlertDialog.Builder(activity)
            alertDialogBuilder.setTitle(resources.getString(R.string.app_name))
            alertDialogBuilder.setMessage(titleMessage).setCancelable(false).setPositiveButton(
                resources.getString(R.string.settings)
            ) { _, _ ->
                settingsLauncher.launch(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            }.setNegativeButton(
                resources.getString(R.string.dialog_cancel)
            ) { dialog, _ ->
                dialog.cancel()
            }

            val alertDialog = alertDialogBuilder.create()
            alertDialog.showSecure()
        }
    }

    /**
     * Displays a short Toast message on the screen.
     * This function ensures that the Toast is shown on the main (UI) thread,
     * even if called from a background thread.
     *
     * @param message The message text to be displayed in the Toast.
     */
    open fun showToast(message: String?) {
        if (!::activity.isInitialized || activity.isFinishing) return
        activity.runOnUiThread {
            Toast.makeText(activity.applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Checks whether a specific permission has been granted.
     *
     * @param permission The permission string to check (e.g., `Manifest.permission.CAMERA`).
     * @return `true` if the permission is granted, `false` otherwise.
     */
    fun checkPermission(permission: String?): Boolean {
        requestPermission = permission
        val result = permission?.let {
            ContextCompat.checkSelfPermission(requireContext(), it)
        }
        return result == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests a specific runtime permission from the user.
     *
     * @param permission The permission string to request.
     */
    fun requestPermission(permission: String?) {
        requestPermission = permission
        ActivityCompat.requestPermissions(
            requireActivity(), arrayOf(permission), PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Wearable BLE flows require Bluetooth to be on (API 31+ scan uses [neverForLocation]).
     *
     * @return `true` when Bluetooth is enabled on the device.
     */
    fun isBluetoothReadyForWearable(): Boolean {
        return BluetoothStateManager.isBluetoothTurnedOn(requireContext())
    }

    /**
     * Displays a status dialog with a message and a dismissible "OK" button.
     *
     * @param hostContext Context used to resolve the host [Activity] (typically [MainActivity]).
     * @param message The status message to be shown.
     */
    fun statusDialog(hostContext: Context, message: String?, onOkAction: (() -> Unit)? = null) {
        if (isStatusDialogVisible) return
        val hostActivity = when (hostContext) {
            is Activity -> if (!hostContext.isFinishing) hostContext else null
            else -> if (::activity.isInitialized && !activity.isFinishing) activity else null
        } ?: return

        hostActivity.runOnUiThread {
            if (hostActivity.isFinishing) return@runOnUiThread

            val dialogViewBinding = DialogCommonMessageBinding.inflate(layoutInflater)
            val alertDialog = Dialog(requireContext()).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(dialogViewBinding.root)
                setCancelable(false)
            }
            isStatusDialogVisible = true

            configureStatusDialogContent(hostActivity, message, dialogViewBinding)
            dialogViewBinding.txtMessage.text = message

            dialogViewBinding.txtOK.setOnClickListener {
                alertDialog.dismiss()
                isStatusDialogVisible = false
                if (onOkAction != null) {
                    onOkAction()
                } else {
                    handleStatusDialogOk(hostActivity, message)
                }
            }

            alertDialog.setOnDismissListener { isStatusDialogVisible = false }
            alertDialog.showSecure()
        }
    }

    /**
     * Sets title, button labels, and visibility for [R.layout.dialog_common_message] based on [message].
     *
     * @param activity Host activity for string resources.
     * @param message Dialog body text; also drives which preset layout variant is applied.
     * @param dialogViewBinding [DialogCommonMessageBinding] dialog view binding
     */
    private fun configureStatusDialogContent(
        activity: Activity,
        message: String?,
        dialogViewBinding: DialogCommonMessageBinding
    ) {
        dialogViewBinding.txtCancel.visibility = View.GONE
        when (message) {
            activity.getString(R.string.text_password_requirements) -> {
                dialogViewBinding.txtTitle.text = activity.getString(R.string.text_password_must)
                dialogViewBinding.txtTitle.setPadding(0, 0, 0, 0)
            }

            activity.getString(R.string.read_more_toast),
            activity.getString(R.string.something_went_wrong) -> {
                dialogViewBinding.txtTitle.text = activity.getString(R.string.text_secora_wallet)
            }

            activity.getString(R.string.text_do_want_to_reset_password) -> {
                dialogViewBinding.txtTitle.text = activity.getString(R.string.forgot_password)
                dialogViewBinding.txtCancel.text = activity.getString(R.string.text_yes)
                dialogViewBinding.txtOK.text = activity.getString(R.string.text_cancel)
                dialogViewBinding.txtCancel.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Runs side effects after the user dismisses the status dialog (navigation, loader, NFC sheet).
     *
     * @param activity Current activity for resources and navigation.
     * @param message Same message as shown in the dialog; used to choose follow-up actions.
     */
    private fun handleStatusDialogOk(activity: Activity, message: String?) {
        val normalizedMessage = message?.lowercase().orEmpty()
        if (message.equals(activity.getString(R.string.bluetooth_not_connected))) {
            findNavController().navigate(R.id.deviceListFragment)
        }
        if (message.equals(GIVEN_DEVICE_MODEL_IS_NOT_PRESENT_OR_DO_NOT_SUPPORT, ignoreCase = true)) {
            dismissLoader()
        }
        if (normalizedMessage.contains("does not belong to")) {
            // Ownership mismatch flow can leave blurred overlay in some sequential paths.
            dismissLoader()
            if (isNFC()) {
                findNavController().navigate(R.id.deviceListFragment)
            }
        }
    }

    /**
     * Hides the progress bar.
     */
    fun dismissLoader() {
        if (FcmSecureFlowCoordinator.isLoaderHoldActive()) {
            return
        }
        activity.showLoading(false, "")
    }

    /**
     * Displays a dialog with a message and two buttons: OK and Cancel.
     * The OK button triggers a custom click listener.
     *
     * @param activity Context used to display the dialog.
     * @param message Message to be displayed in the dialog.
     * @param okListener Listener triggered when the OK button is clicked.
     */
    fun statusDialogListener(
        activity: Context,
        message: String?,
        okListener: OnClickListener,
        cancelListener: (() -> Unit)? = null
    ) {
        (activity as Activity).runOnUiThread {
            if (activity.isFinishing) return@runOnUiThread

            val dialogViewBinding = DialogCommonMessageBinding.inflate(layoutInflater)
            val alertDialog = AlertDialog.Builder(requireContext())
                .setView(dialogViewBinding.root)
                .setCancelable(false)
                .create()

            dialogViewBinding.txtMessage.text = message
            dialogViewBinding.txtOK.text = getString(R.string.text_yes)
            dialogViewBinding.txtCancel.text = getString(R.string.text_no)

            dialogViewBinding.txtOK.setOnClickListener {
                dialogViewBinding.txtOK.isEnabled = false
                okListener.onClick(it)
                alertDialog.dismiss()
            }

            dialogViewBinding.txtCancel.setOnClickListener {
                cancelListener?.invoke()
                alertDialog.dismiss()
            }

            alertDialog.showSecure()
        }
    }

    /**
     * Companion object for holding static/shared members.
     */
    companion object {
        private const val PERMISSION_REQUEST_CODE = 200

        /**
         * Assets path for the JSON payload that defines the APDU script used to read the sequence counter.
         */
        private const val SEQUENCE_COUNTER_ASSET = "sequence-counter.json"
    }

    /**
     * Controls the visibility of a `ProgressBar` on the UI thread.
     *
     * @param progressBar Progress bar whose visibility needs to be changed.
     * @param show If true, the progress bar is shown; otherwise, hidden.
     */
    fun showLoading(progressBar: ProgressBar, show: Boolean) {
        progressBar.post {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    /**
     * Controls the visibility of a `ConstraintLayout` (used as a loading overlay).
     *
     * @param progressBar The layout to show/hide.
     * @param show If true, the layout is shown; otherwise, hidden.
     */
    fun showLoading(progressBar: ConstraintLayout, show: Boolean) {
        progressBar.post {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    /**
     * Returns the current PNO (Payment Network Operator) type from shared preferences.
     *
     * @return `"MDES"` if selected; otherwise `"VTS"`.
     */
    fun pnoType(): String {
        return if (StorageRepository.readString(PreferenceKey.PNO_TYPE) == PNO_MDES
        ) PNO_MDES else PNO_VTS
    }

    /**
     * Checks if biometric authentication is available and enrolled on the device.
     * If available, it shows the biometric prompt to the user.
     *
     * @param context The context used to access the BiometricManager.
     */
    fun bioMetric(context: Context) {
        if (!BuildConfig.ENABLE_BIOMETRIC) return
        val biometricManager = BiometricManager.from(context)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                showBiometricPrompt(context)
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                // No biometric features available on this device
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                // Biometric features are currently unavailable
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // User hasn't enrolled any biometric credentials
               navigateToLoginScreen(getString(R.string.biometric_not_enrolled))
            }

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                // User hasn't enrolled any biometric credentials
            }

            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                // User hasn't enrolled any biometric credentials
            }

            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                // User hasn't enrolled any biometric credentials
            }
        }
    }

    /**
     * Displays the biometric prompt and handles authentication callbacks.
     * On successful authentication, navigate to the device list fragment.
     *
     * @param context The context to access system services and resources.
     */
    fun showBiometricPrompt(context: Context) {
        if (!BuildConfig.ENABLE_BIOMETRIC) return
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    handleNavigation()
                    // Auth success - proceed
                }

            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder().setTitle(getString(R.string.fingerprint))
            .setSubtitle(getString(R.string.login_using_fingerprint))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Handles the fragment navigation flow for the normal usecase as well as notification click event use case.
     *
     */
    fun handleNavigation() {
        arguments?.getString(BundleKey.ENTITY_ID)?.let { entityId ->
            if (!::activity.isInitialized) return@let

            lifecycleScope.launch {
                val card = WalletRepository.getLocalCardDetailsOrRefreshFromProvisionApi(activity, entityId)
                if (card.digitizationReferenceNumber == null) {
                    logger.debug(":: Base fragment Notification Flow : Regular Flow")
                    findNavController().navigate(R.id.deviceListFragment)
                } else {
                    val bundle = Utils.prepareBundleWithCardDetails(card)
                    StorageRepository.saveString(
                        PreferenceKey.DIGITIZATION_REFERENCE_NUMBER,
                        card.digitizationReferenceNumber.toString()
                    )
                    card.paymentAppInstanceId?.takeIf { it.isNotBlank() }?.let { pid ->
                        StorageRepository.saveString(PreferenceKey.PAYMENT_APP_INSTANCE_ID, pid)
                    }
                    logger.debug(":: Base fragment Notification Flow : Direct Flow")
                    findNavController().navigate(
                        R.id.detailFragment, bundle
                    )
                }
            }
        } ?: run {
            logger.debug(":: Base fragment Normal Flow")
            findNavController().navigate(R.id.deviceListFragment)
        }
    }

    /**
     * Hides the keyboard when the user taps outside an input field.
     *
     * @param activity The hosting activity.
     * @param view The root view to attach the touch listener to.
     */
    fun dismissKeyboardOnTap(activity: Activity, view: View) {
        view.isClickable = true
        view.isFocusable = true
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val focusedView = activity.currentFocus
                if (focusedView != null) {
                    val softInputManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    softInputManager.hideSoftInputFromWindow(focusedView.windowToken, 0)
                    focusedView.clearFocus()
                }
                v.performClick()
            }
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        countDownTimer = null
    }

    /**
     * Sets up the toolbar profile icon with appropriate user information.
     *
     * This method retrieves the stored username, profile image, and email from preferences,
     * and updates the profile icon on the toolbar accordingly. It shows:
     * - Initials based on email if username and profile image are not available.
     * - Initials based on username if the profile image is not available.
     * - The actual profile image if available.
     * - A default icon if no user data is available.
     *
     * @param activity The hosting [AppCompatActivity], expected to be [MainActivity], used to access the toolbar and context.
     */
    protected fun setupToolbarProfile(activity: AppCompatActivity) {
        val toolbar = (activity as? MainActivity)?.binding?.toolbar ?: return
        val email = StorageRepository.readString(PreferenceKey.EMAIL_ID)
        toolbar.profileIcon.visibility =
            if (email.isNotEmpty()) View.VISIBLE else View.GONE

        val userName = StorageRepository.readString(PreferenceKey.USER_NAME)
        val profileImage = StorageRepository.readString(PreferenceKey.PROFILE_IMAGE)
        when {
            !isEmptyString(email) && isEmptyString(userName) && isEmptyString(profileImage) -> {
                activity.let {
                    toolbar.profileIcon.setImageBitmap(it.createInitialsDrawable(email))
                }
            }

            !isEmptyString(userName) && isEmptyString(profileImage) -> {
                activity.let {
                    toolbar.profileIcon.setImageBitmap(it.createInitialsDrawable(userName))
                }
            }

            !isEmptyString(profileImage) -> {
                try {
                    Glide.with(this@BaseFragment).load(profileImage).circleCrop().into(toolbar.profileIcon)
                } catch (e: IllegalArgumentException) {
                    logger.noStackTraceLog("SetupToolBarProfile ", e)
                }
            }

            else -> {
                toolbar.profileIcon.setImageResource(R.drawable.ic_user_avatar)
            }
        }
    }

    /**
     * Ensures BLE is connected before executing a sensitive operation.
     *
     * Flow:
     * 1. If Bluetooth is OFF → show dialog and abort.
     * 2. If seId or BLE address is missing (e.g. after reinstall) → navigate to device list so user
     *    can scan, select device, connect; that flow runs fetchSEId() and stores fresh seId and address.
     * 3. If BLE is already connected → execute the action immediately.
     * 4. If BLE is disconnected but SE + address are known → attempt reconnection.
     * 5. If reconnection succeeds → execute the action.
     * 6. If reconnection fails → show failure dialog and abort.
     *
     * @param onConnected Action to execute only when BLE connection is guaranteed.
     */
    /**
     * True when delete scripts and pending-task acknowledgement must not run on BLE
     * (wearable disconnected while the host still needs updateCardStatus cleanup).
     */
    protected fun shouldSkipSeDeleteScriptAndAcknowledgement(): Boolean =
        !isNFC() && !BluetoothStateManager.isConnected

    /**
     * Resolves the sequence counter for backend cleanup after a failed provision attempt.
     * Prompts for BLE reconnect when needed; [onProceedWithoutBle] runs when the user declines
     * reconnect or BLE credentials are unavailable (updateCardStatus without SE scripts).
     */
    protected fun fetchSequenceNumberForProvisionAbort(
        onRetrieved: (String) -> Unit,
        onProceedWithoutBle: () -> Unit
    ) {
        fetchSequenceNumberFromDevice(
            onRetrieved = onRetrieved,
            onFailed = onProceedWithoutBle,
            onCancelled = onProceedWithoutBle,
            allowReconnectPrompt = true
        )
    }

    /**
     * Reconnect prompt after Prep SE or Notify Provision **script** execution fails because BLE dropped.
     * If reconnect fails, shows [R.string.bluetooth_reconnect_failed] and then this dialog again.
     * [onDeclined] mirrors tapping Cancel on the terms screen (cleanup without SE).
     */
    protected fun showProvisionBleReconnectPrompt(
        onConnected: () -> Unit,
        onDeclined: () -> Unit
    ) {
        val context = resolveContextForBleGate() ?: return
        if (!ensureBluetoothEnabledForBleGate(context, allowReconnectPrompt = true, onDeclined)) return

        val seId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)
        val bleAddress = StorageRepository.readString(PreferenceKey.bleAddressKey(seId))
        if (seId.isNullOrBlank() || bleAddress.isNullOrBlank()) {
            onDeclined()
            return
        }
        if (BluetoothStateManager.isDeviceConnected(seId, context)) {
            onConnected()
            return
        }
        promptBleReconnect(
            context = context,
            seId = seId,
            bleAddress = bleAddress,
            onConnected = onConnected,
            onCancelled = onDeclined,
            rePromptOnReconnectFailure = true
        )
    }

    /**
     * Shows a single-action alert when a provision/add-card script fails due to BLE transport loss.
     * [onOk] should run the same backend cleanup as declining the BLE reconnect prompt.
     */
    protected fun showProvisionScriptBleConnectionLostDialog(onOk: () -> Unit) {
        val context = resolveContextForBleGate() ?: return
        statusDialog(context, getString(R.string.failed_to_add_card_bluetooth_connection_lost), onOk)
    }

    protected fun ensureBleConnectedThenRun(
        onConnected: () -> Unit,
        onCancelled: (() -> Unit)? = null,
        allowReconnectPrompt: Boolean = true,
        rePromptOnReconnectFailure: Boolean = false,
        blePromptNotRequired: Boolean = false,
    ) {
        val context = resolveContextForBleGate() ?: return
        if (!ensureBluetoothEnabledForBleGate(context, allowReconnectPrompt, onCancelled)) return

        val seId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)
        val bleAddress = StorageRepository.readString(PreferenceKey.bleAddressKey(seId))

        if (seId.isNullOrBlank() || bleAddress.isNullOrBlank()) {
            handleMissingBleCredentials(context, allowReconnectPrompt, onConnected, onCancelled)
            return
        }

        if (BluetoothStateManager.isDeviceConnected(seId, context)) {
            onConnected()
            return
        }

        if (!allowReconnectPrompt) {
            logger.debug("BLE not connected and reconnect prompt suppressed")
            onCancelled?.invoke()
            return
        }

        if (blePromptNotRequired) {
            val mainActivity = resolveMainActivityForBleGate()
            startBleReconnectFlow(
                context = context,
                seId = seId,
                bleAddress = bleAddress,
                activity = mainActivity,
                onConnected = onConnected,
                onCancelled = onCancelled,
                rePromptOnReconnectFailure = rePromptOnReconnectFailure
            )
            return
        }


        promptBleReconnect(
            context = context,
            seId = seId,
            bleAddress = bleAddress,
            onConnected = onConnected,
            onCancelled = onCancelled,
            rePromptOnReconnectFailure = rePromptOnReconnectFailure
        )
    }

    /**
     * Returns a valid [Context] for BLE gate checks, or `null` when the fragment is detached.
     */
    private fun resolveContextForBleGate(): Context? =
        if (::activity.isInitialized && !activity.isFinishing) {
            activity
        } else {
            context
        }

    /**
     * Verifies Bluetooth is on before BLE reconnect or script execution.
     *
     * @param context              Context for state checks and dialogs.
     * @param allowReconnectPrompt When false, invokes [onCancelled] instead of showing a dialog.
     * @param onCancelled          Optional callback when Bluetooth is off and prompts are suppressed.
     * @return `true` when Bluetooth is enabled.
     */
    private fun ensureBluetoothEnabledForBleGate(
        context: Context,
        allowReconnectPrompt: Boolean,
        onCancelled: (() -> Unit)?
    ): Boolean {
        if (BluetoothStateManager.isBluetoothTurnedOn(context)) return true
        if (allowReconnectPrompt) {
            statusDialog(context, getString(R.string.bluetooth_not_turned_on))
        } else {
            onCancelled?.invoke()
        }
        return false
    }

    /**
     * Returns the host [MainActivity] for loading UI and BLE reconnect flows.
     */
    private fun resolveMainActivityForBleGate(): MainActivity =
        if (::activity.isInitialized && !activity.isFinishing) {
            activity
        } else {
            requireActivity() as MainActivity
        }

    /**
     * Post-reinstall or first use: no stored seId/address — show reconnect dialog; on Yes run scan+connect+fetch.
     */
    private fun handleMissingBleCredentials(
        context: Context,
        allowReconnectPrompt: Boolean,
        onConnected: () -> Unit,
        onCancelled: (() -> Unit)?
    ) {
        if (!allowReconnectPrompt) {
            onCancelled?.invoke()
            return
        }
        val mainActivity = resolveMainActivityForBleGate()
        showBluetoothReconnectConfirmDialog(
            context = context,
            onYes = {
                mainActivity.showLoading(true, getString(R.string.bluetooth_reconnecting))
                try {
                    scanConnectFetchSeIdThenReconnect(context, mainActivity, onConnected)
                } catch (e: SecurityException) {
                    mainActivity.showLoading(false, "")
                    onCancelled?.invoke()
                    showBluetoothNotConnected(context)
                    logger.debug("scanConnectFetchSeIdThenReconnect SecurityException: ${e.message}")
                }
            },
            onNo = {
                logger.debug("User cancelled BLE reconnect")
                onCancelled?.invoke()
            }
        )
    }

    /**
     * Shows the reconnect confirmation dialog and starts [startBleReconnectFlow] when the user accepts.
     *
     * @param context      Context for the dialog.
     * @param seId         Secure element ID of the selected device.
     * @param bleAddress   Stored BLE MAC for the device.
     * @param onConnected  Action to run after a successful reconnect.
     * @param onCancelled  Optional action when the user declines or reconnect fails.
     */
    private fun promptBleReconnect(
        context: Context,
        seId: String,
        bleAddress: String,
        onConnected: () -> Unit,
        onCancelled: (() -> Unit)?,
        rePromptOnReconnectFailure: Boolean = false
    ) {
        val mainActivity = resolveMainActivityForBleGate()
        showBluetoothReconnectConfirmDialog(
            context = context,
            onYes = {
                startBleReconnectFlow(
                    context = context,
                    seId = seId,
                    bleAddress = bleAddress,
                    activity = mainActivity,
                    onConnected = onConnected,
                    onCancelled = onCancelled,
                    rePromptOnReconnectFailure = rePromptOnReconnectFailure
                )
            },
            onNo = {
                logger.debug("User cancelled BLE reconnect")
                onCancelled?.invoke()
            }
        )
    }

    protected fun ensureNfcReadyThenRun(
        onConnected: () -> Unit
    ) {
        val context: Context = if (::activity.isInitialized && !activity.isFinishing) {
            activity
        } else {
            this.context ?: return
        }

        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)

        // NFC not supported
        if (nfcAdapter == null) {
            statusDialog(context, getString(R.string.nfc_not_supported))
            return
        }

        // NFC disabled
        if (!nfcAdapter.isEnabled) {
            statusDialog(context, getString(R.string.nfc_not_turned_on))
            return
        }

        onConnected()
    }

    private val pairedSeIdsKey = "PAIRED_SE_IDS"
    private val reconnectScanTimeoutMs = 10_000L

    /**
     * Holds BLE scan state while searching for a bonded or advertised SECORA device to reconnect.
     *
     * @param scanner          Active BLE LE scanner instance.
     * @param targetDeviceName Stored wearable name selected by the user.
     * @param devices          Mutable list of candidate devices discovered so far.
     * @param mainHandler      Main-thread handler for scan callbacks.
     */
    private data class ReconnectScanParams(
        val scanner: BluetoothLeScanner,
        val targetDeviceName: String,
        val devices: MutableList<BluetoothDevice>,
        val mainHandler: Handler
    )

    /**
     * Shows the standard Bluetooth-not-connected status dialog.
     *
     * @param context Context used to display the dialog.
     */
    private fun showBluetoothNotConnected(context: Context) {
        statusDialog(context, getString(R.string.bluetooth_not_connected))
    }

    /**
     * Validates permissions and prepares bonded-device candidates for BLE reconnect scanning.
     *
     * @param context Context for permission checks and Bluetooth services.
     * @return Scan parameters when ready, or `null` when reconnect cannot proceed.
     */
    private fun prepareReconnectScan(context: Context): ReconnectScanParams? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showBluetoothNotConnected(context)
            return null
        }
        val targetDeviceName = StorageRepository.readString(PreferenceKey.DEVICE_NAME).trim().takeIf { it.isNotEmpty() }
        if (targetDeviceName.isNullOrBlank()) {
            showBluetoothNotConnected(context)
            return null
        }
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: run {
            showBluetoothNotConnected(context)
            return null
        }
        val scanner = adapter.bluetoothLeScanner ?: run {
            showBluetoothNotConnected(context)
            return null
        }
        val devices = mutableListOf<BluetoothDevice>()
        val allBonded = try {
            adapter.bondedDevices ?: emptySet()
        } catch (e: SecurityException) {
            logger.debug("getBondedDevices failed: ${e.message}")
            emptySet()
        }
        allBonded.filter { d ->
            val n = d.name
            n == null || n.isBlank() || n.startsWith("SECORA", ignoreCase = true)
        }.let { devices.addAll(it) }
        logger.info(
            "BLE reconnect: targetDeviceName=$targetDeviceName, bondedCandidates=${devices.size}, names=${
                devices.map {
                    it.name?.take(
                        30
                    ) ?: "null"
                }
            }"
        )
        return ReconnectScanParams(scanner, targetDeviceName, devices, Handler(Looper.getMainLooper()))
    }

    /**
     * Returns whether [device] matches the stored reconnect target by exact or SECORA prefix name.
     *
     * @param device           Candidate Bluetooth device.
     * @param targetDeviceName Expected wearable name from preferences.
     * @return `true` when the device should be used for reconnect.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun deviceNameMatchesReconnectTarget(device: BluetoothDevice, targetDeviceName: String): Boolean {
        val name = try {
            device.name?.trim()
        } catch (e: SecurityException) {
            logger.debug("deviceNameMatchesReconnectTarget SecurityException: ${e.message}")
            return false
        }
        if (name.isNullOrBlank()) return false
        val target = targetDeviceName.trim()
        if (name.equals(target, ignoreCase = true)) return true
        if (name.startsWith("SECORA", ignoreCase = true) && target.startsWith("SECORA", ignoreCase = true)
            && (target.startsWith(name, ignoreCase = true) || name.startsWith(target, ignoreCase = true))
        ) return true
        return false
    }

    /**
     * Attempts reconnect using a bonded SECORA device before starting an active BLE scan.
     *
     * @param params      Prepared scan/bonded-device state.
     * @param context     Context for BLE connect and CPLC fetch.
     * @param activity    Host activity for loading UI.
     * @param onConnected Callback after successful connect and SEID persistence.
     * @return `true` when a bonded match was found and reconnect was started.
     */
    private fun tryConnectFromBonded(
        params: ReconnectScanParams,
        context: Context,
        activity: MainActivity,
        onConnected: () -> Unit
    ): Boolean {
        return try {
            var matched = params.devices.firstOrNull { deviceNameMatchesReconnectTarget(it, params.targetDeviceName) }
            if (matched == null && params.devices.size == 1) {
                val single = params.devices.single()
                val singleName = try {
                    single.name
                } catch (e: SecurityException) {
                    null
                }
                if (singleName == null || singleName.startsWith("SECORA", ignoreCase = true)) {
                    matched = single
                    logger.debug("BLE reconnect: using single bonded SECORA device (name=$singleName)")
                }
            }
            when {
                matched == null -> false
                else -> {
                    connectFetchSeIdAndThenReconnect(context, activity, matched, onConnected)
                    true
                }
            }
        } catch (e: SecurityException) {
            logger.debug("tryConnectFromBonded SecurityException: ${e.message}")
            false
        }
    }

    /**
     * Updates the reconnect candidate list with a scan result, replacing unnamed duplicates by address.
     *
     * @param devices Mutable list of discovered reconnect candidates.
     * @param device  Device reported by the BLE scan callback.
     * @param name    Advertised device name, if available.
     */
    private fun updateReconnectDeviceListFromScan(
        devices: MutableList<BluetoothDevice>,
        device: BluetoothDevice,
        name: String?
    ) {
        val existing = devices.find { it.address == device.address }
        if (existing != null) {
            val existingName = try {
                existing.name
            } catch (e: SecurityException) {
                null
            }
            if (name != null && existingName.isNullOrBlank()) {
                devices.remove(existing)
                devices.add(device)
            }
        } else if (name != null) {
            devices.add(device)
        }
    }

    /**
     * Stops scanning and starts connect-plus-CPLC fetch when a matching device is found live.
     *
     * @param params       Active reconnect scan state.
     * @param scanCallback Scan callback to stop once a match is accepted.
     * @param context      Context for BLE connect.
     * @param activity     Host activity for loading UI.
     * @param device       Matched Bluetooth device.
     * @param onConnected  Callback after reconnect and preference persistence.
     */
    private fun onReconnectScanResultConnected(
        params: ReconnectScanParams,
        scanCallback: ScanCallback,
        context: Context,
        activity: MainActivity,
        device: BluetoothDevice,
        onConnected: () -> Unit
    ) {
        try {
            params.scanner.stopScan(scanCallback)
        } catch (e: SecurityException) {
            logger.debug("StopScan in scan result: ${e.message}")
        }
        connectFetchSeIdAndThenReconnect(context, activity, device, onConnected)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun createReconnectScanCallback(
        params: ReconnectScanParams,
        context: Context,
        activity: MainActivity,
        onConnected: () -> Unit,
        flowStarted: AtomicBoolean
    ): ScanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = try {
                device.name
            } catch (e: SecurityException) {
                logger.debug("onScanResult device.name: ${e.message}"); null
            }

            if (name.isNullOrBlank() ||
                !name.startsWith("SECORA", ignoreCase = true) ||
                name.trim() != params.targetDeviceName.trim()
            ) return

            logger.debug(":: onScanResult : name: $name")
            logger.debug(":: onScanResult : params.targetDeviceName: ${params.targetDeviceName}")

            params.mainHandler.post {
                try {
                    updateReconnectDeviceListFromScan(params.devices, device, name)
                    if (!deviceNameMatchesReconnectTarget(
                            device,
                            params.targetDeviceName
                        ) || flowStarted.getAndSet(true)
                    ) return@post
                    onReconnectScanResultConnected(params, this, context, activity, device, onConnected)
                } catch (e: SecurityException) {
                    logger.debug("onScanResult post SecurityException: ${e.message}")
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            params.mainHandler.post {
                if (flowStarted.get()) return@post
                activity.showLoading(false, "")
                showBluetoothNotConnected(context)
            }
        }
    }

    /**
     * Schedules a delayed timeout to stop BLE scanning and attempt reconnection.
     *
     * After [reconnectScanTimeoutMs], this will:
     * - Stop the BLE scan safely
     * - Hide loading UI
     * - Attempt to find a matching device
     * - Trigger reconnect if a match is found
     * - Otherwise, log and notify UI that device is not connected
     *
     * @param params Holds scan-related data such as scanner, handler, scanned devices,
     *               and the target device name to reconnect.
     * @param scanCallback The BLE scan callback used to stop scanning.
     * @param context Context used for UI notifications and Bluetooth state handling.
     * @param activity Activity used to update loading UI state.
     * @param onConnected Callback invoked after a successful reconnection flow.
     * @param flowStarted Atomic flag indicating whether a connection flow has already started.
     *                    Prevents duplicate execution of reconnect logic.
     *
     * @throws SecurityException If Bluetooth permissions are missing (handled internally).
     */
    private fun scheduleReconnectScanTimeout(
        params: ReconnectScanParams,
        scanCallback: ScanCallback,
        context: Context,
        activity: MainActivity,
        onConnected: () -> Unit,
        flowStarted: AtomicBoolean
    ) {
        params.mainHandler.postDelayed({
            if (flowStarted.get()) return@postDelayed

            try {
                try {
                    params.scanner.stopScan(scanCallback)
                } catch (e: SecurityException) {
                    logger.debug("StopScan on timeout: ${e.message}")
                }

                activity.showLoading(false, "")

                val matched = findMatchedDevice(params)

                if (matched != null) {
                    connectFetchSeIdAndThenReconnect(context, activity, matched, onConnected)
                } else {
                    val names = try {
                        params.devices.map { it.name?.take(20) ?: "null" }
                    } catch (e: SecurityException) {
                        emptyList()
                    }

                    logger.info("BLE reconnect: no match after timeout. devices=${params.devices.size}, names=$names")
                    showBluetoothNotConnected(context)
                }

            } catch (e: SecurityException) {
                logger.debug("scheduleReconnectScanTimeout SecurityException: ${e.message}")
                activity.showLoading(false, "")
                showBluetoothNotConnected(context)
            }

        }, reconnectScanTimeoutMs)
    }

    /**
     * Finds a matching BLE device from the scanned device list.
     *
     * Matching strategy:
     * 1. Primary match using [deviceNameMatchesReconnectTarget]
     * 2. Fallback: if only one device is found and its name is either:
     *    - null, or
     *    - starts with "SECORA" (case-insensitive)
     *
     * @param params Contains scanned devices and the target device name.
     *
     * @return A matched [BluetoothDevice] if found, otherwise null.
     *
     * @throws SecurityException When accessing device.name without permission
     *                           (handled internally and treated as no match).
     */
    private fun findMatchedDevice(params: ReconnectScanParams): BluetoothDevice? {
        var matched = params.devices.firstOrNull { device ->
            try {
                deviceNameMatchesReconnectTarget(device, params.targetDeviceName)
            } catch (e: SecurityException) {
                logger.debug("Device name access denied: ${e.message}")
                false
            }
        }

        if (matched == null && params.devices.size == 1) {
            val single = params.devices.single()
            val singleName = try {
                single.name
            } catch (e: SecurityException) {
                null
            }

            if (singleName == null || singleName.startsWith("SECORA", ignoreCase = true)) {
                matched = single
                logger.debug("BLE reconnect timeout: using single SECORA device (name=$singleName)")
            }
        }

        return matched
    }

    /**
     * Scans for SECORA devices in background, finds the device whose name matches the selected
     * device name (user selected from device list / long-press), connects, fetches SEID via CPLC,
     * stores seId and bleAddress, then shows reconnect confirm dialog. No user-facing device picker.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun scanConnectFetchSeIdThenReconnect(
        context: Context,
        activity: MainActivity,
        onConnected: () -> Unit
    ) {
        val params = prepareReconnectScan(context) ?: return
        if (tryConnectFromBonded(params, context, activity, onConnected)) return
        val flowStarted = AtomicBoolean(false)
        val scanCallback = createReconnectScanCallback(params, context, activity, onConnected, flowStarted)
        try {
            params.scanner.startScan(
                null,
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0).build(),
                scanCallback
            )
        } catch (_: SecurityException) {
            activity.showLoading(false, "")
            showBluetoothNotConnected(context)
            return
        }
        scheduleReconnectScanTimeout(params, scanCallback, context, activity, onConnected, flowStarted)
    }

    /**
     * Persists NFC CPLC-derived fields. SDK now returns normalized CPLC hex values.
     * Returns (seid, tagId) for immediate caller use.
     */
    protected fun handleNfcCplcCallback(
        seid: String?,
        tagId: String?,
        icTypeHex: String?,
        oemIdHex: String?,
        seGroupIdHex: String?,
        wearableModelIdHex: String?
    ): Pair<String, String> {
        val normalizedSeId = seid.orEmpty()
        val normalizedIcTypeHex = icTypeHex.orEmpty()
        val normalizedOemIdHex = oemIdHex.orEmpty()
        val normalizedSeGroupIdHex = seGroupIdHex.orEmpty()
        val normalizedWearableModelIdHex = wearableModelIdHex.orEmpty()
        val normalizedTagId = tagId.orEmpty()

        logger.debug(
            "NFC CPLC callback: seIdHex=$normalizedSeId, icTypeHex=$normalizedIcTypeHex, oemIdHex=$normalizedOemIdHex, seGroupIdHex=$normalizedSeGroupIdHex, wearableModelIdHex=$normalizedWearableModelIdHex"
        )

        ConfiguredWalletIdentity.persistForRegistration(
            requireContext().applicationContext,
            fetchedIcTypeHex = normalizedIcTypeHex,
            fetchedSeGroupIdHex = normalizedSeGroupIdHex
        )
        val seTypeGroup = ConfiguredWalletIdentity.registrationSeTypeGroup(
            requireContext().applicationContext,
            normalizedIcTypeHex,
            normalizedSeGroupIdHex
        )
        CoroutineScope(Dispatchers.IO).launch {
            WalletRepository.saveOEMDetails(
                context = requireContext().applicationContext,
                oemId = ConfiguredWalletIdentity.OEM_ID,
                infineonSalesCodeAndGroup = seTypeGroup
            )
        }

        return Pair(normalizedSeId, normalizedTagId)
    }

    /**
     * Connects to [device], fetches SEID via ScriptRunner.fetchCPLCData, stores seId and bleAddress.
     * Caller already showed "would you like to reconnect" and loading; we're connected after fetch so we just call onConnected().
     */
    private fun connectFetchSeIdAndThenReconnect(
        context: Context,
        activity: MainActivity,
        device: BluetoothDevice,
        onConnected: () -> Unit
    ) {
        clearPreviousDevicesData()
        BluetoothStateManager.connectBleDevice(context, device)
            .thenCompose { protocol ->
                ScriptRunner().fetchCPLCData(context, protocol)
                    .thenApply { cplcData ->
                        persistReconnectCplcData(activity, cplcData)
                        Pair(protocol, cplcData?.seIdHex.orEmpty())
                    }
            }
            .whenComplete { result, throwable ->
                mainHandler.post {
                    activity.showLoading(false, "")
                    handleReconnectFetchComplete(context, device, result, throwable, onConnected)
                }
            }
    }

    /**
     * Saves OEM / SE group and wearable model from BLE reconnect CPLC when data is present.
     *
     * @param activity [MainActivity] used for scoped coroutine [lifecycleScope] when persisting OEM.
     * @param cplcData Parsed CPLC from the wearable stack; no-op when null.
     */
    private fun persistReconnectCplcData(
        activity: MainActivity,
        cplcData: com.infineon.secora.wearable.util.CPLCData?
    ) {
        if (cplcData == null) return
        persistReconnectOemData(activity, cplcData.oemIdHex, cplcData.icTypeHex, cplcData.seGroupIdHex)
        persistReconnectWearableModel(cplcData.wearableModelIdHex)
    }

    /**
     * Persists CPLC-derived OEM id and SE type group when complete triple is available,
     * respecting configured-wallet OEM mismatch checks.
     *
     * @param activity Host for [lifecycleScope] when calling [WalletRepository.saveOEMDetails].
     * @param oemIdHex OEM identifier from CPLC.
     * @param icTypeHex IC type from CPLC.
     * @param seGroupIdHex SE group from CPLC.
     */
    private fun persistReconnectOemData(
        activity: MainActivity,
        oemIdHex: String,
        icTypeHex: String,
        seGroupIdHex: String
    ) {
        val appCtx = activity.applicationContext
        ConfiguredWalletIdentity.persistForRegistration(
            appCtx,
            fetchedIcTypeHex = icTypeHex,
            fetchedSeGroupIdHex = seGroupIdHex
        )
        val seTypeGroup = ConfiguredWalletIdentity.registrationSeTypeGroup(appCtx, icTypeHex, seGroupIdHex)
        lifecycleScope.launch {
            WalletRepository.saveOEMDetails(
                context = appCtx,
                oemId = ConfiguredWalletIdentity.OEM_ID,
                infineonSalesCodeAndGroup = seTypeGroup
            )
        }
    }

    /**
     * Stores hardcoded wearable model id (ignores CPLC).
     */
    private fun persistReconnectWearableModel(wearableModelIdHex: String) {
        ConfiguredWalletIdentity.seedHardcodedIdentity(requireContext().applicationContext)
    }

    /**
     * Handles the result of connect + fetch CPLC (SEID) during reconnect.
     * On failure (throwable or null result, or blank seId/bleAddress) shows "Bluetooth not connected"
     * and disconnects the BLE device if needed. On success stores preferences and invokes [onConnected].
     *
     * @param context Application context for dialogs and storage.
     * @param device The Bluetooth device that was connected.
     * @param result The pair (protocol, seId) from fetch; null if the async operation failed.
     * @param throwable Exception from the async operation; non-null indicates failure.
     * @param onConnected Callback to run when reconnect and store completed successfully.
     */
    private fun handleReconnectFetchComplete(
        context: Context,
        device: BluetoothDevice,
        result: Pair<*, String>?,
        throwable: Throwable?,
        onConnected: () -> Unit
    ) {
        if (throwable != null || result == null) {
            logger.debug("Reconnect fetch SEID failed: ${throwable?.message}")
            showBluetoothNotConnected(context)
            return
        }

        val protocol = result.first as? com.infineon.secora.wearable.ble.BleProtocol
        val seId = result.second

        if (protocol == null) {
            logger.debug("Invalid protocol type in result: ${result.first}")
            showBluetoothNotConnected(context)
            return
        }

        val bleAddress = device.address.orEmpty()
        if (seId.isBlank() || bleAddress.isBlank()) {
            showBluetoothNotConnected(context)
            try {
                SecoraWearableSDK.getInstance().getInterface().disconnectBLEDevice(protocol)
            } catch (e: Exception) {
                logger.debug("Disconnect after failed reconnect fetch: ${e.message}")
            }
            return
        }

        BluetoothStateManager.setActiveProtocol(protocol)
        BluetoothStateManager.addConnectedDevice(bleAddress, seId)
        storeReconnectPreferences(context, device, seId, bleAddress)
        onConnected()
    }

    /**
     * Persists reconnect state so the device is treated as linked/paired.
     * Updates [pairedSeIdsKey], [DEVICE_SE_ID], BLE_ADDRESS_$seId, and [DEVICE_NAME] in preferences.
     * Device name is read only if [Manifest.permission.BLUETOOTH_CONNECT] is granted.
     *
     * @param context Application context for preferences and permission check.
     * @param device The Bluetooth device (used to read name when permitted).
     * @param seId Secure Element ID from CPLC fetch.
     * @param bleAddress BLE address of the device.
     */
    private fun storeReconnectPreferences(context: Context, device: BluetoothDevice, seId: String, bleAddress: String) {
        val pairedSet = StorageRepository.readString(PreferenceKey.PAIRED_SE_IDS)
            .split(",")
            .filter { it.isNotEmpty() }
            .toMutableSet()
        pairedSet.add(seId)

        StorageRepository.apply {
            saveString(PreferenceKey.PAIRED_SE_IDS, pairedSet.joinToString(","))
            saveString(PreferenceKey.DEVICE_SE_ID, seId)
            saveString(PreferenceKey.bleAddressKey(seId), bleAddress)
            saveString(PreferenceKey.SELECTED_DEVICE_ADDRESS, bleAddress)
        }

        StorageRepository.readString(PreferenceKey.PAYMENT_APP_INSTANCE_ID).takeIf { it.isNotBlank() }
            ?.let { pid ->
                DeviceDetachTargetResolver.savePaymentAppToSeIdMapping(context, pid, seId)
            }

        val deviceNameForPref = if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                device.name ?: ""
            } catch (e: SecurityException) {
                ""
            }
        } else ""
        StorageRepository.saveString(PreferenceKey.DEVICE_NAME, deviceNameForPref)
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Shared inputs for the stored-address BLE reconnect retry loop.
     *
     * @param context                   Context for dialogs and BLE connect.
     * @param seId                      Secure element ID of the selected device.
     * @param bleAddress                Stored BLE MAC for the device.
     * @param activity                  Host activity for loading UI.
     * @param onConnected               Action to run after a successful reconnect.
     * @param onCancelled               Optional action when the user declines or reconnect fails.
     * @param rePromptOnReconnectFailure When true, shows the reconnect dialog again after failure.
     */
    private data class BleReconnectFlowParams(
        val context: Context,
        val seId: String,
        val bleAddress: String,
        val activity: MainActivity,
        val onConnected: () -> Unit,
        val onCancelled: (() -> Unit)?,
        val rePromptOnReconnectFailure: Boolean
    )

    /**
     * Starts the timed BLE reconnect flow for a known seId and stored MAC address.
     *
     * @param context                   Context for dialogs and BLE connect.
     * @param seId                      Secure element ID of the selected device.
     * @param bleAddress                Stored BLE MAC for the device.
     * @param activity                  Host activity for loading UI.
     * @param onConnected               Action to run after a successful reconnect.
     * @param onCancelled               Optional action when the user declines or reconnect fails.
     * @param rePromptOnReconnectFailure When true, shows the reconnect dialog again after failure.
     */
    private fun startBleReconnectFlow(
        context: Context,
        seId: String,
        bleAddress: String,
        activity: MainActivity,
        onConnected: () -> Unit,
        onCancelled: (() -> Unit)?,
        rePromptOnReconnectFailure: Boolean = false
    ) {
        val flowParams = BleReconnectFlowParams(
            context = context,
            seId = seId,
            bleAddress = bleAddress,
            activity = activity,
            onConnected = onConnected,
            onCancelled = onCancelled,
            rePromptOnReconnectFailure = rePromptOnReconnectFailure
        )
        activity.showLoading(true, getString(R.string.bluetooth_reconnecting))
        bleReconnectTimeoutRunnable?.let { bleReconnectTimeoutHandler.removeCallbacks(it) }
        val completed = booleanArrayOf(false)
        bleReconnectTimeoutRunnable = Runnable {
            if (!completed[0]) {
                completed[0] = true
                logger.error("BLE reconnect failed due to TIMEOUT")
                handleBleReconnectFlowFailure(flowParams)
            }
        }
        bleReconnectTimeoutHandler.postDelayed(bleReconnectTimeoutRunnable!!, bleReconnectTimeout)
        runBleReconnectAttempts(
            flowParams = flowParams,
            maxRetries = 3,
            completed = completed
        ) {
            completed[0] = true
            bleReconnectTimeoutRunnable?.let { bleReconnectTimeoutHandler.removeCallbacks(it) }
            activity.showLoading(false, "")
            onConnected()
        }
    }

    /**
     * Handles BLE reconnect timeout or retry exhaustion.
     *
     * @param flowParams Shared reconnect flow parameters and callbacks.
     */
    private fun handleBleReconnectFlowFailure(flowParams: BleReconnectFlowParams) {
        flowParams.activity.showLoading(false, "")
        if (flowParams.rePromptOnReconnectFailure) {
            showReconnectFailed {
                promptBleReconnect(
                    context = flowParams.context,
                    seId = flowParams.seId,
                    bleAddress = flowParams.bleAddress,
                    onConnected = flowParams.onConnected,
                    onCancelled = flowParams.onCancelled,
                    rePromptOnReconnectFailure = true
                )
            }
        } else {
            flowParams.onCancelled?.invoke()
            showReconnectFailed()
        }
    }

    /**
     * Schedules repeated BLE connect attempts for a stored MAC address.
     *
     * @param flowParams Shared reconnect flow parameters and callbacks.
     * @param maxRetries Maximum number of connect attempts before failure handling.
     * @param completed  Shared flag set when the overall reconnect flow completes.
     * @param onSuccess  Callback invoked on the main thread after a successful connect.
     */
    private fun runBleReconnectAttempts(
        flowParams: BleReconnectFlowParams,
        maxRetries: Int,
        completed: BooleanArray,
        onSuccess: () -> Unit
    ) {
        var attempt = 0
        val handler = Handler(Looper.getMainLooper())

        fun tryReconnect() {
            if (completed[0]) return
            if (isBleReconnectExhausted(attempt, maxRetries, flowParams)) return
            attempt++
            val delayBeforeAttempt = if (attempt == 1) bleReconnectInitialDelayMs else 0L
            logger.debug("BLE_RECONNECT: attempt $attempt / $maxRetries (delayBefore=${delayBeforeAttempt}ms)")
            val adapter = resolveBleAdapter(flowParams.context)
            if (adapter == null) {
                scheduleBleReconnectRetry(handler, ::tryReconnect)
                return
            }
            connectBleForReconnectAttempt(
                attempt = BleReconnectAttempt(
                    context = flowParams.context,
                    adapter = adapter,
                    bleAddress = flowParams.bleAddress,
                    seId = flowParams.seId,
                    completed = completed
                ),
                handler = handler,
                onSuccess = onSuccess,
                onRetry = ::tryReconnect,
                delayBeforeAttempt = delayBeforeAttempt
            )
        }
        tryReconnect()
    }

    /**
     * Returns whether reconnect retries are exhausted and triggers failure handling when they are.
     *
     * @param attempt    Current zero-based attempt count.
     * @param maxRetries Maximum allowed connect attempts.
     * @param flowParams Shared reconnect flow parameters and callbacks.
     * @return `true` when no further reconnect attempts should run.
     */
    private fun isBleReconnectExhausted(
        attempt: Int,
        maxRetries: Int,
        flowParams: BleReconnectFlowParams
    ): Boolean {
        if (attempt < maxRetries) return false
        bleReconnectTimeoutRunnable?.let { bleReconnectTimeoutHandler.removeCallbacks(it) }
        handleBleReconnectFlowFailure(flowParams)
        return true
    }

    /**
     * Resolves the system Bluetooth adapter for reconnect attempts.
     *
     * @param context Context used to access [BluetoothManager].
     * @return Bluetooth adapter, or `null` when Bluetooth is unavailable.
     */
    private fun resolveBleAdapter(context: Context) =
        context.getSystemService(BluetoothManager::class.java)?.adapter

    /**
     * Schedules another BLE reconnect attempt after a short delay.
     *
     * @param handler Main-thread handler used to post the retry.
     * @param onRetry Retry action to invoke after the delay.
     */
    private fun scheduleBleReconnectRetry(handler: Handler, onRetry: () -> Unit) {
        handler.postDelayed(onRetry, 1000)
    }

    /**
     * Performs one BLE connect attempt for the stored reconnect MAC address.
     *
     * @param attempt            Connect attempt context including adapter, address, and completion flag.
     * @param handler            Main-thread handler used to schedule retries.
     * @param onSuccess          Callback invoked on the main thread after a successful connect.
     * @param onRetry            Callback invoked to schedule the next reconnect attempt.
     * @param delayBeforeAttempt Optional delay in milliseconds before connect starts.
     */
    private fun connectBleForReconnectAttempt(
        attempt: BleReconnectAttempt,
        handler: Handler,
        onSuccess: () -> Unit,
        onRetry: () -> Unit,
        delayBeforeAttempt: Long
    ) {
        val connectAction: () -> Unit = {
            runCatching {
                clearPreviousDevicesData()
                val device = attempt.adapter.getRemoteDevice(attempt.bleAddress)
                BluetoothStateManager.connectBleDevice(attempt.context, device)
                    .orTimeout(25, TimeUnit.SECONDS)
                    .thenApply { protocol ->
                        if (attempt.completed[0]) return@thenApply null
                        BluetoothStateManager.setActiveProtocol(protocol)
                        BluetoothStateManager.addConnectedDevice(attempt.bleAddress, attempt.seId)
                        // Keep existing connected devices in state; reconnect should not
                        // force other connected device icons to switch to disconnected.
                        requireActivity().runOnUiThread(onSuccess)
                        protocol
                    }
                    .exceptionally {
                        BluetoothStateManager.disconnectActiveProtocol()
                        scheduleBleReconnectRetry(handler, onRetry)
                        null
                    }
            }.onFailure {
                scheduleBleReconnectRetry(handler, onRetry)
            }
            Unit
        }
        if (delayBeforeAttempt > 0) {
            handler.postDelayed(connectAction, delayBeforeAttempt)
        } else {
            connectAction()
        }
    }

    /**
     * Context for a single stored-address BLE reconnect attempt.
     *
     * @param context   Context for BLE connect.
     * @param adapter   System Bluetooth adapter.
     * @param bleAddress Stored BLE MAC to connect to.
     * @param seId      Secure element ID to register on success.
     * @param completed Shared completion flag for the overall reconnect flow.
     */
    private data class BleReconnectAttempt(
        val context: Context,
        val adapter: android.bluetooth.BluetoothAdapter,
        val bleAddress: String,
        val seId: String,
        val completed: BooleanArray
    )

    /**
     * Handles BLE reconnection failure.
     *
     * Responsibilities:
     * - Stop any active loading indicator
     * - Inform the user that reconnection failed
     * - Prevent further BLE-dependent operations
     */
    private fun showReconnectFailed(onOk: (() -> Unit)? = null) {
        (requireActivity() as MainActivity).showLoading(false, "")
        statusDialog(
            requireContext(),
            getString(R.string.bluetooth_reconnect_failed),
            onOk
        )
    }

    /**
     * Shows a confirmation dialog when Bluetooth is disconnected.
     *
     * This dialog asks the user whether they want to retry
     * reconnecting to the Bluetooth device.
     *
     * - YES  → Continue BLE reconnect process (onYes callback)
     * - NO   → Cancel reconnect and stay on the current screen (onNo callback)
     *
     * The dialog uses the common `display_dialog` layout and prevents
     * multiple dialogs from appearing at the same time using
     * `isStatusDialogVisible`.
     *
     * @param context Context used to create the dialog (Activity context required)
     * @param onYes Callback invoked when user selects YES
     * @param onNo Callback invoked when user selects NO
     */
    fun showBluetoothReconnectConfirmDialog(
        context: Context,
        onYes: () -> Unit,
        onNo: () -> Unit
    ) {
        if (isStatusDialogVisible) return

        requireActivity().runOnUiThread {
            val activity = context as Activity
            if (activity.isFinishing) return@runOnUiThread

            val dialogViewBinding = DialogCommonMessageBinding.inflate(layoutInflater)
            val dialog = Dialog(context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(dialogViewBinding.root)
                setCancelable(false)
            }

            isStatusDialogVisible = true
            dialogViewBinding.txtTitle.text = context.getString(R.string.text_secora_wallet)
            dialogViewBinding.txtMessage.text =
                context.getString(R.string.would_you_like_to_re_connect_bluetooth)

            dialogViewBinding.txtOK.text = context.getString(R.string.text_yes)
            dialogViewBinding.txtCancel.text = context.getString(R.string.text_no)
            dialogViewBinding.txtCancel.visibility = View.VISIBLE

            dialogViewBinding.txtOK.setOnClickListener {
                dialog.dismiss()
                isStatusDialogVisible = false
                onYes()
            }

            dialogViewBinding.txtCancel.setOnClickListener {
                dialog.dismiss()
                isStatusDialogVisible = false
                onNo()
            }

            dialog.setOnDismissListener {
                isStatusDialogVisible = false
            }

            dialog.showSecure()
        }
    }

    fun isNFC(): Boolean {
        try {
            return StorageRepository.readString(PreferenceKey.DEVICE_NAME)
                .contains(NFC_DEVICE_MODEL)
        } catch (e: Exception) {
            logger.debug(e.message.toString())
            return false;
        }
    }

    fun showNfcSheet(
        supportFragmentManager: FragmentManager,
        onCancelClick: (() -> Unit)? = null

    ) {

        if (supportFragmentManager.isStateSaved) return

        val existing =
            supportFragmentManager.findFragmentByTag("NfcScanBottomSheet")

        if (existing == null) {

            nfcSheet = NfcScanBottomSheet().apply {
                this.onCancelClick = onCancelClick
            }

            nfcSheet?.show(
                supportFragmentManager,
                "NfcScanBottomSheet"
            )
        }
    }

    fun hideNfcSheet() {
        nfcSheet?.let {
            it.isProgrammaticDismiss = true
            it.dismissAllowingStateLoss()
        }
        nfcSheet = null
    }

    /**
     * Handles session expiration and delegates error handling for non-session-related cases.
     *
     * This function checks whether the provided [errorMessage] indicates a session expiration.
     * If the session has expired, it navigates the user to the login screen via
     * [navigateToLoginScreen].
     *
     * If the error is not related to session expiration, the provided [onUnknownError] callback
     * is executed, allowing the caller to define custom error handling behavior.
     *
     * @param errorMessage The error message used to determine if the session has expired.
     * @param onUnknownError Optional lambda to handle non-session-related errors.
     * Defaults to a no-op if not provided.
     */
    protected fun handleSessionOrError(
        errorMessage: String,
        onUnknownError: () -> Unit = {}
    ) {
        if (isSessionExpired(errorMessage)) {
            navigateToLoginScreen()
        } else {
            onUnknownError()
        }
    }

    /**
     *Checks whether the error callback is for the user session timeout or not.
     *
     */
    fun isSessionExpired(error: String): Boolean {
        logger.debug(":: error :$error")
        return CommonResponse.USER_SESSION_EXPIRED.response == error
    }

    /**
     * Fetches the current child fragment of Base Fragment.
     *
     */
    private fun getCurrentFragment(): Fragment? {
        return requireActivity()
            .supportFragmentManager
            .primaryNavigationFragment
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull()
    }

    /**
     * Handles the navigation to login screen and clear the user data in case of user session timeout.
     *
     */
    fun navigateToLoginScreen(message: String = "") {
        logger.debug(":: navigateToLoginScreen")
        if (!isAdded) return
        FirebaseManager.deleteToken(true)
        activity.runOnUiThread {
            val navController = findNavController()
            val currentFragment = getCurrentFragment()
            if ((currentFragment !is LoginFragment) &&
                StorageRepository.readString(PreferenceKey.EMAIL_ID)
                    .isNotEmpty()
            ) {
                logger.debug(":: clear data and navigate To Login Screen")
                StorageRepository.apply {
                    clearString(key = PreferenceKey.EMAIL_ID)
                    clearString(key = PreferenceKey.JWT_TOKEN)
                    clearString(key = PreferenceKey.WALLET_PIN)
                    clearString(key = PreferenceKey.PROFILE_IMAGE)
                    clearString(key = PreferenceKey.USER_NAME)
                }

                navController.popBackStack()
                navController.navigate(R.id.loginFragment)
                statusDialog(
                    requireContext(),
                    if (message.isNullOrEmpty()) getString(R.string.user_session_expired) else message
                )
            }
        }
    }

    /**
     * save the login date which is required for the application user session handling..
     *
     */
    fun saveLoginDateTime() {
        val today = LocalDate.now().toString() // e.g. "2026-03-20"
        StorageRepository.saveString(PreferenceKey.LOGIN_DATE, today)
    }

    /**
     * Checks whether the application user session expiry.
     *
     */
    fun isLoginOlderThanSessionExpiryDuration(): Boolean {
        val loginDateStr = StorageRepository.readString(PreferenceKey.LOGIN_DATE)
        val loginDate = LocalDate.parse(loginDateStr)
        val today = LocalDate.now()

        val daysBetween = ChronoUnit.DAYS.between(loginDate, today)
        return daysBetween >= 15
    }

    /**
     * Clears previous BLE connection, if exists.
     * Preserves the Fission host shared SECORA channel when launched from the demo host.
     */
    fun clearPreviousDevicesData() {
        if (PayExternalLaunch.isHostLaunch() &&
            BluetoothStateManager.activeProtocol is IHostSharedBleProtocol
        ) {
            logger.debug("clearPreviousDevicesData: skip — host shared SECORA channel active")
            return
        }
        BluetoothStateManager.disconnectActiveProtocol()
        BluetoothStateManager.clearAllConnectedDevices()
    }

    fun isNullOrEmpty(byteArray: ByteArray?): Boolean = (byteArray == null || byteArray.isEmpty())

    /**
     * Runs the sequence-counter script over BLE and parses the counter from the second APDU result.
     *
     * @param onRetrieved Callback invoked with the decimal sequence counter string on success.
     * @param onFailed Callback invoked when the script fails, returns an unexpected shape, or throws.
     */
    fun fetchCurrentSequenceCounterBle(
        onRetrieved: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val currentSequenceCounterBytes = currentSequenceCounterRequest()
        if (isNullOrEmpty(currentSequenceCounterBytes)) {
            onFailed()
            return
        }
        val scriptHandler = createScriptHandler()
        scriptHandler.executeScript(currentSequenceCounterBytes).thenAccept { executionResult ->
            requireActivity().runOnUiThread {
                if (!executionResult.success || executionResult.apduResults.size != 2) {
                    onFailed()
                    return@runOnUiThread
                }
                logger.debug("Sequence Counter script executionResult.apduResults.size : ${executionResult.apduResults.size}")

                val result = executionResult.apduResults[1]
                logger.debug("Sequence Counter script result.hexResponse : ${result.hexResponse}")
                if (result.hexResponse == null) {
                    onFailed()
                    return@runOnUiThread
                }
                val hex = result.hexResponse
                logger.debug("Sequence Counter script hex.trim() : ${hex.trim()}")
                val parts = hex.trim().split(" ")
                logger.debug("Sequence Counter script parts : ${parts}")
                logger.debug("Sequence Counter script parts.size : ${parts.size}")

                if (parts.size == 7) {
                    val valueDecimal = (parts[2] + parts[3] + parts[4]).toInt(16)

                    logger.debug("Sequence Counter script (parts[2]+parts[3]+parts[4]) : ${(parts[2] + parts[3] + parts[4])}")
                    logger.debug("Sequence Counter script valueDecimal : $valueDecimal")
                    onRetrieved(valueDecimal.toString())
                } else {
                    onFailed()
                }

                logger.debug("Sequence Counter script after register: success=${executionResult.success}")
            }
        }.exceptionally { throwable ->
            requireActivity().runOnUiThread {
                onFailed()
                logger.debug("Sequence Counter  script after register failed: ${throwable.message}")
            }
            null
        }

    }

    /**
     * Loads the sequence-counter APDU script from application assets and returns it as UTF-8 bytes.
     *
     * The JSON is validated with [JSONObject] before the raw bytes are returned for script execution.
     *
     * @return The file contents as a byte array, or `null` if the asset is missing, unreadable, or not valid JSON.
     */
    private fun currentSequenceCounterRequest(): ByteArray? {
        return try {
            requireContext().assets.open(SEQUENCE_COUNTER_ASSET).use { input ->
                val bytes = input.readBytes()
                JSONObject(String(bytes, StandardCharsets.UTF_8))
                bytes
            }
        } catch (e: JSONException) {
            logger.debug("Invalid sequence-counter asset: ${e.message}")
            null
        } catch (e: Exception) {
            logger.debug("Unable to read sequence counter request from assets: ${e.message}")
            null
        }
    }

    /**
     * Creates and returns a [ScriptHandler] instance with required callbacks.
     *
     * Uses [requireContext] for initialization. Handles toast display and log updates while
     * delegating loading UI to the main activity (the [showLoading] callback is intentionally empty).
     *
     * @return Configured [ScriptHandler] instance for flows that use the default fragment context.
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
     * Resolves the current sequence counter from the wearable, using BLE or NFC depending on connectivity.
     *
     * When the SEI TSM flow is disabled, invokes [onRetrieved] with a fixed fallback value without accessing the device.
     *
     * @param onRetrieved Callback invoked with the decimal sequence counter string on success.
     * @param onFailed Callback invoked when the counter cannot be read or the operation is cancelled.
     */
    fun fetchSequenceNumberFromDevice(
        onRetrieved: (String) -> Unit,
        onFailed: () -> Unit,
        onCancelled: (() -> Unit)? = null,
        allowReconnectPrompt: Boolean = true,
        rePromptOnReconnectFailure: Boolean = false
    ) {
        if (!isSeiTsmFlowEnabled) {
            onRetrieved("161")
            return
        }

        val runFetch = {
            if (!isNFC()) {
                ensureBleConnectedThenRun(
                    onConnected = {
                        fetchCurrentSequenceCounterBle(onRetrieved, onFailed)
                    },
                    onCancelled = onCancelled ?: onFailed,
                    allowReconnectPrompt = allowReconnectPrompt,
                    rePromptOnReconnectFailure = rePromptOnReconnectFailure
                )
            } else {
                fetchCurrentSequenceCounterNfc(onRetrieved, onFailed)
            }
        }

        if (!isNFC() && SecureElementScriptCoordinator.isScriptRunning()) {
            logger.debug("fetchSequenceNumberFromDevice: waiting for in-flight BLE script to finish")
            viewLifecycleOwner.lifecycleScope.launch {
                if (awaitBleScriptIdleThenProceed()) {
                    runFetch()
                }
            }
            return
        }

        runFetch()
    }

    /**
     * Waits for any in-flight BLE script to finish; resets BLE if still stuck.
     *
     * @return `true` when the fragment is still added and the fetch may continue.
     */
    private suspend fun awaitBleScriptIdleThenProceed(): Boolean {
        SecureElementScriptCoordinator.awaitIdle()
        if (!isAdded) return false
        if (SecureElementScriptCoordinator.isScriptRunning()) {
            logger.debug("fetchSequenceNumberFromDevice: script still active after wait, resetting BLE")
            SecureElementScriptCoordinator.forceResetActiveScripts()
            // Host shared GATT must stay up; releaseSharedChannel is handled separately.
            if (BluetoothStateManager.activeProtocol !is IHostSharedBleProtocol) {
                BluetoothStateManager.disconnectActiveProtocol()
            }
        }
        return true
    }

    /**
     * Runs the sequence-counter script over NFC and parses the counter from the second APDU response.
     *
     * @param onRetrieved Callback invoked with the decimal sequence counter string on success.
     * @param onFailed Callback invoked when NFC is cancelled, the script fails, or the response cannot be parsed.
     */
    fun fetchCurrentSequenceCounterNfc(
        onRetrieved: (String) -> Unit,
        onFailed: () -> Unit
    ) {

        val currentSequenceCounterBytes = currentSequenceCounterRequest()
        if (isNullOrEmpty(currentSequenceCounterBytes)) {
            onFailed()
            return
        }
        showNfcSheet(parentFragmentManager, onCancelClick = { onFailed() })
        val currentSequenceCounterResponses = mutableListOf<String>()
        NfcScriptExecutionTracker.onNfcScriptStarted()
        SecoraWearableSDK.getInstance().getInterface().executeNfcOperation(
            requireActivity(),
            SCRIPT,
            currentSequenceCounterBytes,
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
                    logger.debug("SE_ID: $seid")
                }

                override fun onApduProgress(request: String?, response: String?) {
                    logger.debug("APDU REQUEST=$request \n RESPONSE=$response")
                    if (response?.isNotEmpty() == true) {
                        currentSequenceCounterResponses.add(response)
                    }
                }

                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onSuccess(
                    responseItems: MutableList<ApduResponsesItem>?,
                    completed: Boolean
                ) {
                    hideNfcSheet()
                    if (!completed) {
                        logger.debug("Cleanup NFC progress callback received, waiting for completion")
                        return
                    }
                    NfcScriptExecutionTracker.onNfcScriptFinished()
                    if (currentSequenceCounterResponses.isEmpty()) {
                        logger.debug("currentSequenceCounterResponses : $currentSequenceCounterResponses")
                        onFailed()
                        return
                    }

                    if (currentSequenceCounterResponses.size == 2) {
                        val result = currentSequenceCounterResponses[1]
                        logger.debug("Sequence Counter script result : ${result}")
                        logger.debug("Sequence Counter script hex.trim() : ${result.trim()}")
                        val parts = result.trim().split(" ")
                        logger.debug("Sequence Counter script parts : ${parts}")
                        logger.debug("Sequence Counter script parts.size : ${parts.size}")

                        if (parts.size == 7) {
                            val valueDecimal = (parts[2] + parts[3] + parts[4]).toInt(16)

                            logger.debug("Sequence Counter script (parts[2]+parts[3]+parts[4]) : ${(parts[2] + parts[3] + parts[4])}")
                            logger.debug("Sequence Counter script valueDecimal : $valueDecimal")
                            onRetrieved(valueDecimal.toString())
                        } else {
                            onFailed()
                        }

                    } else {
                        onFailed()
                    }
                }

                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onError(error: String) {
                    logger.debug("onError: $error")
                    NfcScriptExecutionTracker.onNfcScriptFinished()
                    hideNfcSheet()
                    onFailed()
                }
            })
    }

    /**
     * Validates whether a Cognito user session already exists.
     *
     * If a session is found, the current user is signed out before
     * continuing with the requested authentication operation.
     *
     */
    fun validateUserSession(onCompleted: () -> Unit) {

        CognitoSignInFlowCoordinator.checkForUserSession({isUserFound ->
            if (isUserFound) {
                proceedWithCognitoLogoutFlow(onCompleted)
            } else {
                onCompleted()
            }

        },{
            onCompleted()
        })
    }

    /**
     * Signs out the current Cognito user and continues with the
     * requested authentication operation.
     *
     */
    private fun proceedWithCognitoLogoutFlow(onCompleted: () -> Unit) {
        CognitoSignInFlowCoordinator.signOut( {
            activity.runOnUiThread {
                    onCompleted()
            }
        },
            { errorMessage ->
                activity.runOnUiThread {
                    activity.showLoading(false, "")
                    statusDialog(requireActivity(), errorMessage)
                }

            })
    }

    /**
     * Requests a new account verification code for the specified user.
     *
     * @param username User account identifier.
     * @param onSuccess Invoked when the verification code is resent.
     * @param onFailure Invoked when the operation fails.
     */
    fun handleResendConfirmationCodeFlow(username : String, onSuccess: (Boolean) -> Unit,
                                         onFailure: (String?) -> Unit) {
        CognitoSignInFlowCoordinator.resendConfirmationCode(username,
            {
                onSuccess(it)
            },
            {
                onFailure(it)
            })
    }

    /**
     * fcm suspend event to check if its received for current device.
     */
    fun isNotificationForCurrentDevice(event: AppEvent): Boolean {
        val receivedDeviceName = event.getStringExtra(BundleKey.DEVICE_NAME)
        val wearableName = StorageRepository.readString(PreferenceKey.DEVICE_NAME)
        logger.debug("deviceStatus :: detail screen device:" + wearableName + " noti device:" + receivedDeviceName)
        return receivedDeviceName == wearableName
    }

    /**
     * Handle fcm suspend related events.
     * check if its for current device .If so navigate to card listing screen.
     */
    fun handleSuspendNotification(event: AppEvent) {
        if (isNotificationForCurrentDevice(event)) {
            val bundle = Bundle().apply {
                putString(ACTION_DEVICE_STATUS_UPDATE, event.getStringExtra(BundleKey.DEVICE_NAME))
            }
            findNavController().navigate(R.id.deviceListFragment, bundle)
        }
    }

    /**
     * Handle actions via intent.
     */
    fun launchIntent(action:String, uri:String?){
        val intent = Intent(action).apply {
            data = uri?.toUri()
        }
        context?.startActivity(intent)
    }
}
