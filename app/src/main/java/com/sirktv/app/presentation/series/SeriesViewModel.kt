package com.sirktv.app.presentation.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Series
import com.sirktv.app.domain.usecase.ObserveSeriesCategoriesUseCase
import com.sirktv.app.domain.usecase.ObserveSeriesUseCase
import com.sirktv.app.domain.usecase.SyncSeriesUseCase
import com.sirktv.app.domain.usecase.ToggleSeriesFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SeriesUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val allSeries: List<Series> = emptyList(),
    val visibleSeries: List<Series> = emptyList(),
    val searchQuery: String = ""
) {
    val isSearching: Boolean get() = searchQuery.trim().length >= 2

    val searchResults: List<Series>
        get() {
            val normalized = searchQuery.trim().lowercase()
            if (normalized.length < 2) return emptyList()
            return allSeries.filter { it.title.lowercase().contains(normalized) }
        }

    // Series has no add-time column in the local schema (unlike Movie), so
    // "recently added" is approximated by catalog order — the order the
    // provider itself returns it in — rather than a fabricated timestamp.
    val recentlyAdded: List<Series>
        get() = allSeries.take(RECENTLY_ADDED_LIMIT)
}

private const val RECENTLY_ADDED_LIMIT = 20

@HiltViewModel
class SeriesViewModel @Inject constructor(
    observeSeriesCategoriesUseCase: ObserveSeriesCategoriesUseCase,
    observeSeriesUseCase: ObserveSeriesUseCase,
    private val syncSeriesUseCase: SyncSeriesUseCase,
    private val toggleSeriesFavoriteUseCase: ToggleSeriesFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { syncSeriesUseCase() }

        viewModelScope.launch {
            observeSeriesCategoriesUseCase().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        viewModelScope.launch {
            observeSeriesUseCase().collect { series ->
                _uiState.update { it.copy(allSeries = series, visibleSeries = filter(series, it.selectedCategoryId)) }
            }
        }
    }

    fun onCategorySelected(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId, visibleSeries = filter(it.allSeries, categoryId)) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onToggleFavorite(seriesId: String) {
        viewModelScope.launch { toggleSeriesFavoriteUseCase(seriesId) }
    }

    private fun filter(series: List<Series>, categoryId: String?): List<Series> =
        if (categoryId == null) series else series.filter { it.categoryId == categoryId }
}
