package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.PlayerSettings
import com.sirktv.app.domain.repository.PlayerSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlayerSettingsUseCase @Inject constructor(
    private val playerSettingsRepository: PlayerSettingsRepository
) {
    operator fun invoke(): Flow<PlayerSettings> = playerSettingsRepository.observe()
}
