package com.sirktv.app.presentation.livetv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.model.EpgProgram
import com.sirktv.app.domain.usecase.GetEpgListingsUseCase
import com.sirktv.app.domain.usecase.GetEpgNowNextUseCase
import com.sirktv.app.domain.usecase.ObserveCategoriesUseCase
import com.sirktv.app.domain.usecase.ObserveChannelsUseCase
import com.sirktv.app.domain.usecase.SyncChannelsUseCase
import com.sirktv.app.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveTvBrowseUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val countryGroups: List<CountryGroup> = emptyList(),
    val allChannels: List<Channel> = emptyList(),
    val visibleChannels: List<Channel> = emptyList(),
    val selectedCountryCode: String? = null,
    val selectedCategoryId: String? = null,
    val selectedChannelId: String? = null,
    val epgCache: Map<String, EpgNowNext> = emptyMap(),
    val epgListingsCache: Map<String, List<EpgProgram>> = emptyMap(),
    val loadError: String? = null
) {
    val selectedChannel: Channel? get() = allChannels.find { it.id == selectedChannelId } ?: visibleChannels.firstOrNull()
}

/**
 * Country sidebar + channel list + preview panel shown after login/from Home
 * so the user picks a channel manually — nothing plays until "Watch Full
 * Screen" is pressed. Replaces the old behavior of auto-tuning a channel on
 * the user's behalf.
 */
@HiltViewModel
class LiveTvBrowseViewModel @Inject constructor(
    private val observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val observeChannelsUseCase: ObserveChannelsUseCase,
    private val syncChannelsUseCase: SyncChannelsUseCase,
    private val getEpgNowNextUseCase: GetEpgNowNextUseCase,
    private val getEpgListingsUseCase: GetEpgListingsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveTvBrowseUiState())
    val uiState: StateFlow<LiveTvBrowseUiState> = _uiState.asStateFlow()

    private val requestedEpgChannelIds = mutableSetOf<String>()
    private val requestedEpgListingChannelIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            combine(observeCategoriesUseCase(), observeChannelsUseCase()) { categories, channels ->
                categories to channels
            }.collect { (categories, channels) ->
                _uiState.update {
                    val visible = filterChannels(channels, it.selectedCategoryId)
                    it.copy(
                        categories = categories,
                        countryGroups = groupCategoriesByCountry(categories),
                        allChannels = channels,
                        visibleChannels = visible,
                        selectedChannelId = it.selectedChannelId ?: visible.firstOrNull()?.id,
                        isLoading = if (channels.isNotEmpty()) false else it.isLoading
                    )
                }
            }
        }
        refresh()
    }

    /** Re-fetches categories/channels from the Xtream API; also drives the initial loading screen. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.allChannels.isEmpty(), loadError = null) }
            syncChannelsUseCase()
            if (_uiState.value.allChannels.isEmpty()) {
                _uiState.update {
                    it.copy(isLoading = false, loadError = "Couldn't load channels. Check your connection and try again.")
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onCountrySelected(code: String?) {
        _uiState.update { it.copy(selectedCountryCode = code, selectedCategoryId = null) }
    }

    fun onCategorySelected(categoryId: String?) {
        _uiState.update {
            val visible = filterChannels(it.allChannels, categoryId)
            it.copy(selectedCategoryId = categoryId, visibleChannels = visible, selectedChannelId = visible.firstOrNull()?.id)
        }
    }

    fun onChannelHighlighted(channelId: String) {
        _uiState.update { it.copy(selectedChannelId = channelId) }
    }

    fun requestEpgFor(channelId: String) {
        if (!requestedEpgChannelIds.add(channelId)) return
        viewModelScope.launch {
            val nowNext = getEpgNowNextUseCase(channelId)
            _uiState.update { it.copy(epgCache = it.epgCache + (channelId to nowNext)) }
        }
    }

    fun requestEpgListingsFor(channelId: String) {
        if (!requestedEpgListingChannelIds.add(channelId)) return
        viewModelScope.launch {
            val listings = getEpgListingsUseCase(channelId)
            _uiState.update { it.copy(epgListingsCache = it.epgListingsCache + (channelId to listings)) }
        }
    }

    fun onToggleFavorite(channelId: String) {
        viewModelScope.launch { toggleFavoriteUseCase(channelId) }
    }

    private fun filterChannels(channels: List<Channel>, categoryId: String?): List<Channel> =
        if (categoryId == null) channels else channels.filter { it.categoryId == categoryId }
}
