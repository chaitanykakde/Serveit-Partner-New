package com.nextserve.serveitpartnernew.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.OutlinedInputField
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
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
    
    // Initialize NetworkMonitor for offline detection
    val networkMonitor = remember { NetworkMonitor(context) }
    val focusRequester = remember { FocusRequester() }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth >= 600.dp
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Setup callbacks
    LaunchedEffect(Unit) {
        viewModel.onOtpSent = { phoneNumber, verificationId, resendToken ->
            onNavigateToOtp(phoneNumber, verificationId, resendToken)
        }
        viewModel.onAutoVerified = { uid ->
            // Auto-verification succeeded, navigate directly to onboarding
            onNavigateToOnboarding(uid)
        }
        viewModel.onError = { error ->
            // Error shown in UI state
        }
    }

    // Light gradient background
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF8FAFF), // Light blue-white
            Color(0xFFFFFFFF)  // White
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .widthIn(max = if (isTablet) 600.dp else screenWidth)
                .padding(horizontal = 24.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(if (isTablet) 60.dp else 48.dp))

            // Serveit Partner Logo
            Image(
                painter = painterResource(id = R.drawable.serveitpartnerlogo),
                contentDescription = "Serveit Partner Logo",
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .widthIn(max = 180.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(32.dp))

            // App Name
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Mobile Number Input with India Flag and +91
            OutlinedInputField(
                value = uiState.phoneNumber,
                onValueChange = { newValue ->
                    viewModel.updatePhoneNumber(newValue)
                    if (uiState.errorMessage != null) {
                        viewModel.clearError()
                    }
                },
                placeholder = stringResource(R.string.enter_mobile_number),
                leadingIcon = {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🇮🇳",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "+91",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
                isError = uiState.errorMessage != null,
                errorMessage = uiState.errorMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Helper Text
            Text(
                text = stringResource(R.string.otp_verification_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

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
                modifier = Modifier.fillMaxWidth()
            )

            // Error message
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Main Illustration
            Image(
                painter = painterResource(id = R.drawable.big_loginpageicon),
                contentDescription = "Login Illustration",
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .widthIn(max = 280.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
