package com.sirktv.app.presentation.search

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Surface
import com.sirktv.app.domain.model.SearchResults
import com.sirktv.app.presentation.common.CategoryPill
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.home.MediaCard
import com.sirktv.app.presentation.home.RowHeader
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
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
    LaunchedEffect(Unit) { fieldFocusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SirKTVBackground)
            .padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical)
    ) {
        Text("Search", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)

        OutlinedTextField(
            value = uiState.query,
            onValueChange = viewModel::onQueryChanged,
            placeholder = { Text("Search live TV, movies, and series") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.onSearchSubmitted() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SirKTVBackground,
                unfocusedContainerColor = SirKTVBackground,
                focusedBorderColor = SirKTVPrimary,
                unfocusedBorderColor = SirKTVOnSurfaceMuted,
                cursorColor = SirKTVPrimary,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.SpaceMd, bottom = Dimens.SpaceMd)
                .focusRequester(fieldFocusRequester)
                .tvFocusStyle()
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
                onChannelSelected = onChannelSelected,
                onMovieSelected = onMovieSelected,
                onSeriesSelected = onSeriesSelected
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
        Surface(onClick = onClear, modifier = Modifier.tvFocusStyle(cornerRadius = 6.dp)) {
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
    onChannelSelected: (String) -> Unit,
    onMovieSelected: (String) -> Unit,
    onSeriesSelected: (String) -> Unit
) {
    if (results.isEmpty) {
        Text("No matches found.", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = Dimens.SpaceLg), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)) {
        if (results.channels.isNotEmpty()) {
            item {
                ResultRow(title = "Live TV", count = results.channels.size) {
                    items(results.channels, key = { "ch-${it.id}" }) { channel ->
                        MediaCard(
                            title = channel.name,
                            imageUrl = channel.logoUrl,
                            onClick = { onChannelSelected(channel.id) },
                            modifier = Modifier.width(220.dp)
                        )
                    }
                }
            }
        }
        if (results.movies.isNotEmpty()) {
            item {
                ResultRow(title = "Movies", count = results.movies.size) {
                    items(results.movies, key = { "mv-${it.id}" }) { movie ->
                        MediaCard(
                            title = movie.title,
                            imageUrl = movie.posterUrl,
                            aspectRatio = 2f / 3f,
                            isFavorite = movie.isFavorite,
                            onClick = { onMovieSelected(movie.id) },
                            modifier = Modifier.width(140.dp)
                        )
                    }
                }
            }
        }
        if (results.series.isNotEmpty()) {
            item {
                ResultRow(title = "Series", count = results.series.size) {
                    items(results.series, key = { "sr-${it.id}" }) { series ->
                        MediaCard(
                            title = series.title,
                            imageUrl = series.posterUrl,
                            aspectRatio = 2f / 3f,
                            isFavorite = series.isFavorite,
                            onClick = { onSeriesSelected(series.id) },
                            modifier = Modifier.width(140.dp)
                        )
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
