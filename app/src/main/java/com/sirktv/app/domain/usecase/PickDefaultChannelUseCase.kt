package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.repository.ChannelRepository
import javax.inject.Inject

/**
 * Picks a channel to tune when the user explicitly asks for "Live TV" without
 * naming one (e.g. Home's Live TV quick-launch) — first favorite, else the first channel.
 * Unlike [ResolveStartupDestinationUseCase] this ignores the auto-start
 * preference entirely, since it only runs on a direct user action.
 */
class PickDefaultChannelUseCase @Inject constructor(
    private val channelRepository: ChannelRepository
) {
    suspend operator fun invoke(): String? {
        var channels = channelRepository.getCachedChannels()
        if (channels.isEmpty()) {
            channelRepository.sync()
            channels = channelRepository.getCachedChannels()
        }
        if (channels.isEmpty()) return null

        val channelIds = channels.map { it.id }.toSet()
        val favoriteId = channelRepository.getFavoriteChannelIds().firstOrNull { it in channelIds }
        return favoriteId ?: channels.first().id
    }
}
