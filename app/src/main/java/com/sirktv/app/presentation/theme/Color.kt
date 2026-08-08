package com.sirktv.app.presentation.theme

import androidx.compose.ui.graphics.Color

val SirKTVBackground = Color(0xFF0A0A0F)
val SirKTVSurface = Color(0xFF16161D)
val SirKTVSurfaceVariant = Color(0xFF201F29)

// Dark navy card background used by every channel/EPG card (Home, Live TV
// channel list, Live TV browse, Favorites Channels tab) — distinct from the
// near-black app background so cards read as raised surfaces.
val SirKTVCardBackground = Color(0xFF1A1A2E)

// Brand primary is Royal Blue #0066FF — used for the wordmark, large surfaces,
// buttons, and (per the focus-visibility spec) the focus ring on every
// focusable card, exactly matching the brand hex rather than a lifted tint.
val SirKTVPrimary = Color(0xFF0066FF)
val SirKTVPrimaryVariant = Color(0xFF9A86FF)
val SirKTVOnPrimary = Color(0xFFFFFFFF)
val SirKTVOnBackground = Color(0xFFF5F3FF)

// Primary body text on a dark (non-image) background — not quite pure white.
val SirKTVOnSurfaceStrong = Color(0xFFE0E0E0)

// Secondary/subtitle text against a dark background — captions, now-playing
// program titles, muted counts/labels.
val SirKTVOnSurfaceMuted = Color(0xFFAAAAAA)
val SirKTVError = Color(0xFFFF5252)
val SirKTVFocusBorder = Color(0xFF0066FF)

// Sports section accent — intentionally breaks from the app-wide blue focus
// language so entering Sports reads as a distinct, live-scores experience.
val SirKTVSportsAccent = Color(0xFF00C853)
val SirKTVSportsAccentBright = Color(0xFF00E676)
