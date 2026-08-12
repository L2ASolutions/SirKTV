package com.sirktv.app.presentation.livetv

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Surface
import androidx.tv.material3.Text as TvText
import coil.compose.AsyncImage
import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.model.EpgProgram
import com.sirktv.app.presentation.common.SectionErrorState
import com.sirktv.app.presentation.common.SectionLoadingState
import com.sirktv.app.presentation.common.SectionSearchButton
import com.sirktv.app.presentation.common.TvFocusAccent
import com.sirktv.app.presentation.common.tvChannelRowFocusStyle
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVCardBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceStrong
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVPrimaryContainer
import kotlinx.coroutines.delay

private val SidebarWidth = 220.dp
internal val ChannelListWidth = 340.dp
private val ChannelRowHeight = 64.dp
// Matches MiniPlayerPanel's own MiniPlayerWidth — kept as a separate constant
// here (rather than importing that private val) so this placeholder never
// resizes the panel when a channel is later activated.
private val MiniPlayerPanelWidth = 340.dp
internal val LiveTvLiveRed = Color(0xFFFF4757)
internal val LiveTvTrackGray = Color.White.copy(alpha = 0.15f)
internal val LiveTvActiveRowBackground = SirKTVPrimaryContainer

/**
 * Landing point for Live TV: a flat Xtream-category sidebar, a channel list,
 * and — only after the user explicitly presses OK on a channel — a real,
 * fully-audible mini player (TiviMate/IPTV Smarters pattern, not a muted
 * always-on preview). Nothing plays until that OK press; browsing/focus
 * alone never starts audio. "Full Screen" hands the same channel off to the
 * dedicated player route; this screen never auto-tunes it on its own.
 */
