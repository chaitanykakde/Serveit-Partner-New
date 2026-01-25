package com.nextserve.serveitpartnernew.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        titleResId = com.nextserve.serveitpartnernew.R.string.nav_home,
        icon = Icons.Default.Home
    )
    
    object Jobs : BottomNavItem(
        route = "jobs",
        titleResId = com.nextserve.serveitpartnernew.R.string.nav_jobs,
        icon = Icons.Default.List
    )
    
    object Earnings : BottomNavItem(
        route = "earnings",
        titleResId = com.nextserve.serveitpartnernew.R.string.nav_earnings,
        icon = Icons.Default.Star
    )

    object Profile : BottomNavItem(
        route = "profile",
        titleResId = com.nextserve.serveitpartnernew.R.string.nav_profile,
        icon = Icons.Default.Person
    )

    companion object {
        val items = listOf(Home, Jobs, Earnings, Profile)
    }
}
