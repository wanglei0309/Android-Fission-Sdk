// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: SplashActivity.kt displays the app’s splash screen with a zoom-in animation in fullscreen mode,
 * then navigates to MainActivity
 **/
package com.infineon.secora.wallet.ui.home

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.infineon.secora.wallet.CertificateSpec
import com.infineon.secora.wallet.CvCertificateBuilder
import com.infineon.secora.wallet.KeyUsage
import com.infineon.secora.wallet.PsoCommandBuilder
import com.infineon.secora.wallet.PayExternalLaunch
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.client.operations.common.logger.Logger
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.ActivitySplashBinding
import com.infineon.secora.wallet.firebase.FirebaseManager
import com.infineon.secora.wallet.toHex
import com.infineon.secora.wallet.utils.Utils.isNetworkAvailable
import com.infineon.secora.wallet.utils.helper.ScreenCaptureProtection
import com.infineon.secora.wallet.utils.helper.showSecure
import com.infineon.secora.wallet.utils.constants.BundleKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.interfaces.ECPublicKey

/**
 * This is a launching screen.
 * This screen was shown for 2 sec after that MainActivity screen will get launched
 */
class SplashActivity : AppCompatActivity() {

    private val logger = Logger.getNewLogger(SplashActivity::class.java.name.toString())
    private lateinit var binding: ActivitySplashBinding
    private var waitingForNetwork = false
    private var noNetworkDialog: AlertDialog? = null
    private var errorDialog: AlertDialog? = null
    private var hasBeenStopped = false

    companion object {
        private const val PEAK_HEIGHT_DP = 36f

        private const val BOTTOM_PANEL_FRACTION = 0.42f

        private const val MERGE_DURATION = 1050L

        private const val BRANDING_DURATION = 2000L

        private const val BRANDING_START_FRACTION = 0.55f

        private const val SPLASH_HOLD = 5000L
    }

    /**
     * onCreate() is called when the activity is first created.
     *
     * - Inside this the view is getting inflated and set to the activity.
     * - Adding Fullscreen mode to the activity and screen animation is done here.
     *
     * @param savedInstanceState the saved instance state from a previous configuration (if any)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScreenCaptureProtection.apply(this)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpUi()
        setupSplash()
        handleIntent(intent)
            main()
    }

    fun main() {
        // --- Example: build a 2-certificate chain (CERT.KA-KLOC.ECDSA -> CERT.OCE.ECKA) and its PSO APDUs ---

        // In real usage: rootCaKeyPair.private is SK.CA-KLOC.ECDSA (held by your backend/HSM, NOT generated
        // fresh on-device), and each subject key pair is generated wherever that key actually needs to live.
        val kpg = java.security.KeyPairGenerator.getInstance("EC")
        kpg.initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
        val rootCaKeyPair = kpg.generateKeyPair()
        val klocKeyPair = kpg.generateKeyPair()
        val oceKeyPair = kpg.generateKeyPair()

        val klocSpec = CertificateSpec(
            serialNumber = byteArrayOf(0x00, 0x01),
            caKlocIdentifier = "IFX CA-KLOC 0001",
            subjectIdentifier = "IFX KA-KLOC 0001",
            keyUsage = KeyUsage.CA_KLOC_ECDSA,
            expirationDateYYYYMMDD = "20301231",
            subjectPublicKey = klocKeyPair.public as ECPublicKey
        )

        val oceSpec = CertificateSpec(
            serialNumber = byteArrayOf(0x00, 0x01),
            caKlocIdentifier = "IFX KA-KLOC 0001", // must equal the previous cert's subjectIdentifier
            subjectIdentifier = "IFX OCE-ECKA",
            keyUsage = KeyUsage.OCE_ECKA,
            expirationDateYYYYMMDD = "20301231",
            subjectPublicKey = oceKeyPair.public as ECPublicKey
        )

        val certChain = CvCertificateBuilder.buildChain(
            rootIssuerPrivateKey = rootCaKeyPair.private,
            entries = listOf(klocSpec to klocKeyPair, oceSpec to oceKeyPair)
        )

        // Key identifier '0x10' is required by the device for the first certificate (Table 63: 6A88 otherwise).
        // Key version number must match whatever PK.CA-KLOC.ECDSA version is actually provisioned on the device.
        val apdus = PsoCommandBuilder.buildPsoApdus(
            certificates = certChain,
            keyVersionNumber = 0x1A,
            keyIdentifier = 0x10
        )

        apdus.forEachIndexed { i, apdu ->
            println("Deepak :: APDU #$i (${apdu.size} bytes): ${apdu.toHex()}")
        }
    }
    private fun setUpUi() {
        window.insetsController?.apply {
            hide(WindowInsets.Type.statusBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    private fun setupSplash() {

        binding.splashHero.setImageResource(
            R.drawable.splash_back
        )

        binding.walletLogo.setImageResource(
            R.drawable.secora_wallet_icon
        )

        startSplashAnimation()
    }

    private fun startSplashAnimation() {

        binding.splashRoot.post {

            val screenHeight = binding.splashRoot.height

            val density = resources.displayMetrics.density

            val peakHeight = PEAK_HEIGHT_DP * density

            val bottomPanelHeight =
                screenHeight * BOTTOM_PANEL_FRACTION

            val heroHeight =
                screenHeight -
                    bottomPanelHeight +
                    peakHeight

            binding.splashHero.layoutParams =
                binding.splashHero.layoutParams.apply {
                    height = heroHeight.toInt()
                }

            binding.whitePanel.layoutParams =
                binding.whitePanel.layoutParams.apply {
                    height = (
                        bottomPanelHeight + peakHeight
                        ).toInt()
                }

            /*
             * Initial state
             *
             * Hero starts above the screen.
             * White panel starts below the screen.
             * Branding is invisible and slightly smaller.
             */
            binding.splashHero.translationY =
                -heroHeight

