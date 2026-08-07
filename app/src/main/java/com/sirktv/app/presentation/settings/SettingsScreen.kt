package com.sirktv.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVPrimary
import androidx.tv.material3.Surface

@Composable
fun SettingsScreen(onNavigate: (SettingsTile) -> Unit) {
    Box(Modifier.fillMaxSize().background(SirKTVBackground).padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical)) {
        Column {
            Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(top = Dimens.SpaceLg),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Dimens.SpaceSm),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Dimens.SpaceSm)
            ) {
                items(SettingsTile.entries.toList()) { tile ->
                    SettingsTileCard(tile = tile, onClick = { onNavigate(tile) })
                }
            }
        }
    }
}

@Composable
private fun SettingsTileCard(tile: SettingsTile, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().aspectRatio(1.5f).tvFocusStyle()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(Dimens.CornerRadius)),
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
                Text(tile.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
