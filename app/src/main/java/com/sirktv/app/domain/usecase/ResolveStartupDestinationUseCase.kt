package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.StartupDestination
import com.sirktv.app.domain.repository.ChannelRepository
import com.sirktv.app.domain.repository.StartupPreferenceRepository
import javax.inject.Inject

/**
 * Decides where the app lands after authentication: the last-watched or
 * configured startup channel (auto-start on), or Home (auto-start off, or
 * nothing cached yet). Reads only already-cached Room data — never triggers
 * a network sync — so this always resolves in well under a second. A
 * completely empty cache (first-ever login, or auto-start on but Live TV was
 * never opened last session) simply falls back to Home; Live TV syncs itself
 * on demand exactly like every other section the moment it's opened.
 */
class ResolveStartupDestinationUseCase @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val startupPreferenceRepository: StartupPreferenceRepository
) {
    suspend operator fun invoke(): StartupDestination {
        val prefs = startupPreferenceRepository.get()
        if (!prefs.autoStartLiveTv) return StartupDestination.Home

        val channels = channelRepository.getCachedChannels()
        if (channels.isEmpty()) return StartupDestination.Home

        val channelIds = channels.map { it.id }.toSet()
        val resumeId = prefs.lastWatchedChannelId?.takeIf { prefs.resumeLastChannel && it in channelIds }
        val startupId = prefs.startupChannelId?.takeIf { it in channelIds }
        val favoriteId = channelRepository.getFavoriteChannelIds().firstOrNull { it in channelIds }
        val targetId = resumeId ?: startupId ?: favoriteId ?: channels.first().id

        return StartupDestination.LiveTv(targetId)
    }
}
