// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: LoginFragment.kt manages user authentication, handling both email/password and Google sign-in flows.
 * It validates inputs, requests necessary permissions, and interacts with SecoraWalletSDK for login.
 * and provides dynamic environment and URL configuration options.
 **/
package com.infineon.secora.wallet.ui.fragment

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.PayExternalLaunch
import com.infineon.secora.wallet.client.operations.middleware.service.SecoraWalletSDK
import com.infineon.secora.wallet.client.util.AppDispatchers
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.FragmentAuthLoginBinding
import com.infineon.secora.wallet.domain.walletsdk.WalletRepository
import com.infineon.secora.wallet.firebase.FirebaseManager
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.oidc.CognitoSignInFlowCoordinator
import com.infineon.secora.wallet.oidc.GoogleSignInFlowCoordinator
import com.infineon.secora.wallet.oidc.MicrosoftSignInFlowCoordinator
import com.infineon.secora.wallet.oidc.OIDCFLowHelper.OidcLoginType
import com.infineon.secora.wallet.oidc.OIDCFLowHelper.isValidPassword
import com.infineon.secora.wallet.oidc.OIDCFLowHelper.validateEmailCredentials
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.utils.Utils.isNetworkAvailable
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.constants.Constants.SUCCESS
import com.infineon.secora.wallet.utils.helper.UIHelper
import com.infineon.secora.wallet.utils.hostedui.HostedUILanguage
import com.infineon.secora.wearable.SecoraWearableSDK
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * LoginFragment is used for login
 *
 */
