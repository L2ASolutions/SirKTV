package com.sirktv.app.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Surface
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVTextPrimary
import com.sirktv.app.presentation.theme.SirKTVTextSecondary
import com.sirktv.app.presentation.theme.SirKTVTextTertiary
import kotlinx.coroutines.launch

/**
 * The persistent header shown on every screen except the fullscreen player
 * and Login: brand + "Welcome back, {name}" on the left, Search/Favorites/
 * Refresh/Settings icons on the right, and a nav pill row underneath with the
 * currently active destination filled solid. Transparent background — it
 * bleeds directly into the page behind it, Netflix-style, rather than
 * sitting in its own bar.
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
            Column(modifier = Modifier.weight(1f)) {
                Text("SirKTV", color = SirKTVTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                greetingName?.let {
                    Text("Welcome back, $it", color = SirKTVTextTertiary, fontSize = 13.sp)
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

/** Bare icon, no background pill/circle — brightens and scales up slightly on focus. */
@Composable
private fun ChromeIconButton(icon: String, contentDescription: String, onClick: () -> Unit, rotationDegrees: Float = 0f) {
    var isFocused by remember { mutableStateOf(false) }
    val tint by animateColorAsState(
        targetValue = if (isFocused) SirKTVTextPrimary else SirKTVTextSecondary,
        label = "chromeIconTint"
    )
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.1f else 1f, label = "chromeIconScale")
    Surface(border = com.sirktv.app.presentation.common.tvNoBorder(), 
        onClick = onClick,
        modifier = Modifier.size(40.dp).onFocusChanged { isFocused = it.isFocused }.tvFocusStyle(cornerRadius = 20.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().size(40.dp), contentAlignment = Alignment.Center) {
            Text(
                icon,
                fontSize = 20.sp,
                color = tint,
                modifier = Modifier.graphicsLayer { rotationZ = rotationDegrees; scaleX = scale; scaleY = scale }
            )
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
