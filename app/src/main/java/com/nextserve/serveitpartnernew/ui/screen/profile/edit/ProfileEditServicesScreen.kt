package com.nextserve.serveitpartnernew.ui.screen.profile.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.ui.components.ServiceSelector
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileEditScreenLayout
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileSaveButton
import com.nextserve.serveitpartnernew.ui.viewmodel.ProfileEditViewModel

@Composable
fun ProfileEditServicesScreen(
    navController: NavController,
    parentPaddingValues: PaddingValues = PaddingValues()
) {
    val uid = FirebaseProvider.auth.currentUser?.uid ?: return
    val context = LocalContext.current
    val viewModel: ProfileEditViewModel = viewModel(
        factory = ProfileEditViewModel.factory(context, uid)
    )
    val state = viewModel.uiState

    var gender by rememberSaveable { mutableStateOf("male") }
    var mainService by rememberSaveable { mutableStateOf("") }
    var selectedSubs by rememberSaveable { mutableStateOf(setOf<String>()) }

    LaunchedEffect(state.providerData) {
        state.providerData?.let {
            gender = if (it.gender.isNotEmpty()) it.gender else gender
            mainService = it.selectedMainService
            selectedSubs = it.selectedSubServices.toSet()
            viewModel.loadMainServices(gender)
            if (mainService.isNotEmpty()) {
                viewModel.loadSubServices(gender, mainService)
            }
        }
    }

    LaunchedEffect(state.mainServices) {
        if (mainService.isEmpty() && state.mainServices.isNotEmpty()) {
            mainService = state.mainServices.first().name
            viewModel.loadSubServices(gender, mainService)
        }
    }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            navController.popBackStack()
        }
    }

    ProfileEditScreenLayout(
        navController = navController,
        title = "Edit Services",
        subtitle = "Update the services you offer",
        parentPaddingValues = parentPaddingValues
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            ServiceSelector(
                selectedService = mainService,
                services = state.mainServices.map { it.name },
                onServiceSelected = {
                    mainService = it
                    viewModel.loadSubServices(gender, it)
                    selectedSubs = emptySet()
                },
                label = "Primary service"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Sub-services",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (state.subServices.isEmpty()) {
                    Text(
                        text = "Select a primary service to see sub-services",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.subServices.forEach { subServiceName ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = subServiceName, fontWeight = FontWeight.Medium)
                            }
                            Checkbox(
                                checked = selectedSubs.contains(subServiceName),
                                onCheckedChange = { checked ->
                                    selectedSubs = if (checked) {
                                        selectedSubs + subServiceName
                                    } else {
                                        selectedSubs - subServiceName
                                    }
                                }
                            )
                        }
                    }
                }
            }

            ProfileSaveButton(
                text = if (state.isSaving) "Saving..." else "Save services",
                isLoading = state.isSaving,
                enabled = !state.isSaving && mainService.isNotEmpty(),
                onClick = {
                    viewModel.updateServices(
                        gender = gender,
                        mainService = mainService,
                        subServices = selectedSubs.toList()
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

