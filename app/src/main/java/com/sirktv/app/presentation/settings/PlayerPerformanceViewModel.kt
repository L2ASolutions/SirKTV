package com.sirktv.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.BufferProfile
import com.sirktv.app.domain.model.PlayerSettings
import com.sirktv.app.domain.model.StreamQuality
import com.sirktv.app.domain.usecase.GetPlayerSettingsUseCase
import com.sirktv.app.domain.usecase.UpdatePlayerSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerPerformanceViewModel @Inject constructor(
    private val getPlayerSettingsUseCase: GetPlayerSettingsUseCase,
    private val updatePlayerSettingsUseCase: UpdatePlayerSettingsUseCase
) : ViewModel() {

    private val _settings = MutableStateFlow(PlayerSettings())
    val settings: StateFlow<PlayerSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch { getPlayerSettingsUseCase().collect { _settings.value = it } }
    }

    fun setBufferProfile(profile: BufferProfile) = update { it.copy(bufferProfile = profile) }
    fun setQuality(quality: StreamQuality) = update { it.copy(preferredQuality = quality) }
    fun setHardwareDecoding(enabled: Boolean) = update { it.copy(hardwareDecodingEnabled = enabled) }
    fun setReconnectAttempts(attempts: Int) = update { it.copy(reconnectAttempts = attempts) }
    fun setShowBufferHealth(enabled: Boolean) = update { it.copy(showBufferHealth = enabled) }
    fun setUserAgent(userAgent: String) = update { it.copy(userAgent = userAgent) }

    private fun update(transform: (PlayerSettings) -> PlayerSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        viewModelScope.launch { updatePlayerSettingsUseCase(updated) }
    }
}
