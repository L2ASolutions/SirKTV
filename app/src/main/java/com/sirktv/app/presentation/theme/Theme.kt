package com.sirktv.app.presentation.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme as M3Theme
import androidx.compose.material3.darkColorScheme as m3DarkColorScheme
import androidx.tv.material3.MaterialTheme as TvTheme
import androidx.tv.material3.darkColorScheme as tvDarkColorScheme

/**
 * Wraps content in both the Compose Material3 theme (needed by standard
 * text-input components, which don't yet have a TV-native equivalent) and
 * the TV Material3 theme (needed for D-pad focus/click behavior on
 * TV-native components). Both are given the same dark, brand-purple palette.
 *
 * Ripple is disabled globally: the default Material ripple draws a
 * light-on-dark splash that flashes white against this app's near-black
 * scroll surfaces on every D-pad press. Every focusable already has its own
 * accent-colored focus/press treatment via [tvFocusStyle] and friends, so the
 * ripple only ever fought that rather than adding anything.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SirKTVTheme(content: @Composable () -> Unit) {
    val m3Colors = m3DarkColorScheme(
        primary = SirKTVPrimary,
        onPrimary = SirKTVOnPrimary,
        background = SirKTVBackground,
        onBackground = SirKTVOnBackground,
        surface = SirKTVSurface,
        onSurface = SirKTVOnBackground,
        surfaceVariant = SirKTVSurfaceVariant,
        error = SirKTVError
    )
    val tvColors = tvDarkColorScheme(
        primary = SirKTVPrimary,
        onPrimary = SirKTVOnPrimary,
        background = SirKTVBackground,
        onBackground = SirKTVOnBackground,
        surface = SirKTVSurface,
        onSurface = SirKTVOnBackground,
        surfaceVariant = SirKTVSurfaceVariant,
        error = SirKTVError
    )

    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        M3Theme(colorScheme = m3Colors) {
            TvTheme(colorScheme = tvColors) {
                content()
            }
        }
    }
}
