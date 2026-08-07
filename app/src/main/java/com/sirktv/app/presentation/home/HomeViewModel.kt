package com.sirktv.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.ContentIds
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.model.Series
import com.sirktv.app.domain.model.UserProfile
import com.sirktv.app.domain.model.WatchProgress
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.domain.usecase.ClearSavedCredentialsUseCase
import com.sirktv.app.domain.usecase.ObserveContinueWatchingUseCase
import com.sirktv.app.domain.usecase.ObserveFavoriteChannelsUseCase
import com.sirktv.app.domain.usecase.ObserveMoviesUseCase
import com.sirktv.app.domain.usecase.ObserveRecentlyWatchedUseCase
import com.sirktv.app.domain.usecase.ObserveSeriesUseCase
import com.sirktv.app.domain.usecase.ObserveSportsChannelsUseCase
import com.sirktv.app.domain.usecase.PickDefaultChannelUseCase
import com.sirktv.app.domain.usecase.SyncChannelsUseCase
import com.sirktv.app.domain.usecase.SyncMoviesUseCase
import com.sirktv.app.domain.usecase.SyncSeriesUseCase
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
    data class NavigateToLiveTv(val channelId: String) : HomeEvent
    data object NoChannelsAvailable : HomeEvent
    data object NavigateToLogin : HomeEvent
}

data class HomeUiState(
    val heroTitle: String = "SirKTV",
    val heroSubtitle: String = "Your World. Your Channels.",
    val heroTarget: HomeNavTarget? = null,
    val continueWatching: List<WatchProgress> = emptyList(),
    val favoriteChannels: List<Channel> = emptyList(),
    val recentlyWatched: List<WatchProgress> = emptyList(),
    val trendingMovies: List<Movie> = emptyList(),
    val popularSeries: List<Series> = emptyList(),
    val liveSportsNow: List<Channel> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeContinueWatchingUseCase: ObserveContinueWatchingUseCase,
    observeFavoriteChannelsUseCase: ObserveFavoriteChannelsUseCase,
    observeRecentlyWatchedUseCase: ObserveRecentlyWatchedUseCase,
    observeMoviesUseCase: ObserveMoviesUseCase,
    observeSeriesUseCase: ObserveSeriesUseCase,
    observeSportsChannelsUseCase: ObserveSportsChannelsUseCase,
    private val syncChannelsUseCase: SyncChannelsUseCase,
    private val syncMoviesUseCase: SyncMoviesUseCase,
    private val syncSeriesUseCase: SyncSeriesUseCase,
    private val pickDefaultChannelUseCase: PickDefaultChannelUseCase,
    private val clearSavedCredentialsUseCase: ClearSavedCredentialsUseCase,
    private val currentSession: CurrentSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val profile: StateFlow<UserProfile?> = currentSession.profile

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private var isResolvingChannel = false

    init {
        // Best-effort background refresh so Home has real data even if the
        // user never separately opens Movies/Series first.
        viewModelScope.launch { syncChannelsUseCase() }
        viewModelScope.launch { syncMoviesUseCase() }
        viewModelScope.launch { syncSeriesUseCase() }

        val catalogFlow = combine(
            observeMoviesUseCase(),
            observeSeriesUseCase(),
            observeSportsChannelsUseCase()
        ) { movies, series, sportsCatalog ->
            Triple(
                movies.sortedByDescending { it.rating ?: 0f }.take(15),
                series.sortedByDescending { it.rating ?: 0f }.take(15),
                sportsCatalog.channels.take(15)
            )
        }
        val watchFlow = combine(
            observeContinueWatchingUseCase(),
            observeFavoriteChannelsUseCase(),
            observeRecentlyWatchedUseCase()
        ) { continueWatching, favorites, recent -> Triple(continueWatching, favorites, recent) }

        viewModelScope.launch {
            combine(watchFlow, catalogFlow) { (continueWatching, favorites, recent), (movies, series, sports) ->
                val hero = buildHero(continueWatching, movies)
                HomeUiState(
                    heroTitle = hero.first,
                    heroSubtitle = hero.second,
                    heroTarget = hero.third,
                    continueWatching = continueWatching,
                    favoriteChannels = favorites,
                    recentlyWatched = recent,
                    trendingMovies = movies,
                    popularSeries = series,
                    liveSportsNow = sports
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    private fun buildHero(
        continueWatching: List<WatchProgress>,
        movies: List<Movie>
    ): Triple<String, String, HomeNavTarget?> {
        val top = continueWatching.firstOrNull()
        if (top != null) {
            val target = watchProgressTarget(top)
            return Triple(top.title, "Continue watching" + (top.subtitle?.let { " · $it" } ?: ""), target)
        }
        val trendingMovie = movies.firstOrNull()
        if (trendingMovie != null) {
            return Triple(trendingMovie.title, "Trending now", HomeNavTarget.MoviePlayer(trendingMovie.id))
        }
        return Triple("SirKTV", "Your World. Your Channels.", null)
    }

    /** "Live TV" quick-launch action — jumps straight to a channel without going through Continue Watching/Favorites. */
    fun onLiveTvQuickLaunchClicked() {
        if (isResolvingChannel) return
        isResolvingChannel = true
        viewModelScope.launch {
            val channelId = pickDefaultChannelUseCase()
            isResolvingChannel = false
            if (channelId != null) {
                _events.emit(HomeEvent.NavigateToLiveTv(channelId))
            } else {
                _events.emit(HomeEvent.NoChannelsAvailable)
            }
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
