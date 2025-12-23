package com.nextserve.serveitpartnernew.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )
    
    object Jobs : BottomNavItem(
        route = "jobs",
        title = "Jobs",
        icon = Icons.Default.List
    )
    
    object Earnings : BottomNavItem(
        route = "earnings",
        title = "Earnings",
        icon = Icons.Default.Star
    )
    
    object Profile : BottomNavItem(
        route = "profile",
        title = "Profile",
        icon = Icons.Default.Person
    )
    
    companion object {
        val items = listOf(Home, Jobs, Earnings, Profile)
    }
}
