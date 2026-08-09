package com.sirktv.app.presentation.livetv

import android.app.Activity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVPrimary

private enum class OverlaySheet { AUDIO, SUBTITLE, QUALITY }

@Composable
fun LiveTvOverlay(
    uiState: LiveTvUiState,
    onFavoriteClick: () -> Unit,
    onSelectAudioTrack: (androidx.media3.common.Tracks.Group, Int) -> Unit,
    onClearAudioTrack: () -> Unit,
    onSelectSubtitleTrack: (androidx.media3.common.Tracks.Group, Int) -> Unit,
    onDisableSubtitles: () -> Unit,
    onSelectQuality: (com.sirktv.app.domain.model.StreamQuality) -> Unit,
    onChannelListToggle: () -> Unit,
    onChannelUp: () -> Unit,
    onChannelDown: () -> Unit,
    tracks: androidx.media3.common.Tracks?
) {
    var activeSheet by remember { mutableStateOf<OverlaySheet?>(null) }
    val channel = uiState.currentChannel
    val context = LocalContext.current

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
                ProgressBar(uiState.nowNext)
                ActionRow(
                    isFavorite = channel?.isFavorite == true,
                    onFavoriteClick = onFavoriteClick,
                    onAudioClick = { activeSheet = OverlaySheet.AUDIO },
                    onSubtitleClick = { activeSheet = OverlaySheet.SUBTITLE },
                    onQualityClick = { activeSheet = OverlaySheet.QUALITY },
                    onChannelListToggle = onChannelListToggle,
                    onChannelUp = onChannelUp,
                    onChannelDown = onChannelDown,
                    showPip = context.supportsPip(),
                    onPipClick = { (context as? Activity)?.enterSirKTVPip() }
                )
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
        OverlaySheet.SUBTITLE -> TrackSelectorSheet(
            title = "Subtitles",
            tracks = tracks,
            trackType = TrackTypes.TEXT,
            allowOff = true,
            onSelect = onSelectSubtitleTrack,
            onClear = onDisableSubtitles,
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
private fun ProgressBar(nowNext: EpgNowNext) {
    val now = nowNext.now
    val fraction = if (now != null && now.endEpochSeconds > now.startEpochSeconds) {
        val nowSeconds = System.currentTimeMillis() / 1000
        ((nowSeconds - now.startEpochSeconds).toFloat() / (now.endEpochSeconds - now.startEpochSeconds).toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        Modifier
            .fillMaxWidth()
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

@Composable
private fun ActionRow(
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onAudioClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onQualityClick: () -> Unit,
    onChannelListToggle: () -> Unit,
    onChannelUp: () -> Unit,
    onChannelDown: () -> Unit,
    showPip: Boolean,
    onPipClick: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        ActionIcon(label = if (isFavorite) "♥" else "♡", active = isFavorite, onClick = onFavoriteClick)
        ActionIcon(label = "AUD", onClick = onAudioClick)
        ActionIcon(label = "CC", onClick = onSubtitleClick)
        ActionIcon(label = "HD", onClick = onQualityClick)
        ActionIcon(label = "☰", onClick = onChannelListToggle)
        ActionIcon(label = "⌃", onClick = onChannelUp)
        ActionIcon(label = "⌄", onClick = onChannelDown)
        if (showPip) ActionIcon(label = "⧉", onClick = onPipClick)
    }
}

@Composable
private fun ActionIcon(label: String, active: Boolean = false, onClick: () -> Unit) {
    androidx.tv.material3.Surface(border = com.sirktv.app.presentation.common.tvNoBorder(), 
        onClick = onClick,
        modifier = Modifier.size(36.dp).tvFocusStyle(cornerRadius = 18.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (active) SirKTVPrimary else Color.White.copy(alpha = 0.12f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp))
        }
    }
}
