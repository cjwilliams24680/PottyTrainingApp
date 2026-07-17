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
    /** Shown only while signed out, in their own nav graph outside the main scaffold. */
    @Serializable
    data object Login : Route
    @Serializable
    data object Signup : Route

    /** Nested graphs so detail screens keep their tab's bottom-nav item selected. */
    @Serializable
    data object CreateLogGraph : Route
    @Serializable
    data object HistoryGraph : Route

    @Serializable
    data object CreateLog : Route
    @Serializable
    data object History : Route
    @Serializable
    data object Settings : Route
    @Serializable
    data class Success(val isAccident: Boolean) : Route
    @Serializable
    data class EditLog(val logId: String) : Route
}

data class TopLevelRoute<T : Any>(
    @StringRes val nameRes: Int,
    val route: T,
    val icon: ImageVector
)

val TOP_LEVEL_ROUTES = listOf(
    TopLevelRoute(R.string.create_log_title, Route.CreateLogGraph, Icons.Default.Add),
    TopLevelRoute(R.string.history_title, Route.HistoryGraph, Icons.Default.History),
    TopLevelRoute(R.string.settings_title, Route.Settings, Icons.Default.Settings)
)
