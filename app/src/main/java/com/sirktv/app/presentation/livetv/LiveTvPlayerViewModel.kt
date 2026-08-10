package com.sirktv.app.presentation.livetv

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import com.sirktv.app.di.ApplicationScope
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.model.EpgProgram
import com.sirktv.app.domain.model.PlayerSettings
import com.sirktv.app.domain.model.StreamQuality
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.domain.session.PlayerPresence
import com.sirktv.app.domain.usecase.GetEpgListingsUseCase
import com.sirktv.app.domain.usecase.GetEpgNowNextUseCase
import com.sirktv.app.domain.usecase.GetPlayerSettingsUseCase
import com.sirktv.app.domain.usecase.GetSubtitleAppearanceUseCase
import com.sirktv.app.domain.usecase.ObserveChannelsUseCase
import com.sirktv.app.domain.usecase.SetLastWatchedChannelUseCase
import com.sirktv.app.domain.usecase.ToggleFavoriteUseCase
import com.sirktv.app.network.XtreamStreamUrlBuilder
import com.sirktv.app.player.NetworkMonitor
import com.sirktv.app.player.NetworkTransport
import com.sirktv.app.player.PlaybackState
import com.sirktv.app.player.SirKTVPlayerEngine
import com.sirktv.app.presentation.player.ActivePlayerHandle
import com.sirktv.app.presentation.player.ActivePlayerState
import com.sirktv.app.presentation.player.MediaSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LiveTvMode { WATCHING, OVERLAY, CHANNEL_LIST, PROGRAM_GUIDE, QUICK_ACTIONS }

data class LiveTvUiState(
    val mode: LiveTvMode = LiveTvMode.WATCHING,
    val channels: List<Channel> = emptyList(),
    val currentChannelId: String? = null,
    val nowNext: EpgNowNext = EpgNowNext(now = null, next = null),
    val playbackState: PlaybackState = PlaybackState.Idle,
    val showBufferHealth: Boolean = false,
    val preferredQuality: StreamQuality = StreamQuality.AUTO,
    val networkTip: String? = null
) {
    val currentChannel: Channel? get() = channels.find { it.id == currentChannelId }
}

