package com.sirktv.app.presentation.livetv

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
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
import androidx.tv.material3.Surface
import androidx.tv.material3.Text as TvText
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.model.EpgProgram
import com.sirktv.app.player.PlaybackState
import com.sirktv.app.presentation.common.TvFocusAccent
import com.sirktv.app.presentation.common.tvChannelRowFocusStyle
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.common.tvNoBorder
import com.sirktv.app.presentation.common.tvNoButtonBorder
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVCardBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceStrong
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVSurfaceElevated
import com.sirktv.app.presentation.theme.SirKTVTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MiniPlayerWidth = 340.dp
private val MiniPlayerVideoHeight = 200.dp
private val MiniPlayerFavoriteBg = SirKTVSurfaceElevated
private val MiniPlayerEndTimeColor = SirKTVTextTertiary

/**
 * The Live TV browse screen's third column: a real, fully-audible mini
 * player (TiviMate/IPTV Smarters pattern) that only appears after the user
 * presses OK on a channel row — never a muted always-on preview. [player]/
 * [playerState] are owned by [LiveTvBrowseViewModel]; this composable only
 * renders whatever they currently report. [onDismiss] hides the panel and
 * stops playback without navigating away; [onExpandFullScreen] hands the
 * same channel off to the dedicated player route.
 */
@Composable
fun MiniPlayerPanel(
    channel: Channel,
    nowNext: EpgNowNext?,
    listings: List<EpgProgram>,
    player: ExoPlayer?,
    playerState: PlaybackState,
    isFavorite: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    onRequestListings: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
    onExpandFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(channel.id) { onRequestListings(channel.id) }

    Column(
        modifier = modifier
            .width(MiniPlayerWidth)
            .fillMaxHeight()
            .padding(Dimens.SpaceLg)
            // Left always leaves the panel back to the channel list, no
            // matter which element inside currently has focus — intercepted
            // here, in the capture phase, before any child gets a chance.
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (event.key == Key.DirectionLeft) {
                    leftFocusRequester.requestFocus()
                    true
                } else false
            },
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
    ) {
        MiniPlayerVideo(
            channel = channel,
            nowNext = nowNext,
            player = player,
            playerState = playerState,
            focusRequester = focusRequester,
            onDismiss = onDismiss,
            onExpandFullScreen = onExpandFullScreen
        )

        Text(channel.name, color = SirKTVOnSurfaceStrong, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)

        nowNext?.now?.let { now ->
            Text(now.title, color = SirKTVOnSurfaceMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            MiniPlayerProgressBar(now, modifier = Modifier.padding(top = 6.dp))
            Text(
                text = "Ends ${formatClockTime(now.endEpochSeconds)}",
                color = MiniPlayerEndTimeColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm), modifier = Modifier.padding(top = Dimens.SpaceXs)) {
            Button(
                border = tvNoButtonBorder(),
                onClick = onExpandFullScreen,
                colors = ButtonDefaults.colors(containerColor = SirKTVPrimary, contentColor = Color.White),
                modifier = Modifier.weight(1f).height(44.dp).tvFocusStyle(accent = TvFocusAccent.BORDER, cornerRadius = 8.dp)
            ) {
                TvText("⤢ Full Screen", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                border = tvNoButtonBorder(),
                onClick = onToggleFavorite,
                colors = ButtonDefaults.colors(
                    containerColor = if (isFavorite) SirKTVPrimary else MiniPlayerFavoriteBg,
                    contentColor = Color.White
                ),
                modifier = Modifier.weight(1f).height(44.dp).tvFocusStyle(accent = TvFocusAccent.BORDER, cornerRadius = 8.dp)
            ) {
                TvText(if (isFavorite) "❤ Saved" else "♡ Favorite", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text("Schedule", color = SirKTVOnSurfaceStrong, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = Dimens.SpaceSm))

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

/** Currently-airing program first (if still within [listings]), then up to 7 more upcoming, chronological. */
private fun upcomingSchedule(listings: List<EpgProgram>): List<EpgProgram> {
    val nowSeconds = System.currentTimeMillis() / 1000
    return listings.filter { it.endEpochSeconds > nowSeconds }.sortedBy { it.startEpochSeconds }.take(8)
}

@Composable
private fun MiniPlayerVideo(
    channel: Channel,
    nowNext: EpgNowNext?,
    player: ExoPlayer?,
    playerState: PlaybackState,
    focusRequester: FocusRequester,
    onDismiss: () -> Unit,
    onExpandFullScreen: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MiniPlayerVideoHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .focusRequester(focusRequester)
            .tvFocusStyle(accent = TvFocusAccent.BORDER, cornerRadius = 8.dp)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter)) {
                    onExpandFullScreen()
                    true
                } else false
            }
    ) {
        // Gated behind a null check — the mini player is built lazily and
        // defensively (see LiveTvBrowseViewModel.initMiniPlayer), so it may
        // genuinely be null (not yet built, or failed to build) at any point.
        // androidViewFailed additionally guards the PlayerView/AndroidView
        // interop itself — if the native view ever fails to construct or bind
        // this flips permanently for this composition and the placeholder
        // takes over instead of retrying a broken view.
        var androidViewFailed by remember(channel.id) { mutableStateOf(false) }

        if (player != null && playerState !is PlaybackState.Error && !androidViewFailed) {
            AndroidView(
                factory = { context ->
                    runCatching {
                        PlayerView(context).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    }.getOrElse {
                        androidViewFailed = true
                        PlayerView(context)
                    }
                },
                update = { view ->
                    runCatching { view.player = player }
                        .onFailure { androidViewFailed = true }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        when {
            androidViewFailed || playerState is PlaybackState.Error -> MiniPlayerUnavailablePlaceholder(channel)
            playerState is PlaybackState.Buffering || playerState is PlaybackState.Idle -> {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SirKTVPrimary, modifier = Modifier.size(32.dp))
                }
            }
            else -> Unit
        }

        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(LiveTvLiveRed, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        MiniPlayerIconButton(
            icon = "✕",
            contentDescription = "Close mini player",
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(channel.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    nowNext?.now?.let {
                        Text(it.title, color = SirKTVOnSurfaceMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                MiniPlayerIconButton(icon = "⤢", contentDescription = "Expand to full screen", onClick = onExpandFullScreen)
            }
        }
    }
}

@Composable
private fun MiniPlayerIconButton(icon: String, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        border = tvNoBorder(),
        onClick = onClick,
        modifier = modifier.size(28.dp).tvFocusStyle(cornerRadius = 14.dp)
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MiniPlayerUnavailablePlaceholder(channel: Channel) {
    Box(Modifier.fillMaxSize().background(SirKTVCardBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            LiveTvChannelLogo(channel = channel, showLiveBadge = false)
            Text("Stream unavailable", color = SirKTVOnSurfaceMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MiniPlayerProgressBar(program: EpgProgram, modifier: Modifier = Modifier) {
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
