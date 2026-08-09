package com.sirktv.app.presentation.series

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirktv.app.domain.model.Series
import com.sirktv.app.domain.util.RecentlyAdded
import com.sirktv.app.presentation.common.CategoryPill
import com.sirktv.app.presentation.common.SirKTVChrome
import com.sirktv.app.presentation.common.SirKTVNavItem
import com.sirktv.app.presentation.common.TvSearchField
import com.sirktv.app.presentation.home.FavoriteToggleChip
import com.sirktv.app.presentation.home.MediaCard
import com.sirktv.app.presentation.home.MediaRow
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted

private val PosterCardWidth = 150.dp

@Composable
fun SeriesScreen(
    onSeriesSelected: (seriesId: String) -> Unit,
    onNavigate: (SirKTVNavItem) -> Unit,
    viewModel: SeriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val firstResultFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SirKTVBackground)
            .padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical)
    ) {
        SirKTVChrome(activeItem = SirKTVNavItem.SERIES, onNavigate = onNavigate, onRefresh = {})

        Row(
            modifier = Modifier.padding(top = Dimens.SpaceLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
        ) {
            Column {
                Text("Series", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = "${uiState.allSeries.size} series on this account",
                    color = SirKTVOnSurfaceMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        TvSearchField(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChanged,
            placeholder = "Search series",
            firstResultFocusRequester = firstResultFocusRequester.takeIf { uiState.isSearching && uiState.searchResults.isNotEmpty() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.SpaceMd)
        )

        if (uiState.isSearching) {
            Box(Modifier.padding(top = Dimens.SpaceMd).fillMaxSize()) {
                SeriesResultsGrid(
                    series = uiState.searchResults,
                    categoryName = { categoryId -> uiState.categories.find { it.id == categoryId }?.name },
                    firstResultFocusRequester = firstResultFocusRequester,
                    onSeriesSelected = onSeriesSelected,
                    onToggleFavorite = viewModel::onToggleFavorite,
                    emptyMessage = "No series match your search."
                )
            }
            return@Column
        }

        if (uiState.recentlyAdded.isNotEmpty()) {
            MediaRow(
                title = "Recently Added",
                rowItems = uiState.recentlyAdded,
                modifier = Modifier.padding(top = Dimens.SpaceMd)
            ) { series ->
                MediaCard(
                    title = series.title,
                    imageUrl = series.posterUrl,
                    aspectRatio = 2f / 3f,
                    rating = series.rating,
                    badge = if (RecentlyAdded.isWithinNewWindow(series.lastModifiedEpochMillis)) "NEW" else null,
                    isFavorite = series.isFavorite,
                    onClick = { onSeriesSelected(series.id) },
                    modifier = Modifier.width(PosterCardWidth)
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = Dimens.SpaceMd)) {
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

        Box(Modifier.padding(top = Dimens.SpaceMd).fillMaxSize()) {
            SeriesResultsGrid(
                series = uiState.visibleSeries,
                categoryName = { null },
                firstResultFocusRequester = null,
                onSeriesSelected = onSeriesSelected,
                onToggleFavorite = viewModel::onToggleFavorite,
                emptyMessage = "No series found in this category."
            )
        }
    }
}

@Composable
private fun SeriesResultsGrid(
    series: List<Series>,
    categoryName: (String) -> String?,
    firstResultFocusRequester: FocusRequester?,
    onSeriesSelected: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    emptyMessage: String
) {
    if (series.isEmpty()) {
        Text(emptyMessage, color = SirKTVOnSurfaceMuted, fontSize = 13.sp)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(bottom = Dimens.SpaceLg),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
    ) {
        gridItemsIndexed(series, key = { _, it -> it.id }) { index, item ->
            Column {
                MediaCard(
                    title = item.title,
                    imageUrl = item.posterUrl,
                    aspectRatio = 2f / 3f,
                    rating = item.rating,
                    subtitle = categoryName(item.categoryId),
                    isFavorite = item.isFavorite,
                    onClick = { onSeriesSelected(item.id) },
                    modifier = if (index == 0 && firstResultFocusRequester != null) {
                        Modifier.focusRequester(firstResultFocusRequester)
                    } else Modifier
                )
                FavoriteToggleChip(
                    isFavorite = item.isFavorite,
                    onToggle = { onToggleFavorite(item.id) },
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
