package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.EpgProgram
import com.sirktv.app.domain.repository.ChannelRepository
import javax.inject.Inject

class GetEpgListingsUseCase @Inject constructor(
    private val channelRepository: ChannelRepository
) {
    suspend operator fun invoke(channelId: String, limit: Int = 20): List<EpgProgram> =
        channelRepository.getEpgListings(channelId, limit)
}
