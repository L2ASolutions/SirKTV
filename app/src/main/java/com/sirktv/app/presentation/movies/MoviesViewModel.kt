package com.sirktv.app.presentation.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.model.WatchProgress
import com.sirktv.app.domain.usecase.ObserveContinueWatchingUseCase
import com.sirktv.app.domain.usecase.ObserveMovieCategoriesUseCase
import com.sirktv.app.domain.usecase.ObserveMoviesUseCase
import com.sirktv.app.domain.usecase.SyncMoviesUseCase
import com.sirktv.app.domain.usecase.ToggleMovieFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ROW_LIMIT = 20

/** One horizontally-scrolling shelf on the Movies browse screen. */
data class MovieRow(val title: String, val movies: List<Movie>)

data class MoviesUiState(
    val categories: List<Category> = emptyList(),
    val allMovies: List<Movie> = emptyList(),
    val continueWatching: List<WatchProgress> = emptyList(),
    val searchQuery: String = ""
) {
    val isSearching: Boolean get() = searchQuery.trim().length >= 2

    val searchResults: List<Movie>
        get() {
            val normalized = searchQuery.trim().lowercase()
            if (normalized.length < 2) return emptyList()
            return allMovies.filter { it.title.lowercase().contains(normalized) }
        }

    /** Recently Added + genre rows — pulled dynamically from whatever categories the Xtream account has. */
    val rows: List<MovieRow>
        get() = buildList {
            if (allMovies.isNotEmpty()) {
                add(MovieRow("Recently Added", allMovies.sortedByDescending { it.addedAtEpochMillis }.take(ROW_LIMIT)))
            }
            categories.forEach { category ->
                val inCategory = allMovies.filter { it.categoryId == category.id }
                if (inCategory.isNotEmpty()) add(MovieRow(category.name, inCategory))
            }
        }
}

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
        viewModelScope.launch { syncMoviesUseCase() }

        viewModelScope.launch {
            combine(
                observeMovieCategoriesUseCase(),
                observeMoviesUseCase(),
                observeContinueWatchingUseCase()
            ) { categories, movies, continueWatching ->
                Triple(categories, movies, continueWatching.filter { it.contentType == ContentType.MOVIE })
            }.collect { (categories, movies, continueWatching) ->
                _uiState.update { it.copy(categories = categories, allMovies = movies, continueWatching = continueWatching) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onToggleFavorite(movieId: String) {
        viewModelScope.launch { toggleMovieFavoriteUseCase(movieId) }
    }
}
