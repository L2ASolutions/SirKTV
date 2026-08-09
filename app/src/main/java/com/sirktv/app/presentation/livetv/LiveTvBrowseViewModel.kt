package com.sirktv.app.presentation.livetv

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.model.EpgProgram
import com.sirktv.app.domain.model.PlayerSettings
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.domain.usecase.GetEpgListingsUseCase
import com.sirktv.app.domain.usecase.GetEpgNowNextUseCase
import com.sirktv.app.domain.usecase.GetStartupPreferenceUseCase
import com.sirktv.app.domain.usecase.ObserveCategoriesUseCase
import com.sirktv.app.domain.usecase.ObserveChannelsUseCase
import com.sirktv.app.domain.usecase.SyncChannelsUseCase
import com.sirktv.app.domain.usecase.ToggleFavoriteUseCase
import com.sirktv.app.network.XtreamStreamUrlBuilder
import com.sirktv.app.player.PlaybackState
import com.sirktv.app.presentation.common.SECTION_SYNC_TIMEOUT_MS
import com.sirktv.app.presentation.common.SectionLoadError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import javax.inject.Inject

data class LiveTvBrowseUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val allChannels: List<Channel> = emptyList(),
    val visibleChannels: List<Channel> = emptyList(),
    val selectedCategoryId: String? = null,
    val selectedChannelId: String? = null,
    val nowPlayingChannelId: String? = null,
    val epgCache: Map<String, EpgNowNext> = emptyMap(),
    val epgListingsCache: Map<String, List<EpgProgram>> = emptyMap(),
    val loadError: SectionLoadError? = null,
    // Distinct from loadError (a known, "expected" sync failure): set only
    // when something in this screen's own init/collector logic throws
    // unexpectedly, so the UI can fail into a full-screen error + Retry
    // instead of taking the whole app down with it.
    val isError: Boolean = false
) {
    val selectedChannel: Channel? get() = allChannels.find { it.id == selectedChannelId } ?: visibleChannels.firstOrNull()
}

/**
 * Flat Xtream-category sidebar + channel list + a real (muted, looping)
 * preview player, shown after login/from Home so the user picks a channel
 * manually. Nothing plays unmuted until "Watch Full Screen" is pressed —
 * this screen never auto-tunes the fullscreen player on its own.
 */
