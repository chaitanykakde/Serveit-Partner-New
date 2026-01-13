package com.nextserve.serveitpartnernew.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.util.Dimens
import com.nextserve.serveitpartnernew.ui.viewmodel.AuthState
import com.nextserve.serveitpartnernew.utils.LanguageManager
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    authState: AuthState,
    onNavigateToMobileNumber: () -> Unit,
    onNavigateToLanguageSelection: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    val context = LocalContext.current

    // Handle navigation based on auth state after minimum splash time
    LaunchedEffect(authState) {
        delay(1500) // Minimum splash time

        when (authState) {
            is AuthState.Authenticated -> {
                // User is already authenticated, check if language is selected
                val savedLanguage = LanguageManager.getSavedLanguage(context)
                if (savedLanguage.isNotEmpty() && savedLanguage != "en") {
                    // Language already selected, go directly to onboarding
                    onNavigateToOnboarding()
                } else {
                    // No language selected, go to language selection
                    onNavigateToLanguageSelection()
                }
            }
            else -> {
                // User not authenticated, start login flow
                onNavigateToMobileNumber()
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD),
                        Color(0xFFFFFFFF)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Centered content with logo and text
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            // Big Logo
            Image(
                painter = painterResource(id = R.drawable.serveit_partner_logo_light),
                contentDescription = "Serveit Partner Logo",
                modifier = Modifier.size(280.dp),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(Dimens.spacingXl))
            
            // Heading - Centered
            Text(
                text = "Serveit Professional",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(Dimens.spacingMd))
            
            // Subtitle - Centered
            Text(
                text = "Your trusted partner for professional services",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

