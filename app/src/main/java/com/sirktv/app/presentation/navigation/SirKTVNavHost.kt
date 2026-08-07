package com.sirktv.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sirktv.app.presentation.favorites.FavoritesScreen
import com.sirktv.app.presentation.home.HomeNavTarget
import com.sirktv.app.presentation.home.HomeScreen
import com.sirktv.app.presentation.livetv.LiveTvPlayerScreen
import com.sirktv.app.presentation.login.LoginScreen
import com.sirktv.app.presentation.movies.MoviesScreen
import com.sirktv.app.presentation.search.SearchScreen
import com.sirktv.app.presentation.series.SeriesDetailScreen
import com.sirktv.app.presentation.series.SeriesScreen
import com.sirktv.app.presentation.settings.ComingSoonScreen
import com.sirktv.app.presentation.settings.PlayerPerformanceScreen
import com.sirktv.app.presentation.settings.SettingsScreen
import com.sirktv.app.presentation.settings.SettingsTile
import com.sirktv.app.presentation.settings.StartupPreferencesScreen
import com.sirktv.app.presentation.settings.SubtitleAppearanceScreen
import com.sirktv.app.presentation.sports.SportsScreen
import com.sirktv.app.presentation.vodplayer.VodPlayerScreen

@Composable
fun SirKTVNavHost() {
    val navController = rememberNavController()

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
                onNavigate = { target ->
                    when (target) {
                        is HomeNavTarget.LiveTv -> navController.navigate(SirKTVDestinations.liveTv(target.channelId))
                        is HomeNavTarget.MoviePlayer -> navController.navigate(SirKTVDestinations.moviePlayer(target.movieId))
                        is HomeNavTarget.EpisodePlayer ->
                            navController.navigate(SirKTVDestinations.episodePlayer(target.seriesId, target.season, target.episode))
                        is HomeNavTarget.SeriesDetail ->
                            navController.navigate(SirKTVDestinations.seriesDetail(target.seriesId))
                    }
                },
                onNavigateToSports = { navController.navigate(SirKTVDestinations.SPORTS) },
                onNavigateToSettings = { navController.navigate(SirKTVDestinations.SETTINGS) },
                onNavigateToMovies = { navController.navigate(SirKTVDestinations.MOVIES) },
                onNavigateToSeries = { navController.navigate(SirKTVDestinations.SERIES) },
                onNavigateToFavorites = { navController.navigate(SirKTVDestinations.FAVORITES) },
                onNavigateToSearch = { navController.navigate(SirKTVDestinations.SEARCH) },
                onLoggedOut = {
                    navController.navigate(SirKTVDestinations.LOGIN) {
                        popUpTo(SirKTVDestinations.HOME) { inclusive = true }
                    }
                }
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
                }
            )
        }

        composable(SirKTVDestinations.FAVORITES) {
            FavoritesScreen(
                onLiveSelected = { channelId -> navController.navigate(SirKTVDestinations.liveTv(channelId)) },
                onMovieSelected = { movieId -> navController.navigate(SirKTVDestinations.moviePlayer(movieId)) },
                onSeriesSelected = { seriesId -> navController.navigate(SirKTVDestinations.seriesDetail(seriesId)) }
            )
        }

        composable(SirKTVDestinations.SEARCH) {
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
            VodPlayerScreen()
        }

        composable(
            route = SirKTVDestinations.EPISODE_PLAYER,
            arguments = listOf(
                navArgument("seriesId") { type = NavType.StringType },
                navArgument("season") { type = NavType.IntType },
                navArgument("episode") { type = NavType.IntType }
            )
        ) {
            VodPlayerScreen()
        }

        composable(SirKTVDestinations.SETTINGS) {
            SettingsScreen(onNavigate = { tile -> navController.navigate(SirKTVDestinations.settingsTile(tile)) })
        }

        composable(
            route = SirKTVDestinations.SETTINGS_TILE,
            arguments = listOf(navArgument("tileName") { type = NavType.StringType })
        ) { backStackEntry ->
            val tile = backStackEntry.arguments?.getString("tileName")
                ?.let { runCatching { SettingsTile.valueOf(it) }.getOrNull() }
            when (tile) {
                SettingsTile.STARTUP_PREFERENCES -> StartupPreferencesScreen()
                SettingsTile.PLAYER_PERFORMANCE, SettingsTile.PLAYBACK_QUALITY -> PlayerPerformanceScreen()
                SettingsTile.SUBTITLES_AUDIO -> SubtitleAppearanceScreen()
                SettingsTile.MANAGE_FAVORITES -> FavoritesScreen(
                    onLiveSelected = { channelId -> navController.navigate(SirKTVDestinations.liveTv(channelId)) },
                    onMovieSelected = { movieId -> navController.navigate(SirKTVDestinations.moviePlayer(movieId)) },
                    onSeriesSelected = { seriesId -> navController.navigate(SirKTVDestinations.seriesDetail(seriesId)) }
                )
                else -> ComingSoonScreen(title = tile?.label ?: "Settings")
            }
        }
    }
}
