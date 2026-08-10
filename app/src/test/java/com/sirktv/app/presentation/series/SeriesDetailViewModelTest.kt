package com.sirktv.app.presentation.series

import androidx.lifecycle.SavedStateHandle
import com.sirktv.app.domain.model.Episode
import com.sirktv.app.domain.model.SeasonInfo
import com.sirktv.app.domain.model.SeriesInfo
import com.sirktv.app.domain.usecase.GetSeriesInfoUseCase
import com.sirktv.app.domain.usecase.ObserveSeriesUseCase
import com.sirktv.app.domain.usecase.ToggleSeriesFavoriteUseCase
import com.sirktv.app.testutil.FakeSeriesRepository
import com.sirktv.app.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SeriesDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun episode(season: Int, number: Int) = Episode(
        id = "s1:$season:$number",
        seriesId = "s1",
        seasonNumber = season,
        episodeNumber = number,
        title = "Episode $number",
        durationMinutes = 42,
        containerExtension = "mp4",
        synopsis = null
    )

    @Test
    fun `season tabs return correct episodes`() = runTest {
        val repo = FakeSeriesRepository().apply {
            seriesInfo = SeriesInfo(
                seasons = listOf(
                    SeasonInfo(seasonNumber = 1, episodes = listOf(episode(1, 1), episode(1, 2))),
                    SeasonInfo(seasonNumber = 2, episodes = listOf(episode(2, 1)))
                )
            )
        }
        val viewModel = SeriesDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("seriesId" to "s1")),
            observeSeriesUseCase = ObserveSeriesUseCase(repo),
            getSeriesInfoUseCase = GetSeriesInfoUseCase(repo),
            toggleSeriesFavoriteUseCase = ToggleSeriesFavoriteUseCase(repo)
        )

        // First season is selected by default.
        assertEquals(1, viewModel.uiState.value.selectedSeasonNumber)
        assertEquals(2, viewModel.uiState.value.selectedSeasonEpisodes.size)

        viewModel.onSeasonSelected(2)

        assertEquals(2, viewModel.uiState.value.selectedSeasonNumber)
        assertEquals(1, viewModel.uiState.value.selectedSeasonEpisodes.size)
        assertEquals("s1:2:1", viewModel.uiState.value.selectedSeasonEpisodes.first().id)
    }
}
