package com.cjwilliams.pottytraining

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
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
