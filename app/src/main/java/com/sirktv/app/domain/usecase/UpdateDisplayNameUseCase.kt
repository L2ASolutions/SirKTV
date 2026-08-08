package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.repository.DisplayNameRepository
import javax.inject.Inject

class UpdateDisplayNameUseCase @Inject constructor(
    private val displayNameRepository: DisplayNameRepository
) {
    suspend operator fun invoke(displayName: String?) = displayNameRepository.set(displayName)
}
