package com.cjwilliams.pottytraining.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cjwilliams.pottytraining.Route

/**
 * The signed-out graph. It sits outside the main scaffold, so these screens draw their own
 * chrome and there is no bottom nav bar.
 */
@Composable
fun AuthNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Login,
        modifier = modifier
    ) {
        composable<Route.Login> {
            LoginScreen(onCreateAccount = { navController.navigate(Route.Signup) })
        }
        composable<Route.Signup> {
            SignupScreen(onBack = { navController.navigateUp() })
        }
    }
}
