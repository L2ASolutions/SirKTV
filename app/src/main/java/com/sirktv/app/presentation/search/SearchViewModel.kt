package com.sirktv.app.presentation.search

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
    val hasSearched: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
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
            observeRecentSearchesUseCase().collect { recent ->
                _uiState.update { it.copy(recentSearches = recent) }
            }
        }
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
        val results = searchContentUseCase(query)
        _uiState.update { it.copy(results = results, hasSearched = query.trim().length >= 2) }
    }
}
