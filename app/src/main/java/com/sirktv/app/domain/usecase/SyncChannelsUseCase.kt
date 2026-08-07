package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.repository.ChannelRepository
import javax.inject.Inject

class SyncChannelsUseCase @Inject constructor(
    private val channelRepository: ChannelRepository
) {
    suspend operator fun invoke() = channelRepository.sync()
}
