package com.sirktv.app.presentation.series

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.Episode
import com.sirktv.app.domain.model.SeasonInfo
import com.sirktv.app.domain.model.Series
import com.sirktv.app.domain.usecase.GetSeriesInfoUseCase
import com.sirktv.app.domain.usecase.ObserveSeriesUseCase
import com.sirktv.app.domain.usecase.ToggleSeriesFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SeriesDetailUiState(
    val series: Series? = null,
    val seasons: List<SeasonInfo> = emptyList(),
    val selectedSeasonNumber: Int? = null,
    val isLoading: Boolean = true
) {
    val selectedSeasonEpisodes: List<Episode>
        get() = seasons.find { it.seasonNumber == selectedSeasonNumber }?.episodes.orEmpty()
}

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeSeriesUseCase: ObserveSeriesUseCase,
    private val getSeriesInfoUseCase: GetSeriesInfoUseCase,
    private val toggleSeriesFavoriteUseCase: ToggleSeriesFavoriteUseCase
) : ViewModel() {

    private val seriesId: String = checkNotNull(savedStateHandle["seriesId"])

    private val _uiState = MutableStateFlow(SeriesDetailUiState())
    val uiState: StateFlow<SeriesDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSeriesUseCase().collect { list ->
                _uiState.update { it.copy(series = list.find { series -> series.id == seriesId }) }
            }
        }
        viewModelScope.launch {
            val info = getSeriesInfoUseCase(seriesId)
            val firstSeason = info?.seasons?.minByOrNull { it.seasonNumber }?.seasonNumber
            _uiState.update { it.copy(seasons = info?.seasons.orEmpty(), selectedSeasonNumber = firstSeason, isLoading = false) }
        }
    }

    fun onSeasonSelected(seasonNumber: Int) {
        _uiState.update { it.copy(selectedSeasonNumber = seasonNumber) }
    }

    fun onToggleFavorite() {
        viewModelScope.launch { toggleSeriesFavoriteUseCase(seriesId) }
    }
}
