package com.sirktv.app.presentation.series

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Series
import com.sirktv.app.domain.util.RecentlyAdded
import com.sirktv.app.presentation.common.SectionErrorState
import com.sirktv.app.presentation.common.SectionLoadingState
import com.sirktv.app.presentation.common.tvChannelRowFocusStyle
import com.sirktv.app.presentation.common.tvNoBorder
import com.sirktv.app.presentation.common.tvSidebarEscapeLeft
import com.sirktv.app.presentation.home.MediaCard
import com.sirktv.app.presentation.home.MediaRow
import com.sirktv.app.presentation.theme.AppSidebar
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceStrong
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVPrimaryContainer
import com.sirktv.app.presentation.theme.SirKTVTextSecondary

private val PosterCardWidth = 150.dp
private val CategoryListWidth = 260.dp
private val CategoryRowHeight = 56.dp
private val CategoryRowSelectedBg = SirKTVPrimaryContainer

@Composable
fun SeriesScreen(
    onSeriesSelected: (seriesId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: SeriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // This screen has no sidebar (full-width panels instead) — hardware Back
    // is the only way to leave, so it jumps to Home.
    BackHandler(enabled = true) { onBack() }

    val categoryFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(SirKTVBackground)) {
        when {
            uiState.isLoading -> SectionLoadingState("Loading series…", modifier = Modifier.padding(Dimens.SafeAreaHorizontal))
            uiState.loadError != null ->
                SectionErrorState(error = uiState.loadError!!, onRetry = viewModel::refresh, modifier = Modifier.padding(Dimens.SafeAreaHorizontal))
            // Full width, edge-to-edge — no horizontal safe-zone padding on the columns themselves.
            else -> Row(Modifier.fillMaxSize()) {
                // Every category the provider returns, in API order, each
                // showing its own total — no name/keyword filtering, no
                // truncation. "All Series" is a pinned pseudo-category on top.
                SeriesCategorySidebar(
                    categories = uiState.categories,
                    counts = remember(uiState.allSeries) { uiState.allSeries.groupingBy { it.categoryId }.eachCount() },
                    totalCount = uiState.allSeries.size,
                    selectedCategoryId = uiState.selectedCategoryId,
                    focusRequester = categoryFocusRequester,
                    rightFocusRequester = contentFocusRequester,
                    onCategorySelected = viewModel::onCategorySelected
                )

                Box(Modifier.weight(1f).fillMaxHeight().padding(Dimens.SpaceLg)) {
                    SeriesContent(
                        recentlyAdded = uiState.recentlyAdded,
                        series = uiState.visibleSeries,
                        onSeriesSelected = onSeriesSelected,
                        onToggleFavorite = viewModel::onToggleFavorite,
                        focusRequester = contentFocusRequester,
                        emptyMessage = "No series found in this category."
                    )
                }
            }
        }
    }
        com.sirktv.app.presentation.common.BackHomeHint(modifier = Modifier.align(Alignment.TopStart))
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun SeriesCategorySidebar(
    categories: List<Category>,
    counts: Map<String, Int>,
    totalCount: Int,
    selectedCategoryId: String?,
    focusRequester: FocusRequester,
    rightFocusRequester: FocusRequester,
    onCategorySelected: (String?) -> Unit
) {
    Box(Modifier.fillMaxHeight().width(CategoryListWidth).background(AppSidebar)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusRestorer()
                .tvSidebarEscapeLeft()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (event.key == Key.DirectionRight) {
                        rightFocusRequester.requestFocus()
                        true
                    } else false
                }
        ) {
            item {
                SeriesCategoryRow(name = "All Series", count = totalCount, selected = selectedCategoryId == null) {
                    onCategorySelected(null)
                }
            }
            items(categories, key = { it.id }) { category ->
                SeriesCategoryRow(
                    name = category.name,
                    count = counts[category.id] ?: 0,
                    selected = selectedCategoryId == category.id
                ) {
                    onCategorySelected(category.id)
                }
            }
        }
    }
}

@Composable
private fun SeriesCategoryRow(name: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) CategoryRowSelectedBg else Color.Transparent,
        label = "seriesCategoryRowBg"
    )
    androidx.tv.material3.Surface(
        border = tvNoBorder(),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().tvChannelRowFocusStyle(cornerRadius = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(CategoryRowHeight)
                .background(bgColor)
                .drawBehind {
                    if (selected) drawRect(color = SirKTVPrimary, size = Size(3.dp.toPx(), size.height))
                }
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(name, color = SirKTVOnSurfaceStrong, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Total: $count", color = SirKTVTextSecondary, fontSize = 11.sp)
        }
    }
}

/**
 * Deliberately does NOT intercept DirectionLeft on this container — the
 * Recently Added row is its own horizontally-scrolling LazyRow and the grid
 * below is multi-column, so a blanket Left-jumps-to-sidebar handler here
 * would swallow ordinary in-row/in-grid Left navigation. Compose's default
 * 2D focus search already reaches the category sidebar correctly once
 * nothing further left remains — see tvSidebarEscapeLeft's doc comment.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun SeriesContent(
    recentlyAdded: List<Series>,
    series: List<Series>,
    onSeriesSelected: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    focusRequester: FocusRequester,
    emptyMessage: String
) {
    if (series.isEmpty() && recentlyAdded.isEmpty()) {
        Text(emptyMessage, color = SirKTVOnSurfaceMuted, fontSize = 13.sp)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
        contentPadding = PaddingValues(bottom = Dimens.SpaceLg),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusRestorer()
    ) {
        if (recentlyAdded.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MediaRow(title = "Recently Added", rowItems = recentlyAdded) { item ->
                    MediaCard(
                        title = item.title,
                        imageUrl = item.posterUrl,
                        aspectRatio = 2f / 3f,
                        rating = item.rating,
                        badge = if (RecentlyAdded.isWithinNewWindow(item.lastModifiedEpochMillis)) "NEW" else null,
                        isFavorite = item.isFavorite,
                        onClick = { onSeriesSelected(item.id) },
                        onLongClick = { onToggleFavorite(item.id) },
                        modifier = Modifier.width(PosterCardWidth)
                    )
                }
            }
        }
        gridItemsIndexed(series, key = { _, it -> it.id }) { _, item ->
            MediaCard(
                title = item.title,
                imageUrl = item.posterUrl,
                aspectRatio = 2f / 3f,
                rating = item.rating,
                isFavorite = item.isFavorite,
                onClick = { onSeriesSelected(item.id) },
                onLongClick = { onToggleFavorite(item.id) }
            )
        }
    }
}
