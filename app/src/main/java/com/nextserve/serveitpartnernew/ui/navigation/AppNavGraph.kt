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
            authState = authViewModel.authState.value,
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
        val onboardingViewModel: com.nextserve.serveitpartnernew.ui.viewmodel.OnboardingViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
            factory = OnboardingViewModelFactory(androidx.compose.ui.platform.LocalContext.current)
        )
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
            onUseCurrentLocation = { _, _ -> /* TODO: Implement location */ },
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
            onReset = onboardingViewModel::resetOnboarding
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

