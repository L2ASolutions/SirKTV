package com.sirktv.app.presentation.common

/** The eight destinations reachable from the persistent nav pill row on every non-player, non-login screen. */
enum class SirKTVNavItem(val label: String) {
    HOME("Home"),
    FAVORITES("Favorites"),
    LIVE_TV("Live TV"),
    MOVIES("Movies"),
    SERIES("Series"),
    SPORTS("Sports"),
    SEARCH("Search"),
    SETTINGS("Settings")
}
