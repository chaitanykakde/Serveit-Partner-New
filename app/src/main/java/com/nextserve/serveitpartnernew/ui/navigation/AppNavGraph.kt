package com.nextserve.serveitpartnernew.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.nextserve.serveitpartnernew.ui.screen.auth.LoginScreen
import com.nextserve.serveitpartnernew.ui.screen.auth.OtpScreen
import com.nextserve.serveitpartnernew.ui.screen.welcome.WelcomeScreen
import com.nextserve.serveitpartnernew.ui.viewmodel.OnboardingViewModelFactory

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
        SplashScreen(
            authViewModel = authViewModel,
            onNavigateToMobileNumber = {
                navController.navigate(Screen.MobileNumber.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            },
            onNavigateToLanguageSelection = {
                navController.navigate(Screen.LanguageSelection.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            },
            onNavigateToOnboarding = {
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            },
            onNavigateToWaiting = {
                navController.navigate(Screen.Waiting.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            },
            onNavigateToRejection = { uid ->
                navController.navigate(Screen.Rejection.createRoute(uid)) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            },
            onNavigateToHome = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        )
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
        LoginScreen(
            onOtpRequested = {
                navController.navigate(Screen.OtpVerification.route)
            },
            authViewModel = authViewModel
        )
    }

    composable(Screen.OtpVerification.route) {
        OtpScreen(
            onVerificationSuccess = {
                navController.navigate(Screen.Splash.route) {
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
            },
            authViewModel = authViewModel
        )
    }

    composable(Screen.Onboarding.route) {
        val onboardingViewModel: com.nextserve.serveitpartnernew.ui.viewmodel.OnboardingViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
            factory = OnboardingViewModelFactory(androidx.compose.ui.platform.LocalContext.current)
        )
        
        // Observe auth state to navigate when logged out
        val authState by authViewModel.authState.collectAsStateWithLifecycle()
        androidx.compose.runtime.LaunchedEffect(authState) {
            if (authState is com.nextserve.serveitpartnernew.ui.viewmodel.AuthState.LoggedOut) {
                android.util.Log.d("AppNavGraph", "🚪 User logged out from onboarding, navigating to splash")
                navController.navigate(Screen.Splash.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            }
        }
        
        // REMOVED: Navigation logic from OnboardingScreen
        // Navigation is now handled by SplashScreen based on startDestination
        // Onboarding is an inner app flow - users can logout, edit, resubmit
        
        OnboardingScreen(
            uiState = onboardingViewModel.uiState,
            onUpdateFullName = onboardingViewModel::updateFullName,
            onUpdateGender = onboardingViewModel::updateGender,
            onUpdatePrimaryService = onboardingViewModel::updatePrimaryService,
            onUpdateEmail = onboardingViewModel::updateEmail,
            onUpdateLanguage = onboardingViewModel::updateLanguage,
            onUpdateSelectedMainService = onboardingViewModel::updateSelectedMainService,
            onToggleSubService = onboardingViewModel::toggleSubService,
            onToggleSelectAll = onboardingViewModel::toggleSelectAll,
            onUpdateOtherService = onboardingViewModel::updateOtherService,
            onLoadSubServices = onboardingViewModel::loadSubServices,
            onUpdateState = onboardingViewModel::updateState,
            onUpdateCity = onboardingViewModel::updateCity,
            onUpdateAddress = onboardingViewModel::updateAddress,
            onUpdateFullAddress = onboardingViewModel::updateFullAddress,
            onUpdateLocationPincode = onboardingViewModel::updateLocationPincode,
            onUpdateServiceRadius = onboardingViewModel::updateServiceRadius,
            onUseCurrentLocation = onboardingViewModel::useCurrentLocation,
            onUploadAadhaarFront = onboardingViewModel::uploadAadhaarFront,
            onUploadAadhaarBack = onboardingViewModel::uploadAadhaarBack,
            onUploadProfilePhoto = onboardingViewModel::uploadProfilePhoto,
            onDeleteAadhaarFront = onboardingViewModel::deleteAadhaarFront,
            onDeleteAadhaarBack = onboardingViewModel::deleteAadhaarBack,
            onDeleteProfilePhoto = onboardingViewModel::deleteProfilePhoto,
            onNextStep = onboardingViewModel::nextStep,
            onPreviousStep = onboardingViewModel::previousStep,
            onNavigateToStep = onboardingViewModel::navigateToStep,
            onSubmit = onboardingViewModel::submitOnboarding,
            onReset = onboardingViewModel::resetOnboarding,
            onEditRejectedProfile = onboardingViewModel::editRejectedProfile,
            onContactSupport = {
                // TODO: Navigate to contact support screen or open support dialog
            },
            onLogout = {
                authViewModel.signOut()
            }
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
        // Observe auth state to navigate when logged out
        val authState by authViewModel.authState.collectAsStateWithLifecycle()
        androidx.compose.runtime.LaunchedEffect(authState) {
            if (authState is com.nextserve.serveitpartnernew.ui.viewmodel.AuthState.LoggedOut ||
                authState is com.nextserve.serveitpartnernew.ui.viewmodel.AuthState.Idle) {
                android.util.Log.d("AppNavGraph", "🚪 User logged out, navigating to splash")
                navController.navigate(Screen.Splash.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            }
        }
        
        // Main app with bottom navigation will be shown here
        com.nextserve.serveitpartnernew.ui.screen.main.MainAppScreen(
            authViewModel = authViewModel
        )
    }
}

