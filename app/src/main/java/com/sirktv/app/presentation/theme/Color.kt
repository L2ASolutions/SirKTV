package com.sirktv.app.presentation.theme

import androidx.compose.ui.graphics.Color

// Premium dark streaming palette (Netflix/Apple TV feel) — a warm near-black
// rather than pure black, with progressively lighter surface tiers so raised
// content (cards, sheets, the login card) reads as physically closer to the
// viewer without ever needing a border to separate it from the background.
val SirKTVBackground = Color(0xFF080808)
val SirKTVSurface = Color(0xFF141414)
val SirKTVSurfaceElevated = Color(0xFF1F1F1F)
val SirKTVSurfaceVariant = SirKTVSurfaceElevated

// Card background used by every channel/EPG/poster card (Home, Live TV
// channel list, Live TV browse, Favorites Channels tab) — distinct from the
// near-black app background so cards read as raised surfaces, exactly like
// Netflix's #181818 tiles.
val SirKTVCardBackground = Color(0xFF181818)

// Brand primary is Royal Blue #0066FF — used for the wordmark, large surfaces,
// buttons, and (per the focus-visibility spec) the glow on every focusable
// element, exactly matching the brand hex rather than a lifted tint. NEVER
// rendered as a border — see tvFocusStyle, which glow-only by design.
val SirKTVPrimary = Color(0xFF0066FF)
val SirKTVPrimaryVariant = Color(0xFF9A86FF)
val SirKTVOnPrimary = Color(0xFFFFFFFF)
val SirKTVOnBackground = Color(0xFFFFFFFF)

// Text hierarchy — three tiers, cinematic/Netflix-style: pure white for
// headlines/titles, a mid gray for body/secondary copy, and a dim gray for
// metadata (rating, year, duration) that should recede from everything else.
val SirKTVTextPrimary = Color(0xFFFFFFFF)
val SirKTVTextSecondary = Color(0xFFA3A3A3)
val SirKTVTextTertiary = Color(0xFF666666)

// Back-compat aliases — dozens of existing call sites reference these names;
// repointing them at the new tiers (rather than renaming every call site)
// applies the new palette everywhere at once.
val SirKTVOnSurfaceStrong = SirKTVTextPrimary
val SirKTVOnSurfaceMuted = SirKTVTextSecondary

val SirKTVDivider = Color(0xFF2A2A2A)

// Netflix red — reserved for genuine error states only (never a general accent).
val SirKTVError = Color(0xFFE50914)
val SirKTVFocusBorder = Color(0xFF0066FF)
val SirKTVLiveIndicator = Color(0xFFFF4757)

// Sports section accent — intentionally breaks from the app-wide blue focus
// language so entering Sports reads as a distinct, live-scores experience.
val SirKTVSportsAccent = Color(0xFF00C853)
val SirKTVSportsAccentBright = Color(0xFF00E676)
