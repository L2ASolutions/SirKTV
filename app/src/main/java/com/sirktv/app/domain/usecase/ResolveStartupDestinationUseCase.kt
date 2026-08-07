package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.StartupDestination
import com.sirktv.app.domain.repository.ChannelRepository
import com.sirktv.app.domain.repository.StartupPreferenceRepository
import javax.inject.Inject

/**
 * Decides where the app lands after authentication: the last-watched or
 * configured startup channel (auto-start on), or Home (auto-start off, or
 * the account has no live channels at all). Cached channels are used when
 * available so a returning session resolves instantly; a completely empty
 * cache (first-ever login) forces one blocking sync.
 */
class ResolveStartupDestinationUseCase @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val startupPreferenceRepository: StartupPreferenceRepository
) {
    suspend operator fun invoke(): StartupDestination {
        val prefs = startupPreferenceRepository.get()
        if (!prefs.autoStartLiveTv) return StartupDestination.Home

        var channels = channelRepository.getCachedChannels()
        if (channels.isEmpty()) {
            channelRepository.sync()
            channels = channelRepository.getCachedChannels()
        }
        if (channels.isEmpty()) return StartupDestination.Home

        val channelIds = channels.map { it.id }.toSet()
        val resumeId = prefs.lastWatchedChannelId?.takeIf { prefs.resumeLastChannel && it in channelIds }
        val startupId = prefs.startupChannelId?.takeIf { it in channelIds }
        val favoriteId = channelRepository.getFavoriteChannelIds().firstOrNull { it in channelIds }
        val targetId = resumeId ?: startupId ?: favoriteId ?: channels.first().id

        return StartupDestination.LiveTv(targetId)
    }
}
