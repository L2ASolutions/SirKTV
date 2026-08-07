package com.sirktv.app.presentation.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
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

/**
 * One reusable poster/backdrop card for every row in the app (Home, Movies,
 * Series, Favorites, Sports). A missing image falls back to a brand-gradient
 * monogram rather than a broken-image icon or a stock placeholder.
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
    isFavorite: Boolean = false
) {
    Surface(onClick = onClick, modifier = modifier.tvFocusStyle()) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(Dimens.CornerRadius))
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().background(SirKTVSurfaceVariant, RoundedCornerShape(Dimens.CornerRadius))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(listOf(SirKTVPrimary, SirKTVPrimaryVariant)),
                                RoundedCornerShape(Dimens.CornerRadius)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(title.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
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
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                        Text("♥", color = SirKTVPrimary, fontSize = 13.sp)
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

            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = SirKTVOnSurfaceMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                fontWeight = FontWeight.Bold
            )
        }
    }
}
