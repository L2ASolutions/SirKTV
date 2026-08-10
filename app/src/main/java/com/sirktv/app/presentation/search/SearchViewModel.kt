package com.sirktv.app.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.SearchResults
import com.sirktv.app.domain.usecase.ClearSearchHistoryUseCase
import com.sirktv.app.domain.usecase.ObserveRecentSearchesUseCase
import com.sirktv.app.domain.usecase.RecordSearchQueryUseCase
import com.sirktv.app.domain.usecase.SearchContentUseCase
import com.sirktv.app.domain.usecase.ToggleFavoriteUseCase
import com.sirktv.app.domain.usecase.ToggleMovieFavoriteUseCase
import com.sirktv.app.domain.usecase.ToggleSeriesFavoriteUseCase
import com.sirktv.app.presentation.navigation.SirKTVDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MS = 300L

data class SearchUiState(
    val query: String = "",
    val recentSearches: List<String> = emptyList(),
    val results: SearchResults = SearchResults.EMPTY,
    val hasSearched: Boolean = false,
    /** Set when Search was opened from a specific section's own Search button — see [SirKTVDestinations.SearchSection]. Null (the hardware SEARCH/mic button's path) means unscoped, cross-content search. */
    val section: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeRecentSearchesUseCase: ObserveRecentSearchesUseCase,
    private val searchContentUseCase: SearchContentUseCase,
    private val recordSearchQueryUseCase: RecordSearchQueryUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase,
    private val toggleChannelFavoriteUseCase: ToggleFavoriteUseCase,
    private val toggleMovieFavoriteUseCase: ToggleMovieFavoriteUseCase,
    private val toggleSeriesFavoriteUseCase: ToggleSeriesFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching {
                observeRecentSearchesUseCase().collect { recent ->
                    _uiState.update { it.copy(recentSearches = recent) }
                }
            }
        }
        // Set only when a section's own Search button (Live TV/Movies/Series)
        // launched this screen — the hardware SEARCH/mic button never sets it,
        // so that path stays unscoped cross-content search as before.
        val section = savedStateHandle.get<String>("section")?.takeIf { it.isNotBlank() }
        _uiState.update { it.copy(section = section) }
        // Pre-filled by the Fire TV remote's mic button (ACTION_SEARCH) or the
        // hardware SEARCH key reopening this screen with a query already typed.
        val initialQuery = savedStateHandle.get<String>("query").orEmpty()
        if (initialQuery.isNotBlank()) onSearchSubmitted(initialQuery)
    }

    /** Live-filters as the user types; does not touch search history — only [onSearchSubmitted] does. */
    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runSearch(query)
        }
    }

    /** Explicit commit (IME search action or picking a recent chip) — the only path that writes history. */
    fun onSearchSubmitted(query: String = _uiState.value.query) {
        searchJob?.cancel()
        _uiState.update { it.copy(query = query) }
        viewModelScope.launch {
            runSearch(query)
            recordSearchQueryUseCase(query)
        }
    }

    fun onClearHistory() {
        viewModelScope.launch { clearSearchHistoryUseCase() }
    }

    /** Toggles favorite state directly from a result row, then re-runs the current search so the icon reflects it. */
    fun onToggleChannelFavorite(channelId: String) = toggleAndRefresh { toggleChannelFavoriteUseCase(channelId) }
    fun onToggleMovieFavorite(movieId: String) = toggleAndRefresh { toggleMovieFavoriteUseCase(movieId) }
    fun onToggleSeriesFavorite(seriesId: String) = toggleAndRefresh { toggleSeriesFavoriteUseCase(seriesId) }

    private fun toggleAndRefresh(toggle: suspend () -> Unit) {
        viewModelScope.launch {
            toggle()
            runSearch(_uiState.value.query)
        }
    }

    private suspend fun runSearch(query: String) {
        val results = searchContentUseCase(query).scopedTo(_uiState.value.section)
        _uiState.update { it.copy(results = results, hasSearched = query.trim().length >= 2) }
    }

    /** Zeroes out the two content types [section] didn't ask for — see [SearchUiState.section]. */
    private fun SearchResults.scopedTo(section: String?): SearchResults = when (section) {
        SirKTVDestinations.SearchSection.LIVE -> copy(movies = emptyList(), series = emptyList())
        SirKTVDestinations.SearchSection.MOVIES -> copy(channels = emptyList(), series = emptyList())
        SirKTVDestinations.SearchSection.SERIES -> copy(channels = emptyList(), movies = emptyList())
        else -> this
    }
}
