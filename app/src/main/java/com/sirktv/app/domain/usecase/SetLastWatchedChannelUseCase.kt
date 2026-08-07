package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.repository.StartupPreferenceRepository
import javax.inject.Inject

class SetLastWatchedChannelUseCase @Inject constructor(
    private val startupPreferenceRepository: StartupPreferenceRepository
) {
    suspend operator fun invoke(channelId: String) = startupPreferenceRepository.setLastWatchedChannel(channelId)
}
