package com.lamentcfg.ntfymirror.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.lamentcfg.ntfymirror.NtfyMirrorApplication
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure storage using Android Keystore + AES/GCM encryption.
 * Keys are SHA-256 hashed and values are AES-256-GCM encrypted before
 * being stored in regular SharedPreferences.
 */
class SecureStorage(context: Context) {

    private val sharedPreferences: SharedPreferences
    private val keyStore: KeyStore
    private val gson = Gson()

    init {
        keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        ensureKeyExists()
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun ensureKeyExists() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(AES_KEY_SIZE)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    private fun hashKey(key: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(key.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(AES_GCM_CIPHER)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encrypted: String): String {
        val combined = Base64.decode(encrypted, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(AES_GCM_CIPHER)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    // String Operations

    fun putString(key: String, value: String?) {
        val hashedKey = hashKey(key)
        if (value == null) {
            sharedPreferences.edit().remove(hashedKey).apply()
        } else {
            sharedPreferences.edit().putString(hashedKey, encrypt(value)).apply()
        }
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        val encrypted = sharedPreferences.getString(hashKey(key), null)
        return if (encrypted != null) decrypt(encrypted) else defaultValue
    }

    // Boolean Operations

    fun putBoolean(key: String, value: Boolean) {
        putString(key, value.toString())
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        val value = getString(key) ?: return defaultValue
        return value.toBooleanStrictOrNull() ?: defaultValue
    }

    // Integer Operations

    fun putInt(key: String, value: Int) {
        putString(key, value.toString())
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        val value = getString(key) ?: return defaultValue
        return value.toIntOrNull() ?: defaultValue
    }

    // Long Operations

    fun putLong(key: String, value: Long) {
        putString(key, value.toString())
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        val value = getString(key) ?: return defaultValue
        return value.toLongOrNull() ?: defaultValue
    }

    // Float Operations

    fun putFloat(key: String, value: Float) {
        putString(key, value.toString())
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        val value = getString(key) ?: return defaultValue
        return value.toFloatOrNull() ?: defaultValue
    }

    // StringSet Operations

    fun putStringSet(key: String, values: Set<String>) {
        putString(key, gson.toJson(values))
    }

    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> {
        val json = getString(key) ?: return defaultValue
        val type = object : TypeToken<Set<String>>() {}.type
        return gson.fromJson<Set<String>>(json, type) ?: defaultValue
    }

    // Utility Operations

    fun remove(key: String) {
        sharedPreferences.edit().remove(hashKey(key)).apply()
    }

    fun contains(key: String): Boolean {
        return sharedPreferences.contains(hashKey(key))
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    fun commit() {
        sharedPreferences.edit().commit()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "ntfy_mirror_master_key"
        private const val PREFS_NAME = "ntfy_mirror_secure_prefs"
        private const val AES_GCM_CIPHER = "AES/GCM/NoPadding"
        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH_BITS = 128

        fun getInstance(): SecureStorage {
            return InstanceHolder.instance
        }

        private object InstanceHolder {
            val instance = SecureStorage(NtfyMirrorApplication.ServiceLocator.getContext())
        }
    }
}
