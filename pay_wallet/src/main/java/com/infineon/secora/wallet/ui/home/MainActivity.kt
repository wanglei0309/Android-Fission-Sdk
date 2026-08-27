// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: MainActivity.kt is the central controller of the app that manages navigation, toolbar, and bottom navigation
 * across Home, Health, and Wallet sections. It also handles user session setup, loading overlays, profile icon
 * updates, and secure navigation between fragments.
 **/
package com.infineon.secora.wallet.ui.home

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.infineon.secora.wallet.BuildConfig
import com.infineon.secora.wallet.PayExternalLaunch
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.utils.helper.ConfiguredWalletIdentity
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.ActivityMainBinding
import com.infineon.secora.wallet.databinding.DialogCommonMessageBinding
import com.infineon.secora.wallet.domain.devicedetach.DeviceDetachUpdateHandler
import com.infineon.secora.wallet.domain.devicedetach.FcmDeletedCardHandler
import com.infineon.secora.wallet.domain.walletsdk.WalletRepository
import com.infineon.secora.wallet.firebase.AppEvent
import com.infineon.secora.wallet.firebase.EventBus
import com.infineon.secora.wallet.firebase.FirebaseManager
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.ui.fragment.CardListFragment
import com.infineon.secora.wallet.ui.fragment.BaseFragment
import com.infineon.secora.wallet.ui.fragment.CreateWalletFragment
import com.infineon.secora.wallet.ui.fragment.EnterWalletFragment
import com.infineon.secora.wallet.ui.fragment.HomeFragment
import com.infineon.secora.wallet.utils.helper.DigitizationDeleteFlowGate
import com.infineon.secora.wallet.utils.helper.FcmSecureFlowCoordinator
import com.infineon.secora.wallet.utils.helper.ScreenCaptureProtection
import com.infineon.secora.wallet.utils.helper.showSecure
import com.infineon.secora.wallet.utils.StringUtils.isEmptyString
import com.infineon.secora.wallet.utils.helper.UIHelper
import com.infineon.secora.wallet.utils.Utils
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_CARD
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_FORCE_LOGOUT
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_NAVIGATE_LISTENER
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_OPEN_DEVICE_LIST
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_TOGGLE
import com.infineon.secora.wallet.utils.constants.Constants.AVATAR_BLUE_COLOR
import com.infineon.secora.wallet.utils.constants.Constants.DELETED_CARD
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * MainActivity is the entry point of the application. It handles navigation setup,
 * manages UI state transitions, and responds to user actions through the bottom navigation.
 */
