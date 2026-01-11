package com.nextserve.serveitpartnernew.ui.screen.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nextserve.serveitpartnernew.ui.components.BottomNavigationBar
import com.nextserve.serveitpartnernew.ui.navigation.BottomNavItem
import com.nextserve.serveitpartnernew.ui.screen.about.AboutAppScreen
import com.nextserve.serveitpartnernew.ui.screen.profile.edit.ProfileEditAddressScreen
import com.nextserve.serveitpartnernew.ui.screen.profile.edit.ProfileEditBasicScreen
import com.nextserve.serveitpartnernew.ui.screen.profile.edit.ProfileEditDocumentsScreen
import com.nextserve.serveitpartnernew.ui.screen.profile.edit.ProfileEditPreferencesScreen
import com.nextserve.serveitpartnernew.ui.screen.profile.edit.ProfileEditServicesScreen
import com.nextserve.serveitpartnernew.ui.screen.support.HelpSupportScreen
import com.nextserve.serveitpartnernew.ui.screen.main.JobDetailsScreen
import com.nextserve.serveitpartnernew.ui.screen.earnings.EarningsScreen
import com.nextserve.serveitpartnernew.ui.screen.payout.PayoutScreen

@Composable
fun MainAppScreen(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(BottomNavItem.Home.route) { backStackEntry ->
                val providerId = com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider.auth.currentUser?.uid ?: ""

                // Create HomeViewModel scoped to this navigation entry
                val homeViewModel: com.nextserve.serveitpartnernew.ui.viewmodel.HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = com.nextserve.serveitpartnernew.ui.viewmodel.HomeViewModel.factory(
                        providerId = providerId,
                        context = androidx.compose.ui.platform.LocalContext.current
                    )
                )

                HomeScreen(
                    providerId = providerId,
                    viewModel = homeViewModel,
                    parentPaddingValues = paddingValues,
                    onOngoingJobClick = { job ->
                        // Navigate to job details with full job information
                        // For ongoing jobs, bookingIndex may not be available, so use -1
                        val route = "jobDetails/${job.bookingId}/${job.customerPhoneNumber}/-1"
                        navController.navigate(route)
                    },
                    onJobAccepted = { jobId ->
                        // Job acceptance is handled internally by HomeScreen's ViewModel
                        // This callback is for any additional navigation or side effects
                    },
                    onViewAllJobs = {
                        // Navigate to jobs list screen
                        navController.navigate(BottomNavItem.Jobs.route) {
                            popUpTo(BottomNavItem.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(BottomNavItem.Jobs.route) { backStackEntry ->
                val providerId = com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider.auth.currentUser?.uid ?: ""
                JobsScreen(
                    providerId = providerId,
                    parentPaddingValues = paddingValues,
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
            composable(BottomNavItem.Payouts.route) {
                PayoutScreen()
            }
            composable(BottomNavItem.Profile.route) { backStackEntry ->
                ProfileScreen(
                    navController = navController,
                    parentPaddingValues = paddingValues
                )
            }
            composable("profile/edit/basic") { ProfileEditBasicScreen(navController) }
            composable("profile/edit/services") { ProfileEditServicesScreen(navController) }
            composable("profile/edit/address") { ProfileEditAddressScreen(navController) }
            composable("profile/edit/documents") { ProfileEditDocumentsScreen(navController) }
            composable("profile/edit/preferences") { ProfileEditPreferencesScreen(navController) }
            composable("help/support") { HelpSupportScreen(navController) }
            composable("about/app") { AboutAppScreen(navController) }
        }
    }
}

