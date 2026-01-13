package com.nextserve.serveitpartnernew.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextserve.serveitpartnernew.data.model.MainService
import com.nextserve.serveitpartnernew.data.model.ProviderData
import com.nextserve.serveitpartnernew.data.repository.OnboardingRepository
import com.nextserve.serveitpartnernew.domain.onboarding.OnboardingStatus
import com.nextserve.serveitpartnernew.domain.onboarding.OnboardingStep
import com.nextserve.serveitpartnernew.utils.LanguageManager
import com.nextserve.serveitpartnernew.utils.ValidationUtils
import kotlinx.coroutines.launch

/**
 * Thin ViewModel for onboarding UI state management.
 * Delegates all data operations to OnboardingRepository.
 * Focuses on UI state and business logic coordination.
 */
class OnboardingViewModel(
    private val uid: String,
    private val context: Context,
    private val repository: OnboardingRepository = OnboardingRepository(context)
) : ViewModel() {

    var uiState by mutableStateOf(OnboardingUiState())
        private set

    init {
        loadProviderData()
        loadLanguage()
    }

    /**
     * Load provider data and initialize UI state.
     */
    private fun loadProviderData() {
        uiState = uiState.copy(isLoading = true)

        viewModelScope.launch {
            repository.loadProviderData(uid).onSuccess { providerData ->
                if (providerData != null) {
                    hydrateUiStateFromProviderData(providerData)
                } else {
                    uiState = uiState.copy(isLoading = false)
                }
            }.onFailure { exception ->
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Failed to load data"
                )
            }
        }
    }

    /**
     * Hydrate UI state from Firestore provider data.
     */
    private fun hydrateUiStateFromProviderData(providerData: ProviderData) {
        val onboardingStatus = OnboardingStatus.fromStatusString(providerData.onboardingStatus)
        val currentStep = determineCurrentStep(providerData, onboardingStatus)

        uiState = uiState.copy(
            isLoading = false,
            currentStep = currentStep,
            onboardingStatus = onboardingStatus,
            fullName = providerData.fullName,
            gender = providerData.gender,
            primaryService = providerData.primaryService,
            email = providerData.email,
            language = providerData.language.ifEmpty { "en" },
            selectedMainService = providerData.selectedMainService,
            selectedSubServices = providerData.selectedSubServices.toSet(),
            otherService = providerData.otherService,
            state = providerData.state,
            city = providerData.city,
            address = providerData.address,
            fullAddress = providerData.fullAddress,
            locationPincode = providerData.pincode,
            serviceRadius = providerData.serviceRadius.toFloat(),
            latitude = providerData.latitude,
            longitude = providerData.longitude,
            aadhaarFrontUrl = providerData.aadhaarFrontUrl,
            aadhaarBackUrl = providerData.aadhaarBackUrl,
            aadhaarFrontUploaded = providerData.aadhaarFrontUrl.isNotEmpty(),
            aadhaarBackUploaded = providerData.aadhaarBackUrl.isNotEmpty(),
            profilePhotoUrl = providerData.profilePhotoUrl,
            profilePhotoUploaded = providerData.profilePhotoUrl.isNotEmpty(),
            isSubmitted = onboardingStatus == OnboardingStatus.SUBMITTED
        )

        // Load sub-services if primary service is set
        if (providerData.primaryService.isNotEmpty() && providerData.gender.isNotEmpty()) {
            uiState = uiState.copy(selectedMainService = providerData.primaryService)
            loadSubServices(providerData.gender, providerData.primaryService)
        }
    }

    /**
     * Determine which step to show based on provider data.
     */
    private fun determineCurrentStep(providerData: ProviderData, onboardingStatus: OnboardingStatus): OnboardingStep {
        return if (onboardingStatus == OnboardingStatus.SUBMITTED) {
            OnboardingStep.REVIEW // Show completed review
        } else {
            OnboardingStep.fromStepNumber(providerData.currentStep)
        }
    }

    /**
     * Load saved language.
     */
    private fun loadLanguage() {
        val savedLanguage = LanguageManager.getSavedLanguage(context)
        uiState = uiState.copy(language = savedLanguage)
        LanguageManager.applyLanguage(context, savedLanguage)
    }

    // Step 1: Basic Info
    fun updateFullName(name: String) {
        val validationResult = ValidationUtils.validateName(name)
        uiState = uiState.copy(
            fullName = name,
            errorMessage = if (!validationResult.isValid) validationResult.errorMessage else null
        )
        if (validationResult.isValid || name.isEmpty()) {
            saveStep1Data()
        }
    }

    fun updateGender(gender: String) {
        uiState = uiState.copy(gender = gender, primaryService = "")
        saveStep1Data()
    }

    fun updatePrimaryService(service: String) {
        uiState = uiState.copy(primaryService = service)
        saveStep1Data()
    }

    fun updateEmail(email: String) {
        val validationResult = ValidationUtils.validateEmail(email)
        uiState = uiState.copy(
            email = email,
            errorMessage = if (!validationResult.isValid) validationResult.errorMessage else null
        )
        if (validationResult.isValid || email.isEmpty()) {
            saveStep1Data()
        }
    }

    fun updateLanguage(language: String) {
        uiState = uiState.copy(language = language)
        LanguageManager.applyLanguage(context, language)
        saveStep1Data()
    }

    private fun saveStep1Data() {
        viewModelScope.launch {
            repository.saveStep1Data(
                uid = uid,
                fullName = uiState.fullName,
                gender = uiState.gender,
                email = uiState.email,
                language = uiState.language,
                primaryService = uiState.primaryService
            )
        }
    }

    // Step 2: Service Selection
    fun updateSelectedMainService(service: String) {
        uiState = uiState.copy(selectedMainService = service)
        saveStep2Data()
    }

    fun toggleSubService(service: String) {
        val updatedSelection = if (uiState.selectedSubServices.contains(service)) {
            uiState.selectedSubServices - service
        } else {
            uiState.selectedSubServices + service
        }
        uiState = uiState.copy(selectedSubServices = updatedSelection)
        saveStep2Data()
    }

    fun toggleSelectAll() {
        val allSelected = uiState.availableSubServices.size == uiState.selectedSubServices.size
        val newSelection = if (allSelected) emptySet() else uiState.availableSubServices.toSet()
        uiState = uiState.copy(
            selectedSubServices = newSelection,
            isSelectAllChecked = !allSelected
        )
        saveStep2Data()
    }

    fun updateOtherService(service: String) {
        uiState = uiState.copy(otherService = service)
        saveStep2Data()
    }

    fun loadSubServices(gender: String, mainService: String) {
        uiState = uiState.copy(isLoadingSubServices = true)

        viewModelScope.launch {
            repository.loadSubServices(gender, mainService).onSuccess { subServices ->
                uiState = uiState.copy(
                    availableSubServices = subServices,
                    isLoadingSubServices = false
                )
            }.onFailure {
                uiState = uiState.copy(isLoadingSubServices = false)
            }
        }
    }

    private fun saveStep2Data() {
        viewModelScope.launch {
            repository.saveStep2Data(
                uid = uid,
                selectedMainService = uiState.selectedMainService,
                selectedSubServices = uiState.selectedSubServices,
                otherService = uiState.otherService
            )
        }
    }

    // Step 3: Location
    fun updateState(state: String) {
        uiState = uiState.copy(state = state)
        saveStep3Data()
    }

    fun updateCity(city: String) {
        uiState = uiState.copy(city = city)
        saveStep3Data()
    }

    fun updateAddress(address: String) {
        uiState = uiState.copy(address = address)
        saveStep3Data()
    }

    fun updateFullAddress(fullAddress: String) {
        uiState = uiState.copy(fullAddress = fullAddress)
        saveStep3Data()
    }

    fun updateLocationPincode(pincode: String) {
        uiState = uiState.copy(locationPincode = pincode)
        saveStep3Data()
    }

    fun updateServiceRadius(radius: Float) {
        uiState = uiState.copy(serviceRadius = radius)
        saveStep3Data()
    }

    private fun saveStep3Data() {
        viewModelScope.launch {
            repository.saveStep3Data(
                uid = uid,
                state = uiState.state,
                city = uiState.city,
                address = uiState.address,
                fullAddress = uiState.fullAddress,
                pincode = uiState.locationPincode,
                serviceRadius = uiState.serviceRadius,
                latitude = uiState.latitude,
                longitude = uiState.longitude
            )
        }
    }

    // Step 4: Document Upload
    fun uploadAadhaarFront(imageUri: Uri) {
        uiState = uiState.copy(isUploading = true)

        viewModelScope.launch {
            repository.uploadAadhaarFront(uid, imageUri).onSuccess { downloadUrl ->
                uiState = uiState.copy(
                    aadhaarFrontUrl = downloadUrl,
                    aadhaarFrontUploaded = true,
                    isUploading = false
                )
            }.onFailure { exception ->
                uiState = uiState.copy(
                    isUploading = false,
                    errorMessage = "Failed to upload Aadhaar front: ${exception.message}"
                )
            }
        }
    }

    fun uploadAadhaarBack(imageUri: Uri) {
        uiState = uiState.copy(isUploading = true)

        viewModelScope.launch {
            repository.uploadAadhaarBack(uid, imageUri).onSuccess { downloadUrl ->
                uiState = uiState.copy(
                    aadhaarBackUrl = downloadUrl,
                    aadhaarBackUploaded = true,
                    isUploading = false
                )
            }.onFailure { exception ->
                uiState = uiState.copy(
                    isUploading = false,
                    errorMessage = "Failed to upload Aadhaar back: ${exception.message}"
                )
            }
        }
    }

    fun uploadProfilePhoto(imageUri: Uri) {
        uiState = uiState.copy(isUploading = true)

        viewModelScope.launch {
            repository.uploadProfilePhoto(uid, imageUri).onSuccess { downloadUrl ->
                uiState = uiState.copy(
                    profilePhotoUrl = downloadUrl,
                    profilePhotoUploaded = true,
                    isUploading = false
                )
            }.onFailure { exception ->
                uiState = uiState.copy(
                    isUploading = false,
                    errorMessage = "Failed to upload profile photo: ${exception.message}"
                )
            }
        }
    }

    fun deleteAadhaarFront() {
        viewModelScope.launch {
            repository.deleteAadhaarFront(uid).onSuccess {
                uiState = uiState.copy(
                    aadhaarFrontUrl = "",
                    aadhaarFrontUploaded = false
                )
            }
        }
    }

    fun deleteAadhaarBack() {
        viewModelScope.launch {
            repository.deleteAadhaarBack(uid).onSuccess {
                uiState = uiState.copy(
                    aadhaarBackUrl = "",
                    aadhaarBackUploaded = false
                )
            }
        }
    }

    fun deleteProfilePhoto() {
        viewModelScope.launch {
            repository.deleteProfilePhoto(uid).onSuccess {
                uiState = uiState.copy(
                    profilePhotoUrl = "",
                    profilePhotoUploaded = false
                )
            }
        }
    }

    // Navigation and Step Control
    fun nextStep() {
        if (uiState.currentStep.stepNumber < 5) {
            val validationError = validateCurrentStep()
            if (validationError != null) {
                uiState = uiState.copy(errorMessage = validationError)
                return
            }

            val newStep = OnboardingStep.fromStepNumber(uiState.currentStep.stepNumber + 1)
            uiState = uiState.copy(currentStep = newStep, errorMessage = null)
            saveCurrentStep(newStep.stepNumber)
        }
    }

    fun previousStep() {
        if (uiState.currentStep.stepNumber > 1) {
            val newStep = OnboardingStep.fromStepNumber(uiState.currentStep.stepNumber - 1)
            uiState = uiState.copy(currentStep = newStep, errorMessage = null)
            saveCurrentStep(newStep.stepNumber)
        }
    }

    fun navigateToStep(stepNumber: Int) {
        if (OnboardingStep.isValidStep(stepNumber)) {
            val newStep = OnboardingStep.fromStepNumber(stepNumber)
            uiState = uiState.copy(currentStep = newStep, errorMessage = null)
            saveCurrentStep(stepNumber)
        }
    }

    private fun saveCurrentStep(step: Int) {
        viewModelScope.launch {
            repository.updateCurrentStep(uid, step)
        }
    }

    // Submission
    fun submitOnboarding() {
        if (uiState.isSubmitted || uiState.isLoading) return

        if (uiState.onboardingStatus == OnboardingStatus.SUBMITTED) return

        val validationErrors = validateAllSteps()
        if (validationErrors.isNotEmpty()) {
            uiState = uiState.copy(errorMessage = validationErrors.first())
            return
        }

        uiState = uiState.copy(isLoading = true)

        val finalData = mapOf(
            "fullName" to uiState.fullName,
            "gender" to uiState.gender,
            "email" to uiState.email,
            "primaryService" to uiState.primaryService,
            "selectedMainService" to uiState.selectedMainService,
            "selectedSubServices" to uiState.selectedSubServices.toList(),
            "otherService" to uiState.otherService,
            "state" to uiState.state,
            "city" to uiState.city,
            "address" to uiState.address,
            "fullAddress" to uiState.fullAddress,
            "pincode" to uiState.locationPincode,
            "serviceRadius" to uiState.serviceRadius.toDouble(),
            "latitude" to uiState.latitude,
            "longitude" to uiState.longitude,
            "aadhaarFrontUrl" to uiState.aadhaarFrontUrl,
            "aadhaarBackUrl" to uiState.aadhaarBackUrl,
            "profilePhotoUrl" to uiState.profilePhotoUrl
        ).filterValues { it != null } as Map<String, Any>

        viewModelScope.launch {
            repository.submitOnboarding(uid, finalData).onSuccess {
                uiState = uiState.copy(
                    isLoading = false,
                    isSubmitted = true,
                    onboardingStatus = OnboardingStatus.SUBMITTED
                )
            }.onFailure { exception ->
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Submission failed"
                )
            }
        }
    }

    // Reset functionality
    fun resetOnboarding() {
        viewModelScope.launch {
            repository.resetOnboarding(uid).onSuccess {
                uiState = OnboardingUiState() // Reset to initial state
                loadLanguage() // Restore language setting
            }.onFailure { exception ->
                uiState = uiState.copy(
                    errorMessage = "Failed to reset onboarding: ${exception.message}"
                )
            }
        }
    }

    // Validation
    private fun validateCurrentStep(): String? {
        return when (uiState.currentStep) {
            OnboardingStep.BASIC_INFO -> {
                val nameValidation = ValidationUtils.validateName(uiState.fullName)
                if (!nameValidation.isValid) return nameValidation.errorMessage

                if (uiState.gender.isEmpty()) return "Please select your gender"
                if (uiState.primaryService.isEmpty()) return "Please select a primary service"

                val emailValidation = ValidationUtils.validateEmail(uiState.email)
                if (!emailValidation.isValid) return emailValidation.errorMessage

                null
            }
            OnboardingStep.SERVICE_SELECTION -> {
                if (uiState.selectedMainService == "Other Services") {
                    if (uiState.otherService.isEmpty()) return "Please specify your service"
                } else {
                    if (uiState.selectedSubServices.isEmpty()) return "Please select at least one sub-service"
                }
                null
            }
            OnboardingStep.LOCATION -> {
                val addressValidation = ValidationUtils.validateAddress(
                    uiState.state,
                    uiState.city,
                    uiState.address
                )
                if (!addressValidation.isValid) return addressValidation.errorMessage
                null
            }
            else -> null // Steps 4 and 5 don't have validation requirements
        }
    }

    private fun validateAllSteps(): List<String> {
        val errors = mutableListOf<String>()

        // Step 1 validation
        val nameValidation = ValidationUtils.validateName(uiState.fullName)
        if (!nameValidation.isValid) errors.add("Step 1: ${nameValidation.errorMessage}")

        if (uiState.gender.isEmpty()) errors.add("Step 1: Please select your gender")
        if (uiState.primaryService.isEmpty()) errors.add("Step 1: Please select a primary service")

        val emailValidation = ValidationUtils.validateEmail(uiState.email)
        if (!emailValidation.isValid) errors.add("Step 1: ${emailValidation.errorMessage}")

        // Step 2 validation
        if (uiState.selectedMainService == "Other Services") {
            if (uiState.otherService.isEmpty()) errors.add("Step 2: Please specify your service")
        } else {
            if (uiState.selectedSubServices.isEmpty()) errors.add("Step 2: Please select at least one sub-service")
        }

        // Step 3 validation
        val addressValidation = ValidationUtils.validateAddress(
            uiState.state,
            uiState.city,
            uiState.address
        )
        if (!addressValidation.isValid) errors.add("Step 3: ${addressValidation.errorMessage}")

        return errors
    }
}