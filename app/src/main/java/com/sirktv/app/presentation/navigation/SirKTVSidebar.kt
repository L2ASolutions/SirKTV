package com.sirktv.app.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.tv.material3.Surface
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.presentation.common.SirKTVLogoMark
import com.sirktv.app.presentation.common.SirKTVNavItem
import com.sirktv.app.presentation.common.tvNoBorder
import com.sirktv.app.presentation.common.tvNoGlow
import com.sirktv.app.presentation.theme.AppSidebar
import com.sirktv.app.presentation.theme.SirKTVDivider
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVPrimaryContainer
import com.sirktv.app.presentation.theme.SirKTVSurfaceElevated
import com.sirktv.app.presentation.theme.SirKTVTextPrimary
import com.sirktv.app.presentation.theme.SirKTVTextSecondary
import com.sirktv.app.presentation.theme.SirKTVTextTertiary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

internal val SidebarWidth = 220.dp
private val SidebarItemHeight = 56.dp
private val SidebarAccentBarWidth = 3.dp

private val SidebarBackground = AppSidebar
private val SidebarDivider = SirKTVDivider
private val SidebarFooterText = SirKTVTextTertiary
private val SidebarAccentColor = SirKTVPrimary

// Focus/selection is ONLY ever a full-row background color change plus (for
// the selected item) the left accent bar — no pill, no glow, no border. A
// pill/glow box behind the icon previously rendered as a stray bright
// highlight on real hardware; the row background is the one and only focus
// indicator now.
private val SidebarRowSelectedBg = SirKTVPrimaryContainer
private val SidebarRowFocusedBg = SirKTVSurfaceElevated
private val SidebarIconActive = SirKTVTextPrimary
private val SidebarIconIdle = SirKTVTextSecondary
private val SidebarTextActive = SirKTVTextPrimary
private val SidebarTextIdle = SirKTVTextSecondary

private val SidebarEntries = SirKTVNavItem.entries.toList()

/** Backs [SirKTVSidebar] only — sources the footer's display name so the sidebar doesn't need its own plumbing for it. */
@HiltViewModel
class SirKTVSidebarViewModel @Inject constructor(
    currentSession: CurrentSession
) : ViewModel() {
    val greetingName: StateFlow<String?> = currentSession.greetingName
}

/**
 * The one always-visible way to move between top-level sections on a
 * TV-native layout: a fixed-width left rail with modern, custom-drawn,
 * animated iconography — replacing the old horizontal nav-pill/top-bar
 * chrome and flat Material default icons alike. Rendered once by
 * [SirKTVNavHost] alongside the NavHost content, never per-screen — see
 * [SirKTVNavHost] for the route-based show/hide logic (hidden on Login and
 * every player route).
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun SirKTVSidebar(
    currentRoute: String?,
    onNavigate: (SirKTVNavItem) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    viewModel: SirKTVSidebarViewModel = hiltViewModel()
) {
    val greetingName by viewModel.greetingName.collectAsState()
    val activeItem = routeToNavItem(currentRoute)

    Column(
        modifier = modifier
            .width(SidebarWidth)
            .fillMaxHeight()
            .shadow(elevation = 8.dp, spotColor = Color.Black, ambientColor = Color.Black)
            .background(SidebarBackground)
            .padding(top = 32.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SirKTVLogoMark(modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "SirKTV",
                color = SirKTVTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
        }

        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SidebarDivider))
        Spacer(Modifier.height(8.dp))

        // The sidebar is never auto-focused on screen entry — see
        // tvSidebarEscapeLeft — this focusRestorer() only matters once
        // something explicitly calls focusRequester.requestFocus(), sending
        // focus to whichever item was last focused (or the first, on the
        // very first visit).
        Column(
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusRestorer()
        ) {
            SidebarEntries.forEach { item ->
                SidebarNavItem(
                    item = item,
                    isSelected = activeItem == item,
                    onClick = { onNavigate(item) }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SirKTVIcons.Person(tint = SidebarFooterText, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = greetingName.orEmpty(),
                color = SidebarFooterText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SidebarNavItem(
    item: SirKTVNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val active = isSelected || isFocused

    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> SidebarRowSelectedBg
            isFocused -> SidebarRowFocusedBg
            else -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "sidebarRowBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (active) SidebarIconActive else SidebarIconIdle,
        animationSpec = tween(200),
        label = "sidebarIconTint"
    )
    val textColor by animateColorAsState(
        targetValue = if (active) SidebarTextActive else SidebarTextIdle,
        animationSpec = tween(200),
        label = "sidebarTextColor"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        border = tvNoBorder(),
        glow = tvNoGlow(),
        modifier = Modifier
            .fillMaxWidth()
            .height(SidebarItemHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(bgColor)
                // Royal Blue accent bar on the item's own left edge — the
                // active section only, never on focus alone (focus already
                // reads as the brighter row background + text/icon color).
                .drawBehind {
                    if (isSelected) {
                        drawRect(color = SidebarAccentColor, size = Size(SidebarAccentBarWidth.toPx(), size.height))
                    }
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SidebarIcon(item = item, tint = iconTint, isSelected = isSelected, isFocused = isFocused)
            Spacer(Modifier.width(14.dp))
            Text(
                text = item.label,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Dispatches to the right custom-drawn icon per section, layering on the two
 * bespoke animations: Settings' gear spins continuously while focused (a
 * fresh 8s rotation cycle, easing back to rest when focus leaves), and
 * Favorites' heart swaps outline-to-filled on focus/selection with a bouncy
 * pulse instead of an instant swap.
 */
