package com.sirktv.app.presentation.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text as TvText
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.model.EpgProgram
import com.sirktv.app.player.PlaybackState
import com.sirktv.app.presentation.common.TvFocusAccent
import com.sirktv.app.presentation.common.tvChannelRowFocusStyle
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVCardBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceStrong
import com.sirktv.app.presentation.theme.SirKTVPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The right-hand column of the Live TV browse screen: a real, muted, looping
 * ExoPlayer preview of whichever channel is currently selected in the center
 * list, its EPG now/next info, and an up-to-8-item upcoming schedule.
 * [previewPlayer]/[previewState] are owned by [LiveTvBrowseViewModel] — this
 * composable only renders whatever they currently report.
 */
@Composable
fun LiveTvPreviewPanel(
    channel: Channel?,
    categoryLabel: String?,
    nowNext: EpgNowNext?,
    listings: List<EpgProgram>,
    previewPlayer: ExoPlayer?,
    previewState: PlaybackState,
    watchFullScreenFocusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    onRequestListings: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onWatchFullScreen: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxHeight()
            // Left always leaves the panel back to the channel list, no matter
            // which element inside currently has focus (video, heart, the
            // Watch Full Screen button, or a schedule row) — intercepted here,
            // in the capture phase, before any of those get a chance to see it.
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (event.key == Key.DirectionLeft) {
                    leftFocusRequester.requestFocus()
                    true
                } else false
            }
    ) {
        if (channel == null) {
            Text(
                "Select a channel to preview it here.",
                color = SirKTVOnSurfaceMuted,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        LaunchedEffect(channel.id) { onRequestListings(channel.id) }

        Column(
            modifier = Modifier.fillMaxSize().padding(Dimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
        ) {
            LiveTvPreviewVideo(
                channel = channel,
                previewPlayer = previewPlayer,
                previewState = previewState,
                onClick = { onWatchFullScreen(channel) }
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                LiveTvFavoriteHeart(isFavorite = channel.isFavorite, onToggle = onToggleFavorite, onFocusChanged = {})
            }

            categoryLabel?.let {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PreviewInfoChip(it)
                }
            }

            nowNext?.now?.let { now ->
                Text(now.title, color = SirKTVOnSurfaceStrong, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                PreviewProgressBar(now, modifier = Modifier.padding(top = 6.dp))
                Text(
                    text = "ends ${formatClockTime(now.endEpochSeconds)}",
                    color = SirKTVOnSurfaceMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Button(
                onClick = { onWatchFullScreen(channel) },
                colors = ButtonDefaults.colors(containerColor = SirKTVPrimary, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(top = Dimens.SpaceSm)
                    .focusRequester(watchFullScreenFocusRequester)
                    .tvFocusStyle(accent = TvFocusAccent.BORDER, cornerRadius = 12.dp)
            ) {
                TvText("▶ Watch Full Screen", fontWeight = FontWeight.Bold)
            }

            Text("Schedule", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = Dimens.SpaceSm))

            val upcoming = remember(listings, nowNext) { upcomingSchedule(listings) }
            if (upcoming.isEmpty()) {
                Text("No schedule available", color = SirKTVOnSurfaceMuted, fontSize = 12.sp)
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(upcoming, key = { it.startEpochSeconds }) { program ->
                        LiveTvScheduleRow(
                            program = program,
                            isNow = program == nowNext?.now,
                            isNext = program == nowNext?.next
                        )
                    }
                }
            }
        }
    }
}

/** Currently-airing program first (if still within [listings]), then up to 7 more upcoming, chronological. */
private fun upcomingSchedule(listings: List<EpgProgram>): List<EpgProgram> {
    val nowSeconds = System.currentTimeMillis() / 1000
    return listings.filter { it.endEpochSeconds > nowSeconds }.sortedBy { it.startEpochSeconds }.take(8)
}

@Composable
private fun LiveTvPreviewVideo(
    channel: Channel,
    previewPlayer: ExoPlayer?,
    previewState: PlaybackState,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(Dimens.CornerRadius))
            .background(Color.Black)
            .clickable(onClick = onClick)
            .tvFocusStyle(accent = TvFocusAccent.BORDER, cornerRadius = Dimens.CornerRadius)
    ) {
        // Gated behind a null check — the preview player is built lazily and
        // defensively (see LiveTvBrowseViewModel.initPreviewPlayer), so it may
        // genuinely be null (not yet built, or failed to build) at any point.
        if (previewPlayer != null && previewState !is PlaybackState.Error) {
            AndroidView(
                factory = { context ->
                    runCatching {
                        PlayerView(context).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    }.getOrElse { PlayerView(context) }
                },
                update = { view -> runCatching { view.player = previewPlayer } },
                modifier = Modifier.fillMaxSize()
            )
        }

        when (previewState) {
            is PlaybackState.Error -> PreviewUnavailablePlaceholder(channel)
            is PlaybackState.Buffering, is PlaybackState.Idle -> {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SirKTVPrimary)
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun PreviewUnavailablePlaceholder(channel: Channel) {
    Box(Modifier.fillMaxSize().background(SirKTVCardBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            LiveTvChannelLogo(channel = channel, showLiveBadge = false)
            Text("Preview unavailable", color = SirKTVOnSurfaceMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PreviewInfoChip(text: String) {
    Box(Modifier.background(SirKTVCardBackground, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(text, color = SirKTVOnSurfaceMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PreviewProgressBar(program: EpgProgram, modifier: Modifier = Modifier) {
    val fraction = programProgressFraction(program)
    Box(modifier.fillMaxWidth().height(4.dp).background(LiveTvTrackGray, RoundedCornerShape(8.dp))) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(SirKTVPrimary, RoundedCornerShape(8.dp)))
    }
}

@Composable
private fun LiveTvScheduleRow(program: EpgProgram, isNow: Boolean, isNext: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusable()
            .tvChannelRowFocusStyle(cornerRadius = 8.dp)
            .background(if (isNow) SirKTVCardBackground else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.width(3.dp).height(18.dp).background(if (isNow) SirKTVPrimary else Color.Transparent, RoundedCornerShape(2.dp)))
        Text(formatEpochSeconds(program.startEpochSeconds), color = SirKTVOnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.width(44.dp))
        Text(
            text = program.title,
            color = SirKTVOnSurfaceStrong,
            fontSize = 13.sp,
            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        when {
            isNow -> ScheduleBadge(text = "NOW", background = SirKTVPrimary, textColor = Color.White)
            isNext -> ScheduleBadge(text = "NEXT", background = LiveTvTrackGray, textColor = SirKTVOnSurfaceMuted)
        }
    }
}

@Composable
private fun ScheduleBadge(text: String, background: Color, textColor: Color) {
    Box(Modifier.background(background, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(text, color = textColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatClockTime(epochSeconds: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochSeconds * 1000))
