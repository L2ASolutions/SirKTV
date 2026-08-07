package com.sirktv.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.SubtitleAppearance
import com.sirktv.app.domain.model.SubtitleBackground
import com.sirktv.app.domain.model.SubtitleEdgeStyle
import com.sirktv.app.domain.model.SubtitleTextColor
import com.sirktv.app.domain.model.SubtitleTextSize
import com.sirktv.app.domain.usecase.GetSubtitleAppearanceUseCase
import com.sirktv.app.domain.usecase.UpdateSubtitleAppearanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubtitleAppearanceViewModel @Inject constructor(
    private val getSubtitleAppearanceUseCase: GetSubtitleAppearanceUseCase,
    private val updateSubtitleAppearanceUseCase: UpdateSubtitleAppearanceUseCase
) : ViewModel() {

    private val _appearance = MutableStateFlow(SubtitleAppearance())
    val appearance: StateFlow<SubtitleAppearance> = _appearance.asStateFlow()

    init {
        viewModelScope.launch { getSubtitleAppearanceUseCase().collect { _appearance.value = it } }
    }

    fun setTextSize(size: SubtitleTextSize) = update { it.copy(textSize = size) }
    fun setTextColor(color: SubtitleTextColor) = update { it.copy(textColor = color) }
    fun setBackground(background: SubtitleBackground) = update { it.copy(background = background) }
    fun setEdgeStyle(edgeStyle: SubtitleEdgeStyle) = update { it.copy(edgeStyle = edgeStyle) }

    private fun update(transform: (SubtitleAppearance) -> SubtitleAppearance) {
        val updated = transform(_appearance.value)
        _appearance.value = updated
        // Applies live to whichever PlayerView is currently attached — same
        // collector path as init, since updateSubtitleAppearanceUseCase writes
        // through the DataStore that GetSubtitleAppearanceUseCase observes.
        viewModelScope.launch { updateSubtitleAppearanceUseCase(updated) }
    }
}
