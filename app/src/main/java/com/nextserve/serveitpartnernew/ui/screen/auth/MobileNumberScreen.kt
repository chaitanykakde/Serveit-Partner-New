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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.ErrorDisplay
import com.nextserve.serveitpartnernew.ui.components.OutlinedInputField
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.theme.OrangeAccent
import com.nextserve.serveitpartnernew.ui.util.Dimens
import com.nextserve.serveitpartnernew.ui.viewmodel.AuthState
import com.nextserve.serveitpartnernew.ui.viewmodel.AuthViewModel

/**
 * Mobile number input screen.
 * Pure UI - observes AuthState, triggers ViewModel events.
 */
@Composable
fun MobileNumberScreen(
    onOtpRequested: () -> Unit,
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
    var phoneNumber by remember { mutableStateOf("") }

    // React to state changes
    when (authState) {
        is AuthState.OtpSent -> {
            // Navigate to OTP screen
            onOtpRequested()
        }
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
                text = "Welcome back",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spacingSm))

            // Subtitle
            Text(
                text = "Login using your registered mobile number",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXxl))

            // Phone Number Input
            PhoneNumberInput(
                phoneNumber = phoneNumber,
                onPhoneNumberChange = { newValue ->
                    phoneNumber = newValue.filter { it.isDigit() }.take(10)
                    authViewModel.validatePhoneNumber(newValue)
                },
                isError = authState is AuthState.PhoneError,
                errorMessage = (authState as? AuthState.PhoneError)?.message,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXl))

            // Send OTP Button
            PrimaryButton(
                text = when (authState) {
                    is AuthState.OtpSending -> stringResource(R.string.sending)
                    else -> stringResource(R.string.send_otp)
                },
                onClick = {
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

            Spacer(modifier = Modifier.height(Dimens.spacingSm))

            // Helper Text
            Text(
                text = "You'll receive a 6-digit verification code on this number",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Error Display
            when (authState) {
                is AuthState.OtpSendError -> {
                    Spacer(modifier = Modifier.height(Dimens.spacingSm))
                    ErrorDisplay(
                        error = com.nextserve.serveitpartnernew.ui.viewmodel.UiError(
                            message = (authState as AuthState.OtpSendError).message,
                            canRetry = (authState as AuthState.OtpSendError).canRetry
                        ),
                        onRetry = { authViewModel.clearError() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is AuthState.PhoneError -> {
                    Spacer(modifier = Modifier.height(Dimens.spacingSm))
                    ErrorDisplay(
                        error = com.nextserve.serveitpartnernew.ui.viewmodel.UiError(
                            message = (authState as AuthState.PhoneError).message,
                            canRetry = false
                        ),
                        onRetry = { authViewModel.clearError() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> { /* No error to show */ }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.spacingXl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "By continuing, you agree to our",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

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

/**
 * Phone number input with +91 prefix.
 */
@Composable
private fun PhoneNumberInput(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.mobile_number),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = Dimens.spacingXs)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // +91 Prefix
            Text(
                text = "+91",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = Dimens.spacingSm)
            )

            // Phone Input
            OutlinedInputField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                placeholder = "Enter 10-digit number",
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.weight(1f),
                isError = isError,
                errorMessage = errorMessage
            )
        }
    }
}
