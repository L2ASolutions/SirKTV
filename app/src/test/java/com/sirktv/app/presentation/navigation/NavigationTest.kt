package com.sirktv.app.presentation.navigation

import com.sirktv.app.presentation.common.SirKTVNavItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the two pieces of the sidebar-shell routing that are pure functions
 * — [shouldShowSidebar] and [routeToNavItem] — and are therefore safe to
 * unit-test without a Compose/Navigation test rule or an emulator. The
 * behavioral cases requested alongside these (back-from-player, back-from-
 * home showing the exit dialog) are BackHandler/NavController integration
 * behavior that lives entirely inside composables; verifying those needs an
 * instrumented or Robolectric-backed Compose UI test, which this project
 * doesn't have set up yet — see the final report for what's not covered here.
 */
class NavigationTest {

    @Test
    fun `player screens show no sidebar`() {
        assertFalse(shouldShowSidebar(SirKTVDestinations.LIVE_TV))
        assertFalse(shouldShowSidebar(SirKTVDestinations.MOVIE_PLAYER))
        assertFalse(shouldShowSidebar(SirKTVDestinations.EPISODE_PLAYER))
    }

    @Test
    fun `login screen shows no sidebar`() {
        assertFalse(shouldShowSidebar(SirKTVDestinations.LOGIN))
    }

    @Test
    fun `unknown or null route shows no sidebar`() {
        assertFalse(shouldShowSidebar(null))
    }

    @Test
    fun `only Home, Favorites, Search, and Settings show the sidebar`() {
        assertTrue(shouldShowSidebar(SirKTVDestinations.HOME))
        assertTrue(shouldShowSidebar(SirKTVDestinations.FAVORITES))
        assertTrue(shouldShowSidebar(SirKTVDestinations.SETTINGS))
        assertTrue(shouldShowSidebar(SirKTVDestinations.search("batman")))
    }

    @Test
    fun `full-panel content browsers hide the sidebar for full screen width`() {
        assertFalse(shouldShowSidebar(SirKTVDestinations.LIVE_TV_BROWSE))
        assertFalse(shouldShowSidebar(SirKTVDestinations.MOVIES))
        assertFalse(shouldShowSidebar(SirKTVDestinations.SERIES))
        assertFalse(shouldShowSidebar(SirKTVDestinations.SPORTS))
        assertFalse(shouldShowSidebar(SirKTVDestinations.SERIES_DETAIL))
    }

    @Test
    fun `sidebar navigation routes to correct screen`() {
        assertEquals(SirKTVNavItem.HOME, routeToNavItem(SirKTVDestinations.HOME))
        assertEquals(SirKTVNavItem.LIVE_TV, routeToNavItem(SirKTVDestinations.LIVE_TV_BROWSE))
        assertEquals(SirKTVNavItem.MOVIES, routeToNavItem(SirKTVDestinations.MOVIES))
        assertEquals(SirKTVNavItem.SERIES, routeToNavItem(SirKTVDestinations.SERIES))
        assertEquals(SirKTVNavItem.SPORTS, routeToNavItem(SirKTVDestinations.SPORTS))
        assertEquals(SirKTVNavItem.FAVORITES, routeToNavItem(SirKTVDestinations.FAVORITES))
        assertEquals(SirKTVNavItem.SETTINGS, routeToNavItem(SirKTVDestinations.SETTINGS))
    }

    @Test
    fun `series detail keeps series highlighted in sidebar`() {
        assertEquals(SirKTVNavItem.SERIES, routeToNavItem(SirKTVDestinations.SERIES_DETAIL))
    }

    @Test
    fun `search with a query still highlights search`() {
        assertEquals(SirKTVNavItem.SEARCH, routeToNavItem(SirKTVDestinations.search("batman")))
    }
}
