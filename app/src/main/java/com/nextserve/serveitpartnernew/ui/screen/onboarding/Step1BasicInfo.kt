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
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.ui.components.OutlinedInputField
import com.nextserve.serveitpartnernew.ui.components.ServiceSelector

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
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        // Section Title
        Text(
            text = "Basic Information",
            style = MaterialTheme.typography.headlineSmall,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        // Subtitle
        Text(
            text = "Tell us about yourself",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Full Name
        OutlinedInputField(
            value = fullName,
            onValueChange = onFullNameChange,
            label = "Full Name",
            placeholder = "Full Name",
            modifier = Modifier.fillMaxWidth()
        )

        // Gender
        Text(
            text = "Gender",
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            val genders = listOf("Male", "Female")
            genders.forEach { genderOption ->
                GenderButton(
                    text = genderOption,
                    isSelected = gender == genderOption,
                    onClick = { onGenderChange(genderOption) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Primary Service
        if (isLoadingServices) {
            Text(
                text = "Loading services...",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            ServiceSelector(
                selectedService = primaryService,
                services = primaryServices.map { it.name },
                onServiceSelected = onPrimaryServiceChange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Email (optional)
        OutlinedInputField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email (optional)",
            placeholder = "Email (optional)",
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
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

