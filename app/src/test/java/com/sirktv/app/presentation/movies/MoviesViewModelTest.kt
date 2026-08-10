package com.sirktv.app.presentation.movies

import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.usecase.GetCachedMoviesUseCase
import com.sirktv.app.domain.usecase.ObserveContinueWatchingUseCase
import com.sirktv.app.domain.usecase.ObserveMovieCategoriesUseCase
import com.sirktv.app.domain.usecase.ObserveMoviesUseCase
import com.sirktv.app.domain.usecase.SyncMoviesUseCase
import com.sirktv.app.domain.usecase.ToggleMovieFavoriteUseCase
import com.sirktv.app.testutil.FakeMovieRepository
import com.sirktv.app.testutil.FakeWatchProgressRepository
import com.sirktv.app.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MoviesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun movie(
        id: String,
        categoryId: String = "cat1",
        addedAtEpochMillis: Long = 0L
    ) = Movie(
        id = id,
        title = "Movie $id",
        posterUrl = null,
        categoryId = categoryId,
        rating = null,
        containerExtension = "mp4",
        isFavorite = false,
        addedAtEpochMillis = addedAtEpochMillis
    )

    private fun buildViewModel(repo: FakeMovieRepository): MoviesViewModel = MoviesViewModel(
        observeMovieCategoriesUseCase = ObserveMovieCategoriesUseCase(repo),
        observeMoviesUseCase = ObserveMoviesUseCase(repo),
        observeContinueWatchingUseCase = ObserveContinueWatchingUseCase(FakeWatchProgressRepository()),
        syncMoviesUseCase = SyncMoviesUseCase(repo),
        getCachedMoviesUseCase = GetCachedMoviesUseCase(repo),
        toggleMovieFavoriteUseCase = ToggleMovieFavoriteUseCase(repo)
    )

    @Test
    fun `movies load from repository on entry`() = runTest {
        val repo = FakeMovieRepository(movies = listOf(movie("m1"), movie("m2")))
        val viewModel = buildViewModel(repo)

        // init() calls refresh(), which syncs before Room's own flow is
        // trusted as authoritative — the sync call itself is the entry hook.
        assertEquals(1, repo.syncCallCount)
        assertEquals(2, viewModel.uiState.value.allMovies.size)
    }

    @Test
    fun `genre filter updates displayed movies`() = runTest {
        val repo = FakeMovieRepository(
            movies = listOf(movie("m1", categoryId = "action"), movie("m2", categoryId = "comedy"))
        )
        val viewModel = buildViewModel(repo)

        viewModel.onCategorySelected("action")

        assertEquals(1, viewModel.uiState.value.visibleMovies.size)
        assertEquals("m1", viewModel.uiState.value.visibleMovies.first().id)

        viewModel.onCategorySelected(null)
        assertEquals(2, viewModel.uiState.value.visibleMovies.size)
    }

    @Test
    fun `recently added sorted by added date descending`() = runTest {
        val repo = FakeMovieRepository(
            movies = listOf(
                movie("old", addedAtEpochMillis = 1_000L),
                movie("new", addedAtEpochMillis = 3_000L),
                movie("mid", addedAtEpochMillis = 2_000L)
            )
        )
        val viewModel = buildViewModel(repo)

        val ranked = viewModel.uiState.value.recentlyAddedMovies.map { it.id }
        assertEquals(listOf("new", "mid", "old"), ranked)
    }

    @Test
    fun `favorite toggle persists to Room`() = runTest {
        val repo = FakeMovieRepository(movies = listOf(movie("m1")))
        val viewModel = buildViewModel(repo)

        viewModel.onToggleFavorite("m1")

        assertTrue(repo.toggledFavoriteIds.contains("m1"))
    }
}
