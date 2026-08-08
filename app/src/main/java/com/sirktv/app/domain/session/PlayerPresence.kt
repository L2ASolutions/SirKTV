package com.sirktv.app.domain.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks whether a fullscreen player screen (Live TV/Sports or VOD/episode)
 * is currently on screen, set from each player ViewModel's init/onCleared.
 * The idle screensaver reads this so it never overlays live/VOD playback.
 */
@Singleton
class PlayerPresence @Inject constructor() {
    private val _isPlayerActive = MutableStateFlow(false)
    val isPlayerActive: StateFlow<Boolean> = _isPlayerActive.asStateFlow()

    fun setActive(active: Boolean) {
        _isPlayerActive.value = active
    }
}
