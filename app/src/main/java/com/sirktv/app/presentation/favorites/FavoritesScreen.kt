package com.sirktv.app.presentation.favorites

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Surface
import com.sirktv.app.presentation.common.SirKTVChrome
import com.sirktv.app.presentation.common.SirKTVNavItem
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.home.FavoriteToggleChip
import com.sirktv.app.presentation.home.MediaCard
import com.sirktv.app.presentation.livetv.ChannelCard
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted

private val PosterCardWidth = 150.dp
private val LiveTvCardWidth = 320.dp

@Composable
fun FavoritesScreen(
    onLiveSelected: (channelId: String) -> Unit,
    onMovieSelected: (movieId: String) -> Unit,
    onSeriesSelected: (seriesId: String) -> Unit,
    onNavigate: (SirKTVNavItem) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val channelEpgCache by viewModel.channelEpgCache.collectAsState()

    var liveExpanded by remember { mutableStateOf(false) }
    var moviesExpanded by remember { mutableStateOf(false) }
    var seriesExpanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(SirKTVBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)
        ) {
            item {
                SirKTVChrome(activeItem = SirKTVNavItem.FAVORITES, onNavigate = onNavigate, onRefresh = {})
            }

            item {
                AccordionSection(title = "Live TV", count = uiState.channels.size, expanded = liveExpanded, onToggle = { liveExpanded = !liveExpanded }) {
                    if (uiState.channels.isEmpty()) {
                        EmptySectionText("No favorite channels yet. Tap the heart on any channel to add it here.")
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
                            items(uiState.channels, key = { it.id }) { channel ->
                                LaunchedEffect(channel.id) { viewModel.requestEpgFor(channel.id) }
                                ChannelCard(
                                    channel = channel,
                                    nowNext = channelEpgCache[channel.id],
                                    onClick = { onLiveSelected(channel.id) },
                                    onToggleFavorite = { viewModel.onRemoveChannel(channel.id) },
                                    modifier = Modifier.width(LiveTvCardWidth)
                                )
                            }
                        }
                    }
                }
            }

            item {
                AccordionSection(title = "Favorite Movies", count = uiState.movies.size, expanded = moviesExpanded, onToggle = { moviesExpanded = !moviesExpanded }) {
                    if (uiState.movies.isEmpty()) {
                        EmptySectionText("No favorite movies yet. Tap the heart on any movie to add it here.")
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
                            items(uiState.movies, key = { it.id }) { movie ->
                                Column(modifier = Modifier.width(PosterCardWidth)) {
                                    MediaCard(
                                        title = movie.title,
                                        imageUrl = movie.posterUrl,
                                        aspectRatio = 2f / 3f,
                                        rating = movie.rating,
                                        isFavorite = true,
                                        onClick = { onMovieSelected(movie.id) }
                                    )
                                    FavoriteToggleChip(
                                        isFavorite = true,
                                        onToggle = { viewModel.onRemoveMovie(movie.id) },
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                AccordionSection(title = "Favorite Series", count = uiState.series.size, expanded = seriesExpanded, onToggle = { seriesExpanded = !seriesExpanded }) {
                    if (uiState.series.isEmpty()) {
                        EmptySectionText("No favorite series yet. Tap the heart on any series to add it here.")
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
                            items(uiState.series, key = { it.id }) { series ->
                                Column(modifier = Modifier.width(PosterCardWidth)) {
                                    MediaCard(
                                        title = series.title,
                                        imageUrl = series.posterUrl,
                                        aspectRatio = 2f / 3f,
                                        rating = series.rating,
                                        isFavorite = true,
                                        onClick = { onSeriesSelected(series.id) }
                                    )
                                    FavoriteToggleChip(
                                        isFavorite = true,
                                        onToggle = { viewModel.onRemoveSeries(series.id) },
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccordionSection(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Surface(onClick = onToggle, modifier = Modifier.fillMaxWidth().tvFocusStyle(cornerRadius = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$title ($count)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(if (expanded) "▾" else "▸", color = SirKTVOnSurfaceMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        AnimatedVisibility(visible = expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Box(Modifier.padding(top = Dimens.SpaceSm)) {
                content()
            }
        }
    }
}

@Composable
private fun EmptySectionText(message: String) {
    Text(message, color = SirKTVOnSurfaceMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = Dimens.SpaceSm))
}
