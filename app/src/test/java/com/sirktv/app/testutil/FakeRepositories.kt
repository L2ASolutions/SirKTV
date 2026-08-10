package com.sirktv.app.testutil

import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.model.EpgProgram
import com.sirktv.app.domain.model.FavoriteItem
import com.sirktv.app.domain.model.FavoriteSort
import com.sirktv.app.domain.model.LoginResult
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.model.MovieDetail
import com.sirktv.app.domain.model.SavedCredentials
import com.sirktv.app.domain.model.Series
import com.sirktv.app.domain.model.SeriesInfo
import com.sirktv.app.domain.model.StartupPreference
import com.sirktv.app.domain.model.WatchProgress
import com.sirktv.app.domain.repository.AuthRepository
import com.sirktv.app.domain.repository.ChannelRepository
import com.sirktv.app.domain.repository.DisplayNameRepository
import com.sirktv.app.domain.repository.FavoritesRepository
import com.sirktv.app.domain.repository.MovieRepository
import com.sirktv.app.domain.repository.SeriesRepository
import com.sirktv.app.domain.repository.StartupPreferenceRepository
import com.sirktv.app.domain.repository.WatchProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Records which content ids were passed to toggleFavorite — lets a test assert the ViewModel called through with the right id. */
class FakeChannelRepository(
    categories: List<Category> = emptyList(),
    channels: List<Channel> = emptyList()
) : ChannelRepository {
    val categoriesFlow = MutableStateFlow(categories)
    val channelsFlow = MutableStateFlow(channels)
    val favoriteChannelsFlow = MutableStateFlow<List<Channel>>(emptyList())
    var syncCallCount = 0
    var syncResult: Result<Unit> = Result.success(Unit)
    val toggledFavoriteIds = mutableListOf<String>()
    var epgNowNext: EpgNowNext = EpgNowNext(now = null, next = null)
    var epgListings: List<EpgProgram> = emptyList()

    override fun observeCategories(): Flow<List<Category>> = categoriesFlow
    override fun observeChannels(categoryId: String?): Flow<List<Channel>> = channelsFlow
    override fun observeFavoriteChannels(): Flow<List<Channel>> = favoriteChannelsFlow
    override suspend fun getCachedChannels(): List<Channel> = channelsFlow.value
    override suspend fun getFavoriteChannelIds(): List<String> = favoriteChannelsFlow.value.map { it.id }
    override suspend fun toggleFavorite(channelId: String) {
        toggledFavoriteIds.add(channelId)
    }
    override suspend fun sync(): Result<Unit> {
        syncCallCount++
        return syncResult
    }
    override suspend fun getEpgNowNext(channelId: String): EpgNowNext = epgNowNext
    override suspend fun getEpgListings(channelId: String, limit: Int): List<EpgProgram> = epgListings
}

class FakeMovieRepository(
    categories: List<Category> = emptyList(),
    movies: List<Movie> = emptyList()
) : MovieRepository {
    val categoriesFlow = MutableStateFlow(categories)
    val moviesFlow = MutableStateFlow(movies)
    val favoriteMoviesFlow = MutableStateFlow<List<Movie>>(emptyList())
    var syncCallCount = 0
    var syncResult: Result<Unit> = Result.success(Unit)
    val toggledFavoriteIds = mutableListOf<String>()

    override fun observeCategories(): Flow<List<Category>> = categoriesFlow
    override fun observeMovies(categoryId: String?): Flow<List<Movie>> = moviesFlow
    override fun observeFavoriteMovies(): Flow<List<Movie>> = favoriteMoviesFlow
    override suspend fun getCachedMovies(): List<Movie> = moviesFlow.value
    override suspend fun toggleFavorite(movieId: String) {
        toggledFavoriteIds.add(movieId)
    }
    override suspend fun sync(): Result<Unit> {
        syncCallCount++
        return syncResult
    }
    override suspend fun getMovieDetail(movieId: String): MovieDetail? = null
}

