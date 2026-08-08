package com.sirktv.app.presentation.common

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
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

/**
 * D-pad OK short-press vs long-press on a single focusable item, for cards
 * that need a "long press for quick actions" affordance — androidx.tv
 * .material3.Surface has no onLongClick, so this drives its own focus +
 * key handling directly (same SystemClock-based long-press detection
 * LiveTvPlayerScreen/VodPlayerScreen already use at the screen root, just
 * scoped to one item) instead of layering onto Surface's opaque internal
 * click handling. Pair with [tvFocusStyle] for the focus glow, and use a
 * plain Box/Row as the root instead of Surface.
 */
fun Modifier.tvPressLongPress(
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    longPressThresholdMs: Long = 500L
): Modifier = composed {
    var downAt by remember { mutableStateOf(0L) }
    var longPressTriggered by remember { mutableStateOf(false) }

    this
        .focusable()
        .onKeyEvent { keyEvent ->
            val code = keyEvent.nativeKeyEvent.keyCode
            if (code != KeyEvent.KEYCODE_DPAD_CENTER && code != KeyEvent.KEYCODE_ENTER) return@onKeyEvent false
            when (keyEvent.type) {
                KeyEventType.KeyDown -> {
                    if (keyEvent.nativeKeyEvent.repeatCount == 0) {
                        downAt = SystemClock.elapsedRealtime()
                        longPressTriggered = false
                    } else if (!longPressTriggered && SystemClock.elapsedRealtime() - downAt >= longPressThresholdMs) {
                        longPressTriggered = true
                        onLongPress()
                    }
                    true
                }
                KeyEventType.KeyUp -> {
                    if (!longPressTriggered) onClick()
                    true
                }
                else -> false
            }
        }
}
