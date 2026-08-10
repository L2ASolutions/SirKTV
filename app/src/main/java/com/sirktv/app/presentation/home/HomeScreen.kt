package com.sirktv.app.presentation.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Surface
import androidx.tv.material3.Text as TvText
import com.sirktv.app.presentation.common.SirKTVLogoMark
import com.sirktv.app.presentation.common.TvFocusAccent
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.common.tvNoBorder
import com.sirktv.app.presentation.common.tvNoButtonBorder
import com.sirktv.app.presentation.navigation.SirKTVIcons
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVCardBackground
import com.sirktv.app.presentation.theme.SirKTVSurface
import com.sirktv.app.presentation.theme.SirKTVSurfaceElevated
import com.sirktv.app.presentation.theme.SirKTVTextPrimary
import com.sirktv.app.presentation.theme.SirKTVTextSecondary
import java.util.Calendar

private val LiveTvTileAccent = Color(0xFF1F6FEB)
private val MoviesTileAccent = Color(0xFF7C3AED)
private val SeriesTileAccent = Color(0xFF0891B2)
private val FavoritesTileAccent = Color(0xFFE5534B)
private val SettingsTileAccent = Color(0xFF57606A)
private val TileBorderIdle = Color(0xFF21262D)
private val SettingsTileHeight = 80.dp

/**
 * Home is a pure full-screen launcher now that there's no persistent sidebar
 * anywhere in the app — exactly 5 tiles (Live TV, Movies, Series, Favorites,
 * Settings) in a 2x2 grid plus a wide Settings bar, nothing else. It never
 * reads Room or calls the Xtream API; [HomeViewModel] only carries the
 * greeting name.
 */
@Composable
fun HomeScreen(
    onNavigateToLiveTv: () -> Unit,
    onNavigateToMovies: () -> Unit,
    onNavigateToSeries: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val displayName by viewModel.displayName.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    // Home is the root of the back stack — Back here must never exit
    // immediately, only after the user confirms.
    BackHandler(enabled = !showExitDialog) { showExitDialog = true }
    BackHandler(enabled = showExitDialog) { showExitDialog = false }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SirKTVBackground)
            .padding(horizontal = 48.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SirKTVLogoMark(modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text("SirKTV", color = SirKTVTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${timeOfDayGreeting()}, ${displayName ?: "there"}", color = SirKTVTextSecondary, fontSize = 14.sp)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HomeTile(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = { tint, mod -> SirKTVIcons.LiveTv(tint, mod) },
                label = "Live TV",
                subtitle = "Browse live channels",
                accentColor = LiveTvTileAccent,
                onClick = onNavigateToLiveTv
            )
            HomeTile(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = { tint, mod -> SirKTVIcons.Movies(tint, mod) },
                label = "Movies",
                subtitle = "Browse movies",
                accentColor = MoviesTileAccent,
                onClick = onNavigateToMovies
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HomeTile(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = { tint, mod -> SirKTVIcons.Series(tint, mod) },
                label = "Series",
                subtitle = "Browse TV series",
                accentColor = SeriesTileAccent,
                onClick = onNavigateToSeries
            )
            HomeTile(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = { tint, mod -> SirKTVIcons.Favorites(tint = tint, filled = true, modifier = mod) },
                label = "Favorites",
                subtitle = "Your saved content",
                accentColor = FavoritesTileAccent,
                onClick = onNavigateToFavorites
            )
        }

        HomeTile(
            modifier = Modifier.fillMaxWidth().height(SettingsTileHeight),
            icon = { tint, mod -> SirKTVIcons.Settings(tint, mod) },
            label = "Settings",
            subtitle = "Player, subtitles, preferences",
            accentColor = SettingsTileAccent,
            onClick = onNavigateToSettings,
            isWide = true
        )
    }

    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = { activity?.finish() },
            onDismiss = { showExitDialog = false }
        )
    }
}

private fun timeOfDayGreeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
private fun HomeTile(
    icon: @Composable (Color, Modifier) -> Unit,
    label: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isWide: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.03f else 1f, label = "homeTileScale")
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) SirKTVSurfaceElevated else SirKTVCardBackground,
        label = "homeTileBg"
    )
    val borderColor by animateColorAsState(targetValue = if (isFocused) accentColor else TileBorderIdle, label = "homeTileBorder")
    val borderWidth by animateDpAsState(targetValue = if (isFocused) 2.dp else 1.dp, label = "homeTileBorderWidth")
    val iconTint by animateColorAsState(targetValue = if (isFocused) accentColor else SirKTVTextSecondary, label = "homeTileIconTint")

    Surface(
        border = tvNoBorder(),
        onClick = onClick,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(borderWidth, borderColor, RoundedCornerShape(12.dp)),
            contentAlignment = if (isWide) Alignment.CenterStart else Alignment.Center
        ) {
            if (isWide) {
                Row(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    icon(iconTint, Modifier.size(24.dp))
                    Column {
                        Text(label, color = SirKTVTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(subtitle, color = SirKTVTextSecondary, fontSize = 12.sp)
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(accentColor.copy(alpha = if (isFocused) 0.25f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        icon(iconTint, Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(label, color = SirKTVTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, color = SirKTVTextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ExitConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val noFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { noFocusRequester.requestFocus() } }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .background(SirKTVSurface, RoundedCornerShape(16.dp))
                .padding(Dimens.SpaceLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
        ) {
            Text("Exit SirKTV?", color = SirKTVTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                Button(
                    border = tvNoButtonBorder(),
                    onClick = onDismiss,
                    modifier = Modifier.focusRequester(noFocusRequester).tvFocusStyle(accent = TvFocusAccent.BORDER)
                ) {
                    TvText("No")
                }
                Button(border = tvNoButtonBorder(), onClick = onConfirm, modifier = Modifier.tvFocusStyle(accent = TvFocusAccent.BORDER)) {
                    TvText("Yes")
                }
            }
        }
    }
}
