package com.lamentcfg.ntfymirror.ui

import android.graphics.drawable.Drawable

/**
 * Data class representing an installed app with notification info.
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isEnabled: Boolean,
    val channels: List<ChannelInfo> = emptyList(),
    val isExpanded: Boolean = false
)
