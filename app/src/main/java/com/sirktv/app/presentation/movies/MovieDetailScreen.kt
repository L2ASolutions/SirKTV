package com.sirktv.app.presentation.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text as TvText
import coil.compose.AsyncImage
import com.sirktv.app.presentation.common.TvFocusAccent
import com.sirktv.app.presentation.common.glassCard
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.common.tvNoBorder
import com.sirktv.app.presentation.common.tvNoGlow
import com.sirktv.app.presentation.common.tvNoButtonBorder
import com.sirktv.app.presentation.theme.AppSidebar
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVSurfaceVariant

private val MovieInfoPanelWidth = 300.dp

/**
 * Movie's counterpart to [com.sirktv.app.presentation.series.SeriesDetailScreen]
 * — same two-column layout (fixed, independently scrollable info panel on the
 * left; synopsis/cast/director on the right) so a movie gets a look at its
 * plot/cast/genre before committing, instead of the previous behavior of
 * jumping straight from the grid into the player.
 */
@Composable
fun MovieDetailScreen(
    onPlay: (movieId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val movie = uiState.movie
    val detail = uiState.detail

    Row(modifier = Modifier.fillMaxSize().background(SirKTVBackground)) {
        // LEFT — poster + actions, fixed width, independently scrollable so a
        // long cast list never pushes the Play/Favorite buttons off-screen.
        LazyColumn(
            modifier = Modifier
                .width(MovieInfoPanelWidth)
                .fillMaxHeight()
                .background(AppSidebar)
                .padding(Dimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
        ) {
            item {
                Surface(
                    border = tvNoBorder(), glow = tvNoGlow(),
                    onClick = onBack,
                    modifier = Modifier.padding(bottom = Dimens.SpaceSm).tvFocusStyle(cornerRadius = 6.dp)
                ) {
                    Text("‹ Back", color = SirKTVPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            item {
                Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).glassCard()) {
                    if (!movie?.posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = movie?.posterUrl,
                            contentDescription = movie?.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().background(SirKTVSurfaceVariant, RoundedCornerShape(Dimens.CornerRadius))
                        )
                    }
                }
            }
            item {
                Text(movie?.title ?: "", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            if (movie?.rating != null || detail?.genre?.isNotBlank() == true) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                        movie?.rating?.let { rating ->
                            Text("★ ${"%.1f".format(rating)}", color = SirKTVPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        detail?.genre?.takeIf { it.isNotBlank() }?.let { genre ->
                            Text(genre, color = SirKTVOnSurfaceMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            detail?.durationMinutes?.let { minutes ->
                item { Text("$minutes min", color = SirKTVOnSurfaceMuted, fontSize = 12.sp) }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                    Button(
                        border = tvNoButtonBorder(),
                        onClick = { movie?.let { onPlay(it.id) } },
                        colors = ButtonDefaults.colors(containerColor = SirKTVPrimary, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().tvFocusStyle(accent = TvFocusAccent.BORDER)
                    ) {
                        TvText("▶ Play")
                    }
                    Button(
                        border = tvNoButtonBorder(),
                        onClick = viewModel::onToggleFavorite,
                        colors = if (movie?.isFavorite == true) {
                            ButtonDefaults.colors(containerColor = SirKTVPrimary, contentColor = Color.White)
                        } else {
                            ButtonDefaults.colors(containerColor = Color.White.copy(alpha = 0.10f), contentColor = Color.White)
                        },
                        modifier = Modifier.fillMaxWidth().tvFocusStyle(accent = TvFocusAccent.BORDER)
                    ) {
                        TvText(if (movie?.isFavorite == true) "♥ Favorited" else "♡ Add to Favorites")
                    }
                }
            }
        }

        // RIGHT — synopsis/cast/director, fills all remaining width/height.
        Column(modifier = Modifier.fillMaxSize().padding(Dimens.SpaceLg)) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SirKTVPrimary)
                }
                detail == null -> Text(
                    "No additional details available for this title.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
                    detail.director?.takeIf { it.isNotBlank() }?.let { director ->
                        InfoBlock("Director", director)
                    }
                    if (detail.cast.isNotEmpty()) {
                        InfoBlock("Cast", detail.cast.joinToString(", "))
                    }
                    detail.synopsis?.takeIf { it.isNotBlank() }?.let { synopsis ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Synopsis", color = SirKTVPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(synopsis, color = SirKTVOnSurfaceMuted, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBlock(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = SirKTVPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = SirKTVOnSurfaceMuted, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}
