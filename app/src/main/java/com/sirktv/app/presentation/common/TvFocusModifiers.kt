package com.sirktv.app.presentation.common

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceGlow
import androidx.tv.material3.Glow
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVFocusBorder

private val FocusAnimSpecFloat = tween<Float>(180)
private val FocusAnimSpecDp = tween<Dp>(180)

/**
 * How strongly a focused element scales up, layered on top of the shared
 * Royal Blue glow. Focus is NEVER a border/outline anywhere in this app —
 * only scale + glow, Netflix/Apple-TV style.
 */
enum class TvFocusAccent {
    /** Card-scale glow (1.08x) — the default for cards, tiles, icons, and pills. */
    GLOW,

    /** Button-scale glow (1.03x) — for Button, which already has a solid fill and shouldn't scale as dramatically. */
    BORDER,

    /** No glow/scale at all — used where a component draws its own focus cue (e.g. a left accent bar). */
    NONE
}

/**
 * Shared D-pad focus feedback: the focused element scales up gently and gets
 * a soft Royal Blue glow (an elevation shadow tinted Royal Blue, not a hard
 * rectangular border); every unfocused sibling dims to [Dimens.UnfocusedAlpha]
 * so the focused element is unambiguous at a glance.
 */
fun Modifier.tvFocusStyle(
    cornerRadius: Dp = Dimens.CornerRadius,
    accent: TvFocusAccent = TvFocusAccent.GLOW,
    scaleOnFocus: Boolean = true,
    onFocusChanged: (Boolean) -> Unit = {}
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val targetScale = if (accent == TvFocusAccent.BORDER) Dimens.ButtonFocusScale else Dimens.FocusScale
    val scale by animateFloatAsState(
        targetValue = if (isFocused && scaleOnFocus) targetScale else 1f,
        animationSpec = FocusAnimSpecFloat,
        label = "tvFocusScale"
    )
    val dimAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else Dimens.UnfocusedAlpha,
        animationSpec = FocusAnimSpecFloat,
        label = "tvFocusDim"
    )
    val glowElevation by animateDpAsState(
        targetValue = if (isFocused && accent != TvFocusAccent.NONE) Dimens.FocusGlowElevation else 0.dp,
        animationSpec = FocusAnimSpecDp,
        label = "tvFocusGlow"
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

/**
 * androidx.tv.material3.Surface draws its own default focus/press border on
 * top of whatever [tvFocusStyle]/[tvChannelRowFocusStyle] already renders —
 * on most TV boxes that shows up as a hard blue/white rectangle around the
 * item, layered over our scale+glow, especially noticeable while scrolling a
 * row. Passing this to every `Surface(border = tvNoBorder())` call switches
 * every one of its border states off, leaving scale + Royal Blue glow as the
 * only focus indicator anywhere in the app.
 */
@Composable
fun tvNoBorder(): ClickableSurfaceBorder = ClickableSurfaceDefaults.border(
    border = Border.None,
    focusedBorder = Border.None,
    pressedBorder = Border.None,
    disabledBorder = Border.None,
    focusedDisabledBorder = Border.None
)

/**
 * androidx.tv.material3.Surface also draws its own default elevation-tinted
 * glow on focus/press, independent of border — on some hardware this reads
 * as a stray bright halo behind whatever custom focus treatment sits on top
 * of it (e.g. the sidebar's full-row background color change). Pass this
 * wherever that default glow needs to be fully suppressed.
 */
@Composable
fun tvNoGlow(): ClickableSurfaceGlow = ClickableSurfaceDefaults.glow(
    glow = Glow.None,
    focusedGlow = Glow.None,
    pressedGlow = Glow.None
)

/** Same as [tvNoBorder] but for androidx.tv.material3.Button, which has its own separate border token set. */
@Composable
fun tvNoButtonBorder(): ButtonBorder = ButtonDefaults.border(
    border = Border.None,
    focusedBorder = Border.None,
    pressedBorder = Border.None,
    disabledBorder = Border.None,
    focusedDisabledBorder = Border.None
)

/**
 * The always-visible app-shell sidebar's own [FocusRequester], provided once
 * at the app-shell root (see SirKTVSidebar.kt) so any screen deep in the
 * NavHost can hand focus back to it without threading a parameter through
 * every screen's signature. Null on routes that render no sidebar at all
 * (login, player screens) — [tvSidebarEscapeLeft] is a no-op there.
 */
val LocalSidebarFocusRequester = compositionLocalOf<FocusRequester?> { null }

/**
 * Explicit "Left exits to the sidebar" handoff for a screen's single-column
 * leftmost container (a vertical list/stack where DirectionLeft has no other
 * in-container meaning — e.g. a category sidebar or a stack of section
 * headers). Deliberately NOT meant for multi-item horizontal rows or
 * multi-column grids: attaching this to a whole Row/grid container would
 * intercept every Left press in the capture phase before the currently
 * focused child gets a chance to move focus to its own left neighbor,
 * breaking in-row navigation. Those containers instead rely on Compose's
 * default 2D focus search to reach the sidebar once nothing further left
 * remains — which already works in this codebase since no row/grid item here
 * consumes DirectionLeft itself.
 */
fun Modifier.tvSidebarEscapeLeft(): Modifier = composed {
    val sidebarFocusRequester = LocalSidebarFocusRequester.current
    if (sidebarFocusRequester == null) {
        this
    } else {
        this.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                sidebarFocusRequester.requestFocus()
                true
            } else false
        }
    }
}
