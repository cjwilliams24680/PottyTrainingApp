package com.cjwilliams.pottytraining

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cjwilliams.pottytraining.ui.createlog.PottyLogScreen
import com.cjwilliams.pottytraining.ui.createlog.PottyLogViewModel
import com.cjwilliams.pottytraining.ui.createlog.SuccessScreen
import com.cjwilliams.pottytraining.ui.history.HistoryScreen
import com.cjwilliams.pottytraining.ui.settings.SettingsScreen
import com.cjwilliams.pottytraining.ui.theme.PottyTrainingTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object CreateLog : Route
    @Serializable
    data object History : Route
    @Serializable
    data object Settings : Route
    @Serializable
    data class Success(val isAccident: Boolean) : Route
    @Serializable
    data class EditLog(val logId: Int) : Route
}

data class TopLevelRoute<T : Any>(
    val name: String,
    val route: T,
    val icon: ImageVector
)

val TOP_LEVEL_ROUTES = listOf(
    TopLevelRoute("Create Log", Route.CreateLog, Icons.Default.Add),
    TopLevelRoute("History", Route.History, Icons.Default.History),
    TopLevelRoute("Settings", Route.Settings, Icons.Default.Settings)
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PottyTrainingTheme {
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            TOP_LEVEL_ROUTES.forEach { topLevelRoute ->
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            topLevelRoute.icon,
                                            contentDescription = topLevelRoute.name
                                        )
                                    },
                                    label = { Text(topLevelRoute.name) },
                                    selected = currentDestination?.hierarchy?.any {
                                        it.hasRoute(topLevelRoute.route::class)
                                    } == true,
                                    onClick = {
                                        Log.d("Navigation", "Tapped ${topLevelRoute.name}")
                                        navController.navigate(topLevelRoute.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Route.CreateLog,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable<Route.CreateLog> {
                            PottyLogScreen(
                                onSaveSuccess = { result ->
                                    navController.navigate(Route.Success(result.isAccident))
                                }
                            )
                        }
                        composable<Route.History> {
                            HistoryScreen(
                                onEditLog = { logId ->
                                    navController.navigate(Route.EditLog(logId))
                                }
                            )
                        }
                        composable<Route.Settings> {
                            SettingsScreen()
                        }
                        composable<Route.Success> { backStackEntry ->
                            val route = backStackEntry.toRoute<Route.Success>()
                            SuccessScreen(
                                isAccident = route.isAccident,
                                onContinue = {
                                    navController.navigate(Route.History) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            inclusive = false
                                        }
                                    }
                                }
                            )
                        }
                        composable<Route.EditLog> {
                            PottyLogScreen(
                                onSaveSuccess = { result ->
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenPlaceholder(name: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = name)
    }
}
