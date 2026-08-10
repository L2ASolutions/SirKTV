package com.sirktv.app.presentation.series

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import com.sirktv.app.presentation.theme.AppSidebar
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVCardBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVSurfaceVariant

private val SeriesInfoPanelWidth = 300.dp

/**
 * Two-column layout: a fixed-width, independently scrollable info panel on
 * the left (poster/title/synopsis/actions) and the season tabs + episode
 * list filling all remaining height on the right. Previously this was one
 * non-scrolling Column with the episode LazyColumn stacked below the info
 * block — on series with a long synopsis/cast or several seasons, the
 * combined height overflowed the screen and cut the episode list off with no
 * way to scroll to it. Splitting the info panel into its own column means
 * the episode LazyColumn always gets a bounded height (fillMaxSize inside a
 * weighted Column) and can always scroll to show every episode.
 */
@Composable
fun SeriesDetailScreen(
    onEpisodeSelected: (seriesId: String, season: Int, episode: Int) -> Unit,
    onBack: () -> Unit,
    viewModel: SeriesDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val series = uiState.series
    val firstEpisode = uiState.seasons.firstOrNull()?.episodes?.firstOrNull()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SirKTVBackground)
    ) {
        // LEFT — poster + info, fixed width, independently scrollable so a
        // long synopsis/cast list never pushes the episode panel off-screen.
        LazyColumn(
            modifier = Modifier
                .width(SeriesInfoPanelWidth)
                .fillMaxHeight()
                .background(AppSidebar)
                .padding(Dimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
        ) {
            item {
                androidx.tv.material3.Surface(border = com.sirktv.app.presentation.common.tvNoBorder(),
                    onClick = onBack,
                    modifier = Modifier.padding(bottom = Dimens.SpaceSm).tvFocusStyle(cornerRadius = 6.dp)
                ) {
                    Text("‹ Back", color = SirKTVPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
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
            }
            item {
                Text(series?.title ?: "", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            series?.rating?.let { rating ->
                item { Text("★ ${"%.1f".format(rating)}", color = SirKTVPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
            series?.director?.let { director ->
                item { Text("Director: $director", color = SirKTVOnSurfaceMuted, fontSize = 12.sp) }
            }
            if (!series?.cast.isNullOrEmpty()) {
                item {
                    Text(
                        "Cast: ${series?.cast?.joinToString(", ")}",
                        color = SirKTVOnSurfaceMuted,
                        fontSize = 12.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            series?.synopsis?.let { synopsis ->
                item {
                    Text(synopsis, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                    if (firstEpisode != null) {
                        Button(border = com.sirktv.app.presentation.common.tvNoButtonBorder(),
                            onClick = { onEpisodeSelected(firstEpisode.seriesId, firstEpisode.seasonNumber, firstEpisode.episodeNumber) },
                            colors = ButtonDefaults.colors(containerColor = SirKTVPrimary, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().tvFocusStyle(accent = TvFocusAccent.BORDER)
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
                        modifier = Modifier.fillMaxWidth().tvFocusStyle(accent = TvFocusAccent.BORDER)
                    ) {
                        TvText(if (series?.isFavorite == true) "♥ Favorited" else "♡ Add to Favorites")
                    }
                }
            }
        }

        // RIGHT — seasons + episodes, fills all remaining width/height.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.SpaceLg)
        ) {
            if (uiState.seasons.isNotEmpty()) {
                Text(
                    "Seasons",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = Dimens.SpaceSm)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = Dimens.SpaceMd)
                ) {
                    items(uiState.seasons, key = { it.seasonNumber }) { season ->
                        SeasonTab(
                            label = "Season ${season.seasonNumber}",
                            selected = uiState.selectedSeasonNumber == season.seasonNumber,
                            onClick = { viewModel.onSeasonSelected(season.seasonNumber) }
                        )
                    }
                }

                Text(
                    "Episodes",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = Dimens.SpaceSm)
                )

                // Fills all remaining height in this weighted Column and
                // scrolls internally — every episode stays reachable no
                // matter how long the list or how much info sits on the left.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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
            } else if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SirKTVPrimary)
                }
            } else {
                Text(
                    "No episode information available for this series yet.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
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
