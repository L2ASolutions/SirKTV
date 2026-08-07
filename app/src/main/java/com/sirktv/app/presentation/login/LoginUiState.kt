package com.sirktv.app.presentation.login

data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = true,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isReconnecting: Boolean = false,
    val errorMessage: String? = null
)

sealed interface LoginEvent {
    data class NavigateToLiveTv(val channelId: String) : LoginEvent
    data object NavigateToHome : LoginEvent
}
