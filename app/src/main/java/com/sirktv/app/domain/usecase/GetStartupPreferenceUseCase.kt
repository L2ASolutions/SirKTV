package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.StartupPreference
import com.sirktv.app.domain.repository.StartupPreferenceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStartupPreferenceUseCase @Inject constructor(
    private val startupPreferenceRepository: StartupPreferenceRepository
) {
    operator fun invoke(): Flow<StartupPreference> = startupPreferenceRepository.observe()
}
