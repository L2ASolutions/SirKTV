package com.sirktv.app.presentation.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.model.WatchProgress
import com.sirktv.app.domain.util.RecentlyAdded
import com.sirktv.app.domain.usecase.ObserveContinueWatchingUseCase
import com.sirktv.app.domain.usecase.ObserveMovieCategoriesUseCase
import com.sirktv.app.domain.usecase.ObserveMoviesUseCase
import com.sirktv.app.domain.usecase.SyncMoviesUseCase
import com.sirktv.app.domain.usecase.ToggleMovieFavoriteUseCase
import com.sirktv.app.presentation.common.SECTION_SYNC_TIMEOUT_MS
import com.sirktv.app.presentation.common.SectionLoadError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

private const val ROW_LIMIT = 20

/** One horizontally-scrolling shelf on the Movies browse screen. */
data class MovieRow(val title: String, val movies: List<Movie>)

data class MoviesUiState(
    val isLoading: Boolean = true,
    val loadError: SectionLoadError? = null,
    val categories: List<Category> = emptyList(),
    val allMovies: List<Movie> = emptyList(),
    val continueWatching: List<WatchProgress> = emptyList(),
    // null = the "All" genre pill — shows the full Netflix-style row browse
    // below. Any other value narrows to a single-category poster grid, same
    // pattern as Series.
    val selectedCategoryId: String? = null
) {
    val visibleMovies: List<Movie>
        get() = selectedCategoryId?.let { id -> allMovies.filter { it.categoryId == id } } ?: allMovies

    /** Recently Added + genre rows — pulled dynamically from whatever categories the Xtream account has. */
    val rows: List<MovieRow>
        get() = buildList {
            if (allMovies.isNotEmpty()) {
                add(MovieRow("Recently Added", recentlyAddedMovies))
            }
            categories.forEach { category ->
                val inCategory = allMovies.filter { it.categoryId == category.id }
                if (inCategory.isNotEmpty()) add(MovieRow(category.name, inCategory))
            }
        }

    /**
     * Ranked most-recently-added first (real "added" timestamp, falling back
     * to stream ID when the provider doesn't send one) — see [RecentlyAdded].
     */
    val recentlyAddedMovies: List<Movie>
        get() = RecentlyAdded.select(
            items = allMovies,
            limit = ROW_LIMIT,
            addedAtEpochMillis = { it.addedAtEpochMillis },
            fallbackKey = { it.id.toLongOrNull() ?: 0L }
        )
}

/**
 * Movies never syncs on app startup or from Home — this is the on-demand
 * sync for the section, triggered the first time the screen is opened (and
 * again from the top bar's refresh icon or the error screen's Retry button).
 * Room is the source of truth: [observeMoviesUseCase] drives the UI
 * reactively, [refresh] only decides whether to show a spinner or an error.
 */
@HiltViewModel
class MoviesViewModel @Inject constructor(
    observeMovieCategoriesUseCase: ObserveMovieCategoriesUseCase,
    observeMoviesUseCase: ObserveMoviesUseCase,
    observeContinueWatchingUseCase: ObserveContinueWatchingUseCase,
    private val syncMoviesUseCase: SyncMoviesUseCase,
    private val toggleMovieFavoriteUseCase: ToggleMovieFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    init {
        refresh()

        viewModelScope.launch {
            combine(
                observeMovieCategoriesUseCase(),
                observeMoviesUseCase(),
                observeContinueWatchingUseCase()
            ) { categories, movies, continueWatching ->
                Triple(categories, movies, continueWatching.filter { it.contentType == ContentType.MOVIE })
            }.collect { (categories, movies, continueWatching) ->
                _uiState.update {
                    it.copy(
                        categories = categories,
                        allMovies = movies,
                        continueWatching = continueWatching,
                        // Self-heals any stale error the moment real data actually lands,
                        // regardless of what triggered it to arrive.
                        loadError = if (movies.isNotEmpty()) null else it.loadError
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.allMovies.isEmpty(), loadError = null) }
            val result = withTimeoutOrNull(SECTION_SYNC_TIMEOUT_MS) { syncMoviesUseCase() }
            val error = when {
                result == null -> SectionLoadError.TIMEOUT
                result.isFailure -> SectionLoadError.NETWORK
                _uiState.value.allMovies.isEmpty() -> SectionLoadError.EMPTY
                else -> null
            }
            _uiState.update { it.copy(isLoading = false, loadError = error) }
        }
    }

    fun onCategorySelected(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun onToggleFavorite(movieId: String) {
        viewModelScope.launch { toggleMovieFavoriteUseCase(movieId) }
    }
}
