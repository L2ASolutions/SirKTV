package com.sirktv.app.presentation.screensaver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVPrimaryVariant
import kotlinx.coroutines.delay

private const val ROTATE_MS = 6_000L

/** Fullscreen idle takeover — cycles through [items] every 6 seconds until dismissed. */
@Composable
fun ScreensaverOverlay(items: List<ScreensaverItem>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    var index by remember(items) { mutableIntStateOf(0) }

    LaunchedEffect(items, index) {
        delay(ROTATE_MS)
        index = (index + 1) % items.size
    }

    val current = items[index.coerceIn(0, items.lastIndex)]

    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (!current.backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = current.backdropUrl,
                contentDescription = current.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(SirKTVPrimary.copy(alpha = 0.5f), Color.Black))
                )
            )
        }

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Transparent, 0.35f to Color.Black.copy(alpha = 0.35f), 1f to Color.Black.copy(alpha = 0.9f))
            )
        )

        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(Dimens.SafeAreaHorizontal, Dimens.SafeAreaVertical),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("NOW SHOWING", color = SirKTVPrimaryVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(current.title, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            current.subtitle?.let {
                Text(it, color = SirKTVOnSurfaceMuted, fontSize = 16.sp)
            }
        }

        Text(
            text = "Tap anywhere to continue",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(Dimens.SafeAreaHorizontal, Dimens.SafeAreaVertical)
        )
    }
}
