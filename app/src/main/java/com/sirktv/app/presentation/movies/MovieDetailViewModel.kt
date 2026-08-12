package com.sirktv.app.presentation.movies

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.model.MovieDetail
import com.sirktv.app.domain.usecase.GetMovieDetailUseCase
import com.sirktv.app.domain.usecase.ObserveMoviesUseCase
import com.sirktv.app.domain.usecase.ToggleMovieFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieDetailUiState(
    val movie: Movie? = null,
    val detail: MovieDetail? = null,
    val isLoading: Boolean = true
)

/**
 * [movie] (title/poster/rating/favorite state) comes from the already-synced
 * catalog via [ObserveMoviesUseCase], same as [com.sirktv.app.presentation.vodplayer.VodPlayerViewModel]
 * does for the player's title card — instant, reactive, no extra request.
 * [detail] (synopsis/cast/director/genre/duration) is the one-shot get_vod_info
 * call [GetMovieDetailUseCase] wraps; see [MovieDetail]'s doc comment for why
 * that's fetched on demand rather than synced with the catalog.
 */
@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeMoviesUseCase: ObserveMoviesUseCase,
    private val getMovieDetailUseCase: GetMovieDetailUseCase,
    private val toggleMovieFavoriteUseCase: ToggleMovieFavoriteUseCase
) : ViewModel() {

    private val movieId: String = checkNotNull(savedStateHandle["movieId"])

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                observeMoviesUseCase().collect { movies ->
                    _uiState.update { it.copy(movie = movies.find { movie -> movie.id == movieId }) }
                }
            }
        }
        viewModelScope.launch {
            val detail = getMovieDetailUseCase(movieId)
            _uiState.update { it.copy(detail = detail, isLoading = false) }
        }
    }

    fun onToggleFavorite() {
        viewModelScope.launch { toggleMovieFavoriteUseCase(movieId) }
    }
}
