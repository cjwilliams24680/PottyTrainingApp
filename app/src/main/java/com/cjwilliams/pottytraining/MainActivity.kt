package com.cjwilliams.pottytraining

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cjwilliams.pottytraining.ui.auth.AuthNavHost
import com.cjwilliams.pottytraining.ui.createlog.PottyLogScreen
import com.cjwilliams.pottytraining.ui.createlog.SuccessScreen
import com.cjwilliams.pottytraining.ui.history.HistoryScreen
import com.cjwilliams.pottytraining.ui.settings.SettingsScreen
import com.cjwilliams.pottytraining.ui.theme.PottyTrainingTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PottyTrainingTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val sessionUiState by viewModel.sessionUiState.collectAsStateWithLifecycle()

                // Swapping on session state means signing out anywhere — including a refresh
                // token the server rejected — lands the user back on Login without navigating.
                when (sessionUiState) {
                    is SessionUiState.Unknown -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }

                    is SessionUiState.LoggedOut -> AuthNavHost()
                    is SessionUiState.LoggedIn -> MainAppScaffold()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppScaffold() {
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
                            Timber.d("Tapped ${topLevelRoute.javaClass.simpleName}")
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
            // consumeWindowInsets keeps imePadding from re-adding the bottom-bar
            // inset already applied via innerPadding.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
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
            composable<Route.EditLog> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.EditLog>()
                PottyLogScreen(
                    logId = route.logId,
                    onSaveSuccess = {
                        navController.popBackStack()
                    }
                )
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
