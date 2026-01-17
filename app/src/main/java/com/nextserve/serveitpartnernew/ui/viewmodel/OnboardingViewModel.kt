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
import com.nextserve.serveitpartnernew.data.model.LocationData
import com.nextserve.serveitpartnernew.data.repository.LocationRepository
import com.nextserve.serveitpartnernew.data.repository.OnboardingRepository
import com.nextserve.serveitpartnernew.domain.onboarding.OnboardingStatus
import com.nextserve.serveitpartnernew.domain.onboarding.OnboardingStep
import com.nextserve.serveitpartnernew.utils.LanguageManager
import com.nextserve.serveitpartnernew.utils.ValidationUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.google.firebase.firestore.ListenerRegistration

/**
 * Thin ViewModel for onboarding UI state management.
 * Delegates all data operations to OnboardingRepository.
 * Focuses on UI state and business logic coordination.
 */
class OnboardingViewModel(
    private val uid: String,
    private val context: Context,
    private val repository: OnboardingRepository = OnboardingRepository(context),
    private val locationRepository: LocationRepository = LocationRepository(context, com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context))
) : ViewModel() {

    var uiState by mutableStateOf(OnboardingUiState())
        private set

    // Provider document listener for real-time verification status updates
    private var providerListener: ListenerRegistration? = null

    init {
        loadProviderData()
        loadLanguage()
        // Start observing provider document for real-time verification status changes
        startProviderObservation()
    }

    override fun onCleared() {
        super.onCleared()
        stopProviderObservation()
    }

    /**
     * Start observing provider document for real-time verification status updates.
     */
    private fun startProviderObservation() {
        viewModelScope.launch {
            val firestoreRepository = com.nextserve.serveitpartnernew.data.repository.FirestoreRepository(
                com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider.firestore
            )
            providerListener = firestoreRepository.observeProviderDocument(uid) { providerData ->
                if (providerData != null) {
                    // Update verification status in UI state
                    uiState = uiState.copy(
                        verificationStatus = providerData.verificationDetails.status,
                        rejectionReason = providerData.verificationDetails.rejectedReason,
                        isSubmitted = providerData.onboardingStatus == "SUBMITTED"
                    )
                }
            }
        }
    }

    /**
     * Stop observing provider document.
     */
    private fun stopProviderObservation() {
        providerListener?.remove()
        providerListener = null
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
        
        // Sync language from Firestore to local storage if Firestore has it but local doesn't
        val firestoreLanguage = providerData.language.ifEmpty { "en" }
        val localLanguage = LanguageManager.getSavedLanguage(context)
        if (firestoreLanguage.isNotEmpty() && firestoreLanguage != "en" && (localLanguage.isEmpty() || localLanguage == "en")) {
            // Firestore has a language but local doesn't - sync it
            LanguageManager.applyLanguage(context, firestoreLanguage)
        }

        uiState = uiState.copy(
            isLoading = false,
            currentStep = currentStep,
            onboardingStatus = onboardingStatus,
            fullName = providerData.fullName,
            gender = providerData.gender,
            primaryService = providerData.primaryService,
            email = providerData.email,
            language = firestoreLanguage,
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
            isSubmitted = onboardingStatus == OnboardingStatus.SUBMITTED,
            verificationStatus = providerData.verificationDetails.status,
            rejectionReason = providerData.verificationDetails.rejectedReason,
            submittedAt = providerData.submittedAt?.toDate()?.time
        )

        // Load services if gender is set
        if (providerData.gender.isNotEmpty()) {
            loadMainServices(providerData.gender)

            // Load sub-services if primary service is set
            if (providerData.primaryService.isNotEmpty()) {
                uiState = uiState.copy(selectedMainService = providerData.primaryService)

                // VALIDATE SUB-SERVICES: Ensure they belong to the current primary service
                if (shouldValidateSubServices(providerData)) {
                    // Sub-services exist but may be stale - validate them
                    validateAndLoadSubServices(providerData.gender, providerData.primaryService, providerData.selectedSubServices)
                } else {
                    // Fresh load - no existing sub-services to validate
                    loadSubServices(providerData.gender, providerData.primaryService)
                }
            }
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
        uiState = uiState.copy(gender = gender, primaryService = "", isLoadingServices = true)
        saveStep1Data()
        loadMainServices(gender)
    }

    fun updatePrimaryService(service: String) {
        // ENFORCE DEPENDENCY RULE: Primary service is the parent
        // When primary service changes, sub-services must be reset
        val previousService = uiState.primaryService

        if (service != previousService) {
            // PRIMARY SERVICE CHANGED - Clear all dependent data
            uiState = uiState.copy(
                primaryService = service,
                selectedMainService = "",  // Clear dependent data
                selectedSubServices = emptySet(),  // Clear sub-service selections
                availableSubServices = emptyList(),  // Clear cached sub-services
                otherService = ""  // Clear other service input
            )

            // Clear sub-service data in Firestore for consistency
            clearSubServiceData()

            // Save updated primary service
            saveStep1Data()

            // TRIGGER FRESH SUB-SERVICE LOAD for new primary service
            if (service.isNotEmpty()) {
                loadSubServices(uiState.gender, service)
            }
        } else {
            // Same service selected, just update UI state
            uiState = uiState.copy(primaryService = service)
            saveStep1Data()
        }
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

    /**
     * Clear sub-service data when primary service changes.
     * This ensures Firestore consistency and prevents stale data.
     */
    private fun clearSubServiceData() {
        viewModelScope.launch {
            repository.clearSubServiceData(uid)
        }
    }

    /**
     * Check if sub-services need validation against primary service.
     */
    private fun shouldValidateSubServices(providerData: ProviderData): Boolean {
        return providerData.selectedSubServices.isNotEmpty() ||
               providerData.selectedMainService.isNotEmpty() ||
               providerData.otherService.isNotEmpty()
    }

    /**
     * Validate existing sub-services against primary service and load fresh data if needed.
     * This prevents stale sub-service data from being used.
     */
    private fun validateAndLoadSubServices(gender: String, primaryService: String, existingSubServices: List<String>) {
        // For now, we always reload fresh sub-services to ensure consistency
        // In a more sophisticated implementation, we could validate if existing sub-services
        // are still valid for the current primary service
        loadSubServices(gender, primaryService)

        // Note: If we wanted to preserve selections, we would need to:
        // 1. Load fresh sub-services
        // 2. Filter existing selections to only include valid ones
        // 3. Update UI state with filtered selections
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

    fun loadMainServices(gender: String) {
        if (gender.isEmpty()) return

        viewModelScope.launch {
            repository.loadMainServices(gender).onSuccess { mainServices ->
                uiState = uiState.copy(
                    mainServices = mainServices,
                    isLoadingServices = false
                )
            }.onFailure {
                uiState = uiState.copy(
                    mainServices = emptyList(),
                    isLoadingServices = false,
                    errorMessage = "Failed to load services"
                )
            }
        }
    }

    fun loadSubServices(gender: String, mainService: String) {
        uiState = uiState.copy(isLoadingSubServices = true)

        viewModelScope.launch {
            repository.loadSubServices(gender, mainService).onSuccess { subServices ->
                // Auto-select all sub-services by default when first loading (if none are currently selected)
                val newSelection = if (uiState.selectedSubServices.isEmpty() && subServices.isNotEmpty()) {
                    subServices.toSet() // Select all by default
                } else {
                    uiState.selectedSubServices // Keep existing selection
                }
                
                uiState = uiState.copy(
                    availableSubServices = subServices,
                    selectedSubServices = newSelection,
                    isSelectAllChecked = newSelection.size == subServices.size && subServices.isNotEmpty(),
                    isLoadingSubServices = false
                )
                
                // Save the auto-selected sub-services
                if (newSelection != uiState.selectedSubServices) {
                    saveStep2Data()
                }
            }.onFailure {
                uiState = uiState.copy(
                    isLoadingSubServices = false,
                    errorMessage = "Failed to load sub-services"
                )
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

    fun useCurrentLocation() {
        uiState = uiState.copy(isLocationLoading = true)

        viewModelScope.launch {
            val locationResult = locationRepository.getCurrentLocation()
            locationResult.onSuccess { location ->
                val addressResult = locationRepository.getDetailedAddressFromLocation(
                    location.latitude,
                    location.longitude
                )
                addressResult.onSuccess { address ->
                    // Extract individual address components with fallbacks
                    val city = address.locality ?: address.subAdminArea ?: ""
                    val state = address.adminArea ?: ""
                    val pincode = address.postalCode ?: ""
                    val areaLocality = address.thoroughfare ?: address.featureName ?: ""

                    // Build full address string
                    val fullAddressParts = mutableListOf<String>()
                    address.featureName?.let { fullAddressParts.add(it) }
                    address.thoroughfare?.let { fullAddressParts.add(it) }
                    address.locality?.let { fullAddressParts.add(it) }
                    address.adminArea?.let { fullAddressParts.add(it) }
                    address.postalCode?.let { fullAddressParts.add(it) }
                    address.countryName?.let { fullAddressParts.add(it) }
                    val fullAddress = fullAddressParts.joinToString(", ")

                    // Update UI state
                    uiState = uiState.copy(
                        state = state,
                        city = city,
                        address = areaLocality,
                        fullAddress = fullAddress,
                        locationPincode = pincode,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        isLocationLoading = false
                    )

                    // Save to Firestore
                    saveStep3Data()
                }.onFailure { error ->
                    uiState = uiState.copy(
                        isLocationLoading = false,
                        errorMessage = "Failed to get address: ${error.message}"
                    )
                }
            }.onFailure { error ->
                uiState = uiState.copy(
                    isLocationLoading = false,
                    errorMessage = "Failed to get location: ${error.message}"
                )
            }
        }
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
    fun uploadAadhaarFront(imageBytes: ByteArray) {
        uiState = uiState.copy(
            isUploading = true,
            uploadingDocumentType = "front",
            uploadProgress = 0f
        )

        viewModelScope.launch {
            repository.uploadAadhaarFront(uid, imageBytes) { progress ->
                // Update progress on main thread
                viewModelScope.launch(Dispatchers.Main) {
                    uiState = uiState.copy(uploadProgress = progress.toFloat())
                }
            }.onSuccess { downloadUrl ->
                uiState = uiState.copy(
                    aadhaarFrontUrl = downloadUrl,
                    aadhaarFrontUploaded = true,
                    isUploading = false,
                    uploadingDocumentType = null,
                    uploadProgress = 0f
                )
            }.onFailure { exception ->
                uiState = uiState.copy(
                    isUploading = false,
                    uploadingDocumentType = null,
                    uploadProgress = 0f,
                    errorMessage = "Failed to upload Aadhaar front: ${exception.message}"
                )
            }
        }
    }

    fun uploadAadhaarBack(imageBytes: ByteArray) {
        uiState = uiState.copy(
            isUploading = true,
            uploadingDocumentType = "back",
            uploadProgress = 0f
        )

        viewModelScope.launch {
            repository.uploadAadhaarBack(uid, imageBytes) { progress ->
                // Update progress on main thread
                viewModelScope.launch(Dispatchers.Main) {
                    uiState = uiState.copy(uploadProgress = progress.toFloat())
                }
            }.onSuccess { downloadUrl ->
                uiState = uiState.copy(
                    aadhaarBackUrl = downloadUrl,
                    aadhaarBackUploaded = true,
                    isUploading = false,
                    uploadingDocumentType = null,
                    uploadProgress = 0f
                )
            }.onFailure { exception ->
                uiState = uiState.copy(
                    isUploading = false,
                    uploadingDocumentType = null,
                    uploadProgress = 0f,
                    errorMessage = "Failed to upload Aadhaar back: ${exception.message}"
                )
            }
        }
    }

    fun uploadProfilePhoto(imageBytes: ByteArray) {
        uiState = uiState.copy(
            isUploading = true,
            uploadingDocumentType = "profile",
            uploadProgress = 0f
        )

        viewModelScope.launch {
            repository.uploadProfilePhoto(uid, imageBytes) { progress ->
                // Update progress on main thread
                viewModelScope.launch(Dispatchers.Main) {
                    uiState = uiState.copy(uploadProgress = progress.toFloat())
                }
            }.onSuccess { downloadUrl ->
                uiState = uiState.copy(
                    profilePhotoUrl = downloadUrl,
                    profilePhotoUploaded = true,
                    isUploading = false,
                    uploadingDocumentType = null,
                    uploadProgress = 0f
                )
            }.onFailure { exception ->
                uiState = uiState.copy(
                    isUploading = false,
                    uploadingDocumentType = null,
                    uploadProgress = 0f,
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

    /**
     * Allow editing of rejected profile.
     * Resets Firestore verification state and onboarding status to allow resubmission.
     * 
     * CRITICAL: This method MUST reset Firestore state, not just UI state.
     * Without Firestore reset, the real-time listener will immediately restore rejected status,
     * causing an infinite loop where user is stuck on rejected screen.
     */
    fun editRejectedProfile() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            repository.resetVerificationAndOnboarding(uid).onSuccess {
                // After successful Firestore reset, update UI state
                uiState = uiState.copy(
                    isLoading = false,
                    isSubmitted = false,
                    onboardingStatus = OnboardingStatus.IN_PROGRESS,
                    currentStep = OnboardingStep.BASIC_INFO,
                    verificationStatus = null,
                    rejectionReason = null,
                    errorMessage = null
                )
                // Navigate to Step 1
                navigateToStep(OnboardingStep.BASIC_INFO.stepNumber)
            }.onFailure { exception ->
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "Failed to reset profile: ${exception.message ?: "Unknown error"}"
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