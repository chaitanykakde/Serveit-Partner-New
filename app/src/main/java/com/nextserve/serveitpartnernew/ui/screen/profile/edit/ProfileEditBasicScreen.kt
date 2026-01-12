package com.nextserve.serveitpartnernew.ui.screen.profile.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileEditScreenLayout
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileMinimalTextField
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileSaveButton
import com.nextserve.serveitpartnernew.ui.viewmodel.ProfileEditViewModel

@Composable
fun ProfileEditBasicScreen(
    navController: NavController,
    parentPaddingValues: PaddingValues = PaddingValues()
) {
    val uid = FirebaseProvider.auth.currentUser?.uid ?: return
    val context = LocalContext.current
    val viewModel: ProfileEditViewModel = viewModel(
        factory = ProfileEditViewModel.factory(context, uid)
    )
    val state = viewModel.uiState

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }

    val genderOptions = listOf("Male", "Female")

    LaunchedEffect(state.providerData) {
        state.providerData?.let {
            fullName = it.fullName
            email = it.email
            // Normalize gender value to match options (handle lowercase from database)
            gender = when (it.gender.lowercase()) {
                "male" -> "Male"
                "female" -> "Female"
                else -> it.gender.takeIf { g -> g in genderOptions } ?: ""
            }
        }
    }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            navController.popBackStack()
        }
    }

    ProfileEditScreenLayout(
        navController = navController,
        title = "Edit Basic Info",
        subtitle = "Update your personal details",
        parentPaddingValues = parentPaddingValues
    ) {
        // Full Name Field
        ProfileMinimalTextField(
                value = fullName,
                onValueChange = { fullName = it },
            label = "Full Name",
            modifier = Modifier.padding(bottom = 24.dp)
            )

        // Email Address Field
        ProfileMinimalTextField(
                value = email,
                onValueChange = { email = it },
            label = "Email Address",
            keyboardType = KeyboardType.Email,
            modifier = Modifier.padding(bottom = 24.dp)
            )

        // Gender Radio Button Selector
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            // Label
            Text(
                text = "Gender".uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Radio buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                genderOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { gender = option },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = gender == option,
                            onClick = { gender = option },
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

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                color = Color(0xFFD32F2F), // Error red
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                )
            }

        // Save Changes Button - immediately after Gender field
        ProfileSaveButton(
            isLoading = state.isSaving,
            enabled = !state.isSaving,
            onClick = { viewModel.updateBasicInfo(fullName, email, gender) }
        )
    }
}
