package com.sirktv.app.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVPrimaryVariant
import com.sirktv.app.presentation.theme.SirKTVSurface

/** Opened by tapping the hero carousel: large image header, title, synopsis, Play/Favorite, close X. */
@Composable
fun HeroPreviewModal(
    item: HeroItem,
    synopsis: String?,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SirKTVSurface)
        ) {
            Box(Modifier.fillMaxWidth().height(240.dp)) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(SirKTVPrimary, SirKTVPrimaryVariant))))
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f))
                    )
                )
                Surface(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(32.dp).tvFocusStyle(cornerRadius = 16.dp)) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f), CircleShape), contentAlignment = Alignment.Center) {
                        Text("✕", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomStart).padding(Dimens.SpaceMd)
                )
            }

            Column(
                modifier = Modifier.padding(Dimens.SpaceLg).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item.genre?.let { Text(it, color = SirKTVOnSurfaceMuted, fontSize = 13.sp) }
                    item.rating?.let { Text("★ ${"%.1f".format(it)}", color = SirKTVOnSurfaceMuted, fontSize = 13.sp) }
                }
                Text(
                    text = synopsis ?: "Loading synopsis…",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                    Button(
                        onClick = onPlay,
                        colors = ButtonDefaults.colors(containerColor = SirKTVPrimary, contentColor = Color.White),
                        modifier = Modifier.tvFocusStyle(accent = TvFocusAccent.BORDER)
                    ) {
                        TvText("▶ Play")
                    }
                    Button(
                        onClick = onToggleFavorite,
                        colors = if (item.isFavorite) {
                            ButtonDefaults.colors(containerColor = SirKTVPrimary, contentColor = Color.White)
                        } else {
                            ButtonDefaults.colors(containerColor = Color.White.copy(alpha = 0.10f), contentColor = Color.White)
                        },
                        modifier = Modifier.tvFocusStyle(accent = TvFocusAccent.BORDER)
                    ) {
                        TvText(if (item.isFavorite) "♥ Favorited" else "♡ Add to Favorites")
                    }
                }
            }
        }
    }
}
