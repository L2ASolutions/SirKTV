package com.sirktv.app.presentation.livetv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.UserProfile
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.domain.usecase.ClearSavedCredentialsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveTvViewModel @Inject constructor(
    private val clearSavedCredentialsUseCase: ClearSavedCredentialsUseCase,
    private val currentSession: CurrentSession
) : ViewModel() {

    val profile: StateFlow<UserProfile?> = currentSession.profile

    private val _events = MutableSharedFlow<LiveTvEvent>()
    val events: SharedFlow<LiveTvEvent> = _events.asSharedFlow()

    fun onLogoutClicked() {
        viewModelScope.launch {
            clearSavedCredentialsUseCase()
            currentSession.clear()
            _events.emit(LiveTvEvent.NavigateToLogin)
        }
    }
}

sealed interface LiveTvEvent {
    data object NavigateToLogin : LiveTvEvent
}
