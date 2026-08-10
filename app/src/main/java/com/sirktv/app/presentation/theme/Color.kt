package com.sirktv.app.presentation.theme

import androidx.compose.ui.graphics.Color

// Premium dark-navy IPTV palette (Hot Player feel) — a deep navy rather than
// pure black or neutral gray, with progressively lighter navy surface tiers
// so raised content (cards, sheets, the login card) reads as physically
// closer to the viewer without ever needing a border to separate it from the
// background. SirKTVBackground/Surface/etc. are kept as the primary names —
// dozens of existing call sites already reference them, so repointing their
// values here (rather than renaming every call site) applies the navy
// palette across the whole app at once. AppBackground/AppSurface/etc. are
// aliases of the exact same values for anything written against those names.
val SirKTVBackground = Color(0xFF0A0F1E)
val SirKTVSurface = Color(0xFF141E33)
val SirKTVSurfaceElevated = Color(0xFF1A2540)
val SirKTVSurfaceVariant = SirKTVSurfaceElevated

// Card background used by every channel/EPG/poster card (Home, Live TV
// channel list, Live TV browse, Favorites Channels tab) — distinct from the
// navy app background so cards read as raised surfaces.
val SirKTVCardBackground = Color(0xFF111827)

// Brand primary is Royal Blue #0066FF — used for the wordmark, large surfaces,
// buttons, and (per the focus-visibility spec) the glow on every focusable
// element, exactly matching the brand hex rather than a lifted tint. NEVER
// rendered as a border — see tvFocusStyle, which glow-only by design.
val SirKTVPrimary = Color(0xFF0066FF)
val SirKTVPrimaryVariant = Color(0xFF9A86FF)
val SirKTVOnPrimary = Color(0xFFFFFFFF)
val SirKTVOnBackground = Color(0xFFFFFFFF)

// Text hierarchy — three tiers: pure white for headlines/titles/primary
// content (never gray, never blue-gray for primary labels — this is a
// dark-mode-only app, so nothing here should ever read as dark-on-dark), a
// muted blue-gray for body/secondary copy (counts, metadata, subtitles), and
// a very muted blue-gray for tertiary text (timestamps, small labels) that
// should recede from everything else.
val SirKTVTextPrimary = Color(0xFFFFFFFF)
val SirKTVTextSecondary = Color(0xFF8899BB)
val SirKTVTextTertiary = Color(0xFF445577)

// Back-compat aliases — dozens of existing call sites reference these names;
// repointing them at the new tiers (rather than renaming every call site)
// applies the new palette everywhere at once.
val SirKTVOnSurfaceStrong = SirKTVTextPrimary
val SirKTVOnSurfaceMuted = SirKTVTextSecondary

val SirKTVDivider = Color(0xFF1E2D4A)

// Netflix red — reserved for genuine error states only (never a general accent).
val SirKTVError = Color(0xFFE50914)
val SirKTVFocusBorder = Color(0xFF0066FF)
val SirKTVLiveIndicator = Color(0xFFFF4757)

// Sports section accent — intentionally breaks from the app-wide blue focus
// language so entering Sports reads as a distinct, live-scores experience.
val SirKTVSportsAccent = Color(0xFF00C853)
val SirKTVSportsAccentBright = Color(0xFF00E676)

// Series section accent — used for the Home launcher tile only.
val SirKTVSeriesAccent = Color(0xFF00BCD4)

// --- Named aliases matching the Hot-Player-style palette spec exactly. Same
// values as above — use whichever name reads more clearly at the call site. ---
val AppBackground = SirKTVBackground
val AppSidebar = Color(0xFF0F1626)
val AppSurface = SirKTVSurface
val AppSurfaceElevated = SirKTVSurfaceElevated
val AppCard = SirKTVCardBackground
val AppBorder = SirKTVDivider
val AccentBlue = SirKTVPrimary
val TextPrimary = SirKTVTextPrimary
val TextSecondary = SirKTVTextSecondary
val TextTertiary = SirKTVTextTertiary
