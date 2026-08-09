package com.sirktv.app.presentation.player

import android.content.Context
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.sirktv.app.di.ApplicationScope
import com.sirktv.app.player.PlaybackState
import com.sirktv.app.player.SirKTVPlayerEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val SUPPORTED_ACTIONS =
    PlaybackStateCompat.ACTION_PLAY or
        PlaybackStateCompat.ACTION_PAUSE or
        PlaybackStateCompat.ACTION_PLAY_PAUSE or
        PlaybackStateCompat.ACTION_FAST_FORWARD or
        PlaybackStateCompat.ACTION_REWIND or
        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
        PlaybackStateCompat.ACTION_STOP

/**
 * A single MediaSessionCompat kept active for as long as any player screen
 * (Live TV, VOD, or episode) is on screen — this is what makes the Fire TV
 * remote's physical media buttons route to SirKTV reliably, including when
 * Compose has no focused view to catch a key event (overlay hidden, etc).
 * [ActivePlayerState] supplies the callback target; this class only tracks
 * how many player screens are currently open and mirrors
 * [SirKTVPlayerEngine.state] into [PlaybackStateCompat].
 */
@Singleton
class MediaSessionManager @Inject constructor(
    @ApplicationContext context: Context,
    private val playerEngine: SirKTVPlayerEngine,
    private val activePlayerState: ActivePlayerState,
    @ApplicationScope private val appScope: CoroutineScope
) {
    private val mediaSession = MediaSessionCompat(context, "SirKTVMediaSession").apply {
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() { activePlayerState.current?.play() }
            override fun onPause() { activePlayerState.current?.pause() }
            override fun onFastForward() { activePlayerState.current?.fastForward() }
            override fun onRewind() { activePlayerState.current?.rewind() }
            override fun onSkipToNext() { activePlayerState.current?.skipToNext() }
            override fun onSkipToPrevious() { activePlayerState.current?.skipToPrevious() }
            override fun onStop() { activePlayerState.current?.stopAndExit() }
        })
        setPlaybackState(idlePlaybackState())
    }

    private var openPlayerScreens = 0
    private var stateObserverJob: Job? = null

    fun enterPlayerScreen(title: String) {
        openPlayerScreens += 1
        mediaSession.isActive = true
        updateMetadata(title)
        if (stateObserverJob == null) {
            stateObserverJob = appScope.launch(Dispatchers.Main) {
                playerEngine.state.collect(::updatePlaybackState)
            }
        }
    }

    fun exitPlayerScreen() {
        openPlayerScreens = (openPlayerScreens - 1).coerceAtLeast(0)
        if (openPlayerScreens == 0) {
            stateObserverJob?.cancel()
            stateObserverJob = null
            mediaSession.setPlaybackState(idlePlaybackState())
            mediaSession.isActive = false
        }
    }

    /** Channel name for Live TV, movie/episode title for VOD — call again whenever the current item changes. */
    fun updateMetadata(title: String) {
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .build()
        )
    }

    private fun updatePlaybackState(state: PlaybackState) {
        val position = playerEngine.snapshotPosition().first
        val (stateInt, speed) = when (state) {
            is PlaybackState.Buffering, is PlaybackState.Reconnecting -> PlaybackStateCompat.STATE_BUFFERING to 0f
            is PlaybackState.Playing -> PlaybackStateCompat.STATE_PLAYING to 1f
            is PlaybackState.Paused -> PlaybackStateCompat.STATE_PAUSED to 0f
            is PlaybackState.Idle, is PlaybackState.Error -> PlaybackStateCompat.STATE_STOPPED to 0f
        }
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(SUPPORTED_ACTIONS)
                .setState(stateInt, position, speed, SystemClock.elapsedRealtime())
                .build()
        )
    }

    private fun idlePlaybackState() = PlaybackStateCompat.Builder()
        .setActions(SUPPORTED_ACTIONS)
        .setState(PlaybackStateCompat.STATE_STOPPED, 0L, 0f)
        .build()

    /** Process-teardown only (Application.onTerminate) — normal screen exits go through [exitPlayerScreen]. */
    fun release() {
        stateObserverJob?.cancel()
        mediaSession.release()
    }
}
