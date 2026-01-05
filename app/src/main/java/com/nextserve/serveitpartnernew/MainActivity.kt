package com.nextserve.serveitpartnernew

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.ui.navigation.Screen
import com.nextserve.serveitpartnernew.ui.screen.HomeScreen
import com.nextserve.serveitpartnernew.ui.screen.LanguageSelectionScreen
import com.nextserve.serveitpartnernew.ui.screen.LoginScreen
import com.nextserve.serveitpartnernew.ui.screen.OnboardingScreen
import com.nextserve.serveitpartnernew.ui.screen.OtpScreen
import com.nextserve.serveitpartnernew.ui.screen.RejectionScreen
import com.nextserve.serveitpartnernew.ui.screen.SplashScreen
import com.nextserve.serveitpartnernew.ui.screen.WaitingScreen
import com.nextserve.serveitpartnernew.ui.screen.welcome.WelcomeScreen
import com.nextserve.serveitpartnernew.ui.theme.ServeitPartnerNewTheme
import com.nextserve.serveitpartnernew.ui.utils.rememberNotificationPermissionState
import com.nextserve.serveitpartnernew.ui.viewmodel.AuthState
import com.nextserve.serveitpartnernew.ui.viewmodel.AuthViewModel
import com.nextserve.serveitpartnernew.utils.LanguageManager
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Apply saved language on app start
        LanguageManager.applySavedLanguage(this)

        setContent {
            ServeitPartnerNewTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()

                // Observe auth state with lifecycle awareness
                val authState by authViewModel.authState.collectAsStateWithLifecycle()

                // Request notification permission on app start
                val (hasNotificationPermission, requestNotificationPermission) =
                    rememberNotificationPermissionState()

                // Request notification permission when app opens (Android 13+)
                LaunchedEffect(Unit) {
                    if (!hasNotificationPermission) {
                        requestNotificationPermission()
                    }
                }

                // Always start with Splash screen, let auth state determine navigation
                val startDestination = Screen.Splash.route

                // Handle navigation based on auth state changes
                // Show splash for minimum time, then navigate based on auth state
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1500) // Minimum splash time

                    // Wait for auth state to be determined
                    authViewModel.authState.collect { state ->
                        if (state != AuthState.Uninitialized) {
                            when (state) {
                                AuthState.LoggedOut -> {
                                    if (navController.currentDestination?.route == Screen.Splash.route) {
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(Screen.Splash.route) { inclusive = true }
                                        }
                                    }
                                }
                                is AuthState.OtpSent -> {
                                    val otpState = state as AuthState.OtpSent
                                    if (navController.currentDestination?.route != Screen.Otp.route) {
                                        navController.navigate(Screen.Otp.createRoute(otpState.phoneNumber, otpState.verificationId)) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    }
                                }
                                is AuthState.Authenticated -> {
                                    // New user after OTP verification - navigate to language selection
                                    if (navController.currentDestination?.route != Screen.LanguageSelection.route) {
                                        navController.navigate(Screen.LanguageSelection.route) {
                                            popUpTo(navController.graph.id) { inclusive = true }
                                        }
                                    }
                                }
                                is AuthState.Onboarding -> {
                                    // User in onboarding process
                                    if (navController.currentDestination?.route != Screen.Onboarding.route) {
                                        navController.navigate(Screen.Onboarding.route) {
                                            popUpTo(navController.graph.id) { inclusive = true }
                                        }
                                    }
                                }
                                is AuthState.PendingApproval -> {
                                    // User waiting for approval
                                    if (navController.currentDestination?.route != Screen.Waiting.route) {
                                        navController.navigate(Screen.Waiting.route) {
                                            popUpTo(navController.graph.id) { inclusive = true }
                                        }
                                    }
                                }
                                is AuthState.Rejected -> {
                                    // User rejected
                                    val rejectedState = state as AuthState.Rejected
                                    val rejectionRoute = Screen.Rejection.createRoute(rejectedState.uid)
                                    if (navController.currentDestination?.route != rejectionRoute) {
                                        navController.navigate(rejectionRoute) {
                                            popUpTo(navController.graph.id) { inclusive = true }
                                        }
                                    }
                                }
                                AuthState.Approved -> {
                                    // User approved - navigate to home
                                    if (navController.currentDestination?.route != Screen.Home.route) {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(navController.graph.id) { inclusive = true }
                                        }
                                    }
                                }
                                else -> {
                                    // For splash screen, navigate to login as fallback
                                    if (navController.currentDestination?.route == Screen.Splash.route) {
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(Screen.Splash.route) { inclusive = true }
                                        }
                                    }
                                }
                            }
                            // Cancel collection after first determined state
                            return@collect
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize()
                ) {
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
                        LoginScreen(authViewModel = authViewModel)
                    }

                    composable(
                        route = Screen.Otp.route,
                        arguments = listOf(
                            navArgument("phoneNumber") { type = NavType.StringType },
                            navArgument("verificationId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
                        val verificationId = backStackEntry.arguments?.getString("verificationId") ?: ""
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
                                authViewModel.startOnboarding((authState as? AuthState.Authenticated)?.uid ?: "")
                            }
                        )
                    }

                    composable(Screen.Onboarding.route) {
                        val onboardingState = authState as? AuthState.Onboarding
                        OnboardingScreen(
                            uid = onboardingState?.uid,
                            authViewModel = authViewModel
                        )
                    }

                    composable(Screen.Waiting.route) {
                        WaitingScreen()
                    }

                    composable(
                        route = Screen.Rejection.route,
                        arguments = listOf(androidx.navigation.navArgument("uid") {
                            type = androidx.navigation.NavType.StringType
                        })
                    ) { backStackEntry ->
                        val uid = backStackEntry.arguments?.getString("uid") ?: ""
                        RejectionScreen(
                            uid = uid,
                            onEditProfile = {
                                authViewModel.startOnboarding(uid)
                            }
                        )
                    }

                    composable(Screen.Home.route) {
                        // Start incoming call listener service when provider reaches home
                        LaunchedEffect(Unit) {
                            com.nextserve.serveitpartnernew.data.service.IncomingCallListenerService.startService(
                                this@MainActivity
                            )
                        }

                        // Main app with bottom navigation will be shown here
                        com.nextserve.serveitpartnernew.ui.screen.main.MainAppScreen()
                    }
                }
            }
        }
    }
}