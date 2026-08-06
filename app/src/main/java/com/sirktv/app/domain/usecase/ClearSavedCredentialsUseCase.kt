package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.repository.AuthRepository
import javax.inject.Inject

class ClearSavedCredentialsUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() = authRepository.clearSavedCredentials()
}
