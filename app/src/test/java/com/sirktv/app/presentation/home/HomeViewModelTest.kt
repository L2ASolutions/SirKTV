package com.sirktv.app.presentation.home

import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.WatchProgress
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.domain.usecase.ClearSavedCredentialsUseCase
import com.sirktv.app.domain.usecase.GetEpgNowNextUseCase
import com.sirktv.app.domain.usecase.ObserveContinueWatchingUseCase
import com.sirktv.app.domain.usecase.ObserveFavoriteChannelsUseCase
import com.sirktv.app.domain.usecase.ObserveFavoriteMoviesUseCase
import com.sirktv.app.domain.usecase.ObserveFavoriteSeriesUseCase
import com.sirktv.app.domain.usecase.ObserveRecentlyWatchedUseCase
import com.sirktv.app.testutil.FakeAuthRepository
import com.sirktv.app.testutil.FakeChannelRepository
import com.sirktv.app.testutil.FakeDisplayNameRepository
import com.sirktv.app.testutil.FakeMovieRepository
import com.sirktv.app.testutil.FakeSeriesRepository
import com.sirktv.app.testutil.FakeWatchProgressRepository
import com.sirktv.app.testutil.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Verifies Home reads exclusively from Room-backed repositories: every
 * dependency wired into [HomeViewModel] here is a Flow-observing use case
 * (ObserveContinueWatching/RecentlyWatched/FavoriteX) — there is no sync/API
 * use case injected at all, so "no API calls" is a structural guarantee this
 * test exercises by never touching a network-shaped fake.
 */
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun buildViewModel(
        watchProgressRepo: FakeWatchProgressRepository = FakeWatchProgressRepository(),
        channelRepo: FakeChannelRepository = FakeChannelRepository(),
        movieRepo: FakeMovieRepository = FakeMovieRepository(),
        seriesRepo: FakeSeriesRepository = FakeSeriesRepository()
    ): HomeViewModel {
        val appScope = CoroutineScope(StandardTestDispatcher())
        val currentSession = CurrentSession(FakeDisplayNameRepository("Kay"), appScope)
        return HomeViewModel(
            observeContinueWatchingUseCase = ObserveContinueWatchingUseCase(watchProgressRepo),
            observeRecentlyWatchedUseCase = ObserveRecentlyWatchedUseCase(watchProgressRepo),
            observeFavoriteChannelsUseCase = ObserveFavoriteChannelsUseCase(channelRepo),
            observeFavoriteMoviesUseCase = ObserveFavoriteMoviesUseCase(movieRepo),
            observeFavoriteSeriesUseCase = ObserveFavoriteSeriesUseCase(seriesRepo),
            getEpgNowNextUseCase = GetEpgNowNextUseCase(channelRepo),
            clearSavedCredentialsUseCase = ClearSavedCredentialsUseCase(FakeAuthRepository()),
            currentSession = currentSession
        )
    }

    private fun progress(id: String, type: ContentType = ContentType.MOVIE) = WatchProgress(
        contentId = id,
        contentType = type,
        title = "Title $id",
        subtitle = null,
        imageUrl = null,
        positionMs = 1000,
        durationMs = 2000,
        updatedAtEpochMillis = 0L
    )

    @Test
    fun `home loads only from Room with no API calls`() = runTest {
        val movieRepo = FakeMovieRepository(movies = emptyList())
        val viewModel = buildViewModel(movieRepo = movieRepo)

        // No sync-shaped dependency exists on HomeViewModel at all — the only
        // possible signal a real API call happened on this fake would be its
        // sync counter incrementing, and nothing in HomeViewModel can reach it.
        assertEquals(0, movieRepo.syncCallCount)
        assertTrue(viewModel.uiState.value.continueWatching.isEmpty())
    }

    @Test
    fun `continue watching hidden when empty`() = runTest {
        val watchProgressRepo = FakeWatchProgressRepository(continueWatching = emptyList())
        val viewModel = buildViewModel(watchProgressRepo = watchProgressRepo)

        assertTrue(viewModel.uiState.value.continueWatching.isEmpty())
        assertFalse(viewModel.uiState.value.hasAnyContent)
    }

    @Test
    fun `recently watched hidden when empty`() = runTest {
        val watchProgressRepo = FakeWatchProgressRepository(recentlyWatched = emptyList())
        val viewModel = buildViewModel(watchProgressRepo = watchProgressRepo)

        assertTrue(viewModel.uiState.value.recentlyWatched.isEmpty())
    }

    @Test
    fun `rows populate from Room when data exists`() = runTest {
        val watchProgressRepo = FakeWatchProgressRepository(
            continueWatching = listOf(progress("m1")),
            recentlyWatched = listOf(progress("m2"))
        )
        val viewModel = buildViewModel(watchProgressRepo = watchProgressRepo)

        assertEquals(1, viewModel.uiState.value.continueWatching.size)
        assertEquals(1, viewModel.uiState.value.recentlyWatched.size)
        assertTrue(viewModel.uiState.value.hasAnyContent)
    }
}
