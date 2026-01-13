package com.nextserve.serveitpartnernew.ui.viewmodel

import com.nextserve.serveitpartnernew.data.model.MainService
import com.nextserve.serveitpartnernew.domain.onboarding.OnboardingStatus
import com.nextserve.serveitpartnernew.domain.onboarding.OnboardingStep

/**
 * UI state for onboarding flow.
 * Uses domain models instead of magic strings/numbers.
 */
data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.BASIC_INFO,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val onboardingStatus: OnboardingStatus = OnboardingStatus.NOT_STARTED,

    // Step 1 data
    val fullName: String = "",
    val gender: String = "",
    val primaryService: String = "",
    val email: String = "",
    val language: String = "en",
    val mainServices: List<MainService> = emptyList(),
    val isLoadingServices: Boolean = false,

    // Step 2 data
    val selectedMainService: String = "",
    val isInSubServiceView: Boolean = false,
    val availableSubServices: List<String> = emptyList(),
    val selectedSubServices: Set<String> = emptySet(),
    val isSelectAllChecked: Boolean = true,
    val otherService: String = "",
    val isLoadingSubServices: Boolean = false,

    // Step 3 data
    val state: String = "",
    val city: String = "",
    val address: String = "",
    val fullAddress: String = "",
    val locationPincode: String = "",
    val serviceRadius: Float = 5f,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isLocationLoading: Boolean = false,

    // Step 4 data
    val aadhaarFrontUrl: String = "",
    val aadhaarBackUrl: String = "",
    val aadhaarFrontUploaded: Boolean = false,
    val aadhaarBackUploaded: Boolean = false,
    val profilePhotoUrl: String = "",
    val profilePhotoUploaded: Boolean = false,
    val uploadProgress: Float = 0f,
    val isUploading: Boolean = false,

    // Step 5 data
    val isSubmitted: Boolean = false
)
