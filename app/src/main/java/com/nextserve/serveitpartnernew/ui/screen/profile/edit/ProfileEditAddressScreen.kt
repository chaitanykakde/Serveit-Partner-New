package com.nextserve.serveitpartnernew.ui.screen.profile.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.ui.components.OutlinedInputField
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.utils.rememberLocationPermissionState
import com.nextserve.serveitpartnernew.ui.viewmodel.ProfileEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditAddressScreen(navController: NavController) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Address") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Update your service location",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PrimaryButton(
                text = "Use current location",
                leadingIcon = {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Location")
                },
                onClick = {
                    viewModel.useCurrentLocation { result ->
                        result.onSuccess { loc ->
                            stateText = loc.state
                            city = loc.city
                            address = loc.address
                            fullAddress = loc.fullAddress
                            pincode = loc.pincode
                            latitude = loc.latitude
                            longitude = loc.longitude
                        }
                    }.also {
                        if (!hasLocationPermission) requestLocationPermission()
                    }
                },
                enabled = !state.isSaving
            )

            OutlinedInputField(
                value = stateText,
                onValueChange = { stateText = it },
                label = "State",
                placeholder = "State"
            )

            OutlinedInputField(
                value = city,
                onValueChange = { city = it },
                label = "City",
                placeholder = "City"
            )

            OutlinedInputField(
                value = address,
                onValueChange = { address = it },
                label = "Area / Locality / Landmark",
                placeholder = "Area / locality / landmark",
                singleLine = false
            )

            OutlinedInputField(
                value = fullAddress,
                onValueChange = { fullAddress = it },
                label = "Full address",
                placeholder = "Enter full address",
                singleLine = false
            )

            OutlinedInputField(
                value = pincode,
                onValueChange = { pincode = it },
                label = "Pincode",
                placeholder = "Pincode"
            )

            Text(
                text = "Service radius: ${serviceRadius.toInt()} km",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Slider(
                value = serviceRadius,
                onValueChange = { serviceRadius = it },
                valueRange = 3f..10f,
                steps = 6,
                modifier = Modifier.fillMaxWidth()
            )

            PrimaryButton(
                text = if (state.isSaving) "Saving..." else "Save address",
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
                },
                isLoading = state.isSaving,
                enabled = !state.isSaving
            )

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

