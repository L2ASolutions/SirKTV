package com.sirktv.app.presentation.common

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVFocusBorder

private val FocusAnimSpecFloat = tween<Float>(180)
private val FocusAnimSpecDp = tween<Dp>(180)

/** How a focused element signals focus, layered on top of the shared scale/dim treatment. */
enum class TvFocusAccent {
    /** Soft Royal Blue glow (shadow), no hard edge — the default for cards, icons, and pills. */
    GLOW,

    /** Thin 2dp Royal Blue border with soft corners — for Button, which already has a solid fill. */
    BORDER,

    /** No glow/border at all — used where a component draws its own focus cue (e.g. a left accent bar). */
    NONE
}

/**
 * Shared D-pad focus feedback: the focused element scales up gently and
 * (depending on [accent]) gets either a soft Royal Blue glow or a thin
 * border; every unfocused sibling dims to [Dimens.UnfocusedAlpha] so the
 * focused element is unambiguous at a glance. Deliberately avoids a hard
 * rectangular border by default — [TvFocusAccent.GLOW] uses an elevation
 * shadow tinted Royal Blue instead, which reads as a premium glow rather
 * than a harsh outline.
 */
fun Modifier.tvFocusStyle(
    cornerRadius: Dp = Dimens.CornerRadius,
    accent: TvFocusAccent = TvFocusAccent.GLOW,
    scaleOnFocus: Boolean = true,
    onFocusChanged: (Boolean) -> Unit = {}
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused && scaleOnFocus) Dimens.FocusScale else 1f,
        animationSpec = FocusAnimSpecFloat,
        label = "tvFocusScale"
    )
    val dimAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else Dimens.UnfocusedAlpha,
        animationSpec = FocusAnimSpecFloat,
        label = "tvFocusDim"
    )
    val glowElevation by animateDpAsState(
        targetValue = if (isFocused && accent == TvFocusAccent.GLOW) Dimens.FocusGlowElevation else 0.dp,
        animationSpec = FocusAnimSpecDp,
        label = "tvFocusGlow"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused && accent == TvFocusAccent.BORDER) SirKTVFocusBorder else Color.Transparent,
        animationSpec = tween(180),
        label = "tvFocusBorderColor"
    )

    this
        .onFocusChanged { state ->
            isFocused = state.isFocused
            onFocusChanged(state.isFocused)
        }
        .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = dimAlpha }
        .shadow(
            elevation = glowElevation,
            shape = RoundedCornerShape(cornerRadius),
            clip = false,
            ambientColor = SirKTVFocusBorder,
            spotColor = SirKTVFocusBorder
        )
        .then(
            if (accent == TvFocusAccent.BORDER) {
                Modifier.border(Dimens.ButtonFocusBorderWidth, borderColor, RoundedCornerShape(cornerRadius))
            } else {
                Modifier
            }
        )
}

/**
 * Focus treatment for dense list rows (channel rows): instead of a border or
 * glow, focus is a smoothly animated Royal Blue left accent bar plus a very
 * subtle background wash — a border or glow reads as clutter once you have
 * dozens of full-width rows stacked in a list. [onFocusChanged] lets the
 * caller drive its own accent-bar content (e.g. combining this with an
 * "is currently playing" indicator) instead of layering a second bar on top.
 */
fun Modifier.tvChannelRowFocusStyle(
    cornerRadius: Dp = Dimens.CornerRadius,
    onFocusChanged: (Boolean) -> Unit = {}
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val dimAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else Dimens.UnfocusedAlpha,
        animationSpec = FocusAnimSpecFloat,
        label = "tvRowDim"
    )
    val wash by animateColorAsState(
        targetValue = if (isFocused) SirKTVFocusBorder.copy(alpha = 0.10f) else Color.Transparent,
        animationSpec = tween(180),
        label = "tvRowWash"
    )

    this
        .onFocusChanged { state ->
            isFocused = state.isFocused
            onFocusChanged(state.isFocused)
        }
        .graphicsLayer { this.alpha = dimAlpha }
        .background(wash, RoundedCornerShape(cornerRadius))
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