@HiltViewModel
class LiveTvBrowseViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val currentSession: CurrentSession,
    private val observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val observeChannelsUseCase: ObserveChannelsUseCase,
    private val syncChannelsUseCase: SyncChannelsUseCase,
    private val getEpgNowNextUseCase: GetEpgNowNextUseCase,
    private val getEpgListingsUseCase: GetEpgListingsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getStartupPreferenceUseCase: GetStartupPreferenceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveTvBrowseUiState())
    val uiState: StateFlow<LiveTvBrowseUiState> = _uiState.asStateFlow()

    private val requestedEpgChannelIds = mutableSetOf<String>()
    private val requestedEpgListingChannelIds = mutableSetOf<String>()

    // --- Preview player: a second, independent ExoPlayer instance (muted,
    // looping) dedicated to this screen only — never shared with the
    // fullscreen SirKTVPlayerEngine singleton. ---

    private val _previewPlayer = MutableStateFlow<ExoPlayer?>(null)
    val previewPlayer: StateFlow<ExoPlayer?> = _previewPlayer.asStateFlow()

    private val _previewState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val previewState: StateFlow<PlaybackState> = _previewState.asStateFlow()

    private var previewChannelId: String? = null
    private var previewLoadTimeoutJob: Job? = null

    private val previewListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    previewLoadTimeoutJob?.cancel()
                    _previewState.value = PlaybackState.Playing
                }
                Player.STATE_BUFFERING -> {
                    if (_previewState.value !is PlaybackState.Error) _previewState.value = PlaybackState.Buffering
                }
                else -> Unit
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            previewLoadTimeoutJob?.cancel()
            _previewState.value = PlaybackState.Error(attemptsExhausted = true)
        }
    }

    init {
        // The whole init block is defensive: any Room query, mapper, or
        // combine/collect step in here throwing was the suspected root cause
        // of the Live TV crash-on-entry — everything below fails into
        // isError = true (a full-screen error + Retry) instead of an
        // uncaught exception taking the whole app down.
        try {
            viewModelScope.launch {
                try {
                    combine(
                        runCatching { observeCategoriesUseCase() }.getOrElse { flowOf(emptyList()) },
                        runCatching { observeChannelsUseCase() }.getOrElse { flowOf(emptyList()) }
                    ) { categories, channels ->
                        categories to channels
                    }.collect { (categories, channels) ->
                        _uiState.update {
                            val visible = filterChannels(channels, it.selectedCategoryId)
                            it.copy(
                                categories = categories,
                                allChannels = channels,
                                visibleChannels = visible,
                                selectedChannelId = it.selectedChannelId ?: visible.firstOrNull()?.id,
                                isLoading = if (channels.isNotEmpty()) false else it.isLoading
                            )
                        }
                        // Deliberately does NOT auto-load a preview here: the preview
                        // ExoPlayer is only ever built/used once a channel row actually
                        // gains D-pad focus (see onChannelHighlighted) — never eagerly
                        // on screen entry, so a slow/failing player build can't take
                        // down the initial render of this screen.
                    }
                } catch (e: Exception) {
                    Log.e("SirKTV_LiveTV", "CRASH in categories/channels collector: ${e.stackTraceToString()}")
                    _uiState.update { it.copy(isLoading = false, isError = true) }
                }
            }
            viewModelScope.launch {
                try {
                    getStartupPreferenceUseCase().collect { preference ->
                        _uiState.update { it.copy(nowPlayingChannelId = preference.lastWatchedChannelId) }
                    }
                } catch (e: Exception) {
                    Log.e("SirKTV_LiveTV", "CRASH in startup preference collector: ${e.stackTraceToString()}")
                }
            }
            refresh()
        } catch (e: Exception) {
            Log.e("SirKTV_LiveTV", "CRASH on Live TV ViewModel init: ${e.stackTraceToString()}")
            _uiState.update { it.copy(isLoading = false, isError = true) }
        }
    }

    /**
     * On-demand sync — called once when this screen is first entered, and
     * again by the top bar's refresh icon or the error screen's Retry
     * button. Room is the source of truth throughout: the [combine]
     * collector above updates [LiveTvBrowseUiState] reactively the moment
     * new data lands, this function only decides whether to show a spinner
     * or an error alongside whatever Room already has cached.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.allChannels.isEmpty(), loadError = null, isError = false) }
            try {
                // withTimeoutOrNull already fails soft into null on the 15s
                // budget below — no TimeoutCancellationException ever escapes
                // this block for the caller to see.
                val result = withTimeoutOrNull(SECTION_SYNC_TIMEOUT_MS) { syncChannelsUseCase() }
                val error = when {
                    result == null -> SectionLoadError.TIMEOUT
                    result.isFailure -> SectionLoadError.NETWORK
                    _uiState.value.allChannels.isEmpty() -> SectionLoadError.EMPTY
                    else -> null
                }
                _uiState.update { it.copy(isLoading = false, loadError = error) }
            } catch (e: Exception) {
                Log.e("SirKTV_LiveTV", "CRASH during channel sync: ${e.stackTraceToString()}")
                _uiState.update { it.copy(isLoading = false, isError = true) }
            }
        }
    }

    /** Retry from the full-screen crash-guard error state — clears [LiveTvBrowseUiState.isError] and re-syncs. */
    fun retryAfterError() {
        _uiState.update { it.copy(isError = false) }
        refresh()
    }

    /** null selects the pinned "All Channels" row. */
    fun onCategorySelected(categoryId: String?) {
        _uiState.update {
            val visible = filterChannels(it.allChannels, categoryId)
            it.copy(selectedCategoryId = categoryId, visibleChannels = visible, selectedChannelId = visible.firstOrNull()?.id)
        }
        _uiState.value.selectedChannel?.let { loadPreview(it.id) }
    }

    /** Called as D-pad focus lands on a channel row — loads its preview immediately, no OK press required. */
    fun onChannelHighlighted(channelId: String) {
        _uiState.update { it.copy(selectedChannelId = channelId) }
        loadPreview(channelId)
    }

    fun requestEpgFor(channelId: String) {
        if (!requestedEpgChannelIds.add(channelId)) return
        viewModelScope.launch {
            val nowNext = runCatching { getEpgNowNextUseCase(channelId) }
                .getOrDefault(EpgNowNext(now = null, next = null))
            _uiState.update { it.copy(epgCache = it.epgCache + (channelId to nowNext)) }
        }
    }

    fun requestEpgListingsFor(channelId: String) {
        if (!requestedEpgListingChannelIds.add(channelId)) return
        viewModelScope.launch {
            val listings = runCatching { getEpgListingsUseCase(channelId, limit = 20) }
                .getOrDefault(emptyList())
            _uiState.update { it.copy(epgListingsCache = it.epgListingsCache + (channelId to listings)) }
        }
    }

    fun onToggleFavorite(channelId: String) {
        viewModelScope.launch { toggleFavoriteUseCase(channelId) }
    }

    private fun filterChannels(channels: List<Channel>, categoryId: String?): List<Channel> =
        if (categoryId == null) channels else channels.filter { it.categoryId == categoryId }

    /**
     * Builds the preview ExoPlayer if it doesn't exist yet. Called explicitly
     * from the screen's `LaunchedEffect(Unit)` — never from init/the
     * constructor — so construction only ever happens once the Composable is
     * actually alive, and a failure here (device without the right codec,
     * ExoPlayer internals throwing during setup, etc.) can't take the whole
     * screen down before it even renders.
     */
    fun initPreviewPlayer() {
        if (_previewPlayer.value != null) return
        runCatching { buildPreviewPlayer() }
            .onSuccess { _previewPlayer.value = it }
            .onFailure { _previewState.value = PlaybackState.Error(attemptsExhausted = true) }
    }

    private fun buildPreviewPlayer(): ExoPlayer {
        // Same buffer profile the fullscreen engine defaults to, but with a
        // much shorter start buffer so previews feel instant while scrolling.
        val buffer = PlayerSettings.MEDIUM_BUFFER
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(buffer.minBufferMs, buffer.maxBufferMs, 2_000, buffer.rebufferThresholdMs)
            .build()
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient).setUserAgent(PlayerSettings().userAgent)
        val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(httpDataSourceFactory)

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
        player.volume = 0f
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.addListener(previewListener)
        return player
    }

    /** Lazy, focus-driven — called only from [onChannelHighlighted]/[onCategorySelected], never on screen entry. */
    private fun loadPreview(channelId: String) {
        if (previewChannelId == channelId && _previewState.value !is PlaybackState.Error) return
        val credentials = currentSession.credentials.value ?: return
        initPreviewPlayer()
        val player = _previewPlayer.value ?: return

        runCatching {
            previewChannelId = channelId
            val url = XtreamStreamUrlBuilder.buildPrimaryUrl(credentials.serverUrl, credentials.username, credentials.password, channelId)
            previewLoadTimeoutJob?.cancel()
            _previewState.value = PlaybackState.Buffering
            player.stop()
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.play()

            previewLoadTimeoutJob = viewModelScope.launch {
                delay(8_000)
                if (_previewState.value is PlaybackState.Buffering) {
                    _previewState.value = PlaybackState.Error(attemptsExhausted = true)
                }
            }
        }.onFailure {
            _previewState.value = PlaybackState.Error(attemptsExhausted = true)
        }
    }

    /** Full teardown — called from the screen's DisposableEffect(onDispose) and from [onCleared]. */
    fun releasePreview() {
        previewLoadTimeoutJob?.cancel()
        runCatching {
            _previewPlayer.value?.let {
                it.removeListener(previewListener)
                it.stop()
                it.release()
            }
        }
        _previewPlayer.value = null
        _previewState.value = PlaybackState.Idle
        previewChannelId = null
    }

    override fun onCleared() {
        releasePreview()
    }
}
