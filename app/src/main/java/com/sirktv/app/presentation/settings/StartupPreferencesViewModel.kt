package com.sirktv.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.StartupPreference
import com.sirktv.app.domain.usecase.GetStartupPreferenceUseCase
import com.sirktv.app.domain.usecase.ObserveChannelsUseCase
import com.sirktv.app.domain.usecase.UpdateStartupPreferenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartupPreferencesViewModel @Inject constructor(
    private val getStartupPreferenceUseCase: GetStartupPreferenceUseCase,
    private val updateStartupPreferenceUseCase: UpdateStartupPreferenceUseCase,
    observeChannelsUseCase: ObserveChannelsUseCase
) : ViewModel() {

    private val _preference = MutableStateFlow(StartupPreference())
    val preference: StateFlow<StartupPreference> = _preference.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    init {
        viewModelScope.launch { getStartupPreferenceUseCase().collect { _preference.value = it } }
        viewModelScope.launch { observeChannelsUseCase().collect { _channels.value = it } }
    }

    fun setAutoStart(enabled: Boolean) = updatePreference { it.copy(autoStartLiveTv = enabled) }
    fun setResumeLastChannel(enabled: Boolean) = updatePreference { it.copy(resumeLastChannel = enabled) }
    fun setStartupChannel(channelId: String) = updatePreference { it.copy(startupChannelId = channelId) }

    private fun updatePreference(transform: (StartupPreference) -> StartupPreference) {
        val updated = transform(_preference.value)
        _preference.value = updated
        viewModelScope.launch { updateStartupPreferenceUseCase(updated) }
    }
}
