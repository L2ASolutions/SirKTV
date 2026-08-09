package com.sirktv.app.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text as TvText
import com.sirktv.app.presentation.common.SirKTVChrome
import com.sirktv.app.presentation.common.SirKTVNavItem
import com.sirktv.app.presentation.common.TvFocusAccent
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVError
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceStrong
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVSurface
import androidx.tv.material3.Surface

@Composable
fun SettingsScreen(
    onNavigate: (SirKTVNavItem) -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var expandedTile by remember { mutableStateOf<SettingsTile?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.NavigateToLogin -> onLoggedOut()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(SirKTVBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)
        ) {
            item {
                SirKTVChrome(activeItem = SirKTVNavItem.SETTINGS, onNavigate = onNavigate, onRefresh = {})
            }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
                ) {
                    items(SettingsTile.entries.toList()) { tile ->
                        SettingsTileCard(
                            tile = tile,
                            selected = expandedTile == tile,
                            onClick = { expandedTile = if (expandedTile == tile) null else tile }
                        )
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = expandedTile != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(Modifier.fillMaxWidth().padding(top = Dimens.SpaceSm)) {
                        when (expandedTile) {
                            SettingsTile.PLAYER_PERFORMANCE -> PlayerPerformanceScreen()
                            SettingsTile.SUBTITLE_APPEARANCE -> SubtitleAppearanceScreen()
                            SettingsTile.STARTUP_PREFERENCES -> StartupPreferencesScreen()
                            SettingsTile.PARENTAL_CONTROLS -> ParentalControlsPanel()
                            SettingsTile.ABOUT -> AboutPanel()
                            null -> Unit
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = viewModel::onLogoutClicked,
                    colors = ButtonDefaults.colors(containerColor = SirKTVError, contentColor = Color.White),
                    modifier = Modifier.tvFocusStyle(accent = TvFocusAccent.BORDER)
                ) {
                    TvText("Log Out")
                }
            }
        }
    }
}

@Composable
private fun SettingsTileCard(tile: SettingsTile, selected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val background by animateColorAsState(
        targetValue = when {
            selected -> SirKTVPrimary.copy(alpha = 0.22f)
            isFocused -> SirKTVPrimary.copy(alpha = 0.12f)
            else -> SirKTVSurface
        },
        label = "settingsTileBackground"
    )
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().aspectRatio(1.5f).tvFocusStyle { isFocused = it }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background, RoundedCornerShape(Dimens.CornerRadius)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.padding(bottom = 8.dp)) {
                    Box(
                        Modifier
                            .background(SirKTVPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text("⚙", fontSize = 14.sp, color = SirKTVPrimary)
                    }
                }
                Text(tile.label, color = SirKTVOnSurfaceStrong, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
