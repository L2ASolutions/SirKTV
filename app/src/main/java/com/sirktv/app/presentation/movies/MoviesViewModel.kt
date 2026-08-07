package com.sirktv.app.presentation.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.usecase.ObserveMovieCategoriesUseCase
import com.sirktv.app.domain.usecase.ObserveMoviesUseCase
import com.sirktv.app.domain.usecase.SyncMoviesUseCase
import com.sirktv.app.domain.usecase.ToggleMovieFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MoviesUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val allMovies: List<Movie> = emptyList(),
    val visibleMovies: List<Movie> = emptyList()
)

@HiltViewModel
class MoviesViewModel @Inject constructor(
    observeMovieCategoriesUseCase: ObserveMovieCategoriesUseCase,
    observeMoviesUseCase: ObserveMoviesUseCase,
    private val syncMoviesUseCase: SyncMoviesUseCase,
    private val toggleMovieFavoriteUseCase: ToggleMovieFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { syncMoviesUseCase() }

        viewModelScope.launch {
            observeMovieCategoriesUseCase().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        viewModelScope.launch {
            observeMoviesUseCase().collect { movies ->
                _uiState.update { it.copy(allMovies = movies, visibleMovies = filter(movies, it.selectedCategoryId)) }
            }
        }
    }

    fun onCategorySelected(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId, visibleMovies = filter(it.allMovies, categoryId)) }
    }

    fun onToggleFavorite(movieId: String) {
        viewModelScope.launch { toggleMovieFavoriteUseCase(movieId) }
    }

    private fun filter(movies: List<Movie>, categoryId: String?): List<Movie> =
        if (categoryId == null) movies else movies.filter { it.categoryId == categoryId }
}
