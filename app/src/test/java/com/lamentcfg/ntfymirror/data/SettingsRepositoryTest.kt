package com.lamentcfg.ntfymirror.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for SettingsRepository.
 * Tests the repository's ability to correctly store and retrieve settings
 * using a mocked SecureStorage.
 */
class SettingsRepositoryTest {

    private lateinit var mockStorage: SecureStorage
    private lateinit var repository: SettingsRepository

    // In-memory storage to simulate SharedPreferences behavior
    private val storageMap = mutableMapOf<String, Any?>()

    @Before
    fun setUp() {
        mockStorage = mock {
            // Mock getString to return values from our map
            on { getString(any(), any()) }.thenAnswer { invocation ->
                val key = invocation.getArgument<String>(0)
                val defaultValue = invocation.getArgument<String?>(1)
                storageMap[key] as? String ?: defaultValue
            }
            // Mock getBoolean to return values from our map
            on { getBoolean(any(), any()) }.thenAnswer { invocation ->
                val key = invocation.getArgument<String>(0)
                val defaultValue = invocation.getArgument<Boolean>(1)
                storageMap[key] as? Boolean ?: defaultValue
            }
            // Mock putString to store in our map
            on { putString(any(), any()) }.thenAnswer { invocation ->
                val key = invocation.getArgument<String>(0)
                val value = invocation.getArgument<String?>(1)
                if (value != null) storageMap[key] = value else storageMap.remove(key)
                Unit
            }
            // Mock putBoolean to store in our map
            on { putBoolean(any(), any()) }.thenAnswer { invocation ->
                val key = invocation.getArgument<String>(0)
                val value = invocation.getArgument<Boolean>(1)
                storageMap[key] = value
                Unit
            }
            // Mock getStringSet
            on { getStringSet(any(), any()) }.thenAnswer { invocation ->
                val key = invocation.getArgument<String>(0)
                val defaultValue = invocation.getArgument<Set<String>>(1)
                @Suppress("UNCHECKED_CAST")
                storageMap[key] as? Set<String> ?: defaultValue
            }
            // Mock putStringSet
            on { putStringSet(any(), any()) }.thenAnswer { invocation ->
                val key = invocation.getArgument<String>(0)
                val value = invocation.getArgument<Set<String>>(1)
                storageMap[key] = value
                Unit
            }
            // Mock remove
            on { remove(any()) }.thenAnswer { invocation ->
                val key = invocation.getArgument<String>(0)
                storageMap.remove(key)
                Unit
            }
        }
        storageMap.clear()
        repository = SettingsRepository(mockStorage)
    }

    // Server URL Tests

    @Test
    fun `getServerUrl returns default when not set`() {
        assertEquals(SettingsRepository.DEFAULT_SERVER_URL, repository.getServerUrl())
    }

    @Test
    fun `setServerUrl stores and retrieves value`() {
        val testUrl = "https://custom.ntfy.server.com"
        repository.setServerUrl(testUrl)
        assertEquals(testUrl, repository.getServerUrl())
        verify(mockStorage).putString("server_url", testUrl)
    }

    // Topic Tests

    @Test
    fun `getTopic returns empty string when not set`() {
        assertEquals("", repository.getTopic())
    }

    @Test
    fun `setTopic stores and retrieves value`() {
        val testTopic = "my-secret-topic"
        repository.setTopic(testTopic)
        assertEquals(testTopic, repository.getTopic())
        verify(mockStorage).putString("topic", testTopic)
    }

    // Username Tests

    @Test
    fun `getUsername returns empty string when not set`() {
        assertEquals("", repository.getUsername())
    }

    @Test
    fun `setUsername stores and retrieves value`() {
        val testUsername = "testuser"
        repository.setUsername(testUsername)
        assertEquals(testUsername, repository.getUsername())
        verify(mockStorage).putString("username", testUsername)
    }

    // Password Tests

    @Test
    fun `getPassword returns empty string when not set`() {
        assertEquals("", repository.getPassword())
    }