class LoginFragment : BaseFragment() {

    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)
    private lateinit var binding: FragmentAuthLoginBinding
    private lateinit var activity: MainActivity
    private lateinit var currentPermission: String
    private lateinit var credentialManager: CredentialManager
    private var selectedOidcType: OidcLoginType = OidcLoginType.NONE

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                handleNextPermission(selectedOidcType)
            } else {
                showToast("$currentPermission " + getString(R.string.permission_denied))
            }
        }

    private val requiredPermissions = emptyList<String>()
    private var permissionIndex = 0
    private var enableBackPress = false

    /**
     * onCreateView method is used to inflate the layout for this fragment
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAuthLoginBinding.inflate(inflater, container, false)
        activity = (requireActivity() as MainActivity)
        activity.binding.toolbar.profileIcon.visibility = View.GONE
        credentialManager = CredentialManager.create(requireContext())
        dismissKeyboardOnTap(requireActivity(), binding.root)
        initListeners()
        setupBackPressedCallback()
        val walletSdkVersion = SecoraWalletSDK.getInstance()?.getSDKVersion()
        val wearableSdkVersion = SecoraWearableSDK.getInstance().getSDKVersion()
        logger.debug("Using walletSdkVersion : $walletSdkVersion")
        logger.debug("Using wearableSdkVersion : $wearableSdkVersion")
        return binding.root
    }

    /**
     * retrieve fcm token before calling login api.
     */
    private fun retrieveFcmToken(email: String) {

        val fcmToken = StorageRepository.readString(PreferenceKey.FCM_TOKEN)
        if (fcmToken.isNotEmpty()) {
            logger.debug("fcm :: login token present")
            walletServerLoginFlow(email, fcmToken)
            return
        }
        FirebaseManager.fetchToken(
            { token ->
                logger.debug("fcm :: login Token: $token")
                StorageRepository.saveString(PreferenceKey.FCM_TOKEN, token)
                walletServerLoginFlow(email, token)
            }, { error ->
                statusDialog(activity, error ?: getString(R.string.failed_to_fetch_fcm_token))
                activity.showLoading(false, "")
                logger.debug("fcm :: login Token failed to fetch: $error")
            })
    }

    /**
     * Handles back press action events.
     *
     */
    private fun setupBackPressedCallback() {
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (PayExternalLaunch.isHostLaunch()) {
                        PayExternalLaunch.exitToHost(activity)
                        return
                    }
                    if (!enableBackPress) {
                        enableBackPress = true
                        return
                    }
                    activity.finish()
                }
            })
    }

    /**
     *  onViewCreated method is used to initialize the views for this fragment
     *
     * @param view
     * @param savedInstanceState
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.binding.toolbar.profileIcon.visibility = View.GONE
    }

    /**
     * Validates login form inputs and initiates the Cognito
     * sign-in flow when all validations pass.
     */
    private fun handleLoginButtonClickEvent() {
        if (!validateEmailCredentials(requireContext(), binding.etUsername)) {
            return
        }
        if (!isValidPassword(requireContext(), binding.etPassword.text.toString())) {
            return
        }
        handleOidcLogin(OidcLoginType.COGNITO_LOGIN)
    }

    /**
     * Toggles visibility of the login password field and updates
     * the visibility icon while preserving the cursor position.
     */
    private fun handleLoginPasswordVisibilityToggle() {

        isPasswordVisible = !isPasswordVisible

        if (isPasswordVisible) {
            binding.etPassword.transformationMethod =
                HideReturnsTransformationMethod.getInstance()
            binding.ivToggleEye.setImageResource(R.drawable.eye_open)
        } else {
            binding.etPassword.transformationMethod =
                PasswordTransformationMethod.getInstance()
            binding.ivToggleEye.setImageResource(R.drawable.eye_slash)
        }

        // Keep cursor at end
        binding.etPassword.setSelection(
            binding.etPassword.text?.length ?: 0
        )

    }

    /**
     * Initializes click listeners for UI elements within this fragment.
     * Handles navigation, login flows (Google and email), and validations.
     */
    private fun initListeners() {

        binding.btnGoogleLogin.setOnClickListener {
            handleOidcLogin(OidcLoginType.GOOGLE_LOGIN)
        }

        binding.btnLogin.setOnClickListener {
            handleLoginButtonClickEvent()
        }

        binding.btnMicrosoftLogin.setOnClickListener {
          handleOidcLogin(OidcLoginType.MICROSOFT_LOGIN)
        }

        binding.tvRegister.setOnClickListener {
            handleOidcLogin(OidcLoginType.COGNITO_REGISTER)
        }

        binding.ivToggleEye.setOnClickListener {
            handleLoginPasswordVisibilityToggle()
        }

        binding.tvForgotPassword.setOnClickListener {
            handleOidcLogin(OidcLoginType.COGNITO_FORGOT_PWD)
        }

        binding.textviewOem.setOnClickListener {
            navigateToOemFragment()
        }

        binding.pencil.setOnClickListener {
            navigateToOemFragment()
        }
    }

    /** Handles the Google login button click */
    private fun handleOidcLogin(oidcLoginType: OidcLoginType) {
        selectedOidcType = oidcLoginType

        when {
            !isNetworkAvailable(requireContext()) -> {
                activity.showLoading(false, "")
                confirmDataDialog(getString(R.string.data_enable))
            }

            else -> startSequentialPermissionCheck(oidcLoginType)
        }
    }

    /** Navigates to the OEM fragment and hides the profile icon */
    private fun navigateToOemFragment() {
        findNavController().navigate(R.id.oemFragment)
        activity.binding.toolbar.profileIcon.visibility = View.GONE
    }

    /**
     * Navigates the user to the appropriate screen based on wallet pin status.
     */
    private fun navigateBasedOnWalletPin() {
        if (!::activity.isInitialized || activity.isFinishing) return

        val walletPin = StorageRepository.readString(PreferenceKey.WALLET_PIN)

        activity.runOnUiThread {
            activity.requestWalletBiometricAfterLogin()
            if (walletPin.isEmpty()) {
                findNavController().navigate(R.id.createwalletpin)
            } else {
                findNavController().navigate(R.id.enterWalletPin)
            }
        }
    }

    /**
     * Handles the Google-based login flow using the SecoraWalletSDK and processes the user session.
     *
     * This function initiates user authentication via the `userLogin()` method provided
     * by `SecoraWalletSDK`. On a successful response, it:
     *
     * - Save user-specific information (User ID, Email, Password, Wallet ID, Environment mode) into shared preferences.
     * - Checks for an existing wallet PIN in preferences.
     *   - If no PIN is found, navigate to the "Create Wallet PIN" screen.
     *   - If a PIN exists, navigate to the "Enter Wallet PIN" screen.
     * - If login fails, shows an error dialog with the returned status message.
     *
     * On error (e.g., network issues or SDK errors), it shows a failure dialog with the error message.
     *
     * UI operations (like navigation or dialogs) are safely executed on the main thread.
     *
     * Dependencies:
     * - `logger`: For logging debug information.
     * - Navigation: Uses `findNavController()` to route to next screens based on login outcome.
     * - `statusDialog()`: Displays feedback to the user in case of errors or failures.
     */
    private fun walletServerLoginFlow(email: String, fcmToken: String) {
        if (fcmToken.isBlank()) {
            statusDialog(activity, getString(R.string.failed_to_fetch_fcm_token))
            activity.showLoading(false, "")
            return
        }
        lifecycleScope.launch {
            if (!::activity.isInitialized || activity.isFinishing) return@launch
            val oauthToken = StorageRepository.readString(PreferenceKey.JWT_TOKEN).takeIf { it.isNotBlank() }
            val sdkResult = WalletRepository.userLogin(email = email, idToken = oauthToken, fcmToken = fcmToken)
            withContext(AppDispatchers.MAIN) {
                if (sdkResult.isSuccess) {
                    sdkResult.response?.let { userResponseBody ->
                        handleWalletServerLoginSuccess(userResponseBody.statusMessage.toString(), email)
                    } ?: run {
                        activity.showLoading(false, "")
                        statusDialog(activity, getString(R.string.no_response_data_received))
                    }
                } else {
                    activity.showLoading(false, "")
                    statusDialog(activity, sdkResult.errorMessage)
                }
            }
        }
    }

    /**
     * Handles a successful Wallet Server login response.
     *
     * @param statusMessage The statusMessage from response object returned from the Google login process.
     */
    private fun handleWalletServerLoginSuccess(statusMessage: String, email: String) {
        if (statusMessage.equals(SUCCESS, ignoreCase = true)) {
            if (email.isNotEmpty()) {
                StorageRepository.saveString(PreferenceKey.EMAIL_ID, email)
            }
            setScreenConfiguration()
            saveLoginDateTime()
            navigateBasedOnWalletPin()
        } else {
            activity.showLoading(false, "")
            statusDialog(activity, statusMessage)
        }
    }

    /**
     * This function will call setScreenConfiguration of SecoraWalletSDK to set Hosted Screen Settings
     * Based on this hosted UI will be displayed.
     */
    private fun setScreenConfiguration() {
        lifecycleScope.launch {
            if (activity.isFinishing) return@launch
            WalletRepository.setScreenConfiguration(context = activity, HostedUILanguage.ENGLISH)
        }
    }

    /**
     * startSequentialPermissionCheck(): start sequential permission check
     *
     */
    private fun startSequentialPermissionCheck(oidcLoginType: OidcLoginType) {
        permissionIndex = 0
        handleNextPermission(oidcLoginType)
    }

    /**
     * handleNextPermission(): handle next permission
     *
     */
    private fun handleNextPermission(oidcLoginType: OidcLoginType) {
        if (permissionIndex < requiredPermissions.size) {
            currentPermission = requiredPermissions[permissionIndex]
            if (!checkPermission(currentPermission)) {
                permissionLauncher.launch(currentPermission)
            } else {
                permissionIndex++
                handleNextPermission(oidcLoginType)
            }
        } else {
            UIHelper.hideKeyboard(requireView(), requireContext())
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            // Amplify Auth, Credential Manager, and UI must run on the main thread; IO here broke sign-in callbacks.
            lifecycleScope.launch {
                when (oidcLoginType) {
                    OidcLoginType.COGNITO_REGISTER -> navigateToRegisterScreen()
                    OidcLoginType.COGNITO_FORGOT_PWD -> navigateToResetPasswordScreen()
                    OidcLoginType.GOOGLE_LOGIN -> handleGoogleOidcFlow()
                    OidcLoginType.COGNITO_LOGIN -> handleCognitoLoginOidcFlow()
                    OidcLoginType.MICROSOFT_LOGIN -> handleMicrosoftLoginOidcFlow()
                    else -> {
                        statusDialog(requireActivity(), getString(R.string.invalid_operation))
                    }
                }
            }
        }
    }

    /**
     * Initiates the Google Sign-In authentication flow.
     *
     * Authenticates the user through Google Identity Services and
     * proceeds with wallet server login upon successful authentication.
     */
    private fun handleGoogleOidcFlow() {
        activity.showLoading(true, getString(R.string.text_please_wait))
        GoogleSignInFlowCoordinator.loginWithGoogle(requireActivity(), { email ->
            retrieveFcmToken(email)
        }, { errorMessage ->
            activity.showLoading(false, "")
            statusDialog(requireActivity(), errorMessage)
        })
    }

    /**
     * Starts the Microsoft login flow after validating any existing user session.
     */
    private fun handleMicrosoftLoginOidcFlow() {
        activity.showLoading(true, getString(R.string.text_please_wait))
        MicrosoftSignInFlowCoordinator.checkUserSession({ isUserFound ->
            if (isUserFound) {
                proceedWithMicrosoftLogOutFlow()
            } else {
                proceedWithMicrosoftLoginFlow()
            }
        }, { errorMessage ->
            activity.runOnUiThread {
                activity.showLoading(false, "")
                statusDialog(requireActivity(), errorMessage)
            }

        })
    }

    /**
     * Performs Microsoft user authentication.
     */
    private fun proceedWithMicrosoftLoginFlow() {
        MicrosoftSignInFlowCoordinator.login(requireActivity(), { email ->
            retrieveFcmToken(email)
        }, { errorMessage ->
            activity.runOnUiThread {
                activity.showLoading(false, "")
                statusDialog(requireActivity(), errorMessage)
            }

        })
    }

    /**
     * Performs Microsoft user logout.
     */
    private fun proceedWithMicrosoftLogOutFlow() {
        MicrosoftSignInFlowCoordinator.logout({
            proceedWithMicrosoftLoginFlow()
        }, { errorMessage ->
            activity.runOnUiThread {
                activity.showLoading(false, "")
                statusDialog(requireActivity(), errorMessage)
            }

        })
    }

    /**
     * Starts the Cognito login flow after validating any existing user session.
     */
    private fun handleCognitoLoginOidcFlow() {
        activity.showLoading(true, getString(R.string.text_please_wait))
        validateUserSession({
            proceedWithCognitoLoginFlow()
        })
    }

    /**
     * Performs Cognito user authentication using the provided
     * username and password credentials.
     */
    private fun proceedWithCognitoLoginFlow() {

        val userName = binding.etUsername.text.toString()
        val password = binding.etPassword.text.toString()
        CognitoSignInFlowCoordinator.signIn(
            userName, password, { isSuccess ->
                if (!isSuccess) {
                    handleResendConfirmationCodeFlow(
                        userName, {
                            activity.runOnUiThread {
                                activity.showLoading(false, "")
                                val bundle = Bundle().apply {
                                    putString(BundleKey.USERNAME, userName)
                                    putString(BundleKey.OPERATION_TYPE, OidcLoginType.COGNITO_LOGIN.name)
                                }
                                findNavController().navigate(
                                    R.id.verifyCodeFragment, bundle
                                )
                            }
                        },
                        { errorMessage ->
                            activity.runOnUiThread {
                                activity.showLoading(false, "")
                                statusDialog(requireActivity(), errorMessage)
                            }
                        })
                    return@signIn
                }
                activity.runOnUiThread {
                    handleCognitoSuccessFlow()
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
     * Completes the Cognito authentication process by retrieving
     * token details and initiating wallet server authentication.
     */
    private fun handleCognitoSuccessFlow() {
        CognitoSignInFlowCoordinator.fetchTokenDetails({ email ->
            retrieveFcmToken(email)
        }, { errorMessage ->
            activity.runOnUiThread {
                activity.showLoading(false, "")
                statusDialog(requireActivity(), errorMessage)
            }
        })
    }

    private fun navigateToRegisterScreen() {
        findNavController().navigate(
            R.id.registerFragment
        )
    }

    /**
     * Navigates to the password reset screen and initializes
     * the first step of the reset workflow.
     */
    private fun navigateToResetPasswordScreen() {
        findNavController().navigate(
            R.id.forgotPasswordFragment
        )
    }

}
