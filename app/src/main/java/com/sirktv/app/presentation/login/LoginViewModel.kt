package com.sirktv.app.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.LoginResult
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.domain.usecase.GetSavedCredentialsUseCase
import com.sirktv.app.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val getSavedCredentialsUseCase: GetSavedCredentialsUseCase,
    private val currentSession: CurrentSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val saved = getSavedCredentialsUseCase()
            if (saved != null) {
                _uiState.update {
                    it.copy(serverUrl = saved.serverUrl, username = saved.username, password = saved.password)
                }
                attemptLogin(reconnecting = true)
            }
        }
    }

    fun onServerUrlChanged(value: String) {
        _uiState.update { it.copy(serverUrl = value, errorMessage = null) }
    }

    fun onUsernameChanged(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onToggleRememberMe() {
        _uiState.update { it.copy(rememberMe = !it.rememberMe) }
    }

    fun onSignInClicked() {
        attemptLogin(reconnecting = false)
    }

    private fun attemptLogin(reconnecting: Boolean) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isReconnecting = reconnecting) }
            when (val result = loginUseCase(state.serverUrl, state.username, state.password, state.rememberMe)) {
                is LoginResult.Success -> {
                    currentSession.set(result.profile)
                    _uiState.update { it.copy(isLoading = false, isReconnecting = false) }
                    _events.emit(LoginEvent.NavigateToLiveTv)
                }
                is LoginResult.InvalidCredentials -> setError("Incorrect username or password.")
                is LoginResult.SubscriptionInactive -> setError(result.message)
                is LoginResult.InvalidServerUrl -> setError(result.reason)
                is LoginResult.ServerError -> setError(result.message)
                is LoginResult.NetworkError -> setError("Can't reach the server. Check your connection and try again.")
            }
        }
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(isLoading = false, isReconnecting = false, errorMessage = message) }
    }
}
