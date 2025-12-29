package com.nextserve.serveitpartnernew.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.screen.login.LoginComponents
import com.nextserve.serveitpartnernew.ui.theme.OrangeAccent
import com.nextserve.serveitpartnernew.ui.util.Dimens
import com.nextserve.serveitpartnernew.ui.viewmodel.LoginViewModel
import com.nextserve.serveitpartnernew.utils.NetworkMonitor

@Composable
fun LoginScreen(
    onNavigateToOtp: (String, String, com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken?) -> Unit,
    onNavigateToOnboarding: (String) -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    
    val networkMonitor = remember { NetworkMonitor(context) }
    val focusRequester = remember { FocusRequester() }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth >= 600.dp
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        viewModel.onOtpSent = { phoneNumber, verificationId, resendToken ->
            onNavigateToOtp(phoneNumber, verificationId, resendToken)
        }
        viewModel.onAutoVerified = { uid ->
            onNavigateToOnboarding(uid)
        }
        viewModel.onError = { error ->
            // Error shown in UI state
        }
    }

    // Background with image and gradient overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // Background Image - Fill entire screen
        Image(
            painter = painterResource(id = R.drawable.serveit_partner_flow_bg),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Light gradient overlay for readability (reduced opacity)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE3F2FD).copy(alpha = 0.3f),
                            Color(0xFFFFFFFF).copy(alpha = 0.4f)
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .widthIn(max = if (isTablet) 600.dp else screenWidth)
                .padding(horizontal = Dimens.paddingLg)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(if (isTablet) 60.dp else 48.dp))

            // Logo
            LoginComponents.ServeitLogo()

            Spacer(modifier = Modifier.height(Dimens.spacingXxl))

            // Title - Centered
            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spacingSm))

            // Subtitle - Centered
            Text(
                text = "Login using your registered mobile number",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXxl))

            // Phone Number Input with +91 prefix
            LoginComponents.PhoneNumberInput(
                phoneNumber = uiState.phoneNumber,
                onPhoneNumberChange = { newValue ->
                    viewModel.updatePhoneNumber(newValue)
                    if (uiState.errorMessage != null) {
                        viewModel.clearError()
                    }
                },
                isError = uiState.errorMessage != null,
                errorMessage = uiState.errorMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXl))

            // Send OTP Button
            PrimaryButton(
                text = if (uiState.isSendingOtp) stringResource(R.string.sending) else stringResource(R.string.send_otp),
                onClick = {
                    if (uiState.isPhoneNumberValid && !uiState.isSendingOtp) {
                        viewModel.sendOtp(activity)
                    }
                },
                enabled = uiState.isPhoneNumberValid && !uiState.isSendingOtp,
                isLoading = uiState.isSendingOtp,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            )

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            // Helper Text
            Text(
                text = "You'll receive a 6-digit verification code on this number",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Error message
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(Dimens.spacingSm))
                Text(
                    text = uiState.errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.paddingXs),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer - Two rows
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.spacingXl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Row 1: "By continuing, you agree to our" (unbolded)
                Text(
                    text = "By continuing, you agree to our",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                // Row 2: "Terms and Privacy Policy" (bolded)
                TextButton(
                    onClick = { /* Handle Terms & Privacy click */ },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Terms and Privacy Policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
