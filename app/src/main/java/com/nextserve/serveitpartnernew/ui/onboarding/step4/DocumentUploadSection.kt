package com.nextserve.serveitpartnernew.ui.onboarding.step4

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DocumentUploadSection(
    aadhaarFrontUploaded: Boolean,
    aadhaarBackUploaded: Boolean,
    aadhaarFrontUrl: String = "",
    aadhaarBackUrl: String = "",
    profilePhotoUploaded: Boolean = false,
    profilePhotoUrl: String = "",
    isUploading: Boolean = false,
    onAadhaarFrontUpload: () -> Unit,
    onAadhaarBackUpload: () -> Unit,
    onAadhaarFrontReplace: () -> Unit,
    onAadhaarBackReplace: () -> Unit,
    onAadhaarFrontRemove: () -> Unit,
    onAadhaarBackRemove: () -> Unit,
    onProfilePhotoUpload: () -> Unit,
    onProfilePhotoReplace: () -> Unit,
    onProfilePhotoRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Aadhaar Card (Front)
        DocumentUploadCard(
            title = "Aadhaar Card (Front)",
            subtitle = "Clear photo of front side",
            icon = Icons.Default.Info,
            isUploaded = aadhaarFrontUploaded,
            imageUrl = aadhaarFrontUrl,
            isUploading = isUploading,
            onUpload = onAadhaarFrontUpload,
            onReplace = onAadhaarFrontReplace,
            onRemove = onAadhaarFrontRemove
        )

        // Aadhaar Card (Back)
        DocumentUploadCard(
            title = "Aadhaar Card (Back)",
            subtitle = "Clear photo of back side",
            icon = Icons.Default.Person,
            isUploaded = aadhaarBackUploaded,
            imageUrl = aadhaarBackUrl,
            isUploading = isUploading,
            onUpload = onAadhaarBackUpload,
            onReplace = onAadhaarBackReplace,
            onRemove = onAadhaarBackRemove
        )

        // Profile Photo
        DocumentUploadCard(
            title = "Profile Photo",
            subtitle = "Clear photo of your face",
            icon = Icons.Default.AccountCircle,
            isUploaded = profilePhotoUploaded,
            imageUrl = profilePhotoUrl,
            isUploading = isUploading,
            onUpload = onProfilePhotoUpload,
            onReplace = onProfilePhotoReplace,
            onRemove = onProfilePhotoRemove
        )
    }
}
