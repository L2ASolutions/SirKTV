package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.SavedCredentials
import com.sirktv.app.domain.repository.AuthRepository
import javax.inject.Inject

class GetSavedCredentialsUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): SavedCredentials? = authRepository.getSavedCredentials()
}
