package com.nextserve.serveitpartnernew.ui.screen.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nextserve.serveitpartnernew.ui.onboarding.step5.ReviewScreen

@Composable
fun Step5Review(
    fullName: String,
    gender: String,
    email: String,
    primaryService: String,
    selectedMainService: String,
    selectedSubServices: List<String>,
    otherService: String,
    state: String,
    city: String,
    address: String,
    fullAddress: String,
    locationPincode: String,
    serviceRadius: Float,
    aadhaarFrontUploaded: Boolean,
    aadhaarBackUploaded: Boolean,
    profilePhotoUploaded: Boolean,
    isSubmitted: Boolean,
    verificationStatus: String? = null,
    rejectionReason: String? = null,
    submittedAt: Long? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onEditBasicInfo: () -> Unit,
    onEditServices: () -> Unit,
    onEditLocation: () -> Unit,
    onEditDocuments: () -> Unit,
    onSubmit: () -> Unit,
    onEditRejectedProfile: (() -> Unit)? = null,
    onContactSupport: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ReviewScreen(
        fullName = fullName,
        gender = gender,
        email = email,
        primaryService = primaryService,
        selectedMainService = selectedMainService,
        selectedSubServices = selectedSubServices,
        otherService = otherService,
        state = state,
        city = city,
        address = address,
        fullAddress = fullAddress,
        locationPincode = locationPincode,
        serviceRadius = serviceRadius,
        aadhaarFrontUploaded = aadhaarFrontUploaded,
        aadhaarBackUploaded = aadhaarBackUploaded,
        profilePhotoUploaded = profilePhotoUploaded,
        isSubmitted = isSubmitted,
        verificationStatus = verificationStatus,
        rejectionReason = rejectionReason,
        submittedAt = submittedAt,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onEditBasicInfo = onEditBasicInfo,
        onEditServices = onEditServices,
        onEditLocation = onEditLocation,
        onEditDocuments = onEditDocuments,
        onSubmit = onSubmit,
        onEditRejectedProfile = onEditRejectedProfile,
        onContactSupport = onContactSupport,
        onLogout = onLogout,
        modifier = modifier
    )
}