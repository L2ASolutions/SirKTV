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
    val CardCornerRadius = 6.dp
    val ButtonCornerRadius = 8.dp

    /** Alpha applied to unfocused focusable elements so the focused one reads unambiguously. */
    const val UnfocusedAlpha = 0.92f

    // Premium D-pad focus language, Netflix/Apple-TV style: focus is ALWAYS a
    // soft Royal Blue glow + scale-up — never a border, on cards or buttons.
    // Cards scale a touch more (they're the primary browsing unit); buttons
    // scale less since a 56dp-tall filled button scaling 1.08x would feel
    // heavy. tvFocusStyle picks between these by [TvFocusAccent].
    const val FocusScale = 1.08f
    const val ButtonFocusScale = 1.03f
    val FocusGlowElevation = 20.dp
    val RowFocusAccentBarWidth = 4.dp
}
