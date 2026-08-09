package com.sirktv.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.ContentIds
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.WatchProgress
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.domain.usecase.ClearSavedCredentialsUseCase
import com.sirktv.app.domain.usecase.ObserveContinueWatchingUseCase
import com.sirktv.app.domain.usecase.ObserveRecentlyWatchedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeNavTarget {
    data class LiveTv(val channelId: String) : HomeNavTarget
    data class MoviePlayer(val movieId: String) : HomeNavTarget
    data class EpisodePlayer(val seriesId: String, val season: Int, val episode: Int) : HomeNavTarget
    data class SeriesDetail(val seriesId: String) : HomeNavTarget
}

sealed interface HomeEvent {
    data object NavigateToLogin : HomeEvent
}

/**
 * Continue Watching / Recently Watched are pure Room reads (watch history the
 * user has already generated locally) — nothing here ever calls the Xtream
 * API, so both rows are safe to observe immediately and render the instant
 * Home appears, with no spinner and no sync. If Room has nothing yet, the
 * corresponding list is simply empty and the row doesn't render.
 */
data class HomeUiState(
    val continueWatching: List<WatchProgress> = emptyList(),
    val recentlyWatched: List<WatchProgress> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeContinueWatchingUseCase: ObserveContinueWatchingUseCase,
    observeRecentlyWatchedUseCase: ObserveRecentlyWatchedUseCase,
    private val clearSavedCredentialsUseCase: ClearSavedCredentialsUseCase,
    private val currentSession: CurrentSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(observeContinueWatchingUseCase(), observeRecentlyWatchedUseCase()) { continueWatching, recentlyWatched ->
                HomeUiState(continueWatching = continueWatching, recentlyWatched = recentlyWatched)
            }.collect { state -> _uiState.value = state }
        }
    }

    fun onLogoutClicked() {
        viewModelScope.launch {
            clearSavedCredentialsUseCase()
            currentSession.clear()
            _events.emit(HomeEvent.NavigateToLogin)
        }
    }

    fun watchProgressTarget(progress: WatchProgress): HomeNavTarget? = when (progress.contentType) {
        ContentType.LIVE -> HomeNavTarget.LiveTv(progress.contentId)
        ContentType.MOVIE -> HomeNavTarget.MoviePlayer(progress.contentId)
        ContentType.EPISODE -> ContentIds.parseEpisode(progress.contentId)?.let { (seriesId, season, episode) ->
            HomeNavTarget.EpisodePlayer(seriesId, season, episode)
        }
        ContentType.SERIES -> HomeNavTarget.SeriesDetail(progress.contentId)
    }
}
