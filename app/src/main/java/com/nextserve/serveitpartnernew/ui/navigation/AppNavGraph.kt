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
import com.nextserve.serveitpartnernew.ui.screen.OnboardingScreen
import com.nextserve.serveitpartnernew.ui.screen.RejectionScreen
import com.nextserve.serveitpartnernew.ui.screen.SplashScreen
import com.nextserve.serveitpartnernew.ui.screen.WaitingScreen
import com.nextserve.serveitpartnernew.ui.screen.auth.MobileNumberScreen
import com.nextserve.serveitpartnernew.ui.screen.auth.OtpVerificationScreen
import com.nextserve.serveitpartnernew.ui.screen.welcome.WelcomeScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object MobileNumber : Screen("mobile_number")
    object OtpVerification : Screen("otp_verification")
    object LanguageSelection : Screen("language_selection")
    object Onboarding : Screen("onboarding")
    object Waiting : Screen("waiting")
    object Rejection : Screen("rejection/{uid}") {
        fun createRoute(uid: String) = "rejection/$uid"
    }
    object Home : Screen("home")
}

fun NavGraphBuilder.appNavGraph(navController: NavController, authViewModel: com.nextserve.serveitpartnernew.ui.viewmodel.AuthViewModel) {
    composable(Screen.Splash.route) {
        SplashScreen()
        // Auto-navigate to mobile number screen after splash
        androidx.compose.runtime.LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500) // Minimum splash time
            navController.navigate(Screen.MobileNumber.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }
    
    composable(Screen.Welcome.route) {
        WelcomeScreen(
            onJoinClick = {
                navController.navigate(Screen.MobileNumber.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                }
            }
        )
    }

    composable(Screen.MobileNumber.route) {
        MobileNumberScreen(
            onOtpRequested = {
                navController.navigate(Screen.OtpVerification.route)
            },
            authViewModel = authViewModel
        )
    }

    composable(Screen.OtpVerification.route) {
        OtpVerificationScreen(
            onVerificationSuccess = {
                navController.navigate(Screen.LanguageSelection.route) {
                    popUpTo(Screen.MobileNumber.route) { inclusive = true }
                }
            },
            onBackToPhone = {
                navController.popBackStack()
            },
            authViewModel = authViewModel
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

