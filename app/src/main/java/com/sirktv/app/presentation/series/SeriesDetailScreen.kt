package com.sirktv.app.presentation.series

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import com.sirktv.app.domain.model.Episode
import com.sirktv.app.presentation.common.TvFocusAccent
import com.sirktv.app.presentation.common.glassCard
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVCardBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVSurfaceVariant

@Composable
fun SeriesDetailScreen(
    onEpisodeSelected: (seriesId: String, season: Int, episode: Int) -> Unit,
    onBack: () -> Unit,
    viewModel: SeriesDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val series = uiState.series
    val firstEpisode = uiState.seasons.firstOrNull()?.episodes?.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SirKTVBackground)
            .padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical)
    ) {
        androidx.tv.material3.Surface(border = com.sirktv.app.presentation.common.tvNoBorder(),
            onClick = onBack,
            modifier = Modifier.padding(top = Dimens.SpaceLg, bottom = Dimens.SpaceSm).tvFocusStyle(cornerRadius = 6.dp)
        ) {
            Text("‹ Back", color = SirKTVPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)) {
            Box(
                Modifier
                    .width(180.dp)
                    .aspectRatio(2f / 3f)
                    .glassCard()
            ) {
                if (!series?.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = series?.posterUrl,
                        contentDescription = series?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().background(SirKTVSurfaceVariant, RoundedCornerShape(Dimens.CornerRadius))
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                Text(series?.title ?: "", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                series?.rating?.let {
                    Text("★ ${"%.1f".format(it)}", color = SirKTVPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                series?.director?.let {
                    Text("Director: $it", color = SirKTVOnSurfaceMuted, fontSize = 12.sp)
                }
                if (!series?.cast.isNullOrEmpty()) {
                    Text("Cast: ${series?.cast?.joinToString(", ")}", color = SirKTVOnSurfaceMuted, fontSize = 12.sp)
                }
                series?.synopsis?.let {
                    Text(it, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm), modifier = Modifier.padding(top = Dimens.SpaceXs)) {
                    if (firstEpisode != null) {
                        Button(border = com.sirktv.app.presentation.common.tvNoButtonBorder(), 
                            onClick = { onEpisodeSelected(firstEpisode.seriesId, firstEpisode.seasonNumber, firstEpisode.episodeNumber) },
                            colors = ButtonDefaults.colors(containerColor = SirKTVPrimary, contentColor = Color.White),
                            modifier = Modifier.tvFocusStyle(accent = TvFocusAccent.BORDER)
                        ) {
                            TvText("▶ Play")
                        }
                    }
                    Button(border = com.sirktv.app.presentation.common.tvNoButtonBorder(), 
                        onClick = viewModel::onToggleFavorite,
                        colors = if (series?.isFavorite == true) {
                            ButtonDefaults.colors(containerColor = SirKTVPrimary, contentColor = Color.White)
                        } else {
                            ButtonDefaults.colors(containerColor = Color.White.copy(alpha = 0.10f), contentColor = Color.White)
                        },
                        modifier = Modifier.tvFocusStyle(accent = TvFocusAccent.BORDER)
                    ) {
                        TvText(if (series?.isFavorite == true) "♥ Favorited" else "♡ Add to Favorites")
                    }
                }
            }
        }

        if (uiState.seasons.isNotEmpty()) {
            Text(
                "Seasons",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = Dimens.SpaceLg, bottom = Dimens.SpaceSm)
            )
            LazyRow(modifier = Modifier.background(SirKTVBackground), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.seasons, key = { it.seasonNumber }) { season ->
                    SeasonTab(
                        label = "Season ${season.seasonNumber}",
                        selected = uiState.selectedSeasonNumber == season.seasonNumber,
                        onClick = { viewModel.onSeasonSelected(season.seasonNumber) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.background(SirKTVBackground).padding(top = Dimens.SpaceMd),
                contentPadding = PaddingValues(bottom = Dimens.SpaceLg),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(uiState.selectedSeasonEpisodes, key = { it.id }) { episode ->
                    EpisodeRow(
                        episode = episode,
                        onSelect = {
                            uiState.selectedSeasonNumber?.let { season ->
                                onEpisodeSelected(episode.seriesId, season, episode.episodeNumber)
                            }
                        }
                    )
                }
            }
        } else if (!uiState.isLoading) {
            Text(
                "No episode information available for this series yet.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = Dimens.SpaceLg)
            )
        }
    }
}

/**
 * Season selector pill. Built as its own explicit tv.material3 [Surface]
 * (not a standard Material3 Tab/TabRow, which doesn't handle D-pad focus or
 * OK/Select at all on TV) with a defensive [Modifier.onPreviewKeyEvent]
 * fallback that fires the season switch directly on DPAD_CENTER/Enter
 * release — belt-and-suspenders alongside Surface's own onClick so a season
 * change is never silently swallowed. DIRECTION_LEFT/RIGHT are left
 * unconsumed so the surrounding LazyRow's default D-pad focus search still
 * moves between tabs.
 */
@Composable
private fun SeasonTab(label: String, selected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val background by animateColorAsState(
        targetValue = when {
            selected -> SirKTVPrimary
            isFocused -> SirKTVPrimary.copy(alpha = 0.35f)
            else -> SirKTVCardBackground
        },
        label = "seasonTabBackground"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else SirKTVOnSurfaceMuted,
        label = "seasonTabText"
    )

    Surface(border = com.sirktv.app.presentation.common.tvNoBorder(), 
        onClick = onClick,
        modifier = Modifier
            .tvFocusStyle(cornerRadius = 999.dp) { isFocused = it }
            .onPreviewKeyEvent { event ->
                val isSelectKey = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
                if (isSelectKey && event.type == KeyEventType.KeyUp) {
                    onClick()
                    true
                } else {
                    false
                }
            }
    ) {
        Box(
            modifier = Modifier
                .background(background, RoundedCornerShape(999.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(text = label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode, onSelect: () -> Unit) {
    Surface(border = com.sirktv.app.presentation.common.tvNoBorder(), onClick = onSelect, modifier = Modifier.fillMaxWidth().tvFocusStyle(cornerRadius = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(12.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "E${episode.episodeNumber}",
                color = SirKTVPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                episode.synopsis?.let {
                    Text(it, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            episode.durationMinutes?.let {
                Text("${it}m", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
            }
        }
    }
}
