package com.sirktv.app.domain.model

data class StartupPreference(
    val autoStartLiveTv: Boolean = false,
    val startupChannelId: String? = null,
    val resumeLastChannel: Boolean = false,
    val lastWatchedChannelId: String? = null
)

sealed interface StartupDestination {
    data object Home : StartupDestination
    data class LiveTv(val channelId: String) : StartupDestination
}
