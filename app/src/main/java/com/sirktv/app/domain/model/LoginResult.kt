package com.sirktv.app.domain.model

sealed interface LoginResult {
    data class Success(val profile: UserProfile) : LoginResult
    data object InvalidCredentials : LoginResult
    data class SubscriptionInactive(val status: SubscriptionStatus, val message: String) : LoginResult
    data class InvalidServerUrl(val reason: String) : LoginResult
    data class ServerError(val message: String) : LoginResult
    data class NetworkError(val message: String) : LoginResult
}
