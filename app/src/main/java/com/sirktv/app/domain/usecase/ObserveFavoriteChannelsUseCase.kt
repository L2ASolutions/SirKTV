package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFavoriteChannelsUseCase @Inject constructor(
    private val channelRepository: ChannelRepository
) {
    operator fun invoke(): Flow<List<Channel>> = channelRepository.observeFavoriteChannels()
}
