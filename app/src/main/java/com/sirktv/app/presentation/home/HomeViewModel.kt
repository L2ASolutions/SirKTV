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
import com.sirktv.app.domain.usecase.GetMovieDetailUseCase
import com.sirktv.app.domain.usecase.ObserveChannelsUseCase
import com.sirktv.app.domain.usecase.ObserveContinueWatchingUseCase
import com.sirktv.app.domain.usecase.ObserveMovieCategoriesUseCase
import com.sirktv.app.domain.usecase.ObserveMoviesUseCase
import com.sirktv.app.domain.usecase.ObserveRecentlyWatchedUseCase
import com.sirktv.app.domain.usecase.ObserveSeriesCategoriesUseCase
import com.sirktv.app.domain.usecase.ObserveSeriesUseCase
import com.sirktv.app.domain.usecase.SyncChannelsUseCase
import com.sirktv.app.domain.usecase.SyncMoviesUseCase
import com.sirktv.app.domain.usecase.SyncSeriesUseCase
import com.sirktv.app.domain.usecase.ToggleFavoriteUseCase
import com.sirktv.app.domain.usecase.ToggleMovieFavoriteUseCase
import com.sirktv.app.domain.usecase.ToggleSeriesFavoriteUseCase
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

private const val LIVE_TV_ROW_LIMIT = 25
private const val RECENTLY_ADDED_MOVIE_LIMIT = 14
private const val RECENTLY_ADDED_SERIES_LIMIT = 8
private const val HERO_ITEM_LIMIT = 6

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
 * One rotating slide in the Home hero carousel — sourced from top-rated
 * movies and series. [contentId] is the raw movie/series id (no prefix), used
 * to key the lazy movie-synopsis cache; [id] is the list key.
 */
data class HeroItem(
    val id: String,
    val contentId: String,
    val title: String,
    val imageUrl: String?,
    val genre: String?,
    val rating: Float?,
    val isFavorite: Boolean,
    val synopsis: String?,
    val contentType: ContentType,
    val navTarget: HomeNavTarget
)

/** One tile in the Home "Recently Added" row — movies (real add time) and series (catalog order) merged. */
data class RecentlyAddedItem(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val rating: Float?,
    val isFavorite: Boolean,
    val navTarget: HomeNavTarget
)

