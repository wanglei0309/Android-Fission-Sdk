// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

package com.infineon.secora.wallet.firebase

import android.os.Handler
import android.os.Looper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.infineon.secora.wallet.MyApplication
import com.infineon.secora.wallet.client.operations.common.logger.Logger
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey

object FirebaseManager {

    private val logger: Logger = Logger.getNewLogger(FirebaseManager::class.java.name)
    private val mainHandler = Handler(Looper.getMainLooper())

    private const val MAX_RETRIES = 2
    private const val RETRY_DELAY_MS = 1500L
    private const val SERVICE_NOT_AVAILABLE = "SERVICE_NOT_AVAILABLE"

    fun isFirebaseReady(context: android.content.Context = MyApplication.appContext): Boolean {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                return false
            }
            FirebaseApp.getInstance()
            true
        } catch (e: Exception) {
            logger.debug("fcm :: Firebase not ready: ${e.message}")
            false
        }
    }

    /**
     * Fetches the FCM registration token required by the wallet login API.
     */
    fun fetchToken(onSuccess: (String) -> Unit, onFailure: (String?) -> Unit) {
        val context = MyApplication.appContext
        if (!isFirebaseReady(context)) {
            onFailure("Firebase not initialized. Ensure google-services.json is configured for the host app.")
            return
        }

        val playStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        val playError = GoogleApiAvailability.getInstance().getErrorString(playStatus)
        logger.debug("fcm :: Play Services status=$playStatus ($playError)")

        if (playStatus != ConnectionResult.SUCCESS) {
            onFailure("Google Play Services unavailable ($playStatus): $playError")
            return
        }

        try {
            val options = FirebaseApp.getInstance().options
            logger.debug(
                "fcm :: project=${options.projectId} appId=${options.applicationId} sender=${options.gcmSenderId}"
            )
        } catch (e: Exception) {
            onFailure("Firebase not initialized: ${e.message}")
            return
        }

        fetchTokenInternal(onSuccess, onFailure, MAX_RETRIES)
    }

    private fun fetchTokenInternal(
        onSuccess: (String) -> Unit,
        onFailure: (String?) -> Unit,
        retriesLeft: Int
    ) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result?.trim().orEmpty()
                    if (token.isEmpty()) {
                        onFailure("FCM returned an empty token")
                        return@addOnCompleteListener
                    }
                    logger.debug("fcm :: fetchToken, Success : $token")
                    onSuccess(token)
                    return@addOnCompleteListener
                }
                val exception = task.exception
                val errorMessage = exception?.message
                logger.debug("fcm :: fetchToken, Failed : $exception")
                retryOrFail(errorMessage, onSuccess, onFailure, retriesLeft)
            }
    }

    private fun retryOrFail(
        errorMessage: String?,
        onSuccess: (String) -> Unit,
        onFailure: (String?) -> Unit,
        retriesLeft: Int
    ) {
        val canRetry = retriesLeft > 0 &&
            errorMessage?.contains(SERVICE_NOT_AVAILABLE, ignoreCase = true) == true
        if (!canRetry) {
            onFailure(errorMessage)
            return
        }
        logger.debug("fcm :: fetchToken retrying, retriesLeft=${retriesLeft - 1}")
        mainHandler.postDelayed(
            { fetchTokenInternal(onSuccess, onFailure, retriesLeft - 1) },
            RETRY_DELAY_MS
        )
    }

    fun deleteToken(shouldDelete: Boolean) {
        if (shouldDelete) {
            FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        logger.debug("fcm :: token deleted")
                        StorageRepository.clearString(PreferenceKey.FCM_TOKEN)
                    } else {
                        logger.debug("fcm :: token delete failed")
                    }
                }.addOnFailureListener {
                    logger.debug("fcm :: token delete failed onFail")
                }
        }
    }
}
