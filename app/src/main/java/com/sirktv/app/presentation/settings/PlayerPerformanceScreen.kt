package com.sirktv.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirktv.app.domain.model.BufferProfile
import com.sirktv.app.domain.model.StreamQuality
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary

/** Embedded inline as the expanded panel for the "Player Performance" settings tile. */
@Composable
fun PlayerPerformanceScreen(viewModel: PlayerPerformanceViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()

    Box(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)
        ) {
            SettingsSectionCard {
                SettingsSegmentedRow(
                    label = "Buffer size",
                    options = listOf("Low" to BufferProfile.LOW, "Medium" to BufferProfile.MEDIUM, "High" to BufferProfile.HIGH, "Custom" to BufferProfile.CUSTOM),
                    selected = settings.bufferProfile,
                    onSelect = viewModel::setBufferProfile
                )
                val buffer = settings.resolveBufferMillis()
                Text(
                    "Min ${buffer.minBufferMs / 1000}s · Max ${buffer.maxBufferMs / 1000}s · Start ${buffer.startBufferMs / 1000f}s · Rebuffer ${buffer.rebufferThresholdMs / 1000}s",
                    color = SirKTVOnSurfaceMuted,
                    fontSize = 11.sp
                )
            }

            SettingsSectionCard {
                SettingsSegmentedRow(
                    label = "Preferred stream quality",
                    options = listOf("Auto" to StreamQuality.AUTO, "1080p" to StreamQuality.P1080, "720p" to StreamQuality.P720, "480p" to StreamQuality.P480),
                    selected = settings.preferredQuality,
                    onSelect = viewModel::setQuality
                )
            }

            SettingsSectionCard {
                SettingsToggleRow(
                    label = "Hardware decoding",
                    subtitle = "Keep on — critical for Fire Stick performance",
                    checked = settings.hardwareDecodingEnabled,
                    onCheckedChange = viewModel::setHardwareDecoding
                )
            }

            SettingsSectionCard {
                SettingsSegmentedRow(
                    label = "Reconnect attempts",
                    options = listOf("3" to 3, "5" to 5, "10" to 10),
                    selected = settings.reconnectAttempts,
                    onSelect = viewModel::setReconnectAttempts
                )
            }

            SettingsSectionCard {
                SettingsToggleRow(
                    label = "Show buffer health in player",
                    checked = settings.showBufferHealth,
                    onCheckedChange = viewModel::setShowBufferHealth
                )
            }

            SettingsSectionCard {
                Text("User agent", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Only change this if your provider requires a specific client identifier.",
                    color = SirKTVOnSurfaceMuted,
                    fontSize = 11.sp
                )
                OutlinedTextField(
                    value = settings.userAgent,
                    onValueChange = viewModel::setUserAgent,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SirKTVPrimary,
                        unfocusedBorderColor = SirKTVOnSurfaceMuted,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().tvFocusStyle()
                )
            }
        }
    }
}
