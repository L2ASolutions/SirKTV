package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.repository.ParentalSettingsRepository
import javax.inject.Inject

class ToggleCategoryLockUseCase @Inject constructor(
    private val parentalSettingsRepository: ParentalSettingsRepository
) {
    suspend operator fun invoke(categoryId: String, locked: Boolean) =
        parentalSettingsRepository.setCategoryLocked(categoryId, locked)
}
