package com.sirktv.app.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVFocusBorder

/**
 * Scale-and-glow focus feedback for D-pad navigation, since not every input
 * widget here is TV-native. The focused element gets a Royal Blue border glow
 * and scales up; every unfocused sibling dims to [Dimens.UnfocusedAlpha] so
 * the focused card is unambiguous at a glance.
 */
fun Modifier.tvFocusStyle(cornerRadius: Dp = Dimens.CornerRadius): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) Dimens.FocusScale else 1f,
        label = "tvFocusScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else Dimens.UnfocusedAlpha,
        label = "tvFocusAlpha"
    )

    val borderModifier = if (isFocused) {
        Modifier.border(Dimens.FocusBorderWidth, SirKTVFocusBorder, RoundedCornerShape(cornerRadius))
    } else {
        Modifier
    }

    this
        .onFocusChanged { isFocused = it.isFocused }
        .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
        .then(borderModifier)
}
