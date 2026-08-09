package com.sirktv.app.presentation.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.common.tvPressLongPress
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVCardBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceStrong
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVPrimaryVariant
import com.sirktv.app.presentation.theme.SirKTVSurfaceVariant
import androidx.tv.material3.Surface

/** Section header shared by every horizontally-scrolling row on Home/Movies/Series/Favorites. */
@Composable
fun RowHeader(title: String, count: Int? = null, modifier: Modifier = Modifier) {
    Text(
        text = if (count != null) "$title  ($count)" else title,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        modifier = modifier.padding(bottom = Dimens.SpaceSm)
    )
}

/** One horizontally-scrolling, D-pad navigable row with a header — shared by Home, Movies, and Series. */
@Composable
fun <T> MediaRow(title: String, rowItems: List<T>, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    Column(modifier = modifier) {
        RowHeader(title = title, count = rowItems.size)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            items(rowItems) { item -> content(item) }
        }
    }
}

/**
 * One reusable poster/backdrop card for every row in the app (Home, Movies,
 * Series, Favorites, Search, Sports). A missing image falls back to a
 * brand-gradient monogram rather than a broken-image icon or a stock
 * placeholder. Title/subtitle are overlaid directly on the artwork, bottom-
 * aligned against a scrim that fades from transparent to
 * rgba(0,0,0,0.8) — this keeps the text readable no matter how light the
 * underlying poster is, instead of relying on the app background showing
 * through behind a separate text row.
 */
@Composable
fun MediaCard(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    aspectRatio: Float = 16f / 9f,
    progressFraction: Float? = null,
    badge: String? = null,
    rating: Float? = null,
    isFavorite: Boolean = false,
    // Opt-in only: when set, the card swaps its root from a tv.material3
    // Surface (which owns its own internal D-pad click handling) to a plain
    // focusable Box driving tvPressLongPress directly — bolting long-press
    // detection onto Surface's opaque internal click node risks double
    // handling, so every other call site (which passes null) keeps the
    // original, unmodified Surface behavior.
    onLongClick: (() -> Unit)? = null
) {
    val content = @Composable {
        MediaCardArt(title, imageUrl, subtitle, aspectRatio, progressFraction, badge, rating, isFavorite)
    }
    if (onLongClick != null) {
        Box(modifier = modifier.tvFocusStyle().tvPressLongPress(onClick = onClick, onLongPress = onLongClick)) {
            content()
        }
    } else {
        Surface(onClick = onClick, modifier = modifier.tvFocusStyle()) {
            content()
        }
    }
}

@Composable
private fun MediaCardArt(
    title: String,
    imageUrl: String?,
    subtitle: String?,
    aspectRatio: Float,
    progressFraction: Float?,
    badge: String?,
    rating: Float?,
    isFavorite: Boolean
) {
    val hasImage = !imageUrl.isNullOrBlank()

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(Dimens.CornerRadius))
    ) {
            if (hasImage) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(SirKTVSurfaceVariant, RoundedCornerShape(Dimens.CornerRadius))
                )
            } else {
                // No artwork: a flat dark card (never a bright/colorful fill
                // sitting directly behind the title) so the monogram + text
                // stay readable without needing a scrim on top of it.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SirKTVCardBackground, RoundedCornerShape(Dimens.CornerRadius)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(listOf(SirKTVPrimary, SirKTVPrimaryVariant)),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(title.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            // Bottom scrim + overlaid title/subtitle/rating — always on top of
            // the artwork, transparent at the top so it never clips into a
            // badge/favorite icon near the top of the card. Cards without
            // artwork already sit on the flat dark card background above, so
            // they skip the scrim and use the standard on-dark text colors
            // instead of white.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .then(
                        if (hasImage) {
                            Modifier.background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.45f to Color.Black.copy(alpha = 0.6f),
                                    1f to Color.Black.copy(alpha = 0.85f)
                                ),
                                RoundedCornerShape(bottomStart = Dimens.CornerRadius, bottomEnd = Dimens.CornerRadius)
                            )
                        } else Modifier
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Column {
                    Text(
                        text = title,
                        color = if (hasImage) Color.White else SirKTVOnSurfaceStrong,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        subtitle?.let {
                            Text(
                                text = it,
                                color = if (hasImage) Color.White.copy(alpha = 0.75f) else SirKTVOnSurfaceMuted,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        rating?.let {
                            Text(
                                "★ ${"%.1f".format(it)}",
                                color = if (hasImage) Color.White.copy(alpha = 0.85f) else SirKTVOnSurfaceMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            badge?.let {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(it, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♥", color = SirKTVPrimary, fontSize = 13.sp, modifier = Modifier.padding(4.dp))
                }
            }

            progressFraction?.let { fraction ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxSize().background(SirKTVPrimary))
                }
            }
    }
}

/**
 * Standalone focus stop placed *below* a [MediaCard] in browse grids (Movies,
 * Series) so adding/removing a favorite doesn't need a second focusable
 * target nested inside the card's own click target — D-pad Up/Down between
 * a tile and its own toggle stays predictable that way.
 */
@Composable
fun FavoriteToggleChip(isFavorite: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    // Small pulse on the icon whenever the favorite state flips, so toggling
    // reads as a deliberate "fill" action rather than an instant color swap.
    val fillScale by animateFloatAsState(
        targetValue = if (isFavorite) 1f else 0.85f,
        animationSpec = spring(dampingRatio = 0.4f),
        label = "favoriteFillScale"
    )
    Surface(onClick = onToggle, modifier = modifier.fillMaxWidth().tvFocusStyle(cornerRadius = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isFavorite) SirKTVPrimary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isFavorite) "♥ Favorited" else "♡ Add to Favorites",
                color = if (isFavorite) SirKTVPrimary else Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { scaleX = fillScale; scaleY = fillScale }
            )
        }
    }
}
