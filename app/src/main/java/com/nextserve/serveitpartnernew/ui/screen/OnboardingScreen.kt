package com.nextserve.serveitpartnernew.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.domain.onboarding.OnboardingStep
import com.nextserve.serveitpartnernew.ui.components.OnboardingStepIndicator
import com.nextserve.serveitpartnernew.ui.screen.onboarding.Step1BasicInfo
import com.nextserve.serveitpartnernew.ui.screen.onboarding.Step2ServiceSelection
import com.nextserve.serveitpartnernew.ui.screen.onboarding.Step3Location
import com.nextserve.serveitpartnernew.ui.screen.onboarding.Step4Verification
import com.nextserve.serveitpartnernew.ui.screen.onboarding.Step5Review
import com.nextserve.serveitpartnernew.ui.utils.rememberLocationPermissionState
import com.nextserve.serveitpartnernew.ui.viewmodel.OnboardingUiState
import com.nextserve.serveitpartnernew.ui.viewmodel.OnboardingViewModel

/**
 * Pure UI composable for onboarding flow.
 * Only emits events, no Firebase logic or business logic.
 */
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onUpdateFullName: (String) -> Unit,
    onUpdateGender: (String) -> Unit,
    onUpdatePrimaryService: (String) -> Unit,
    onUpdateEmail: (String) -> Unit,
    onUpdateLanguage: (String) -> Unit,
    onUpdateSelectedMainService: (String) -> Unit,
    onToggleSubService: (String) -> Unit,
    onToggleSelectAll: () -> Unit,
    onUpdateOtherService: (String) -> Unit,
    onLoadSubServices: (String, String) -> Unit,
    onUpdateState: (String) -> Unit,
    onUpdateCity: (String) -> Unit,
    onUpdateAddress: (String) -> Unit,
    onUpdateFullAddress: (String) -> Unit,
    onUpdateLocationPincode: (String) -> Unit,
    onUpdateServiceRadius: (Float) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onUploadAadhaarFront: (ByteArray) -> Unit,
    onUploadAadhaarBack: (ByteArray) -> Unit,
    onUploadProfilePhoto: (ByteArray) -> Unit,
    onDeleteAadhaarFront: () -> Unit,
    onDeleteAadhaarBack: () -> Unit,
    onDeleteProfilePhoto: () -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onNavigateToStep: (Int) -> Unit,
    onSubmit: () -> Unit,
    onReset: () -> Unit,
    onEditRejectedProfile: () -> Unit,
    onContactSupport: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth >= 600.dp
    val (hasLocationPermission, requestLocationPermission) = rememberLocationPermissionState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Helper function to convert URI to ByteArray immediately
    fun uriToByteArray(uri: android.net.Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            null
        }
    }

    // Wrapper functions that convert URI to ByteArray before calling viewModel
    val uploadAadhaarFrontWrapper: (android.net.Uri) -> Unit = { uri ->
        uriToByteArray(uri)?.let { bytes ->
            onUploadAadhaarFront(bytes)
        }
    }

    val uploadAadhaarBackWrapper: (android.net.Uri) -> Unit = { uri ->
        uriToByteArray(uri)?.let { bytes ->
            onUploadAadhaarBack(bytes)
        }
    }

    val uploadProfilePhotoWrapper: (android.net.Uri) -> Unit = { uri ->
        uriToByteArray(uri)?.let { bytes ->
            onUploadProfilePhoto(bytes)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
                // Step Content - Use a Box that fills available space without weight constraints
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = if (isTablet) 600.dp else screenWidth)
                        .align(Alignment.TopCenter)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Stepper Header - Use minimalist progress bar for all steps
                        OnboardingStepIndicator(
                            currentStep = uiState.currentStep.stepNumber,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Step Content - No weight constraints, let content size itself
                        when (uiState.currentStep) {
                            OnboardingStep.BASIC_INFO -> Step1BasicInfo(
                                fullName = uiState.fullName,
                                onFullNameChange = onUpdateFullName,
                                gender = uiState.gender,
                                onGenderChange = onUpdateGender,
                                primaryService = uiState.primaryService,
                                onPrimaryServiceChange = onUpdatePrimaryService,
                                primaryServices = uiState.mainServices,
                                isLoadingServices = uiState.isLoadingServices,
                                email = uiState.email,
                                onEmailChange = onUpdateEmail,
                                language = uiState.language,
                                onLanguageChange = onUpdateLanguage,
                                errorMessage = uiState.errorMessage,
                                onContinue = onNextStep,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OnboardingStep.SERVICE_SELECTION -> {
                                // Load sub-services when entering Step 2 if not already loaded
                                androidx.compose.runtime.LaunchedEffect(uiState.primaryService, uiState.gender) {
                                    if (uiState.availableSubServices.isEmpty() &&
                                        uiState.primaryService.isNotEmpty() &&
                                        uiState.gender.isNotEmpty()) {
                                        onLoadSubServices(uiState.gender, uiState.primaryService)
                                    }
                                }
                                Step2ServiceSelection(
                                    primaryServiceName = uiState.selectedMainService.ifEmpty { uiState.primaryService },
                                    availableSubServices = uiState.availableSubServices,
                                    selectedSubServices = uiState.selectedSubServices,
                                    isSelectAllChecked = uiState.isSelectAllChecked,
                                    isLoadingSubServices = uiState.isLoadingSubServices,
                                    otherService = uiState.otherService,
                                    onSubServiceToggle = onToggleSubService,
                                    onSelectAllToggle = onToggleSelectAll,
                                    onOtherServiceChange = onUpdateOtherService,
                                    onPrevious = onPreviousStep,
                                    onNext = onNextStep,
                                    errorMessage = uiState.errorMessage,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            OnboardingStep.LOCATION -> Step3Location(
                                state = uiState.state,
                                onStateChange = onUpdateState,
                                city = uiState.city,
                                onCityChange = onUpdateCity,
                                address = uiState.address,
                                onAddressChange = onUpdateAddress,
                                fullAddress = uiState.fullAddress,
                                onFullAddressChange = onUpdateFullAddress,
                                locationPincode = uiState.locationPincode,
                                onLocationPincodeChange = onUpdateLocationPincode,
                                serviceRadius = uiState.serviceRadius,
                                onServiceRadiusChange = onUpdateServiceRadius,
                                isLocationLoading = uiState.isLocationLoading,
                                errorMessage = uiState.errorMessage,
                                onUseCurrentLocation = { hasPermission, requestPermission ->
                                    if (hasPermission) {
                                        onUseCurrentLocation()
                                    } else {
                                        requestPermission()
                                    }
                                },
                                onPrevious = onPreviousStep,
                                onNext = onNextStep,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OnboardingStep.VERIFICATION -> Step4Verification(
                                aadhaarFrontUploaded = uiState.aadhaarFrontUploaded,
                                aadhaarBackUploaded = uiState.aadhaarBackUploaded,
                                aadhaarFrontUrl = uiState.aadhaarFrontUrl,
                                aadhaarBackUrl = uiState.aadhaarBackUrl,
                                profilePhotoUploaded = uiState.profilePhotoUploaded,
                                profilePhotoUrl = uiState.profilePhotoUrl,
                                isUploading = uiState.isUploading,
                                uploadProgress = uiState.uploadProgress,
                                uploadingDocumentType = uiState.uploadingDocumentType,
                                errorMessage = uiState.errorMessage,
                                onAadhaarFrontUpload = uploadAadhaarFrontWrapper,
                                onAadhaarBackUpload = uploadAadhaarBackWrapper,
                                onProfilePhotoUpload = uploadProfilePhotoWrapper,
                                onAadhaarFrontDelete = onDeleteAadhaarFront,
                                onAadhaarBackDelete = onDeleteAadhaarBack,
                                onProfilePhotoDelete = onDeleteProfilePhoto,
                                onPrevious = onPreviousStep,
                                onNext = onNextStep,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OnboardingStep.REVIEW -> Step5Review(
                                fullName = uiState.fullName,
                                gender = uiState.gender,
                                email = uiState.email,
                                primaryService = uiState.primaryService,
                                selectedMainService = uiState.selectedMainService,
                                selectedSubServices = uiState.selectedSubServices.toList(),
                                otherService = uiState.otherService,
                                state = uiState.state,
                                city = uiState.city,
                                address = uiState.address,
                                fullAddress = uiState.fullAddress,
                                locationPincode = uiState.locationPincode,
                                serviceRadius = uiState.serviceRadius,
                                aadhaarFrontUploaded = uiState.aadhaarFrontUploaded,
                                aadhaarBackUploaded = uiState.aadhaarBackUploaded,
                                profilePhotoUploaded = uiState.profilePhotoUploaded,
                                isSubmitted = uiState.isSubmitted,
                                verificationStatus = uiState.verificationStatus,
                                rejectionReason = uiState.rejectionReason,
                                submittedAt = uiState.submittedAt,
                                onEditBasicInfo = { onNavigateToStep(1) },
                                onEditServices = { onNavigateToStep(2) },
                                onEditLocation = { onNavigateToStep(3) },
                                onEditDocuments = { onNavigateToStep(4) },
                                onSubmit = onSubmit,
                                onEditRejectedProfile = onEditRejectedProfile,
                                onContactSupport = onContactSupport,
                                onLogout = onLogout,
                                isLoading = uiState.isLoading,
                                errorMessage = uiState.errorMessage,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } // Close Column
                } // Close Box
    }
}

