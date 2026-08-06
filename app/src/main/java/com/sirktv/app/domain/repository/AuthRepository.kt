package com.sirktv.app.domain.repository

import com.sirktv.app.domain.model.LoginResult
import com.sirktv.app.domain.model.SavedCredentials

interface AuthRepository {
    suspend fun login(
        serverUrl: String,
        username: String,
        password: String,
        rememberMe: Boolean
    ): LoginResult

    suspend fun getSavedCredentials(): SavedCredentials?

    suspend fun clearSavedCredentials()
}