@Composable
fun LiveTvBrowseScreen(
    onChannelSelected: (channelId: String) -> Unit,
    onSearch: () -> Unit,
    viewModel: LiveTvBrowseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val miniPlayer by viewModel.miniPlayer.collectAsState()
    val miniPlayerState by viewModel.miniPlayerState.collectAsState()

    // Local override only for the one case the central NavHost-level Back
    // handler (SirKTVBackHandler) can't know about: an open mini player must
    // be dismissed by the first Back press rather than immediately leaving
    // the screen. Disabled (and so a no-op) the instant no mini player is
    // active, at which point the central handler's "no sidebar, so Back goes
    // to Home" behavior takes over untouched.
    BackHandler(enabled = uiState.activeChannelId != null) {
        viewModel.dismissMiniPlayer()
    }

    // The mini player ExoPlayer is a screen-scoped resource, independent of
    // the fullscreen SirKTVPlayerEngine singleton — it must stop the moment
    // this screen leaves composition, not just when the ViewModel is cleared
    // (the nav back-stack entry can outlive that if the user is routed
    // elsewhere without popping this destination). This is also what stops
    // this screen's audio the instant "Full Screen" pushes the player route.
    DisposableEffect(Unit) {
        onDispose { viewModel.releaseMiniPlayer() }
    }

    val sidebarFocusRequester = remember { FocusRequester() }
    val channelListFocusRequester = remember { FocusRequester() }
    val miniPlayerFocusRequester = remember { FocusRequester() }
    // D-pad Right from the category sidebar must land on a specific,
    // already-placed channel row, not the lazy container itself — see the
    // doc comment on LiveTvCategorySidebar's rightFocusRequester param.
    val firstChannelFocusRequester = remember { FocusRequester() }

    // Default focus on entry is the category list — no player of any kind is
    // ever built here. Wrapped so a crash anywhere in this entry sequence
    // lands as a logged diagnostic instead of taking the whole app down —
    // see "SirKTV_LiveTV" in logcat.
    LaunchedEffect(Unit) {
        try {
            delay(300)
            sidebarFocusRequester.requestFocus()
        } catch (e: Exception) {
            Log.e("SirKTV_LiveTV", "CRASH on Live TV entry: ${e.stackTraceToString()}")
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(SirKTVBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(com.sirktv.app.presentation.theme.AppSidebar)
                .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Live TV", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            SectionSearchButton(contentDescription = "Search channels", onClick = onSearch)
        }
        when {
            uiState.isError -> LiveTvCrashErrorState(onRetry = viewModel::retryAfterError)
            uiState.isLoading -> SectionLoadingState("Loading channels…")
            uiState.loadError != null && uiState.allChannels.isEmpty() -> SectionErrorState(
                error = uiState.loadError!!,
                onRetry = viewModel::refresh
            )
            uiState.allChannels.isEmpty() -> SectionLoadingState("Loading channels…")
            // Full width, edge-to-edge — no horizontal safe-zone padding on the columns themselves.
            else -> Row(Modifier.fillMaxSize()) {
                LiveTvCategorySidebar(
                    categories = uiState.categories,
                    counts = remember(uiState.allChannels) { uiState.allChannels.groupingBy { it.categoryId }.eachCount() },
                    totalCount = uiState.allChannels.size,
                    selectedCategoryId = uiState.selectedCategoryId,
                    focusRequester = sidebarFocusRequester,
                    rightFocusRequester = firstChannelFocusRequester,
                    rightFallbackFocusRequester = channelListFocusRequester,
                    onCategorySelected = viewModel::onCategorySelected
                )
                LiveTvChannelListColumn(
                    channels = uiState.visibleChannels,
                    selectedChannelId = uiState.selectedChannelId,
                    activeChannelId = uiState.activeChannelId,
                    nowPlayingChannelId = uiState.nowPlayingChannelId,
                    epgCache = uiState.epgCache,
                    focusRequester = channelListFocusRequester,
                    firstItemFocusRequester = firstChannelFocusRequester,
                    leftFocusRequester = sidebarFocusRequester,
                    rightFocusRequester = miniPlayerFocusRequester,
                    hasActiveChannel = uiState.activeChannel != null,
                    onVisible = viewModel::requestEpgFor,
                    onHighlight = viewModel::onChannelHighlighted,
                    onActivate = viewModel::onChannelActivated,
                    onToggleFavorite = viewModel::onToggleFavorite,
                    modifier = Modifier.weight(1f)
                )
                val activeChannel = uiState.activeChannel
                if (activeChannel != null) {
                    MiniPlayerPanel(
                        channel = activeChannel,
                        nowNext = uiState.epgCache[activeChannel.id],
                        listings = uiState.epgListingsCache[activeChannel.id].orEmpty(),
                        player = miniPlayer,
                        playerState = miniPlayerState,
                        isFavorite = activeChannel.isFavorite,
                        focusRequester = miniPlayerFocusRequester,
                        leftFocusRequester = channelListFocusRequester,
                        onRequestListings = viewModel::requestEpgListingsFor,
                        onToggleFavorite = { viewModel.onToggleFavorite(activeChannel.id) },
                        onDismiss = viewModel::dismissMiniPlayer,
                        onExpandFullScreen = {
                            viewModel.prewarmFullScreen(activeChannel.id)
                            onChannelSelected(activeChannel.id)
                        }
                    )
                } else {
                    MiniPlayerEmptyPanel()
                }
            }
        }
    }
        com.sirktv.app.presentation.common.BackHomeHint(modifier = Modifier.align(Alignment.TopStart))
    }
}

/** Third-panel placeholder shown before the user has pressed OK on any channel — keeps the three-panel layout fixed-width and always present, never a two-panel layout that shifts when a channel is later activated. */
@Composable
private fun MiniPlayerEmptyPanel() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(MiniPlayerPanelWidth)
            .background(com.sirktv.app.presentation.theme.AppSidebar),
        contentAlignment = Alignment.Center
    ) {
        Text("Select a channel", color = SirKTVOnSurfaceMuted, fontSize = 14.sp)
    }
}

/**
 * Full-screen crash guard: shown instead of the three-column browse layout
 * whenever [LiveTvBrowseUiState.isError] is set — i.e. something in this
 * screen's own init/collector logic threw, as opposed to an ordinary sync
 * failure (which uses [SectionErrorState] instead). Retry resets the error
 * flag and re-runs the ViewModel's init sync from scratch.
 */
