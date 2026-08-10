package com.sirktv.app.presentation.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Series
import com.sirktv.app.domain.util.RecentlyAdded
import com.sirktv.app.domain.usecase.ObserveSeriesCategoriesUseCase
import com.sirktv.app.domain.usecase.ObserveSeriesUseCase
import com.sirktv.app.domain.usecase.SyncSeriesUseCase
import com.sirktv.app.domain.usecase.ToggleSeriesFavoriteUseCase
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

data class SeriesUiState(
    val isLoading: Boolean = true,
    val loadError: SectionLoadError? = null,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val allSeries: List<Series> = emptyList(),
    val visibleSeries: List<Series> = emptyList()
) {
    // Ranked by Xtream's real last_modified timestamp, falling back to
    // numeric series ID (higher = newer) when the provider doesn't send
    // one — see [RecentlyAdded].
    val recentlyAdded: List<Series>
        get() = RecentlyAdded.select(
            items = allSeries,
            limit = RECENTLY_ADDED_LIMIT,
            addedAtEpochMillis = { it.lastModifiedEpochMillis },
            fallbackKey = { it.id.toLongOrNull() ?: 0L }
        )
}

private const val RECENTLY_ADDED_LIMIT = 20

/**
 * Series never syncs on app startup or from Home — this is the on-demand
 * sync for the section, triggered the first time the screen is opened (and
 * again from the top bar's refresh icon or the error screen's Retry button).
 * Room is the source of truth: the observers below drive the UI reactively,
 * [refresh] only decides whether to show a spinner or an error.
 */
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
        refresh()

        viewModelScope.launch {
            runCatching {
                observeSeriesCategoriesUseCase().collect { categories ->
                    _uiState.update { it.copy(categories = categories) }
                }
            }
        }
        viewModelScope.launch {
            runCatching {
                observeSeriesUseCase().collect { series ->
                    _uiState.update {
                        it.copy(
                            allSeries = series,
                            visibleSeries = filter(series, it.selectedCategoryId),
                            // Self-heals any stale error the moment real data actually
                            // lands, regardless of what triggered it to arrive.
                            loadError = if (series.isNotEmpty()) null else it.loadError
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.allSeries.isEmpty(), loadError = null) }
            val result = withTimeoutOrNull(SECTION_SYNC_TIMEOUT_MS) { syncSeriesUseCase() }
            val error = when {
                result == null -> SectionLoadError.TIMEOUT
                result.isFailure -> SectionLoadError.NETWORK
                _uiState.value.allSeries.isEmpty() -> SectionLoadError.EMPTY
                else -> null
            }
            _uiState.update { it.copy(isLoading = false, loadError = error) }
        }
    }

    fun onCategorySelected(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId, visibleSeries = filter(it.allSeries, categoryId)) }
    }

    fun onToggleFavorite(seriesId: String) {
        viewModelScope.launch { toggleSeriesFavoriteUseCase(seriesId) }
    }

    private fun filter(series: List<Series>, categoryId: String?): List<Series> =
        if (categoryId == null) series else series.filter { it.categoryId == categoryId }
}
