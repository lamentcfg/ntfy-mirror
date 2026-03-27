package com.lamentcfg.ntfymirror.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for SecureStorage using actual EncryptedSharedPreferences.
 * These tests run on a device/emulator to verify real encryption/decryption behavior.
 */
@RunWith(AndroidJUnit4::class)
class SecureStorageIntegrationTest {

    private lateinit var context: Context
    private lateinit var masterKey: MasterKey
    private lateinit var encryptedPrefs: android.content.SharedPreferences
    private lateinit var secureStorage: SecureStorage

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Use a separate prefs file for testing to avoid affecting production data
        encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            TEST_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        secureStorage = SecureStorage(encryptedPrefs)

        // Clear any existing test data
        secureStorage.clearAll()
        secureStorage.commit()
    }

    @After
    fun tearDown() {
        // Clean up test data after each test
        secureStorage.clearAll()
        secureStorage.commit()
    }

    // String Operations Tests

    @Test
    fun putString_and_getString_storesAndRetrievesValue() {
        val testKey = "test_string_key"
        val testValue = "sensitive_data_123"

        secureStorage.putString(testKey, testValue)
        secureStorage.commit()

        assertEquals(testValue, secureStorage.getString(testKey))
    }

    @Test
    fun getString_returnsDefault_whenKeyNotFound() {
        val defaultValue = "default_value"

        val result = secureStorage.getString("nonexistent_key", defaultValue)

        assertEquals(defaultValue, result)
    }

    @Test
    fun getString_returnsNull_whenNoDefaultAndKeyNotFound() {
        val result = secureStorage.getString("nonexistent_key")

        assertNull(result)
    }

    @Test
    fun putString_storesEmptyString() {
        secureStorage.putString("empty_key", "")
        secureStorage.commit()

        assertEquals("", secureStorage.getString("empty_key"))
    }

    @Test
    fun putString_storesNull_removesKey() {
        secureStorage.putString("will_be_null", "value")
        secureStorage.commit()
        secureStorage.putString("will_be_null", null)
        secureStorage.commit()

        assertFalse(secureStorage.contains("will_be_null"))
    }

    @Test
    fun putString_storesUnicodeCharacters() {
        val unicodeValue = "日本語 中文 한글 العربية 🎉🚀"

        secureStorage.putString("unicode_key", unicodeValue)
        secureStorage.commit()

        assertEquals(unicodeValue, secureStorage.getString("unicode_key"))
    }

    @Test
    fun putString_storesLongString() {
        val longValue = "x".repeat(10000)

        secureStorage.putString("long_key", longValue)
        secureStorage.commit()

        assertEquals(longValue, secureStorage.getString("long_key"))
    }

    // Boolean Operations Tests

    @Test
    fun putBoolean_and_getBoolean_storesAndRetrievesTrue() {
        secureStorage.putBoolean("bool_true_key", true)
        secureStorage.commit()

        assertTrue(secureStorage.getBoolean("bool_true_key"))
    }

    @Test
    fun putBoolean_and_getBoolean_storesAndRetrievesFalse() {
        secureStorage.putBoolean("bool_false_key", false)
        secureStorage.commit()

        assertFalse(secureStorage.getBoolean("bool_false_key"))
    }

    @Test
    fun getBoolean_returnsDefault_whenKeyNotFound() {
        assertFalse(secureStorage.getBoolean("nonexistent_bool", false))
        assertTrue(secureStorage.getBoolean("nonexistent_bool", true))
    }

    // Integer Operations Tests

    @Test
    fun putInt_and_getInt_storesAndRetrievesValue() {
        secureStorage.putInt("int_key", 42)
        secureStorage.commit()

        assertEquals(42, secureStorage.getInt("int_key"))
    }

    @Test
    fun putInt_storesNegativeValue() {
        secureStorage.putInt("negative_int_key", -999)
        secureStorage.commit()

        assertEquals(-999, secureStorage.getInt("negative_int_key"))
    }

    @Test
    fun putInt_storesMaxInteger() {
        secureStorage.putInt("max_int_key", Int.MAX_VALUE)
        secureStorage.commit()

        assertEquals(Int.MAX_VALUE, secureStorage.getInt("max_int_key"))
    }

    @Test
    fun getInt_returnsDefault_whenKeyNotFound() {
        assertEquals(123, secureStorage.getInt("nonexistent_int", 123))
    }

    // Long Operations Tests

    @Test
    fun putLong_and_getLong_storesAndRetrievesValue() {
        val testValue = 9876543210123L
        secureStorage.putLong("long_key", testValue)
        secureStorage.commit()

        assertEquals(testValue, secureStorage.getLong("long_key"))
    }

    @Test
    fun putLong_storesNegativeValue() {
        secureStorage.putLong("negative_long_key", -9876543210123L)
        secureStorage.commit()

        assertEquals(-9876543210123L, secureStorage.getLong("negative_long_key"))
    }

    @Test
    fun getLong_returnsDefault_whenKeyNotFound() {
        assertEquals(999L, secureStorage.getLong("nonexistent_long", 999L))
    }

    // Float Operations Tests

    @Test
    fun putFloat_and_getFloat_storesAndRetrievesValue() {
        val testValue = 3.14159f
        secureStorage.putFloat("float_key", testValue)
        secureStorage.commit()

        assertEquals(testValue, secureStorage.getFloat("float_key"), 0.00001f)
    }

    @Test
    fun getFloat_returnsDefault_whenKeyNotFound() {
        assertEquals(2.718f, secureStorage.getFloat("nonexistent_float", 2.718f), 0.00001f)
    }

    // StringSet Operations Tests

    @Test
    fun putStringSet_and_getStringSet_storesAndRetrievesValue() {
        val testSet = setOf("item1", "item2", "item3")
        secureStorage.putStringSet("set_key", testSet)
        secureStorage.commit()

        val result = secureStorage.getStringSet("set_key")
        assertEquals(testSet, result)
    }

    @Test
    fun putStringSet_storesEmptySet() {
        secureStorage.putStringSet("empty_set_key", emptySet())
        secureStorage.commit()

        val result = secureStorage.getStringSet("empty_set_key")
        assertTrue(result.isEmpty())
    }

    @Test
    fun getStringSet_returnsDefault_whenKeyNotFound() {
        val defaultSet = setOf("default1", "default2")

        val result = secureStorage.getStringSet("nonexistent_set", defaultSet)

        assertEquals(defaultSet, result)
    }

    @Test
    fun getStringSet_returnsEmptySet_whenNoDefaultAndKeyNotFound() {
        val result = secureStorage.getStringSet("nonexistent_set")

        assertTrue(result.isEmpty())
    }

    // Remove Operations Tests

    @Test
    fun remove_deletesKey() {
        secureStorage.putString("to_remove", "value")
        secureStorage.commit()
        assertTrue(secureStorage.contains("to_remove"))

        secureStorage.remove("to_remove")
        secureStorage.commit()

        assertFalse(secureStorage.contains("to_remove"))
    }

    @Test
    fun remove_doesNothingForNonexistentKey() {
        // Should not throw
        secureStorage.remove("nonexistent_key")
    }

    // Contains Operations Tests

    @Test
    fun contains_returnsTrue_forExistingKey() {
        secureStorage.putString("existing_key", "value")
        secureStorage.commit()

        assertTrue(secureStorage.contains("existing_key"))
    }

    @Test
    fun contains_returnsFalse_forNonexistentKey() {
        assertFalse(secureStorage.contains("nonexistent_key"))
    }

    // ClearAll Operations Tests

    @Test
    fun clearAll_removesAllKeys() {
        secureStorage.putString("key1", "value1")
        secureStorage.putBoolean("key2", true)
        secureStorage.putInt("key3", 123)
        secureStorage.commit()

        secureStorage.clearAll()
        secureStorage.commit()

        assertFalse(secureStorage.contains("key1"))
        assertFalse(secureStorage.contains("key2"))
        assertFalse(secureStorage.contains("key3"))
    }

    // Overwrite Tests

    @Test
    fun putString_overwritesExistingValue() {
        secureStorage.putString("overwrite_key", "original")
        secureStorage.commit()
        secureStorage.putString("overwrite_key", "updated")
        secureStorage.commit()

        assertEquals("updated", secureStorage.getString("overwrite_key"))
    }

    @Test
    fun putBoolean_overwritesExistingValue() {
        secureStorage.putBoolean("overwrite_bool", true)
        secureStorage.commit()
        secureStorage.putBoolean("overwrite_bool", false)
        secureStorage.commit()

        assertFalse(secureStorage.getBoolean("overwrite_bool"))
    }

    @Test
    fun putInt_overwritesExistingValue() {
        secureStorage.putInt("overwrite_int", 100)
        secureStorage.commit()
        secureStorage.putInt("overwrite_int", 200)
        secureStorage.commit()

        assertEquals(200, secureStorage.getInt("overwrite_int"))
    }

    // Multiple Data Types Tests

    @Test
    fun storesMultipleDataTypes_withSameKeyPrefix() {
        secureStorage.putString("config_url", "https://ntfy.sh")
        secureStorage.putString("config_topic", "mytopic")
        secureStorage.putString("config_username", "user")
        secureStorage.putString("config_password", "secret123")
        secureStorage.putBoolean("config_enabled", true)
        secureStorage.putInt("config_timeout", 30)
        secureStorage.commit()

        assertEquals("https://ntfy.sh", secureStorage.getString("config_url"))
        assertEquals("mytopic", secureStorage.getString("config_topic"))
        assertEquals("user", secureStorage.getString("config_username"))
        assertEquals("secret123", secureStorage.getString("config_password"))
        assertTrue(secureStorage.getBoolean("config_enabled"))
        assertEquals(30, secureStorage.getInt("config_timeout"))
    }

    @Test
    fun storesPerAppSettings_withPackageNameKeys() {
        val packages = listOf("com.app.one", "com.app.two", "com.app.three")

        packages.forEach { pkg ->
            secureStorage.putBoolean("app_enabled_$pkg", true)
            secureStorage.putStringSet("channels_$pkg", setOf("channel1", "channel2"))
        }
        secureStorage.commit()

        packages.forEach { pkg ->
            assertTrue(secureStorage.getBoolean("app_enabled_$pkg"))
            assertEquals(setOf("channel1", "channel2"), secureStorage.getStringSet("channels_$pkg"))
        }
    }

    @Test
    fun storesPerChannelSettings_withCompoundKeys() {
        val packageName = "com.test.app"
        val channels = listOf("alerts", "messages", "updates")

        channels.forEach { channel ->
            val key = "channel_enabled_${packageName}|||$channel"
            secureStorage.putBoolean(key, channel != "updates")
        }
        secureStorage.commit()

        assertTrue(secureStorage.getBoolean("channel_enabled_com.test.app|||alerts"))
        assertTrue(secureStorage.getBoolean("channel_enabled_com.test.app|||messages"))
        assertFalse(secureStorage.getBoolean("channel_enabled_com.test.app|||updates"))
    }

    // Persistence Tests

    @Test
    fun dataPersists_afterCreatingNewStorageInstance() {
        secureStorage.putString("persist_key", "persist_value")
        secureStorage.putInt("persist_int", 42)
        secureStorage.commit()

        // Create a new SecureStorage instance with the same encrypted prefs
        val newStorageInstance = SecureStorage(encryptedPrefs)

        assertEquals("persist_value", newStorageInstance.getString("persist_key"))
        assertEquals(42, newStorageInstance.getInt("persist_int"))
    }

    // Sensitive Data Tests

    @Test
    fun storesServerCredentials_securely() {
        val serverUrl = "https://custom.server.com"
        val topic = "super_secret_topic"
        val username = "admin"
        val password = "super_secret_password_123!"

        secureStorage.putString("server_url", serverUrl)
        secureStorage.putString("topic", topic)
        secureStorage.putString("username", username)
        secureStorage.putString("password", password)
        secureStorage.commit()

        assertEquals(serverUrl, secureStorage.getString("server_url"))
        assertEquals(topic, secureStorage.getString("topic"))
        assertEquals(username, secureStorage.getString("username"))
        assertEquals(password, secureStorage.getString("password"))
    }

    @Test
    fun clearCredentials_removesOnlyCredentialData() {
        secureStorage.putString("server_url", "https://server.com")
        secureStorage.putString("topic", "topic")
        secureStorage.putString("username", "user")
        secureStorage.putString("password", "pass")
        secureStorage.putBoolean("forwarding_enabled", true)
        secureStorage.commit()

        secureStorage.remove("username")
        secureStorage.remove("password")
        secureStorage.commit()

        // Credentials should be gone
        assertNull(secureStorage.getString("username"))
        assertNull(secureStorage.getString("password"))
        // Other settings should remain
        assertEquals("https://server.com", secureStorage.getString("server_url"))
        assertEquals("topic", secureStorage.getString("topic"))
        assertTrue(secureStorage.getBoolean("forwarding_enabled"))
    }

    // Stress Tests

    @Test
    fun handlesManyKeys() {
        val keyCount = 100

        for (i in 0 until keyCount) {
            secureStorage.putString("key_$i", "value_$i")
        }
        secureStorage.commit()

        for (i in 0 until keyCount) {
            assertEquals("value_$i", secureStorage.getString("key_$i"))
        }
    }

    @Test
    fun handlesRapidUpdates() {
        val iterations = 50

        for (i in 0 until iterations) {
            secureStorage.putInt("counter", i)
            secureStorage.commit()
        }

        assertEquals(iterations - 1, secureStorage.getInt("counter"))
    }

    companion object {
        private const val TEST_PREFS_NAME = "ntfy_mirror_test_prefs"
    }
}
