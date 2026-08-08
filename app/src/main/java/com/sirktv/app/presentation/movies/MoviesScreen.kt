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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.model.WatchProgress
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.home.FavoriteToggleChip
import com.sirktv.app.presentation.home.MediaCard
import com.sirktv.app.presentation.home.MediaRow
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary

private val PosterCardWidth = 150.dp

@Composable
fun MoviesScreen(
    onMovieSelected: (movieId: String) -> Unit,
    viewModel: MoviesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchVisible by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SirKTVBackground)
            .padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Movies", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = "${uiState.allMovies.size} movie(s) on this account",
                    color = SirKTVOnSurfaceMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            MoviesSearchIcon(
                active = searchVisible,
                onClick = {
                    searchVisible = !searchVisible
                    if (!searchVisible) viewModel.onSearchQueryChanged("")
                }
            )
        }

        if (searchVisible) {
            LaunchedEffect(Unit) { searchFocusRequester.requestFocus() }
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Search movies") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {}),
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
                    .padding(top = Dimens.SpaceMd)
                    .focusRequester(searchFocusRequester)
                    .tvFocusStyle()
            )
        }

        Box(Modifier.padding(top = Dimens.SpaceMd).fillMaxSize()) {
            when {
                uiState.isSearching -> MoviesSearchResults(
                    results = uiState.searchResults,
                    categoryName = { categoryId -> uiState.categories.find { it.id == categoryId }?.name },
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
private fun MoviesSearchIcon(active: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.tvFocusStyle(cornerRadius = 20.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (active) SirKTVPrimary else Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("🔍", fontSize = 16.sp)
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
            MediaRow(title = row.title, rowItems = row.movies) { movie ->
                MediaCard(
                    title = movie.title,
                    imageUrl = movie.posterUrl,
                    aspectRatio = 2f / 3f,
                    rating = movie.rating,
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
        gridItems(results, key = { it.id }) { movie ->
            Column {
                MediaCard(
                    title = movie.title,
                    imageUrl = movie.posterUrl,
                    aspectRatio = 2f / 3f,
                    rating = movie.rating,
                    subtitle = categoryName(movie.categoryId),
                    isFavorite = movie.isFavorite,
                    onClick = { onMovieSelected(movie.id) }
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
