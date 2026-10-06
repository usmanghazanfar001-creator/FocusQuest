package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val filledIcon: ImageVector? = null,
    val outlinedIcon: ImageVector? = null
) {
    object Onboarding : Screen("onboarding", "Onboarding")
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Tasks : Screen("tasks", "Tasks", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle)
    object Focus : Screen("focus", "Focus", Icons.Filled.Timer, Icons.Outlined.Timer)
    object Stats : Screen("stats", "Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)

    object Rewards : Screen("rewards", "Rewards Shop")
    object Leaderboard : Screen("leaderboard", "Leaderboard")
    object ProUpgrade : Screen("pro_upgrade", "FocusQuest Pro")

    companion object {
        val bottomNavItems = listOf(Home, Tasks, Focus, Stats, Profile)
    }
}
