package com.cjwilliams.pottytraining

import androidx.annotation.StringRes
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
    @StringRes val nameRes: Int,
    val route: T,
    val icon: ImageVector
)

val TOP_LEVEL_ROUTES = listOf(
    TopLevelRoute(R.string.create_log_title, Route.CreateLog, Icons.Default.Add),
    TopLevelRoute(R.string.history_title, Route.History, Icons.Default.History),
    TopLevelRoute(R.string.settings_title, Route.Settings, Icons.Default.Settings)
)
