package com.sirktv.app.presentation.movies

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.model.WatchProgress
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
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVTextSecondary
import com.sirktv.app.presentation.theme.SirKTVTextTertiary

private val PosterCardWidth = 150.dp
private val CategoryListWidth = 220.dp
private val CategoryRowHeight = 56.dp
private val CategoryRowSelectedBg = Color(0xFF1A2A4A)

@Composable
fun MoviesScreen(
    onMovieSelected: (movieId: String) -> Unit,
    viewModel: MoviesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // The sidebar is always visible and is how the user leaves this screen —
    // hardware Back intentionally does nothing here, matching TiviMate.
    BackHandler(enabled = true) {}

    val categoryFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }

    Column(Modifier.fillMaxSize().background(SirKTVBackground)) {
        when {
            uiState.isLoading -> SectionLoadingState("Loading movies…", modifier = Modifier.padding(Dimens.SafeAreaHorizontal))
            uiState.loadError != null && uiState.allMovies.isEmpty() ->
                SectionErrorState(error = uiState.loadError!!, onRetry = viewModel::refresh, modifier = Modifier.padding(Dimens.SafeAreaHorizontal))
            // Full width, edge-to-edge — no horizontal safe-zone padding on the columns themselves.
            else -> Row(Modifier.fillMaxSize()) {
                // Every category the provider returns, in API order, each
                // showing its own total — no name/keyword filtering, no
                // truncation. "All" is a pinned pseudo-category on top.
                MoviesCategorySidebar(
                    categories = uiState.categories,
                    counts = remember(uiState.allMovies) { uiState.allMovies.groupingBy { it.categoryId }.eachCount() },
                    totalCount = uiState.allMovies.size,
                    selectedCategoryId = uiState.selectedCategoryId,
                    focusRequester = categoryFocusRequester,
                    rightFocusRequester = contentFocusRequester,
                    onCategorySelected = viewModel::onCategorySelected
                )

                Box(Modifier.weight(1f).fillMaxHeight().padding(Dimens.SpaceLg)) {
                    if (uiState.selectedCategoryId != null) {
                        MoviesCategoryGrid(
                            movies = uiState.visibleMovies,
                            onMovieSelected = onMovieSelected,
                            onToggleFavorite = viewModel::onToggleFavorite,
                            focusRequester = contentFocusRequester
                        )
                    } else {
                        MoviesRows(
                            continueWatching = uiState.continueWatching,
                            rows = uiState.rows,
                            onMovieSelected = onMovieSelected,
                            focusRequester = contentFocusRequester
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun MoviesCategorySidebar(
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
                MoviesCategoryRow(name = "All", count = totalCount, selected = selectedCategoryId == null) {
                    onCategorySelected(null)
                }
            }
            items(categories, key = { it.id }) { category ->
                MoviesCategoryRow(
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
private fun MoviesCategoryRow(name: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) CategoryRowSelectedBg else Color.Transparent,
        label = "movieCategoryRowBg"
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
            Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Total: $count", color = SirKTVTextSecondary, fontSize = 11.sp)
        }
    }
}

/**
 * Deliberately does NOT intercept DirectionLeft on this container: every
 * MediaRow inside is its own horizontally-scrolling LazyRow, so a blanket
 * "Left always jumps to the category sidebar" handler here would swallow
 * ordinary in-row Left navigation (moving from card 3 to card 2) before it
 * ever reaches the focused card. Compose's default 2D focus search already
 * reaches the category sidebar correctly once nothing further left remains
 * in a given row — see tvSidebarEscapeLeft's doc comment for the same
 * reasoning applied to the app-level sidebar.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun MoviesRows(
    continueWatching: List<WatchProgress>,
    rows: List<MovieRow>,
    onMovieSelected: (String) -> Unit,
    focusRequester: FocusRequester
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = Dimens.SpaceLg),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusRestorer()
    ) {
        if (continueWatching.isNotEmpty()) {
            item {
                MediaRow(title = "Continue Watching", rowItems = continueWatching) { progress ->
                    MediaCard(
                        title = progress.title,
                        imageUrl = progress.imageUrl,
                        aspectRatio = 2f / 3f,
                        progressFraction = progress.progressFraction,
                        onClick = { onMovieSelected(progress.contentId) },
                        modifier = Modifier.width(PosterCardWidth)
                    )
                }
            }
        }
        items(rows, key = { it.title }) { row ->
            val isRecentlyAddedRow = row.title == "Recently Added"
            MediaRow(title = row.title, rowItems = row.movies) { movie ->
                MediaCard(
                    title = movie.title,
                    imageUrl = movie.posterUrl,
                    aspectRatio = 2f / 3f,
                    rating = movie.rating,
                    badge = if (isRecentlyAddedRow && RecentlyAdded.isWithinNewWindow(movie.addedAtEpochMillis)) "NEW" else null,
                    isFavorite = movie.isFavorite,
                    onClick = { onMovieSelected(movie.id) },
                    modifier = Modifier.width(PosterCardWidth)
                )
            }
        }
    }
}

/** See [MoviesRows]'s doc comment — same reasoning, this grid is multi-column so DirectionLeft is never blanket-intercepted here either. */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun MoviesCategoryGrid(
    movies: List<Movie>,
    onMovieSelected: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    focusRequester: FocusRequester
) {
    if (movies.isEmpty()) {
        Text("No movies found in this category.", color = SirKTVOnSurfaceMuted, fontSize = 13.sp)
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
        gridItemsIndexed(movies, key = { _, it -> it.id }) { _, movie ->
            MediaCard(
                title = movie.title,
                imageUrl = movie.posterUrl,
                aspectRatio = 2f / 3f,
                rating = movie.rating,
                isFavorite = movie.isFavorite,
                onClick = { onMovieSelected(movie.id) },
                onLongClick = { onToggleFavorite(movie.id) }
            )
        }
    }
}
