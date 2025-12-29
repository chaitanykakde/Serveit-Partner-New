package com.nextserve.serveitpartnernew.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.OutlinedInputField
import com.nextserve.serveitpartnernew.ui.components.ServiceSelector
import com.nextserve.serveitpartnernew.ui.util.Dimens

@Composable
fun Step1BasicInfo(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit,
    primaryService: String,
    onPrimaryServiceChange: (String) -> Unit,
    primaryServices: List<com.nextserve.serveitpartnernew.data.model.MainServiceModel>,
    isLoadingServices: Boolean = false,
    email: String,
    onEmailChange: (String) -> Unit,
    language: String = "en",
    onLanguageChange: (String) -> Unit = {},
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = Dimens.paddingLg),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Dimens.spacingMd)
    ) {
        Spacer(modifier = Modifier.height(Dimens.spacingMd))
        
        // Section Title - Large and Bold
        Text(
            text = stringResource(R.string.onboarding_basic_info),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(bottom = Dimens.spacingXs)
        )

        // Subtitle
        Text(
            text = stringResource(R.string.onboarding_tell_about_yourself),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Dimens.spacingLg)
        )

        // Full Name
        OutlinedInputField(
            value = fullName,
            onValueChange = onFullNameChange,
            label = stringResource(R.string.full_name),
            placeholder = stringResource(R.string.full_name),
            modifier = Modifier.fillMaxWidth()
        )

        // Gender
        Column(
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Dimens.spacingSm)
        ) {
            Text(
                text = stringResource(R.string.gender),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(bottom = Dimens.spacingXs)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Dimens.spacingMd)
            ) {
                val genders = listOf(
                    stringResource(R.string.male) to "Male",
                    stringResource(R.string.female) to "Female"
                )
                genders.forEach { (displayText, genderValue) ->
                    GenderButton(
                        text = displayText,
                        isSelected = gender == genderValue,
                        onClick = { onGenderChange(genderValue) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // Helper text for gender
            Text(
                text = "This helps us assign you relevant jobs",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.spacingXs)
            )
        }

        // Primary Service
        if (isLoadingServices) {
            Text(
                text = stringResource(R.string.loading_services),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Dimens.spacingMd)
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

        // Email (optional)
        OutlinedInputField(
            value = email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.email_optional),
            placeholder = stringResource(R.string.email_optional),
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Error message display
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(Dimens.spacingSm))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.paddingXs)
            )
        }

        Spacer(modifier = Modifier.height(Dimens.spacingXl))
    }
}

@Composable
private fun GenderButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .height(56.dp)
            .background(
                color = if (isSelected) {
                    colorScheme.primaryContainer.copy(alpha = 0.2f)
                } else {
                    colorScheme.surface
                },
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    colorScheme.primary
                } else {
                    colorScheme.outline.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) {
                colorScheme.primary
            } else {
                colorScheme.onSurface
            }
        )
    }
}

