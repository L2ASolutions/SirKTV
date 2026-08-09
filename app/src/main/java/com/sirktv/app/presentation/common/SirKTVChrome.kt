package com.sirktv.app.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Surface
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceStrong
import kotlinx.coroutines.launch

/**
 * The persistent header shown on every screen except the fullscreen player
 * and Login: brand + "Welcome back, {name}" on the left, Search/Favorites/
 * Refresh/Settings icons on the right, and a nav pill row underneath with the
 * currently active destination filled solid.
 */
@Composable
fun SirKTVChrome(
    activeItem: SirKTVNavItem,
    onNavigate: (SirKTVNavItem) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SirKTVChromeViewModel = hiltViewModel()
) {
    val greetingName by viewModel.greetingName.collectAsState()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            SirKTVLogoMark()
            Column(modifier = Modifier.weight(1f)) {
                Text("SirKTV", color = SirKTVOnSurfaceStrong, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                greetingName?.let {
                    Text("Welcome back, $it", color = SirKTVOnSurfaceMuted, fontSize = 12.sp)
                }
            }
            ChromeIconButton(icon = "🔍", contentDescription = "Search", onClick = { onNavigate(SirKTVNavItem.SEARCH) })
            ChromeIconButton(icon = "♥", contentDescription = "Favorites", onClick = { onNavigate(SirKTVNavItem.FAVORITES) })
            SpinningRefreshIcon(onClick = onRefresh)
            ChromeIconButton(icon = "⚙", contentDescription = "Settings", onClick = { onNavigate(SirKTVNavItem.SETTINGS) })
        }

        LazyRow(
            modifier = Modifier.padding(top = Dimens.SpaceSm),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
        ) {
            items(SirKTVNavItem.entries.toList(), key = { it.name }) { item ->
                CategoryPill(label = item.label, selected = item == activeItem, onClick = { onNavigate(item) })
            }
        }
    }
}

@Composable
private fun ChromeIconButton(icon: String, contentDescription: String, onClick: () -> Unit, rotationDegrees: Float = 0f) {
    Surface(onClick = onClick, modifier = Modifier.tvFocusStyle(cornerRadius = 20.dp)) {
        Box(
            modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 16.sp, modifier = Modifier.graphicsLayer { rotationZ = rotationDegrees })
        }
    }
}

@Composable
private fun SpinningRefreshIcon(onClick: () -> Unit) {
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    ChromeIconButton(
        icon = "⟳",
        contentDescription = "Refresh",
        rotationDegrees = rotation.value,
        onClick = {
            onClick()
            scope.launch {
                val target = rotation.value + 360f
                rotation.animateTo(target, animationSpec = tween(durationMillis = 900, easing = LinearEasing))
            }
        }
    )
}
