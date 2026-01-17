package com.nextserve.serveitpartnernew.ui.screen.onboarding

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nextserve.serveitpartnernew.ui.onboarding.step4.VerificationScreen

@Composable
fun Step4Verification(
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
    VerificationScreen(
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
        onAadhaarFrontUpload = onAadhaarFrontUpload,
        onAadhaarBackUpload = onAadhaarBackUpload,
        onProfilePhotoUpload = onProfilePhotoUpload,
        onAadhaarFrontDelete = onAadhaarFrontDelete,
        onAadhaarBackDelete = onAadhaarBackDelete,
        onProfilePhotoDelete = onProfilePhotoDelete,
        onPrevious = onPrevious,
        onNext = onNext,
        modifier = modifier
    )
}