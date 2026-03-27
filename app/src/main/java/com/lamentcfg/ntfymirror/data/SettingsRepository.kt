package com.lamentcfg.ntfymirror.data

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Repository for managing app configuration settings.
 * Provides a type-safe API for accessing and modifying all app settings.
 * Uses SecureStorage for persistence with in-memory caching via StateFlow.
 */
class SettingsRepository(private val storage: SecureStorage) {

    // Internal mutable state flows for reactive updates
    private val _serverUrl = MutableStateFlow(getStoredServerUrl())
    private val _topic = MutableStateFlow(getStoredTopic())
    private val _username = MutableStateFlow(getStoredUsername())
    private val _password = MutableStateFlow(getStoredPassword())
    private val _forwardingEnabled = MutableStateFlow(getStoredForwardingEnabled())
    private val _forwardOngoing = MutableStateFlow(getStoredForwardOngoing())
    private val _includeBody = MutableStateFlow(getStoredIncludeBody())
    private val _batteryOptimizationPromptShown = MutableStateFlow(getStoredBatteryOptimizationPromptShown())

    // Server Configuration

    /**
     * Gets the ntfy server URL.
     * Default: DEFAULT_SERVER_URL (ntfy.sh)
     */
    fun getServerUrl(): String = _serverUrl.value

    /**
     * Sets the ntfy server URL.
     */
    fun setServerUrl(url: String) {
        storage.putString(KEY_SERVER_URL, url)
        _serverUrl.value = url
    }

