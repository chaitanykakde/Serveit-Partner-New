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
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.OutlinedInputField
import com.nextserve.serveitpartnernew.ui.components.ServiceSelector
import com.nextserve.serveitpartnernew.utils.LanguageManager

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
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        // Section Title
        Text(
            text = stringResource(R.string.onboarding_basic_info),
            style = MaterialTheme.typography.headlineSmall,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        // Subtitle
        Text(
            text = stringResource(R.string.onboarding_tell_about_yourself),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Language Selector
        Text(
            text = stringResource(R.string.language),
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            val languages = listOf("en", "hi", "mr")
            languages.forEach { langCode ->
                LanguageButton(
                    languageCode = langCode,
                    displayName = LanguageManager.getLanguageDisplayName(langCode, context),
                    isSelected = language == langCode,
                    onClick = { onLanguageChange(langCode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Full Name
        OutlinedInputField(
            value = fullName,
            onValueChange = onFullNameChange,
            label = stringResource(R.string.full_name),
            placeholder = stringResource(R.string.full_name),
            modifier = Modifier.fillMaxWidth()
        )

        // Gender
        Text(
            text = stringResource(R.string.gender),
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
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

        // Primary Service
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

        // Email (optional)
        OutlinedInputField(
            value = email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.email_optional),
            placeholder = stringResource(R.string.email_optional),
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun LanguageButton(
    languageCode: String,
    displayName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                color = if (isSelected) {
                    colorScheme.primaryContainer
                } else {
                    colorScheme.surface
                },
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    colorScheme.primary
                } else {
                    colorScheme.outline.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) {
                colorScheme.onPrimaryContainer
            } else {
                colorScheme.onSurface
            }
        )
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
            .height(48.dp)
            .background(
                color = if (isSelected) {
                    colorScheme.primaryContainer
                } else {
                    colorScheme.surface
                },
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    colorScheme.primary
                } else {
                    colorScheme.outline.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) {
                colorScheme.onPrimaryContainer
            } else {
                colorScheme.onSurface
            }
        )
    }
}

