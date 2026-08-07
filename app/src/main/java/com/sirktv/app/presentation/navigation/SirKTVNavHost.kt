package com.sirktv.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sirktv.app.presentation.hub.HubScreen
import com.sirktv.app.presentation.livetv.LiveTvPlayerScreen
import com.sirktv.app.presentation.login.LoginScreen
import com.sirktv.app.presentation.settings.ComingSoonScreen
import com.sirktv.app.presentation.settings.PlayerPerformanceScreen
import com.sirktv.app.presentation.settings.SettingsScreen
import com.sirktv.app.presentation.settings.SettingsTile
import com.sirktv.app.presentation.settings.StartupPreferencesScreen
import com.sirktv.app.presentation.sports.SportsScreen

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
                onNavigateToHub = {
                    navController.navigate(SirKTVDestinations.HUB) {
                        popUpTo(SirKTVDestinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(SirKTVDestinations.HUB) {
            HubScreen(
                onNavigateToLiveTv = { channelId -> navController.navigate(SirKTVDestinations.liveTv(channelId)) },
                onNavigateToSports = { navController.navigate(SirKTVDestinations.SPORTS) },
                onNavigateToSettings = { navController.navigate(SirKTVDestinations.SETTINGS) },
                onLoggedOut = {
                    navController.navigate(SirKTVDestinations.LOGIN) {
                        popUpTo(SirKTVDestinations.HUB) { inclusive = true }
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
                else -> ComingSoonScreen(title = tile?.label ?: "Settings")
            }
        }
    }
}
