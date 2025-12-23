package com.nextserve.serveitpartnernew.ui.screen.onboarding

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
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.ui.components.OutlinedInputField
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.components.SecondaryButton
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
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "Where do you provide services?",
            style = MaterialTheme.typography.headlineMedium,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        // Subtitle
        Text(
            text = "We'll use your location to find nearby service requests",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Use Current Location Button
        PrimaryButton(
            text = if (isLocationLoading) "Getting location..." else "Use Current Location",
            onClick = { onUseCurrentLocation(hasLocationPermission, requestLocationPermission) },
            isLoading = isLocationLoading,
            leadingIcon = {
                if (!isLocationLoading) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // State
        OutlinedInputField(
            value = state,
            onValueChange = onStateChange,
            label = "State",
            placeholder = "State",
            modifier = Modifier.fillMaxWidth()
        )

        // City
        OutlinedInputField(
            value = city,
            onValueChange = onCityChange,
            label = "City",
            placeholder = "City",
            modifier = Modifier.fillMaxWidth()
        )

        // Address
        OutlinedInputField(
            value = address,
            onValueChange = onAddressChange,
            label = "Area / Locality / Landmark",
            placeholder = "Area / locality / landmark",
            singleLine = false,
            modifier = Modifier.fillMaxWidth()
        )

        // Full Address
        OutlinedInputField(
            value = fullAddress,
            onValueChange = onFullAddressChange,
            label = "Full Address",
            placeholder = "Enter your complete address",
            singleLine = false,
            modifier = Modifier.fillMaxWidth()
        )

        // Pincode
        OutlinedInputField(
            value = locationPincode,
            onValueChange = onLocationPincodeChange,
            label = "Pincode",
            placeholder = "Pincode",
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
            modifier = Modifier.fillMaxWidth()
        )

        // Error message
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        // Helper text
        Text(
            text = "Auto-filled from your location. You can change if needed.",
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Service Radius
        Text(
            text = "You will get jobs within ${serviceRadius.toInt()} km",
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

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            SecondaryButton(
                text = "Previous",
                onClick = onPrevious,
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                text = "Next",
                onClick = onNext,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

