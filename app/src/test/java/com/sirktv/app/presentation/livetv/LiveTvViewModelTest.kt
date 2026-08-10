package com.sirktv.app.presentation.livetv

import android.content.Context
import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.domain.usecase.GetEpgListingsUseCase
import com.sirktv.app.domain.usecase.GetEpgNowNextUseCase
import com.sirktv.app.domain.usecase.GetStartupPreferenceUseCase
import com.sirktv.app.domain.usecase.ObserveCategoriesUseCase
import com.sirktv.app.domain.usecase.ObserveChannelsUseCase
import com.sirktv.app.domain.usecase.SyncChannelsUseCase
import com.sirktv.app.domain.usecase.ToggleFavoriteUseCase
import com.sirktv.app.player.PlaybackState
import com.sirktv.app.testutil.FakeChannelRepository
import com.sirktv.app.testutil.FakeDisplayNameRepository
import com.sirktv.app.testutil.FakeStartupPreferenceRepository
import com.sirktv.app.testutil.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class LiveTvViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun channel(id: String, categoryId: String = "cat1") = Channel(
        id = id,
        name = "Channel $id",
        logoUrl = null,
        categoryId = categoryId,
        channelNumber = id.toIntOrNull() ?: 0,
        isFavorite = false
    )

    private fun buildViewModel(repo: FakeChannelRepository): LiveTvBrowseViewModel {
        // Context/OkHttpClient are only ever touched inside initMiniPlayer()/
        // loadPreview() — neither of which these tests exercise (loadPreview
        // itself no-ops with no signed-in credentials, matching production
        // behavior when currentSession.credentials is null). A mocked Context
        // is enough to satisfy the constructor without ever invoking a method
        // on it; OkHttpClient is a real, harmless instance — it never makes a
        // request in these tests.
        val context = Mockito.mock(Context::class.java)
        val appScope = CoroutineScope(StandardTestDispatcher())
        val currentSession = CurrentSession(FakeDisplayNameRepository(), appScope)
        return LiveTvBrowseViewModel(
            context = context,
            okHttpClient = OkHttpClient(),
            currentSession = currentSession,
            observeCategoriesUseCase = ObserveCategoriesUseCase(repo),
            observeChannelsUseCase = ObserveChannelsUseCase(repo),
            syncChannelsUseCase = SyncChannelsUseCase(repo),
            getEpgNowNextUseCase = GetEpgNowNextUseCase(repo),
            getEpgListingsUseCase = GetEpgListingsUseCase(repo),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(repo),
            getStartupPreferenceUseCase = GetStartupPreferenceUseCase(FakeStartupPreferenceRepository())
        )
    }

    @Test
    fun `categories load successfully from repository`() = runTest {
        val repo = FakeChannelRepository(categories = listOf(Category("c1", "News"), Category("c2", "Sports")))
        val viewModel = buildViewModel(repo)

        assertEquals(2, viewModel.uiState.value.categories.size)
        assertEquals("News", viewModel.uiState.value.categories.first().name)
    }

    @Test
    fun `channel list filters correctly by category`() = runTest {
        val repo = FakeChannelRepository(
            categories = listOf(Category("news", "News"), Category("sports", "Sports")),
            channels = listOf(channel("1", categoryId = "news"), channel("2", categoryId = "sports"))
        )
        val viewModel = buildViewModel(repo)

        viewModel.onCategorySelected("sports")

        assertEquals(1, viewModel.uiState.value.visibleChannels.size)
        assertEquals("2", viewModel.uiState.value.visibleChannels.first().id)

        viewModel.onCategorySelected(null)
        assertEquals(2, viewModel.uiState.value.visibleChannels.size)
    }

    @Test
    fun `channel selection updates selected channel state`() = runTest {
        val repo = FakeChannelRepository(channels = listOf(channel("1"), channel("2")))
        val viewModel = buildViewModel(repo)

        viewModel.onChannelHighlighted("2")

        assertEquals("2", viewModel.uiState.value.selectedChannelId)
    }

    @Test
    fun `mini player state resets on release`() = runTest {
        val repo = FakeChannelRepository(channels = listOf(channel("1")))
        val viewModel = buildViewModel(repo)

        // No mini player was ever built (it's only built lazily on the first
        // OK press — see onChannelActivated) — release must still be a
        // harmless no-op that leaves the state machine in Idle, exactly as
        // it would after a real ExoPlayer teardown.
        viewModel.releaseMiniPlayer()

        assertEquals(PlaybackState.Idle, viewModel.miniPlayerState.value)
        assertTrue(viewModel.miniPlayer.value == null)
    }

    @Test
    fun `channel activation sets the active channel id`() = runTest {
        val repo = FakeChannelRepository(channels = listOf(channel("1"), channel("2")))
        val viewModel = buildViewModel(repo)

        // No signed-in credentials on this session — loadMiniPlayer no-ops
        // before ever touching the mocked Context/ExoPlayer, exactly as it
        // does in production when currentSession.credentials is null.
        viewModel.onChannelActivated("2")

        assertEquals("2", viewModel.uiState.value.activeChannelId)
    }

    @Test
    fun `dismissing the mini player clears the active channel`() = runTest {
        val repo = FakeChannelRepository(channels = listOf(channel("1")))
        val viewModel = buildViewModel(repo)

        viewModel.onChannelActivated("1")
        viewModel.dismissMiniPlayer()

        assertEquals(null, viewModel.uiState.value.activeChannelId)
        assertEquals(PlaybackState.Idle, viewModel.miniPlayerState.value)
    }
}
