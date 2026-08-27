// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * File Name: MyApplication.kt
 * Description: MyApplication.kt initializes Firebase and loads app properties during startup.
 * It also registers a BluetoothStateReceiver to monitor Bluetooth connection and disconnection events.
 **/
package com.infineon.secora.wallet

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.infineon.secora.wallet.client.util.PropertiesLoader
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateReceiver
import com.infineon.secora.wallet.oidc.MicrosoftSignInFlowCoordinator
import com.infineon.secora.wallet.utils.helper.DeviceIntegrity

/**
 * Custom Application class for initializing global app components.
 *
 * This class is responsible for setting up Firebase, loading configuration
 * properties, and registering a Bluetooth broadcast receiver for monitoring
 * device connection states.
 *
 * 本模块作为库集成到宿主 app 时，宿主已有自己的 Application，
 * 因此初始化逻辑收敛到 [initialize]，由宿主在进入支付模块前调用一次。
 */
class MyApplication : Application() {

    companion object {
        lateinit var appContext: Context

        private val logger: ApplicationLogger = getApplicationLogger("MyApplication")

        private val bluetoothStateReceiver = BluetoothStateReceiver()

        private var initialized = false

        /**
         * Initializes Firebase, loads custom properties, and registers a
         * [BluetoothStateReceiver] to listen for Bluetooth connection and
         * disconnection events.
         *
         * 幂等：宿主可以在每次进入支付模块时调用。
         */
        @JvmStatic
        @Synchronized
        fun initialize(application: Application) {
            if (initialized) {
                return
            }
            initialized = true
            appContext = application
            ApplicationLogger.initialize(application)
            logger.debug("AppInit : Starting application initialization")

            loadAppProperties(application)
            initializeFirebase(application)
            logDeviceIntegrity()
            registerBluetoothReceiver(application)
            initializeAmplify(application)
            MicrosoftSignInFlowCoordinator.init(application)
        }

        /**
         * Ensures the default Firebase app exists before FCM token requests.
         * google-services.json in the host app is preferred; falls back to the same project options.
         */
        private fun initializeFirebase(application: Application) {
            try {
                if (FirebaseApp.getApps(application).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setProjectId("anywear-a3e6e")
                        .setApplicationId("1:5343513359:android:6883820471a7fc5631cc7a")
                        .setApiKey("AIzaSyBYIOKuOCKkiCxoKG_pRLKwEtIHMyzeXKQ")
                        .setGcmSenderId("5343513359")
                        .build()
                    val app = FirebaseApp.initializeApp(application, options)
                        ?: FirebaseApp.initializeApp(application)
                    logger.debug(
                        "FirebaseInit : initialized app=${app?.name} project=${app?.options?.projectId}"
                    )
                } else {
                    val app = FirebaseApp.getInstance()
                    logger.debug(
                        "FirebaseInit : already initialized project=${app.options.projectId} " +
                            "appId=${app.options.applicationId}"
                    )
                }
            } catch (e: Exception) {
                logger.debug("FirebaseInit : Failed to init Firebase : ${e.message}")
            }
        }

        /**
         * Checks device integrity at startup and logs whether root indicators were detected.
         */
        private fun logDeviceIntegrity() {
            if (DeviceIntegrity.isDeviceRooted()) {
                logger.debug("DeviceIntegrity : Root indicators detected")
            } else {
                logger.debug("DeviceIntegrity : No root indicators detected")
            }
        }

        /**
         * Loads application configuration properties via [PropertiesLoader].
         *
         * Failures are logged and do not interrupt application startup.
         */
        private fun loadAppProperties(application: Application) {
            try {
                PropertiesLoader.loadProperties(application)
                logger.debug("PropertiesInit : App properties loaded")
            } catch (e: Exception) {
                logger.debug("PropertiesInit : Failed to load app properties : ${e.message}")
            }
        }

        /**
         * Initializes AWS Amplify with the Cognito auth plugin.
         *
         * Failures are logged and do not interrupt application startup.
         */
        private fun initializeAmplify(application: Application) {
            try {
                Amplify.addPlugin(AWSCognitoAuthPlugin())
                Amplify.configure(application.applicationContext)
                logger.debug("AmplifyInit : Amplify Configured")
            } catch (e: Exception) {
                logger.debug("AmplifyInit : Failed to init Amplify : ${e.message}")
            }
        }

        /**
         * Registers [BluetoothStateReceiver] for Bluetooth adapter and ACL connection events.
         *
         * Registration failures are logged and do not interrupt application startup.
         */
        private fun registerBluetoothReceiver(application: Application) {
            val filter = IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }

            try {
                ContextCompat.registerReceiver(application, bluetoothStateReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
                logger.debug("BluetoothReceiver : Registered at application scope")
            } catch (e: Exception) {
                logger.debug("BluetoothReceiver : Registration failed : ${e.message}")
            }
        }
    }

    /**
     * Called when the application is created.
     */
    override fun onCreate() {
        super.onCreate()
        initialize(this)
    }
}
