package com.sirktv.app.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirktv.app.presentation.theme.SirKTVTextTertiary
import kotlinx.coroutines.delay

/**
 * Screens with no sidebar (Live TV/Movies/Series/Sports browse) repurpose
 * hardware Back to jump straight to Home instead of doing nothing — this is
 * the only on-screen cue that tells the user that. Shown for 3 seconds after
 * entry, then fades out permanently for that composition; it never reappears
 * just because the user re-focuses something.
 */
@Composable
fun BackHomeHint(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(3000)
        visible = false
    }
    AnimatedVisibility(visible = visible, exit = fadeOut(animationSpec = tween(600)), modifier = modifier) {
        Text("← Home", color = SirKTVTextTertiary, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
    }
}
