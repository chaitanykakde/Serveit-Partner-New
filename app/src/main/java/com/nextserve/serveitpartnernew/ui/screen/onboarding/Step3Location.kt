package com.nextserve.serveitpartnernew.ui.screen.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileMinimalTextField
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileSaveButton
import com.nextserve.serveitpartnernew.ui.utils.rememberLocationPermissionState

@Composable
fun Step3Location(
    state: String,
    onStateChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    fullAddress: String,
    onFullAddressChange: (String) -> Unit,
    locationPincode: String,
    onLocationPincodeChange: (String) -> Unit,
    serviceRadius: Float,
    onServiceRadiusChange: (Float) -> Unit,
    isLocationLoading: Boolean,
    errorMessage: String? = null,
    onUseCurrentLocation: (Boolean, () -> Unit) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val (hasLocationPermission, requestLocationPermission) = rememberLocationPermissionState()

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
            text = stringResource(R.string.where_provide_services),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Subtitle
        Text(
            text = stringResource(R.string.location_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            color = colorScheme.onSurfaceVariant
        )

        // Use Current Location Button
        ProfileSaveButton(
            text = if (isLocationLoading) "Getting location..." else "Use current location",
            onClick = { onUseCurrentLocation(hasLocationPermission, requestLocationPermission) },
            isLoading = isLocationLoading,
            leadingIcon = Icons.Default.LocationOn,
            showTrailingArrow = false,
            modifier = Modifier.fillMaxWidth()
        )

        // State
        ProfileMinimalTextField(
            value = state,
            onValueChange = onStateChange,
            label = "State",
            modifier = Modifier.fillMaxWidth()
        )

        // City
        ProfileMinimalTextField(
            value = city,
            onValueChange = onCityChange,
            label = "City",
            modifier = Modifier.fillMaxWidth()
        )

        // Address
        ProfileMinimalTextField(
            value = address,
            onValueChange = onAddressChange,
            label = "Area / Locality / Landmark",
            modifier = Modifier.fillMaxWidth()
        )

        // Full Address
        ProfileMinimalTextField(
            value = fullAddress,
            onValueChange = onFullAddressChange,
            label = "Full Address",
            modifier = Modifier.fillMaxWidth()
        )

        // Pincode
        ProfileMinimalTextField(
            value = locationPincode,
            onValueChange = onLocationPincodeChange,
            label = "Pincode",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.fillMaxWidth()
        )

        // Service Radius
        Text(
            text = "Service radius: ${serviceRadius.toInt()} km",
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Slider(
            value = serviceRadius,
            onValueChange = onServiceRadiusChange,
            valueRange = 3f..10f,
            steps = 6,
            modifier = Modifier.fillMaxWidth()
        )

        // Helper text
        Text(
            text = "Your address will be auto-filled when using current location",
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )

        // Error message
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color(0xFFD32F2F), // Error red - same as Step 1
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Navigation Buttons - Previous & Next side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileSaveButton(
                text = "Previous",
                onClick = onPrevious,
                modifier = Modifier.weight(1f),
                showTrailingArrow = false
            )
            ProfileSaveButton(
                text = "Next",
                onClick = onNext,
                modifier = Modifier.weight(1f),
                showTrailingArrow = true
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

