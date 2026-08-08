package com.sirktv.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.usecase.GetParentalSettingsUseCase
import com.sirktv.app.domain.usecase.SetParentalPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentalControlsViewModel @Inject constructor(
    getParentalSettingsUseCase: GetParentalSettingsUseCase,
    private val setParentalPinUseCase: SetParentalPinUseCase
) : ViewModel() {

    val isPinLockEnabled: StateFlow<Boolean> = getParentalSettingsUseCase()
        .map { it.isEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun disablePinLock() {
        viewModelScope.launch { setParentalPinUseCase(null) }
    }

    fun enablePinLock(pin: String) {
        viewModelScope.launch { setParentalPinUseCase(pin) }
    }
}
