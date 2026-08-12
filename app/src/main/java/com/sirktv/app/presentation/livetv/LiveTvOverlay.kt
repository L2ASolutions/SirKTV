package com.sirktv.app.presentation.livetv

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.player.PlaybackState
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVPrimary
import kotlinx.coroutines.delay

private enum class OverlaySheet { AUDIO, QUALITY }

@Composable
fun LiveTvOverlay(
    uiState: LiveTvUiState,
    onFavoriteClick: () -> Unit,
    onPlayPause: () -> Unit,
    onToggleCaptions: () -> Unit,
    onSelectAudioTrack: (androidx.media3.common.Tracks.Group, Int) -> Unit,
    onClearAudioTrack: () -> Unit,
    onSelectQuality: (com.sirktv.app.domain.model.StreamQuality) -> Unit,
    onChannelListToggle: () -> Unit,
    onChannelUp: () -> Unit,
    onChannelDown: () -> Unit,
    onClose: () -> Unit,
    tracks: androidx.media3.common.Tracks?
) {
    var activeSheet by remember { mutableStateOf<OverlaySheet?>(null) }
    val channel = uiState.currentChannel
    val context = LocalContext.current

    // Row 1 (playback) gets initial D-pad focus the instant the overlay
    // appears — LiveTvOverlay is only ever composed while the caller's
    // AnimatedVisibility has it visible (see LiveTvPlayerScreen), so a plain
    // LaunchedEffect(Unit) here fires exactly once per show.
    val playPauseFocus = remember { FocusRequester() }
    val ccFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { playPauseFocus.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
        // The now/next title, progress bar, and action row sit directly on
        // top of the live video frame (unlike the chips above, which already
        // carry their own dark pill background) — without this scrim, a
        // bright frame in the broadcast can make the white overlay text
        // unreadable, so it always gets a dark gradient behind it regardless
        // of what's currently playing.
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.3f to Color.Black.copy(alpha = 0.55f),
                        1f to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )

        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical)
        ) {
            Row(
                modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                channel?.let { ChannelChip(name = it.name, number = it.channelNumber) }
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    LiveDotBadge()
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
            ) {
                NowNextRow(uiState.nowNext)

                // ROW 1 — playback: previous channel, play/pause, current-
                // program progress, next channel. Down hands focus to Row 2's
                // CC button.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                                runCatching { ccFocus.requestFocus() }
                                true
                            } else false
                        }
                ) {
                    ActionIcon(label = "⏪", onClick = onChannelUp)
                    ActionIcon(
                        label = if (uiState.playbackState is PlaybackState.Playing) "❚❚" else "▶",
                        onClick = onPlayPause,
                        modifier = Modifier.focusRequester(playPauseFocus)
                    )
                    ProgressBar(uiState.nowNext, modifier = Modifier.weight(1f))
                    ActionIcon(label = "⏩", onClick = onChannelDown)
                }

                // ROW 2 — secondary: back, CC (status label, direct on/off
                // toggle — not a language picker, matches VodPlayerScreen's
                // identical CC button), Audio, Favorite, Quality, channel
                // list, then a spacer pushing PiP and explicit channel
                // up/down to the trailing edge. Up hands focus back to Row
                // 1's play/pause button.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                                runCatching { playPauseFocus.requestFocus() }
                                true
                            } else false
                        }
                ) {
                    ActionIcon(label = "←", onClick = onClose)
                    ActionIcon(
                        label = if (uiState.captionsEnabled) "CC ${uiState.captionLanguageLabel}" else "CC OFF",
                        active = uiState.captionsEnabled,
                        onClick = onToggleCaptions,
                        modifier = Modifier.focusRequester(ccFocus)
                    )
                    ActionIcon(label = "Audio", onClick = { activeSheet = OverlaySheet.AUDIO })
                    ActionIcon(label = if (channel?.isFavorite == true) "♥" else "♡", active = channel?.isFavorite == true, onClick = onFavoriteClick)
                    ActionIcon(label = "HD", onClick = { activeSheet = OverlaySheet.QUALITY })
                    ActionIcon(label = "☰", onClick = onChannelListToggle)
                    Spacer(Modifier.weight(1f))
                    if (context.supportsPip()) {
                        ActionIcon(label = "⧉", onClick = { (context as? Activity)?.enterSirKTVPip() })
                    }
                    ActionIcon(label = "▲", onClick = onChannelUp)
                    ActionIcon(label = "▼", onClick = onChannelDown)
                }
            }
        }
    }

    when (activeSheet) {
        OverlaySheet.AUDIO -> TrackSelectorSheet(
            title = "Audio",
            tracks = tracks,
            trackType = TrackTypes.AUDIO,
            allowOff = false,
            onSelect = onSelectAudioTrack,
            onClear = onClearAudioTrack,
            onDismiss = { activeSheet = null }
        )
        OverlaySheet.QUALITY -> QualitySelectorSheet(
            current = uiState.preferredQuality,
            onSelect = { onSelectQuality(it); activeSheet = null },
            onDismiss = { activeSheet = null }
        )
        null -> Unit
    }
}

@Composable
private fun ChannelChip(name: String, number: Int) {
    Row(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(24.dp)
                .background(SirKTVPrimary, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Text("CH $number", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun LiveDotBadge() {
    Row(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(6.dp).background(Color(0xFFFF4757), RoundedCornerShape(50)))
        Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NowNextRow(nowNext: EpgNowNext) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(
                text = nowNext.now?.title ?: "Program guide unavailable",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            nowNext.now?.let {
                Text(
                    "${formatEpochSeconds(it.startEpochSeconds)} – ${formatEpochSeconds(it.endEpochSeconds)}",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 11.sp
                )
            }
        }
        nowNext.next?.let {
            Column(horizontalAlignment = Alignment.End) {
                Text("UP NEXT", color = SirKTVPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(it.title, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ProgressBar(nowNext: EpgNowNext, modifier: Modifier = Modifier) {
    val now = nowNext.now
    val fraction = if (now != null && now.endEpochSeconds > now.startEpochSeconds) {
        val nowSeconds = System.currentTimeMillis() / 1000
        ((nowSeconds - now.startEpochSeconds).toFloat() / (now.endEpochSeconds - now.startEpochSeconds).toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier
            .height(3.dp)
            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(3.dp)
                .background(SirKTVPrimary, RoundedCornerShape(999.dp))
        )
    }
}

/** Icon-only (⏪/▶/⏩/←/▲/▼) labels stay pill-shaped-but-round via widthIn(min = height); status labels (CC OFF/CC EN) grow to fit instead of clipping. */
@Composable
private fun ActionIcon(label: String, active: Boolean = false, onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.tv.material3.Surface(border = com.sirktv.app.presentation.common.tvNoBorder(), glow = com.sirktv.app.presentation.common.tvNoGlow(),
        onClick = onClick,
        modifier = modifier.height(36.dp).widthIn(min = 36.dp).tvFocusStyle(cornerRadius = 18.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (active) SirKTVPrimary else Color.White.copy(alpha = 0.12f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}
