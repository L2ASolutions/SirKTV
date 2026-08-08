package com.sirktv.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sirktv.app.BuildConfig
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted

/** Embedded inline as the expanded panel for the "About" settings tile. */
@Composable
fun AboutPanel() {
    Box(Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)) {
            SettingsSectionCard {
                Text("SirKTV", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Version ${BuildConfig.VERSION_NAME}", color = SirKTVOnSurfaceMuted, fontSize = 13.sp)
                Text(
                    "SirKTV is a media player for Xtream Codes-compatible IPTV accounts. It plays " +
                        "whatever your provider supplies and does not host or supply content itself.",
                    color = SirKTVOnSurfaceMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}
