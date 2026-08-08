package com.sirktv.app.presentation.livetv

import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.PlayerView
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.player.PlaybackState
import com.sirktv.app.presentation.common.KeepScreenOnWhilePlaying
import com.sirktv.app.presentation.common.SirKTVLogoMark
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val LONG_PRESS_THRESHOLD_MS = 500L

@Composable
fun LiveTvPlayerScreen(
    viewModel: LiveTvPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Only intercepts Back while inside a sub-mode (overlay/panels/quick actions).
    // At WATCHING it's disabled so Back falls through to normal navigation.
    BackHandler(enabled = uiState.mode != LiveTvMode.WATCHING) {
        viewModel.onBackPressed()
    }

    KeepScreenOnWhilePlaying(uiState.playbackState)

    var okDownAt by remember { mutableStateOf(0L) }
    var longPressTriggered by remember { mutableStateOf(false) }
    val rootFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { rootFocusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                val native = keyEvent.nativeKeyEvent
                val mode = uiState.mode
                when (native.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (mode == LiveTvMode.WATCHING) {
                            if (keyEvent.type == KeyEventType.KeyDown) viewModel.onChannelUp()
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (mode == LiveTvMode.WATCHING) {
                            if (keyEvent.type == KeyEventType.KeyDown) viewModel.onChannelDown()
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (mode == LiveTvMode.WATCHING || mode == LiveTvMode.OVERLAY) {
                            if (keyEvent.type == KeyEventType.KeyDown) viewModel.onLeftPressed()
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (mode == LiveTvMode.WATCHING || mode == LiveTvMode.OVERLAY) {
                            if (keyEvent.type == KeyEventType.KeyDown) viewModel.onRightPressed()
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (mode == LiveTvMode.WATCHING || mode == LiveTvMode.OVERLAY) {
                            when (keyEvent.type) {
                                KeyEventType.KeyDown -> {
                                    if (native.repeatCount == 0) {
                                        okDownAt = SystemClock.elapsedRealtime()
                                        longPressTriggered = false
                                    } else if (
                                        mode == LiveTvMode.WATCHING &&
                                        !longPressTriggered &&
                                        SystemClock.elapsedRealtime() - okDownAt >= LONG_PRESS_THRESHOLD_MS
                                    ) {
                                        longPressTriggered = true
                                        viewModel.onLongOkPressed()
                                    }
                                }
                                KeyEventType.KeyUp -> {
                                    if (!longPressTriggered) viewModel.onOkPressed()
                                }
                                else -> Unit
                            }
                            true
                        } else false
                    }
                    else -> false
                }
            }
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                }.also(viewModel::attach)
            },
            modifier = Modifier.fillMaxSize()
        )

        when (val playback = uiState.playbackState) {
            is PlaybackState.Buffering -> BufferingOverlay(channelName = uiState.currentChannel?.name, nowNext = uiState.nowNext)
            is PlaybackState.Reconnecting -> ReconnectingOverlay(attempt = playback.attempt, maxAttempts = playback.maxAttempts)
            is PlaybackState.Error -> ErrorOverlay(
                channelName = uiState.currentChannel?.name,
                onRetry = viewModel::onManualRetry,
                onBackToChannelList = viewModel::onErrorBackToChannelList
            )
            else -> Unit
        }

        AnimatedVisibility(visible = uiState.mode == LiveTvMode.OVERLAY, enter = fadeIn(), exit = fadeOut()) {
            val tracks by viewModel.tracks.collectAsState()
            LiveTvOverlay(
                uiState = uiState,
                onFavoriteClick = { uiState.currentChannelId?.let(viewModel::onToggleFavorite) },
                onSelectAudioTrack = viewModel::selectTrack,
                onClearAudioTrack = viewModel::clearAudioOverride,
                onSelectSubtitleTrack = viewModel::selectTrack,
                onDisableSubtitles = viewModel::disableSubtitles,
                onSelectQuality = viewModel::selectQuality,
                onChannelListToggle = viewModel::onLeftPressed,
                onChannelUp = viewModel::onChannelUp,
                onChannelDown = viewModel::onChannelDown,
                tracks = tracks
            )
        }

        AnimatedVisibility(visible = uiState.mode == LiveTvMode.CHANNEL_LIST, enter = fadeIn(), exit = fadeOut()) {
            ChannelListPanel(
                channels = uiState.channels,
                currentChannelId = uiState.currentChannelId,
                onChannelSelected = viewModel::onChannelSelected,
                onFavoriteToggle = { channel -> viewModel.onToggleFavorite(channel.id) }
            )
        }

        AnimatedVisibility(visible = uiState.mode == LiveTvMode.PROGRAM_GUIDE, enter = fadeIn(), exit = fadeOut()) {
            ProgramGuidePanel(
                channels = uiState.channels,
                currentChannelId = uiState.currentChannelId,
                onChannelSelected = viewModel::onChannelSelected
            )
        }

        if (uiState.mode == LiveTvMode.QUICK_ACTIONS) {
            QuickActionsSheet(
                isFavorite = uiState.currentChannel?.isFavorite == true,
                onFavoriteToggle = { uiState.currentChannelId?.let(viewModel::onToggleFavorite) },
                onRestartStream = viewModel::onManualRetry,
                onDismiss = viewModel::onBackPressed
            )
        }

        if (uiState.showBufferHealth) {
            val bufferedAheadMs by viewModel.bufferedAheadMs.collectAsState()
            BufferHealthIndicator(bufferedAheadMs = bufferedAheadMs, modifier = Modifier.align(Alignment.TopEnd))
        }

        uiState.networkTip?.let { tip ->
            NetworkTipBanner(text = tip, modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun BufferingOverlay(channelName: String?, nowNext: EpgNowNext) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            SirKTVLogoMark()
            channelName?.let { Text(it, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            nowNext.now?.let { Text(it.title, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp) }
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = SirKTVPrimary)
        }
    }
}

@Composable
private fun ReconnectingOverlay(attempt: Int, maxAttempts: Int) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        StatusPill(text = "Reconnecting — attempt $attempt of $maxAttempts", dotColor = Color(0xFFFF9F43))
    }
}

@Composable
private fun ErrorOverlay(channelName: String?, onRetry: () -> Unit, onBackToChannelList: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            Text(
                text = "Couldn't load${channelName?.let { " $it" } ?: " this channel"}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "We tried several times. Check your connection and try again, or pick another channel.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                modifier = Modifier.padding(top = Dimens.SpaceSm)
            ) {
                androidx.tv.material3.Button(onClick = onRetry) {
                    androidx.tv.material3.Text("Retry")
                }
                androidx.tv.material3.Button(onClick = onBackToChannelList) {
                    androidx.tv.material3.Text("Channel List")
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, dotColor: Color) {
    Row(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(8.dp).background(dotColor, RoundedCornerShape(50)))
        Text(text, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun BufferHealthIndicator(bufferedAheadMs: Long, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(Dimens.SpaceMd)) {
        StatusPill(text = "buffer ${"%.1f".format(bufferedAheadMs / 1000f)}s", dotColor = SirKTVPrimary)
    }
}

@Composable
private fun NetworkTipBanner(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(top = Dimens.SpaceMd)) {
        Row(
            modifier = Modifier
                .background(Color(0xCC1A1A22), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(text, color = Color.White, fontSize = 12.sp)
        }
    }
}

internal fun formatEpochSeconds(epochSeconds: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochSeconds * 1000))
