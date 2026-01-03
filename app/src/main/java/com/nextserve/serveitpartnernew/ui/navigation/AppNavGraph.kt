package com.nextserve.serveitpartnernew.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.nextserve.serveitpartnernew.ui.screen.HomeScreen
import com.nextserve.serveitpartnernew.ui.screen.LanguageSelectionScreen
import com.nextserve.serveitpartnernew.ui.screen.LoginScreen
import com.nextserve.serveitpartnernew.ui.screen.OnboardingScreen
import com.nextserve.serveitpartnernew.ui.screen.OtpScreen
import com.nextserve.serveitpartnernew.ui.screen.RejectionScreen
import com.nextserve.serveitpartnernew.ui.screen.SplashScreen
import com.nextserve.serveitpartnernew.ui.screen.WaitingScreen
import com.nextserve.serveitpartnernew.ui.screen.welcome.WelcomeScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Otp : Screen("otp/{phoneNumber}/{verificationId}") {
        fun createRoute(phoneNumber: String, verificationId: String) = "otp/$phoneNumber/$verificationId"
    }
    object LanguageSelection : Screen("language_selection")
    object Onboarding : Screen("onboarding")
    object Waiting : Screen("waiting")
    object Rejection : Screen("rejection/{uid}") {
        fun createRoute(uid: String) = "rejection/$uid"
    }
    object Home : Screen("home")
}

fun NavGraphBuilder.appNavGraph(navController: NavController) {
    composable(Screen.Splash.route) {
        SplashScreen()
    }
    
    composable(Screen.Welcome.route) {
        WelcomeScreen(
            onJoinClick = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                }
            }
        )
    }
    
    composable(Screen.Login.route) {
        val authViewModel: com.nextserve.serveitpartnernew.ui.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        LoginScreen(authViewModel = authViewModel)
    }

    composable(
        route = Screen.Otp.route,
        arguments = listOf(
            navArgument("phoneNumber") {
                type = NavType.StringType
            },
            navArgument("verificationId") {
                type = NavType.StringType
            }
        )
    ) { backStackEntry ->
        val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
        val verificationId = backStackEntry.arguments?.getString("verificationId") ?: ""
        val authViewModel: com.nextserve.serveitpartnernew.ui.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        OtpScreen(
            phoneNumber = phoneNumber,
            verificationId = verificationId,
            authViewModel = authViewModel,
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }

    composable(Screen.LanguageSelection.route) {
        LanguageSelectionScreen(
            onNavigateToOnboarding = {
                navController.navigate(Screen.Onboarding.route) {
                    // Pop language selection after navigating to onboarding
                    popUpTo(Screen.LanguageSelection.route) { inclusive = true }
                }
            }
        )
    }

    composable(Screen.Onboarding.route) {
        val authViewModel: com.nextserve.serveitpartnernew.ui.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        OnboardingScreen(
            authViewModel = authViewModel
        )
    }

    composable(Screen.Waiting.route) {
        WaitingScreen()
    }

    composable(
        route = Screen.Rejection.route,
        arguments = listOf(navArgument("uid") { type = NavType.StringType })
    ) { backStackEntry ->
        val uid = backStackEntry.arguments?.getString("uid") ?: ""
        RejectionScreen(
            uid = uid,
            onEditProfile = {
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(Screen.Rejection.route) { inclusive = true }
                }
            }
        )
    }

    composable(Screen.Home.route) {
        // Main app with bottom navigation will be shown here
        com.nextserve.serveitpartnernew.ui.screen.main.MainAppScreen()
    }
}

