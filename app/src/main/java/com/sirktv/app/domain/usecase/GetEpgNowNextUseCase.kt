package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.repository.ChannelRepository
import javax.inject.Inject

class GetEpgNowNextUseCase @Inject constructor(
    private val channelRepository: ChannelRepository
) {
    suspend operator fun invoke(channelId: String): EpgNowNext = channelRepository.getEpgNowNext(channelId)
}