@HiltViewModel
class LiveTvPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeChannelsUseCase: ObserveChannelsUseCase,
    private val getEpgNowNextUseCase: GetEpgNowNextUseCase,
    private val getEpgListingsUseCase: GetEpgListingsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val setLastWatchedChannelUseCase: SetLastWatchedChannelUseCase,
    private val getPlayerSettingsUseCase: GetPlayerSettingsUseCase,
    private val getSubtitleAppearanceUseCase: GetSubtitleAppearanceUseCase,
    private val currentSession: CurrentSession,
    private val playerEngine: SirKTVPlayerEngine,
    private val networkMonitor: NetworkMonitor,
    private val playerPresence: PlayerPresence,
    private val activePlayerState: ActivePlayerState,
    private val mediaSessionManager: MediaSessionManager,
    @ApplicationScope private val appScope: CoroutineScope
) : ViewModel(), ActivePlayerHandle {

    private val initialChannelId: String? = savedStateHandle.get<String>("channelId")

    private val _exitEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val exitEvent: SharedFlow<Unit> = _exitEvent.asSharedFlow()

    init {
        playerPresence.setActive(true)
        activePlayerState.register(this)
        mediaSessionManager.enterPlayerScreen(title = "Live TV")
    }

    private val _uiState = MutableStateFlow(LiveTvUiState())
    val uiState: StateFlow<LiveTvUiState> = _uiState.asStateFlow()

    val bufferedAheadMs: StateFlow<Long> = playerEngine.bufferedAheadMs
    val tracks: StateFlow<Tracks?> = playerEngine.tracks

    private var hasTunedInitialChannel = false
    private var overlayHideJob: Job? = null
    private var epgFetchJob: Job? = null
    private var networkTipShown = false

    // Per-row EPG for the Channel List / Program Guide panels. Rows request
    // their own now/next via LaunchedEffect when they enter composition, so a
    // LazyColumn's natural "only compose visible items" behavior is what makes
    // this lazy — nothing is fetched for a channel that's never scrolled into view.
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

    // Full-listing counterpart to [_channelEpgCache] above, for the program guide
    // grid's timeline — same lazy-on-visible fetch pattern, separate cache because
    // the grid needs every upcoming program, not just now/next.
    private val _channelEpgListingsCache = MutableStateFlow<Map<String, List<EpgProgram>>>(emptyMap())
    val channelEpgListingsCache: StateFlow<Map<String, List<EpgProgram>>> = _channelEpgListingsCache.asStateFlow()
    private val requestedEpgListingChannelIds = mutableSetOf<String>()

    fun requestEpgListingsFor(channelId: String) {
        if (!requestedEpgListingChannelIds.add(channelId)) return
        viewModelScope.launch {
            val listings = getEpgListingsUseCase(channelId)
            _channelEpgListingsCache.update { it + (channelId to listings) }
        }
    }

    init {
        viewModelScope.launch {
            runCatching {
                observeChannelsUseCase().collect { channels ->
                    _uiState.update { it.copy(channels = channels) }
                    if (!hasTunedInitialChannel && channels.isNotEmpty()) {
                        val target = channels.find { it.id == initialChannelId } ?: channels.first()
                        hasTunedInitialChannel = true
                        tuneTo(target)
                    }
                }
            }
        }
        viewModelScope.launch {
            playerEngine.state.collect { state ->
                _uiState.update { it.copy(playbackState = state) }
                if (state is PlaybackState.Reconnecting || state is PlaybackState.Buffering) {
                    maybeShowNetworkTip()
                }
            }
        }
        viewModelScope.launch {
            getPlayerSettingsUseCase().collect { settings: PlayerSettings ->
                playerEngine.updateSettings(settings)
                _uiState.update { it.copy(showBufferHealth = settings.showBufferHealth, preferredQuality = settings.preferredQuality) }
            }
        }
        viewModelScope.launch {
            getSubtitleAppearanceUseCase().collect { playerEngine.updateSubtitleAppearance(it) }
        }
    }

    private fun maybeShowNetworkTip() {
        if (networkTipShown) return
        val info = networkMonitor.currentInfo()
        if (info.transport == NetworkTransport.WIFI && info.isLowBandWifi) {
            networkTipShown = true
            _uiState.update { it.copy(networkTip = "Playback may be limited on 2.4GHz WiFi — try 5GHz WiFi or Ethernet.") }
            viewModelScope.launch {
                delay(6_000)
                _uiState.update { it.copy(networkTip = null) }
            }
        }
    }

    fun attach(playerView: androidx.media3.ui.PlayerView) = playerEngine.attachTo(playerView)

    fun onChannelUp() = stepChannel(delta = -1)
    fun onChannelDown() = stepChannel(delta = 1)

    private fun stepChannel(delta: Int) {
        val state = _uiState.value
        val channels = state.channels
        if (channels.isEmpty()) return
        val currentIndex = channels.indexOfFirst { it.id == state.currentChannelId }.takeIf { it >= 0 } ?: 0
        val nextIndex = (currentIndex + delta + channels.size) % channels.size
        tuneTo(channels[nextIndex])
    }

    fun onChannelSelected(channel: Channel) {
        tuneTo(channel)
        setMode(LiveTvMode.WATCHING)
    }

    private fun tuneTo(channel: Channel) {
        val credentials = currentSession.credentials.value ?: return
        val primary = XtreamStreamUrlBuilder.buildPrimaryUrl(credentials.serverUrl, credentials.username, credentials.password, channel.id)
        val backup = XtreamStreamUrlBuilder.buildBackupUrl(credentials.serverUrl, credentials.username, credentials.password, channel.id)
        playerEngine.play(channel.id, primary, backup)
        _uiState.update { it.copy(currentChannelId = channel.id, nowNext = EpgNowNext(now = null, next = null)) }
        mediaSessionManager.updateMetadata(channel.name)
        viewModelScope.launch { setLastWatchedChannelUseCase(channel.id) }
        fetchEpg(channel.id)
    }

    private fun fetchEpg(channelId: String) {
        epgFetchJob?.cancel()
        epgFetchJob = viewModelScope.launch {
            val nowNext = getEpgNowNextUseCase(channelId)
            if (_uiState.value.currentChannelId == channelId) {
                _uiState.update { it.copy(nowNext = nowNext) }
            }
        }
    }

    fun onToggleFavorite(channelId: String) {
        viewModelScope.launch { toggleFavoriteUseCase(channelId) }
    }

    fun onOkPressed() {
        when (_uiState.value.mode) {
            LiveTvMode.WATCHING -> setMode(LiveTvMode.OVERLAY)
            LiveTvMode.OVERLAY -> playerEngine.togglePlayPause()
            LiveTvMode.CHANNEL_LIST, LiveTvMode.PROGRAM_GUIDE, LiveTvMode.QUICK_ACTIONS -> Unit
        }
    }

    fun onLongOkPressed() {
        if (_uiState.value.mode == LiveTvMode.WATCHING) setMode(LiveTvMode.QUICK_ACTIONS)
    }

    fun onLeftPressed() {
        if (_uiState.value.mode == LiveTvMode.WATCHING || _uiState.value.mode == LiveTvMode.OVERLAY) {
            setMode(LiveTvMode.CHANNEL_LIST)
        }
    }

    fun onRightPressed() {
        if (_uiState.value.mode == LiveTvMode.WATCHING || _uiState.value.mode == LiveTvMode.OVERLAY) {
            setMode(LiveTvMode.PROGRAM_GUIDE)
        }
    }

    /**
     * First Back press: reveal the overlay instead of leaving the screen —
     * mirrors the OK button's "show overlay" behavior and matches the VOD
     * player's two-stage Back. A second press lands here with the overlay
     * already visible: [LiveTvPlayerScreen]'s BackHandler intercepts that
     * case itself and calls the nav `onBack` callback directly (which pops
     * the back stack and triggers [onCleared]), so OVERLAY never reaches
     * this function.
     */
    fun onBackPressed() {
        when (_uiState.value.mode) {
            LiveTvMode.WATCHING, LiveTvMode.QUICK_ACTIONS, LiveTvMode.CHANNEL_LIST, LiveTvMode.PROGRAM_GUIDE -> setMode(LiveTvMode.OVERLAY)
            LiveTvMode.OVERLAY -> Unit
        }
    }

    fun onManualRetry() = playerEngine.manualRetry()

    /** "Back to channel list" action offered alongside Retry on the playback error overlay. */
    fun onErrorBackToChannelList() = setMode(LiveTvMode.CHANNEL_LIST)

    fun selectTrack(group: Tracks.Group, trackIndexInGroup: Int) =
        playerEngine.setTrackOverride(TrackSelectionOverride(group.mediaTrackGroup, trackIndexInGroup))

    fun clearAudioOverride() = playerEngine.clearTrackTypeOverride(C.TRACK_TYPE_AUDIO)
    fun disableSubtitles() = playerEngine.clearTrackTypeOverride(C.TRACK_TYPE_TEXT)

    /** Session-only override — does not touch the persisted Settings default. */
    fun selectQuality(quality: StreamQuality) {
        playerEngine.applyPreferredQuality(quality)
        _uiState.update { it.copy(preferredQuality = quality) }
    }

    private fun setMode(mode: LiveTvMode) {
        overlayHideJob?.cancel()
        _uiState.update { it.copy(mode = mode) }
        if (mode == LiveTvMode.OVERLAY) {
            overlayHideJob = viewModelScope.launch {
                delay(5_000)
                if (_uiState.value.mode == LiveTvMode.OVERLAY) setMode(LiveTvMode.WATCHING)
            }
        }
    }

    // --- ActivePlayerHandle — routes MainActivity's onKeyDown and the
    // MediaSessionCompat callback to this screen while it's the one on top. ---

    override fun togglePlayPause() = playerEngine.togglePlayPause()
    override fun play() = playerEngine.resume()
    override fun pause() = playerEngine.pause()

    /** No per-content distinction for Live TV: FF and skip-next both mean "next channel." */
    override fun fastForward() = onChannelDown()
    override fun rewind() = onChannelUp()
    override fun skipToNext() = onChannelDown()
    override fun skipToPrevious() = onChannelUp()

    override fun stopAndExit() {
        releasePlayer()
        _exitEvent.tryEmit(Unit)
    }

    // Routes to OVERLAY rather than the (narrower) long-press QUICK_ACTIONS
    // sheet: OVERLAY is the surface that actually has audio track, subtitle,
    // quality, PiP, and channel-info controls together, matching what a
    // MENU-button quick-actions surface is supposed to offer.
    override fun showQuickActions() {
        if (_uiState.value.mode == LiveTvMode.WATCHING) setMode(LiveTvMode.OVERLAY)
    }

    /**
     * Full teardown used both by explicit exits ([stopAndExit]) and by
     * [onCleared] — stops audio/video immediately rather than leaving the
     * app-wide [SirKTVPlayerEngine] instance running with no attached view.
     * The last-watched channel is already persisted continuously from
     * [tuneTo], so there's no resume position to save here (live content has
     * no meaningful seek position).
     */
    fun releasePlayer() {
        playerEngine.pause()
        appScope.launch { playerEngine.release() }
        mediaSessionManager.exitPlayerScreen()
    }

    override fun onCleared() {
        overlayHideJob?.cancel()
        epgFetchJob?.cancel()
        playerPresence.setActive(false)
        activePlayerState.clear(this)
        releasePlayer()
    }
}
