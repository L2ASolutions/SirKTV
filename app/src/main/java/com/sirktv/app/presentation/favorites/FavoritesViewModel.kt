package com.sirktv.app.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.model.Series
import com.sirktv.app.domain.usecase.GetEpgNowNextUseCase
import com.sirktv.app.domain.usecase.ObserveFavoriteChannelsUseCase
import com.sirktv.app.domain.usecase.ObserveFavoriteMoviesUseCase
import com.sirktv.app.domain.usecase.ObserveFavoriteSeriesUseCase
import com.sirktv.app.domain.usecase.ToggleFavoriteUseCase
import com.sirktv.app.domain.usecase.ToggleMovieFavoriteUseCase
import com.sirktv.app.domain.usecase.ToggleSeriesFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val channels: List<Channel> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList()
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    observeFavoriteChannelsUseCase: ObserveFavoriteChannelsUseCase,
    observeFavoriteMoviesUseCase: ObserveFavoriteMoviesUseCase,
    observeFavoriteSeriesUseCase: ObserveFavoriteSeriesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val toggleMovieFavoriteUseCase: ToggleMovieFavoriteUseCase,
    private val toggleSeriesFavoriteUseCase: ToggleSeriesFavoriteUseCase,
    private val getEpgNowNextUseCase: GetEpgNowNextUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    // Same lazy-on-visible EPG pattern as Home/LiveTvBrowse — populates now/next
    // for the Live TV accordion's channel cards.
    private val _channelEpgCache = MutableStateFlow<Map<String, EpgNowNext>>(emptyMap())
    val channelEpgCache: StateFlow<Map<String, EpgNowNext>> = _channelEpgCache.asStateFlow()
    private val requestedEpgChannelIds = mutableSetOf<String>()

    fun requestEpgFor(channelId: String) {
        if (!requestedEpgChannelIds.add(channelId)) return
        viewModelScope.launch {
            val nowNext = getEpgNowNextUseCase(channelId)
            _channelEpgCache.update { it + (channelId to nowNext) }
        }
    }

    init {
        viewModelScope.launch {
            runCatching {
                combine(
                    observeFavoriteChannelsUseCase(),
                    observeFavoriteMoviesUseCase(),
                    observeFavoriteSeriesUseCase()
                ) { channels, movies, series -> FavoritesUiState(channels, movies, series) }
                    .collect { state -> _uiState.value = state }
            }
        }
    }

    fun onRemoveChannel(channelId: String) {
        viewModelScope.launch { toggleFavoriteUseCase(channelId) }
    }

    fun onRemoveMovie(movieId: String) {
        viewModelScope.launch { toggleMovieFavoriteUseCase(movieId) }
    }

    fun onRemoveSeries(seriesId: String) {
        viewModelScope.launch { toggleSeriesFavoriteUseCase(seriesId) }
    }
}