            binding.whitePanel.translationY =
                bottomPanelHeight + peakHeight

            binding.brandingContainer.alpha = 0f

            binding.brandingContainer.scaleX = 0.9f
            binding.brandingContainer.scaleY = 0.9f

            /*
             * Start both animations together.
             */
            animateHero()

            animateWhitePanel()

            /*
             * Branding starts at 55% of merge animation.
             *
             * 1050 * 0.55 = 577.5ms
             */
            binding.root.postDelayed(
                {
                    animateBranding()
                },
                (MERGE_DURATION * BRANDING_START_FRACTION)
                    .toLong()
            )

            /*
             * Finish splash.
             */
            binding.root.postDelayed(
                {
                    onSplashFinished()
                },
                SPLASH_HOLD
            )
        }
    }

    private fun animateHero() {

        binding.splashHero
            .animate()
            .translationY(0f)
            .setDuration(MERGE_DURATION)
            .setInterpolator(
                AccelerateDecelerateInterpolator()
            )
            .start()
    }

    private fun animateWhitePanel() {

        binding.whitePanel
            .animate()
            .translationY(0f)
            .setDuration(MERGE_DURATION)
            .setInterpolator(
                AccelerateDecelerateInterpolator()
            )
            .start()
    }

    private fun animateBranding() {

        binding.brandingContainer
            .animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(BRANDING_DURATION)
            .setInterpolator(
                AccelerateDecelerateInterpolator()
            )
            .start()
    }

    private fun onSplashFinished() {

        // Navigate to your next screen here.

        // Example:
        // startActivity(Intent(this, LoginActivity::class.java))
        // finish()
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val digitizationReferenceNumber = intent?.getStringExtra(BundleKey.ENTITY_ID)

        if (digitizationReferenceNumber != null) {
            logger.debug(":: SplashScreen Notification Flow")
            val intent = Intent(this, MainActivity::class.java)
            intent.apply {
                putExtra(BundleKey.ENTITY_ID, digitizationReferenceNumber)
            }
            PayExternalLaunch.copyLaunchExtras(getIntent(), intent)
            navigateToMainScreen(intent)
            return
        }

        // 宿主 Fission demo 集成：无 google-services.json，跳过 FCM 拉取直接进入主流程
        if (intent?.getBooleanExtra(PayExternalLaunch.EXTRA_PRECONNECTED, false) == true) {
            logger.debug(":: SplashScreen Host preconnected flow, skip FCM")
            val mainIntent = Intent(this, MainActivity::class.java)
            PayExternalLaunch.copyLaunchExtras(getIntent(), mainIntent)
            navigateToMainScreen(mainIntent)
            return
        }

        logger.debug(":: SplashScreen Normal Flow")
        loadFcmConfig()
    }

    private fun loadFcmConfig(skipNetworkCheck: Boolean = false) {
        val fcmToken = StorageRepository.readString(PreferenceKey.FCM_TOKEN)
        if (fcmToken.isNotEmpty()) {
            val intent = Intent(this, MainActivity::class.java)
            PayExternalLaunch.copyLaunchExtras(getIntent(), intent)
            navigateToMainScreen(intent)
            return
        }

        if (!skipNetworkCheck && !isNetworkAvailable(this)) {
            waitingForNetwork = true
            confirmDataDialog(getString(R.string.no_network_error_msg))
            return
        }
        if (!skipNetworkCheck) waitingForNetwork = false
        FirebaseManager.fetchToken({ token -> handleFcmSuccess(token) }, { error -> handleFcmFailure(error) })
    }

    /**
     * Handles successful retrieval of the Firebase Cloud Messaging (FCM) token.
     *
     * This method stores it in shared preferences,
     * and navigates the user to the main screen of the application.
     *
     * @param fcmToken The successfully retrieved FCM registration token.
     */
    private fun handleFcmSuccess(fcmToken: String) {
        logger.debug("fcmToken: $fcmToken")
        StorageRepository.saveString(PreferenceKey.FCM_TOKEN, fcmToken)
        val intent = Intent(this, MainActivity::class.java)
        PayExternalLaunch.copyLaunchExtras(getIntent(), intent)
        navigateToMainScreen(intent)
    }

    /**
     * Handles failure during Firebase Cloud Messaging (FCM) token retrieval.
     *
     * This method displays an error dialog with the provided error message.
     * If the error message is null, a default localized error message is shown.
     *
     * @param errMessage Optional error message describing the failure reason.
     */
    private fun handleFcmFailure(errMessage: String?) {
        showErrorDialog(errMessage ?: applicationContext.getString(R.string.failed_to_fetch_fcm_token))
    }

    /**
     * Navigates the user to the main screen of the application.
     *
     * @param intent The intent used to launch the target activity.
     */
    private fun navigateToMainScreen(intent: Intent) {
        lifecycleScope.launch {
            delay(2000)
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (waitingForNetwork && hasBeenStopped) {
            hasBeenStopped = false
            lifecycleScope.launch {
                delay(800)
                if (!waitingForNetwork) return@launch
                dismissNoNetworkDialog()
                loadFcmConfig(skipNetworkCheck = true)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        hasBeenStopped = true
    }

    override fun onDestroy() {
        dismissNoNetworkDialog()
        errorDialog?.takeIf { it.isShowing }?.dismiss()
        errorDialog = null
        super.onDestroy()
    }

    private fun dismissNoNetworkDialog() {
        noNetworkDialog?.takeIf { it.isShowing }?.dismiss()
        noNetworkDialog = null
    }

    fun confirmDataDialog(titleMessage: String?) {
        dismissNoNetworkDialog()
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(resources.getString(R.string.app_name))
        alertDialogBuilder.setMessage(titleMessage).setCancelable(false).setPositiveButton(
            resources.getString(R.string.ok)
        ) { dialog, _ ->
            noNetworkDialog = null
            dialog.dismiss()
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        }
        noNetworkDialog = alertDialogBuilder.create()
        noNetworkDialog?.showSecure()
    }

    /**
     * Shows a generic error dialog with OK that only dismisses (no navigation to settings).
     * Used for non–network-related Firebase config failures.
     */
    private fun showErrorDialog(message: String) {
        errorDialog?.takeIf { it.isShowing }?.dismiss()
        errorDialog = null
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.app_name))
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                errorDialog = null
                dialog.dismiss()
            }
        errorDialog = builder.create()
        errorDialog?.showSecure()
    }
}
