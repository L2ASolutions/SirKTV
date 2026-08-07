package com.sirktv.app.player

sealed interface PlaybackState {
    data object Idle : PlaybackState
    data object Buffering : PlaybackState
    data object Playing : PlaybackState
    data object Paused : PlaybackState
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : PlaybackState
    data class Error(val attemptsExhausted: Boolean) : PlaybackState
}
