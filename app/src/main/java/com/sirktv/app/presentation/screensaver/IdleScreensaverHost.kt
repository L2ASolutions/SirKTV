package com.sirktv.app.presentation.screensaver

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

private const val IDLE_TIMEOUT_MS = 45_000L

/**
 * Wraps the whole app: tracks input activity app-wide (without consuming any
 * of it) and shows [ScreensaverOverlay] after 45 seconds of inactivity, but
 * only while logged in and outside the fullscreen player. Any key, D-pad, or
 * pointer-down event dismisses it and resets the idle clock.
 */
@Composable
fun IdleScreensaverHost(
    viewModel: ScreensaverViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isPlayerActive by viewModel.isPlayerActive.collectAsState()
    val items by viewModel.items.collectAsState()

    var lastActivityAt by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var isShowingScreensaver by remember { mutableStateOf(false) }

    fun registerActivity() {
        lastActivityAt = SystemClock.elapsedRealtime()
        isShowingScreensaver = false
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            val idleForMs = SystemClock.elapsedRealtime() - lastActivityAt
            if (isLoggedIn && !isPlayerActive && items.isNotEmpty() && idleForMs >= IDLE_TIMEOUT_MS) {
                isShowingScreensaver = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { registerActivity(); false }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    registerActivity()
                }
            }
    ) {
        content()

        if (isShowingScreensaver) {
            ScreensaverOverlay(items = items, modifier = Modifier.fillMaxSize())
        }
    }
}
