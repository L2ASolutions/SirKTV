package com.sirktv.app.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.FavoriteItem
import com.sirktv.app.domain.model.FavoriteSort
import com.sirktv.app.domain.usecase.ObserveAllFavoritesUseCase
import com.sirktv.app.domain.usecase.ObserveFavoriteChannelsUseCase
import com.sirktv.app.domain.usecase.ObserveFavoriteMoviesUseCase
import com.sirktv.app.domain.usecase.ObserveFavoriteSeriesUseCase
import com.sirktv.app.domain.usecase.ReorderFavoriteUseCase
import com.sirktv.app.domain.usecase.ToggleFavoriteUseCase
import com.sirktv.app.domain.usecase.ToggleMovieFavoriteUseCase
import com.sirktv.app.domain.usecase.ToggleSeriesFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val selectedType: ContentType = ContentType.LIVE,
    val sort: FavoriteSort = FavoriteSort.CUSTOM,
    val items: List<FavoriteItem> = emptyList(),
    val channelCount: Int = 0,
    val movieCount: Int = 0,
    val seriesCount: Int = 0
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val observeAllFavoritesUseCase: ObserveAllFavoritesUseCase,
    private val observeFavoriteChannelsUseCase: ObserveFavoriteChannelsUseCase,
    private val observeFavoriteMoviesUseCase: ObserveFavoriteMoviesUseCase,
    private val observeFavoriteSeriesUseCase: ObserveFavoriteSeriesUseCase,
    private val reorderFavoriteUseCase: ReorderFavoriteUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val toggleMovieFavoriteUseCase: ToggleMovieFavoriteUseCase,
    private val toggleSeriesFavoriteUseCase: ToggleSeriesFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private var favoritesJob: Job? = null

    init {
        loadFavorites()
        viewModelScope.launch {
            combine(
                observeFavoriteChannelsUseCase(),
                observeFavoriteMoviesUseCase(),
                observeFavoriteSeriesUseCase()
            ) { channels, movies, series -> Triple(channels.size, movies.size, series.size) }
                .collect { (channelCount, movieCount, seriesCount) ->
                    _uiState.update { it.copy(channelCount = channelCount, movieCount = movieCount, seriesCount = seriesCount) }
                }
        }
    }

    fun onTypeSelected(type: ContentType) {
        _uiState.update { it.copy(selectedType = type) }
        loadFavorites()
    }

    fun onSortSelected(sort: FavoriteSort) {
        _uiState.update { it.copy(sort = sort) }
        loadFavorites()
    }

    fun onRemove(item: FavoriteItem) {
        viewModelScope.launch {
            when (item.contentType) {
                ContentType.LIVE -> toggleFavoriteUseCase(item.contentId)
                ContentType.MOVIE -> toggleMovieFavoriteUseCase(item.contentId)
                ContentType.SERIES -> toggleSeriesFavoriteUseCase(item.contentId)
                ContentType.EPISODE -> Unit
            }
        }
    }

    fun onMoveUp(item: FavoriteItem) {
        viewModelScope.launch { reorderFavoriteUseCase.moveUp(item.contentId, item.contentType) }
    }

    fun onMoveDown(item: FavoriteItem) {
        viewModelScope.launch { reorderFavoriteUseCase.moveDown(item.contentId, item.contentType) }
    }

    private fun loadFavorites() {
        favoritesJob?.cancel()
        val state = _uiState.value
        favoritesJob = viewModelScope.launch {
            observeAllFavoritesUseCase(state.selectedType, state.sort).collect { items ->
                _uiState.update { it.copy(items = items) }
            }
        }
    }
}
