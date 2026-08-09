package com.sirktv.app.presentation.livetv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirktv.app.domain.model.StreamQuality
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVSurface
import androidx.tv.material3.Surface

@Composable
fun QualitySelectorSheet(
    current: StreamQuality,
    onSelect: (StreamQuality) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .background(SirKTVSurface)
                .padding(Dimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
        ) {
            Text("Playback quality", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "Only takes effect if the source offers more than one quality.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
            StreamQuality.entries.forEach { quality ->
                Surface(border = com.sirktv.app.presentation.common.tvNoBorder(), onClick = { onSelect(quality) }, modifier = Modifier.tvFocusStyle(cornerRadius = 8.dp)) {
                    Text(
                        text = (if (quality == current) "● " else "") + quality.label(),
                        color = if (quality == current) SirKTVPrimary else Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

private fun StreamQuality.label(): String = when (this) {
    StreamQuality.AUTO -> "Auto"
    StreamQuality.P1080 -> "1080p"
    StreamQuality.P720 -> "720p"
    StreamQuality.P480 -> "480p"
}
