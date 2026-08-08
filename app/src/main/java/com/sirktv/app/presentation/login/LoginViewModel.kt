package com.sirktv.app.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.LoginResult
import com.sirktv.app.domain.model.SavedCredentials
import com.sirktv.app.domain.model.StartupDestination
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.domain.usecase.GetDisplayNameUseCase
import com.sirktv.app.domain.usecase.GetSavedCredentialsUseCase
import com.sirktv.app.domain.usecase.LoginUseCase
import com.sirktv.app.domain.usecase.ResolveStartupDestinationUseCase
import com.sirktv.app.domain.usecase.UpdateDisplayNameUseCase
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
    private val resolveStartupDestinationUseCase: ResolveStartupDestinationUseCase,
    private val getDisplayNameUseCase: GetDisplayNameUseCase,
    private val updateDisplayNameUseCase: UpdateDisplayNameUseCase,
    private val currentSession: CurrentSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val savedDisplayName = getDisplayNameUseCase().orEmpty()
            _uiState.update { it.copy(displayName = savedDisplayName) }
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

    fun onDisplayNameChanged(value: String) {
        _uiState.update { it.copy(displayName = value) }
    }

    fun onToggleRememberMe() {
        _uiState.update { it.copy(rememberMe = !it.rememberMe) }
    }

    fun onToggleDisclaimer() {
        _uiState.update { it.copy(isDisclaimerExpanded = !it.isDisclaimerExpanded) }
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
                    currentSession.set(
                        result.profile,
                        SavedCredentials(state.serverUrl, state.username, state.password)
                    )
                    updateDisplayNameUseCase(state.displayName)
                    _uiState.update { it.copy(isLoading = false, isReconnecting = false) }
                    when (val destination = resolveStartupDestinationUseCase()) {
                        is StartupDestination.LiveTv -> _events.emit(LoginEvent.NavigateToLiveTv(destination.channelId))
                        StartupDestination.Home -> _events.emit(LoginEvent.NavigateToHome)
                    }
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
