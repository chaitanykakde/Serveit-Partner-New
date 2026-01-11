package com.nextserve.serveitpartnernew.ui.screen.profile.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileEditScreenLayout
import com.nextserve.serveitpartnernew.ui.viewmodel.ProfileEditViewModel

@Composable
fun ProfileEditDocumentsScreen(
    navController: NavController,
    parentPaddingValues: PaddingValues = PaddingValues()
) {
    val uid = FirebaseProvider.auth.currentUser?.uid ?: return
    val context = LocalContext.current
    val viewModel: ProfileEditViewModel = viewModel(
        factory = ProfileEditViewModel.factory(context, uid)
    )
    val state = viewModel.uiState

    val frontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.uploadDocuments(frontUri = it, backUri = null)
        }
    }

    val backPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.uploadDocuments(frontUri = null, backUri = it)
        }
    }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            navController.popBackStack()
        }
    }

    val providerData = state.providerData
    val hasFrontDocument = !providerData?.aadhaarFrontUrl.isNullOrEmpty()
    val hasBackDocument = !providerData?.aadhaarBackUrl.isNullOrEmpty()

    ProfileEditScreenLayout(
        navController = navController,
        title = "Documents",
        subtitle = if (hasFrontDocument && hasBackDocument) "Your uploaded Aadhaar documents" else "Re-upload your Aadhaar if any detail changed",
        parentPaddingValues = parentPaddingValues
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Aadhaar Front
            if (hasFrontDocument) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        Text(
                            text = "AADHAAR FRONT",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = androidx.compose.ui.graphics.Color(0xFF9E9E9E),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        AsyncImage(
                            model = providerData?.aadhaarFrontUrl,
                            contentDescription = "Aadhaar Front",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            } else {
                // Show upload button only if document not uploaded
                Button(
                    onClick = { frontPicker.launch("image/*") },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (state.isSaving) "Uploading..." else "Upload Aadhaar Front",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Aadhaar Back
            if (hasBackDocument) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        Text(
                            text = "AADHAAR BACK",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = androidx.compose.ui.graphics.Color(0xFF9E9E9E),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        AsyncImage(
                            model = providerData?.aadhaarBackUrl,
                            contentDescription = "Aadhaar Back",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            } else {
                // Show upload button only if document not uploaded
                Button(
                    onClick = { backPicker.launch("image/*") },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (state.isSaving) "Uploading..." else "Upload Aadhaar Back",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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

