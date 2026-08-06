package com.sirktv.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sirktv.app.presentation.livetv.LiveTvScreen
import com.sirktv.app.presentation.login.LoginScreen

@Composable
fun SirKTVNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = SirKTVDestinations.LOGIN
    ) {
        composable(SirKTVDestinations.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(SirKTVDestinations.LIVE_TV) {
                        popUpTo(SirKTVDestinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(SirKTVDestinations.LIVE_TV) {
            LiveTvScreen(
                onLoggedOut = {
                    navController.navigate(SirKTVDestinations.LOGIN) {
                        popUpTo(SirKTVDestinations.LIVE_TV) { inclusive = true }
                    }
                }
            )
        }
    }
}
