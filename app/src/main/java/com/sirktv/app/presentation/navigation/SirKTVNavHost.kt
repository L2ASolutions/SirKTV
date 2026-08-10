package com.sirktv.app.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sirktv.app.presentation.common.LocalSidebarFocusRequester
import com.sirktv.app.presentation.common.SirKTVNavItem
import com.sirktv.app.presentation.favorites.FavoritesScreen
import com.sirktv.app.presentation.home.HomeNavTarget
import com.sirktv.app.presentation.home.HomeScreen
import com.sirktv.app.presentation.livetv.LiveTvBrowseScreen
import com.sirktv.app.presentation.livetv.LiveTvPlayerScreen
import com.sirktv.app.presentation.login.LoginScreen
import com.sirktv.app.presentation.movies.MoviesScreen
import com.sirktv.app.presentation.search.SearchScreen
import com.sirktv.app.presentation.series.SeriesDetailScreen
import com.sirktv.app.presentation.series.SeriesScreen
import com.sirktv.app.presentation.settings.SettingsScreen
import com.sirktv.app.presentation.sports.SportsScreen
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.vodplayer.VodPlayerScreen

/** Maps a sidebar destination to its route and navigates there, preserving each top-level section's own scroll/state across switches. */
private fun NavHostController.navigateToNavItem(item: SirKTVNavItem) {
    val route = when (item) {
        SirKTVNavItem.HOME -> SirKTVDestinations.HOME
        SirKTVNavItem.FAVORITES -> SirKTVDestinations.FAVORITES
        SirKTVNavItem.LIVE_TV -> SirKTVDestinations.LIVE_TV_BROWSE
        SirKTVNavItem.MOVIES -> SirKTVDestinations.MOVIES
        SirKTVNavItem.SERIES -> SirKTVDestinations.SERIES
        SirKTVNavItem.SPORTS -> SirKTVDestinations.SPORTS
        SirKTVNavItem.SEARCH -> SirKTVDestinations.search()
        SirKTVNavItem.SETTINGS -> SirKTVDestinations.SETTINGS
    }
    navigate(route) {
        popUpTo(SirKTVDestinations.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

internal val PLAYER_ROUTES = setOf(SirKTVDestinations.LIVE_TV, SirKTVDestinations.MOVIE_PLAYER, SirKTVDestinations.EPISODE_PLAYER)

/** Pure route-classification logic, pulled out of the composable so it's unit-testable without a Compose/Navigation test rule. */
internal fun shouldShowSidebar(route: String?): Boolean =
    route != null && route != SirKTVDestinations.LOGIN && route !in PLAYER_ROUTES

/**
 * Leaving a player screen for Search/Settings via a hardware button (not the
 * player's own Back handling) must still tear the player down properly —
 * popping its back-stack entry here is what makes that happen, since it's
 * the ViewModel's onCleared() that actually stops ExoPlayer.
 */
private fun NavHostController.popIfOnPlayerRoute() {
    if (currentBackStackEntry?.destination?.route in PLAYER_ROUTES) popBackStack()
}

/**
 * TV-native app shell: a fixed left sidebar (see [SirKTVSidebar]) is always
 * on screen except on Login and every fullscreen player route — no per-screen
 * top bar or horizontal nav pills anymore. The sidebar and the NavHost are
 * siblings in one Row so D-pad Left from a screen's own leftmost content can
 * hand off to it (see [LocalSidebarFocusRequester]/tvSidebarEscapeLeft)
 * without the sidebar ever stealing focus on its own.
 */
@Composable
fun SirKTVNavHost(navigationCommandBus: NavigationCommandBus) {
    val navController = rememberNavController()
    val onNavigate: (SirKTVNavItem) -> Unit = { item -> navController.navigateToNavItem(item) }
    val sidebarFocusRequester = remember { FocusRequester() }

    // MainActivity.onKeyDown/onNewIntent have no NavHostController of their
    // own — this is where their posted commands actually become navigation.
    LaunchedEffect(navigationCommandBus) {
        navigationCommandBus.commands.collect { command ->
            when (command) {
                is NavigationCommand.OpenSearch -> {
                    navController.popIfOnPlayerRoute()
                    navController.navigate(SirKTVDestinations.search(command.query)) { launchSingleTop = true }
                }
                NavigationCommand.MenuPressed -> {
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    if (currentRoute == SirKTVDestinations.SETTINGS) {
                        navController.popBackStack()
                    } else {
                        navController.popIfOnPlayerRoute()
                        navController.navigate(SirKTVDestinations.SETTINGS) { launchSingleTop = true }
                    }
                }
            }
        }
    }

    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val showSidebar = shouldShowSidebar(currentRoute)

    Row(Modifier.fillMaxSize().background(SirKTVBackground)) {
        if (showSidebar) {
            SirKTVSidebar(
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                focusRequester = sidebarFocusRequester
            )
        }

        Box(Modifier.weight(1f).fillMaxSize()) {
            CompositionLocalProvider(LocalSidebarFocusRequester provides sidebarFocusRequester.takeIf { showSidebar }) {
            NavHost(
                navController = navController,
                startDestination = SirKTVDestinations.LOGIN,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(SirKTVDestinations.LOGIN) {
                    LoginScreen(
                        onNavigateToLiveTv = { channelId ->
                            navController.navigate(SirKTVDestinations.liveTv(channelId)) {
                                popUpTo(SirKTVDestinations.LOGIN) { inclusive = true }
                            }
                        },
                        onNavigateToHome = {
                            navController.navigate(SirKTVDestinations.HOME) {
                                popUpTo(SirKTVDestinations.LOGIN) { inclusive = true }
                            }
                        }
                    )
                }

                composable(SirKTVDestinations.HOME) {
                    HomeScreen(
                        onOpenContent = { target ->
                            when (target) {
                                is HomeNavTarget.LiveTv -> navController.navigate(SirKTVDestinations.liveTv(target.channelId))
                                is HomeNavTarget.MoviePlayer -> navController.navigate(SirKTVDestinations.moviePlayer(target.movieId))
                                is HomeNavTarget.EpisodePlayer ->
                                    navController.navigate(SirKTVDestinations.episodePlayer(target.seriesId, target.season, target.episode))
                                is HomeNavTarget.SeriesDetail ->
                                    navController.navigate(SirKTVDestinations.seriesDetail(target.seriesId))
                            }
                        },
                        // Same nav semantics as a sidebar click (popUpTo HOME
                        // saveState, singleTop, restoreState) — the Home
                        // tiles are shortcuts to the sidebar's own destinations.
                        onNavigateToSection = onNavigate,
                        onLoggedOut = {
                            navController.navigate(SirKTVDestinations.LOGIN) {
                                popUpTo(SirKTVDestinations.HOME) { inclusive = true }
                            }
                        }
                    )
                }

                composable(SirKTVDestinations.LIVE_TV_BROWSE) {
                    LiveTvBrowseScreen(
                        onChannelSelected = { channelId -> navController.navigate(SirKTVDestinations.liveTv(channelId)) }
                    )
                }

                composable(
                    route = SirKTVDestinations.LIVE_TV,
                    arguments = listOf(navArgument("channelId") { type = NavType.StringType })
                ) {
                    LiveTvPlayerScreen(onBack = { navController.popBackStack() })
                }

                composable(SirKTVDestinations.SPORTS) {
                    SportsScreen(
                        onChannelSelected = { channelId -> navController.navigate(SirKTVDestinations.liveTv(channelId)) }
                    )
                }

                composable(SirKTVDestinations.MOVIES) {
                    MoviesScreen(
                        onMovieSelected = { movieId -> navController.navigate(SirKTVDestinations.moviePlayer(movieId)) }
                    )
                }

                composable(SirKTVDestinations.SERIES) {
                    SeriesScreen(
                        onSeriesSelected = { seriesId -> navController.navigate(SirKTVDestinations.seriesDetail(seriesId)) }
                    )
                }

                composable(
                    route = SirKTVDestinations.SERIES_DETAIL,
                    arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
                ) {
                    SeriesDetailScreen(
                        onEpisodeSelected = { seriesId, season, episode ->
                            navController.navigate(SirKTVDestinations.episodePlayer(seriesId, season, episode))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(SirKTVDestinations.FAVORITES) {
                    FavoritesScreen(
                        onLiveSelected = { channelId -> navController.navigate(SirKTVDestinations.liveTv(channelId)) },
                        onMovieSelected = { movieId -> navController.navigate(SirKTVDestinations.moviePlayer(movieId)) },
                        onSeriesSelected = { seriesId -> navController.navigate(SirKTVDestinations.seriesDetail(seriesId)) }
                    )
                }

                composable(
                    route = SirKTVDestinations.SEARCH,
                    arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" })
                ) {
                    SearchScreen(
                        onChannelSelected = { channelId -> navController.navigate(SirKTVDestinations.liveTv(channelId)) },
                        onMovieSelected = { movieId -> navController.navigate(SirKTVDestinations.moviePlayer(movieId)) },
                        onSeriesSelected = { seriesId -> navController.navigate(SirKTVDestinations.seriesDetail(seriesId)) }
                    )
                }

                composable(
                    route = SirKTVDestinations.MOVIE_PLAYER,
                    arguments = listOf(navArgument("movieId") { type = NavType.StringType })
                ) {
                    VodPlayerScreen(onBack = { navController.popBackStack() })
                }

                composable(
                    route = SirKTVDestinations.EPISODE_PLAYER,
                    arguments = listOf(
                        navArgument("seriesId") { type = NavType.StringType },
                        navArgument("season") { type = NavType.IntType },
                        navArgument("episode") { type = NavType.IntType }
                    )
                ) {
                    VodPlayerScreen(onBack = { navController.popBackStack() })
                }

                composable(SirKTVDestinations.SETTINGS) {
                    SettingsScreen(
                        onLoggedOut = {
                            navController.navigate(SirKTVDestinations.LOGIN) {
                                popUpTo(SirKTVDestinations.HOME) { inclusive = true }
                            }
                        }
                    )
                }
            }
            }
        }
    }
}
