package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.ParentalSettings
import com.sirktv.app.domain.repository.ParentalSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetParentalSettingsUseCase @Inject constructor(
    private val parentalSettingsRepository: ParentalSettingsRepository
) {
    operator fun invoke(): Flow<ParentalSettings> = parentalSettingsRepository.observe()
}
