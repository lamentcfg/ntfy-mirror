package com.lamentcfg.ntfymirror.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages discovery of notification channels from active notifications.
 * Provides channel metadata including names (where available) and notification states.
 */
class ChannelDiscoveryManager private constructor() {

    private var notificationManager: NotificationManager? = null

    // Cache of discovered channels per package: packageName -> (channelId -> ChannelMetadata)
    private val _discoveredChannels = MutableStateFlow<Map<String, Map<String, ChannelMetadata>>>(emptyMap())
    val discoveredChannels: StateFlow<Map<String, Map<String, ChannelMetadata>>> = _discoveredChannels.asStateFlow()

    data class ChannelMetadata(
        val channelId: String,
        val channelName: String? = null,
        val hasOngoingNotification: Boolean = false,
        val notificationCount: Int = 0,
        val lastSeen: Long = System.currentTimeMillis()
    )

    fun initialize(context: Context) {
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * Updates channel discovery from active notifications.
     * Should be called from NotificationListenerService when notifications are posted/removed.
     */
    fun updateFromActiveNotifications(activeNotifications: Array<StatusBarNotification>) {
        val channelMap = mutableMapOf<String, MutableMap<String, ChannelMetadata>>()

        for (sbn in activeNotifications) {
            val packageName = sbn.packageName
            val channelId = sbn.notification.channelId ?: continue
            val isOngoing = sbn.isOngoing

            // Get or create package map
            val packageChannels = channelMap.getOrPut(packageName) { mutableMapOf() }

            // Get existing metadata or create new
            val existing = packageChannels[channelId]
            if (existing != null) {
                packageChannels[channelId] = existing.copy(
                    hasOngoingNotification = existing.hasOngoingNotification || isOngoing,
                    notificationCount = existing.notificationCount + 1,
                    lastSeen = System.currentTimeMillis()
                )
            } else {
                // Try to get channel name (API 33+)
                val channelName = getChannelName(packageName, channelId)

                packageChannels[channelId] = ChannelMetadata(
                    channelId = channelId,
                    channelName = channelName,
                    hasOngoingNotification = isOngoing,
                    notificationCount = 1,
                    lastSeen = System.currentTimeMillis()
                )
            }
        }

        // Merge with existing discovered channels (preserve channel names even if notification is gone)
        val currentMap = _discoveredChannels.value.toMutableMap()
        for ((packageName, channels) in channelMap) {
            val existingChannels = currentMap[packageName]?.toMutableMap() ?: mutableMapOf()
            for ((channelId, metadata) in channels) {
                // Preserve channel name if we had it and new data doesn't
                val existing = existingChannels[channelId]
                if (existing != null && metadata.channelName == null && existing.channelName != null) {
                    existingChannels[channelId] = metadata.copy(channelName = existing.channelName)
                } else {
                    existingChannels[channelId] = metadata
                }
            }
            currentMap[packageName] = existingChannels
        }

        _discoveredChannels.value = currentMap
    }

    /**
     * Gets channel name using system API (Android 13+ only).
     * Returns null on older versions or if channel not found.
     */
    private fun getChannelName(packageName: String, channelId: String): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val channel = notificationManager?.getNotificationChannel(packageName, channelId)
                return channel?.name?.toString()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get channel name for $packageName/$channelId: ${e.message}")
            }
        }
        return null
    }

    /**
     * Formats a channel ID into a human-readable name.
     * E.g., "phone_key_service_channel" -> "Phone Key Service Channel"
     */
    fun formatChannelIdAsName(channelId: String): String {
        return channelId
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    /**
     * Gets discovered channels for a specific package.
     */
    fun getChannelsForPackage(packageName: String): Map<String, ChannelMetadata> {
        return _discoveredChannels.value[packageName] ?: emptyMap()
    }

    /**
     * Clears all discovered channel data.
     */
    fun clear() {
        _discoveredChannels.value = emptyMap()
    }

    companion object {
        private const val TAG = "ChannelDiscovery"

        fun getInstance(): ChannelDiscoveryManager {
            return InstanceHolder.instance
        }

        private object InstanceHolder {
            val instance = ChannelDiscoveryManager()
        }
    }
}
