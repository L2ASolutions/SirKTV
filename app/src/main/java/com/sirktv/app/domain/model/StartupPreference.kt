package com.sirktv.app.domain.model

data class StartupPreference(
    val autoStartLiveTv: Boolean = true,
    val startupChannelId: String? = null,
    val resumeLastChannel: Boolean = false,
    val lastWatchedChannelId: String? = null
)

sealed interface StartupDestination {
    data object Hub : StartupDestination
    data class LiveTv(val channelId: String) : StartupDestination
}
