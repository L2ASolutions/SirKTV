package com.sirktv.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.sirktv.app.presentation.vodplayer.VodPlayerScreen

/** Maps a nav-pill destination to its route and navigates, deduping repeat taps on the already-active pill. */
private fun NavHostController.navigateToNavItem(item: SirKTVNavItem) {
    val route = when (item) {
        SirKTVNavItem.HOME -> SirKTVDestinations.HOME
        SirKTVNavItem.FAVORITES -> SirKTVDestinations.FAVORITES
        SirKTVNavItem.LIVE_TV -> SirKTVDestinations.LIVE_TV_BROWSE
        SirKTVNavItem.MOVIES -> SirKTVDestinations.MOVIES
        SirKTVNavItem.SERIES -> SirKTVDestinations.SERIES
        SirKTVNavItem.SPORTS -> SirKTVDestinations.SPORTS
        SirKTVNavItem.SEARCH -> SirKTVDestinations.SEARCH
        SirKTVNavItem.SETTINGS -> SirKTVDestinations.SETTINGS
    }
    navigate(route) { launchSingleTop = true }
}

@Composable
fun SirKTVNavHost() {
    val navController = rememberNavController()
    val onNavigate: (SirKTVNavItem) -> Unit = { item -> navController.navigateToNavItem(item) }

    NavHost(navController = navController, startDestination = SirKTVDestinations.LOGIN) {
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
                onNavigate = onNavigate,
                onLoggedOut = {
                    navController.navigate(SirKTVDestinations.LOGIN) {
                        popUpTo(SirKTVDestinations.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(SirKTVDestinations.LIVE_TV_BROWSE) {
            LiveTvBrowseScreen(
                onChannelSelected = { channelId -> navController.navigate(SirKTVDestinations.liveTv(channelId)) },
                onNavigate = onNavigate
            )
        }

        composable(
            route = SirKTVDestinations.LIVE_TV,
            arguments = listOf(navArgument("channelId") { type = NavType.StringType })
        ) {
            LiveTvPlayerScreen()
        }

        composable(SirKTVDestinations.SPORTS) {
            SportsScreen(
                onChannelSelected = { channelId -> navController.navigate(SirKTVDestinations.liveTv(channelId)) },
                onNavigate = onNavigate
            )
        }

        composable(SirKTVDestinations.MOVIES) {
            MoviesScreen(
                onMovieSelected = { movieId -> navController.navigate(SirKTVDestinations.moviePlayer(movieId)) },
                onNavigate = onNavigate
            )
        }

        composable(SirKTVDestinations.SERIES) {
            SeriesScreen(
                onSeriesSelected = { seriesId -> navController.navigate(SirKTVDestinations.seriesDetail(seriesId)) },
                onNavigate = onNavigate
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
                onBack = { navController.popBackStack() },
                onNavigate = onNavigate
            )
        }

        composable(SirKTVDestinations.FAVORITES) {
            FavoritesScreen(
                onLiveSelected = { channelId -> navController.navigate(SirKTVDestinations.liveTv(channelId)) },
                onMovieSelected = { movieId -> navController.navigate(SirKTVDestinations.moviePlayer(movieId)) },
                onSeriesSelected = { seriesId -> navController.navigate(SirKTVDestinations.seriesDetail(seriesId)) },
                onNavigate = onNavigate
            )
        }

        composable(SirKTVDestinations.SEARCH) {
            SearchScreen(
                onChannelSelected = { channelId -> navController.navigate(SirKTVDestinations.liveTv(channelId)) },
                onMovieSelected = { movieId -> navController.navigate(SirKTVDestinations.moviePlayer(movieId)) },
                onSeriesSelected = { seriesId -> navController.navigate(SirKTVDestinations.seriesDetail(seriesId)) },
                onNavigate = onNavigate
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
                onNavigate = onNavigate,
                onLoggedOut = {
                    navController.navigate(SirKTVDestinations.LOGIN) {
                        popUpTo(SirKTVDestinations.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}
