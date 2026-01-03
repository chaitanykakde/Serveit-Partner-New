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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.ErrorDisplay
import com.nextserve.serveitpartnernew.ui.components.OfflineIndicator
import com.nextserve.serveitpartnernew.ui.components.OTPInputField
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.screen.otp.OtpComponents
import com.nextserve.serveitpartnernew.ui.theme.OrangeAccent
import com.nextserve.serveitpartnernew.ui.util.Dimens
import com.nextserve.serveitpartnernew.ui.viewmodel.OtpViewModel
import com.nextserve.serveitpartnernew.utils.NetworkMonitor

/**
 * UI state for OTP screen to maintain compatibility.
 */
data class OtpUiState(
    val otp: String = "",
    val isOtpValid: Boolean = false,
    val phoneNumber: String = "",
    val verificationId: String? = null,
    val resendToken: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken? = null,
    val timeRemaining: Int = 60,
    val canResend: Boolean = false,
    val isVerifying: Boolean = false,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val isOffline: Boolean = false
)

@Composable
fun OtpScreen(
    phoneNumber: String,
    verificationId: String,
    authViewModel: com.nextserve.serveitpartnernew.ui.viewmodel.AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth >= 600.dp
    val scrollState = rememberScrollState()

    // Local OTP state
    var localOtp by remember { mutableStateOf("") }

    // Handle successful OTP verification navigation
    LaunchedEffect(authState) {
        when (authState) {
            is com.nextserve.serveitpartnernew.ui.viewmodel.AuthState.Authenticated,
            is com.nextserve.serveitpartnernew.ui.viewmodel.AuthState.Onboarding,
            is com.nextserve.serveitpartnernew.ui.viewmodel.AuthState.PendingApproval,
            is com.nextserve.serveitpartnernew.ui.viewmodel.AuthState.Rejected,
            com.nextserve.serveitpartnernew.ui.viewmodel.AuthState.Approved -> {
                // OTP verification successful - navigation will be handled by MainActivity
                // Just show success feedback here if needed
            }
            else -> {
                // Handle other states
            }
        }
    }

    // Create OtpUiState from AuthState for compatibility
    val uiState = remember(authState, localOtp) {
        val isOtpValid = localOtp.length == 6 && localOtp.all { it.isDigit() }
        when (authState) {
            is com.nextserve.serveitpartnernew.ui.viewmodel.AuthState.OtpSent -> OtpUiState(
                otp = localOtp,
                isOtpValid = isOtpValid,
                phoneNumber = phoneNumber,
                verificationId = verificationId,
                timeRemaining = authViewModel.getResendCooldownSeconds(),
                canResend = authViewModel.canResendOtp()
            )
            is com.nextserve.serveitpartnernew.ui.viewmodel.AuthState.Loading -> OtpUiState(
                otp = localOtp,
                isOtpValid = isOtpValid,
                phoneNumber = phoneNumber,
                verificationId = verificationId,
                isVerifying = true,
                timeRemaining = authViewModel.getResendCooldownSeconds(),
                canResend = authViewModel.canResendOtp()
            )
            is com.nextserve.serveitpartnernew.ui.viewmodel.AuthState.Error -> {
                val errorState = authState as com.nextserve.serveitpartnernew.ui.viewmodel.AuthState.Error
                OtpUiState(
                    otp = localOtp,
                    isOtpValid = isOtpValid,
                    phoneNumber = phoneNumber,
                    verificationId = verificationId,
                    errorMessage = errorState.message,
                    timeRemaining = authViewModel.getResendCooldownSeconds(),
                    canResend = authViewModel.canResendOtp()
                )
            }
            else -> OtpUiState(
                otp = localOtp,
                isOtpValid = isOtpValid,
                phoneNumber = phoneNumber,
                verificationId = verificationId,
                timeRemaining = authViewModel.getResendCooldownSeconds(),
                canResend = authViewModel.canResendOtp()
            )
        }
    }

    // Auto-submit when 6 digits are entered
    LaunchedEffect(localOtp) {
        if (localOtp.length == 6 && localOtp.all { it.isDigit() } &&
            authState !is com.nextserve.serveitpartnernew.ui.viewmodel.AuthState.Loading) {
            authViewModel.verifyOtp(localOtp)
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

            // Offline indicator (show when offline)
            val offlineCheckState = authState
            if (offlineCheckState is com.nextserve.serveitpartnernew.ui.viewmodel.AuthState.Error &&
                offlineCheckState.message.contains("internet", ignoreCase = true)) {
                OfflineIndicator(modifier = Modifier.padding(bottom = 16.dp))
            }

            // Logo
            OtpComponents.ServeitLogo()

            Spacer(modifier = Modifier.height(Dimens.spacingXxl))

            // Title - Centered
            Text(
                text = "Enter OTP Code",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spacingSm))

            // Subtitle - Centered
            Text(
                text = "A 6-digit verification code was just sent to",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spacingSm))

            // Phone Number Display with Wrong Number link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatPhoneNumber(phoneNumber),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(Dimens.spacingXs))
            
            TextButton(
                onClick = onNavigateBack,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(
                    text = "Wrong number?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXxl))

            // OTP Input Fields
            OTPInputField(
                otpLength = 6,
                onOtpChange = { otp ->
                    localOtp = otp
                    // Auto-submit when OTP is complete (6 digits)
                    if (otp.length == 6 && otp.all { it.isDigit() } && !uiState.isVerifying) {
                        authViewModel.verifyOtp(otp)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXl))

            // Error display for AuthState errors
            val errorDisplayState = authState
            if (errorDisplayState is com.nextserve.serveitpartnernew.ui.viewmodel.AuthState.Error) {
                ErrorDisplay(
                    error = com.nextserve.serveitpartnernew.ui.viewmodel.UiError(
                        message = errorDisplayState.message,
                        canRetry = errorDisplayState.canRetry
                    ),
                    onRetry = { authViewModel.clearError() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Dimens.spacingSm))
            }

            // Legacy error message display (for compatibility)
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.paddingXs),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                // Show retry count if applicable
                if (uiState.retryCount > 0 && uiState.retryCount < uiState.maxRetries) {
                    Spacer(modifier = Modifier.height(Dimens.spacingXs))
                    Text(
                        text = "Attempts remaining: ${uiState.maxRetries - uiState.retryCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.paddingXs),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.spacingSm))
            }

            // Verify Button
            PrimaryButton(
                text = if (uiState.isVerifying) stringResource(R.string.verifying) else stringResource(R.string.verify),
                onClick = {
                    // Allow manual verification even if not 6 digits (for testing)
                    if (localOtp.isNotEmpty() && localOtp.all { it.isDigit() } && !uiState.isVerifying) {
                        authViewModel.verifyOtp(localOtp)
                    }
                },
                enabled = localOtp.isNotEmpty() && localOtp.all { it.isDigit() } && !uiState.isVerifying,
                isLoading = uiState.isVerifying,
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

            Spacer(modifier = Modifier.height(Dimens.spacingLg))

            // Resend Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Didn't receive the OTP?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(Dimens.spacingSm))
                
                if (uiState.canResend) {
                    TextButton(
                        onClick = { authViewModel.resendOtp(activity) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.resend_otp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ri_time_line),
                            contentDescription = "Timer",
                            modifier = Modifier.size(16.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.width(Dimens.spacingXs))
                        Text(
                            text = "Resend code in ${formatTime(uiState.timeRemaining)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXxl))
        }
    }
}

/**
 * Formats phone number for display
 */
private fun formatPhoneNumber(phoneNumber: String): String {
    val cleaned = phoneNumber.replace(Regex("[^0-9]"), "")
    return if (cleaned.length == 10) {
        "+91 ${cleaned.substring(0, 5)} ${cleaned.substring(5)}"
    } else {
        "+91 $phoneNumber"
    }
}

/**
 * Formats seconds to MM:SS format
 */
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
