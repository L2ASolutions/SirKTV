package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.PlayerSettings
import com.sirktv.app.domain.repository.PlayerSettingsRepository
import javax.inject.Inject

class UpdatePlayerSettingsUseCase @Inject constructor(
    private val playerSettingsRepository: PlayerSettingsRepository
) {
    suspend operator fun invoke(settings: PlayerSettings) = playerSettingsRepository.update(settings)
}
