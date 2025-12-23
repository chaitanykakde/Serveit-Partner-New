package com.nextserve.serveitpartnernew.ui.screen

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.BottomStickyButtonContainer
import com.nextserve.serveitpartnernew.ui.components.OTPInputField
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.components.ScreenHeader
import com.nextserve.serveitpartnernew.ui.viewmodel.OtpViewModel

@Composable
fun OtpScreen(
    phoneNumber: String,
    verificationId: String,
    onNavigateToOnboarding: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: OtpViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth >= 600.dp
    val scrollState = rememberScrollState()

    // Initialize phone number and verification ID
    LaunchedEffect(phoneNumber, verificationId) {
        viewModel.setPhoneNumber(phoneNumber)
        if (verificationId.isNotEmpty()) {
            viewModel.setVerificationId(verificationId)
        }
    }

    // Setup verification success callback
    LaunchedEffect(Unit) {
        viewModel.onVerificationSuccess = { uid ->
            onNavigateToOnboarding(uid)
        }
    }

    BottomStickyButtonContainer(
        button = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                PrimaryButton(
                    text = if (uiState.isVerifying) "Verifying..." else "Verify OTP",
                    onClick = {
                        if (uiState.isOtpValid && !uiState.isVerifying) {
                            viewModel.verifyOtp(activity)
                        }
                    },
                    enabled = uiState.isOtpValid && !uiState.isVerifying,
                    isLoading = uiState.isVerifying
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Resend OTP button
                TextButton(
                    onClick = { viewModel.resendOtp(activity) },
                    enabled = uiState.canResend,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.canResend) {
                        Text(
                            text = "Resend OTP",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "Resend OTP in ${uiState.timeRemaining}s",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                    Spacer(modifier = Modifier.height(if (isTablet) 40.dp else 32.dp))

                    // Smaller Logo
                    Image(
                        painter = painterResource(id = R.drawable.serveitpartnerlogo),
                        contentDescription = "Serveit Partner Logo",
                        modifier = Modifier
                            .size(120.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ScreenHeader(
                        title = "Verify OTP",
                        subtitle = "OTP sent to +91 ${maskPhoneNumber(phoneNumber)}"
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // OTP Input
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
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
                        )
                    }

                    // Info text
                    Text(
                        text = "Enter the 6-digit code sent to your mobile number",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    )
}

private fun maskPhoneNumber(phoneNumber: String): String {
    return if (phoneNumber.length >= 4) {
        "XXXXXX${phoneNumber.takeLast(4)}"
    } else {
        "XXXXXX"
    }
}

