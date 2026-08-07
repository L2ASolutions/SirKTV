package com.sirktv.app.presentation.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.UserProfile
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.domain.usecase.ClearSavedCredentialsUseCase
import com.sirktv.app.domain.usecase.PickDefaultChannelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Temporary landing screen shown when auto-start is off. Stands in for the
 * full Home screen (rows, Continue Watching, Movies/Series) that ships in
 * Phase 3 — just enough navigation to reach what Phase 2 actually built.
 */
@HiltViewModel
class HubViewModel @Inject constructor(
    private val clearSavedCredentialsUseCase: ClearSavedCredentialsUseCase,
    private val pickDefaultChannelUseCase: PickDefaultChannelUseCase,
    private val currentSession: CurrentSession
) : ViewModel() {

    val profile: StateFlow<UserProfile?> = currentSession.profile

    private val _events = MutableSharedFlow<HubEvent>()
    val events: SharedFlow<HubEvent> = _events.asSharedFlow()

    private var isResolvingChannel = false

    fun onLiveTvClicked() {
        if (isResolvingChannel) return
        isResolvingChannel = true
        viewModelScope.launch {
            val channelId = pickDefaultChannelUseCase()
            isResolvingChannel = false
            if (channelId != null) {
                _events.emit(HubEvent.NavigateToLiveTv(channelId))
            } else {
                _events.emit(HubEvent.NoChannelsAvailable)
            }
        }
    }

    fun onSportsClicked() {
        viewModelScope.launch { _events.emit(HubEvent.NavigateToSports) }
    }

    fun onSettingsClicked() {
        viewModelScope.launch { _events.emit(HubEvent.NavigateToSettings) }
    }

    fun onLogoutClicked() {
        viewModelScope.launch {
            clearSavedCredentialsUseCase()
            currentSession.clear()
            _events.emit(HubEvent.NavigateToLogin)
        }
    }
}

sealed interface HubEvent {
    data class NavigateToLiveTv(val channelId: String) : HubEvent
    data object NavigateToSports : HubEvent
    data object NavigateToSettings : HubEvent
    data object NavigateToLogin : HubEvent
    data object NoChannelsAvailable : HubEvent
}
