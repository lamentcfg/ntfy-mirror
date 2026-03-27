package com.lamentcfg.ntfymirror.ui

/**
 * Data class representing a notification channel with its enabled state and metadata.
 */
data class ChannelInfo(
    val channelId: String,
    val channelName: String,
    val packageName: String,
    val isEnabled: Boolean,
    val hasOngoingNotification: Boolean = false,
    val notificationCount: Int = 0
)
