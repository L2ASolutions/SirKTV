package com.sirktv.app.presentation.movies

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
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
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.model.WatchProgress
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
fun MoviesScreen(
    onMovieSelected: (movieId: String) -> Unit,
    onNavigate: (SirKTVNavItem) -> Unit,
    viewModel: MoviesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val firstResultFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SirKTVBackground)
            .padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical)
    ) {
        SirKTVChrome(activeItem = SirKTVNavItem.MOVIES, onNavigate = onNavigate, onRefresh = {})

        Row(
            modifier = Modifier.padding(top = Dimens.SpaceLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
        ) {
            Column {
                Text("Movies", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = "${uiState.allMovies.size} movie(s) on this account",
                    color = SirKTVOnSurfaceMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        TvSearchField(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChanged,
            placeholder = "Search movies",
            firstResultFocusRequester = firstResultFocusRequester.takeIf { uiState.isSearching && uiState.searchResults.isNotEmpty() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.SpaceMd)
        )

        Box(Modifier.padding(top = Dimens.SpaceMd).fillMaxSize()) {
            when {
                uiState.isSearching -> MoviesSearchResults(
                    results = uiState.searchResults,
                    categoryName = { categoryId -> uiState.categories.find { it.id == categoryId }?.name },
                    firstResultFocusRequester = firstResultFocusRequester,
                    onMovieSelected = onMovieSelected,
                    onToggleFavorite = viewModel::onToggleFavorite
                )
                uiState.allMovies.isEmpty() -> Text("No movies found on this account.", color = SirKTVOnSurfaceMuted, fontSize = 13.sp)
                else -> MoviesRows(
                    continueWatching = uiState.continueWatching,
                    rows = uiState.rows,
                    onMovieSelected = onMovieSelected
                )
            }
        }
    }
}

@Composable
private fun MoviesRows(
    continueWatching: List<WatchProgress>,
    rows: List<MovieRow>,
    onMovieSelected: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = Dimens.SpaceLg),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)
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
            val isRecentlyAdded = row.title == "Recently Added"
            MediaRow(title = row.title, rowItems = row.movies) { movie ->
                MediaCard(
                    title = movie.title,
                    imageUrl = movie.posterUrl,
                    aspectRatio = 2f / 3f,
                    rating = movie.rating,
                    badge = if (isRecentlyAdded) "NEW" else null,
                    isFavorite = movie.isFavorite,
                    onClick = { onMovieSelected(movie.id) },
                    modifier = Modifier.width(PosterCardWidth)
                )
            }
        }
    }
}

@Composable
private fun MoviesSearchResults(
    results: List<Movie>,
    categoryName: (String) -> String?,
    firstResultFocusRequester: FocusRequester,
    onMovieSelected: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    if (results.isEmpty()) {
        Text("No movies match your search.", color = SirKTVOnSurfaceMuted, fontSize = 13.sp)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(bottom = Dimens.SpaceLg),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
    ) {
        gridItemsIndexed(results, key = { _, it -> it.id }) { index, movie ->
            Column {
                MediaCard(
                    title = movie.title,
                    imageUrl = movie.posterUrl,
                    aspectRatio = 2f / 3f,
                    rating = movie.rating,
                    subtitle = categoryName(movie.categoryId),
                    isFavorite = movie.isFavorite,
                    onClick = { onMovieSelected(movie.id) },
                    modifier = if (index == 0) Modifier.focusRequester(firstResultFocusRequester) else Modifier
                )
                FavoriteToggleChip(
                    isFavorite = movie.isFavorite,
                    onToggle = { onToggleFavorite(movie.id) },
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
