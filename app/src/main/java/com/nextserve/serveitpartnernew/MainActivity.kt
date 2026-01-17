package com.nextserve.serveitpartnernew

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.nextserve.serveitpartnernew.ui.navigation.appNavGraph
import com.nextserve.serveitpartnernew.ui.screen.HomeScreen
import com.nextserve.serveitpartnernew.ui.screen.LanguageSelectionScreen
import com.nextserve.serveitpartnernew.ui.screen.OnboardingScreen
import com.nextserve.serveitpartnernew.ui.screen.RejectionScreen
import com.nextserve.serveitpartnernew.ui.screen.SplashScreen
import com.nextserve.serveitpartnernew.ui.screen.WaitingScreen
import com.nextserve.serveitpartnernew.ui.screen.welcome.WelcomeScreen
import com.nextserve.serveitpartnernew.ui.theme.ServeitPartnerNewTheme
import com.nextserve.serveitpartnernew.ui.utils.rememberNotificationPermissionState
import com.nextserve.serveitpartnernew.ui.viewmodel.AuthState
import com.nextserve.serveitpartnernew.ui.viewmodel.AuthViewModel
import com.nextserve.serveitpartnernew.ui.viewmodel.AuthViewModelFactory
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
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(LocalContext.current)
                )

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

                // Navigation will be handled by appNavGraph and individual screens

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize()
                ) {
                    appNavGraph(navController, authViewModel)
                }
            }
        }
    }
}