    @Test
    fun `setPassword stores and retrieves value`() {
        val testPassword = "secretpassword123"
        repository.setPassword(testPassword)
        assertEquals(testPassword, repository.getPassword())
        verify(mockStorage).putString("password", testPassword)
    }

    // hasCredentials Tests

    @Test
    fun `hasCredentials returns false when no credentials set`() {
        assertFalse(repository.hasCredentials())
    }

    @Test
    fun `hasCredentials returns false when only username set`() {
        repository.setUsername("user")
        assertFalse(repository.hasCredentials())
    }

    @Test
    fun `hasCredentials returns false when only password set`() {
        repository.setPassword("pass")
        assertFalse(repository.hasCredentials())
    }

    @Test
    fun `hasCredentials returns true when both username and password set`() {
        repository.setUsername("user")
        repository.setPassword("pass")
        assertTrue(repository.hasCredentials())
    }

    // isConfigured Tests

    @Test
    fun `isConfigured returns false when topic not set`() {
        // Server URL has a default, but topic is empty
        repository.setServerUrl("https://server.com")
        repository.setTopic("")
        assertFalse(repository.isConfigured())
    }

    @Test
    fun `isConfigured returns true when server url and topic are set`() {
        repository.setServerUrl("https://server.com")
        repository.setTopic("mytopic")
        assertTrue(repository.isConfigured())
    }

    @Test
    fun `isConfigured returns true with default server url and topic`() {
        repository.setTopic("mytopic")
        assertTrue(repository.isConfigured())
    }

    // Forwarding Enabled Tests

    @Test
    fun `isForwardingEnabled returns default true when not set`() {
        assertTrue(repository.isForwardingEnabled())
    }

    @Test
    fun `setForwardingEnabled stores and retrieves value`() {
        repository.setForwardingEnabled(false)
        assertFalse(repository.isForwardingEnabled())
        verify(mockStorage).putBoolean("forwarding_enabled", false)
    }

    // Forward Ongoing Tests

    @Test
    fun `shouldForwardOngoing returns default false when not set`() {
        assertFalse(repository.shouldForwardOngoing())
    }

    @Test
    fun `setForwardOngoing stores and retrieves value`() {
        repository.setForwardOngoing(true)
        assertTrue(repository.shouldForwardOngoing())
        verify(mockStorage).putBoolean("forward_ongoing", true)
    }

    // Include Body Tests

    @Test
    fun `shouldIncludeBody returns default true when not set`() {
        assertTrue(repository.shouldIncludeBody())
    }

    @Test
    fun `setIncludeBody stores and retrieves value`() {
        repository.setIncludeBody(false)
        assertFalse(repository.shouldIncludeBody())
        verify(mockStorage).putBoolean("include_body", false)
    }

    // Per-App Settings Tests

    @Test
    fun `isAppEnabled returns true by default for unknown app`() {
        assertTrue(repository.isAppEnabled("com.unknown.app"))
    }

    @Test
    fun `setAppEnabled stores and retrieves value`() {
        val packageName = "com.test.app"
        repository.setAppEnabled(packageName, false)
        assertFalse(repository.isAppEnabled(packageName))
        verify(mockStorage).putBoolean("app_enabled_com.test.app", false)
    }

    @Test
    fun `getConfiguredApps returns empty set when none configured`() {
        assertTrue(repository.getConfiguredApps().isEmpty())
    }

    @Test
    fun `markAppConfigured adds app to configured set`() {
        repository.markAppConfigured("com.app.one")
        assertTrue(repository.getConfiguredApps().contains("com.app.one"))
    }

    @Test
    fun `markAppConfigured does not duplicate entries`() {
        repository.markAppConfigured("com.app.test")
        repository.markAppConfigured("com.app.test")
        assertEquals(1, repository.getConfiguredApps().size)
    }

    // Per-Channel Settings Tests

    @Test
    fun `isChannelEnabled returns true by default for unknown channel`() {
        assertTrue(repository.isChannelEnabled("com.test.app", "channel_id"))
    }

