package com.sirktv.app.presentation.series

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirktv.app.presentation.common.CategoryPill
import com.sirktv.app.presentation.home.FavoriteToggleChip
import com.sirktv.app.presentation.home.MediaCard
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground

@Composable
fun SeriesScreen(
    onSeriesSelected: (seriesId: String) -> Unit,
    viewModel: SeriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SirKTVBackground)
            .padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical)
    ) {
        Text("Series", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            text = "${uiState.allSeries.size} series on this account",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = Dimens.SpaceMd)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                CategoryPill(label = "All Series", selected = uiState.selectedCategoryId == null) {
                    viewModel.onCategorySelected(null)
                }
            }
            items(uiState.categories, key = { it.id }) { category ->
                CategoryPill(label = category.name, selected = uiState.selectedCategoryId == category.id) {
                    viewModel.onCategorySelected(category.id)
                }
            }
        }

        Box(Modifier.padding(top = Dimens.SpaceMd)) {
            if (uiState.visibleSeries.isEmpty()) {
                Text("No series found in this category.", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(bottom = Dimens.SpaceLg),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
                ) {
                    items(uiState.visibleSeries, key = { it.id }) { series ->
                        Column {
                            MediaCard(
                                title = series.title,
                                imageUrl = series.posterUrl,
                                aspectRatio = 2f / 3f,
                                isFavorite = series.isFavorite,
                                onClick = { onSeriesSelected(series.id) }
                            )
                            FavoriteToggleChip(
                                isFavorite = series.isFavorite,
                                onToggle = { viewModel.onToggleFavorite(series.id) },
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
