package com.sirktv.app.presentation.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.sirktv.app.presentation.common.SirKTVNavItem
import com.sirktv.app.presentation.common.TvFocusAccent
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.common.tvNoBorder
import com.sirktv.app.presentation.common.tvNoButtonBorder
import com.sirktv.app.presentation.livetv.ChannelCard
import com.sirktv.app.presentation.navigation.SirKTVIcons
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVCardBackground
import com.sirktv.app.presentation.theme.SirKTVSurface
import com.sirktv.app.presentation.theme.SirKTVSurfaceElevated
import com.sirktv.app.presentation.theme.SirKTVTextPrimary
import com.sirktv.app.presentation.theme.SirKTVTextSecondary
import com.sirktv.app.presentation.theme.SirKTVTextTertiary
import java.util.Calendar

private val WideCardWidth = 300.dp
private val PosterCardWidth = 150.dp

private val EmptyStateIconColor = SirKTVTextTertiary
private val EmptyStateTitleColor = SirKTVTextSecondary
private val EmptyStateSubtitleColor = SirKTVTextTertiary

private val LiveTvTileAccent = Color(0xFF1F6FEB)
private val MoviesTileAccent = Color(0xFF7C3AED)
private val SeriesTileAccent = Color(0xFF0891B2)

/**
 * Home is a personal dashboard (time-aware greeting, Continue Watching, My
 * Favorites, Recently Watched — each pulled straight from Room and only
 * shown when it actually has data) plus three section launcher tiles
 * (Live TV/Movies/Series) at the top, mirroring the always-visible sidebar's
 * top three destinations as one-tap shortcuts. No content sync ever happens
 * here — [HomeViewModel] only reads what's already cached locally.
 */
@Composable
fun HomeScreen(
    onOpenContent: (HomeNavTarget) -> Unit,
    onNavigateToSection: (SirKTVNavItem) -> Unit,
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
            contentPadding = PaddingValues(
                horizontal = Dimens.SafeAreaHorizontal,
                vertical = Dimens.SafeAreaVertical
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)
        ) {
            item {
                Text(
                    text = "${timeOfDayGreeting()}, ${displayName ?: "there"}",
                    color = SirKTVTextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                HomeSectionTiles(onNavigate = onNavigateToSection)
            }

            if (state.continueWatching.isNotEmpty()) {
                item {
                    MediaRow(title = "Continue Watching", rowItems = state.continueWatching) { progress ->
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
                        onClick = { favorite -> onOpenContent(viewModel.favoriteTarget(favorite)) }
                    )
                }
            }

            if (state.recentlyWatched.isNotEmpty()) {
                item {
                    MediaRow(title = "Recently Watched", rowItems = state.recentlyWatched) { progress ->
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
                    HomeEmptyState(modifier = Modifier.padding(vertical = Dimens.SpaceLg))
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

private fun timeOfDayGreeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

/** One-tap shortcuts to the sidebar's top three destinations — stacked full-width, 80dp tall each. */
@Composable
private fun HomeSectionTiles(onNavigate: (SirKTVNavItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        HomeSectionTile(
            title = "Live TV",
            subtitle = "Browse live channels",
            accentColor = LiveTvTileAccent,
            icon = { tint, mod -> SirKTVIcons.LiveTv(tint, mod) },
            onClick = { onNavigate(SirKTVNavItem.LIVE_TV) }
        )
        HomeSectionTile(
            title = "Movies",
            subtitle = "Browse the movie library",
            accentColor = MoviesTileAccent,
            icon = { tint, mod -> SirKTVIcons.Movies(tint, mod) },
            onClick = { onNavigate(SirKTVNavItem.MOVIES) }
        )
        HomeSectionTile(
            title = "Series",
            subtitle = "Browse TV series",
            accentColor = SeriesTileAccent,
            icon = { tint, mod -> SirKTVIcons.Series(tint, mod) },
            onClick = { onNavigate(SirKTVNavItem.SERIES) }
        )
    }
}

@Composable
private fun HomeSectionTile(
    title: String,
    subtitle: String,
    accentColor: Color,
    icon: @Composable (Color, Modifier) -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.03f else 1f, label = "homeTileScale")
    val rowBackground by animateColorAsState(
        targetValue = if (isFocused) SirKTVSurfaceElevated else SirKTVCardBackground,
        label = "homeTileBackground"
    )
    val glowElevation by animateDpAsState(targetValue = if (isFocused) Dimens.FocusGlowElevation else 0.dp, label = "homeTileGlow")
    val chevronColor by animateColorAsState(targetValue = if (isFocused) accentColor else SirKTVTextTertiary, label = "homeTileChevron")

    Surface(
        border = tvNoBorder(),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = glowElevation,
                shape = RoundedCornerShape(8.dp),
                clip = false,
                ambientColor = accentColor,
                spotColor = accentColor
            )
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(rowBackground),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(accentColor))
            Box(
                modifier = Modifier
                    .padding(start = Dimens.SpaceMd)
                    .size(48.dp)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                icon(accentColor, Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = Dimens.SpaceMd)) {
                Text(title, color = SirKTVTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Text(subtitle, color = SirKTVTextSecondary, fontSize = 12.sp)
            }
            Text(
                "❯",
                color = chevronColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = Dimens.SpaceMd)
            )
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
            Text("Exit SirKTV?", color = SirKTVTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