    @Test
    fun `setChannelEnabled stores and retrieves value`() {
        val packageName = "com.test.app"
        val channelId = "notifications"
        repository.setChannelEnabled(packageName, channelId, false)
        assertFalse(repository.isChannelEnabled(packageName, channelId))
        verify(mockStorage).putBoolean("channel_enabled_com.test.app|||notifications", false)
    }

    @Test
    fun `getConfiguredChannels returns empty set when none configured`() {
        assertTrue(repository.getConfiguredChannels("com.test.app").isEmpty())
    }

    @Test
    fun `markChannelConfigured adds channel to configured set`() {
        repository.markChannelConfigured("com.test.app", "channel_one")
        assertTrue(repository.getConfiguredChannels("com.test.app").contains("channel_one"))
    }

    @Test
    fun `markChannelConfigured does not duplicate entries`() {
        repository.markChannelConfigured("com.test.app", "channel_test")
        repository.markChannelConfigured("com.test.app", "channel_test")
        assertEquals(1, repository.getConfiguredChannels("com.test.app").size)
    }

    // clearCredentials Tests

    @Test
    fun `clearCredentials removes username and password`() {
        repository.setUsername("user")
        repository.setPassword("pass")
        assertTrue(repository.hasCredentials())

        repository.clearCredentials()

        assertFalse(repository.hasCredentials())
        assertEquals("", repository.getUsername())
        assertEquals("", repository.getPassword())
        verify(mockStorage).remove("username")
        verify(mockStorage).remove("password")
    }

    // clearAppSettings Tests

    @Test
    fun `clearAppSettings removes all app and channel settings`() {
        // Set up some app and channel settings
        repository.markAppConfigured("com.app.one")
        repository.markAppConfigured("com.app.two")
        repository.setAppEnabled("com.app.one", false)
        repository.markChannelConfigured("com.app.one", "channel1")
        repository.setChannelEnabled("com.app.one", "channel1", false)

        repository.clearAppSettings()

        assertTrue(repository.getConfiguredApps().isEmpty())
        assertTrue(repository.getConfiguredChannels("com.app.one").isEmpty())
        assertTrue(repository.isAppEnabled("com.app.one")) // Back to default
        assertTrue(repository.isChannelEnabled("com.app.one", "channel1")) // Back to default
    }

    // resetToDefaults Tests

    @Test
    fun `resetToDefaults clears all settings and restores defaults`() {
        // Set up various settings
        repository.setServerUrl("https://custom.com")
        repository.setTopic("mytopic")
        repository.setUsername("user")
        repository.setPassword("pass")
        repository.setForwardingEnabled(false)
        repository.setForwardOngoing(true)
        repository.setIncludeBody(false)
        repository.markAppConfigured("com.test.app")
        repository.setAppEnabled("com.test.app", false)

        repository.resetToDefaults()

        assertEquals(SettingsRepository.DEFAULT_SERVER_URL, repository.getServerUrl())
        assertEquals("", repository.getTopic())
        assertEquals("", repository.getUsername())
        assertEquals("", repository.getPassword())
        assertEquals(SettingsRepository.DEFAULT_FORWARDING_ENABLED, repository.isForwardingEnabled())
        assertEquals(SettingsRepository.DEFAULT_FORWARD_ONGOING, repository.shouldForwardOngoing())
        assertEquals(SettingsRepository.DEFAULT_INCLUDE_BODY, repository.shouldIncludeBody())
        assertTrue(repository.getConfiguredApps().isEmpty())
    }

    // Default Values Companion Object Tests

    @Test
    fun `DEFAULT_SERVER_URL is ntfy_sh`() {
        assertEquals("https://ntfy.sh", SettingsRepository.DEFAULT_SERVER_URL)
    }

    @Test
    fun `DEFAULT_FORWARDING_ENABLED is true`() {
        assertTrue(SettingsRepository.DEFAULT_FORWARDING_ENABLED)
    }

    @Test
    fun `DEFAULT_FORWARD_ONGOING is false`() {
        assertFalse(SettingsRepository.DEFAULT_FORWARD_ONGOING)
    }

    @Test
    fun `DEFAULT_INCLUDE_BODY is true`() {
        assertTrue(SettingsRepository.DEFAULT_INCLUDE_BODY)
    }
}
