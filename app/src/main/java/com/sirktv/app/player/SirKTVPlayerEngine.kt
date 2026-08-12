package com.sirktv.app.player

import android.content.Context
import android.graphics.Color
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.sirktv.app.di.ApplicationScope
import com.sirktv.app.domain.model.PlayerSettings
import com.sirktv.app.domain.model.StreamQuality
import com.sirktv.app.domain.model.SubtitleAppearance
import com.sirktv.app.domain.model.SubtitleBackground
import com.sirktv.app.domain.model.SubtitleEdgeStyle
import com.sirktv.app.domain.model.SubtitleTextColor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns a single ExoPlayer instance for the whole app — Live TV always plays
 * through the same engine, so switching channels never tears down and rebuilds
 * the player, only swaps the media item. Buffer sizing, hardware decoding
 * preference, and the User-Agent are baked into the player at construction
 * time (ExoPlayer has no live setter for these), so changing them in Settings
 * rebuilds the player; changing quality or track selection does not.
 *
 * Reconnect behavior: on a fatal player error, retries the backup URL once
 * (HLS -> raw MPEG-TS), then falls into exponential backoff against whichever
 * URL last seemed viable (1s, 2s, 4s, 8s, 16s, capped at [PlayerSettings.reconnectAttempts]).
 */
private const val LOAD_TIMEOUT_MS = 10_000L

