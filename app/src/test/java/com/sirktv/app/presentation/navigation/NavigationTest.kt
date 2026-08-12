package com.sirktv.app.presentation.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers [centralBackHandlerEnabled] — the pure route-classification logic
 * behind the app's single centralized Back handler, and therefore safe to
 * unit-test without a Compose/Navigation test rule or an emulator. Home and
 * the three player routes manage Back locally (root exit-confirmation and
 * the two-stage "reveal controls, then leave" respectively); every other
 * screen has no sidebar left to navigate away with, so the central handler
 * takes it back to Home.
 */
class NavigationTest {

    @Test
    fun `home manages its own Back — central handler stays disabled there`() {
        assertFalse(centralBackHandlerEnabled(SirKTVDestinations.HOME))
    }

    @Test
    fun `player routes manage their own two-stage Back — central handler stays disabled there`() {
        assertFalse(centralBackHandlerEnabled(SirKTVDestinations.LIVE_TV))
        assertFalse(centralBackHandlerEnabled(SirKTVDestinations.MOVIE_PLAYER))
        assertFalse(centralBackHandlerEnabled(SirKTVDestinations.EPISODE_PLAYER))
    }

    @Test
    fun `unknown or null route stays disabled`() {
        assertFalse(centralBackHandlerEnabled(null))
    }

    @Test
    fun `every full-panel content browser and utility screen is handled centrally`() {
        assertTrue(centralBackHandlerEnabled(SirKTVDestinations.LIVE_TV_BROWSE))
        assertTrue(centralBackHandlerEnabled(SirKTVDestinations.MOVIES))
        assertTrue(centralBackHandlerEnabled(SirKTVDestinations.SERIES))
        assertTrue(centralBackHandlerEnabled(SirKTVDestinations.SPORTS))
        assertTrue(centralBackHandlerEnabled(SirKTVDestinations.SERIES_DETAIL))
        assertTrue(centralBackHandlerEnabled(SirKTVDestinations.MOVIE_DETAIL))
        assertTrue(centralBackHandlerEnabled(SirKTVDestinations.FAVORITES))
        assertTrue(centralBackHandlerEnabled(SirKTVDestinations.SETTINGS))
        assertTrue(centralBackHandlerEnabled(SirKTVDestinations.search("batman")))
    }

    @Test
    fun `login is excluded — Back must never force-navigate to Home and skip authentication`() {
        assertFalse(centralBackHandlerEnabled(SirKTVDestinations.LOGIN))
    }
}
