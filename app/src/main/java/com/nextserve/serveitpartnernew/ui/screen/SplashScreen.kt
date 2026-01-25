package com.nextserve.serveitpartnernew.ui.screen

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.viewmodel.AppStartDestination
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    authViewModel: com.nextserve.serveitpartnernew.ui.viewmodel.AuthViewModel,
    onNavigateToMobileNumber: () -> Unit,
    onNavigateToLanguageSelection: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToWaiting: () -> Unit,
    onNavigateToRejection: (String) -> Unit,
    onNavigateToHome: () -> Unit
) {
    // Observe the single source of truth for navigation destination
    val destination by authViewModel.startDestination.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val currentUserId = authViewModel.getCurrentUserId()

    // Handle navigation based on destination after minimum splash time
    LaunchedEffect(destination) {
        Log.d("SplashScreen", "🚀 SplashScreen LaunchedEffect triggered with destination: $destination")

        delay(1500) // Minimum splash time

        Log.d("SplashScreen", "⏰ Splash delay complete, processing navigation for destination: $destination")

        when (destination) {
            AppStartDestination.Splash -> {
                // Stay on splash - waiting for state resolution
                Log.d("SplashScreen", "⏳ Waiting for state resolution...")
            }
            AppStartDestination.MobileNumber -> {
                Log.d("SplashScreen", "📱 Navigating to mobile number (login)")
                onNavigateToMobileNumber()
            }
            AppStartDestination.LanguageSelection -> {
                Log.d("SplashScreen", "🌐 Navigating to language selection")
                onNavigateToLanguageSelection()
            }
            AppStartDestination.Onboarding -> {
                Log.d("SplashScreen", "📝 Navigating to onboarding (or step 5 for pending verification)")
                onNavigateToOnboarding()
            }
            AppStartDestination.Home -> {
                Log.d("SplashScreen", "🏠 Navigating to home")
                onNavigateToHome()
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF5EC6F4)),
        contentAlignment = Alignment.Center
    ) {
        // Centered logo
        Image(
            painter = painterResource(id = R.drawable.serveitlogo),
            contentDescription = "Serveit Partner Logo",
            modifier = Modifier.size(280.dp),
            contentScale = ContentScale.Fit
        )
    }
}

