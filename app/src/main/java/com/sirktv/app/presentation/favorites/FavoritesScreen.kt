package com.sirktv.app.presentation.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.FavoriteItem
import com.sirktv.app.domain.model.FavoriteSort
import com.sirktv.app.presentation.common.CategoryPill
import com.sirktv.app.presentation.common.glassCard
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.home.MediaCard
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import androidx.tv.material3.Surface

@Composable
fun FavoritesScreen(
    onLiveSelected: (channelId: String) -> Unit,
    onMovieSelected: (movieId: String) -> Unit,
    onSeriesSelected: (seriesId: String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                Text(
                    "No favorites yet — browse and add some!",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 190.dp),
                    contentPadding = PaddingValues(bottom = Dimens.SpaceLg),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
                ) {
                    items(uiState.items, key = { it.contentId }) { item ->
                        FavoriteCell(
                            item = item,
                            allowReorder = uiState.sort == FavoriteSort.CUSTOM,
                            onOpen = {
                                when (item.contentType) {
                                    ContentType.LIVE -> onLiveSelected(item.contentId)
                                    ContentType.MOVIE -> onMovieSelected(item.contentId)
                                    ContentType.SERIES -> onSeriesSelected(item.contentId)
                                    ContentType.EPISODE -> Unit
                                }
                            },
                            onRemove = { viewModel.onRemove(item) },
                            onMoveUp = { viewModel.onMoveUp(item) },
                            onMoveDown = { viewModel.onMoveDown(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteCell(
    item: FavoriteItem,
    allowReorder: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Column {
        MediaCard(
            title = item.title,
            imageUrl = item.imageUrl,
            aspectRatio = if (item.contentType == ContentType.LIVE) 16f / 9f else 2f / 3f,
            isFavorite = true,
            onClick = onOpen
        )
        Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallChip(label = "✕ Remove", modifier = Modifier.weight(1f), onClick = onRemove)
            if (allowReorder) {
                SmallChip(label = "↑", onClick = onMoveUp)
                SmallChip(label = "↓", onClick = onMoveDown)
            }
        }
    }
}

@Composable
private fun SmallChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.tvFocusStyle(cornerRadius = 8.dp)) {
        Box(
            modifier = Modifier.glassCard(8.dp).padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