data class HomeUiState(
    val heroItems: List<HeroItem> = emptyList(),
    val continueWatching: List<WatchProgress> = emptyList(),
    val recentlyAdded: List<RecentlyAddedItem> = emptyList(),
    val recentlyWatched: List<WatchProgress> = emptyList(),
    val liveChannels: List<Channel> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeContinueWatchingUseCase: ObserveContinueWatchingUseCase,
    observeChannelsUseCase: ObserveChannelsUseCase,
    observeRecentlyWatchedUseCase: ObserveRecentlyWatchedUseCase,
    observeMoviesUseCase: ObserveMoviesUseCase,
    observeSeriesUseCase: ObserveSeriesUseCase,
    observeMovieCategoriesUseCase: ObserveMovieCategoriesUseCase,
    observeSeriesCategoriesUseCase: ObserveSeriesCategoriesUseCase,
    private val getEpgNowNextUseCase: GetEpgNowNextUseCase,
    private val getMovieDetailUseCase: GetMovieDetailUseCase,
    private val syncChannelsUseCase: SyncChannelsUseCase,
    private val syncMoviesUseCase: SyncMoviesUseCase,
    private val syncSeriesUseCase: SyncSeriesUseCase,
    private val toggleChannelFavoriteUseCase: ToggleFavoriteUseCase,
    private val toggleMovieFavoriteUseCase: ToggleMovieFavoriteUseCase,
    private val toggleSeriesFavoriteUseCase: ToggleSeriesFavoriteUseCase,
    private val clearSavedCredentialsUseCase: ClearSavedCredentialsUseCase,
    private val currentSession: CurrentSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    // Lazy-on-visible EPG now/next for the Live TV row — same pattern as
    // LiveTvPlayerViewModel/LiveTvBrowseViewModel.
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

    // Lazy-on-open synopsis for a movie hero item in the Preview modal — Movie
    // has no inline synopsis (unlike Series), so it's fetched only when the
    // modal actually needs it, same "on demand" contract GetMovieDetailUseCase
    // already documents.
    private val _movieSynopsisCache = MutableStateFlow<Map<String, String?>>(emptyMap())
    val movieSynopsisCache: StateFlow<Map<String, String?>> = _movieSynopsisCache.asStateFlow()
    private val requestedMovieSynopsisIds = mutableSetOf<String>()

    fun requestMovieSynopsis(movieId: String) {
        if (!requestedMovieSynopsisIds.add(movieId)) return
        viewModelScope.launch {
            val detail = getMovieDetailUseCase(movieId)
            _movieSynopsisCache.update { it + (movieId to detail?.synopsis) }
        }
    }

    init {
        // Best-effort background refresh so Home has real data even if the
        // user never separately opens Live TV/Movies/Series first.
        refresh()

        val catalogFlow = combine(
            observeMoviesUseCase(),
            observeSeriesUseCase(),
            observeMovieCategoriesUseCase(),
            observeSeriesCategoriesUseCase()
        ) { movies, series, movieCategories, seriesCategories ->
            CatalogQuad(movies, series, movieCategories.associate { it.id to it.name }, seriesCategories.associate { it.id to it.name })
        }
        val watchFlow = combine(
            observeContinueWatchingUseCase(),
            observeChannelsUseCase(),
            observeRecentlyWatchedUseCase()
        ) { continueWatching, channels, recent ->
            WatchTriple(continueWatching, channels.take(LIVE_TV_ROW_LIMIT), recent)
        }

        viewModelScope.launch {
            combine(watchFlow, catalogFlow) { watch, catalog ->
                HomeUiState(
                    heroItems = buildHeroItems(catalog),
                    continueWatching = watch.continueWatching,
                    recentlyAdded = buildRecentlyAdded(catalog),
                    recentlyWatched = watch.recentlyWatched,
                    liveChannels = watch.liveChannels
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun refresh() {
        viewModelScope.launch { syncChannelsUseCase() }
        viewModelScope.launch { syncMoviesUseCase() }
        viewModelScope.launch { syncSeriesUseCase() }
    }

    private fun buildHeroItems(catalog: CatalogQuad): List<HeroItem> {
        val movieHeroes = catalog.movies
            .sortedByDescending { it.rating ?: 0f }
            .take(HERO_ITEM_LIMIT)
            .map { movie ->
                HeroItem(
                    id = "movie-${movie.id}",
                    contentId = movie.id,
                    title = movie.title,
                    imageUrl = movie.posterUrl,
                    genre = catalog.movieCategoryNames[movie.categoryId],
                    rating = movie.rating,
                    isFavorite = movie.isFavorite,
                    synopsis = null,
                    contentType = ContentType.MOVIE,
                    navTarget = HomeNavTarget.MoviePlayer(movie.id)
                )
            }
        val seriesHeroes = catalog.series
            .sortedByDescending { it.rating ?: 0f }
            .take(HERO_ITEM_LIMIT)
            .map { series ->
                HeroItem(
                    id = "series-${series.id}",
                    contentId = series.id,
                    title = series.title,
                    imageUrl = series.posterUrl,
                    genre = catalog.seriesCategoryNames[series.categoryId],
                    rating = series.rating,
                    isFavorite = series.isFavorite,
                    synopsis = series.synopsis,
                    contentType = ContentType.SERIES,
                    navTarget = HomeNavTarget.SeriesDetail(series.id)
                )
            }
        return (movieHeroes + seriesHeroes)
            .sortedByDescending { it.rating ?: 0f }
            .take(HERO_ITEM_LIMIT)
    }

    private fun buildRecentlyAdded(catalog: CatalogQuad): List<RecentlyAddedItem> {
        val movies = catalog.movies
            .sortedByDescending { it.addedAtEpochMillis }
            .take(RECENTLY_ADDED_MOVIE_LIMIT)
            .map { movie ->
                RecentlyAddedItem(
                    id = "movie-${movie.id}",
                    title = movie.title,
                    imageUrl = movie.posterUrl,
                    rating = movie.rating,
                    isFavorite = movie.isFavorite,
                    navTarget = HomeNavTarget.MoviePlayer(movie.id)
                )
            }
        // Series has no add-time column in the local schema, so it's ranked by
        // catalog order (the order the provider itself returns it in) rather
        // than a fabricated timestamp.
        val series = catalog.series
            .take(RECENTLY_ADDED_SERIES_LIMIT)
            .map { series ->
                RecentlyAddedItem(
                    id = "series-${series.id}",
                    title = series.title,
                    imageUrl = series.posterUrl,
                    rating = series.rating,
                    isFavorite = series.isFavorite,
                    navTarget = HomeNavTarget.SeriesDetail(series.id)
                )
            }
        return movies + series
    }

    fun onLogoutClicked() {
        viewModelScope.launch {
            clearSavedCredentialsUseCase()
            currentSession.clear()
            _events.emit(HomeEvent.NavigateToLogin)
        }
    }

    fun onToggleChannelFavorite(channelId: String) {
        viewModelScope.launch { toggleChannelFavoriteUseCase(channelId) }
    }

    fun onToggleMovieFavorite(movieId: String) {
        viewModelScope.launch { toggleMovieFavoriteUseCase(movieId) }
    }

    fun onToggleSeriesFavorite(seriesId: String) {
        viewModelScope.launch { toggleSeriesFavoriteUseCase(seriesId) }
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

private data class WatchTriple(
    val continueWatching: List<WatchProgress>,
    val liveChannels: List<Channel>,
    val recentlyWatched: List<WatchProgress>
)

private data class CatalogQuad(
    val movies: List<Movie>,
    val series: List<Series>,
    val movieCategoryNames: Map<String, String>,
    val seriesCategoryNames: Map<String, String>
)
