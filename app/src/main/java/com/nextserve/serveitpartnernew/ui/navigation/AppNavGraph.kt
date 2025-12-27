package com.nextserve.serveitpartnernew.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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
        SplashScreen(
            onNavigateToWelcome = {
                navController.navigate(Screen.Welcome.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            },
            onNavigateToLogin = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        )
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
        LoginScreen(
            onNavigateToOtp = { phoneNumber, verificationId, _ ->
                // resendToken is not serializable, so we pass null
                // It will be handled by AuthRepository when resending
                navController.navigate(Screen.Otp.createRoute(phoneNumber, verificationId))
            },
            onNavigateToOnboarding = { uid ->
                // Navigate to language selection first
                navController.navigate(Screen.LanguageSelection.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        )
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
        OtpScreen(
            phoneNumber = phoneNumber,
            verificationId = verificationId,
            resendToken = null, // resendToken not serializable, handled by repository
            onNavigateToOnboarding = { uid ->
                // Navigate to language selection first
                navController.navigate(Screen.LanguageSelection.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            },
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }

    composable(Screen.LanguageSelection.route) {
        LanguageSelectionScreen(
            onNavigateToOnboarding = {
                navController.navigate(Screen.Onboarding.route) {
                    // Don't pop language selection, allow back navigation if needed
                }
            }
        )
    }

    composable(Screen.Onboarding.route) {
        OnboardingScreen()
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

