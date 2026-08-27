// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: Encrypted preferences for the host **application** module only.
 * The SDK keeps its own internal persistence; the app does not read or write SDK storage.
 */
package com.infineon.secora.wallet.data.local.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.infineon.secora.wallet.client.operations.common.logger.Logger

class AppPreferenceStorage private constructor() {

    companion object {
        private const val PREFS_NAME = "SECORA_WALLET_APP_ENCRYPTED_PREFS"

        @Volatile
        private var instance: AppPreferenceStorage? = null
        private val LOCK = Any()
        private val logger = Logger.getNewLogger(AppPreferenceStorage::class.simpleName.toString())

        @Volatile
        private var sharedPreferences: SharedPreferences? = null

        /**
         * Returns the singleton encrypted app preference storage.
         *
         * @param context The application or activity context.
         * @return Singleton instance of [AppPreferenceStorage].
         */
        fun getInstance(context: Context): AppPreferenceStorage {
            sharedPreferences = sharedPreferences ?: synchronized(LOCK) {
                sharedPreferences ?: getEncryptedSharePreference(
                    context.applicationContext
                )
            }

            return instance ?: synchronized(LOCK) {
                instance ?: AppPreferenceStorage().also { instance = it }
            }
        }

        /**
         * Retrieves an instance of [androidx.security.crypto.EncryptedSharedPreferences] for secure key-value storage.
         *
         * @param context The application context used to access storage and cryptographic keys.
         * @param file The name of the preference file.
         * @return A secured instance of [android.content.SharedPreferences].
         * @throws IllegalStateException If there's an error creating encrypted preferences.
         */
        @Throws(IllegalStateException::class)
        private fun getEncryptedSharePreference(context: Context): SharedPreferences {
            try {
                return EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                throw IllegalStateException(e.message, e)
            }
        }
    }

    /**
     * Stores a string preference in encrypted storage.
     *
     * @param key The preference key.
     * @param value The string value to persist.
     */
    fun setPreferenceForString(key: String, value: String) {
        try {
            sharedPreferences?.edit { putString(key, value) }
        } catch (e: Exception) {
            logger.debug(e.message ?: "Unknown error in setPreferenceForString")
        }
    }

    /**
     * Reads a string preference from encrypted storage.
     *
     * @param key The preference key to look up.
     * @return Stored value for the key, or an empty string if missing or on error.
     */
    fun getPreferenceForString(key: String): String {
        return try {
            sharedPreferences?.getString(key, "") ?: ""
        } catch (e: Exception) {
            logger.debug(e.message ?: "Unknown error in getPreferenceForString")
            ""
        }
    }

    /**
     * Stores a boolean preference in encrypted storage.
     *
     * @param key The preference key.
     * @param value The boolean value to persist.
     */
    fun setPreferenceForBoolean(key: String, value: Boolean) {
        try {
            sharedPreferences?.edit { putBoolean(key, value) }
        } catch (e: Exception) {
            logger.debug(e.message ?: "Unknown error in setPreferenceForBoolean")
        }
    }

    /**
     * Reads a boolean preference from encrypted storage.
     *
     * @param key The preference key to look up.
     * @return Stored value for the key, or false if missing or on error.
     */
    fun getPreferenceForBoolean(key: String): Boolean {
        return try {
            sharedPreferences?.getBoolean(key, false) ?: false
        } catch (e: Exception) {
            logger.debug(e.message ?: "Unknown error in getPreferenceForBoolean")
            false
        }
    }
}
