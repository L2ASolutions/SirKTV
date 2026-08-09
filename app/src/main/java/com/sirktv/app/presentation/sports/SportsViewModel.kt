package com.sirktv.app.presentation.sports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.model.EpgProgram
import com.sirktv.app.domain.usecase.GetEpgListingsUseCase
import com.sirktv.app.domain.usecase.GetEpgNowNextUseCase
import com.sirktv.app.domain.usecase.ObserveSportsChannelsUseCase
import com.sirktv.app.domain.usecase.SyncChannelsUseCase
import com.sirktv.app.domain.usecase.ToggleFavoriteUseCase
import com.sirktv.app.presentation.common.SECTION_SYNC_TIMEOUT_MS
import com.sirktv.app.presentation.common.SectionLoadError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/** Flat filter list shown in the Sports sidebar — keyword-matched against category/channel names, not tied to provider category IDs. */
enum class SportsFilter(val label: String, val keywords: List<String>) {
    ALL_SPORTS("All Sports", emptyList()),
    BASKETBALL("Basketball", listOf("basketball", "nba", "ncaab")),
    SOCCER("Soccer", listOf("soccer", "football", "fifa", "uefa", "premier league", "la liga", "seri", "bundesliga")),
    HOCKEY("Hockey", listOf("hockey", "nhl")),
    PPV("PPV", listOf("ppv", "pay-per-view", "pay per view"))
}

data class SportsUiState(
    val isLoading: Boolean = true,
    val loadError: SectionLoadError? = null,
    val categories: List<Category> = emptyList(),
    val allChannels: List<Channel> = emptyList(),
    val visibleChannels: List<Channel> = emptyList(),
    val selectedFilter: SportsFilter = SportsFilter.ALL_SPORTS,
    val selectedChannelId: String? = null,
    val epgCache: Map<String, EpgNowNext> = emptyMap(),
    val epgListingsCache: Map<String, List<EpgProgram>> = emptyMap()
) {
    val selectedChannel: Channel? get() = allChannels.find { it.id == selectedChannelId } ?: visibleChannels.firstOrNull()
}

/**
 * Sports channels are just a keyword-filtered slice of Live TV channels —
 * there is no separate "sports sync." If Live TV has already been synced
 * this loads instantly from Room; otherwise this triggers the same
 * [SyncChannelsUseCase] Live TV uses, then filters whatever comes back.
 */
@HiltViewModel
class SportsViewModel @Inject constructor(
    private val observeSportsChannelsUseCase: ObserveSportsChannelsUseCase,
    private val syncChannelsUseCase: SyncChannelsUseCase,
    private val getEpgNowNextUseCase: GetEpgNowNextUseCase,
    private val getEpgListingsUseCase: GetEpgListingsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SportsUiState())
    val uiState: StateFlow<SportsUiState> = _uiState.asStateFlow()

    private val requestedEpgChannelIds = mutableSetOf<String>()
    private val requestedEpgListingChannelIds = mutableSetOf<String>()

    init {
        refresh()

        viewModelScope.launch {
            observeSportsChannelsUseCase().collect { catalog ->
                _uiState.update {
                    val visible = filterChannels(catalog.channels, catalog.categories, it.selectedFilter)
                    it.copy(
                        categories = catalog.categories,
                        allChannels = catalog.channels,
                        visibleChannels = visible,
                        selectedChannelId = it.selectedChannelId ?: visible.firstOrNull()?.id,
                        // Self-heals any stale error the moment real data actually
                        // lands, regardless of what triggered it to arrive.
                        loadError = if (catalog.channels.isNotEmpty()) null else it.loadError
                    )
                }
            }
        }
    }

    /** Syncs the underlying Live TV channel cache (skipped for free by Room if it's already warm) then filters to sports. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.allChannels.isEmpty(), loadError = null) }
            val result = withTimeoutOrNull(SECTION_SYNC_TIMEOUT_MS) { syncChannelsUseCase() }
            val error = when {
                result == null -> SectionLoadError.TIMEOUT
                result.isFailure -> SectionLoadError.NETWORK
                _uiState.value.allChannels.isEmpty() -> SectionLoadError.EMPTY
                else -> null
            }
            _uiState.update { it.copy(isLoading = false, loadError = error) }
        }
    }

    fun onFilterSelected(filter: SportsFilter) {
        _uiState.update {
            val visible = filterChannels(it.allChannels, it.categories, filter)
            it.copy(selectedFilter = filter, visibleChannels = visible, selectedChannelId = visible.firstOrNull()?.id)
        }
    }

    fun onChannelHighlighted(channelId: String) {
        _uiState.update { it.copy(selectedChannelId = channelId) }
    }

    fun requestEpgFor(channelId: String) {
        if (!requestedEpgChannelIds.add(channelId)) return
        viewModelScope.launch {
            val nowNext = runCatching { getEpgNowNextUseCase(channelId) }
                .getOrDefault(EpgNowNext(now = null, next = null))
            _uiState.update { it.copy(epgCache = it.epgCache + (channelId to nowNext)) }
        }
    }

    fun requestEpgListingsFor(channelId: String) {
        if (!requestedEpgListingChannelIds.add(channelId)) return
        viewModelScope.launch {
            val listings = runCatching { getEpgListingsUseCase(channelId) }.getOrDefault(emptyList())
            _uiState.update { it.copy(epgListingsCache = it.epgListingsCache + (channelId to listings)) }
        }
    }

    fun onToggleFavorite(channelId: String) {
        viewModelScope.launch { toggleFavoriteUseCase(channelId) }
    }

    private fun filterChannels(channels: List<Channel>, categories: List<Category>, filter: SportsFilter): List<Channel> {
        if (filter == SportsFilter.ALL_SPORTS) return channels
        val categoryNameById = categories.associate { it.id to it.name.lowercase() }
        return channels.filter { channel ->
            val haystack = "${channel.name.lowercase()} ${categoryNameById[channel.categoryId].orEmpty()}"
            filter.keywords.any { haystack.contains(it) }
        }
    }
}
