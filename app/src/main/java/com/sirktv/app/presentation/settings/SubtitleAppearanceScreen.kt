package com.sirktv.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirktv.app.domain.model.SubtitleAppearance
import com.sirktv.app.domain.model.SubtitleBackground
import com.sirktv.app.domain.model.SubtitleEdgeStyle
import com.sirktv.app.domain.model.SubtitleTextColor
import com.sirktv.app.domain.model.SubtitleTextSize
import com.sirktv.app.presentation.theme.Dimens

/** Embedded inline as the expanded panel for the "Subtitle Appearance" settings tile. */
@Composable
fun SubtitleAppearanceScreen(viewModel: SubtitleAppearanceViewModel = hiltViewModel()) {
    val appearance by viewModel.appearance.collectAsState()

    Box(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)
        ) {
            SubtitlePreview(appearance)

            SettingsSectionCard {
                SettingsSegmentedRow(
                    label = "Text Size",
                    options = listOf(
                        "Small" to SubtitleTextSize.SMALL,
                        "Medium" to SubtitleTextSize.MEDIUM,
                        "Large" to SubtitleTextSize.LARGE,
                        "Extra Large" to SubtitleTextSize.EXTRA_LARGE
                    ),
                    selected = appearance.textSize,
                    onSelect = viewModel::setTextSize
                )
            }

            SettingsSectionCard {
                SettingsSegmentedRow(
                    label = "Text Color",
                    options = listOf(
                        "White" to SubtitleTextColor.WHITE,
                        "Yellow" to SubtitleTextColor.YELLOW,
                        "Green" to SubtitleTextColor.GREEN,
                        "Cyan" to SubtitleTextColor.CYAN
                    ),
                    selected = appearance.textColor,
                    onSelect = viewModel::setTextColor
                )
            }

            SettingsSectionCard {
                SettingsSegmentedRow(
                    label = "Background",
                    options = listOf(
                        "None" to SubtitleBackground.NONE,
                        "Semi-transparent" to SubtitleBackground.SEMI_TRANSPARENT,
                        "Solid" to SubtitleBackground.SOLID
                    ),
                    selected = appearance.background,
                    onSelect = viewModel::setBackground
                )
            }

            SettingsSectionCard {
                SettingsSegmentedRow(
                    label = "Edge Style",
                    options = listOf(
                        "None" to SubtitleEdgeStyle.NONE,
                        "Outline" to SubtitleEdgeStyle.OUTLINE,
                        "Drop Shadow" to SubtitleEdgeStyle.DROP_SHADOW
                    ),
                    selected = appearance.edgeStyle,
                    onSelect = viewModel::setEdgeStyle
                )
            }
        }
    }
}

@Composable
private fun SubtitlePreview(appearance: SubtitleAppearance) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(Color(0xFF14141C), RoundedCornerShape(Dimens.CornerRadius)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier.padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .background(appearance.background.toPreviewColor(), RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "This is how subtitles will look",
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        color = appearance.textColor.toPreviewColor(),
                        fontSize = appearance.textSize.toPreviewSp(),
                        shadow = appearance.edgeStyle.toPreviewShadow()
                    )
                )
            }
        }
    }
}

private fun SubtitleTextColor.toPreviewColor(): Color = when (this) {
    SubtitleTextColor.WHITE -> Color.White
    SubtitleTextColor.YELLOW -> Color(0xFFFFEB3B)
    SubtitleTextColor.GREEN -> Color(0xFF4CAF50)
    SubtitleTextColor.CYAN -> Color(0xFF00E5FF)
}

private fun SubtitleBackground.toPreviewColor(): Color = when (this) {
    SubtitleBackground.NONE -> Color.Transparent
    SubtitleBackground.SEMI_TRANSPARENT -> Color.Black.copy(alpha = 0.6f)
    SubtitleBackground.SOLID -> Color.Black
}

private fun SubtitleTextSize.toPreviewSp() = when (this) {
    SubtitleTextSize.SMALL -> 14.sp
    SubtitleTextSize.MEDIUM -> 18.sp
    SubtitleTextSize.LARGE -> 22.sp
    SubtitleTextSize.EXTRA_LARGE -> 26.sp
}

private fun SubtitleEdgeStyle.toPreviewShadow(): Shadow? = when (this) {
    SubtitleEdgeStyle.NONE -> null
    // Approximates an outline as a tight, low-offset soft shadow all around the glyphs.
    SubtitleEdgeStyle.OUTLINE -> Shadow(color = Color.Black, offset = Offset(0f, 0f), blurRadius = 6f)
    SubtitleEdgeStyle.DROP_SHADOW -> Shadow(color = Color.Black.copy(alpha = 0.8f), offset = Offset(3f, 3f), blurRadius = 4f)
}
