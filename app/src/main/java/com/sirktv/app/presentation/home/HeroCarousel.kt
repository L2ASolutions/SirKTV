package com.sirktv.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text as TvText
import coil.compose.AsyncImage
import com.sirktv.app.presentation.common.TvFocusAccent
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVPrimaryVariant
import com.sirktv.app.presentation.theme.SirKTVTextPrimary
import com.sirktv.app.presentation.theme.SirKTVTextSecondary
import kotlinx.coroutines.delay

private val HeroHeight = 480.dp
private const val AUTO_ROTATE_MS = 6_000L

/**
 * Full-bleed, cinematic auto-rotating hero — no rounded corners, edge to
 * edge, with a heavy bottom scrim that blends straight into the page
 * background below it. FEATURED eyebrow, large title, a white "Play" button
 * (jumps straight into playback) and a translucent gray "More Info" button
 * (opens the preview modal), plus dot indicators and prev/next chevrons.
 */
@Composable
fun HeroCarousel(
    items: List<HeroItem>,
    onPlay: (HeroItem) -> Unit,
    onMoreInfo: (HeroItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    var index by remember(items) { mutableIntStateOf(0) }

    LaunchedEffect(items, index) {
        delay(AUTO_ROTATE_MS)
        index = (index + 1) % items.size
    }

    val current = items[index.coerceIn(0, items.lastIndex)]

    Box(modifier = modifier.fillMaxWidth().height(HeroHeight)) {
        if (!current.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = current.imageUrl,
                contentDescription = current.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(HeroHeight)
            )
        } else {
            Box(Modifier.fillMaxWidth().height(HeroHeight).background(Brush.linearGradient(listOf(SirKTVPrimary, SirKTVPrimaryVariant))))
        }

        // Heavy bottom scrim — blends straight into the page background
        // below the hero rather than reading as a separate framed panel.
        Box(
            Modifier.fillMaxWidth().height(HeroHeight).background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.55f to SirKTVBackground.copy(alpha = 0.6f),
                    1f to SirKTVBackground
                )
            )
        )

        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(Dimens.SpaceLg).fillMaxWidth(0.6f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "FEATURED",
                color = SirKTVPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = 2.sp
            )
            Text(
                text = current.title,
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                current.genre?.let {
                    Text(it, color = SirKTVTextSecondary, fontSize = 14.sp)
                }
                current.rating?.let {
                    Text("★ ${"%.1f".format(it)}", color = SirKTVTextSecondary, fontSize = 14.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm), modifier = Modifier.padding(top = Dimens.SpaceSm)) {
                Button(
                    onClick = { onPlay(current) },
                    colors = ButtonDefaults.colors(containerColor = Color.White, contentColor = SirKTVBackground),
                    modifier = Modifier.tvFocusStyle(accent = TvFocusAccent.BORDER, cornerRadius = Dimens.ButtonCornerRadius)
                ) {
                    TvText("▶ Play", fontWeight = FontWeight.W600)
                }
                Button(
                    onClick = { onMoreInfo(current) },
                    colors = ButtonDefaults.colors(containerColor = Color(0xB36D6D6E), contentColor = Color.White),
                    modifier = Modifier.tvFocusStyle(accent = TvFocusAccent.BORDER, cornerRadius = Dimens.ButtonCornerRadius)
                ) {
                    TvText("More Info", fontWeight = FontWeight.W600)
                }
            }
        }

        if (items.size > 1) {
            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(Dimens.SpaceLg),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items.indices.forEach { dotIndex ->
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(
                                if (dotIndex == index) Color.White else Color.White.copy(alpha = 0.4f),
                                RoundedCornerShape(50)
                            )
                    )
                }
            }

            HeroChevron(symbol = "‹", modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) {
                index = (index - 1 + items.size) % items.size
            }
            HeroChevron(symbol = "›", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                index = (index + 1) % items.size
            }
        }
    }
}

@Composable
private fun HeroChevron(symbol: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.size(36.dp).tvFocusStyle(cornerRadius = 18.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(36.dp).background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(symbol, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
