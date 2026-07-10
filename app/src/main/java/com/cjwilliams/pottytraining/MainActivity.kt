package com.cjwilliams.pottytraining

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cjwilliams.pottytraining.ui.createlog.PottyLogScreen
import com.cjwilliams.pottytraining.ui.createlog.SuccessScreen
import com.cjwilliams.pottytraining.ui.history.HistoryScreen
import com.cjwilliams.pottytraining.ui.settings.SettingsScreen
import com.cjwilliams.pottytraining.ui.theme.PottyTrainingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PottyTrainingTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val titleRes = currentDestination?.getTitle()
                val isTopLevel = TOP_LEVEL_ROUTES.any { topLevelRoute ->
                    currentDestination?.hasRoute(topLevelRoute.route::class) == true
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        titleRes?.let { resId ->
                            TopAppBar(
                                title = {
                                    Text(stringResource(resId))
                                },
                                navigationIcon = {
                                    if (!isTopLevel && navController.previousBackStackEntry != null) {
                                        IconButton(onClick = { navController.navigateUp() }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = stringResource(R.string.back_button_content_description)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        NavigationBar {
                            TOP_LEVEL_ROUTES.forEach { topLevelRoute ->
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            topLevelRoute.icon,
                                            contentDescription = stringResource(topLevelRoute.nameRes)
                                        )
                                    },
                                    label = { Text(stringResource(topLevelRoute.nameRes)) },
                                    selected = currentDestination?.hierarchy?.any {
                                        it.hasRoute(topLevelRoute.route::class)
                                    } == true,
                                    onClick = {
                                        Log.d("Navigation", "Tapped ${topLevelRoute.javaClass.simpleName}")
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
                                        popUpTo(navController.graph.findStartDestination().id)
                                        launchSingleTop = true
                                        restoreState = false
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

private fun NavDestination.getTitle(): Int? =
    when {
        hasRoute<Route.CreateLog>() -> R.string.create_log_title
        hasRoute<Route.History>() -> R.string.history_title
        hasRoute<Route.Settings>() -> R.string.settings_title
        hasRoute<Route.Success>() -> R.string.success_title
        hasRoute<Route.EditLog>() -> R.string.edit_log_title
        else -> null
    }
