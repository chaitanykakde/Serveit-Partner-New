package com.nextserve.serveitpartnernew.ui.screen.auth

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
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.ErrorDisplay
import com.nextserve.serveitpartnernew.ui.components.OTPInputField
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.components.SecondaryButton
import com.nextserve.serveitpartnernew.ui.util.Dimens
import com.nextserve.serveitpartnernew.ui.viewmodel.AuthState
import com.nextserve.serveitpartnernew.ui.viewmodel.AuthViewModel
import com.nextserve.serveitpartnernew.utils.PhoneNumberFormatter

/**
 * OTP verification screen.
 * Pure UI - observes AuthState, triggers ViewModel events.
 * Explicit user intent - no auto-submit.
 */
@Composable
fun OtpVerificationScreen(
    onVerificationSuccess: () -> Unit,
    onBackToPhone: () -> Unit,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth >= 600.dp
    val scrollState = rememberScrollState()

    // Local UI state - survives recomposition
    var otpCode by remember { mutableStateOf("") }

    val phoneNumber = authViewModel.getCurrentPhoneNumber()
    val canResend = authViewModel.canResendOtp()
    val resendCooldownSeconds = authViewModel.getResendCooldownSeconds()

    // React to state changes - ONLY navigate when explicitly authenticated
    when (authState) {
        is AuthState.Authenticated -> {
            // Navigate to next screen ONLY when authentication succeeds
            onVerificationSuccess()
        }
        // Remove auto-navigation back to phone input - OTP screen should remain stable
        // during the entire OTP verification phase
        else -> { /* Handle other states in UI */ }
    }

    // Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.serveit_partner_flow_bg),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay
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
            Image(
                painter = painterResource(id = R.drawable.serveit_partner_logo_light),
                contentDescription = "Serveit Logo",
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXxl))

            // Title
            Text(
                text = "Enter OTP Code",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spacingSm))

            // Subtitle
            Text(
                text = "A 6-digit verification code was just sent to",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spacingSm))

            // Phone Number Display
            phoneNumber?.let { number ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = PhoneNumberFormatter.formatPhoneNumber(number),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXs))

            // Wrong number link
            TextButton(
                onClick = onBackToPhone,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(
                    text = "Wrong number?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXxl))

            // OTP Input
            OTPInputField(
                otpLength = 6,
                onOtpChange = { otp ->
                    otpCode = otp.filter { it.isDigit() }.take(6)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXl))

            // Error Display
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
                    Spacer(modifier = Modifier.height(Dimens.spacingSm))
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
                    Spacer(modifier = Modifier.height(Dimens.spacingSm))
                }
                else -> { /* No error to show */ }
            }

            // Verify Button
            PrimaryButton(
                text = when (authState) {
                    is AuthState.OtpVerifying -> stringResource(R.string.verifying)
                    else -> stringResource(R.string.verify)
                },
                onClick = {
                    if (otpCode.length == 6 && activity != null) {
                        authViewModel.verifyOtp(otpCode)
                    }
                },
                enabled = otpCode.length == 6 &&
                         authState !is AuthState.OtpVerifying,
                isLoading = authState is AuthState.OtpVerifying,
                modifier = Modifier.fillMaxWidth()
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

                if (canResend) {
                    TextButton(
                        onClick = {
                            if (activity != null) {
                                authViewModel.resendOtp(activity)
                            }
                        },
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
                            painter = painterResource(id = R.drawable.ri_time_line), // Update with actual icon
                            contentDescription = "Timer",
                            modifier = Modifier.size(16.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.padding(horizontal = Dimens.spacingXs))
                        Text(
                            text = "Resend code in ${resendCooldownSeconds}s",
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
