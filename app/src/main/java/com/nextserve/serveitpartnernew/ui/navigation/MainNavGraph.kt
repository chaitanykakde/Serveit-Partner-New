package com.nextserve.serveitpartnernew.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nextserve.serveitpartnernew.ui.screen.main.EarningsScreen
import com.nextserve.serveitpartnernew.ui.screen.main.HomeScreen
import com.nextserve.serveitpartnernew.ui.screen.main.JobDetailsScreen
import com.nextserve.serveitpartnernew.ui.screen.main.JobsScreen
import com.nextserve.serveitpartnernew.ui.screen.main.ProfileScreen

fun NavGraphBuilder.mainNavGraph(navController: NavController) {
    composable(BottomNavItem.Home.route) {
        val providerId = com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider.auth.currentUser?.uid ?: ""
        HomeScreen(providerId = providerId)
    }
    
    composable(BottomNavItem.Jobs.route) {
        val providerId = com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider.auth.currentUser?.uid ?: ""
        JobsScreen(
            providerId = providerId,
            onNavigateToJobDetails = { bookingId, customerPhoneNumber, bookingIndex ->
                val route = if (bookingIndex != null && bookingIndex >= 0) {
                    "jobDetails/$bookingId/$customerPhoneNumber/$bookingIndex"
                } else {
                    "jobDetails/$bookingId/$customerPhoneNumber/-1"
                }
                navController.navigate(route)
            }
        )
    }
    
    composable(
        route = "jobDetails/{bookingId}/{customerPhoneNumber}/{bookingIndex}",
        arguments = listOf(
            navArgument("bookingId") { type = NavType.StringType },
            navArgument("customerPhoneNumber") { type = NavType.StringType },
            navArgument("bookingIndex") { type = NavType.IntType; defaultValue = -1 }
        )
    ) { backStackEntry ->
        val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
        val customerPhoneNumber = backStackEntry.arguments?.getString("customerPhoneNumber") ?: ""
        val bookingIndexArg = backStackEntry.arguments?.getInt("bookingIndex") ?: -1
        val providerId = com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider.auth.currentUser?.uid ?: ""
        val bookingIndex = if (bookingIndexArg >= 0) bookingIndexArg else null
        
        JobDetailsScreen(
            bookingId = bookingId,
            customerPhoneNumber = customerPhoneNumber,
            providerId = providerId,
            bookingIndex = bookingIndex,
            onBack = { navController.popBackStack() },
            onJobAccepted = { navController.popBackStack() },
            onJobRejected = { navController.popBackStack() }
        )
    }
    
    composable(BottomNavItem.Earnings.route) {
        EarningsScreen()
    }
    
    composable(BottomNavItem.Profile.route) {
        ProfileScreen(navController = navController)
    }
}

