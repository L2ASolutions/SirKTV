package com.sirktv.app.presentation.vodplayer

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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.Surface
import androidx.tv.material3.Text as TvText
import com.sirktv.app.player.PlaybackState
import com.sirktv.app.presentation.common.KeepScreenOnWhilePlaying
import com.sirktv.app.presentation.common.SirKTVLogoMark
import com.sirktv.app.presentation.common.TvFocusAccent
import com.sirktv.app.presentation.common.glassCard
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.livetv.QualitySelectorSheet
import com.sirktv.app.presentation.livetv.TrackSelectorSheet
import com.sirktv.app.presentation.livetv.TrackTypes
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVPrimary

private enum class VodSheet { AUDIO, QUALITY }

@Composable
fun VodPlayerScreen(
    onBack: () -> Unit,
    viewModel: VodPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Two-stage Back: first press reveals controls if hidden (matches the OK
    // button's "show overlay" behavior); only a second press — with controls
    // already visible — actually leaves the screen. Leaving pops the nav
    // back stack, which destroys this screen's ViewModel and pauses/releases
    // the player from VodPlayerViewModel.onCleared().
    BackHandler(enabled = true) {
        if (uiState.mode == VodPlayerMode.WATCHING) {
            viewModel.onShowControls()
        } else {
            onBack()
        }
    }

    KeepScreenOnWhilePlaying(uiState.playbackState)

    val rootFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { rootFocusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val mode = uiState.mode
                when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (mode == VodPlayerMode.WATCHING || mode == VodPlayerMode.OVERLAY) { viewModel.onSeekBack(); true } else false
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (mode == VodPlayerMode.WATCHING || mode == VodPlayerMode.OVERLAY) { viewModel.onSeekForward(); true } else false
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        val isFreshPress = keyEvent.nativeKeyEvent.repeatCount == 0
                        if (isFreshPress && (mode == VodPlayerMode.WATCHING || mode == VodPlayerMode.OVERLAY)) {
                            viewModel.onOkPressed(); true
                        } else {
                            mode == VodPlayerMode.WATCHING || mode == VodPlayerMode.OVERLAY
                        }
                    }
                    else -> false
                }
            }
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply { useController = false }.also(viewModel::attach)
            },
            modifier = Modifier.fillMaxSize()
        )

        when (uiState.playbackState) {
            is PlaybackState.Buffering -> BufferingOverlay(title = uiState.title)
            is PlaybackState.Reconnecting -> ReconnectingOverlay()
            is PlaybackState.Error -> ErrorOverlay(title = uiState.title, onRetry = viewModel::onManualRetry)
            else -> Unit
        }

        AnimatedVisibility(visible = uiState.mode == VodPlayerMode.OVERLAY, enter = fadeIn(), exit = fadeOut()) {
            val tracks by viewModel.tracks.collectAsState()
            VodOverlay(
                uiState = uiState,
                tracks = tracks,
                onFavoriteClick = viewModel::onToggleFavorite,
                onNextEpisode = viewModel::onNextEpisode,
                onPlayPause = { viewModel.onOkPressed() },
                onSeekBack = viewModel::onSeekBack,
                onSeekForward = viewModel::onSeekForward,
                onSelectAudioTrack = viewModel::selectTrack,
                onClearAudioTrack = viewModel::clearAudioOverride,
                onToggleCaptions = viewModel::onToggleCaptions,
                onSelectQuality = viewModel::selectQuality
            )
        }
    }
}

@Composable
private fun VodOverlay(
    uiState: VodPlayerUiState,
    tracks: androidx.media3.common.Tracks?,
    onFavoriteClick: () -> Unit,
    onNextEpisode: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSelectAudioTrack: (androidx.media3.common.Tracks.Group, Int) -> Unit,
    onClearAudioTrack: () -> Unit,
    onToggleCaptions: () -> Unit,
    onSelectQuality: (com.sirktv.app.domain.model.StreamQuality) -> Unit
) {
    var activeSheet by remember { mutableStateOf<VodSheet?>(null) }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))) {
        // The seek bar, timestamps, and action row sit directly on the video
        // frame — a bright scene underneath can wash out plain white text,
        // so the bottom third always gets a dark gradient behind it on top
        // of the overlay's base dim.
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.3f to Color.Black.copy(alpha = 0.5f),
                        1f to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )

        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical)
        ) {
            Column(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().glassCard().padding(Dimens.SpaceMd)) {
                Text(uiState.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                uiState.subtitle?.let {
                    Text(it, color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
            ) {
                SeekBar(positionMs = uiState.positionMs, durationMs = uiState.durationMs)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDurationMs(uiState.positionMs), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    if (uiState.captionsEnabled) {
                        Text("CC · ${uiState.captionLanguageLabel}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                    Text(formatDurationMs(uiState.durationMs), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                    ActionIcon(label = "⏪", onClick = onSeekBack)
                    ActionIcon(
                        label = if (uiState.playbackState is PlaybackState.Playing) "❚❚" else "▶",
                        onClick = onPlayPause
                    )
                    ActionIcon(label = "⏩", onClick = onSeekForward)
                    ActionIcon(label = if (uiState.isFavorite) "♥" else "♡", active = uiState.isFavorite, onClick = onFavoriteClick)
                    ActionIcon(label = "AUD", onClick = { activeSheet = VodSheet.AUDIO })
                    ActionIcon(label = "CC", active = uiState.captionsEnabled, onClick = onToggleCaptions)
                    ActionIcon(label = "HD", onClick = { activeSheet = VodSheet.QUALITY })
                    if (uiState.isEpisode && uiState.hasNextEpisode) {
                        ActionIcon(label = "⏭", onClick = onNextEpisode)
                    }
                }
            }
        }
    }

    when (activeSheet) {
        VodSheet.AUDIO -> TrackSelectorSheet(
            title = "Audio",
            tracks = tracks,
            trackType = TrackTypes.AUDIO,
            allowOff = false,
            onSelect = onSelectAudioTrack,
            onClear = onClearAudioTrack,
            onDismiss = { activeSheet = null }
        )
        VodSheet.QUALITY -> QualitySelectorSheet(
            current = uiState.preferredQuality,
            onSelect = { onSelectQuality(it); activeSheet = null },
            onDismiss = { activeSheet = null }
        )
        null -> Unit
    }
}

@Composable
private fun SeekBar(positionMs: Long, durationMs: Long) {
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    Box(Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(999.dp))) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxSize().background(SirKTVPrimary, RoundedCornerShape(999.dp)))
    }
}

@Composable
private fun ActionIcon(label: String, active: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.size(40.dp).tvFocusStyle(cornerRadius = 20.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (active) SirKTVPrimary else Color.White.copy(alpha = 0.12f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp))
        }
    }
}

@Composable
private fun BufferingOverlay(title: String) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            SirKTVLogoMark()
            if (title.isNotBlank()) Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = SirKTVPrimary)
        }
    }
}

@Composable
private fun ReconnectingOverlay() {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        Text("Reconnecting…", color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun ErrorOverlay(title: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            Text(
                text = "Playback interrupted${if (title.isNotBlank()) " — $title" else ""}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Button(onClick = onRetry, modifier = Modifier.padding(top = Dimens.SpaceSm).tvFocusStyle(accent = TvFocusAccent.BORDER)) {
                TvText("Retry")
            }
        }
    }
}

private fun formatDurationMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
