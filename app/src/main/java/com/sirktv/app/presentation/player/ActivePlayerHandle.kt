package com.sirktv.app.presentation.player

/**
 * Remote-control surface exposed by whichever player ViewModel (Live TV or
 * VOD/episode) currently owns the screen. Both [com.sirktv.app.MainActivity]'s
 * onKeyDown and [MediaSessionManager]'s MediaSessionCompat callback route
 * through the single instance registered in [ActivePlayerState] instead of
 * duplicating player logic at the Activity or session layer.
 */
interface ActivePlayerHandle {
    fun togglePlayPause()
    fun play()
    fun pause()

    /** VOD: seek forward 30s. Live TV: next channel. */
    fun fastForward()

    /** VOD: seek back 10s. Live TV: previous channel. */
    fun rewind()

    /** VOD: next episode (no-op for movies). Live TV: next channel. */
    fun skipToNext()

    /** Live TV: previous channel. VOD: no-op. */
    fun skipToPrevious()

    fun stopAndExit()
    fun showQuickActions()
}
