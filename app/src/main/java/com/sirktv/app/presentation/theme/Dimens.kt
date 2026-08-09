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

    val CornerRadius = 12.dp

    /** Alpha applied to unfocused focusable elements so the focused one reads unambiguously. */
    const val UnfocusedAlpha = 0.85f

    // Premium D-pad focus language: a soft Royal Blue glow + gentle scale for
    // cards/tabs/icons, a thin 2dp border only for Button (which already has
    // its own solid fill and needs a crisper edge instead of a glow), and a
    // left accent bar for dense list rows (channel rows) instead of any
    // border at all.
    const val FocusScale = 1.05f
    val FocusGlowElevation = 20.dp
    val ButtonFocusBorderWidth = 2.dp
    val RowFocusAccentBarWidth = 4.dp
}
