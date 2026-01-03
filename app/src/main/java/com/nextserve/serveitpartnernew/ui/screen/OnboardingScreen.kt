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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.ui.components.BottomStickyButtonContainer
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.components.StepperHeader
import com.nextserve.serveitpartnernew.ui.screen.onboarding.Step1BasicInfo
import com.nextserve.serveitpartnernew.ui.screen.onboarding.Step2ServiceSelection
import com.nextserve.serveitpartnernew.ui.screen.onboarding.Step3Location
import com.nextserve.serveitpartnernew.ui.screen.onboarding.Step4Verification
import com.nextserve.serveitpartnernew.ui.screen.onboarding.Step5Review
import com.nextserve.serveitpartnernew.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    uid: String? = null,
    authViewModel: com.nextserve.serveitpartnernew.ui.viewmodel.AuthViewModel
) {
    val currentUid = uid ?: FirebaseProvider.auth.currentUser?.uid
    val context = LocalContext.current

    if (currentUid == null) {
        // Show error state
        return
    }

    // Use the existing OnboardingViewModel with AuthViewModel coordination
    val onboardingViewModel: OnboardingViewModel = remember(currentUid) {
        OnboardingViewModel(
            uid = currentUid,
            context = context
        )
    }

    val uiState = onboardingViewModel.uiState

    // Sync onboarding step changes with AuthViewModel
    LaunchedEffect(uiState.currentStep) {
        authViewModel.updateOnboardingStep(currentUid, uiState.currentStep)
    }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth >= 600.dp

    BottomStickyButtonContainer(
        button = {
            // Steps 2, 3, 4, and 5 have their own buttons
            if (uiState.currentStep != 2 && uiState.currentStep != 3 && uiState.currentStep != 4 && uiState.currentStep != 5) {
                PrimaryButton(
                    text = stringResource(R.string.continue_button),
                    onClick = {
                        if (uiState.currentStep < 5) {
                            onboardingViewModel.nextStep()
                        }
                    },
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
                        currentStep = uiState.currentStep,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Step Content
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        when (uiState.currentStep) {
                            1 -> Step1BasicInfo(
                                fullName = uiState.fullName,
                                onFullNameChange = { onboardingViewModel.updateFullName(it) },
                                gender = uiState.gender,
                                onGenderChange = { onboardingViewModel.updateGender(it) },
                                primaryService = uiState.primaryService,
                                onPrimaryServiceChange = { onboardingViewModel.updatePrimaryService(it) },
                                primaryServices = onboardingViewModel.mainServices.value,
                                isLoadingServices = uiState.isLoadingServices,
                                email = uiState.email,
                                onEmailChange = { onboardingViewModel.updateEmail(it) },
                                language = uiState.language,
                                onLanguageChange = { onboardingViewModel.updateLanguage(it) },
                                errorMessage = uiState.errorMessage,
                                modifier = Modifier.fillMaxSize()
                            )
                            2 -> {
                                // Load sub-services when entering Step 2 if not already loaded
                                LaunchedEffect(uiState.primaryService, uiState.gender) {
                                    if (uiState.availableSubServices.isEmpty() && 
                                        uiState.primaryService.isNotEmpty() && 
                                        uiState.gender.isNotEmpty()) {
                                        onboardingViewModel.loadSubServices(uiState.gender, uiState.primaryService)
                                    }
                                }
                                Step2ServiceSelection(
                                    primaryServiceName = uiState.selectedMainService.ifEmpty { uiState.primaryService },
                                    availableSubServices = uiState.availableSubServices,
                                    selectedSubServices = uiState.selectedSubServices,
                                    isSelectAllChecked = uiState.isSelectAllChecked,
                                    isLoadingSubServices = uiState.isLoadingSubServices,
                                    otherService = uiState.otherService,
                                    onSubServiceToggle = { onboardingViewModel.toggleSubService(it) },
                                    onSelectAllToggle = { onboardingViewModel.toggleSelectAll() },
                                    onOtherServiceChange = { onboardingViewModel.updateOtherService(it) },
                                    onPrevious = { onboardingViewModel.previousStep() },
                                    onNext = { onboardingViewModel.nextStep() },
                                    errorMessage = uiState.errorMessage,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            3 -> Step3Location(
                                state = uiState.state,
                                onStateChange = { onboardingViewModel.updateState(it) },
                                city = uiState.city,
                                onCityChange = { onboardingViewModel.updateCity(it) },
                                address = uiState.address,
                                onAddressChange = { onboardingViewModel.updateAddress(it) },
                                fullAddress = uiState.fullAddress,
                                onFullAddressChange = { onboardingViewModel.updateFullAddress(it) },
                                locationPincode = uiState.locationPincode,
                                onLocationPincodeChange = { onboardingViewModel.updateLocationPincode(it) },
                                serviceRadius = uiState.serviceRadius,
                                onServiceRadiusChange = { onboardingViewModel.updateServiceRadius(it) },
                                isLocationLoading = uiState.isLocationLoading,
                                errorMessage = uiState.errorMessage,
                                onUseCurrentLocation = { hasPermission, requestPermission ->
                                    onboardingViewModel.useCurrentLocation(hasPermission, requestPermission)
                                },
                                onPrevious = { onboardingViewModel.previousStep() },
                                onNext = { onboardingViewModel.nextStep() },
                                modifier = Modifier.fillMaxSize()
                            )
                            4 -> Step4Verification(
                                aadhaarFrontUploaded = uiState.aadhaarFrontUploaded,
                                aadhaarBackUploaded = uiState.aadhaarBackUploaded,
                                aadhaarFrontUrl = uiState.aadhaarFrontUrl,
                                aadhaarBackUrl = uiState.aadhaarBackUrl,
                                profilePhotoUploaded = uiState.profilePhotoUploaded,
                                profilePhotoUrl = uiState.profilePhotoUrl,
                                isUploading = uiState.isUploading,
                                uploadProgress = uiState.uploadProgress,
                                errorMessage = uiState.errorMessage,
                                onAadhaarFrontUpload = { imageUri ->
                                    onboardingViewModel.uploadAadhaarFront(imageUri)
                                },
                                onAadhaarBackUpload = { imageUri ->
                                    onboardingViewModel.uploadAadhaarBack(imageUri)
                                },
                                onProfilePhotoUpload = { imageUri ->
                                    onboardingViewModel.uploadProfilePhoto(imageUri)
                                },
                                onAadhaarFrontDelete = {
                                    onboardingViewModel.deleteAadhaarFront()
                                },
                                onAadhaarBackDelete = {
                                    onboardingViewModel.deleteAadhaarBack()
                                },
                                onProfilePhotoDelete = {
                                    onboardingViewModel.deleteProfilePhoto()
                                },
                                onPrevious = { onboardingViewModel.previousStep() },
                                onNext = { onboardingViewModel.nextStep() },
                                modifier = Modifier.fillMaxSize()
                            )
                            5 -> Step5Review(
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
                                onEditBasicInfo = { onboardingViewModel.navigateToStep(1) },
                                onEditServices = { onboardingViewModel.navigateToStep(2) },
                                onEditLocation = { onboardingViewModel.navigateToStep(3) },
                                onEditDocuments = { onboardingViewModel.navigateToStep(4) },
                                onSubmit = {
                                    onboardingViewModel.submitOnboarding()
                                    // Update AuthState after successful submission
                                    authViewModel.completeOnboarding(currentUid)
                                },
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

