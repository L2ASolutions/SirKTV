package com.sirktv.app.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVPrimaryVariant

/**
 * The SirKTV mark: three ascending signal bars with a live-beacon dot at the
 * peak. Coordinates mirror the 64x64 viewBox used in the product's brand
 * reference so the app icon, this composable, and the web spec stay in sync.
 */
@Composable
fun SirKTVLogoMark(modifier: Modifier = Modifier.size(56.dp)) {
    Canvas(modifier = modifier) {
        val scale = size.width / 64f
        val gradient = Brush.linearGradient(colors = listOf(SirKTVPrimary, SirKTVPrimaryVariant))

        fun bar(x: Float, y: Float, w: Float, h: Float) {
            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x * scale, y * scale),
                size = Size(w * scale, h * scale),
                cornerRadius = CornerRadius(3.5f * scale, 3.5f * scale)
            )
        }

        bar(x = 12f, y = 34f, w = 9f, h = 20f)
        bar(x = 27f, y = 16f, w = 9f, h = 38f)
        bar(x = 42f, y = 25f, w = 9f, h = 29f)
        drawCircle(color = SirKTVPrimaryVariant, radius = 4.2f * scale, center = Offset(31.5f * scale, 10f * scale))
    }
}