class FakeSeriesRepository(
    categories: List<Category> = emptyList(),
    series: List<Series> = emptyList()
) : SeriesRepository {
    val categoriesFlow = MutableStateFlow(categories)
    val seriesFlow = MutableStateFlow(series)
    val favoriteSeriesFlow = MutableStateFlow<List<Series>>(emptyList())
    var syncCallCount = 0
    var syncResult: Result<Unit> = Result.success(Unit)
    val toggledFavoriteIds = mutableListOf<String>()
    var seriesInfo: SeriesInfo? = null

    override fun observeCategories(): Flow<List<Category>> = categoriesFlow
    override fun observeSeries(categoryId: String?): Flow<List<Series>> = seriesFlow
    override fun observeFavoriteSeries(): Flow<List<Series>> = favoriteSeriesFlow
    override suspend fun getCachedSeries(): List<Series> = seriesFlow.value
    override suspend fun toggleFavorite(seriesId: String) {
        toggledFavoriteIds.add(seriesId)
    }
    override suspend fun sync(): Result<Unit> {
        syncCallCount++
        return syncResult
    }
    override suspend fun getSeriesInfo(seriesId: String): SeriesInfo? = seriesInfo
}

class FakeWatchProgressRepository(
    continueWatching: List<WatchProgress> = emptyList(),
    recentlyWatched: List<WatchProgress> = emptyList()
) : WatchProgressRepository {
    val continueWatchingFlow = MutableStateFlow(continueWatching)
    val recentlyWatchedFlow = MutableStateFlow(recentlyWatched)
    val upserted = mutableListOf<WatchProgress>()

    override fun observeContinueWatching(): Flow<List<WatchProgress>> = continueWatchingFlow
    override fun observeRecentlyWatched(): Flow<List<WatchProgress>> = recentlyWatchedFlow
    override suspend fun getProgress(contentId: String): WatchProgress? =
        (continueWatchingFlow.value + recentlyWatchedFlow.value).find { it.contentId == contentId }
    override suspend fun upsert(progress: WatchProgress) {
        upserted.add(progress)
    }
    override suspend fun remove(contentId: String) = Unit
    override suspend fun clearAll() = Unit
}

class FakeFavoritesRepository : FavoritesRepository {
    var items: List<FavoriteItem> = emptyList()
    override fun observe(contentType: ContentType, sort: FavoriteSort): Flow<List<FavoriteItem>> =
        MutableStateFlow(items.filter { it.contentType == contentType }).asStateFlow()
    override suspend fun moveUp(contentId: String, contentType: ContentType) = Unit
    override suspend fun moveDown(contentId: String, contentType: ContentType) = Unit
}

class FakeDisplayNameRepository(initial: String? = null) : DisplayNameRepository {
    private val flow = MutableStateFlow(initial)
    override fun observe(): Flow<String?> = flow
    override suspend fun get(): String? = flow.value
    override suspend fun set(displayName: String?) {
        flow.value = displayName
    }
}

class FakeStartupPreferenceRepository(initial: StartupPreference = StartupPreference()) : StartupPreferenceRepository {
    private val flow = MutableStateFlow(initial)
    override fun observe(): Flow<StartupPreference> = flow
    override suspend fun get(): StartupPreference = flow.value
    override suspend fun update(preference: StartupPreference) {
        flow.value = preference
    }
    override suspend fun setLastWatchedChannel(channelId: String) {
        flow.value = flow.value.copy(lastWatchedChannelId = channelId)
    }
}

class FakeAuthRepository : AuthRepository {
    var savedCredentials: SavedCredentials? = null
    var clearCallCount = 0
    override suspend fun login(serverUrl: String, username: String, password: String, rememberMe: Boolean): LoginResult =
        throw NotImplementedError("not needed by these tests")
    override suspend fun getSavedCredentials(): SavedCredentials? = savedCredentials
    override suspend fun clearSavedCredentials() {
        clearCallCount++
        savedCredentials = null
    }
}
