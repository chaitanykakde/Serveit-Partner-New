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
        val providerId = com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider.auth.currentUser?.uid ?: ""
        HomeScreen(providerId = providerId)
    }
    
    composable(BottomNavItem.Jobs.route) {
        val providerId = com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider.auth.currentUser?.uid ?: ""
        JobsScreen(providerId = providerId)
    }
    
    composable(BottomNavItem.Earnings.route) {
        EarningsScreen()
    }
    
    composable(BottomNavItem.Profile.route) {
        ProfileScreen(navController = navController)
    }
}

