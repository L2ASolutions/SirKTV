package com.sirktv.app.presentation.theme

import androidx.compose.ui.unit.dp

/** TV-safe spacing and focus constants — avoids overscan clipping and gives D-pad focus room to animate. */
object Dimens {
    val SafeAreaHorizontal = 48.dp
    val SafeAreaVertical = 27.dp

    val SpaceXs = 4.dp
    val SpaceSm = 8.dp
    val SpaceMd = 16.dp
    val SpaceLg = 24.dp
    val SpaceXl = 32.dp
    val SpaceXxl = 48.dp

    val FocusBorderWidth = 3.dp
    val CornerRadius = 12.dp
    const val FocusScale = 1.06f

    /** Alpha applied to unfocused focusable elements so the focused one reads unambiguously. */
    const val UnfocusedAlpha = 0.85f
}
