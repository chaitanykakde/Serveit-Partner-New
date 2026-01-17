package com.nextserve.serveitpartnernew.ui.onboarding.step4

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun VerificationScreen(
    // Document states
    aadhaarFrontUploaded: Boolean,
    aadhaarBackUploaded: Boolean,
    aadhaarFrontUrl: String = "",
    aadhaarBackUrl: String = "",
    profilePhotoUploaded: Boolean = false,
    profilePhotoUrl: String = "",
    isUploading: Boolean = false,
    uploadProgress: Float = 0f,
    uploadingDocumentType: String? = null,
    errorMessage: String? = null,

    // Callbacks
    onAadhaarFrontUpload: (Uri) -> Unit,
    onAadhaarBackUpload: (Uri) -> Unit,
    onProfilePhotoUpload: (Uri) -> Unit,
    onAadhaarFrontDelete: () -> Unit = {},
    onAadhaarBackDelete: () -> Unit = {},
    onProfilePhotoDelete: () -> Unit = {},
    onPrevious: () -> Unit,
    onNext: () -> Unit,

    modifier: Modifier = Modifier
) {
    // Image pickers
    val frontImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAadhaarFrontUpload(it) }
    }

    val backImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAadhaarBackUpload(it) }
    }

    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onProfilePhotoUpload(it) }
    }

    VerificationContent(
        aadhaarFrontUploaded = aadhaarFrontUploaded,
        aadhaarBackUploaded = aadhaarBackUploaded,
        aadhaarFrontUrl = aadhaarFrontUrl,
        aadhaarBackUrl = aadhaarBackUrl,
        profilePhotoUploaded = profilePhotoUploaded,
        profilePhotoUrl = profilePhotoUrl,
        isUploading = isUploading,
        uploadProgress = uploadProgress,
        uploadingDocumentType = uploadingDocumentType,
        errorMessage = errorMessage,
        onAadhaarFrontUpload = { frontImagePicker.launch("image/*") },
        onAadhaarBackUpload = { backImagePicker.launch("image/*") },
        onAadhaarFrontReplace = { frontImagePicker.launch("image/*") },
        onAadhaarBackReplace = { backImagePicker.launch("image/*") },
        onAadhaarFrontRemove = onAadhaarFrontDelete,
        onAadhaarBackRemove = onAadhaarBackDelete,
        onProfilePhotoUpload = { profilePhotoPicker.launch("image/*") },
        onProfilePhotoReplace = { profilePhotoPicker.launch("image/*") },
        onProfilePhotoRemove = onProfilePhotoDelete,
        onPrevious = onPrevious,
        onNext = onNext,
        modifier = modifier
    )
}
