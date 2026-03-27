package com.lamentcfg.ntfymirror

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.lamentcfg.ntfymirror.data.ChannelDiscoveryManager
import com.lamentcfg.ntfymirror.data.SettingsRepository
import com.lamentcfg.ntfymirror.network.NtfyClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.lamentcfg.ntfymirror.R
import java.util.Collections

/**
 * Notification listener service that intercepts notifications and forwards them to ntfy server.
 * Requires BIND_NOTIFICATION_LISTENER_SERVICE permission and user authorization.
 */
class NtfyNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var ntfyClient: NtfyClient
    private lateinit var channelDiscoveryManager: ChannelDiscoveryManager

    companion object {
        private const val TAG = "NtfyNotificationListener"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
        private const val DEDUP_WINDOW_MS = 60_000L // 60 seconds, covers network reconnection replays
    }

    // Deduplication: track recently seen notifications by content to filter duplicates
    // Catches doze mode replays and network reconnection replays
    private val recentNotifications = Collections.synchronizedMap(
        linkedMapOf<String, Long>()
    )

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository.getInstance()
        ntfyClient = NtfyClient.getInstance()
        channelDiscoveryManager = ChannelDiscoveryManager.getInstance()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")

        // Re-initialize channel discovery with service context for channel name access
        channelDiscoveryManager.initialize(this)

        // Initial channel discovery from existing notifications
        updateChannelDiscovery()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Skip notifications from our own app
        if (sbn.packageName == packageName) {
            return
        }

        // Skip group summary notifications (e.g., "New Messages" from email apps)
        // These are generic summaries while individual notifications have the real content
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            Log.d(TAG, "Skipping group summary notification: package=${sbn.packageName}")
            return
        }

        // Extract notification data
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val notificationPackageName = sbn.packageName
        val channelId = sbn.notification.channelId ?: ""
        val isOngoing = sbn.isOngoing

        // Skip duplicate notifications by content (doze replays, network reconnection replays)
        if (isDuplicate(notificationPackageName, title, text)) {
            Log.d(TAG, "Skipping duplicate notification: package=$notificationPackageName, title=$title")
            return
        }

        Log.d(
            TAG, "Notification posted: package=$notificationPackageName, " +
                    "title=$title, channel=$channelId, ongoing=$isOngoing"
        )

        // Check if forwarding should proceed
        if (!shouldForwardNotification(notificationPackageName, channelId, isOngoing)) {
            Log.d(TAG, "Skipping notification - disabled by settings")
            return
        }

        // Mark this app/channel as seen for settings management
        settingsRepository.markAppConfigured(notificationPackageName)
        if (channelId.isNotEmpty()) {
            settingsRepository.markChannelConfigured(notificationPackageName, channelId)
        }

        // Get app name for better display
        val appName = getAppName(notificationPackageName)

        // Build the notification message
        val messageTitle = if (title.isNotEmpty()) title else appName
        val messageBody = if (settingsRepository.shouldIncludeBody()) text else getString(R.string.content_hidden)

        // Forward to ntfy server
        forwardNotification(messageTitle, messageBody, notificationPackageName, channelId)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d(TAG, "Notification removed: package=${sbn.packageName}")
        // Update channel discovery when notifications are removed
        updateChannelDiscovery()
    }

    /**
     * Updates channel discovery from active notifications.
     */
    private fun updateChannelDiscovery() {
        try {
            channelDiscoveryManager.updateFromActiveNotifications(activeNotifications)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update channel discovery: ${e.message}")
        }
    }

    /**
     * Determines if a notification should be forwarded based on all applicable settings.
     */
    private fun shouldForwardNotification(packageName: String, channelId: String, isOngoing: Boolean): Boolean {
        // Check if forwarding is globally enabled
        if (!settingsRepository.isForwardingEnabled()) {
            Log.d(TAG, "Forwarding disabled globally")
            return false
        }

        // Check if server is configured
        if (!settingsRepository.isConfigured()) {
            Log.d(TAG, "Server not configured")
            return false
        }

        // Check if we should skip ongoing notifications
        if (isOngoing && !settingsRepository.shouldForwardOngoing()) {
            Log.d(TAG, "Skipping ongoing notification")
            return false
        }

        // Check if this app is enabled for forwarding
        if (!settingsRepository.isAppEnabled(packageName)) {
            Log.d(TAG, "App disabled: $packageName")
            return false
        }

        // Check if this channel is enabled for forwarding (if channel ID exists)
        if (channelId.isNotEmpty() && !settingsRepository.isChannelEnabled(packageName, channelId)) {
            Log.d(TAG, "Channel disabled: $packageName/$channelId")
            return false
        }

        return true
    }

    /**
     * Forwards a notification to the ntfy server with retry logic.
     */
    private fun forwardNotification(
        title: String,
        body: String,
        packageName: String,
        channelId: String
    ) {
        serviceScope.launch {
            val serverUrl = settingsRepository.getServerUrl()
            val topic = settingsRepository.getTopic()
            val username = settingsRepository.getUsername()
            val password = settingsRepository.getPassword()

            var lastError: Throwable? = null

            repeat(MAX_RETRY_ATTEMPTS) { attempt ->
                if (attempt > 0) {
                    Log.d(TAG, "Retry attempt $attempt for notification: $title")
                    delay(RETRY_DELAY_MS * attempt)
                }

                val result = ntfyClient.publish(
                    serverUrl = serverUrl,
                    topic = topic,
                    title = title,
                    message = body,
                    username = username,
                    password = password
                )

                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully forwarded notification: $title")
                        return@launch
                    },
                    onFailure = { error ->
                        lastError = error
                        Log.w(TAG, "Failed to forward notification (attempt ${attempt + 1}): ${error.message}")
                    }
                )
            }

            // All retries failed
            Log.e(TAG, "Failed to forward notification after $MAX_RETRY_ATTEMPTS attempts: ${lastError?.message}")
        }
    }

    /**
     * Checks if a notification is a duplicate within the deduplication window.
     * Uses content-based deduplication (package + title + body) to prevent
     * double-posting from doze mode replays and network reconnection replays.
     */
    private fun isDuplicate(packageName: String, title: String, text: String): Boolean {
        val key = "$packageName|$title|$text"
        val now = System.currentTimeMillis()

        synchronized(recentNotifications) {
            val lastSeen = recentNotifications[key]
            if (lastSeen != null && (now - lastSeen) < DEDUP_WINDOW_MS) {
                return true
            }
            recentNotifications[key] = now

            // Clean up old entries to prevent memory leak
            val iterator = recentNotifications.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (now - entry.value > DEDUP_WINDOW_MS) {
                    iterator.remove()
                }
            }
        }
        return false
    }

    /**
     * Gets the human-readable app name from a package name.
     */
    private fun getAppName(packageName: String): String {
        return try {
            val packageManager = packageManager
            val applicationInfo: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