@Composable
private fun SidebarIcon(item: SirKTVNavItem, tint: Color, isSelected: Boolean, isFocused: Boolean) {
    val iconModifier = Modifier.size(22.dp)
    when (item) {
        SirKTVNavItem.HOME -> SirKTVIcons.Home(tint, iconModifier)
        SirKTVNavItem.LIVE_TV -> SirKTVIcons.LiveTv(tint, iconModifier)
        SirKTVNavItem.MOVIES -> SirKTVIcons.Movies(tint, iconModifier)
        SirKTVNavItem.SERIES -> SirKTVIcons.Series(tint, iconModifier)
        SirKTVNavItem.SPORTS -> SirKTVIcons.Sports(tint, iconModifier)
        SirKTVNavItem.SEARCH -> SirKTVIcons.Search(tint, iconModifier)
        SirKTVNavItem.FAVORITES -> {
            val favScale by animateFloatAsState(
                targetValue = if (isSelected) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
                label = "sidebarFavoritesPulse"
            )
            SirKTVIcons.Favorites(tint, filled = isSelected || isFocused, modifier = iconModifier.scale(favScale))
        }
        SirKTVNavItem.SETTINGS -> {
            val infiniteTransition = rememberInfiniteTransition(label = "sidebarGear")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = if (isFocused) 360f else 0f,
                animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing)),
                label = "sidebarGearRotation"
            )
            SirKTVIcons.Settings(tint, iconModifier.rotate(rotation))
        }
    }
}

/**
 * Maps the NavHost's current route (a route *template*, e.g.
 * "series_detail/{seriesId}", not a resolved path) back to the sidebar item
 * that should read as active. Drill-down destinations (series detail) keep
 * their parent section highlighted rather than showing no selection at all.
 */
internal fun routeToNavItem(route: String?): SirKTVNavItem? = when {
    route == null -> null
    route == SirKTVDestinations.HOME -> SirKTVNavItem.HOME
    route == SirKTVDestinations.LIVE_TV_BROWSE || route == SirKTVDestinations.LIVE_TV -> SirKTVNavItem.LIVE_TV
    route == SirKTVDestinations.MOVIES || route == SirKTVDestinations.MOVIE_PLAYER -> SirKTVNavItem.MOVIES
    route == SirKTVDestinations.SERIES ||
        route == SirKTVDestinations.SERIES_DETAIL ||
        route == SirKTVDestinations.EPISODE_PLAYER -> SirKTVNavItem.SERIES
    route == SirKTVDestinations.SPORTS -> SirKTVNavItem.SPORTS
    route.startsWith("search") -> SirKTVNavItem.SEARCH
    route == SirKTVDestinations.FAVORITES -> SirKTVNavItem.FAVORITES
    route == SirKTVDestinations.SETTINGS -> SirKTVNavItem.SETTINGS
    else -> null
}
