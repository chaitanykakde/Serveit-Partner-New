package com.nextserve.serveitpartnernew.ui.screen.profile.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileEditScreenLayout
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileMinimalTextField
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileSaveButton
import com.nextserve.serveitpartnernew.ui.utils.rememberLocationPermissionState
import com.nextserve.serveitpartnernew.ui.viewmodel.ProfileEditViewModel

@Composable
fun ProfileEditAddressScreen(
    navController: NavController,
    parentPaddingValues: PaddingValues = PaddingValues()
) {
    val uid = FirebaseProvider.auth.currentUser?.uid ?: return
    val context = LocalContext.current
    val viewModel: ProfileEditViewModel = viewModel(
        factory = ProfileEditViewModel.factory(context, uid)
    )
    val state = viewModel.uiState

    var stateText by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var fullAddress by rememberSaveable { mutableStateOf("") }
    var pincode by rememberSaveable { mutableStateOf("") }
    var serviceRadius by rememberSaveable { mutableStateOf(5f) }
    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }

    val (hasLocationPermission, requestLocationPermission) = rememberLocationPermissionState()

    LaunchedEffect(state.providerData) {
        state.providerData?.let {
            stateText = it.state
            city = it.city
            address = it.address
            fullAddress = it.fullAddress
            pincode = it.pincode
            serviceRadius = it.serviceRadius.toFloat()
            latitude = it.latitude
            longitude = it.longitude
        }
    }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            navController.popBackStack()
        }
    }

    ProfileEditScreenLayout(
        navController = navController,
        title = "Edit Address",
        subtitle = "Update your service location",
        parentPaddingValues = parentPaddingValues
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Use current location button - styled like ProfileSaveButton
            ProfileSaveButton(
                text = "Use current location",
                isLoading = false,
                enabled = !state.isSaving,
                leadingIcon = Icons.Default.LocationOn,
                showTrailingArrow = false,
                onClick = {
                    viewModel.useCurrentLocation { result ->
                        result.onSuccess { locationData ->
                            stateText = locationData.state ?: ""
                            city = locationData.city ?: ""
                            address = locationData.address ?: ""
                            fullAddress = locationData.fullAddress ?: ""
                            pincode = locationData.pincode ?: ""
                            latitude = locationData.latitude
                            longitude = locationData.longitude
                        }
                    }.also {
                        if (!hasLocationPermission) requestLocationPermission()
                    }
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ProfileMinimalTextField(
                value = stateText,
                onValueChange = { stateText = it },
                label = "State",
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ProfileMinimalTextField(
                value = city,
                onValueChange = { city = it },
                label = "City",
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ProfileMinimalTextField(
                value = address,
                onValueChange = { address = it },
                label = "Area / Locality / Landmark",
                singleLine = false,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ProfileMinimalTextField(
                value = fullAddress,
                onValueChange = { fullAddress = it },
                label = "Full address",
                singleLine = false,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ProfileMinimalTextField(
                value = pincode,
                onValueChange = { pincode = it },
                label = "Pincode",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Service radius: ${serviceRadius.toInt()} km",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Slider(
                value = serviceRadius,
                onValueChange = { serviceRadius = it },
                valueRange = 3f..10f,
                steps = 6,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            ProfileSaveButton(
                text = if (state.isSaving) "Saving..." else "Save address",
                isLoading = state.isSaving,
                enabled = !state.isSaving,
                onClick = {
                    viewModel.updateAddress(
                        state = stateText,
                        city = city,
                        address = address,
                        fullAddress = fullAddress,
                        pincode = pincode,
                        serviceRadius = serviceRadius.toDouble(),
                        latitude = latitude,
                        longitude = longitude
                    )
                }
            )

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

