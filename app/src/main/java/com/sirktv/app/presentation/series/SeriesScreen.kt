package com.sirktv.app.presentation.series

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.sirktv.app.domain.model.WatchProgress
import com.sirktv.app.domain.util.RecentlyAdded
import com.sirktv.app.presentation.common.SectionErrorState
import com.sirktv.app.presentation.common.SectionLoadingState
import com.sirktv.app.presentation.common.SectionSearchButton
import com.sirktv.app.presentation.common.tvChannelRowFocusStyle
import com.sirktv.app.presentation.common.tvNoBorder
import com.sirktv.app.presentation.common.tvNoGlow
import com.sirktv.app.presentation.home.MediaCard
import com.sirktv.app.presentation.home.MediaRow
import com.sirktv.app.presentation.home.PosterCardWidth
import com.sirktv.app.presentation.theme.AppSidebar
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVPrimaryContainer
import com.sirktv.app.presentation.theme.SirKTVTextSecondary
import kotlinx.coroutines.delay

private val CategoryListWidth = 260.dp
private val CategoryRowHeight = 56.dp
private val CategoryRowSelectedBg = SirKTVPrimaryContainer

@Composable
fun SeriesScreen(
    onSeriesSelected: (seriesId: String) -> Unit,
    onEpisodeSelected: (contentId: String) -> Unit,
    onSearch: () -> Unit,
    viewModel: SeriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Hardware Back is handled centrally by SirKTVBackHandler (no sidebar to
    // navigate away with, so it goes to Home) — nothing screen-local needed here.

    val categoryFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    // D-pad Right from the category list must land on a specific, already-
    // composed item, not the lazy container itself — see the doc comment on
    // SeriesCategorySidebar's rightFocusRequester param.
    val firstContentItemFocusRequester = remember { FocusRequester() }

    // Without an explicit initial focus request, DirectionRight is only
    // intercepted by the category sidebar's onPreviewKeyEvent when Compose's
    // default focus placement happens to land inside that subtree — which
    // isn't guaranteed on screen entry, making the very first Right press
    // work inconsistently. Mirrors LiveTvBrowseScreen's identical fix.
    LaunchedEffect(Unit) {
        delay(300)
        runCatching { categoryFocusRequester.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(SirKTVBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppSidebar)
                .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Series", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            SectionSearchButton(contentDescription = "Search series", onClick = onSearch)
        }
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
                    rightFocusRequester = firstContentItemFocusRequester,
                    rightFallbackFocusRequester = contentFocusRequester,
                    onCategorySelected = viewModel::onCategorySelected
                )

                Box(Modifier.weight(1f).fillMaxHeight().padding(Dimens.SpaceLg)) {
                    SeriesContent(
                        continueWatching = uiState.continueWatching,
                        recentlyAdded = uiState.recentlyAdded,
                        series = uiState.visibleSeries,
                        onSeriesSelected = onSeriesSelected,
                        onEpisodeSelected = onEpisodeSelected,
                        onToggleFavorite = viewModel::onToggleFavorite,
                        focusRequester = contentFocusRequester,
                        firstItemFocusRequester = firstContentItemFocusRequester,
                        emptyMessage = "No series found in this category."
                    )
                }
            }
        }
    }
        com.sirktv.app.presentation.common.BackHomeHint(modifier = Modifier.align(Alignment.TopStart))
    }
}

