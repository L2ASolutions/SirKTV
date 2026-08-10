package com.sirktv.app.presentation.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Surface
import com.sirktv.app.domain.model.SearchResults
import com.sirktv.app.presentation.common.CategoryPill
import com.sirktv.app.presentation.common.TvSearchField
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.home.FavoriteToggleChip
import com.sirktv.app.presentation.home.MediaCard
import com.sirktv.app.presentation.home.RowHeader
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVPrimary

@Composable
fun SearchScreen(
    onChannelSelected: (channelId: String) -> Unit,
    onMovieSelected: (movieId: String) -> Unit,
    onSeriesSelected: (seriesId: String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val fieldFocusRequester = remember { FocusRequester() }
    val firstResultFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { fieldFocusRequester.requestFocus() }

    // The sidebar is always visible and is how the user leaves this screen —
    // hardware Back intentionally does nothing here, matching TiviMate.
    BackHandler(enabled = true) {}

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SirKTVBackground)
            .padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical)
    ) {
        // No "Search" title — the input itself is the header, sidebar shows the active section.
        TvSearchField(
            query = uiState.query,
            onQueryChange = viewModel::onQueryChanged,
            placeholder = "Search live TV, movies, and series",
            fieldFocusRequester = fieldFocusRequester,
            firstResultFocusRequester = firstResultFocusRequester.takeIf { !uiState.results.isEmpty },
            onSearchSubmitted = { viewModel.onSearchSubmitted() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.SpaceMd, bottom = Dimens.SpaceMd)
        )

        if (uiState.query.trim().length < 2) {
            RecentSearches(
                recent = uiState.recentSearches,
                onPick = { viewModel.onSearchSubmitted(it) },
                onClear = viewModel::onClearHistory
            )
        } else {
            SearchResultsList(
                results = uiState.results,
                firstResultFocusRequester = firstResultFocusRequester,
                onChannelSelected = onChannelSelected,
                onMovieSelected = onMovieSelected,
                onSeriesSelected = onSeriesSelected,
                onToggleChannelFavorite = viewModel::onToggleChannelFavorite,
                onToggleMovieFavorite = viewModel::onToggleMovieFavorite,
                onToggleSeriesFavorite = viewModel::onToggleSeriesFavorite
            )
        }
    }
}

@Composable
private fun RecentSearches(recent: List<String>, onPick: (String) -> Unit, onClear: () -> Unit) {
    if (recent.isEmpty()) {
        Text("Search for a channel, movie, or series title.", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        RowHeader(title = "Recent Searches")
        Surface(border = com.sirktv.app.presentation.common.tvNoBorder(), onClick = onClear, modifier = Modifier.tvFocusStyle(cornerRadius = 6.dp)) {
            Text(
                "Clear",
                color = SirKTVPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(recent, key = { it }) { query ->
            CategoryPill(label = query, selected = false, onClick = { onPick(query) })
        }
    }
}

@Composable
private fun SearchResultsList(
    results: SearchResults,
    firstResultFocusRequester: FocusRequester,
    onChannelSelected: (String) -> Unit,
    onMovieSelected: (String) -> Unit,
    onSeriesSelected: (String) -> Unit,
    onToggleChannelFavorite: (String) -> Unit,
    onToggleMovieFavorite: (String) -> Unit,
    onToggleSeriesFavorite: (String) -> Unit
) {
    if (results.isEmpty) {
        Text("No matches found.", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        return
    }
    // Whichever section renders first gets its first card wired to
    // firstResultFocusRequester, so D-pad Down from the search field (or an
    // explicit search submit) always has a real, deterministic landing spot.
    val firstSectionKey = when {
        results.channels.isNotEmpty() -> "channels"
        results.movies.isNotEmpty() -> "movies"
        else -> "series"
    }
    LazyColumn(contentPadding = PaddingValues(bottom = Dimens.SpaceLg), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)) {
        if (results.channels.isNotEmpty()) {
            item {
                ResultRow(title = "Live TV", count = results.channels.size) {
                    itemsIndexed(results.channels, key = { _, it -> "ch-${it.id}" }) { index, channel ->
                        Column(modifier = Modifier.width(220.dp)) {
                            MediaCard(
                                title = channel.name,
                                imageUrl = channel.logoUrl,
                                isFavorite = channel.isFavorite,
                                onClick = { onChannelSelected(channel.id) },
                                modifier = if (firstSectionKey == "channels" && index == 0) {
                                    Modifier.focusRequester(firstResultFocusRequester)
                                } else Modifier
                            )
                            FavoriteToggleChip(
                                isFavorite = channel.isFavorite,
                                onToggle = { onToggleChannelFavorite(channel.id) },
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
        if (results.movies.isNotEmpty()) {
            item {
                ResultRow(title = "Movies", count = results.movies.size) {
                    itemsIndexed(results.movies, key = { _, it -> "mv-${it.id}" }) { index, movie ->
                        Column(modifier = Modifier.width(140.dp)) {
                            MediaCard(
                                title = movie.title,
                                imageUrl = movie.posterUrl,
                                aspectRatio = 2f / 3f,
                                rating = movie.rating,
                                isFavorite = movie.isFavorite,
                                onClick = { onMovieSelected(movie.id) },
                                modifier = if (firstSectionKey == "movies" && index == 0) {
                                    Modifier.focusRequester(firstResultFocusRequester)
                                } else Modifier
                            )
                            FavoriteToggleChip(
                                isFavorite = movie.isFavorite,
                                onToggle = { onToggleMovieFavorite(movie.id) },
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
        if (results.series.isNotEmpty()) {
            item {
                ResultRow(title = "Series", count = results.series.size) {
                    itemsIndexed(results.series, key = { _, it -> "sr-${it.id}" }) { index, series ->
                        Column(modifier = Modifier.width(140.dp)) {
                            MediaCard(
                                title = series.title,
                                imageUrl = series.posterUrl,
                                aspectRatio = 2f / 3f,
                                rating = series.rating,
                                isFavorite = series.isFavorite,
                                onClick = { onSeriesSelected(series.id) },
                                modifier = if (firstSectionKey == "series" && index == 0) {
                                    Modifier.focusRequester(firstResultFocusRequester)
                                } else Modifier
                            )
                            FavoriteToggleChip(
                                isFavorite = series.isFavorite,
                                onToggle = { onToggleSeriesFavorite(series.id) },
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(title: String, count: Int, content: LazyListScope.() -> Unit) {
    Column {
        RowHeader(title = title, count = count)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd), content = content)
    }
}