    private fun getStoredServerUrl(): String {
        return storage.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    /**
     * Gets the ntfy topic.
     * This is treated as sensitive data.
     */
    fun getTopic(): String = _topic.value

    /**
     * Sets the ntfy topic.
     */
    fun setTopic(topic: String) {
        storage.putString(KEY_TOPIC, topic)
        _topic.value = topic
    }

    private fun getStoredTopic(): String {
        return storage.getString(KEY_TOPIC, "") ?: ""
    }

    /**
     * Gets the optional ntfy username for authentication.
     */
    fun getUsername(): String = _username.value

    /**
     * Sets the optional ntfy username.
     */
    fun setUsername(username: String) {
        storage.putString(KEY_USERNAME, username)
        _username.value = username
    }

    private fun getStoredUsername(): String {
        return storage.getString(KEY_USERNAME, "") ?: ""
    }

    /**
     * Gets the optional ntfy password for authentication.
     * This is treated as sensitive data.
     */
    fun getPassword(): String = _password.value

    /**
     * Sets the optional ntfy password.
     */
    fun setPassword(password: String) {
        storage.putString(KEY_PASSWORD, password)
        _password.value = password
    }

    private fun getStoredPassword(): String {
        return storage.getString(KEY_PASSWORD, "") ?: ""
    }

    /**
     * Checks if server credentials (username/password) are configured.
     */
    fun hasCredentials(): Boolean {
        return _username.value.isNotEmpty() && _password.value.isNotEmpty()
    }

    /**
     * Checks if the basic server configuration is complete.
     */
    fun isConfigured(): Boolean {
        return _serverUrl.value.isNotEmpty() && _topic.value.isNotEmpty()
    }

    // Global Settings

    /**
     * Gets whether notification forwarding is globally enabled.
     */
    fun isForwardingEnabled(): Boolean = _forwardingEnabled.value

    /**
     * Sets whether notification forwarding is globally enabled.
     */
    fun setForwardingEnabled(enabled: Boolean) {
        storage.putBoolean(KEY_FORWARDING_ENABLED, enabled)
        _forwardingEnabled.value = enabled
    }

    private fun getStoredForwardingEnabled(): Boolean {
        return storage.getBoolean(KEY_FORWARDING_ENABLED, DEFAULT_FORWARDING_ENABLED)
    }

    /**
     * Gets whether to forward ongoing notifications.
     */
    fun shouldForwardOngoing(): Boolean = _forwardOngoing.value

    /**
     * Sets whether to forward ongoing notifications.
     */
    fun setForwardOngoing(enabled: Boolean) {
        storage.putBoolean(KEY_FORWARD_ONGOING, enabled)
        _forwardOngoing.value = enabled
    }

    private fun getStoredForwardOngoing(): Boolean {
        return storage.getBoolean(KEY_FORWARD_ONGOING, DEFAULT_FORWARD_ONGOING)
    }

    /**
     * Gets whether to include notification body in forwarded messages.
     */
    fun shouldIncludeBody(): Boolean = _includeBody.value

    /**
     * Sets whether to include notification body in forwarded messages.
     */
    fun setIncludeBody(enabled: Boolean) {
        storage.putBoolean(KEY_INCLUDE_BODY, enabled)
        _includeBody.value = enabled
    }

    private fun getStoredIncludeBody(): Boolean {
        return storage.getBoolean(KEY_INCLUDE_BODY, DEFAULT_INCLUDE_BODY)
    }

    /**
     * Gets whether the battery optimization prompt has been shown.
     */
    fun wasBatteryOptimizationPromptShown(): Boolean = _batteryOptimizationPromptShown.value

    /**
     * Sets whether the battery optimization prompt has been shown.
     */
    fun setBatteryOptimizationPromptShown(shown: Boolean) {
        storage.putBoolean(KEY_BATTERY_OPTIMIZATION_PROMPT_SHOWN, shown)
        _batteryOptimizationPromptShown.value = shown
    }

    private fun getStoredBatteryOptimizationPromptShown(): Boolean {
        return storage.getBoolean(KEY_BATTERY_OPTIMIZATION_PROMPT_SHOWN, false)
    }

    // Per-App Settings

    /**
     * Checks if forwarding is enabled for a specific app package.
     */
    fun isAppEnabled(packageName: String): Boolean {
        return storage.getBoolean(KEY_APP_ENABLED_PREFIX + packageName, true)
    }

    /**
     * Sets whether forwarding is enabled for a specific app package.
     */
    fun setAppEnabled(packageName: String, enabled: Boolean) {
        storage.putBoolean(KEY_APP_ENABLED_PREFIX + packageName, enabled)
    }

    /**
     * Gets all app packages that have been explicitly configured.
     */
    fun getConfiguredApps(): Set<String> {
        return storage.getStringSet(KEY_CONFIGURED_APPS, emptySet())
    }

    /**
     * Marks an app as configured (has posted notifications).
     */
    fun markAppConfigured(packageName: String) {
        val configured = getConfiguredApps().toMutableSet()
        if (configured.add(packageName)) {
            storage.putStringSet(KEY_CONFIGURED_APPS, configured)
        }
    }

    // Per-Channel Settings

    /**
     * Checks if forwarding is enabled for a specific notification channel.
     */
    fun isChannelEnabled(packageName: String, channelId: String): Boolean {
        val key = KEY_CHANNEL_ENABLED_PREFIX + packageName + CHANNEL_SEPARATOR + channelId
        return storage.getBoolean(key, true)
    }

    /**
     * Sets whether forwarding is enabled for a specific notification channel.
     */
    fun setChannelEnabled(packageName: String, channelId: String, enabled: Boolean) {
        val key = KEY_CHANNEL_ENABLED_PREFIX + packageName + CHANNEL_SEPARATOR + channelId
        storage.putBoolean(key, enabled)
    }

    /**
     * Gets all channel keys that have been explicitly configured for an app.
     */
    fun getConfiguredChannels(packageName: String): Set<String> {
        val key = KEY_CONFIGURED_CHANNELS_PREFIX + packageName
        return storage.getStringSet(key, emptySet())
    }

    /**
     * Marks a channel as configured.
     */
    fun markChannelConfigured(packageName: String, channelId: String) {
        val key = KEY_CONFIGURED_CHANNELS_PREFIX + packageName
        val configured = getConfiguredChannels(packageName).toMutableSet()
        if (configured.add(channelId)) {
            storage.putStringSet(key, configured)
        }
    }

    // Utility Methods

    /**
     * Clears all server credentials (username and password).
     */
    fun clearCredentials() {
        storage.remove(KEY_USERNAME)
        storage.remove(KEY_PASSWORD)
        _username.value = ""
        _password.value = ""
    }

    /**
     * Clears all app and channel settings.
     */
    fun clearAppSettings() {
        val configuredApps = getConfiguredApps()
        for (app in configuredApps) {
            storage.remove(KEY_APP_ENABLED_PREFIX + app)
            val channels = getConfiguredChannels(app)
            for (channel in channels) {
                val channelKey = KEY_CHANNEL_ENABLED_PREFIX + app + CHANNEL_SEPARATOR + channel
                storage.remove(channelKey)
            }
            storage.remove(KEY_CONFIGURED_CHANNELS_PREFIX + app)
        }
        storage.remove(KEY_CONFIGURED_APPS)
    }

    /**
     * Resets all settings to defaults.
     */
    fun resetToDefaults() {
        setServerUrl(DEFAULT_SERVER_URL)
        setTopic("")
        clearCredentials()
        setForwardingEnabled(DEFAULT_FORWARDING_ENABLED)
        setForwardOngoing(DEFAULT_FORWARD_ONGOING)
        setIncludeBody(DEFAULT_INCLUDE_BODY)
        clearAppSettings()
    }

    companion object {
        // Key constants
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_TOPIC = "topic"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_FORWARDING_ENABLED = "forwarding_enabled"
        private const val KEY_FORWARD_ONGOING = "forward_ongoing"
        private const val KEY_INCLUDE_BODY = "include_body"
        private const val KEY_BATTERY_OPTIMIZATION_PROMPT_SHOWN = "battery_optimization_prompt_shown"
        private const val KEY_CONFIGURED_APPS = "configured_apps"
        private const val KEY_APP_ENABLED_PREFIX = "app_enabled_"
        private const val KEY_CHANNEL_ENABLED_PREFIX = "channel_enabled_"
        private const val KEY_CONFIGURED_CHANNELS_PREFIX = "configured_channels_"
        private const val CHANNEL_SEPARATOR = "|||"

        // Default values
        const val DEFAULT_SERVER_URL = "https://ntfy.sh"
        const val DEFAULT_FORWARDING_ENABLED = true
        const val DEFAULT_FORWARD_ONGOING = false
        const val DEFAULT_INCLUDE_BODY = true

        /**
         * Gets the singleton instance of SettingsRepository.
         */
        fun getInstance(): SettingsRepository {
            return InstanceHolder.instance
        }

        private object InstanceHolder {
            val instance = SettingsRepository(SecureStorage.getInstance())
        }
    }
}
