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
val SirKTVBackground = Color(0xFF0D1117)
val SirKTVSurface = Color(0xFF161B22)
val SirKTVSurfaceElevated = Color(0xFF1C2333)
val SirKTVSurfaceVariant = SirKTVSurfaceElevated

// Card background used by every channel/EPG/poster card (Home, Live TV
// channel list, Live TV browse, Favorites Channels tab) — same tier as
// SirKTVSurface so cards read as raised surfaces without needing a border.
val SirKTVCardBackground = SirKTVSurface

// Brand primary — used for the wordmark, large surfaces, buttons, and (per
// the focus-visibility spec) the glow on every focusable element. NEVER
// rendered as a border — see tvFocusStyle, which glow-only by design.
val SirKTVPrimary = Color(0xFF1F6FEB)
val SirKTVPrimaryVariant = Color(0xFF9A86FF)
// Selected-row tint (e.g. category/channel rows) — a dim navy fill, not the
// bright primary itself, so a selected row reads as "active" without
// competing with the focus glow.
val SirKTVPrimaryContainer = Color(0xFF1A2A4A)
val SirKTVOnPrimary = Color(0xFFFFFFFF)
val SirKTVOnBackground = Color(0xFFE6EDF3)

// Text hierarchy — three tiers: a soft off-white for headlines/titles/primary
// content (pure white is reserved for text drawn over images/video scrims,
// where the extra contrast is needed — see Color.White usages in poster
// cards and player overlays), a muted gray for body/secondary copy (counts,
// metadata, subtitles), and a dim gray for tertiary text/placeholders that
// should recede from everything else.
val SirKTVTextPrimary = Color(0xFFE6EDF3)
val SirKTVTextSecondary = Color(0xFF848D97)
val SirKTVTextTertiary = Color(0xFF484F58)

// Back-compat aliases — dozens of existing call sites reference these names;
// repointing them at the new tiers (rather than renaming every call site)
// applies the new palette everywhere at once.
val SirKTVOnSurfaceStrong = SirKTVTextPrimary
val SirKTVOnSurfaceMuted = SirKTVTextSecondary

val SirKTVDivider = Color(0xFF1E2D4A)

// Reserved for genuine error states only (never a general accent).
val SirKTVError = Color(0xFFDA3633)
val SirKTVFocusBorder = SirKTVPrimary
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
// Darker rail background for side panels that sit beside the main content
// area — nav sidebar, category columns, mini-player column — one tier below
// SirKTVBackground so those rails read as physically recessed.
val AppSidebar = Color(0xFF080C12)
val AppSurface = SirKTVSurface
val AppSurfaceElevated = SirKTVSurfaceElevated
val AppCard = SirKTVCardBackground
val AppBorder = SirKTVDivider
val AccentBlue = SirKTVPrimary
val TextPrimary = SirKTVTextPrimary
val TextSecondary = SirKTVTextSecondary
val TextTertiary = SirKTVTextTertiary
