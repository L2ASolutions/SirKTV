package com.sirktv.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.domain.usecase.ClearSavedCredentialsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsEvent {
    data object NavigateToLogin : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val clearSavedCredentialsUseCase: ClearSavedCredentialsUseCase,
    private val currentSession: CurrentSession
) : ViewModel() {

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    fun onLogoutClicked() {
        viewModelScope.launch {
            clearSavedCredentialsUseCase()
            currentSession.clear()
            _events.emit(SettingsEvent.NavigateToLogin)
        }
    }
}
