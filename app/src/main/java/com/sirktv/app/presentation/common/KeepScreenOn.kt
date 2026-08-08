package com.sirktv.app.presentation.common

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.sirktv.app.player.PlaybackState

/**
 * Keeps the Fire Stick display (and screen saver) from timing out while a
 * player screen is actively showing content. FLAG_KEEP_SCREEN_ON is applied
 * for every [PlaybackState] except [PlaybackState.Paused] — Buffering/
 * Reconnecting still count as "actively playing" (the user didn't ask for a
 * timeout, the network did), so only an explicit user pause lets the system
 * screen saver kick in on its own timeout. Re-entering a playing state (or
 * leaving the screen entirely) re-applies/clears the flag immediately via the
 * DisposableEffect key, no polling required.
 */
@Composable
fun KeepScreenOnWhilePlaying(playbackState: PlaybackState) {
    val activity = LocalContext.current as? Activity
    val keepOn = playbackState !is PlaybackState.Paused

    DisposableEffect(activity, keepOn) {
        val window = activity?.window
        if (keepOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
