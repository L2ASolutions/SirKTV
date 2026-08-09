package com.sirktv.app.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text as TvText
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVTextSecondary

/**
 * Why a section (Live TV, Movies, Series, Sports) failed to load on-demand —
 * each maps to one fixed, plain-language message per the error-handling spec.
 */
enum class SectionLoadError(val message: String) {
    NETWORK("No connection. Check your WiFi and try again."),
    EMPTY("No content found. Your provider may be offline."),
    TIMEOUT("Taking too long. Try again.")
}

/** Time budget for an on-demand section sync before it's treated as [SectionLoadError.TIMEOUT]. */
const val SECTION_SYNC_TIMEOUT_MS = 15_000L

/** Centered spinner shown while a section's on-demand sync is in flight. */
@Composable
fun SectionLoadingState(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            CircularProgressIndicator(color = SirKTVPrimary)
            Text(message, color = SirKTVTextSecondary, fontSize = 13.sp)
        }
    }
}

/** Centered error message + Retry button, used identically across every on-demand section screen. */
@Composable
fun SectionErrorState(error: SectionLoadError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            Text(
                text = error.message,
                color = SirKTVTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Dimens.SpaceXl)
            )
            Button(
                onClick = onRetry,
                colors = androidx.tv.material3.ButtonDefaults.colors(containerColor = SirKTVPrimary, contentColor = androidx.compose.ui.graphics.Color.White),
                modifier = Modifier.tvFocusStyle(accent = TvFocusAccent.BORDER, cornerRadius = Dimens.ButtonCornerRadius)
            ) {
                TvText("Retry")
            }
        }
    }
}
