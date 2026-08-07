package com.sirktv.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.SearchResults
import com.sirktv.app.domain.usecase.ClearSearchHistoryUseCase
import com.sirktv.app.domain.usecase.ObserveRecentSearchesUseCase
import com.sirktv.app.domain.usecase.RecordSearchQueryUseCase
import com.sirktv.app.domain.usecase.SearchContentUseCase
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
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase
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

    private suspend fun runSearch(query: String) {
        val results = searchContentUseCase(query)
        _uiState.update { it.copy(results = results, hasSearched = query.trim().length >= 2) }
    }
}