@Singleton
class SirKTVPlayerEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    @ApplicationScope private val appScope: CoroutineScope
) {
    private var exoPlayer: ExoPlayer? = null
    private var settings: PlayerSettings = PlayerSettings()
    private var subtitleAppearance: SubtitleAppearance = SubtitleAppearance()
    private var attachedPlayerView: PlayerView? = null

    private var currentChannelId: String? = null

    /**
     * Read-only view of [currentChannelId] for callers deciding whether to
     * pre-warm playback ahead of a navigation (e.g. Live TV's mini player
     * "Full Screen" hand-off) or skip re-tuning a channel this singleton
     * already has loading/loaded. Null whenever nothing has been tuned since
     * the last [release].
     */
    val tunedChannelId: String? get() = currentChannelId

    private var primaryUrl: String? = null
    private var backupUrl: String? = null
    private var usingBackup = false
    private var retryAttempt = 0
    private var retryJob: Job? = null
    private var loadTimeoutJob: Job? = null

    // VOD/episode-only: live content has no meaningful "resume position" — a
    // reconnect should land back on the live edge, not seek anywhere.
    private var isLiveContent = true
    private var resumePositionMs = 0L

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _tracks = MutableStateFlow<Tracks?>(null)
    val tracks: StateFlow<Tracks?> = _tracks.asStateFlow()

    private val _bufferedAheadMs = MutableStateFlow(0L)
    val bufferedAheadMs: StateFlow<Long> = _bufferedAheadMs.asStateFlow()
    private var bufferHealthPollJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    retryAttempt = 0
                    usingBackup = false
                    _state.value = if (exoPlayer?.playWhenReady == true) PlaybackState.Playing else PlaybackState.Paused
                }
                Player.STATE_BUFFERING -> {
                    if (_state.value !is PlaybackState.Reconnecting) _state.value = PlaybackState.Buffering
                }
                else -> Unit
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) _state.value = PlaybackState.Playing
        }

        override fun onTracksChanged(tracks: Tracks) {
            _tracks.value = tracks
        }

        override fun onPlayerError(error: PlaybackException) {
            handlePlaybackDrop()
        }
    }

    fun attachTo(playerView: PlayerView) {
        val player = ensurePlayer()
        runCatching { playerView.player = player }
        attachedPlayerView = playerView
        applySubtitleAppearance(playerView)
    }

    /**
     * Applied immediately to whichever [PlayerView] is currently attached (Live TV
     * and VOD/episode playback each attach their own instance as the user
     * navigates), and re-applied on every future [attachTo] until changed again.
     */
    fun updateSubtitleAppearance(appearance: SubtitleAppearance) {
        subtitleAppearance = appearance
        attachedPlayerView?.let(::applySubtitleAppearance)
    }

    private fun applySubtitleAppearance(playerView: PlayerView) {
        val appearance = subtitleAppearance
        playerView.subtitleView?.apply {
            // Without this, embedded WebVTT/TTML styling in the subtitle track can
            // override the user's chosen appearance.
            setApplyEmbeddedStyles(false)
            setApplyEmbeddedFontSizes(false)
            setFractionalTextSize(appearance.textSize.fraction)
            setStyle(
                CaptionStyleCompat(
                    appearance.textColor.toArgb(),
                    appearance.background.toArgb(),
                    Color.TRANSPARENT,
                    appearance.edgeStyle.toCaptionEdgeType(),
                    Color.BLACK,
                    null
                )
            )
        }
    }

    /**
     * [backupStreamUrl] defaults to [primaryStreamUrl] for content with no
     * real fallback (VOD/episodes) — the reconnect pipeline still applies, it
     * just retries the same URL instead of switching container/protocol.
     * [startPositionMs]/[isLive] control resume: pass isLive = false with a
     * saved position to resume a movie/episode; live callers can ignore both.
     */
    fun play(
        channelId: String,
        primaryStreamUrl: String,
        backupStreamUrl: String = primaryStreamUrl,
        startPositionMs: Long = 0L,
        isLive: Boolean = true
    ) {
        retryJob?.cancel()
        retryAttempt = 0
        usingBackup = false
        currentChannelId = channelId
        primaryUrl = primaryStreamUrl
        backupUrl = backupStreamUrl
        isLiveContent = isLive
        resumePositionMs = startPositionMs
        _state.value = PlaybackState.Buffering
        retryTune(primaryStreamUrl)
        startLoadTimeout()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        runCatching {
            player.playWhenReady = !player.playWhenReady
            _state.value = if (player.playWhenReady) PlaybackState.Playing else PlaybackState.Paused
        }
    }

    /** Manual "Retry" after all automatic attempts were exhausted — starts fresh from the primary URL. */
    fun manualRetry() {
        val primary = primaryUrl ?: return
        retryAttempt = 0
        usingBackup = false
        _state.value = PlaybackState.Buffering
        retryTune(primary)
        startLoadTimeout()
    }

    /**
     * Guards against a stream that never errors but also never progresses
     * past buffering (e.g. a connection that hangs instead of failing
     * outright) — without this, [PlaybackState.Buffering] can persist
     * indefinitely with no path to the error/retry UI.
     */
    private fun startLoadTimeout() {
        loadTimeoutJob?.cancel()
        loadTimeoutJob = appScope.launch(Dispatchers.Main) {
            delay(LOAD_TIMEOUT_MS)
            if (_state.value is PlaybackState.Buffering) {
                retryJob?.cancel()
                _state.value = PlaybackState.Error(attemptsExhausted = true)
            }
        }
    }

    fun applyPreferredQuality(quality: StreamQuality) {
        val player = exoPlayer ?: return
        runCatching {
            val builder = player.trackSelectionParameters.buildUpon()
            when (quality) {
                StreamQuality.AUTO -> builder.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                StreamQuality.P1080 -> builder.setMaxVideoSize(1920, 1080)
                StreamQuality.P720 -> builder.setMaxVideoSize(1280, 720)
                StreamQuality.P480 -> builder.setMaxVideoSize(854, 480)
            }
            player.trackSelectionParameters = builder.build()
        }
    }

    fun setTrackOverride(override: TrackSelectionOverride) {
        val player = exoPlayer ?: return
        runCatching {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().addOverride(override).build()
        }
    }

    fun clearTrackTypeOverride(trackType: Int) {
        val player = exoPlayer ?: return
        runCatching {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().clearOverridesOfType(trackType).build()
        }
    }

    /** Lightweight pause used when the app backgrounds but PiP keeps the player alive. */
    fun pause() {
        runCatching { exoPlayer?.playWhenReady = false }
    }

    fun resume() {
        runCatching { exoPlayer?.playWhenReady = true }
    }

    /** Full teardown — used when the app backgrounds outside of PiP, per the Fire Stick memory budget. */
    fun release() {
        retryJob?.cancel()
        loadTimeoutJob?.cancel()
        bufferHealthPollJob?.cancel()
        runCatching {
            exoPlayer?.let {
                it.removeListener(playerListener)
                it.release()
            }
        }
        exoPlayer = null
        _state.value = PlaybackState.Idle
        _bufferedAheadMs.value = 0L
    }

    /**
     * Applies new settings. Buffer sizing, hardware decoding, and User-Agent are
     * only read at player construction, so a change to any of them rebuilds the
     * player and re-tunes the current channel; a quality-only change is applied live.
     */
    fun updateSettings(newSettings: PlayerSettings) {
        val needsRebuild = exoPlayer != null && (
            settings.resolveBufferMillis() != newSettings.resolveBufferMillis() ||
                settings.hardwareDecodingEnabled != newSettings.hardwareDecodingEnabled ||
                settings.userAgent != newSettings.userAgent
            )
        settings = newSettings
        if (needsRebuild) {
            val channelId = currentChannelId
            val primary = primaryUrl
            val backup = backupUrl
            val wasLive = isLiveContent
            // Freshest position for VOD content — don't lose progress made since the last poll tick.
            val resumeFrom = if (!wasLive) snapshotPosition().first else 0L
            release()
            if (channelId != null && primary != null && backup != null) {
                play(channelId, primary, backup, startPositionMs = resumeFrom, isLive = wasLive)
            }
        } else {
            applyPreferredQuality(newSettings.preferredQuality)
        }
    }

    private fun handlePlaybackDrop() {
        if (currentChannelId == null) return
        if (!usingBackup) {
            usingBackup = true
            val backup = backupUrl
            if (backup != null) {
                retryTune(backup)
                return
            }
        }
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        val maxAttempts = settings.reconnectAttempts
        if (retryAttempt >= maxAttempts) {
            retryJob?.cancel()
            _state.value = PlaybackState.Error(attemptsExhausted = true)
            return
        }
        retryAttempt += 1
        val delayMs = (1000L shl (retryAttempt - 1)).coerceAtMost(16_000L)
        _state.value = PlaybackState.Reconnecting(attempt = retryAttempt, maxAttempts = maxAttempts)
        retryJob?.cancel()
        // Dispatchers.Main: retryTune() touches the ExoPlayer instance, which is
        // only safe to call from its own (main) thread — appScope defaults to
        // Dispatchers.Default for unrelated background work.
        retryJob = appScope.launch(Dispatchers.Main) {
            delay(delayMs)
            val url = if (usingBackup) backupUrl else primaryUrl
            if (url != null) retryTune(url)
        }
    }

    private fun retryTune(url: String) {
        val player = ensurePlayer()
        runCatching {
            player.setMediaItem(MediaItem.fromUri(url))
            if (!isLiveContent && resumePositionMs > 0) {
                player.seekTo(resumePositionMs)
            }
            player.prepare()
            player.playWhenReady = true
        }
        applyPreferredQuality(settings.preferredQuality)
    }

    /** Main-thread only, same constraint as the buffer-health poll. Returns (positionMs, durationMs), (0, 0) if idle. */
    fun snapshotPosition(): Pair<Long, Long> {
        val player = exoPlayer ?: return 0L to 0L
        return runCatching {
            val duration = player.duration
            player.currentPosition to (if (duration == androidx.media3.common.C.TIME_UNSET) 0L else duration)
        }.getOrDefault(0L to 0L)
    }

    /** VOD/episode scrubbing — no-op for live content, which has no seekable timeline. Main-thread only. */
    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        runCatching {
            val duration = player.duration
            val clamped = if (duration == androidx.media3.common.C.TIME_UNSET) {
                positionMs.coerceAtLeast(0L)
            } else {
                positionMs.coerceIn(0L, duration)
            }
            player.seekTo(clamped)
        }
    }

    private fun ensurePlayer(): ExoPlayer {
        exoPlayer?.let { return it }

        val buffer = settings.resolveBufferMillis()
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(buffer.minBufferMs, buffer.maxBufferMs, buffer.startBufferMs, buffer.rebufferThresholdMs)
            .build()

        // EXTENSION_RENDERER_MODE_OFF restricts decoding to the platform's own
        // MediaCodec (hardware-first) path with no software/FFmpeg fallback —
        // the correct-by-construction "hardware decoding" toggle for a Fire
        // Stick-class device. PREFER only matters once a software decoder
        // extension module is actually bundled; today it degrades to the same
        // platform decoders, which is a safe default for the OFF case too.
        val renderersFactory = DefaultRenderersFactory(context).setExtensionRendererMode(
            if (settings.hardwareDecodingEnabled) {
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
            } else {
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            }
        )

        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient).setUserAgent(settings.userAgent)
        val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(httpDataSourceFactory)

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
        player.addListener(playerListener)
        exoPlayer = player
        startBufferHealthPoll(player)
        return player
    }

    /**
     * ExoPlayer has no buffer-fill callback, so the "buffer health" overlay
     * reading is polled. Must run on Main: ExoPlayer's position getters are
     * only safe to call from the thread the player was created on, and
     * [appScope] itself runs on Dispatchers.Default for unrelated background work.
     */
    private fun startBufferHealthPoll(player: ExoPlayer) {
        bufferHealthPollJob?.cancel()
        bufferHealthPollJob = appScope.launch(Dispatchers.Main) {
            while (true) {
                runCatching {
                    _bufferedAheadMs.value = (player.bufferedPosition - player.currentPosition).coerceAtLeast(0L)
                    // Keep the resume point fresh for VOD/episodes, so a mid-stream
                    // reconnect (see scheduleReconnect) lands close to the drop point
                    // instead of wherever play() was originally called with.
                    if (!isLiveContent) resumePositionMs = player.currentPosition
                }
                delay(1_000)
            }
        }
    }
}

private fun SubtitleTextColor.toArgb(): Int = when (this) {
    SubtitleTextColor.WHITE -> Color.WHITE
    SubtitleTextColor.YELLOW -> Color.YELLOW
    SubtitleTextColor.GREEN -> Color.GREEN
    SubtitleTextColor.CYAN -> Color.CYAN
}

private fun SubtitleBackground.toArgb(): Int = when (this) {
    SubtitleBackground.NONE -> Color.TRANSPARENT
    SubtitleBackground.SEMI_TRANSPARENT -> Color.argb(153, 0, 0, 0)
    SubtitleBackground.SOLID -> Color.BLACK
}

private fun SubtitleEdgeStyle.toCaptionEdgeType(): Int = when (this) {
    SubtitleEdgeStyle.NONE -> CaptionStyleCompat.EDGE_TYPE_NONE
    SubtitleEdgeStyle.OUTLINE -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
    SubtitleEdgeStyle.DROP_SHADOW -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
}
