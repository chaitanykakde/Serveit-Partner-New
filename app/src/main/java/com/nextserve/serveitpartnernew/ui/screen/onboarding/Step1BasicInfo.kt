package com.nextserve.serveitpartnernew.ui.screen.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.data.model.MainService
import com.nextserve.serveitpartnernew.ui.components.ServiceSelector
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileMinimalTextField
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileSaveButton

/**
 * Step 1: Basic Information
 * Uses the exact same composables as Profile Edit screens for consistency.
 */
@Composable
fun Step1BasicInfo(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit,
    primaryService: String,
    onPrimaryServiceChange: (String) -> Unit,
    primaryServices: List<MainService>,
    isLoadingServices: Boolean = false,
    email: String,
    onEmailChange: (String) -> Unit,
    language: String = "en",
    onLanguageChange: (String) -> Unit = {},
    errorMessage: String? = null,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val genderOptions = listOf("Male", "Female")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = stringResource(R.string.onboarding_basic_info),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Subtitle
        Text(
            text = stringResource(R.string.onboarding_tell_about_yourself),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            color = colorScheme.onSurfaceVariant
        )

        // Full Name Field - Using ProfileMinimalTextField like ProfileEditBasicScreen
        ProfileMinimalTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            label = "Full Name",
            modifier = Modifier.fillMaxWidth()
        )

        // Email Address Field - Using ProfileMinimalTextField like ProfileEditBasicScreen
        ProfileMinimalTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email Address",
            keyboardType = KeyboardType.Email,
            modifier = Modifier.fillMaxWidth()
        )

        // Gender Radio Button Selector - Using exact same pattern as ProfileEditBasicScreen
        Column(modifier = Modifier.fillMaxWidth()) {
            // Label
            Text(
                text = "Gender".uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Radio buttons - exact same as ProfileEditBasicScreen
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                genderOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGenderChange(option) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = gender == option,
                            onClick = { onGenderChange(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Primary Service - Using ServiceSelector like ProfileEditServicesScreen
        if (isLoadingServices) {
            Text(
                text = stringResource(R.string.loading_services),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            ServiceSelector(
                selectedService = primaryService,
                services = primaryServices.map { it.name },
                onServiceSelected = onPrimaryServiceChange,
                label = stringResource(R.string.primary_service),
                placeholder = stringResource(R.string.select_primary_service),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Error message display
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = colorScheme.error,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Continue Button - Using ProfileSaveButton like all ProfileEdit screens
        ProfileSaveButton(
            text = "Continue",
            isLoading = false,
            enabled = true,
            onClick = onContinue,
            showTrailingArrow = true
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