@Composable
private fun LiveTvCrashErrorState(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            Text(
                text = "Live TV couldn't load",
                color = SirKTVOnSurfaceStrong,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Something went wrong opening Live TV. Try again.",
                color = SirKTVOnSurfaceMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = Dimens.SpaceXl)
            )
            Button(
                onClick = onRetry,
                border = com.sirktv.app.presentation.common.tvNoButtonBorder(),
                colors = androidx.tv.material3.ButtonDefaults.colors(containerColor = SirKTVPrimary, contentColor = Color.White),
                modifier = Modifier.tvFocusStyle(accent = TvFocusAccent.BORDER, cornerRadius = Dimens.ButtonCornerRadius)
            ) {
                TvText("Retry")
            }
        }
    }
}

/**
 * [rightFocusRequester] is attached directly to the first already-placed
 * channel row, not to the channel list's LazyColumn container — requesting
 * focus on the lazy container itself (relying on focusRestorer() to redirect
 * into its first child) is only reliable once some child of that container
 * has been focused at least once before; on the very first Right press, with
 * nothing yet remembered, that request can silently fail and leave focus
 * stuck here. [rightFallbackFocusRequester] (the container) is tried only if
 * the primary target isn't attached to anything, e.g. the channel list is
 * genuinely empty.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun LiveTvCategorySidebar(
    categories: List<Category>,
    counts: Map<String, Int>,
    totalCount: Int,
    selectedCategoryId: String?,
    focusRequester: FocusRequester,
    rightFocusRequester: FocusRequester,
    rightFallbackFocusRequester: FocusRequester,
    onCategorySelected: (String?) -> Unit
) {
    Box(Modifier.fillMaxHeight().width(SidebarWidth).background(com.sirktv.app.presentation.theme.AppSidebar)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .background(com.sirktv.app.presentation.theme.AppSidebar)
                .focusRequester(focusRequester)
                .focusRestorer()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (event.key == Key.DirectionRight) {
                        runCatching { rightFocusRequester.requestFocus() }
                            .onFailure { runCatching { rightFallbackFocusRequester.requestFocus() } }
                        true
                    } else false
                }
        ) {
            item {
                CategorySidebarRow(label = "All Channels", count = totalCount, selected = selectedCategoryId == null, onClick = { onCategorySelected(null) })
            }
            items(categories, key = { it.id }) { category ->
                CategorySidebarRow(
                    label = category.name,
                    count = counts[category.id] ?: 0,
                    selected = selectedCategoryId == category.id,
                    onClick = { onCategorySelected(category.id) }
                )
            }
        }
    }
}

private val CategoryRowSelectedBg = Color(0xFF1A2A4A)

@Composable
private fun CategorySidebarRow(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(targetValue = if (selected) CategoryRowSelectedBg else Color.Transparent, label = "categoryRowBg")

    Surface(border = com.sirktv.app.presentation.common.tvNoBorder(), glow = com.sirktv.app.presentation.common.tvNoGlow(), onClick = onClick, modifier = Modifier.fillMaxWidth().tvChannelRowFocusStyle(cornerRadius = 0.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(bgColor)
                .drawBehind {
                    if (selected) drawRect(color = SirKTVPrimary, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
                }
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Royal Blue — matches the same category-list label treatment on
            // Movies and Series; the left accent bar + background tint above
            // already carry the selected/unselected distinction.
            Text(
                text = label,
                color = SirKTVPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("Total: $count", color = SirKTVOnSurfaceMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun LiveTvChannelListColumn(
    channels: List<Channel>,
    selectedChannelId: String?,
    activeChannelId: String?,
    nowPlayingChannelId: String?,
    epgCache: Map<String, EpgNowNext>,
    focusRequester: FocusRequester,
    firstItemFocusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester,
    hasActiveChannel: Boolean,
    onVisible: (String) -> Unit,
    onHighlight: (String) -> Unit,
    onActivate: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Shared across every row's favorite heart: whichever heart last reported
    // focus, so the column-level Left/Right handler below knows whether the
    // currently focused element is a row body (exits the column) or a heart
    // (the row's own secondary focus zone, reached by default 2D search).
    var heartFocused by remember { mutableStateOf(false) }

    Box(modifier.fillMaxHeight()) {
        if (channels.isEmpty()) {
            Text(
                "No channels in this category yet.",
                color = SirKTVOnSurfaceMuted,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Center).padding(Dimens.SpaceMd)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .background(SirKTVBackground)
                    .focusRequester(focusRequester)
                    .focusRestorer()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            // Only hand off to the mini player's own focus
                            // zone when it's actually on screen — otherwise
                            // rightFocusRequester isn't attached to anything
                            // and requestFocus() would throw.
                            Key.DirectionRight -> if (heartFocused && hasActiveChannel) {
                                runCatching { rightFocusRequester.requestFocus() }; true
                            } else false
                            Key.DirectionLeft -> if (heartFocused) {
                                false
                            } else {
                                runCatching { leftFocusRequester.requestFocus() }; true
                            }
                            else -> false
                        }
                    }
            ) {
                items(channels, key = { it.id }) { channel ->
                    LaunchedEffect(channel.id) { onVisible(channel.id) }
                    LiveTvChannelRow(
                        channel = channel,
                        nowNext = epgCache[channel.id],
                        isHighlighted = channel.id == selectedChannelId,
                        isActive = channel.id == activeChannelId,
                        isNowPlayingFullscreen = channel.id == nowPlayingChannelId,
                        onFocusHighlighted = { onHighlight(channel.id) },
                        onActivate = { onActivate(channel.id) },
                        onToggleFavorite = { onToggleFavorite(channel.id) },
                        onHeartFocusChanged = { heartFocused = it },
                        modifier = if (channel.id == channels.first().id) Modifier.focusRequester(firstItemFocusRequester) else Modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveTvChannelRow(
    channel: Channel,
    nowNext: EpgNowNext?,
    isHighlighted: Boolean,
    isActive: Boolean,
    isNowPlayingFullscreen: Boolean,
    onFocusHighlighted: () -> Unit,
    onActivate: () -> Unit,
    onToggleFavorite: () -> Unit,
    onHeartFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val showAccent = isActive || isNowPlayingFullscreen
    val accentBarColor by animateColorAsState(targetValue = if (showAccent) SirKTVPrimary else Color.Transparent, label = "rowAccent")

    Row(modifier = Modifier.fillMaxWidth().height(ChannelRowHeight), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            border = com.sirktv.app.presentation.common.tvNoBorder(), glow = com.sirktv.app.presentation.common.tvNoGlow(),
            // OK/Select loads this channel into the mini player — browsing
            // (focus alone) never starts playback, matching TiviMate/IPTV
            // Smarters rather than this screen's old "preview on focus".
            onClick = onActivate,
            modifier = modifier
                .weight(1f)
                .fillMaxHeight()
                .tvChannelRowFocusStyle(cornerRadius = Dimens.CardCornerRadius)
                .onFocusChanged { if (it.isFocused) onFocusHighlighted() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isActive) LiveTvActiveRowBackground else Color.Transparent, RoundedCornerShape(Dimens.CardCornerRadius)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(4.dp).height(40.dp).background(accentBarColor, RoundedCornerShape(2.dp)))
                LiveTvChannelLogo(channel = channel, showLiveBadge = isActive || isNowPlayingFullscreen, modifier = Modifier.padding(start = 10.dp))
                Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(
                        text = channel.name,
                        color = SirKTVOnSurfaceStrong,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    LiveTvNowPlayingLine(nowNext)
                }
            }
        }
        LiveTvFavoriteHeart(
            isFavorite = channel.isFavorite,
            onToggle = onToggleFavorite,
            onFocusChanged = onHeartFocusChanged,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

@Composable
internal fun LiveTvChannelLogo(channel: Channel, showLiveBadge: Boolean, modifier: Modifier = Modifier) {
    val hasLogo = !channel.logoUrl.isNullOrBlank()
    Box(modifier.size(44.dp)) {
        if (hasLogo) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().clip(CircleShape).background(SirKTVCardBackground)
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().clip(CircleShape).background(SirKTVCardBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(channel.name.take(1).uppercase(), color = SirKTVPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (showLiveBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .background(LiveTvLiveRed, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text("LIVE", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LiveTvNowPlayingLine(nowNext: EpgNowNext?) {
    val now = nowNext?.now
    if (now != null) {
        Text(
            text = now.title,
            color = SirKTVOnSurfaceMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
        LiveTvEpgProgressBar(now, modifier = Modifier.padding(top = 3.dp))
    } else {
        Text("Live", color = LiveTvLiveRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
internal fun LiveTvEpgProgressBar(program: EpgProgram, modifier: Modifier = Modifier) {
    val fraction = programProgressFraction(program)
    Box(modifier.fillMaxWidth(0.85f).height(2.dp).background(LiveTvTrackGray, RoundedCornerShape(1.dp))) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(SirKTVPrimary, RoundedCornerShape(1.dp)))
    }
}

internal fun programProgressFraction(program: EpgProgram): Float {
    val total = (program.endEpochSeconds - program.startEpochSeconds).toFloat()
    if (total <= 0f) return 0f
    val nowSeconds = System.currentTimeMillis() / 1000
    return ((nowSeconds - program.startEpochSeconds).toFloat() / total).coerceIn(0f, 1f)
}

@Composable
internal fun LiveTvFavoriteHeart(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }
    var isFirstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(isFavorite) {
        if (isFirstComposition) {
            isFirstComposition = false
        } else {
            scale.animateTo(1.3f, tween(100))
            scale.animateTo(1f, tween(100))
        }
    }

    Surface(border = com.sirktv.app.presentation.common.tvNoBorder(), glow = com.sirktv.app.presentation.common.tvNoGlow(), 
        onClick = onToggle,
        modifier = modifier
            .size(40.dp)
            .tvFocusStyle(cornerRadius = 20.dp)
            .onFocusChanged { onFocusChanged(it.isFocused) }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (isFavorite) "♥" else "♡",
                color = if (isFavorite) SirKTVPrimary else SirKTVOnSurfaceMuted,
                fontSize = 20.sp,
                modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            )
        }
    }
}

/** Reused by SportsScreen (green accent) — kept exactly as-is; Sports is out of scope for the Live TV browse overhaul. */
@Composable
internal fun ChannelListColumn(
    channels: List<Channel>,
    categoryName: (String) -> String?,
    countryLabel: (String) -> String?,
    selectedChannelId: String?,
    epgCache: Map<String, EpgNowNext>,
    onVisible: (String) -> Unit,
    onHighlight: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    accentColor: Color = SirKTVPrimary
) {
    Box(Modifier.fillMaxHeight().width(ChannelListWidth)) {
        if (channels.isEmpty()) {
            Text(
                "No channels in this category yet.",
                color = SirKTVOnSurfaceMuted,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Center).padding(Dimens.SpaceMd)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().background(SirKTVBackground)
            ) {
                items(channels, key = { it.id }) { channel ->
                    LaunchedEffect(channel.id) { onVisible(channel.id) }
                    BrowseChannelRow(
                        channel = channel,
                        nowNext = epgCache[channel.id],
                        categoryLabel = categoryName(channel.categoryId),
                        countryLabel = countryLabel(channel.categoryId),
                        isSelected = channel.id == selectedChannelId,
                        onClick = { onHighlight(channel.id) },
                        onToggleFavorite = { onToggleFavorite(channel.id) },
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseChannelRow(
    channel: Channel,
    nowNext: EpgNowNext?,
    categoryLabel: String?,
    countryLabel: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    accentColor: Color = SirKTVPrimary
) {
    var isFocused by remember { mutableStateOf(false) }
    val accentBarWidth by animateDpAsState(
        targetValue = if (isFocused) Dimens.RowFocusAccentBarWidth else 3.dp,
        label = "channelRowAccentBarWidth"
    )
    val accentBarColor by animateColorAsState(
        targetValue = if (isFocused || isSelected) accentColor else Color.Transparent,
        label = "channelRowAccentBarColor"
    )

    Surface(border = com.sirktv.app.presentation.common.tvNoBorder(), glow = com.sirktv.app.presentation.common.tvNoGlow(), 
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().tvChannelRowFocusStyle(cornerRadius = 12.dp) { isFocused = it }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SirKTVCardBackground, RoundedCornerShape(12.dp))
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(accentBarWidth)
                    .height(36.dp)
                    .background(accentBarColor, RoundedCornerShape(2.dp))
            )
            Box(
                Modifier
                    .padding(start = 8.dp)
                    .size(36.dp)
                    .background(accentColor.copy(alpha = 0.85f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(channel.name.take(2).uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    text = channel.name,
                    color = SirKTVOnSurfaceStrong,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = nowNext?.now?.title ?: "Schedule unavailable",
                    color = SirKTVOnSurfaceMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 3.dp)) {
                    countryLabel?.let { InlineTag(it) }
                    categoryLabel?.let { InlineTag(it) }
                }
            }
            FavoriteHeartButton(isFavorite = channel.isFavorite, onToggle = onToggleFavorite, modifier = Modifier.padding(end = 8.dp))
        }
    }
}

@Composable
private fun InlineTag(text: String) {
    Box(Modifier.background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
        Text(text, color = SirKTVOnSurfaceMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Reused by SportsScreen (green accent) — kept exactly as-is; Live TV browse now uses [MiniPlayerPanel] instead. */
@Composable
internal fun PreviewPanel(
    channel: Channel?,
    nowNext: EpgNowNext?,
    listings: List<EpgProgram>,
    onRequestListings: (String) -> Unit,
    onWatchFullScreen: (Channel) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = SirKTVPrimary
) {
    Box(modifier.fillMaxHeight()) {
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
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Brush.linearGradient(listOf(accentColor.copy(alpha = 0.35f), Color.Black)), RoundedCornerShape(Dimens.CornerRadius))
            ) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(Color(0xFFFF4757), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier.align(Alignment.Center).size(56.dp).background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▶", color = Color.White, fontSize = 22.sp)
                }
            }

            Text(channel.name, color = SirKTVOnSurfaceStrong, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            nowNext?.now?.let { now ->
                Text(now.title, color = SirKTVOnSurfaceMuted, fontSize = 13.sp)
                ScheduleProgressBar(now, accentColor)
            }

            Button(border = com.sirktv.app.presentation.common.tvNoButtonBorder(), 
                onClick = { onWatchFullScreen(channel) },
                colors = androidx.tv.material3.ButtonDefaults.colors(containerColor = accentColor),
                modifier = Modifier.fillMaxWidth().tvFocusStyle(accent = TvFocusAccent.BORDER)
            ) {
                TvText("▶ Watch Full Screen")
            }

            Text("Schedule", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = Dimens.SpaceSm))
            LazyColumn(modifier = Modifier.background(SirKTVBackground), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listings.take(6), key = { it.startEpochSeconds }) { program ->
                    ScheduleRow(program = program, isNow = program == nowNext?.now, isNext = program == nowNext?.next, accentColor = accentColor)
                }
            }
        }
    }
}

@Composable
private fun ScheduleProgressBar(program: EpgProgram, accentColor: Color) {
    val total = (program.endEpochSeconds - program.startEpochSeconds).toFloat()
    val fraction = if (total > 0) {
        val nowSeconds = System.currentTimeMillis() / 1000
        ((nowSeconds - program.startEpochSeconds) / total).coerceIn(0f, 1f)
    } else 0f
    Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(999.dp))) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(accentColor, RoundedCornerShape(999.dp)))
    }
}

@Composable
private fun ScheduleRow(program: EpgProgram, isNow: Boolean, isNext: Boolean, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(formatEpochSeconds(program.startEpochSeconds), color = SirKTVOnSurfaceMuted, fontSize = 11.sp, modifier = Modifier.width(48.dp))
        Text(
            text = program.title,
            color = SirKTVOnSurfaceStrong,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        when {
            isNow -> Tag(text = "NOW", color = accentColor)
            isNext -> Tag(text = "NEXT", color = SirKTVOnSurfaceMuted)
        }
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    Box(Modifier.background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
