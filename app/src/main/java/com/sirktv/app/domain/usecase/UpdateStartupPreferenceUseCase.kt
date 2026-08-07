package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.StartupPreference
import com.sirktv.app.domain.repository.StartupPreferenceRepository
import javax.inject.Inject

class UpdateStartupPreferenceUseCase @Inject constructor(
    private val startupPreferenceRepository: StartupPreferenceRepository
) {
    suspend operator fun invoke(preference: StartupPreference) = startupPreferenceRepository.update(preference)
}
