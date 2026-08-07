package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.AppearanceSettings
import com.sirktv.app.domain.repository.AppearanceSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppearanceSettingsUseCase @Inject constructor(
    private val appearanceSettingsRepository: AppearanceSettingsRepository
) {
    operator fun invoke(): Flow<AppearanceSettings> = appearanceSettingsRepository.observe()
}
