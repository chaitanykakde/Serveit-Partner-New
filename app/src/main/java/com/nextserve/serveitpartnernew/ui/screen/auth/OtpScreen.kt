package com.nextserve.serveitpartnernew.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.auth.login.*
import com.nextserve.serveitpartnernew.ui.auth.otp.*
import com.nextserve.serveitpartnernew.ui.components.ErrorDisplay
import com.nextserve.serveitpartnernew.ui.components.OTPInputField
import com.nextserve.serveitpartnernew.utils.PhoneNumberFormatter
import com.nextserve.serveitpartnernew.ui.viewmodel.AuthState
import com.nextserve.serveitpartnernew.ui.viewmodel.AuthViewModel

/**
 * OTP Verification Screen - Refactored UI
 * 
 * BUSINESS LOGIC PRESERVED:
 * - Observes authState.collectAsStateWithLifecycle()
 * - Uses existing OTPInputField component (preserves all OTP logic)
 * - Calls authViewModel.verifyOtp(otpCode)
 * - Navigation only when AuthState.Authenticated
 * - Resend logic via canResendOtp() and getResendCooldownSeconds()
 * - Auto-verification handling preserved
 * - Back navigation behavior preserved
 */
@Composable
fun OtpScreen(
    onVerificationSuccess: () -> Unit,
    onBackToPhone: () -> Unit,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    // Local UI state - survives recomposition
    var otpCode by remember { mutableStateOf("") }

    // PRESERVED: Get phone number, resend state, and cooldown
    val phoneNumber = authViewModel.getCurrentPhoneNumber()
    val canResend = authViewModel.canResendOtp()
    val resendCooldownSeconds = authViewModel.getResendCooldownSeconds()

    // React to state changes - PRESERVED BUSINESS LOGIC
    when (authState) {
        is AuthState.Authenticated -> {
            // Navigate to next screen ONLY when authentication succeeds - PRESERVED
            onVerificationSuccess()
        }
        else -> { /* Handle other states in UI */ }
    }

    val isVerifying = authState is AuthState.OtpVerifying
    
    // Format phone number for display
    val formattedPhoneNumber = phoneNumber?.let { PhoneNumberFormatter.formatPhoneNumber(it) } ?: ""

    // Premium layout with background - PRESERVED BUSINESS LOGIC
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Explicit background for dark theme
            .systemBarsPadding()
    ) {
        // Background with subtle gradients
        LoginBackground()
        
        // Top app bar - back button (fixed at top-left)
        Box(
            modifier = Modifier
                .padding(start = 24.dp, top = 48.dp, end = 24.dp) // px-6 pt-12 equivalent
                .align(Alignment.TopStart)
        ) {
            OtpBackButton(onClick = onBackToPhone) // PRESERVED: Back navigation
        }
        
        // Main content - centered vertically
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp) // px-8 equivalent
                .padding(top = 120.dp, bottom = 48.dp), // Space for back button + header
            horizontalAlignment = Alignment.Start, // Left align header (matching HTML)
            verticalArrangement = Arrangement.Center
        ) {
            // Header (Title + Subtitle with phone number) - below back arrow
            OtpHeader(
                title = "Verify OTP",
                subtitle = "Enter the 6-digit code sent to",
                phoneNumber = formattedPhoneNumber
            )
            
            Spacer(modifier = Modifier.height(32.dp)) // mb-8 equivalent
            
            // OTP Input - PRESERVED: Uses existing OTPInputField component (premium styled)
            // Center-aligned row of OTP boxes
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                OTPInputField(
                    otpLength = 6,
                    onOtpChange = { otp ->
                        // PRESERVED: Filter digits and limit to 6
                        otpCode = otp.filter { it.isDigit() }.take(6)
                    },
                    modifier = Modifier
                )
            }
            
            // Resend Timer - PRESERVED BUSINESS LOGIC
            OtpResendTimer(
                canResend = canResend,
                cooldownSeconds = resendCooldownSeconds,
                onResendClick = {
                    // PRESERVED: Exact same call
                    if (activity != null) {
                        authViewModel.resendOtp(activity)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp)) // pt-4 equivalent
            
            // Error Display - PRESERVED (only for verification errors)
            when (authState) {
                is AuthState.OtpVerificationError -> {
                    val errorState = authState as AuthState.OtpVerificationError
                    ErrorDisplay(
                        error = com.nextserve.serveitpartnernew.ui.viewmodel.UiError(
                            message = errorState.message,
                            canRetry = errorState.canRetry
                        ),
                        onRetry = { authViewModel.clearError() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                is AuthState.Error -> {
                    ErrorDisplay(
                        error = com.nextserve.serveitpartnernew.ui.viewmodel.UiError(
                            message = (authState as AuthState.Error).message,
                            canRetry = true
                        ),
                        onRetry = { authViewModel.clearError() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                else -> { /* No error to show */ }
            }
            
            // Verify Button - PRESERVED BUSINESS LOGIC
            PrimaryCtaButton(
                text = when (authState) {
                    is AuthState.OtpVerifying -> stringResource(R.string.verifying)
                    else -> "Verify & Continue" // Match HTML reference
                },
                onClick = {
                    // PRESERVED: Exact same condition and call
                    if (otpCode.length == 6 && activity != null) {
                        authViewModel.verifyOtp(otpCode)
                    }
                },
                enabled = otpCode.length == 6 &&
                         authState !is AuthState.OtpVerifying,
                isLoading = authState is AuthState.OtpVerifying,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Flexible spacer to push footer down
            Spacer(modifier = Modifier.weight(1f))
        }
        
        // Footer - fixed at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            OtpFooter()
        }
        
        // Loading overlay during verification - PRESERVED
        if (isVerifying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Verifying...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

