// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: FcmMessagingService.kt
 *
 * This service extends [com.google.firebase.messaging.FirebaseMessagingService] to handle
 * **Firebase Cloud Messaging (FCM)** events.
 *
 * Responsibilities include:
 * - Receiving and persisting FCM registration tokens
 * - Handling incoming FCM messages containing notification and data payloads
 * - Processing message data
 * - Displaying local notifications to the user
 */

package com.infineon.secora.wallet.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.client.operations.common.logger.Logger
import com.infineon.secora.wallet.client.util.AppDispatchers
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.domain.walletsdk.WalletRepository
import com.infineon.secora.wallet.utils.Utils
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.constants.Constants
import com.infineon.secora.wallet.utils.constants.Constants.DEVICE_RESUME_UPDATE
import com.infineon.secora.wallet.utils.constants.Constants.DEVICE_SUSPEND_UPDATE
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_FORCE_LOGOUT
import com.infineon.secora.wallet.utils.constants.Constants.DEVICE_LOGOUT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FirebaseMessageService : FirebaseMessagingService() {
    private val logger = Logger.getNewLogger(FirebaseMessageService::class.java.name.toString())
    private val titleKey = "title"

    /**
     * Called whenever a new Firebase Cloud Messaging (FCM) registration token is generated.
     *
     * This method stores the updated token in shared preferences so it can be
     * accessed later for push notification registration or backend synchronization.
     *
     * @param token The newly generated FCM token for this device.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        logger.debug(" :: fcm :: onNewToken : $token")
        StorageRepository.saveString(PreferenceKey.FCM_TOKEN, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        handleSecoraWalletNotifications(remoteMessage)
    }

    private fun handleSecoraWalletNotifications(remoteMessage: RemoteMessage) {
        val notificationTitle = remoteMessage.data[titleKey]
        logger.debug(" :: fcm :: notificationTitle : $notificationTitle")

        if (remoteMessage.data[BundleKey.MSG_TYPE].equals(DEVICE_LOGOUT, ignoreCase = true)) {
            FirebaseManager.deleteToken(StorageRepository.readString(PreferenceKey.FCM_TOKEN).isNotEmpty())
            EventBus.post(ACTION_FORCE_LOGOUT)
        }

        if (Utils.isUserNotLoggedIn()) {
            logger.debug(" :: User not logged in")
            FirebaseManager.deleteToken(StorageRepository.readString(PreferenceKey.FCM_TOKEN).isNotEmpty())
            return
        }
        val isTransactionNotification = notificationTitle.toString().equals(getString(R.string.txn_notification_title),true)
        if (notificationTitle.toString() == getString(R.string.notification_detach_device) || notificationTitle.toString() == getString(
                R.string.notification_detach_device_with_underscore
            )
        ) {
            val data = mutableMapOf<String, String?>()
            remoteMessage.data[BundleKey.PAYMENT_APP_INSTANCE_ID]?.let {
                data[BundleKey.PAYMENT_APP_INSTANCE_ID] = it
            }
            remoteMessage.data[BundleKey.DEVICE_SE_ID]?.let { data[BundleKey.DEVICE_SE_ID] = it }
            EventBus.post(Constants.ACTION_NAVIGATE_LISTENER, data)
        }

        var notificationNotRequired = false
        if (notificationTitle.toString() == getString(R.string.txn_notification_title)) {
            remoteMessage.data[Constants.REGISTRATION_CODE2]?.let { code ->
                remoteMessage.data[Constants.TOKEN_UNIQUE_REFERENCE_NO]?.let {
                    CoroutineScope(AppDispatchers.IO).launch {
                        notificationNotRequired = true
                        WalletRepository.registrationDetailsToMTF(
                            registrationCode = code,
                            digitizeReferenceNumber = it
                        )
                    }
                }
            }

        }
        EventBus.post(Constants.ACTION_FORCE_REFRESH_TXN)
        val dataPayload = mapOf(
            BundleKey.MSG_TYPE to remoteMessage.data[BundleKey.MSG_TYPE].toString(),
            BundleKey.ENTITY_ID to remoteMessage.data[BundleKey.ENTITY_ID].toString(),
            BundleKey.DEVICE_NAME to remoteMessage.data[BundleKey.DEVICE_NAME].toString(),
        )

        checkActiveSuspendState(dataPayload)
        // No host-maintained routing state: each screen subscribes via repeatOnLifecycle, so only the
        // visible Fragment handles its action. Mirrors former exclusive when-branch behavior without
        // requiring the integrator to call SDK APIs from UI code.
        if (!isTransactionNotification) {
            EventBus.post(Constants.ACTION_LISTENER)
        }
        EventBus.post(Constants.ACTION_TOGGLE, dataPayload)
        EventBus.post(Constants.ACTION_CARD, dataPayload)
        var pendingIntent : PendingIntent? = null
        if (isTransactionNotification) {
            pendingIntent = getPendingIntent(remoteMessage)
        }
        logger.debug(":: isTransactionNotification : $isTransactionNotification")
        // Don't show notification for add card
        if (notificationNotRequired || notificationTitle.equals(getString(R.string.add_card_notification), ignoreCase = true) || notificationTitle.equals(getString(R.string.card_provision_update), ignoreCase = true)) {
            logger.debug("Skipping notification")
            return
        }
        sendNotification(notificationTitle.toString(),pendingIntent)
    }

    /**
     * to read fcm device status if suspended or active state and post event to ui
     */
    private fun checkActiveSuspendState(payload: Map<String, String>) {
        val msgType = payload[BundleKey.MSG_TYPE]
        logger.debug("deviceStatus :: msgType " + msgType);
        if (msgType.equals(DEVICE_RESUME_UPDATE) || msgType.equals(DEVICE_SUSPEND_UPDATE)){
            EventBus.post(Constants.ACTION_DEVICE_STATUS_UPDATE, payload)
        }
    }

    /**
     * Builds pending intent which is required for navigation on notification click.
     *
     */
    private fun getPendingIntent(remoteMessage: RemoteMessage) : PendingIntent {
        val digitizationReferenceNumber= remoteMessage.data[BundleKey.ENTITY_ID].toString()
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            // Pass fragment info to host app
            putExtra(BundleKey.ENTITY_ID, digitizationReferenceNumber)

            // Ensure only one instance of launcher activity
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent
    }

    /**
     * Builds and displays a local notification with the provided title and message.
     *
     * Also creates a notification channel if required (Android 8.0+).
     *
     * @param title The title of the notification.
     * @param intent pending intent which is required for navigation on notification click.
     */
    private fun sendNotification(title: String, intent: PendingIntent?) {
        val channelId = "my_channel_id"
        val notificationId = System.currentTimeMillis().toInt()

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_background)  // Ensure you have an icon // Change this
            .setContentTitle(title)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(intent)

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "My Notification Channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}