/**
 * [rightFocusRequester] is attached directly to the first already-placed
 * card/row in the content panel, not to that panel's LazyVerticalGrid
 * container — requesting focus on the lazy container itself (relying on
 * focusRestorer() to redirect into its first child) is only reliable once
 * some child of that container has been focused at least once before; on the
 * very first Right press, with nothing yet remembered, that request can
 * silently fail and leave focus stuck here. [rightFallbackFocusRequester]
 * (the container) is tried only if the primary target isn't attached to
 * anything, e.g. the content panel is genuinely empty.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun SeriesCategorySidebar(
    categories: List<Category>,
    counts: Map<String, Int>,
    totalCount: Int,
    selectedCategoryId: String?,
    focusRequester: FocusRequester,
    rightFocusRequester: FocusRequester,
    rightFallbackFocusRequester: FocusRequester,
    onCategorySelected: (String?) -> Unit
) {
    Box(Modifier.fillMaxHeight().width(CategoryListWidth).background(AppSidebar)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppSidebar)
                .focusRequester(focusRequester)
                .focusRestorer()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (event.key == Key.DirectionRight) {
                        runCatching { rightFocusRequester.requestFocus() }
                            .onFailure { runCatching { rightFallbackFocusRequester.requestFocus() } }
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
        border = tvNoBorder(), glow = tvNoGlow(),
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
            // Royal Blue — the app's brand color for category/channel list
            // labels across Live TV, Movies, and Series alike; the left
            // accent bar + background tint above already carry the
            // selected/unselected distinction, so this stays one flat color.
            Text(name, color = SirKTVPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Total: $count", color = SirKTVTextSecondary, fontSize = 11.sp)
        }
    }
}

/**
 * Deliberately does NOT intercept DirectionLeft on this container — the
 * Recently Added row is its own horizontally-scrolling LazyRow and the grid
 * below is multi-column, so a blanket Left-jumps-to-category-list handler
 * here would swallow ordinary in-row/in-grid Left navigation. Compose's
 * default 2D focus search already reaches the category list correctly once
 * nothing further left remains.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun SeriesContent(
    continueWatching: List<WatchProgress>,
    recentlyAdded: List<Series>,
    series: List<Series>,
    onSeriesSelected: (String) -> Unit,
    onEpisodeSelected: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    focusRequester: FocusRequester,
    firstItemFocusRequester: FocusRequester,
    emptyMessage: String
) {
    if (series.isEmpty() && recentlyAdded.isEmpty() && continueWatching.isEmpty()) {
        Text(emptyMessage, color = SirKTVOnSurfaceMuted, fontSize = 13.sp)
        return
    }
    // Whichever card renders first — Continue Watching's first item, then
    // Recently Added's, then the grid's first series — is the one Right from
    // the category list should land on; see SeriesCategorySidebar's doc
    // comment for why this must be a specific item, not the LazyVerticalGrid
    // itself. Mirrors MoviesRows' identical priority order for Movies.
    val firstCardId = continueWatching.firstOrNull()?.contentId
        ?: recentlyAdded.firstOrNull()?.id
        ?: series.firstOrNull()?.id
    // This grid's call site is reused across category switches (only the
    // underlying lists change), so its LazyGridState keeps whatever scroll
    // offset the user left it at. Without resetting it, the item carrying
    // firstItemFocusRequester (the new first card) can end up outside the
    // composed viewport, so the requester never attaches and D-pad Right
    // from the category list silently breaks after switching categories.
    val gridState = rememberLazyGridState()
    LaunchedEffect(firstCardId) {
        gridState.scrollToItem(0)
    }
    LazyVerticalGrid(
        // Fixed(5), not Adaptive — Adaptive stretches every card to fill
        // whatever cell width it computes, which is exactly what made cards
        // here render larger than the identical series's card in the
        // Recently Added row above, which pins an explicit PosterCardWidth.
        // A fixed column count keeps every cell (and so every card, since it
        // also pins PosterCardWidth below) the same physical size.
        columns = GridCells.Fixed(5),
        state = gridState,
        contentPadding = PaddingValues(bottom = Dimens.SpaceLg),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        modifier = Modifier
            .background(SirKTVBackground)
            .focusRequester(focusRequester)
            .focusRestorer()
    ) {
        if (continueWatching.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MediaRow(title = "Continue Watching", rowItems = continueWatching) { progress ->
                    MediaCard(
                        title = progress.title,
                        imageUrl = progress.imageUrl,
                        subtitle = progress.subtitle,
                        aspectRatio = 2f / 3f,
                        progressFraction = progress.progressFraction,
                        onClick = { onEpisodeSelected(progress.contentId) },
                        modifier = Modifier.width(PosterCardWidth).then(
                            if (progress.contentId == firstCardId) Modifier.focusRequester(firstItemFocusRequester) else Modifier
                        )
                    )
                }
            }
        }
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
                        modifier = Modifier.width(PosterCardWidth).then(
                            if (item.id == firstCardId) Modifier.focusRequester(firstItemFocusRequester) else Modifier
                        )
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
                onLongClick = { onToggleFavorite(item.id) },
                modifier = Modifier.width(PosterCardWidth).then(
                    if (item.id == firstCardId) Modifier.focusRequester(firstItemFocusRequester) else Modifier
                )
            )
        }
    }
}
