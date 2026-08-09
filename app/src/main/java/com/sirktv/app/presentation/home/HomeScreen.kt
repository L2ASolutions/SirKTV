package com.sirktv.app.presentation.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Surface
import androidx.tv.material3.Text as TvText
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.presentation.common.SirKTVChrome
import com.sirktv.app.presentation.common.SirKTVNavItem
import com.sirktv.app.presentation.common.TvFocusAccent
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.common.tvNoBorder
import com.sirktv.app.presentation.common.tvNoButtonBorder
import com.sirktv.app.presentation.livetv.ChannelCard
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVPrimaryVariant
import com.sirktv.app.presentation.theme.SirKTVSeriesAccent
import com.sirktv.app.presentation.theme.SirKTVSportsAccent
import com.sirktv.app.presentation.theme.SirKTVSurface
import com.sirktv.app.presentation.theme.SirKTVTextSecondary

private val HeroHeight = 280.dp
private val MiniTileWidth = 140.dp
private val MiniTileHeight = 80.dp
private val WideCardWidth = 300.dp
private val PosterCardWidth = 150.dp

// Deep near-black hero gradient, distinct from the flat app background so the
// hero reads as its own editorial surface — plus a faint blue glow on the
// right, behind the mini quick-access tiles, echoing brand color without
// competing with the "WELCOME BACK" copy on the left.
private val HeroGradientEnd = Color(0xFF0A0A1A)
private val HeroGlow = Color(0x0F0066FF)

private val EmptyStateIconColor = Color(0xFF333333)
private val EmptyStateTitleColor = Color(0xFF666666)
private val EmptyStateSubtitleColor = Color(0xFF444444)

/**
 * Home is a dashboard, not a launcher: a full-bleed hero (welcome copy + a
 * compact 2x2 quick-access grid) followed by up to three personal rows —
 * Continue Watching, My Favorites, Recently Watched — each pulled straight
 * from Room and only shown when it actually has data. The nav pill row
 * (rendered by [SirKTVChrome]) remains the primary way to reach every
 * section; the hero's mini tiles are a second, faster path to the four most
 * common destinations, not a replacement for it. No content sync ever
 * happens here — [HomeViewModel] only reads what's already cached locally.
 */
@Composable
fun HomeScreen(
    onOpenContent: (HomeNavTarget) -> Unit,
    onNavigate: (SirKTVNavItem) -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    // Home is effectively the root of the back stack after login — Back here
    // must never exit immediately, only after the user confirms.
    BackHandler(enabled = !showExitDialog) { showExitDialog = true }
    BackHandler(enabled = showExitDialog) { showExitDialog = false }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                HomeEvent.NavigateToLogin -> onLoggedOut()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(SirKTVBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Dimens.SafeAreaVertical),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)
        ) {
            item {
                Box(Modifier.padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical)) {
                    SirKTVChrome(activeItem = SirKTVNavItem.HOME, onNavigate = onNavigate, onRefresh = {})
                }
            }

            item {
                HomeHero(
                    displayName = displayName,
                    onNavigate = onNavigate,
                    modifier = Modifier.padding(horizontal = Dimens.SafeAreaHorizontal)
                )
            }

            if (state.continueWatching.isNotEmpty()) {
                item {
                    MediaRow(
                        title = "Continue Watching",
                        rowItems = state.continueWatching,
                        modifier = Modifier.padding(horizontal = Dimens.SafeAreaHorizontal)
                    ) { progress ->
                        MediaCard(
                            title = progress.title,
                            imageUrl = progress.imageUrl,
                            subtitle = progress.subtitle,
                            progressFraction = progress.progressFraction,
                            onClick = { viewModel.watchProgressTarget(progress)?.let(onOpenContent) },
                            modifier = Modifier.width(WideCardWidth)
                        )
                    }
                }
            }

            if (state.favorites.isNotEmpty()) {
                item {
                    HomeFavoritesRow(
                        favorites = state.favorites,
                        channelEpgCache = state.channelEpgCache,
                        onRequestEpg = viewModel::requestEpgFor,
                        onClick = { favorite -> onOpenContent(viewModel.favoriteTarget(favorite)) },
                        modifier = Modifier.padding(horizontal = Dimens.SafeAreaHorizontal)
                    )
                }
            }

            if (state.recentlyWatched.isNotEmpty()) {
                item {
                    MediaRow(
                        title = "Recently Watched",
                        rowItems = state.recentlyWatched,
                        modifier = Modifier.padding(horizontal = Dimens.SafeAreaHorizontal)
                    ) { progress ->
                        MediaCard(
                            title = progress.title,
                            imageUrl = progress.imageUrl,
                            aspectRatio = 2f / 3f,
                            onClick = { viewModel.watchProgressTarget(progress)?.let(onOpenContent) },
                            modifier = Modifier.width(PosterCardWidth)
                        )
                    }
                }
            }

            if (!state.hasAnyContent) {
                item {
                    HomeEmptyState(modifier = Modifier.padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SpaceLg))
                }
            }
        }

        if (showExitDialog) {
            ExitConfirmationDialog(
                onConfirm = { activity?.finish() },
                onDismiss = { showExitDialog = false }
            )
        }
    }
}

