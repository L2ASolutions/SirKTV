package com.sirktv.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.ContentIds
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.model.Series
import com.sirktv.app.domain.model.WatchProgress
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.domain.usecase.ClearSavedCredentialsUseCase
import com.sirktv.app.domain.usecase.GetEpgNowNextUseCase
import com.sirktv.app.domain.usecase.ObserveContinueWatchingUseCase
import com.sirktv.app.domain.usecase.ObserveFavoriteChannelsUseCase
import com.sirktv.app.domain.usecase.ObserveFavoriteMoviesUseCase
import com.sirktv.app.domain.usecase.ObserveFavoriteSeriesUseCase
import com.sirktv.app.domain.usecase.ObserveRecentlyWatchedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
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

/** One entry in the Home hero's "My Favorites" row — mixed channels/movies/series in a single shelf. */
sealed interface HomeFavoriteItem {
    data class ChannelItem(val channel: Channel) : HomeFavoriteItem
    data class MovieItem(val movie: Movie) : HomeFavoriteItem
    data class SeriesItem(val series: Series) : HomeFavoriteItem
}

/**
 * Continue Watching / My Favorites / Recently Watched are pure Room reads —
 * nothing here ever calls the Xtream API, so every row is safe to observe
 * immediately and render the instant Home appears, with no spinner and no
 * sync. If Room has nothing yet for a given row, that row's list is simply
 * empty and the row doesn't render at all (see [hasAnyContent]).
 */
data class HomeUiState(
    val continueWatching: List<WatchProgress> = emptyList(),
    val recentlyWatched: List<WatchProgress> = emptyList(),
    val favoriteChannels: List<Channel> = emptyList(),
    val favoriteMovies: List<Movie> = emptyList(),
    val favoriteSeries: List<Series> = emptyList(),
    val channelEpgCache: Map<String, EpgNowNext> = emptyMap()
) {
    val favorites: List<HomeFavoriteItem>
        get() = favoriteChannels.map(HomeFavoriteItem::ChannelItem) +
            favoriteMovies.map(HomeFavoriteItem::MovieItem) +
            favoriteSeries.map(HomeFavoriteItem::SeriesItem)

    val hasAnyContent: Boolean
        get() = continueWatching.isNotEmpty() || favorites.isNotEmpty() || recentlyWatched.isNotEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeContinueWatchingUseCase: ObserveContinueWatchingUseCase,
    observeRecentlyWatchedUseCase: ObserveRecentlyWatchedUseCase,
    observeFavoriteChannelsUseCase: ObserveFavoriteChannelsUseCase,
    observeFavoriteMoviesUseCase: ObserveFavoriteMoviesUseCase,
    observeFavoriteSeriesUseCase: ObserveFavoriteSeriesUseCase,
    private val getEpgNowNextUseCase: GetEpgNowNextUseCase,
    private val clearSavedCredentialsUseCase: ClearSavedCredentialsUseCase,
    private val currentSession: CurrentSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val displayName: StateFlow<String?> = currentSession.greetingName

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private val requestedEpgChannelIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            combine(
                observeContinueWatchingUseCase(),
                observeRecentlyWatchedUseCase(),
                observeFavoriteChannelsUseCase(),
                observeFavoriteMoviesUseCase(),
                observeFavoriteSeriesUseCase()
            ) { continueWatching, recentlyWatched, channels, movies, series ->
                HomeUiState(
                    continueWatching = continueWatching,
                    recentlyWatched = recentlyWatched,
                    favoriteChannels = channels,
                    favoriteMovies = movies,
                    favoriteSeries = series
                )
            }.collect { state -> _uiState.update { state.copy(channelEpgCache = it.channelEpgCache) } }
        }
    }

    /** Same lazy-on-visible EPG pattern as Live TV/Favorites — populates now/next for favorite channel cards. */
    fun requestEpgFor(channelId: String) {
        if (!requestedEpgChannelIds.add(channelId)) return
        viewModelScope.launch {
            val nowNext = runCatching { getEpgNowNextUseCase(channelId) }
                .getOrDefault(EpgNowNext(now = null, next = null))
            _uiState.update { it.copy(channelEpgCache = it.channelEpgCache + (channelId to nowNext)) }
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

    fun favoriteTarget(item: HomeFavoriteItem): HomeNavTarget = when (item) {
        is HomeFavoriteItem.ChannelItem -> HomeNavTarget.LiveTv(item.channel.id)
        is HomeFavoriteItem.MovieItem -> HomeNavTarget.MoviePlayer(item.movie.id)
        is HomeFavoriteItem.SeriesItem -> HomeNavTarget.SeriesDetail(item.series.id)
    }
}
