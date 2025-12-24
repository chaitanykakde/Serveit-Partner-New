package com.nextserve.serveitpartnernew.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nextserve.serveitpartnernew.ui.screen.main.EarningsScreen
import com.nextserve.serveitpartnernew.ui.screen.main.HomeScreen
import com.nextserve.serveitpartnernew.ui.screen.main.JobsScreen
import com.nextserve.serveitpartnernew.ui.screen.main.ProfileScreen

fun NavGraphBuilder.mainNavGraph(navController: NavController) {
    composable(BottomNavItem.Home.route) {
        HomeScreen()
    }
    
    composable(BottomNavItem.Jobs.route) {
        JobsScreen()
    }
    
    composable(BottomNavItem.Earnings.route) {
        EarningsScreen()
    }
    
    composable(BottomNavItem.Profile.route) {
        ProfileScreen(navController = navController)
    }
}