@Composable
private fun HomeHero(
    displayName: String?,
    onNavigate: (SirKTVNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HeroHeight)
            .clip(RoundedCornerShape(Dimens.CardCornerRadius))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(SirKTVBackground, HeroGradientEnd)))
        )
        // Subtle blue radial glow, biased toward the right where the mini
        // tiles sit — never strong enough to compete with the welcome copy.
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .size(420.dp)
                .background(Brush.radialGradient(listOf(HeroGlow, Color.Transparent)))
        )

        Row(
            modifier = Modifier.fillMaxSize().padding(Dimens.SpaceXl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(0.6f)) {
                Text(
                    text = "WELCOME BACK",
                    color = SirKTVPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = displayName ?: "there",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    text = "What would you like to watch today?",
                    color = SirKTVTextSecondary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Column(
                modifier = Modifier.weight(0.4f),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                horizontalAlignment = Alignment.End
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                    HeroMiniTile("Live TV", "📡", SirKTVPrimary) { onNavigate(SirKTVNavItem.LIVE_TV) }
                    HeroMiniTile("Movies", "🎬", SirKTVPrimaryVariant) { onNavigate(SirKTVNavItem.MOVIES) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                    HeroMiniTile("Series", "📺", SirKTVSeriesAccent) { onNavigate(SirKTVNavItem.SERIES) }
                    HeroMiniTile("Sports", "🏆", SirKTVSportsAccent) { onNavigate(SirKTVNavItem.SPORTS) }
                }
            }
        }
    }
}

/**
 * A second, faster path to the four most common destinations — not a
 * replacement for the nav pills. Focus is scale (1.05x) + a glow tinted the
 * tile's own accent color, never a border, matching the rest of the app.
 */
@Composable
private fun HeroMiniTile(label: String, icon: String, accentColor: Color, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.05f else 1f, label = "heroTileScale")
    val glowElevation by animateDpAsState(targetValue = if (isFocused) 10.dp else 0.dp, label = "heroTileGlow")

    Surface(
        border = tvNoBorder(),
        onClick = onClick,
        modifier = Modifier
            .width(MiniTileWidth)
            .height(MiniTileHeight)
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = glowElevation,
                shape = RoundedCornerShape(8.dp),
                clip = false,
                ambientColor = accentColor,
                spotColor = accentColor
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(SirKTVSurface, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(icon, fontSize = 20.sp, color = accentColor)
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Mixed favorite channels/movies/series in one horizontal shelf — the same card widgets used everywhere else in the app. */
@Composable
private fun HomeFavoritesRow(
    favorites: List<HomeFavoriteItem>,
    channelEpgCache: Map<String, EpgNowNext>,
    onRequestEpg: (String) -> Unit,
    onClick: (HomeFavoriteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        RowHeader(title = "My Favorites", count = favorites.size)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            items(favorites, key = ::favoriteKey) { favorite ->
                when (favorite) {
                    is HomeFavoriteItem.ChannelItem -> {
                        LaunchedEffect(favorite.channel.id) { onRequestEpg(favorite.channel.id) }
                        ChannelCard(
                            channel = favorite.channel,
                            nowNext = channelEpgCache[favorite.channel.id],
                            onClick = { onClick(favorite) },
                            modifier = Modifier.width(WideCardWidth)
                        )
                    }
                    is HomeFavoriteItem.MovieItem -> MediaCard(
                        title = favorite.movie.title,
                        imageUrl = favorite.movie.posterUrl,
                        aspectRatio = 2f / 3f,
                        rating = favorite.movie.rating,
                        isFavorite = true,
                        onClick = { onClick(favorite) },
                        modifier = Modifier.width(PosterCardWidth)
                    )
                    is HomeFavoriteItem.SeriesItem -> MediaCard(
                        title = favorite.series.title,
                        imageUrl = favorite.series.posterUrl,
                        aspectRatio = 2f / 3f,
                        rating = favorite.series.rating,
                        isFavorite = true,
                        onClick = { onClick(favorite) },
                        modifier = Modifier.width(PosterCardWidth)
                    )
                }
            }
        }
    }
}

private fun favoriteKey(item: HomeFavoriteItem): String = when (item) {
    is HomeFavoriteItem.ChannelItem -> "channel:${item.channel.id}"
    is HomeFavoriteItem.MovieItem -> "movie:${item.movie.id}"
    is HomeFavoriteItem.SeriesItem -> "series:${item.series.id}"
}

/** First-launch state: shown only when Continue Watching, Favorites, and Recently Watched are all empty. */
@Composable
private fun HomeEmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            Text("▶", fontSize = 48.sp, color = EmptyStateIconColor)
            Text(
                text = "Start watching to see your content here",
                color = EmptyStateTitleColor,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = Dimens.SpaceSm)
            )
            Text(
                text = "Browse Live TV, Movies or Series to get started",
                color = EmptyStateSubtitleColor,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ExitConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val noFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { noFocusRequester.requestFocus() }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .background(SirKTVSurface, RoundedCornerShape(16.dp))
                .padding(Dimens.SpaceLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
        ) {
            Text("Exit SirKTV?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                Button(
                    border = tvNoButtonBorder(),
                    onClick = onDismiss,
                    modifier = Modifier.focusRequester(noFocusRequester).tvFocusStyle(accent = TvFocusAccent.BORDER)
                ) {
                    TvText("No")
                }
                Button(border = tvNoButtonBorder(), onClick = onConfirm, modifier = Modifier.tvFocusStyle(accent = TvFocusAccent.BORDER)) {
                    TvText("Yes")
                }
            }
        }
    }
}
