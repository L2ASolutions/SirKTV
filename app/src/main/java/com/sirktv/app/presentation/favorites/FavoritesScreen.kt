package com.sirktv.app.presentation.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.FavoriteItem
import com.sirktv.app.domain.model.FavoriteSort
import com.sirktv.app.presentation.common.CategoryPill
import com.sirktv.app.presentation.common.FavoriteQuickActionsSheet
import com.sirktv.app.presentation.home.MediaCard
import com.sirktv.app.presentation.livetv.ChannelCard
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary

@Composable
fun FavoritesScreen(
    onLiveSelected: (channelId: String) -> Unit,
    onMovieSelected: (movieId: String) -> Unit,
    onSeriesSelected: (seriesId: String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val channelEpgCache by viewModel.channelEpgCache.collectAsState()
    var quickActionsFor by remember { mutableStateOf<FavoriteItem?>(null) }

    fun openItem(item: FavoriteItem) {
        when (item.contentType) {
            ContentType.LIVE -> onLiveSelected(item.contentId)
            ContentType.MOVIE -> onMovieSelected(item.contentId)
            ContentType.SERIES -> onSeriesSelected(item.contentId)
            ContentType.EPISODE -> Unit
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SirKTVBackground)
                .padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical)
        ) {
            Text("Favorites", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)

            // D-pad navigable tab row — Left/Right moves focus between pills like any other LazyRow/Row of focusables.
            Row(
                modifier = Modifier.padding(top = Dimens.SpaceMd, bottom = Dimens.SpaceSm),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryPill(label = "Channels (${uiState.channelCount})", selected = uiState.selectedType == ContentType.LIVE) {
                    viewModel.onTypeSelected(ContentType.LIVE)
                }
                CategoryPill(label = "Movies (${uiState.movieCount})", selected = uiState.selectedType == ContentType.MOVIE) {
                    viewModel.onTypeSelected(ContentType.MOVIE)
                }
                CategoryPill(label = "Series (${uiState.seriesCount})", selected = uiState.selectedType == ContentType.SERIES) {
                    viewModel.onTypeSelected(ContentType.SERIES)
                }
            }

            Row(
                modifier = Modifier.padding(bottom = Dimens.SpaceMd),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryPill(label = "Custom Order", selected = uiState.sort == FavoriteSort.CUSTOM) {
                    viewModel.onSortSelected(FavoriteSort.CUSTOM)
                }
                CategoryPill(label = "A-Z", selected = uiState.sort == FavoriteSort.ALPHABETICAL) {
                    viewModel.onSortSelected(FavoriteSort.ALPHABETICAL)
                }
                CategoryPill(label = "Recently Added", selected = uiState.sort == FavoriteSort.RECENTLY_ADDED) {
                    viewModel.onSortSelected(FavoriteSort.RECENTLY_ADDED)
                }
            }

            Box(Modifier.fillMaxSize()) {
                if (uiState.items.isEmpty()) {
                    EmptyFavoritesState()
                } else {
                    val isChannelsTab = uiState.selectedType == ContentType.LIVE
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = if (isChannelsTab) 320.dp else 150.dp),
                        contentPadding = PaddingValues(bottom = Dimens.SpaceLg),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
                    ) {
                        items(uiState.items, key = { it.contentId }) { item ->
                            if (isChannelsTab) {
                                LaunchedEffect(item.contentId) { viewModel.requestEpgFor(item.contentId) }
                                ChannelCard(
                                    channel = Channel(
                                        id = item.contentId,
                                        name = item.title,
                                        logoUrl = item.imageUrl,
                                        categoryId = "",
                                        channelNumber = 0,
                                        isFavorite = true
                                    ),
                                    nowNext = channelEpgCache[item.contentId],
                                    onClick = { openItem(item) },
                                    onLongClick = { quickActionsFor = item },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                MediaCard(
                                    title = item.title,
                                    imageUrl = item.imageUrl,
                                    aspectRatio = 2f / 3f,
                                    isFavorite = true,
                                    onClick = { openItem(item) },
                                    onLongClick = { quickActionsFor = item }
                                )
                            }
                        }
                    }
                }
            }
        }

        quickActionsFor?.let { item ->
            FavoriteQuickActionsSheet(
                itemTitle = item.title,
                onPlay = { openItem(item) },
                onRemove = { viewModel.onRemove(item) },
                onDismiss = { quickActionsFor = null }
            )
        }
    }
}

@Composable
private fun EmptyFavoritesState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            Text("♡", color = SirKTVPrimary.copy(alpha = 0.6f), fontSize = 56.sp)
            Text(
                text = "No favorites yet — press the ♥ on any channel, movie or series to add it here",
                color = SirKTVOnSurfaceMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Dimens.SpaceXl)
            )
        }
    }
}
