package com.nextserve.serveitpartnernew.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.OTPInputField
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.viewmodel.OtpViewModel
import com.nextserve.serveitpartnernew.utils.NetworkMonitor

@Composable
fun OtpScreen(
    phoneNumber: String,
    verificationId: String,
    resendToken: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken? = null,
    onNavigateToOnboarding: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: OtpViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    
    // Initialize NetworkMonitor for offline detection
    val networkMonitor = remember { NetworkMonitor(context) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth >= 600.dp
    val scrollState = rememberScrollState()

    // Initialize phone number, verification ID, and resend token
    LaunchedEffect(phoneNumber, verificationId) {
        viewModel.setPhoneNumber(phoneNumber)
        if (verificationId.isNotEmpty()) {
            viewModel.setVerificationId(verificationId)
        }
        if (resendToken != null) {
            viewModel.setResendToken(resendToken)
        }
    }

    // Setup verification success callback
    LaunchedEffect(Unit) {
        viewModel.onVerificationSuccess = { uid ->
            onNavigateToOnboarding(uid)
        }
    }

    // Cancel verification on back navigation
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            // Cleanup handled in ViewModel.onCleared()
        }
    }

    // Light gradient background (matching Login screen)
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

            // Serveit Partner Logo (smaller than login screen)
            Image(
                painter = painterResource(id = R.drawable.serveitpartnerlogo),
                contentDescription = "Serveit Partner Logo",
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .widthIn(max = 120.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Title: "Enter OTP"
            Text(
                text = stringResource(R.string.otp_title),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle: "We've sent a 6-digit verification code to +91 XXXXXX1234"
            Text(
                text = stringResource(R.string.otp_subtitle_new, maskPhoneNumber(phoneNumber)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 6-Digit OTP Input
            OTPInputField(
                otpLength = 6,
                onOtpChange = { otp ->
                    viewModel.updateOtp(otp)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error message
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Verify Button
            PrimaryButton(
                text = if (uiState.isVerifying) stringResource(R.string.verifying) else stringResource(R.string.verify),
                onClick = {
                    if (uiState.isOtpValid && !uiState.isVerifying) {
                        viewModel.verifyOtp(activity)
                    }
                },
                enabled = uiState.isOtpValid && !uiState.isVerifying,
                isLoading = uiState.isVerifying,
                modifier = Modifier.fillMaxWidth()
            )

            // Retry button (shown on error with retries available)
            if (uiState.errorMessage != null && uiState.retryCount < uiState.maxRetries && !uiState.isVerifying) {
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.retryVerification(activity) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Retry (${uiState.maxRetries - uiState.retryCount} attempts left)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resend & Change Number Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Resend timer or resend button
                if (uiState.canResend) {
                    TextButton(
                        onClick = { viewModel.resendOtp(activity) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.resend_otp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.resend_code_in, formatTime(uiState.timeRemaining)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Change Number link
                TextButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.change_number),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Main Illustration (big_enter_otp)
            Image(
                painter = painterResource(id = R.drawable.big_enter_otp),
                contentDescription = "OTP Illustration",
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .widthIn(max = 280.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Masks phone number to show only last 4 digits
 * Example: "1234567890" -> "XXXXXX7890"
 */
private fun maskPhoneNumber(phoneNumber: String): String {
    return if (phoneNumber.length >= 4) {
        "XXXXXX${phoneNumber.takeLast(4)}"
    } else {
        "XXXXXX"
    }
}

/**
 * Formats seconds to MM:SS format
 * Example: 45 -> "00:45", 125 -> "02:05"
 */
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
