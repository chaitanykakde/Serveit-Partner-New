package com.nextserve.serveitpartnernew.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.auth.login.*
import com.nextserve.serveitpartnernew.ui.components.ErrorDisplay
import com.nextserve.serveitpartnernew.ui.viewmodel.AuthState
import com.nextserve.serveitpartnernew.ui.viewmodel.AuthViewModel

/**
 * Login (Mobile Number) Screen - Refactored UI
 * 
 * BUSINESS LOGIC PRESERVED:
 * - Observes authState.collectAsStateWithLifecycle()
 * - Button enabled when phoneNumber.length == 10
 * - Calls authViewModel.sendOtp(phoneNumber, activity)
 * - Navigation only when AuthState.OtpSent
 * - All validation and error handling unchanged
 */
@Composable
fun LoginScreen(
    onOtpRequested: () -> Unit,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    // Local UI state - survives recomposition
    var phoneNumber by remember { mutableStateOf("") }
    // Track user interaction to delay error display
    var hasUserInteracted by remember { mutableStateOf(false) }

    // React to state changes - PRESERVED BUSINESS LOGIC
    when (authState) {
        is AuthState.OtpSent -> {
            // Navigate to OTP screen - PRESERVED
            onOtpRequested()
        }
        else -> { /* Handle other states in UI */ }
    }

    // Premium layout with background - PRESERVED BUSINESS LOGIC
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Explicit background for dark theme
            .systemBarsPadding()
    ) {
        // Background with subtle gradients
        LoginBackground()
        
        // Main content - centered vertically
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp) // px-8 equivalent
                .padding(bottom = 80.dp), // pb-20 equivalent
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header (Logo + Title + Subtitle)
            LoginHeader(
                title = "Welcome back",
                subtitle = "Please enter your details to continue"
            )
            
            Spacer(modifier = Modifier.height(48.dp)) // mb-8 equivalent
            
            // Phone Number Input - PRESERVED BUSINESS LOGIC
            // Show error only after user interaction AND invalid input
            val showError = hasUserInteracted &&
                    phoneNumber.isNotEmpty() &&
                    phoneNumber.length < 10 &&
                    authState is AuthState.PhoneError
            
            PhoneNumberField(
                value = phoneNumber,
                onValueChange = { newValue ->
                    // Mark user interaction on first input
                    if (!hasUserInteracted && newValue.isNotEmpty()) {
                        hasUserInteracted = true
                    }
                    // PRESERVED: Filter digits and limit to 10
                    phoneNumber = newValue.filter { it.isDigit() }.take(10)
                    authViewModel.validatePhoneNumber(newValue)
                },
                enabled = authState !is AuthState.OtpSending,
                isError = showError,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Error text below field (only when error should be shown)
            if (showError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please enter a valid 10-digit phone number",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
            
            // Send OTP Button - PRESERVED BUSINESS LOGIC
            PrimaryCtaButton(
                text = when (authState) {
                    is AuthState.OtpSending -> stringResource(R.string.sending)
                    else -> "Get Verification Code" // Match HTML reference
                },
                onClick = {
                    // PRESERVED: Exact same condition and call
                    if (phoneNumber.length == 10 && activity != null) {
                        authViewModel.sendOtp(phoneNumber, activity)
                    }
                },
                enabled = phoneNumber.length == 10 &&
                         authState !is AuthState.OtpSending &&
                         authState !is AuthState.PhoneError,
                isLoading = authState is AuthState.OtpSending,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Error Display - PRESERVED (only for OTP send errors, not phone validation)
            when (authState) {
                is AuthState.OtpSendError -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    ErrorDisplay(
                        error = com.nextserve.serveitpartnernew.ui.viewmodel.UiError(
                            message = (authState as AuthState.OtpSendError).message,
                            canRetry = (authState as AuthState.OtpSendError).canRetry
                        ),
                        onRetry = { authViewModel.clearError() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // PhoneError is shown inline above, not here (avoid duplicate)
                else -> { /* No error to show */ }
            }
            
            // Flexible spacer to push footer down
            Spacer(modifier = Modifier.weight(1f))
        }
        
        // Footer - fixed at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            LoginFooter()
        }
    }
}

