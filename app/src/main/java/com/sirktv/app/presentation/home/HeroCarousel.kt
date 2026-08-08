package com.sirktv.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Surface
import coil.compose.AsyncImage
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVPrimaryVariant
import kotlinx.coroutines.delay

private val HeroHeight = 260.dp
private const val AUTO_ROTATE_MS = 6_000L

/**
 * Full-width auto-rotating hero: FEATURED eyebrow, large title, genre/rating
 * subtitle, dot indicators, prev/next chevrons. Tapping the slide (outside
 * the chevrons) opens the Preview modal via [onItemClick].
 */
@Composable
fun HeroCarousel(items: List<HeroItem>, onItemClick: (HeroItem) -> Unit, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    var index by remember(items) { mutableIntStateOf(0) }

    LaunchedEffect(items, index) {
        delay(AUTO_ROTATE_MS)
        index = (index + 1) % items.size
    }

    val current = items[index.coerceIn(0, items.lastIndex)]

    Surface(
        onClick = { onItemClick(current) },
        modifier = modifier.fillMaxWidth().height(HeroHeight).tvFocusStyle(cornerRadius = Dimens.CornerRadius)
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(Dimens.CornerRadius))) {
            if (!current.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = current.imageUrl,
                    contentDescription = current.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(SirKTVPrimary, SirKTVPrimaryVariant))))
            }

            // Dark gradient scrim over the backdrop so the title/eyebrow stay readable.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.4f to Color.Black.copy(alpha = 0.25f),
                        1f to Color.Black.copy(alpha = 0.85f)
                    )
                )
            )

            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(Dimens.SpaceLg).fillMaxWidth(0.7f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("FEATURED", color = SirKTVPrimaryVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = current.title,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    current.genre?.let {
                        Text(it, color = SirKTVOnSurfaceMuted, fontSize = 14.sp)
                    }
                    current.rating?.let {
                        Text("★ ${"%.1f".format(it)}", color = SirKTVOnSurfaceMuted, fontSize = 14.sp)
                    }
                }
            }

            if (items.size > 1) {
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(Dimens.SpaceMd),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items.indices.forEach { dotIndex ->
                        Box(
                            Modifier
                                .size(7.dp)
                                .background(
                                    if (dotIndex == index) SirKTVPrimary else Color.White.copy(alpha = 0.35f),
                                    CircleShape
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
}

@Composable
private fun HeroChevron(symbol: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.size(36.dp).tvFocusStyle(cornerRadius = 18.dp)) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(symbol, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
