package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.AppearanceSettings
import com.sirktv.app.domain.repository.AppearanceSettingsRepository
import javax.inject.Inject

class UpdateAppearanceSettingsUseCase @Inject constructor(
    private val appearanceSettingsRepository: AppearanceSettingsRepository
) {
    suspend operator fun invoke(settings: AppearanceSettings) = appearanceSettingsRepository.update(settings)
}
