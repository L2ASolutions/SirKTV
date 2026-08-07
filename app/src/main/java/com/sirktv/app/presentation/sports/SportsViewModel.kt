package com.sirktv.app.presentation.sports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.usecase.GetEpgNowNextUseCase
import com.sirktv.app.domain.usecase.ObserveSportsChannelsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SportsUiState(
    val categories: List<Category> = emptyList(),
    val allChannels: List<Channel> = emptyList(),
    val visibleChannels: List<Channel> = emptyList(),
    val selectedCategoryId: String? = null,
    val epgCache: Map<String, EpgNowNext> = emptyMap()
)

@HiltViewModel
class SportsViewModel @Inject constructor(
    private val observeSportsChannelsUseCase: ObserveSportsChannelsUseCase,
    private val getEpgNowNextUseCase: GetEpgNowNextUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SportsUiState())
    val uiState: StateFlow<SportsUiState> = _uiState.asStateFlow()

    private val requestedEpgChannelIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            observeSportsChannelsUseCase().collect { catalog ->
                _uiState.update {
                    it.copy(
                        categories = catalog.categories,
                        allChannels = catalog.channels,
                        visibleChannels = filterChannels(catalog.channels, it.selectedCategoryId)
                    )
                }
            }
        }
    }

    fun onCategorySelected(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId, visibleChannels = filterChannels(it.allChannels, categoryId)) }
    }

    fun requestEpgFor(channelId: String) {
        if (!requestedEpgChannelIds.add(channelId)) return
        viewModelScope.launch {
            val nowNext = getEpgNowNextUseCase(channelId)
            _uiState.update { it.copy(epgCache = it.epgCache + (channelId to nowNext)) }
        }
    }

    private fun filterChannels(channels: List<Channel>, categoryId: String?): List<Channel> =
        if (categoryId == null) channels else channels.filter { it.categoryId == categoryId }
}