class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private var email: String = ""
    private var jwtToken: String = ""
    private var walletPin: String = ""
    private var profileImage: String = ""
    private var userName: String = ""
    private var enableBiometric = false
    private var backPressedCallback: OnBackPressedCallback? = null
    private var currentVisibleFragmentId: Int = R.id.nav_host_fragment_home
    private lateinit var navHostFragment: NavHostFragment
    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)
    private var isBlurShown = false
    private val blurOverlayGeneration = AtomicLong(0L)
    private var lastWalletTabClickMs: Long = 0L

    companion object {
        private const val WALLET_TAB_TAP_DEBOUNCE_MS = 700L

        /** Wallet 顶层页显示「返回Demo首页」；进入开卡/卡片详情等子页时隐藏，避免与 Toolbar 返回键重叠。 */
        private val HOST_EXIT_VISIBLE_DESTINATIONS = setOf(
            R.id.deviceListFragment,
            R.id.enterWalletPin,
            R.id.loginFragment,
            R.id.createwalletpin,
            R.id.confirmwalletpin,
        )
    }

    // Cache for initials bitmaps to avoid recreating on every call
    private val initialsCache = mutableMapOf<String, Bitmap>()

    private var portalDelinkSuccessDialog: Dialog? = null
    private var hostExitButton: TextView? = null
    private var hostExitBackCallback: OnBackPressedCallback? = null

    /**
     * Initializes the activity, sets up the navigation graph and bottom navigation.
     *
     * @param savedInstanceState the saved instance state from a previous configuration (if any)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // 仅作用于 Wallet Activity，避免 setDefaultNightMode 触发宿主 MainActivity recreate 并断 BLE。
        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
        super.onCreate(savedInstanceState)
        ScreenCaptureProtection.apply(this)

        // Start EventBus collection ONCE for the whole Activity lifecycle
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                EventBus.events.collect { event ->
                    if (event.action == ACTION_FORCE_LOGOUT) {
                        (getCurrentFragment() as? BaseFragment)?.navigateToLoginScreen(getString(R.string.max_device_login_reached))
                        return@collect
                    }
                    if (Utils.isUserNotLoggedIn()) {
                        logger.debug(" :: Event Bus : User not logged in")
                        FirebaseManager.deleteToken(StorageRepository.readString(PreferenceKey.FCM_TOKEN).isNotEmpty())
                        return@collect
                    }
                    when (event.action) {
                        ACTION_NAVIGATE_LISTENER -> handleDeviceDetachNotification(event)
                        ACTION_OPEN_DEVICE_LIST -> handleOpenDeviceListEvent()
                        ACTION_CARD, ACTION_TOGGLE -> handleFcmCardDeletedNotification(event)
                    }
                }
            }
        }

        initializeActivity()
        setupStatusBarColor()
        loadUserPreferences()
        setupNavigation()
        setupToolbar()
        setupBottomNavigation()
        setupActionBar()
        handleNotificationNavigation(intent)
        PayExternalLaunch.handleIfNeeded(this, intent)
        ConfiguredWalletIdentity.seedHardcodedIdentity(this)
        lifecycleScope.launch {
            WalletRepository.syncOemDetailsFromPreferences(this@MainActivity)
        }
        setupHostExitIfNeeded()
    }

    private fun handleNotificationNavigation(intent: Intent?) {
        val digitizationReferenceNumber = intent?.getStringExtra(BundleKey.ENTITY_ID)
        logger.debug(":: MainScreen Fetching intent Data")
        if (digitizationReferenceNumber == null)
            return

        logger.debug(":: MainScreen Notification Flow")
        val isUserLoggedIn = StorageRepository.readString(PreferenceKey.EMAIL_ID).isNotEmpty()
            && StorageRepository.readString(PreferenceKey.WALLET_PIN).isNotEmpty()
        logger.debug(":: MainScreen isUserLoggedIn : $isUserLoggedIn")

        lifecycleScope.launch {
            val card = WalletRepository.getLocalCardDetailsOrRefreshFromProvisionApi(
                this@MainActivity,
                digitizationReferenceNumber
            )
            if (card.digitizationReferenceNumber != null && isUserLoggedIn) {
                logger.debug(":: MainScreen Notification Navigation with Data")
                val walletNavHost =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment_wallet) as NavHostFragment
                val navController = walletNavHost.navController

                binding.bottomNavigationViewMain.selectedItemId = R.id.menu_pay
                val bundle = Bundle().apply {
                    putString(BundleKey.ENTITY_ID, digitizationReferenceNumber)
                }
                val currentDestination =
                    navHostFragment.findNavController().currentDestination?.id
                if (currentDestination == R.id.enterWalletPin) {
                    logger.debug(":: MainScreen Notification Flow : Two Step Flow")
                    navController.navigate(
                        R.id.enterWalletPin, bundle
                    )
                } else {
                    val detailBundle = Utils.prepareBundleWithCardDetails(card)
                    StorageRepository.saveString(PreferenceKey.DIGITIZATION_REFERENCE_NUMBER, card.digitizationReferenceNumber.toString())
                    card.paymentAppInstanceId?.takeIf { it.isNotBlank() }?.let { pid ->
                        StorageRepository.saveString(PreferenceKey.PAYMENT_APP_INSTANCE_ID, pid)
                    }
                    logger.debug(":: MainScreen Notification Flow : One Step Flow")
                    navController.navigate(
                        R.id.detailFragment, detailBundle
                    )
                }
            }
        }
    }

    /**
     * Triggered when new intent has been received.
     *
     * @param intent, the new intent
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        logger.debug(":: onNewIntent  intent")
        handleNotificationNavigation(intent)
        PayExternalLaunch.handleIfNeeded(this, intent)
    }

    /** 当前底部 Tab 是否为 Wallet（支付开卡）容器。 */
    fun isWalletTabVisible(): Boolean =
        currentVisibleFragmentId == R.id.nav_host_fragment_wallet

    /**
     * Returns the current(visible) Fragment from the FragmentManager.
     *
     */
    fun getCurrentFragment(): Fragment? {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_wallet) as? NavHostFragment
        return navHostFragment?.childFragmentManager?.fragments?.firstOrNull { it.isVisible }
    }

    /**
     * Sets up the status bar color and adjusts padding to ensure proper layout
     * across different Android versions.
     *
     * - For Android 15 (API 35) and above, it uses a window inset listener to
     *   dynamically apply padding equal to the status bar height and sets the
     *   background color on the `appBarLayout`.
     *
     * - For devices below Android 15, the status bar color is applied directly
     *   using `window.statusBarColor`.
     *
     * This ensures a consistent appearance of the status bar across all supported
     * versions of Android.
     */
    private fun setupStatusBarColor() {
        val colorWhite = ContextCompat.getColor(this, R.color.white)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(0, statusBarHeight, 0, 0)
            view.setBackgroundColor(colorWhite)
            insets
        }
    }

    /**
     * Initializes the activity with required configurations and view setup.
     */
    private fun initializeActivity() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableBiometric = false
        getPackageInfo()
    }

    /**
     * Retrieves and logs the app's package information.
     */
    private fun getPackageInfo() {
        val packageInfo =
            applicationContext.packageManager.getPackageInfo(
                applicationContext.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        logger.debug("packageInfo$packageInfo")
    }

    /**
     * Loads the stored user preferences from storage.
     *
     * - Retrieves saved user credentials and profile data such as Email, Password, Wallet PIN etc.
     * - Stores retrieved values in corresponding local variables for later use.
     */
    private fun loadUserPreferences() {
        email = StorageRepository.readString(PreferenceKey.EMAIL_ID)
        walletPin = StorageRepository.readString(PreferenceKey.WALLET_PIN)
        jwtToken = StorageRepository.readString(PreferenceKey.JWT_TOKEN)
        profileImage = StorageRepository.readString(PreferenceKey.PROFILE_IMAGE)
        userName = StorageRepository.readString(PreferenceKey.USER_NAME)
    }

    /**
     * Sets up the app's navigation graph and initializes navigation handling.
     */
    private fun setupNavigation() {
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_wallet) as NavHostFragment
        val navController = navHostFragment.findNavController()
        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.navigation_graph)

        val startDestination = determineStartDestination()
        graph.setStartDestination(startDestination)

        navController.setGraph(graph, null)

        setupDestinationChangeListener(navController)
    }

    /**
     * Determines the start destination for the navigation graph.
     *
     * @return the ID of the appropriate start destination.
     */
    private fun determineStartDestination(): Int {
        return if (jwtToken.isEmpty()) {
            determineStartDestinationWithoutToken()
        } else {
            determineStartDestinationWithToken()
        }
    }

    /**
     * Determines the start destination when no JWT token is available (unauthenticated state).
     *
     * @return the ID of the start destination for unauthenticated users.
     */
    private fun determineStartDestinationWithoutToken(): Int {
        return when {
            email.isNotEmpty() && walletPin.isNotEmpty() -> R.id.enterWalletPin
            email.isNotEmpty() && walletPin.isEmpty() -> R.id.createwalletpin
            email.isEmpty() -> R.id.loginFragment
            else -> R.id.loginFragment
        }
    }

    /**
     * Determines the start destination when a valid JWT token exists (authenticated state).
     *
     * @return ID of the start destination for authenticated users.
     */
    private fun determineStartDestinationWithToken(): Int {
        return when {
            email.isNotEmpty() && walletPin.isNotEmpty() -> R.id.enterWalletPin
            email.isNotEmpty() && walletPin.isEmpty() -> R.id.createwalletpin
            email.isEmpty() -> R.id.loginFragment
            else -> R.id.loginFragment
        }
    }

    /**
     * Sets up a listener to handle navigation destination changes.
     *
     * @param navController The NavController managing navigation events.
     */
    private fun setupDestinationChangeListener(navController: androidx.navigation.NavController?) {
        navController?.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {

                R.id.confirmwalletpin,
                R.id.createwalletpin,
                R.id.loginFragment,
                R.id.enterWalletPin -> {
                    binding.toolbar.profileIcon.visibility = View.GONE
                }

                R.id.profileFragment -> {
                    binding.toolbar.profileIcon.visibility = View.INVISIBLE
                    binding.toolbar.toolbarTitle.text = getString(R.string.profile)
                }

                else -> {
                    val isLoggedIn =
                        StorageRepository.readString(PreferenceKey.EMAIL_ID).isNotEmpty()

                    binding.toolbar.profileIcon.visibility =
                        if (isLoggedIn) View.VISIBLE else View.GONE
                    binding.toolbar.toolbarTitle.text = getString(R.string.menu_pay)
                }
            }
            triggerWalletBiometricIfNeeded(destination.id)
            refreshHostExitButton(destination.id)
        }
    }

    /**
     * 从 Fission 宿主跳入时，在 Wallet 顶层页工具栏显示「返回Demo首页」。
     * 进入卡片列表/开卡详情等子页时隐藏，避免与 ActionBar 返回箭头重叠。
     */
    private fun setupHostExitIfNeeded() {
        if (!PayExternalLaunch.isHostLaunch()) {
            return
        }

        binding.toolbar.toolbarLeftContainer.removeAllViews()
        hostExitButton = TextView(this).apply {
            text = getString(R.string.return_to_fission_home)
            setTextColor("#1D1D1D".toColorInt())
            textSize = 14f
            setPadding(
                resources.getDimensionPixelSize(R.dimen.dp8),
                0,
                resources.getDimensionPixelSize(R.dimen.dp8),
                0
            )
            setOnClickListener { PayExternalLaunch.exitToHost(this@MainActivity) }
        }
        binding.toolbar.toolbarLeftContainer.addView(hostExitButton)

        hostExitBackCallback?.remove()
        hostExitBackCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!PayExternalLaunch.isHostLaunch()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                    return
                }
                if (!isWalletTabVisible()) {
                    PayExternalLaunch.exitToHost(this@MainActivity)
                    return
                }
                val destinationId = navHostFragment.navController.currentDestination?.id
                if (destinationId != null && shouldShowHostExitButton(destinationId)) {
                    PayExternalLaunch.exitToHost(this@MainActivity)
                    return
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
        onBackPressedDispatcher.addCallback(this, hostExitBackCallback!!)

        refreshHostExitButton(navHostFragment.navController.currentDestination?.id)
    }

    private fun shouldShowHostExitButton(destinationId: Int): Boolean {
        if (!PayExternalLaunch.isHostLaunch()) return false
        if (destinationId !in HOST_EXIT_VISIBLE_DESTINATIONS) return false
        if (destinationId == R.id.deviceListFragment &&
            StorageRepository.readBoolean(PreferenceKey.BACK_PRESSED_FLAG)
        ) {
            return false
        }
        return true
    }

    /** 按当前导航目的地（及扫描态）刷新「返回Demo首页」可见性。 */
    fun refreshHostExitButton(destinationId: Int? = null) {
        if (!PayExternalLaunch.isHostLaunch()) {
            hostExitButton?.visibility = View.GONE
            return
        }
        val dest = destinationId ?: navHostFragment.navController.currentDestination?.id
        val show = dest != null && shouldShowHostExitButton(dest)
        hostExitButton?.visibility = if (show) View.VISIBLE else View.GONE
    }

    /**
     * Configures the toolbar setup.
     */
    private fun setupToolbar() {
        setupProfileIconClickListener()
        setupProfileIconImage()
    }

    /**
     * Handles click events for the profile icon.
     */
    private fun setupProfileIconClickListener() {
        binding.toolbar.profileIcon.setOnClickListener {
            val walletNavController = findNavController(R.id.nav_host_fragment_wallet)
            val currentWalletFragmentId = walletNavController.currentDestination?.id
            if (currentVisibleFragmentId != R.id.nav_host_fragment_health && currentWalletFragmentId != R.id.termsFragment) {
                openProfileFromAnywhere()
            }
        }
    }

    /**
     * Loads or generates the toolbar profile icon image.
     *
     * - Displays initials if user profile image is missing.
     * - Loads actual image using Glide if URL/path exists.
     * - Uses default placeholder icon otherwise.
     */
    private fun setupProfileIconImage() {
        when {
            !isEmptyString(email) && isEmptyString(userName) && isEmptyString(profileImage) -> {
                binding.toolbar.profileIcon.setImageBitmap(createInitialsDrawable(email))
            }

            !isEmptyString(userName) && isEmptyString(profileImage) -> {
                binding.toolbar.profileIcon.setImageBitmap(createInitialsDrawable(userName))
            }

            !isEmptyString(profileImage) -> {
                loadProfileImageFromUrl()
            }

            else -> {
                binding.toolbar.profileIcon.setImageResource(R.drawable.ic_user_avatar)
            }
        }
    }

    /**
     * Loads the user’s profile image from a URL into the toolbar using Glide.
     *
     * Gracefully handles invalid URLs with try-catch.
     */
    private fun loadProfileImageFromUrl() {
        try {
            Glide.with(this)
                .load(profileImage)
                .circleCrop()
                .into(binding.toolbar.profileIcon)
        } catch (e: IllegalArgumentException) {
            logger.noStackTraceLog("LoanProfileImage ", e)
        }
    }

    /**
     * Sets up the bottom navigation view.
     *
     * - Handles menu item selection via listener callbacks.
     * - Prevents reloading the same tab when reselected.
     */
    private fun setupBottomNavigation() {
        binding.bottomNavigationViewMain.setOnItemSelectedListener { item ->
            handleBottomNavigationItemSelected(item.itemId)
        }
        binding.bottomNavigationViewMain.setOnItemReselectedListener { /* Do nothing to prevent reload */ }
    }

    /**
     * Routes bottom navigation item selections to their respective handlers.
     *
     * @param itemId The selected menu item ID.
     * @return true if the selection was handled; false otherwise.
     */
    private fun handleBottomNavigationItemSelected(itemId: Int): Boolean {
        when (itemId) {
            R.id.menu_home -> handleHomeMenuSelected()
            R.id.menu_health -> handleHealthMenuSelected()
            R.id.menu_pay -> handlePayMenuSelected()
            else -> return false
        }
        return true
    }


    /**
     * Handles the "Home" menu selection.
     */
    private fun handleHomeMenuSelected() {
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        binding.toolbar.toolbarTitle.text = getString(R.string.menu_home)
        binding.toolbar.profileIcon.visibility = View.GONE
        showOnly(R.id.nav_host_fragment_home)
    }

    /**
     * Handles the "Health" menu selection.
     */
    private fun handleHealthMenuSelected() {
        binding.toolbar.profileIcon.visibility = View.GONE
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        binding.toolbar.toolbarTitle.text = getString(R.string.menu_health)
        showOnly(R.id.nav_host_fragment_health)
    }

    /**
     * Handles the "Pay" menu selection.
     */
    private fun handlePayMenuSelected() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastWalletTabClickMs < WALLET_TAB_TAP_DEBOUNCE_MS) {
            return
        }
        lastWalletTabClickMs = now

        enableBiometric = true
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        val currentDestination =
            navHostFragment.findNavController().currentDestination?.id

        binding.toolbar.toolbarTitle.text = getString(R.string.menu_pay)
        val isLoggedIn =
            StorageRepository.readString(PreferenceKey.EMAIL_ID).isNotEmpty()
        if (currentDestination == R.id.confirmwalletpin ||
            currentDestination == R.id.createwalletpin ||
            currentDestination == R.id.loginFragment ||
            currentDestination == R.id.enterWalletPin
        ) {
            binding.toolbar.profileIcon.visibility = View.GONE
        } else {
            binding.toolbar.profileIcon.visibility =
                if (isLoggedIn) View.VISIBLE else View.GONE
        }
        if (currentDestination == R.id.cardListFragment
            || currentDestination == R.id.detailFragment
            || currentDestination == R.id.termsFragment
            || currentDestination == R.id.profileFragment
            || currentDestination == R.id.wearableSettingFragment
            || currentDestination == R.id.wearablePasscodeFragment
        ) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        handlePayMenuNavigation(currentDestination)
        showOnly(R.id.nav_host_fragment_wallet)
        triggerWalletBiometricIfNeeded(currentDestination)
    }

    /**
     * Triggers biometric authentication for wallet flows when required.
     *
     * Checks the [enableBiometric] flag and the provided navigation
     * `destinationId`. If the destination corresponds to the create-pin or enter-pin
     * screens, this method posts a runnable to the view hierarchy to ensure the
     * fragment transaction and lifecycle are settled, then invokes the appropriate
     * biometric prompt on the current fragment:
     *  - If the current fragment is [EnterWalletFragment], calls
     *    `promptBiometricIfWalletPinExists()` and disables biometric triggering.
     *  - If the current fragment is [CreateWalletFragment], calls
     *    `promptBiometricForWalletAccess()` and disables biometric triggering.
     *
     * Posting to the view ensures the fragment is attached and ready before the
     * biometric flow is started.
     *
     * @param destinationId The navigation destination id to evaluate; biometric is
     *                      considered only for `R.id.createwalletpin` and
     *                      `R.id.enterWalletPin`.
     * @return Unit
     * @throws IllegalStateException if the fragment manager or view hierarchy is not
     *                               available when the posted runnable runs.
     * @see EnterWalletFragment
     * @see CreateWalletFragment
     * @see bioMetric
     */
    /**
     * Enables the wallet biometric prompt for the next PIN screen navigation.
     *
     * Call after a successful login so biometric is offered when navigating to
     * [R.id.createwalletpin] or [R.id.enterWalletPin], including re-login after
     * logout or session expiry.
     */
    fun requestWalletBiometricAfterLogin() {
        enableBiometric = true
    }

    private fun triggerWalletBiometricIfNeeded(destinationId: Int?) {
        if (!BuildConfig.ENABLE_BIOMETRIC) return
        if (!enableBiometric) return
        if (destinationId != R.id.createwalletpin && destinationId != R.id.enterWalletPin) return

        // Post to ensure fragment transaction/lifecycle state is settled.
        binding.root.post {
            if (!enableBiometric) return@post
            when (val walletFragment = getCurrentFragment()) {
                is EnterWalletFragment -> {
                    walletFragment.promptBiometricIfWalletPinExists()
                    enableBiometric = false
                }

                is CreateWalletFragment -> {
                    walletFragment.promptBiometricForWalletAccess()
                    enableBiometric = false
                }
            }
        }
    }

    /**
     * Handles navigation logic when switching to the "Pay" menu.
     */
    private fun handlePayMenuNavigation(currentDestination: Int?) {
        if (currentDestination == R.id.oemFragment || currentDestination == R.id.createwalletpin) {
            binding.toolbar.profileIcon.visibility = View.GONE
        } else if (currentDestination == R.id.profileFragment) {
            binding.toolbar.profileIcon.visibility = View.INVISIBLE
            binding.toolbar.toolbarTitle.text = getString(R.string.profile)
        }

        if (isLoginFlowDestination(currentDestination)) {
            handleLoginFlowNavigation()
        } else {
            handleOtherDestinationNavigation(currentDestination)
        }
    }

    /**
     * Checks whether the current destination belongs to the login/auth flow.
     */
    private fun isLoginFlowDestination(currentDestination: Int?): Boolean {
        return currentDestination == R.id.homeFragment ||
            currentDestination == R.id.loginFragment ||
            currentDestination == R.id.enterWalletPin
    }

    /**
     * Handles navigation within the login flow.
     *
     * Navigates user to login, create PIN, or wallet entry screens depending on stored data.
     */
    private fun handleLoginFlowNavigation() {
        val navController = navHostFragment.findNavController()

        if (StorageRepository.readString(PreferenceKey.EMAIL_ID).isEmpty()) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            navController.navigate(R.id.loginFragment)
        } else {
            navigateBasedOnWalletState(navController)
        }
    }

    /**
     * Determines navigation based on wallet setup state.
     */
    private fun navigateBasedOnWalletState(
        navController: androidx.navigation.NavController?
    ) {
        if (walletPin.isEmpty()) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            if (navController?.currentDestination?.id != R.id.createwalletpin) {
                navController?.navigate(R.id.createwalletpin)
            }
        } else {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            if (navController?.currentDestination?.id != R.id.enterWalletPin) {
                navController?.navigate(R.id.enterWalletPin)
            }
        }
    }

    /**
     * Handles navigation for non-login destinations.
     */
    private fun handleOtherDestinationNavigation(currentDestination: Int?) {
        if (currentDestination != null) {
            if (currentDestination == R.id.deviceListFragment) {
                val backFlag = StorageRepository.readBoolean(PreferenceKey.BACK_PRESSED_FLAG)
                if (backFlag) {
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                } else {
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                }
            }
            navigateToDestination(currentDestination)
        }
    }

    /**
     * Navigates to the given destination ID using NavOptions.
     *
     * Avoids reloading the same destination repeatedly.
     */
    private fun navigateToDestination(destinationId: Int) {
        val navController = navHostFragment.findNavController()
        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .build()

        if (navController.currentDestination?.id != destinationId) {
            navController.navigate(destinationId, null, navOptions)
        }
    }

    /**
     * Sets up the ActionBar with the custom toolbar.
     */
    private fun setupActionBar() {
        val toolbar = binding.toolbar.toolbarView
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
        binding.toolbar.toolbarTitle.text = getString(R.string.menu_home)
    }

    /**
     * Handles FCM "Device Detach Update" from any screen: pending scripts first, then device list navigation.
     */
    private fun handleDeviceDetachNotification(event: AppEvent) {
        val paymentAppInstanceId = event.getStringExtra(BundleKey.PAYMENT_APP_INSTANCE_ID)
        val seIdFromNotification = event.getStringExtra(BundleKey.DEVICE_SE_ID)
        FcmSecureFlowCoordinator.markDeviceDetachScheduled(seIdFromNotification)
        logger.debug(
            ":: MainScreen Device Detach notification paymentAppInstanceId=$paymentAppInstanceId seId=$seIdFromNotification"
        )
        DeviceDetachUpdateHandler.handle(
            activity = this,
            scope = lifecycleScope,
            paymentAppInstanceId = paymentAppInstanceId,
            seIdFromNotification = seIdFromNotification
        )
    }

    /**
     * Handles FCM "Card Deleted" on any screen (device list, card list, settings tab, etc.).
     */
    private fun handleFcmCardDeletedNotification(event: AppEvent) {
        val msgType = event.getStringExtra(BundleKey.MSG_TYPE)
        if (msgType != DELETED_CARD) return
        val entityId = event.getStringExtra(BundleKey.ENTITY_ID)?.trim().orEmpty()
        if (entityId.isEmpty() || entityId.equals("null", ignoreCase = true)) return
        logger.debug(":: MainScreen Card Deleted notification entityId=$entityId")
        FcmDeletedCardHandler.handle(
            activity = this,
            scope = lifecycleScope,
            digitizationReferenceNumber = entityId
        )
    }

    /**
     * Requests [CardListFragment] to reload from the API after FCM card-delete scripts complete.
     * When the card list is already visible, updates its [androidx.lifecycle.SavedStateHandle] so
     * [CardListFragment] refreshes immediately without waiting for [onResume].
     */
    fun signalCardListRefreshAfterFcmDeleted() {
        CardListFragment.shouldForceApiRefresh = true
        if (!::navHostFragment.isInitialized) return
        val nav = navHostFragment.navController
        if (nav.currentDestination?.id != R.id.cardListFragment) return
        try {
            nav.getBackStackEntry(R.id.cardListFragment).savedStateHandle[
                DigitizationDeleteFlowGate.POST_FCM_CARD_DELETED_REFRESH_KEY
            ] = true
        } catch (e: IllegalArgumentException) {
            logger.debug("signalCardListRefreshAfterFcmDeleted: card list not on back stack: ${e.message}")
        }
    }

    /**
     * After FCM card-delete scripts finish, returns to [CardListFragment] when the user is viewing
     * that card in [com.infineon.secora.wallet.ui.fragment.DeviceDetailFragment] (any tab).
     */
    fun navigateToCardListAfterFcmCardDeletedIfOnDetailForCard(entityId: String) {
        if (!::navHostFragment.isInitialized) return
        val nav = navHostFragment.navController
        if (nav.currentDestination?.id != R.id.detailFragment) return

        val viewedCardRef = StorageRepository.readString(PreferenceKey.DIGITIZATION_REFERENCE_NUMBER).trim()
        if (viewedCardRef.isEmpty() ||
            !viewedCardRef.equals(entityId.trim(), ignoreCase = true)
        ) {
            logger.debug(
                "navigateToCardListAfterFcmCardDeletedIfOnDetailForCard: " +
                    "detail open for ref=$viewedCardRef, notification ref=$entityId; no navigation"
            )
            return
        }
        CardListFragment.shouldForceApiRefresh = true
        try {
            if (!nav.popBackStack()) {
                logger.debug("navigateToCardListAfterFcmCardDeletedIfOnDetailForCard: popBackStack returned false")
            } else {
                logger.debug("navigateToCardListAfterFcmCardDeletedIfOnDetailForCard: popped to card list")
            }
        } catch (e: Exception) {
            logger.debug("navigateToCardListAfterFcmCardDeletedIfOnDetailForCard: ${e.message}")
        }
    }

    /**
     * Returns true when the wallet back stack shows a device flow screen (card list, detail, or
     * terms) for [targetSeId], i.e. the user opened that wearable — not another device's screens.
     */
    fun isUserOnDeviceFlowScreenForSeId(targetSeId: String): Boolean {
        if (!::navHostFragment.isInitialized || targetSeId.isBlank()) return false
        val nav = navHostFragment.navController
        val destinationId = nav.currentDestination?.id ?: return false
        val deviceFlowScreens = setOf(
            R.id.cardListFragment,
            R.id.detailFragment,
            R.id.termsFragment
        )
        if (destinationId !in deviceFlowScreens) return false
        val currentSeId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID).trim()
        return currentSeId.isNotEmpty() &&
            normalizeSeIdForComparison(currentSeId) == normalizeSeIdForComparison(targetSeId)
    }

    /**
     * FCM device-detach only: pops back to the device list when the user tapped No on the BLE
     * reconnect dialog. Does not use [handleOpenDeviceListEvent] (normal detach completion flow).
     */
    fun popBackToDeviceListAfterFcmDetachDeclined(targetSeId: String) {
        if (!isUserOnDeviceFlowScreenForSeId(targetSeId)) {
            logger.debug(
                "popBackToDeviceListAfterFcmDetachDeclined: not on flow for seId=$targetSeId, skipping"
            )
            return
        }
        if (!::navHostFragment.isInitialized) return
        binding.bottomNavigationViewMain.selectedItemId = R.id.menu_pay
        showOnly(R.id.nav_host_fragment_wallet)
        val nav = navHostFragment.navController
        if (nav.currentDestination?.id == R.id.deviceListFragment) {
            logger.debug("popBackToDeviceListAfterFcmDetachDeclined: already on device list")
            return
        }
        try {
            val popped = nav.popBackStack(R.id.deviceListFragment, false)
            if (!popped) {
                nav.navigate(R.id.deviceListFragment)
            }
            logger.debug("popBackToDeviceListAfterFcmDetachDeclined: returned to device list")
        } catch (e: Exception) {
            logger.debug("popBackToDeviceListAfterFcmDetachDeclined: ${e.message}")
        }
    }

    /**
     * Refreshes [CardListFragment] when it is visible for [targetSeId] (used when the user declines
     * FCM BLE reconnect on a card-deleted notification).
     */
    fun refreshCardListIfVisibleForSeId(targetSeId: String) {
        if (!isUserOnDeviceFlowScreenForSeId(targetSeId)) return
        if (!::navHostFragment.isInitialized) return
        if (navHostFragment.navController.currentDestination?.id != R.id.cardListFragment) return
        CardListFragment.shouldForceApiRefresh = true
        signalCardListRefreshAfterFcmDeleted()
        logger.debug("refreshCardListIfVisibleForSeId: refresh triggered for seId=$targetSeId")
    }

    /**
     * Normalizes SE ID strings for case-insensitive comparison across FCM and preference sources.
     *
     * @param seId Raw secure element ID from notification or prefs.
     */
    private fun normalizeSeIdForComparison(seId: String): String =
        seId.trim().replace("-", "").uppercase()

    /**
     * Handles "ACTION_OPEN_DEVICE_LIST" events from EventBus.
     */
    private fun handleOpenDeviceListEvent() {
        binding.bottomNavigationViewMain.selectedItemId = R.id.menu_pay

        val walletNavController = navHostFragment.navController
        if (walletNavController.currentDestination?.id == R.id.deviceListFragment) {
            logger.debug(":: MainScreen already on device list; pay tab selected, fragment will refresh via detach event")
            return
        }

        walletNavController.navigate(R.id.deviceListFragment)
    }

    /**
     * Overrides resource configuration to prevent system font scaling.
     */
    override fun getResources(): Resources {
        val res = super.getResources()
        val config =
            Configuration(res.configuration)
        config.fontScale = 1.0f  // Set default font scale (no scaling)
        val context = createConfigurationContext(config)
        return context.resources
    }

    /**
     * Handles toolbar item selections.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()  // or finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Method to update logs in logcat.
     *
     * @param message message to add to logcat.
     */
    fun updateLogs(message: String) {
        runOnUiThread { logger.info(message) }
    }

    /**
     * Shows or hides the loading spinner with blurry overlay and disables back press.
     *
     * @param show Boolean to indicate visibility
     * @param message Message to display during loading
     */
    /**
     * Shows the same de-link success dialog as manual delink in [com.infineon.secora.wallet.ui.fragment.AvailableDeviceFragment],
     * used after a portal-initiated "Device Detach Update" FCM flow completes.
     */
    fun showPortalDelinkSuccessDialog(message: String) {
        if (isFinishing) return
        runOnUiThread {
            if (isFinishing) return@runOnUiThread
            portalDelinkSuccessDialog?.dismiss()
            val dialogBinding = DialogCommonMessageBinding.inflate(layoutInflater)
            val dialog = Dialog(this).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(dialogBinding.root)
                setCancelable(false)
            }
            dialogBinding.txtTitle.text = getString(R.string.text_secora_wallet)
            dialogBinding.txtMessage.text = message
            dialogBinding.txtCancel.visibility = View.GONE
            dialogBinding.txtOK.setOnClickListener {
                dialog.dismiss()
                portalDelinkSuccessDialog = null
            }
            dialog.setOnDismissListener { portalDelinkSuccessDialog = null }
            portalDelinkSuccessDialog = dialog
            dialog.showSecure()
        }
    }

    fun showLoading(show: Boolean, message: String) {
        if (!show && FcmSecureFlowCoordinator.isLoaderHoldActive()) {
            logger.debug("Ignoring loader dismiss; FCM secure flow owns the loader")
            return
        }

        val displayMessage = message.takeIf { it.isNotEmpty() } ?: getString(R.string.text_please_wait)

        runOnUiThread {
            if (show) {
                if (binding.loadingIcon.clCustomProgressBar.visibility != View.VISIBLE) {
                    binding.loadingIcon.clCustomProgressBar.visibility = View.VISIBLE
                }
                if (!isBlurShown) {
                    showBlurryOverlay()
                    isBlurShown = true
                }
                disableBackPress()
                UIHelper.hideKeyboard(binding.root, this)
            } else {
                backPressedCallback?.isEnabled = false
                hideBlurryOverlay()
                isBlurShown = false
                binding.loadingIcon.clCustomProgressBar.visibility = View.GONE
            }

            binding.loadingIcon.tvCustomProcess.text = displayMessage
            binding.loadingIcon.tvCustomBt.visibility =
                if (displayMessage == getString(R.string.scanning)) View.VISIBLE else View.GONE

            binding.bottomNavigationViewMain.isEnabled = !show
        }
    }

    /**
     * Disables the Android back button while loading is visible.
     */
    private fun disableBackPress() {
        if (backPressedCallback == null) {
            backPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    logger.debug("BackPressHandler Back press disabled while loading")
                }
            }
            onBackPressedDispatcher.addCallback(this, backPressedCallback!!)
        } else {
            backPressedCallback?.isEnabled = true
        }
    }

    /**
     * Shows or hides scanner-specific loader overlay.
     */
    fun showScannerLoading(show: Boolean) {
        runOnUiThread {
            binding.scanLoader.clProgressBar.visibility = if (show) View.VISIBLE else View.GONE
            binding.scanLoader.tvBt.visibility = if (show) View.VISIBLE else View.GONE
            binding.bottomNavigationViewMain.isEnabled = show

            if (show) {
                showBlurryOverlay()
            } else {
                hideBlurryOverlay()
            }
        }
    }

    /**
     * Applies a blurred screenshot as an overlay background.
     * Optimized to reduce main thread blocking by downscaling the bitmap.
     */
    fun showBlurryOverlay(vararg excludeViews: View) {
        runOnUiThread {
            val myGen = blurOverlayGeneration.incrementAndGet()
            val rootView = window.decorView.rootView
            val overlay = binding.root

            val visibilityMap = excludeViews.associateWith { it.visibility }
            excludeViews.forEach { it.visibility = View.INVISIBLE }

            // Use downscaled bitmap to reduce main thread work (4x smaller)
            val scaleFactor = 4
            val scaledWidth = rootView.width / scaleFactor
            val scaledHeight = rootView.height / scaleFactor

            if (scaledWidth <= 0 || scaledHeight <= 0) {
                visibilityMap.forEach { (view, visibility) -> view.visibility = visibility }
                return@runOnUiThread
            }

            val screenshot = createBitmap(
                scaledWidth,
                scaledHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(screenshot)
            canvas.scale(1f / scaleFactor, 1f / scaleFactor)
            rootView.draw(canvas)
            visibilityMap.forEach { (view, visibility) -> view.visibility = visibility }

            if (blurOverlayGeneration.get() != myGen) {
                return@runOnUiThread
            }

            overlay.visibility = View.VISIBLE
            binding.blurBackground.visibility = View.VISIBLE

            // Synchronous blur: async().into() could complete after hideBlurryOverlay() and re-show blur.
            Blurry.with(applicationContext)
                .radius(25)
                .sampling(1) // Already downscaled, no need for additional sampling
                .from(screenshot)
                .into(binding.blurBackground)

            if (blurOverlayGeneration.get() != myGen) {
                binding.blurBackground.setImageBitmap(null)
                binding.blurBackground.setImageDrawable(null)
                binding.blurBackground.visibility = View.GONE
            }
        }
    }

    /**
     * Hides the blur overlay and clears the bitmap.
     */
    fun hideBlurryOverlay() {
        runOnUiThread {
            blurOverlayGeneration.incrementAndGet()
            val overlay = binding.root as ViewGroup
            Blurry.delete(overlay)
            binding.blurBackground.clearAnimation()
            binding.blurBackground.setImageBitmap(null)
            binding.blurBackground.setImageDrawable(null)
            binding.blurBackground.visibility = View.GONE
            overlay.invalidate()
            window.decorView.invalidate()
        }
    }

    /**
     * Displays or hides the "Verifying Bank" loading screen.
     *
     * @param show `true` to show the verifying loader, `false` to hide it.
     */
    fun showVerifyingBankLoading(show: Boolean) {
        runOnUiThread {
            binding.scanLoader.clProgressBar.visibility = if (show) View.VISIBLE else View.GONE
            binding.scanLoader.tvBt.visibility = View.GONE
            binding.scanLoader.tvProcess.text =
                binding.root.context.getString(R.string.verifying_identity)
            binding.bottomNavigationViewMain.isEnabled = show

            if (show) {
                showBlurryOverlay()
            } else {
                hideBlurryOverlay()
            }
        }
    }

    /**
     * Shows a specific fragment container and hides others.
     *
     * @param fragmentId Resource ID of the fragment container to display
     */
    private fun showOnly(fragmentId: Int) {
        val fragments = listOf(
            binding.navHostFragmentHome,
            binding.navHostFragmentHealth,
            binding.navHostFragmentWallet
        )

        fragments.forEach { container ->
            container.visibility =
                if (container.id == fragmentId) View.VISIBLE else View.GONE
        }

        // Track current visible fragment
        currentVisibleFragmentId = fragmentId

        // Call refresh method ONLY when HomeFragment is being shown
        if (fragmentId == R.id.nav_host_fragment_home) {
            val homeNavHost =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment_home) as? NavHostFragment
            val homeFragment =
                homeNavHost?.childFragmentManager?.fragments?.firstOrNull() as? HomeFragment
            homeFragment?.refreshProfileIcon()
        }
    }

    /**
     * open profile fragment from anywhere
     *
     */
    fun openProfileFromAnywhere() {
        showOnly(R.id.nav_host_fragment_wallet) // switch container
        // Update BottomNavigationView to select the wallet tab
        binding.bottomNavigationViewMain.selectedItemId = R.id.menu_pay

        val walletNavHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_wallet) as NavHostFragment
        val navController = walletNavHost.navController

        navController.navigate(R.id.profileFragment)
    }

    /**
     * Create username initials circular drawable with caching to avoid UI lag.
     * Cached bitmaps are reused when the same userName is requested.
     *
     * @param userName The user's name to extract initials from
     * @return Bitmap with circular background and initials
     */
    fun createInitialsDrawable(userName: String): Bitmap {
        // Return cached bitmap if available
        initialsCache[userName]?.let { return it }

        val initial = userName
            .trim()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
        val size = 200 // pixels (adjust if needed)

        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        // Draw circle
        val circlePaint = Paint().apply {
            color = AVATAR_BLUE_COLOR.toColorInt() // Blue shade, you can randomize
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, circlePaint)

        // Draw initial
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = size / 2f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textY = (canvas.height / 2 - (textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(initial, size / 2f, textY, textPaint)

        // Cache the bitmap for future use
        initialsCache[userName] = bitmap
        return bitmap
    }

    /**
     * onResume(): Updates the profile icon based on user data.
     *
     */
    override fun onResume() {
        super.onResume()
        userName = StorageRepository.readString(PreferenceKey.USER_NAME)
        profileImage = StorageRepository.readString(PreferenceKey.PROFILE_IMAGE)
        email = StorageRepository.readString(PreferenceKey.EMAIL_ID)
        if (email.isEmpty()) {
            binding.toolbar.profileIcon.visibility = View.GONE
            return
        }
        binding.toolbar.profileIcon.visibility = View.VISIBLE
        when {
            !isEmptyString(email) && isEmptyString(userName)
                && isEmptyString(profileImage) -> {
                binding.toolbar.profileIcon.setImageBitmap(createInitialsDrawable(email))
            }

            !isEmptyString(userName) && isEmptyString(profileImage) -> {
                binding.toolbar.profileIcon.setImageBitmap(createInitialsDrawable(userName))
            }

            !isEmptyString(profileImage) -> {
                try {
                    Glide.with(this)
                        .load(profileImage)
                        .circleCrop()
                        .into(binding.toolbar.profileIcon)
                } catch (e: IllegalArgumentException) {
                    logger.noStackTraceLog("LoanProfileImage ", e)
                }
            }

            else -> {
                binding.toolbar.profileIcon.setImageResource(R.drawable.ic_user_avatar)
            }
        }
    }
}
