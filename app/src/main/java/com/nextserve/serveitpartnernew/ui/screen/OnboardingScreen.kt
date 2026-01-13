package com.nextserve.serveitpartnernew.ui.screen

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.domain.onboarding.OnboardingStep
import com.nextserve.serveitpartnernew.ui.components.BottomStickyButtonContainer
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.components.StepperHeader
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
    onUploadAadhaarFront: (android.net.Uri) -> Unit,
    onUploadAadhaarBack: (android.net.Uri) -> Unit,
    onUploadProfilePhoto: (android.net.Uri) -> Unit,
    onDeleteAadhaarFront: () -> Unit,
    onDeleteAadhaarBack: () -> Unit,
    onDeleteProfilePhoto: () -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onNavigateToStep: (Int) -> Unit,
    onSubmit: () -> Unit,
    onReset: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth >= 600.dp
    val (hasLocationPermission, requestLocationPermission) = rememberLocationPermissionState()

    BottomStickyButtonContainer(
        button = {
            // Steps 2, 3, 4, and 5 have their own buttons
            if (uiState.currentStep.stepNumber != 2 &&
                uiState.currentStep.stepNumber != 3 &&
                uiState.currentStep.stepNumber != 4 &&
                uiState.currentStep.stepNumber != 5) {
                PrimaryButton(
                    text = stringResource(R.string.continue_button),
                    onClick = onNextStep,
                    enabled = !uiState.isLoading
                )
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Background Image - Same as Login Screen
                Image(
                    painter = painterResource(id = R.drawable.serveit_partner_flow_bg),
                    contentDescription = "Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Light gradient overlay for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFE3F2FD).copy(alpha = 0.3f),
                                    Color(0xFFFFFFFF).copy(alpha = 0.4f)
                                )
                            )
                        )
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = if (isTablet) 600.dp else screenWidth)
                        .align(Alignment.TopCenter)
                ) {
                    // Stepper Header
                    StepperHeader(
                        currentStep = uiState.currentStep.stepNumber,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Step Content
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
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
                                modifier = Modifier.fillMaxSize()
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
                                    modifier = Modifier.fillMaxSize()
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
                                modifier = Modifier.fillMaxSize()
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
                                errorMessage = uiState.errorMessage,
                                onAadhaarFrontUpload = onUploadAadhaarFront,
                                onAadhaarBackUpload = onUploadAadhaarBack,
                                onProfilePhotoUpload = onUploadProfilePhoto,
                                onAadhaarFrontDelete = onDeleteAadhaarFront,
                                onAadhaarBackDelete = onDeleteAadhaarBack,
                                onProfilePhotoDelete = onDeleteProfilePhoto,
                                onPrevious = onPreviousStep,
                                onNext = onNextStep,
                                modifier = Modifier.fillMaxSize()
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
                                isSubmitted = uiState.isSubmitted,
                                onEditBasicInfo = { onNavigateToStep(1) },
                                onEditServices = { onNavigateToStep(2) },
                                onEditLocation = { onNavigateToStep(3) },
                                onEditDocuments = { onNavigateToStep(4) },
                                onSubmit = onSubmit,
                                isLoading = uiState.isLoading,
                                errorMessage = uiState.errorMessage,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    )
}

