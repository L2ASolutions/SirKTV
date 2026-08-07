package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.repository.ParentalSettingsRepository
import javax.inject.Inject

class VerifyParentalPinUseCase @Inject constructor(
    private val parentalSettingsRepository: ParentalSettingsRepository
) {
    suspend operator fun invoke(pin: String): Boolean = parentalSettingsRepository.verifyPin(pin)
}
