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
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.model.Series
import com.sirktv.app.presentation.common.FavoriteQuickActionsSheet
import com.sirktv.app.presentation.common.SirKTVLogoMark
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.livetv.ChannelCard
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVPrimaryVariant

private val LiveTvCardWidth = 300.dp
private val PosterCardWidth = 140.dp

/** What a long-press on a favorite row card is currently offering quick actions for. */
private sealed interface FavoriteQuickTarget {
    val title: String

    data class ChannelTarget(val channel: Channel) : FavoriteQuickTarget {
        override val title get() = channel.name
    }

    data class MovieTarget(val movie: Movie) : FavoriteQuickTarget {
        override val title get() = movie.title
    }

    data class SeriesTarget(val series: Series) : FavoriteQuickTarget {
        override val title get() = series.title
    }
}

@Composable
fun HomeScreen(
    onNavigate: (HomeNavTarget) -> Unit,
    onNavigateToLiveTvBrowse: () -> Unit,
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
    val channelEpgCache by viewModel.channelEpgCache.collectAsState()
    var quickActionsTarget by remember { mutableStateOf<FavoriteQuickTarget?>(null) }

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
                HomeTopBar(
                    username = profile?.username,
                    onLiveTvClicked = onNavigateToLiveTvBrowse,
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

            if (state.continueWatching.isNotEmpty()) {
                item {
                    MediaRow(title = "Continue Watching", rowItems = state.continueWatching) { progress ->
                        MediaCard(
                            title = progress.title,
                            imageUrl = progress.imageUrl,
                            subtitle = progress.subtitle,
                            progressFraction = progress.progressFraction,
                            onClick = { viewModel.watchProgressTarget(progress)?.let(onNavigate) },
                            modifier = Modifier.width(LiveTvCardWidth)
                        )
                    }
                }
            }

            if (state.liveChannels.isNotEmpty()) {
                item {
                    Column {
                        RowHeader(title = "Live TV", count = state.liveChannels.size)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
                            items(state.liveChannels, key = { it.id }) { channel ->
                                LaunchedEffect(channel.id) { viewModel.requestEpgFor(channel.id) }
                                ChannelCard(
                                    channel = channel,
                                    nowNext = channelEpgCache[channel.id],
                                    onClick = { onNavigate(HomeNavTarget.LiveTv(channel.id)) },
                                    modifier = Modifier.width(LiveTvCardWidth)
                                )
                            }
                        }
                    }
                }
            }

            if (state.favoriteChannels.isNotEmpty()) {
                item {
                    Column {
                        RowHeader(title = "Favorite Channels", count = state.favoriteChannels.size)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
                            items(state.favoriteChannels, key = { it.id }) { channel ->
                                LaunchedEffect(channel.id) { viewModel.requestEpgFor(channel.id) }
                                ChannelCard(
                                    channel = channel,
                                    nowNext = channelEpgCache[channel.id],
                                    onClick = { onNavigate(HomeNavTarget.LiveTv(channel.id)) },
                                    onLongClick = { quickActionsTarget = FavoriteQuickTarget.ChannelTarget(channel) },
                                    modifier = Modifier.width(LiveTvCardWidth).animateItem()
                                )
                            }
                        }
                    }
                }
            }

            if (state.favoriteMovies.isNotEmpty()) {
                item {
                    Column {
                        RowHeader(title = "Favorite Movies", count = state.favoriteMovies.size)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
                            items(state.favoriteMovies, key = { it.id }) { movie ->
                                MediaCard(
                                    title = movie.title,
                                    imageUrl = movie.posterUrl,
                                    aspectRatio = 2f / 3f,
                                    rating = movie.rating,
                                    isFavorite = true,
                                    onClick = { onNavigate(HomeNavTarget.MoviePlayer(movie.id)) },
                                    onLongClick = { quickActionsTarget = FavoriteQuickTarget.MovieTarget(movie) },
                                    modifier = Modifier.width(PosterCardWidth).animateItem()
                                )
                            }
                        }
                    }
                }
            }

            if (state.favoriteSeries.isNotEmpty()) {
                item {
                    Column {
                        RowHeader(title = "Favorite Series", count = state.favoriteSeries.size)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
                            items(state.favoriteSeries, key = { it.id }) { series ->
                                MediaCard(
                                    title = series.title,
                                    imageUrl = series.posterUrl,
                                    aspectRatio = 2f / 3f,
                                    rating = series.rating,
                                    isFavorite = true,
                                    onClick = { onNavigate(HomeNavTarget.SeriesDetail(series.id)) },
                                    onLongClick = { quickActionsTarget = FavoriteQuickTarget.SeriesTarget(series) },
                                    modifier = Modifier.width(PosterCardWidth).animateItem()
                                )
                            }
                        }
                    }
                }
            }

            if (state.trendingMovies.isNotEmpty()) {
                item {
                    MediaRow(title = "Movies", rowItems = state.trendingMovies) { movie ->
                        MediaCard(
                            title = movie.title,
                            imageUrl = movie.posterUrl,
                            aspectRatio = 2f / 3f,
                            rating = movie.rating,
                            isFavorite = movie.isFavorite,
                            onClick = { onNavigate(HomeNavTarget.MoviePlayer(movie.id)) },
                            modifier = Modifier.width(PosterCardWidth)
                        )
                    }
                }
            }

            if (state.popularSeries.isNotEmpty()) {
                item {
                    MediaRow(title = "TV Series", rowItems = state.popularSeries) { series ->
                        MediaCard(
                            title = series.title,
                            imageUrl = series.posterUrl,
                            aspectRatio = 2f / 3f,
                            rating = series.rating,
                            isFavorite = series.isFavorite,
                            onClick = { onNavigate(HomeNavTarget.SeriesDetail(series.id)) },
                            modifier = Modifier.width(PosterCardWidth)
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
                            modifier = Modifier.width(LiveTvCardWidth)
                        )
                    }
                }
            }
        }

        quickActionsTarget?.let { target ->
            FavoriteQuickActionsSheet(
                itemTitle = target.title,
                onPlay = {
                    when (target) {
                        is FavoriteQuickTarget.ChannelTarget -> onNavigate(HomeNavTarget.LiveTv(target.channel.id))
                        is FavoriteQuickTarget.MovieTarget -> onNavigate(HomeNavTarget.MoviePlayer(target.movie.id))
                        is FavoriteQuickTarget.SeriesTarget -> onNavigate(HomeNavTarget.SeriesDetail(target.series.id))
                    }
                },
                onRemove = {
                    when (target) {
                        is FavoriteQuickTarget.ChannelTarget -> viewModel.onToggleChannelFavorite(target.channel.id)
                        is FavoriteQuickTarget.MovieTarget -> viewModel.onToggleMovieFavorite(target.movie.id)
                        is FavoriteQuickTarget.SeriesTarget -> viewModel.onToggleSeriesFavorite(target.series.id)
                    }
                },
                onDismiss = { quickActionsTarget = null }
            )
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
            TopBarIcon(icon = "🔍", onClick = onSearchClicked)
            TopBarIcon(icon = "♥", onClick = onFavoritesClicked)
            TopBarIcon(icon = "⚙", onClick = onSettingsClicked)
        }
        val navButtons = listOf(
            "Live TV" to onLiveTvClicked,
            "Movies" to onMoviesClicked,
            "Series" to onSeriesClicked,
            "Sports" to onSportsClicked,
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
private fun TopBarIcon(icon: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.tvFocusStyle(cornerRadius = 20.dp)) {
        Box(
            modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 16.sp)
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
