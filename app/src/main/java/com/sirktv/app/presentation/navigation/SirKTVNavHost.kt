package com.sirktv.app.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sirktv.app.presentation.common.SirKTVNavItem
import com.sirktv.app.presentation.favorites.FavoritesScreen
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

/** Maps a Home tile destination to its route, preserving each top-level section's own scroll/state across visits. */
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
 * Pure route-classification logic for [SirKTVBackHandler], pulled out of the
 * composable so it's unit-testable without a Compose/Navigation test rule.
 * True for every screen that has no local reason to intercept Back: Home
 * (root-of-stack exit confirmation) and every player route (two-stage
 * "reveal controls, then leave", which stops playback via its own
 * onCleared()) keep their own local BackHandler instead, so the two never
 * race for the same press. Login is excluded too — navigating to Home from
 * there would skip authentication entirely, so Back on Login falls through
 * to the platform default instead (there's nothing below it on the stack,
 * so that's normally "do nothing"/exit).
 */
internal fun centralBackHandlerEnabled(route: String?): Boolean =
    route != null &&
        route != SirKTVDestinations.HOME &&
        route != SirKTVDestinations.LOGIN &&
        route !in PLAYER_ROUTES

/**
 * The one and only hardware-Back handler for every screen that has no local
 * reason to intercept it (see [centralBackHandlerEnabled]) — Live TV/Movies/
 * Series/Sports browse, Series Detail, Favorites, Settings, Search all have
 * no sidebar left to navigate away with now that the app shell has none, so
 * Back always returns to a clean Home.
 */
@Composable
private fun SirKTVBackHandler(navController: NavHostController) {
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    BackHandler(enabled = centralBackHandlerEnabled(currentRoute)) {
        navController.navigate(SirKTVDestinations.HOME) {
            popUpTo(0) { inclusive = true }
        }
    }
}

/**
 * TV-native app shell: no persistent chrome at all — every screen renders
 * edge-to-edge and Home's tiles are the only top-level navigation surface.
 * Hardware Back is handled once, centrally, in [SirKTVBackHandler] rather
 * than per screen (Home and the player screens are the sole exceptions —
 * see its doc comment).
 */
@Composable
fun SirKTVNavHost(navigationCommandBus: NavigationCommandBus) {
    val navController = rememberNavController()
    val onNavigate: (SirKTVNavItem) -> Unit = { item -> navController.navigateToNavItem(item) }

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

    SirKTVBackHandler(navController = navController)

    Box(Modifier.fillMaxSize().background(SirKTVBackground)) {
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
                    onNavigateToLiveTv = { onNavigate(SirKTVNavItem.LIVE_TV) },
                    onNavigateToMovies = { onNavigate(SirKTVNavItem.MOVIES) },
                    onNavigateToSeries = { onNavigate(SirKTVNavItem.SERIES) },
                    onNavigateToFavorites = { onNavigate(SirKTVNavItem.FAVORITES) },
                    onNavigateToSettings = { onNavigate(SirKTVNavItem.SETTINGS) }
                )
            }

            composable(SirKTVDestinations.LIVE_TV_BROWSE) {
                LiveTvBrowseScreen(
                    onChannelSelected = { channelId -> navController.navigate(SirKTVDestinations.liveTv(channelId)) },
                    onSearch = {
                        navController.navigate(SirKTVDestinations.search(section = SirKTVDestinations.SearchSection.LIVE)) { launchSingleTop = true }
                    }
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
                    onMovieSelected = { movieId -> navController.navigate(SirKTVDestinations.moviePlayer(movieId)) },
                    onSearch = {
                        navController.navigate(SirKTVDestinations.search(section = SirKTVDestinations.SearchSection.MOVIES)) { launchSingleTop = true }
                    }
                )
            }

            composable(SirKTVDestinations.SERIES) {
                SeriesScreen(
                    onSeriesSelected = { seriesId -> navController.navigate(SirKTVDestinations.seriesDetail(seriesId)) },
                    onSearch = {
                        navController.navigate(SirKTVDestinations.search(section = SirKTVDestinations.SearchSection.SERIES)) { launchSingleTop = true }
                    }
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
                arguments = listOf(
                    navArgument("query") { type = NavType.StringType; defaultValue = "" },
                    navArgument("section") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
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
