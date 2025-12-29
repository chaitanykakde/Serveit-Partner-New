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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.OTPInputField
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.screen.otp.OtpComponents
import com.nextserve.serveitpartnernew.ui.theme.OrangeAccent
import com.nextserve.serveitpartnernew.ui.util.Dimens
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
    
    val networkMonitor = remember { NetworkMonitor(context) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth >= 600.dp
    val scrollState = rememberScrollState()

    LaunchedEffect(phoneNumber, verificationId) {
        viewModel.setPhoneNumber(phoneNumber)
        viewModel.setActivity(activity) // Store activity for auto-submit
        if (verificationId.isNotEmpty()) {
            viewModel.setVerificationId(verificationId)
        }
        if (resendToken != null) {
            viewModel.setResendToken(resendToken)
        }
    }
    
    // Update activity reference when it changes
    LaunchedEffect(activity) {
        viewModel.setActivity(activity)
    }

    LaunchedEffect(Unit) {
        viewModel.onVerificationSuccess = { uid ->
            onNavigateToOnboarding(uid)
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
                    viewModel.updateOtp(otp)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXl))

            // Error message
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
                    if (uiState.isOtpValid && !uiState.isVerifying) {
                        viewModel.verifyOtp(activity)
                    }
                },
                enabled = uiState.isOtpValid && !uiState.isVerifying,
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
