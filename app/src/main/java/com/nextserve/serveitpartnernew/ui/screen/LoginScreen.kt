package com.nextserve.serveitpartnernew.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.BottomStickyButtonContainer
import com.nextserve.serveitpartnernew.ui.components.OutlinedInputField
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.components.ScreenHeader
import com.nextserve.serveitpartnernew.ui.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onNavigateToOtp: (String, String) -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth >= 600.dp
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Setup callbacks
    LaunchedEffect(Unit) {
        viewModel.onOtpSent = { verificationId ->
            onNavigateToOtp(uiState.phoneNumber, verificationId)
        }
        viewModel.onAutoVerified = { credential ->
            // Auto-verification - navigate to OTP screen with empty verification ID
            // The credential will be handled in OtpViewModel via verifyWithCredential
            onNavigateToOtp(uiState.phoneNumber, "")
        }
        viewModel.onError = { error ->
            // Error shown in UI state
        }
    }

    BottomStickyButtonContainer(
        button = {
            PrimaryButton(
                text = if (uiState.isSendingOtp) "Sending..." else "Send OTP",
                onClick = {
                    if (uiState.isPhoneNumberValid && !uiState.isSendingOtp) {
                        viewModel.sendOtp(activity)
                    }
                },
                enabled = uiState.isPhoneNumberValid && !uiState.isSendingOtp,
                isLoading = uiState.isSendingOtp
            )
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
                    Spacer(modifier = Modifier.height(if (isTablet) 60.dp else 40.dp))

                    // Logo
                    Image(
                        painter = painterResource(id = R.drawable.serveitpartnerlogo),
                        contentDescription = "Serveit Partner Logo",
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .widthIn(max = 180.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // App Title
                    Text(
                        text = "Serveit Partner",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Subtitle
                    Text(
                        text = "Login to start earning",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // Phone Number Input
                    OutlinedInputField(
                        value = uiState.phoneNumber,
                        onValueChange = { newValue ->
                            viewModel.updatePhoneNumber(newValue)
                            if (uiState.errorMessage != null) {
                                viewModel.clearError()
                            }
                        },
                        label = "Mobile Number",
                        placeholder = "Enter mobile number",
                        leadingIcon = {
                            Text(
                                text = "+91",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                            )
                        },
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
                        isError = uiState.errorMessage != null,
                        errorMessage = uiState.errorMessage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Error message
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)
                        )
                    }

                    // Info text
                    Text(
                        text = "We'll send you a verification code",
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

