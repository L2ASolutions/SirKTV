package com.sirktv.app.presentation.livetv

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
import com.sirktv.app.presentation.common.SirKTVChrome
import com.sirktv.app.presentation.common.SirKTVNavItem
import com.sirktv.app.presentation.common.SirKTVLogoMark
import com.sirktv.app.presentation.common.TvFocusAccent
import com.sirktv.app.presentation.common.tvChannelRowFocusStyle
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVCardBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceStrong
import com.sirktv.app.presentation.theme.SirKTVPrimary
import kotlinx.coroutines.delay

private val SidebarWidth = 200.dp
internal val ChannelListWidth = 340.dp
private val ChannelRowHeight = 72.dp
internal val LiveTvLiveRed = Color(0xFFFF4757)
internal val LiveTvTrackGray = Color(0xFF333333)

/**
 * Landing point for Live TV: a flat Xtream-category sidebar, channel list,
 * and a real (muted, looping) preview panel so the user picks a channel
 * manually. Nothing plays unmuted until "Watch Full Screen" is pressed —
 * this screen never auto-tunes the fullscreen player on its own.
 */
@Composable
fun LiveTvBrowseScreen(
    onChannelSelected: (channelId: String) -> Unit,
    onNavigate: (SirKTVNavItem) -> Unit,
    viewModel: LiveTvBrowseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val previewPlayer by viewModel.previewPlayer.collectAsState()
    val previewState by viewModel.previewState.collectAsState()

    // The preview ExoPlayer is a screen-scoped resource, independent of the
    // fullscreen SirKTVPlayerEngine singleton — it must stop the moment this
    // screen leaves composition, not just when the ViewModel is cleared (the
    // nav back-stack entry can outlive that if the user is routed elsewhere
    // without popping this destination).
    DisposableEffect(Unit) {
        onDispose { viewModel.releasePreview() }
    }

    val sidebarFocusRequester = remember { FocusRequester() }
    val channelListFocusRequester = remember { FocusRequester() }
    val watchFullScreenFocusRequester = remember { FocusRequester() }

    // Preview ExoPlayer construction is deliberately triggered from here, not
    // from the ViewModel's init/constructor — this guarantees it only ever
    // happens once this screen is actually composed and alive.
    LaunchedEffect(Unit) {
        viewModel.initPreviewPlayer()
        delay(300)
        sidebarFocusRequester.requestFocus()
    }

    Column(Modifier.fillMaxSize().background(SirKTVBackground)) {
        Box(Modifier.padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical)) {
            SirKTVChrome(activeItem = SirKTVNavItem.LIVE_TV, onNavigate = onNavigate, onRefresh = viewModel::refresh)
        }

        when {
            uiState.isLoading -> LoadingChannelsState()
            uiState.loadError != null && uiState.allChannels.isEmpty() -> LoadErrorState(
                message = uiState.loadError!!,
                onRetry = viewModel::refresh
            )
            // Full width, edge-to-edge — no horizontal safe-zone padding on the three columns themselves.
            else -> Row(Modifier.fillMaxSize()) {
                LiveTvCategorySidebar(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    focusRequester = sidebarFocusRequester,
                    rightFocusRequester = channelListFocusRequester,
                    onCategorySelected = viewModel::onCategorySelected
                )
                LiveTvChannelListColumn(
                    channels = uiState.visibleChannels,
                    selectedChannelId = uiState.selectedChannelId,
                    nowPlayingChannelId = uiState.nowPlayingChannelId,
                    epgCache = uiState.epgCache,
                    focusRequester = channelListFocusRequester,
                    leftFocusRequester = sidebarFocusRequester,
                    rightFocusRequester = watchFullScreenFocusRequester,
                    onVisible = viewModel::requestEpgFor,
                    onHighlight = viewModel::onChannelHighlighted,
                    onToggleFavorite = viewModel::onToggleFavorite
                )
                LiveTvPreviewPanel(
                    channel = uiState.selectedChannel,
                    categoryLabel = uiState.categories.find { it.id == uiState.selectedChannel?.categoryId }?.name,
                    nowNext = uiState.selectedChannel?.let { uiState.epgCache[it.id] },
                    listings = uiState.selectedChannel?.let { uiState.epgListingsCache[it.id] }.orEmpty(),
                    previewPlayer = previewPlayer,
                    previewState = previewState,
                    watchFullScreenFocusRequester = watchFullScreenFocusRequester,
                    leftFocusRequester = channelListFocusRequester,
                    onRequestListings = viewModel::requestEpgListingsFor,
                    onToggleFavorite = { uiState.selectedChannel?.let { viewModel.onToggleFavorite(it.id) } },
                    onWatchFullScreen = { channel -> onChannelSelected(channel.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LoadingChannelsState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            SirKTVLogoMark()
            CircularProgressIndicator(color = SirKTVPrimary)
            Text("Loading channels…", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                "Fetching categories and channels from your provider",
                color = SirKTVOnSurfaceMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun LoadErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            Text("No channels available", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(message, color = SirKTVOnSurfaceMuted, fontSize = 13.sp)
            Button(onClick = onRetry, modifier = Modifier.padding(top = Dimens.SpaceSm).tvFocusStyle(accent = TvFocusAccent.BORDER)) {
                TvText("Retry")
            }
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun LiveTvCategorySidebar(
    categories: List<Category>,
    selectedCategoryId: String?,
    focusRequester: FocusRequester,
    rightFocusRequester: FocusRequester,
    onCategorySelected: (String?) -> Unit
) {
    Box(Modifier.fillMaxHeight().width(SidebarWidth).background(Color(0xEE0A0A0F))) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .focusRestorer()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (event.key == Key.DirectionRight) {
                        rightFocusRequester.requestFocus()
                        true
                    } else false
                }
                .padding(vertical = Dimens.SpaceMd, horizontal = Dimens.SpaceSm),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                Text(
                    text = "CATEGORIES",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp, start = 8.dp)
                )
            }
            item {
                CategorySidebarRow(label = "All Channels", selected = selectedCategoryId == null, onClick = { onCategorySelected(null) })
            }
            items(categories, key = { it.id }) { category ->
                CategorySidebarRow(label = category.name, selected = selectedCategoryId == category.id, onClick = { onCategorySelected(category.id) })
            }
        }
    }
}

@Composable
private fun CategorySidebarRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val accentBarColor by animateColorAsState(targetValue = if (selected) SirKTVPrimary else Color.Transparent, label = "categoryAccent")

    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().tvChannelRowFocusStyle(cornerRadius = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) SirKTVCardBackground else Color.Transparent, RoundedCornerShape(8.dp))
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(4.dp).height(18.dp).background(accentBarColor, RoundedCornerShape(2.dp)))
            Text(
                text = label,
                color = if (selected) SirKTVOnSurfaceStrong else SirKTVOnSurfaceMuted,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp)
            )
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun LiveTvChannelListColumn(
    channels: List<Channel>,
    selectedChannelId: String?,
    nowPlayingChannelId: String?,
    epgCache: Map<String, EpgNowNext>,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester,
    onVisible: (String) -> Unit,
    onHighlight: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    // Shared across every row's favorite heart: whichever heart last reported
    // focus, so the column-level Left/Right handler below knows whether the
    // currently focused element is a row body (exits the column) or a heart
    // (the row's own secondary focus zone, reached by default 2D search).
    var heartFocused by remember { mutableStateOf(false) }

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
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusRestorer()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionRight -> if (heartFocused) {
                                rightFocusRequester.requestFocus(); true
                            } else false
                            Key.DirectionLeft -> if (heartFocused) {
                                false
                            } else {
                                leftFocusRequester.requestFocus(); true
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
                        isSelected = channel.id == selectedChannelId,
                        isNowPlayingFullscreen = channel.id == nowPlayingChannelId,
                        onFocusSelected = { onHighlight(channel.id) },
                        onToggleFavorite = { onToggleFavorite(channel.id) },
                        onHeartFocusChanged = { heartFocused = it }
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
    isSelected: Boolean,
    isNowPlayingFullscreen: Boolean,
    onFocusSelected: () -> Unit,
    onToggleFavorite: () -> Unit,
    onHeartFocusChanged: (Boolean) -> Unit
) {
    val showAccent = isSelected || isNowPlayingFullscreen
    val accentBarColor by animateColorAsState(targetValue = if (showAccent) SirKTVPrimary else Color.Transparent, label = "rowAccent")

    Row(modifier = Modifier.fillMaxWidth().height(ChannelRowHeight), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            onClick = onFocusSelected,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .tvChannelRowFocusStyle(cornerRadius = Dimens.CardCornerRadius)
                // "Preview on focus" — no OK press required, matching every
                // other player screen in this app where D-pad navigation
                // alone drives what's on screen.
                .onFocusChanged { if (it.isFocused) onFocusSelected() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSelected) SirKTVCardBackground else Color.Transparent, RoundedCornerShape(Dimens.CardCornerRadius)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(4.dp).height(40.dp).background(accentBarColor, RoundedCornerShape(2.dp)))
                LiveTvChannelLogo(channel = channel, showLiveBadge = isSelected, modifier = Modifier.padding(start = 10.dp))
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
                    if (isNowPlayingFullscreen) {
                        Text(
                            "NOW PLAYING",
                            color = SirKTVPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
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
    Box(modifier.size(48.dp)) {
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

    Surface(
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
                modifier = Modifier.fillMaxSize()
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

    Surface(
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

/** Reused by SportsScreen (green accent) — kept exactly as-is; Live TV browse now uses [LiveTvPreviewPanel] instead. */
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

            Button(
                onClick = { onWatchFullScreen(channel) },
                colors = androidx.tv.material3.ButtonDefaults.colors(containerColor = accentColor),
                modifier = Modifier.fillMaxWidth().tvFocusStyle(accent = TvFocusAccent.BORDER)
            ) {
                TvText("▶ Watch Full Screen")
            }

            Text("Schedule", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = Dimens.SpaceSm))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
