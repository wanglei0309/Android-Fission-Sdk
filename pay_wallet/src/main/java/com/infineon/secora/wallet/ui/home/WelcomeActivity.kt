// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: WelcomeActivity.kt manages the app’s onboarding screens, showing introductory slides with navigation dots.
 * It also checks and requests necessary permissions, supports biometric authentication,
 * and navigates users to the login screen after the introduction.
 **/
package com.infineon.secora.wallet.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.ViewPager
import com.infineon.secora.wallet.BuildConfig
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.adapter.IntroductionAdapter
import com.infineon.secora.wallet.databinding.ActivityWelcomeBinding
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.utils.constants.Constants.BIOMETRIC_NOT_AVAILABLE
import com.infineon.secora.wallet.utils.helper.ScreenCaptureProtection

/**
 * [WelcomeActivity] displays the app's introduction or onboarding screens to new users.
 *
 * Features:
 * - Displays multiple intro slides with dot indicators.
 * - Handles navigation to login after onboarding.
 * - Requests and manage runtime permissions.
 * - Performs biometric authentication if supported.
 */
open class WelcomeActivity : AppCompatActivity() {

    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)
    private lateinit var binding: ActivityWelcomeBinding
    private lateinit var dotsIndicator: Array<ImageView?>
    private lateinit var sliderLayout: IntArray
    private lateinit var introductionAdapter: IntroductionAdapter

    /**
     * Initializes the WelcomeActivity UI, sets up view pager slides,
     * dot indicators, and requests necessary permissions.
     *
     * @param savedInstanceState the saved instance state from a previous configuration (if any)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScreenCaptureProtection.apply(this)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkAppPermissions()
        checkPermissionsBluetooth()
        binding.btnSkip.setOnClickListener {
            navigateToLogin()
        }
        binding.next.setOnClickListener {
            val currentPage = binding.viewPager.currentItem + 1
            if (currentPage < sliderLayout.size) {
                binding.viewPager.setCurrentItem(currentPage)
            } else if (currentPage == sliderLayout.size) {
                navigateToLogin()
            }
        }

        sliderLayout = intArrayOf(
            R.layout.activity_slider_one,
            R.layout.activity_slider_two,
            R.layout.activity_slider_three,
        )
        introductionAdapter = IntroductionAdapter(sliderLayout)
        binding.viewPager.setAdapter(introductionAdapter)

        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (position == 0)
                    binding.next.text = getString(R.string.get_started)
                else
                    binding.next.text = getString(R.string.next)
            }

            override fun onPageSelected(position: Int) {
                setDotStatus(position)
                setScreenText(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                logger.info("onPageScrollStateChanged")
            }
        })
        setDotStatus(0)
    }

    /**
     * Handles navigation to the login screen.
     *
     * Currently logs the navigation action. Implementation for actual
     * navigation should be added here.
     */
    private fun navigateToLogin() {
        logger.info("navigateToLogin")
    }

    /**
     * Updates the dot indicators at the bottom of the screen
     * based on the current onboarding page.
     *
     * @param page The index of the currently selected page.
     */
    fun setDotStatus(page: Int) {
        binding.dotLayout.removeAllViews()
        dotsIndicator = arrayOfNulls(sliderLayout.size)

        for (i in dotsIndicator.indices) {
            dotsIndicator[i] = ImageView(this)
            dotsIndicator[i]!!.setImageDrawable(
                ContextCompat.getDrawable(
                    applicationContext,
                    R.drawable.non_selected_circle_dot
                )
            )
            dotsIndicator[i]!!.setPadding(20, 0, 20, 0)
            binding.dotLayout.addView(dotsIndicator[i])
            dotsIndicator[i]!!.maxWidth = 1
            dotsIndicator[i]!!.maxHeight = 1

            dotsIndicator[i]!!.setOnClickListener { binding.viewPager.currentItem = i }
        }

        // SetCurrent dotsIndicator active
        if (dotsIndicator.isNotEmpty()) {
            dotsIndicator[page]!!.setImageDrawable(
                ContextCompat.getDrawable(
                    applicationContext,
                    R.drawable.selected_circle_dot
                )
            )
            dotsIndicator[page]!!.maxWidth = 2
            dotsIndicator[page]!!.maxHeight = 2
        }
    }

    /**
     * Updates the intro text based on the current slide position.
     *
     * @param position Current page index in the ViewPager.
     */
    fun setScreenText(position: Int) {
        when (position) {
            1 -> binding.tvIntro.text = resources.getText(R.string.welcome_screen2_text1)
            2 -> binding.tvIntro.setText(R.string.welcome_screen3_text1)
            else -> binding.tvIntro.setText(R.string.welcome_screen1_text1)
        }
    }

    /**
     * Checks and requests all Bluetooth-related permissions
     * necessary for scanning or connecting Bluetooth devices.
     */
    private fun checkPermissionsBluetooth() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        if (!permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(this, permissions, 1001)
        }
    }

    /**
     * Checks and requests all necessary app permissions if not already granted.
     * Also initiate biometric authentication if available.
     */
    private fun checkAppPermissions() {
        val permissionsToRequest = getDeniedPermissions(
            listOf(Manifest.permission.CAMERA) + getAndroidTiramisuPermissions()
        )

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1)
        }

        if (BuildConfig.ENABLE_BIOMETRIC) {
            if (isBiometricAvailable()) {
                authenticateWithBiometrics(this)
            } else {
                Toast.makeText(this, BIOMETRIC_NOT_AVAILABLE, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Returns a list of permissions from the provided list that are not yet granted.
     *
     * @param permissions List of permissions to check.
     * @return List of denied permissions.
     */
    private fun getDeniedPermissions(permissions: List<String>): List<String> {
        return permissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Returns additional permissions required for Android Tiramisu (API 33) and above.
     *
     * @return List of additional permissions.
     */
    private fun getAndroidTiramisuPermissions(): List<String> {
        return listOf(Manifest.permission.POST_NOTIFICATIONS)
    }

    /**
     * Checks whether biometric authentication is available and enabled on the device.
     *
     * @return true if biometric authentication is supported and ready; false otherwise.
     */
    private fun isBiometricAvailable(): Boolean {
        if (!BuildConfig.ENABLE_BIOMETRIC) return false
        val biometricManager = BiometricManager.from(this)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    /**
     * Authenticates the user using device biometrics (fingerprint or face).
     *
     * @param activity The current [FragmentActivity] instance used for authentication context.
     */
    private fun authenticateWithBiometrics(activity: FragmentActivity) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt =
            BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(activity, "Authentication Succeeded!", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(activity, "Authentication Failed", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(activity, "Authentication Error: $errString", Toast.LENGTH_SHORT)
                        .show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Use your fingerprint or face to authenticate")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}