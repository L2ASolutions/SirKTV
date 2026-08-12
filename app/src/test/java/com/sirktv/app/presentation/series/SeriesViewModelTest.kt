package com.sirktv.app.presentation.series

import com.sirktv.app.domain.model.Series
import com.sirktv.app.domain.usecase.GetCachedSeriesUseCase
import com.sirktv.app.domain.usecase.ObserveContinueWatchingUseCase
import com.sirktv.app.domain.usecase.ObserveSeriesCategoriesUseCase
import com.sirktv.app.domain.usecase.ObserveSeriesUseCase
import com.sirktv.app.domain.usecase.SyncSeriesUseCase
import com.sirktv.app.domain.usecase.ToggleSeriesFavoriteUseCase
import com.sirktv.app.testutil.FakeSeriesRepository
import com.sirktv.app.testutil.FakeWatchProgressRepository
import com.sirktv.app.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SeriesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun series(id: String, categoryId: String = "cat1") = Series(
        id = id,
        title = "Series $id",
        posterUrl = null,
        categoryId = categoryId,
        rating = null,
        cast = emptyList(),
        director = null,
        synopsis = null,
        isFavorite = false,
        lastModifiedEpochMillis = 0L
    )

    private fun buildViewModel(repo: FakeSeriesRepository): SeriesViewModel = SeriesViewModel(
        observeSeriesCategoriesUseCase = ObserveSeriesCategoriesUseCase(repo),
        observeSeriesUseCase = ObserveSeriesUseCase(repo),
        observeContinueWatchingUseCase = ObserveContinueWatchingUseCase(FakeWatchProgressRepository()),
        syncSeriesUseCase = SyncSeriesUseCase(repo),
        getCachedSeriesUseCase = GetCachedSeriesUseCase(repo),
        toggleSeriesFavoriteUseCase = ToggleSeriesFavoriteUseCase(repo)
    )

    @Test
    fun `series load on entry`() = runTest {
        val repo = FakeSeriesRepository(series = listOf(series("s1"), series("s2")))
        val viewModel = buildViewModel(repo)

        assertEquals(1, repo.syncCallCount)
        assertEquals(2, viewModel.uiState.value.allSeries.size)
        assertEquals(2, viewModel.uiState.value.visibleSeries.size)
    }

    @Test
    fun `category filter narrows visible series`() = runTest {
        val repo = FakeSeriesRepository(
            series = listOf(series("s1", categoryId = "drama"), series("s2", categoryId = "comedy"))
        )
        val viewModel = buildViewModel(repo)

        viewModel.onCategorySelected("drama")

        assertEquals(1, viewModel.uiState.value.visibleSeries.size)
        assertEquals("s1", viewModel.uiState.value.visibleSeries.first().id)
    }

    @Test
    fun `favorite toggle persists to Room`() = runTest {
        val repo = FakeSeriesRepository(series = listOf(series("s1")))
        val viewModel = buildViewModel(repo)

        viewModel.onToggleFavorite("s1")

        assertTrue(repo.toggledFavoriteIds.contains("s1"))
    }
}
