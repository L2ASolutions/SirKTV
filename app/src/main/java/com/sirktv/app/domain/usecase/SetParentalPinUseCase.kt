package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.repository.ParentalSettingsRepository
import javax.inject.Inject

class SetParentalPinUseCase @Inject constructor(
    private val parentalSettingsRepository: ParentalSettingsRepository
) {
    suspend operator fun invoke(pin: String?) = parentalSettingsRepository.setPin(pin)
}
