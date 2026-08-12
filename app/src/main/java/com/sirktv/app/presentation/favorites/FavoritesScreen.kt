package com.sirktv.app.presentation.favorites

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Surface
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.home.MediaCard
import com.sirktv.app.presentation.home.PosterCardWidth
import com.sirktv.app.presentation.livetv.ChannelCard
import com.sirktv.app.presentation.theme.AppSurface
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.TextTertiary

private val LiveTvCardWidth = 320.dp

@Composable
fun FavoritesScreen(
    onLiveSelected: (channelId: String) -> Unit,
    onMovieSelected: (movieId: String) -> Unit,
    onSeriesSelected: (seriesId: String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val channelEpgCache by viewModel.channelEpgCache.collectAsState()

    var liveExpanded by remember { mutableStateOf(false) }
    var moviesExpanded by remember { mutableStateOf(false) }
    var seriesExpanded by remember { mutableStateOf(false) }

    // Hardware Back is handled centrally by SirKTVBackHandler (no sidebar to
    // navigate away with, so it goes to Home) — nothing screen-local needed here.

    Box(Modifier.fillMaxSize().background(SirKTVBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(SirKTVBackground),
            contentPadding = PaddingValues(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)
        ) {
            item {
                AccordionSection(title = "Live TV", count = uiState.channels.size, expanded = liveExpanded, onToggle = { liveExpanded = !liveExpanded }) {
                    if (uiState.channels.isEmpty()) {
                        EmptySectionText()
                    } else {
                        LazyRow(modifier = Modifier.background(SirKTVBackground), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
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
                        EmptySectionText()
                    } else {
                        LazyRow(modifier = Modifier.background(SirKTVBackground), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
                            items(uiState.movies, key = { it.id }) { movie ->
                                MediaCard(
                                    title = movie.title,
                                    imageUrl = movie.posterUrl,
                                    aspectRatio = 2f / 3f,
                                    rating = movie.rating,
                                    isFavorite = true,
                                    onClick = { onMovieSelected(movie.id) },
                                    onLongClick = { viewModel.onRemoveMovie(movie.id) },
                                    modifier = Modifier.width(PosterCardWidth)
                                )
                            }
                        }
                    }
                }
            }

            item {
                AccordionSection(title = "Favorite Series", count = uiState.series.size, expanded = seriesExpanded, onToggle = { seriesExpanded = !seriesExpanded }) {
                    if (uiState.series.isEmpty()) {
                        EmptySectionText()
                    } else {
                        LazyRow(modifier = Modifier.background(SirKTVBackground), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
                            items(uiState.series, key = { it.id }) { series ->
                                MediaCard(
                                    title = series.title,
                                    imageUrl = series.posterUrl,
                                    aspectRatio = 2f / 3f,
                                    rating = series.rating,
                                    isFavorite = true,
                                    onClick = { onSeriesSelected(series.id) },
                                    onLongClick = { viewModel.onRemoveSeries(series.id) },
                                    modifier = Modifier.width(PosterCardWidth)
                                )
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
    val chevronRotation by animateFloatAsState(targetValue = if (expanded) 0f else -90f, label = "accordionChevron")

    Column {
        Surface(
            border = com.sirktv.app.presentation.common.tvNoBorder(), glow = com.sirktv.app.presentation.common.tvNoGlow(),
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth().tvFocusStyle(cornerRadius = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(AppSurface, RoundedCornerShape(8.dp))
                    .padding(horizontal = Dimens.SpaceMd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("($count)", color = SirKTVOnSurfaceMuted, fontSize = 13.sp)
                }
                // Chevron points down expanded, right collapsed — animated
                // rotation instead of swapping glyphs.
                Text(
                    text = "▾",
                    color = SirKTVOnSurfaceMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.rotate(chevronRotation)
                )
            }
        }
        AnimatedVisibility(visible = expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Box(Modifier.padding(top = Dimens.SpaceSm)) {
                content()
            }
        }
    }
}

/** Shown instead of an empty row whenever a favorites section has nothing in it yet. */
@Composable
private fun EmptySectionText() {
    Text("Nothing here yet", color = TextTertiary, fontSize = 13.sp, modifier = Modifier.padding(vertical = Dimens.SpaceSm))
}
