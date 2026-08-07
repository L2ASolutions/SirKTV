package com.sirktv.app.presentation.theme

import androidx.compose.ui.graphics.Color

val SirKTVBackground = Color(0xFF0A0A0F)
val SirKTVSurface = Color(0xFF16161D)
val SirKTVSurfaceVariant = Color(0xFF201F29)

// Brand primary is Royal Blue #0066FF — used for the wordmark, large surfaces,
// and buttons. Raw #0066FF sits close to WCAG AA failure for small text/rings
// on a near-black background (~3.1:1), so SirKTVFocusBorder below uses a
// lifted #2E7BFF (~4.6:1) for focus rings specifically instead of the raw brand hex.
val SirKTVPrimary = Color(0xFF0066FF)
val SirKTVPrimaryVariant = Color(0xFF9A86FF)
val SirKTVOnPrimary = Color(0xFFFFFFFF)
val SirKTVOnBackground = Color(0xFFF5F3FF)
val SirKTVOnSurfaceMuted = Color(0xFFAAAAAA)
val SirKTVError = Color(0xFFFF5252)
val SirKTVFocusBorder = Color(0xFF2E7BFF)

// Sports section accent — intentionally breaks from the app-wide blue focus
// language so entering Sports reads as a distinct, live-scores experience.
val SirKTVSportsAccent = Color(0xFF00C853)
val SirKTVSportsAccentBright = Color(0xFF00E676)
