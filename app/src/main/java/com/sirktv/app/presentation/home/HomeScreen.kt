package com.sirktv.app.presentation.home

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Surface
import androidx.tv.material3.Text as TvText
import com.sirktv.app.presentation.common.SirKTVLogoMark
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVError
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVPrimaryVariant

private val LandscapeCardWidth = 220.dp
private val PosterCardWidth = 140.dp

@Composable
fun HomeScreen(
    onNavigate: (HomeNavTarget) -> Unit,
    onNavigateToSports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMovies: () -> Unit,
    onNavigateToSeries: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val profile by viewModel.profile.collectAsState()
    var infoMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToLiveTv -> onNavigate(HomeNavTarget.LiveTv(event.channelId))
                HomeEvent.NoChannelsAvailable -> infoMessage = "No live channels found on this account yet."
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
                HomeTopBar(
                    username = profile?.username,
                    onLiveTvClicked = viewModel::onLiveTvQuickLaunchClicked,
                    onMoviesClicked = onNavigateToMovies,
                    onSeriesClicked = onNavigateToSeries,
                    onSportsClicked = onNavigateToSports,
                    onFavoritesClicked = onNavigateToFavorites,
                    onSearchClicked = onNavigateToSearch,
                    onSettingsClicked = onNavigateToSettings,
                    onLogoutClicked = viewModel::onLogoutClicked
                )
            }

            item {
                HeroBanner(
                    title = state.heroTitle,
                    subtitle = state.heroSubtitle,
                    onPlayClicked = state.heroTarget?.let { target -> { onNavigate(target) } }
                )
            }

            infoMessage?.let { message ->
                item { Text(text = message, color = SirKTVError, fontSize = 13.sp) }
            }

            if (state.continueWatching.isNotEmpty()) {
                item {
                    MediaRow(title = "Continue Watching", rowItems = state.continueWatching) { progress ->
                        MediaCard(
                            title = progress.title,
                            imageUrl = progress.imageUrl,
                            subtitle = progress.subtitle,
                            progressFraction = progress.progressFraction,
                            onClick = { viewModel.watchProgressTarget(progress)?.let(onNavigate) },
                            modifier = Modifier.width(LandscapeCardWidth)
                        )
                    }
                }
            }

            if (state.favoriteChannels.isNotEmpty()) {
                item {
                    MediaRow(title = "Favorite Channels", rowItems = state.favoriteChannels) { channel ->
                        MediaCard(
                            title = channel.name,
                            imageUrl = channel.logoUrl,
                            isFavorite = true,
                            onClick = { onNavigate(HomeNavTarget.LiveTv(channel.id)) },
                            modifier = Modifier.width(LandscapeCardWidth)
                        )
                    }
                }
            }

            if (state.liveSportsNow.isNotEmpty()) {
                item {
                    MediaRow(title = "Live Sports Now", rowItems = state.liveSportsNow) { channel ->
                        MediaCard(
                            title = channel.name,
                            imageUrl = channel.logoUrl,
                            badge = "LIVE",
                            onClick = { onNavigate(HomeNavTarget.LiveTv(channel.id)) },
                            modifier = Modifier.width(LandscapeCardWidth)
                        )
                    }
                }
            }

            if (state.trendingMovies.isNotEmpty()) {
                item {
                    MediaRow(title = "Trending Movies", rowItems = state.trendingMovies) { movie ->
                        MediaCard(
                            title = movie.title,
                            imageUrl = movie.posterUrl,
                            aspectRatio = 2f / 3f,
                            isFavorite = movie.isFavorite,
                            onClick = { onNavigate(HomeNavTarget.MoviePlayer(movie.id)) },
                            modifier = Modifier.width(PosterCardWidth)
                        )
                    }
                }
            }

            if (state.popularSeries.isNotEmpty()) {
                item {
                    MediaRow(title = "Popular Series", rowItems = state.popularSeries) { series ->
                        MediaCard(
                            title = series.title,
                            imageUrl = series.posterUrl,
                            aspectRatio = 2f / 3f,
                            isFavorite = series.isFavorite,
                            onClick = { onNavigate(HomeNavTarget.SeriesDetail(series.id)) },
                            modifier = Modifier.width(PosterCardWidth)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    username: String?,
    onLiveTvClicked: () -> Unit,
    onMoviesClicked: () -> Unit,
    onSeriesClicked: () -> Unit,
    onSportsClicked: () -> Unit,
    onFavoritesClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onLogoutClicked: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            SirKTVLogoMark()
            Column(modifier = Modifier.weight(1f)) {
                Text("SirKTV", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                username?.let {
                    Text("Welcome back, $it", color = SirKTVOnSurfaceMuted, fontSize = 12.sp)
                }
            }
            Surface(onClick = onSearchClicked, modifier = Modifier.tvFocusStyle(cornerRadius = 20.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔍", fontSize = 16.sp)
                }
            }
        }
        val navButtons = listOf(
            "Live TV" to onLiveTvClicked,
            "Movies" to onMoviesClicked,
            "Series" to onSeriesClicked,
            "Sports" to onSportsClicked,
            "Favorites" to onFavoritesClicked,
            "Settings" to onSettingsClicked,
            "Log Out" to onLogoutClicked
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            items(navButtons) { (label, onClick) ->
                Button(onClick = onClick, modifier = Modifier.tvFocusStyle()) { TvText(label) }
            }
        }
    }
}

@Composable
private fun HeroBanner(title: String, subtitle: String, onPlayClicked: (() -> Unit)?) {
    Surface(
        onClick = { onPlayClicked?.invoke() },
        modifier = Modifier.fillMaxWidth().height(220.dp).tvFocusStyle()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(listOf(SirKTVPrimary.copy(alpha = 0.35f), SirKTVPrimaryVariant.copy(alpha = 0.25f))),
                    RoundedCornerShape(Dimens.CornerRadius)
                )
                .padding(Dimens.SpaceLg),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = SirKTVOnSurfaceMuted, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun <T> MediaRow(title: String, rowItems: List<T>, content: @Composable (T) -> Unit) {
    Column {
        RowHeader(title = title, count = rowItems.size)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            items(rowItems) { item -> content(item) }
        }
    }
}
