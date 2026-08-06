package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.LoginResult
import com.sirktv.app.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        serverUrl: String,
        username: String,
        password: String,
        rememberMe: Boolean
    ): LoginResult {
        if (serverUrl.isBlank()) {
            return LoginResult.InvalidServerUrl("Enter your server address")
        }
        if (username.isBlank() || password.isBlank()) {
            return LoginResult.InvalidCredentials
        }
        return authRepository.login(serverUrl.trim(), username.trim(), password, rememberMe)
    }
}
