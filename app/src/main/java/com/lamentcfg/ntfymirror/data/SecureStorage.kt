package com.lamentcfg.ntfymirror.data

import android.content.SharedPreferences
import com.lamentcfg.ntfymirror.NtfyMirrorApplication

/**
 * Secure storage wrapper using EncryptedSharedPreferences.
 * Provides type-safe methods for storing and retrieving sensitive app configuration.
 */
class SecureStorage(private val sharedPreferences: SharedPreferences) {

    /**
     * Stores a string value securely.
     */
    fun putString(key: String, value: String?) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    /**
     * Retrieves a string value from secure storage.
     */
    fun getString(key: String, defaultValue: String? = null): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    /**
     * Stores a boolean value securely.
     */
    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    /**
     * Retrieves a boolean value from secure storage.
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    /**
     * Stores a string set securely.
     */
    fun putStringSet(key: String, values: Set<String>) {
        sharedPreferences.edit().putStringSet(key, values).apply()
    }

    /**
     * Retrieves a string set from secure storage.
     */
    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> {
        return sharedPreferences.getStringSet(key, defaultValue) ?: defaultValue
    }

    /**
     * Removes a value from secure storage.
     */
    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    /**
     * Checks if a key exists in secure storage.
     */
    fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    /**
     * Clears all stored values.
     */
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        /**
         * Gets the singleton instance of SecureStorage using the app's encrypted preferences.
         */
        fun getInstance(): SecureStorage {
            return InstanceHolder.instance
        }

        private object InstanceHolder {
            val instance = SecureStorage(
                NtfyMirrorApplication.ServiceLocator.getEncryptedSharedPreferences()
            )
        }
    }
}
