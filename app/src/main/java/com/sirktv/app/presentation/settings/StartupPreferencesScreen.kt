package com.sirktv.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVPrimary
import androidx.tv.material3.Surface

/** Embedded inline as the expanded panel for the "Startup Preferences" settings tile. */
@Composable
fun StartupPreferencesScreen(viewModel: StartupPreferencesViewModel = hiltViewModel()) {
    val preference by viewModel.preference.collectAsState()
    val channels by viewModel.channels.collectAsState()

    Box(Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)) {
            SettingsSectionCard {
                SettingsToggleRow(
                    label = "Launch directly into Live TV",
                    subtitle = "Skip the Home tile grid and go straight to Live TV on login",
                    checked = preference.autoStartLiveTv,
                    onCheckedChange = viewModel::setAutoStart
                )
            }

            SettingsSectionCard {
                SettingsToggleRow(
                    label = "Resume last channel",
                    subtitle = "Overrides the fixed startup channel below when on",
                    checked = preference.resumeLastChannel,
                    onCheckedChange = viewModel::setResumeLastChannel
                )
            }

            SettingsSectionCard {
                Text("Startup channel", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.alpha(if (preference.resumeLastChannel) 0.4f else 1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(channels, key = { it.id }) { channel ->
                            val isSelected = channel.id == preference.startupChannelId
                            Surface(border = com.sirktv.app.presentation.common.tvNoBorder(), 
                                onClick = { if (!preference.resumeLastChannel) viewModel.setStartupChannel(channel.id) },
                                modifier = Modifier.fillMaxWidth().tvFocusStyle(cornerRadius = 8.dp)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) SirKTVPrimary.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "${channel.channelNumber} · ${channel.name}",
                                        color = if (isSelected) SirKTVPrimary else Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
