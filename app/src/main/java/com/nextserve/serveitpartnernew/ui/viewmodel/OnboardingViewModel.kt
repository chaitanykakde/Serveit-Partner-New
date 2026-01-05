package com.nextserve.serveitpartnernew.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.gms.location.LocationServices
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.model.LocationData
import com.nextserve.serveitpartnernew.data.model.MainService
import com.nextserve.serveitpartnernew.data.model.ProviderData
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import com.nextserve.serveitpartnernew.data.repository.LocationRepository
import com.nextserve.serveitpartnernew.data.repository.StorageRepository
import com.nextserve.serveitpartnernew.utils.LanguageManager
import com.nextserve.serveitpartnernew.utils.ValidationUtils
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentStep: Int = 1,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val approvalStatus: String = "PENDING",
    // Step 1 data
    val fullName: String = "",
    val gender: String = "",
    val primaryService: String = "",
    val email: String = "",
    val language: String = "en",
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
    val isSubmitted: Boolean = false,
    val onboardingStatus: String = "IN_PROGRESS"
)

class OnboardingViewModel(
    private val uid: String,
    private val context: Context,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(FirebaseProvider.firestore),
    private val locationRepository: LocationRepository = LocationRepository(context, LocationServices.getFusedLocationProviderClient(context)),
    private val storageRepository: StorageRepository = StorageRepository(FirebaseProvider.storage, context)
) : ViewModel() {
    var uiState by mutableStateOf(OnboardingUiState())
        private set

    // Main services loaded from Firestore
    private val _mainServices = mutableStateOf<List<MainService>>(emptyList())
    val mainServices: State<List<MainService>> = _mainServices

    init {
        // Load saved language and apply it
        val savedLanguage = LanguageManager.getSavedLanguage(context)
        uiState = uiState.copy(language = savedLanguage)
        LanguageManager.applyLanguage(context, savedLanguage)
        
        loadProviderData()
    }

    private fun loadProviderData() {
        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            val result = firestoreRepository.getProviderData(uid)
            result.onSuccess { providerData ->
                if (providerData != null) {
                    // If profile was rejected, reset onboarding status to allow editing
                    val onboardingStatus = if (providerData.approvalStatus == "REJECTED") {
                        "IN_PROGRESS"
                    } else {
                        providerData.onboardingStatus
                    }
                    
                    // Hydrate state from Firestore
                    uiState = uiState.copy(
                        isLoading = false,
                        currentStep = if (onboardingStatus == "SUBMITTED" && providerData.approvalStatus != "REJECTED") 5 else providerData.currentStep,
                        onboardingStatus = onboardingStatus,
                        approvalStatus = providerData.approvalStatus,
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
                        isSubmitted = onboardingStatus == "SUBMITTED"
                    )
                    
                    // Load sub-services if primary service is set
                    if (providerData.primaryService.isNotEmpty() && providerData.gender.isNotEmpty()) {
                        // Set the main service first, then load subservices
                        uiState = uiState.copy(selectedMainService = providerData.primaryService)
                        loadSubServices(providerData.gender, providerData.primaryService)
                    }
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

    fun nextStep() {
        if (uiState.currentStep < 5) {
            // Validate current step before proceeding
            val validationError = validateCurrentStep()
            if (validationError != null) {
                uiState = uiState.copy(errorMessage = validationError)
                return
            }
            
            val newStep = uiState.currentStep + 1
            uiState = uiState.copy(currentStep = newStep, errorMessage = null)
            saveCurrentStep(newStep)
        }
    }
    
    /**
     * Validates the current step before allowing navigation.
     * @return Error message if validation fails, null if valid
     */
    private fun validateCurrentStep(): String? {
        return when (uiState.currentStep) {
            1 -> {
                val nameValidation = ValidationUtils.validateName(uiState.fullName)
                if (!nameValidation.isValid) return nameValidation.errorMessage
                
                if (uiState.gender.isEmpty()) {
                    return "Please select your gender"
                }
                
                if (uiState.primaryService.isEmpty()) {
                    return "Please select a primary service"
                }
                
                val emailValidation = ValidationUtils.validateEmail(uiState.email)
                if (!emailValidation.isValid) return emailValidation.errorMessage
                
                null
            }
            2 -> {
                if (uiState.selectedMainService == "Other Services") {
                    if (uiState.otherService.isEmpty()) {
                        return "Please specify your service"
                    }
                } else {
                    if (uiState.selectedSubServices.isEmpty()) {
                        return "Please select at least one sub-service"
                    }
                }
                null
            }
            3 -> {
                val addressValidation = ValidationUtils.validateAddress(
                    uiState.state,
                    uiState.city,
                    uiState.address
                )
                if (!addressValidation.isValid) return addressValidation.errorMessage
                
                val pincodeValidation = ValidationUtils.validatePincode(uiState.locationPincode)
                if (!pincodeValidation.isValid) return pincodeValidation.errorMessage
                
                val coordinateValidation = ValidationUtils.validateCoordinates(
                    uiState.latitude,
                    uiState.longitude
                )
                if (!coordinateValidation.isValid) return coordinateValidation.errorMessage
                
                val radiusValidation = ValidationUtils.validateServiceRadius(uiState.serviceRadius)
                if (!radiusValidation.isValid) return radiusValidation.errorMessage
                
                null
            }
            4 -> {
                if (!uiState.aadhaarFrontUploaded) {
                    return "Please upload Aadhaar front side"
                }
                if (!uiState.aadhaarBackUploaded) {
                    return "Please upload Aadhaar back side"
                }
                if (!uiState.profilePhotoUploaded) {
                    return "Please upload profile photo"
                }
                null
            }
            else -> null
        }
    }

    fun previousStep() {
        if (uiState.currentStep > 1) {
            val newStep = uiState.currentStep - 1
            uiState = uiState.copy(currentStep = newStep)
            saveCurrentStep(newStep)
        }
    }

    fun navigateToStep(step: Int) {
        // Allow navigation if not submitted, or if submitted but rejected (allowing re-edit)
        if (step in 1..5 && (uiState.onboardingStatus != "SUBMITTED" || uiState.approvalStatus == "REJECTED")) {
            uiState = uiState.copy(currentStep = step)
            saveCurrentStep(step)
        }
    }

    private fun saveCurrentStep(step: Int) {
        viewModelScope.launch {
            firestoreRepository.updateCurrentStep(uid, step)
        }
    }

    // Step 1 functions
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
        loadMainServices(gender)
        saveStep1Data()
    }
    
    fun loadMainServices(gender: String) {
        if (gender.isEmpty()) return

        uiState = uiState.copy(isLoadingServices = true, errorMessage = null)
        viewModelScope.launch {
            val result = firestoreRepository.getMainServices(gender)
            result.onSuccess { services ->
                _mainServices.value = services
                uiState = uiState.copy(isLoadingServices = false)
                android.util.Log.d("OnboardingViewModel", "Successfully loaded ${services.size} services for gender: $gender")
            }.onFailure { exception ->
                // Repository now returns empty list for missing data, so this should rarely happen
                // Only real Firestore errors (not missing collections) will reach here
                _mainServices.value = emptyList()
                uiState = uiState.copy(
                    isLoadingServices = false,
                    errorMessage = "Unable to load services. Please check your connection."
                )
                android.util.Log.e("OnboardingViewModel", "Failed to load services for gender: $gender", exception)
            }
        }
    }

    fun updatePrimaryService(service: String) {
        val previousService = uiState.primaryService
        uiState = uiState.copy(primaryService = service)
        
        // If service changed, clear sub-service selections to select all by default
        if (service != previousService) {
            uiState = uiState.copy(selectedSubServices = emptySet(), selectedMainService = "")
        }
        
        if (service.isNotEmpty() && uiState.gender.isNotEmpty()) {
            loadSubServices(uiState.gender, service)
        }
        saveStep1Data()
    }
    
    fun loadSubServices(gender: String, mainServiceName: String) {
        if (gender.isEmpty() || mainServiceName.isEmpty()) return
        
        uiState = uiState.copy(isLoadingSubServices = true, errorMessage = null)
        viewModelScope.launch {
            val result = firestoreRepository.getSubServices(gender, mainServiceName)
            result.onSuccess { subServiceNames ->
                // Always select all sub-services by default when loading a new service
                // Only preserve selections if it's the same service and we have existing selections
                val isSameService = uiState.selectedMainService == mainServiceName
                val currentSelected = if (isSameService && uiState.selectedSubServices.isNotEmpty()) {
                    // Same service - preserve existing selections, but filter out names that no longer exist
                    uiState.selectedSubServices.filter { selectedName ->
                        selectedName in subServiceNames
                    }.toSet()
                } else {
                    // New service or no existing selections - select all by default
                    subServiceNames.toSet()
                }

                val allSelected = currentSelected.size == subServiceNames.size && subServiceNames.isNotEmpty()

                uiState = uiState.copy(
                    availableSubServices = subServiceNames,
                    selectedSubServices = currentSelected,
                    isSelectAllChecked = allSelected,
                    selectedMainService = mainServiceName,
                    isLoadingSubServices = false
                )
                saveStep2Data()
            }.onFailure { exception ->
                uiState = uiState.copy(
                    isLoadingSubServices = false,
                    errorMessage = "Failed to load sub-services: ${exception.message}"
                )
            }
        }
    }

    fun updateEmail(email: String) {
        val validationResult = ValidationUtils.validateEmail(email)
        uiState = uiState.copy(
            email = email,
            errorMessage = if (!validationResult.isValid) validationResult.errorMessage else null
        )
        // Email is optional, so save even if empty
        saveStep1Data()
    }

    fun updateLanguage(languageCode: String) {
        uiState = uiState.copy(language = languageCode)
        // Apply language immediately
        com.nextserve.serveitpartnernew.utils.LanguageManager.applyLanguage(context, languageCode)
        saveStep1Data()
    }

    private fun saveStep1Data() {
        viewModelScope.launch {
            firestoreRepository.saveOnboardingStep(
                uid,
                mapOf(
                    "fullName" to uiState.fullName,
                    "gender" to uiState.gender,
                    "primaryService" to uiState.primaryService,
                    "email" to uiState.email,
                    "language" to uiState.language
                )
            )
        }
    }

    // Step 2 functions
    fun toggleSubService(subService: String) {
        val current = uiState.selectedSubServices.toMutableSet()
        if (current.contains(subService)) {
            current.remove(subService)
        } else {
            current.add(subService)
        }
        // Fix: Calculate isSelectAllChecked based on current selection and available services
        val allSelected = current.size == uiState.availableSubServices.size &&
                          uiState.availableSubServices.isNotEmpty() &&
                          current.containsAll(uiState.availableSubServices)
        uiState = uiState.copy(
            selectedSubServices = current,
            isSelectAllChecked = allSelected,
            errorMessage = null // Clear error when user makes selection
        )
        saveStep2Data()
    }

    fun toggleSelectAll() {
        if (uiState.isSelectAllChecked) {
            uiState = uiState.copy(
                selectedSubServices = emptySet(),
                isSelectAllChecked = false,
                errorMessage = null
            )
        } else {
            val allSubServiceNames = uiState.availableSubServices.toSet()
            uiState = uiState.copy(
                selectedSubServices = allSubServiceNames,
                isSelectAllChecked = true,
                errorMessage = null
            )
        }
        saveStep2Data()
    }

    fun updateOtherService(service: String) {
        uiState = uiState.copy(
            otherService = service,
            errorMessage = if (service.isEmpty() && uiState.selectedMainService == "Other Services") {
                "Please specify your service"
            } else null
        )
        saveStep2Data()
    }


    private fun saveStep2Data() {
        viewModelScope.launch {
            firestoreRepository.saveOnboardingStep(
                uid,
                mapOf(
                    "selectedMainService" to uiState.selectedMainService,
                    "selectedSubServices" to uiState.selectedSubServices.toList(),
                    "otherService" to uiState.otherService
                )
            )
        }
    }

    // Step 3 functions
    fun updateCity(city: String) {
        uiState = uiState.copy(
            city = city,
            errorMessage = null // Clear error when user types
        )
        saveStep3Data()
    }

    fun updateState(state: String) {
        uiState = uiState.copy(
            state = state,
            errorMessage = null // Clear error when user types
        )
        saveStep3Data()
    }

    fun updateAddress(address: String) {
        uiState = uiState.copy(
            address = address,
            errorMessage = null // Clear error when user types
        )
        saveStep3Data()
    }

    fun updateFullAddress(fullAddress: String) {
        uiState = uiState.copy(fullAddress = fullAddress)
        saveStep3Data()
    }

    fun updateLocationPincode(pincode: String) {
        val filtered = pincode.filter { it.isDigit() }.take(6)
        val validationResult = ValidationUtils.validatePincode(filtered)
        uiState = uiState.copy(
            locationPincode = filtered,
            errorMessage = if (filtered.isNotEmpty() && !validationResult.isValid) {
                validationResult.errorMessage
            } else null
        )
        saveStep3Data()
    }

    fun updateServiceRadius(radius: Float) {
        val validationResult = ValidationUtils.validateServiceRadius(radius)
        uiState = uiState.copy(
            serviceRadius = radius,
            errorMessage = if (!validationResult.isValid) {
                validationResult.errorMessage
            } else null
        )
        saveStep3Data()
    }

    fun useCurrentLocation(hasPermission: Boolean, requestPermission: () -> Unit) {
        if (!hasPermission) {
            requestPermission()
            return
        }

        uiState = uiState.copy(isLocationLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val locationResult = locationRepository.getCurrentLocation()
                locationResult.onSuccess { location ->
                    // Get detailed address components from coordinates
                    val addressResult = locationRepository.getDetailedAddressFromLocation(
                        location.latitude,
                        location.longitude
                    )
                    addressResult.onSuccess { address ->
                        // Debug: Log all address components
                        android.util.Log.d("OnboardingViewModel", "Address components:")
                        android.util.Log.d("OnboardingViewModel", "  Locality: ${address.locality}")
                        android.util.Log.d("OnboardingViewModel", "  AdminArea: ${address.adminArea}")
                        android.util.Log.d("OnboardingViewModel", "  SubAdminArea: ${address.subAdminArea}")
                        android.util.Log.d("OnboardingViewModel", "  PostalCode: ${address.postalCode}")
                        android.util.Log.d("OnboardingViewModel", "  CountryName: ${address.countryName}")
                        android.util.Log.d("OnboardingViewModel", "  AddressLine(0): ${address.getAddressLine(0)}")
                        android.util.Log.d("OnboardingViewModel", "  FeatureName: ${address.featureName}")
                        android.util.Log.d("OnboardingViewModel", "  Thoroughfare: ${address.thoroughfare}")

                        // Extract individual address components with fallbacks
                        var city = address.locality ?: address.subAdminArea ?: ""
                        var state = address.adminArea ?: ""
                        var pincode = address.postalCode ?: ""
                        val fullAddress = address.getAddressLine(0) ?: ""

                        // Extract landmark/area information (building, street, area name)
                        val landmark = extractLandmarkInfo(address, fullAddress)

                        // If individual components are missing, try to parse from full address
                        if (city.isEmpty() || state.isEmpty() || pincode.isEmpty()) {
                            val parsedComponents = parseAddressComponents(fullAddress)
                            if (city.isEmpty()) city = parsedComponents.city
                            if (state.isEmpty()) state = parsedComponents.state
                            if (pincode.isEmpty()) pincode = parsedComponents.pincode
                        }

                        android.util.Log.d("OnboardingViewModel", "Final - City: '$city', State: '$state', Pincode: '$pincode', Landmark: '$landmark', FullAddress: '$fullAddress'")

                        // Validate coordinates after getting location
                        val coordinateValidation = ValidationUtils.validateCoordinates(
                            location.latitude,
                            location.longitude
                        )

                        uiState = uiState.copy(
                            isLocationLoading = false,
                            fullAddress = fullAddress,
                            address = landmark,  // ← Populate the landmark field
                            city = city,
                            state = state,
                            locationPincode = pincode,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            errorMessage = if (!coordinateValidation.isValid) {
                                coordinateValidation.errorMessage
                            } else null
                        )
                        saveStep3Data()
                    }.onFailure {
                        // If detailed address lookup fails, try basic address lookup
                        val basicAddressResult = locationRepository.getAddressFromLocation(
                            location.latitude,
                            location.longitude
                        )
                        basicAddressResult.onSuccess { basicAddress ->
                            val coordinateValidation = ValidationUtils.validateCoordinates(
                                location.latitude,
                                location.longitude
                            )
                            uiState = uiState.copy(
                                isLocationLoading = false,
                                fullAddress = basicAddress,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                errorMessage = if (!coordinateValidation.isValid) {
                                    coordinateValidation.errorMessage
                                } else null
                            )
                            saveStep3Data()
                        }.onFailure {
                            // If both lookups fail, just use coordinates
                            val coordinateValidation = ValidationUtils.validateCoordinates(
                                location.latitude,
                                location.longitude
                            )
                            uiState = uiState.copy(
                                isLocationLoading = false,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                errorMessage = if (!coordinateValidation.isValid) {
                                    coordinateValidation.errorMessage
                                } else null
                            )
                            saveStep3Data()
                        }
                    }
                }.onFailure { exception ->
                    uiState = uiState.copy(
                        isLocationLoading = false,
                        errorMessage = exception.message ?: "Failed to get location. Please enter manually."
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLocationLoading = false,
                    errorMessage = e.message ?: "Location error. Please enter manually."
                )
            }
        }
    }

    private fun saveStep3Data() {
        viewModelScope.launch {
            val data = mutableMapOf<String, Any>(
                "state" to uiState.state,
                "city" to uiState.city,
                "address" to uiState.address,
                "fullAddress" to uiState.fullAddress,
                "pincode" to uiState.locationPincode,
                "serviceRadius" to uiState.serviceRadius
            )
            uiState.latitude?.let { data["latitude"] = it }
            uiState.longitude?.let { data["longitude"] = it }
            
            firestoreRepository.saveOnboardingStep(uid, data)
        }
    }

    // Step 4 functions
    fun uploadAadhaarFront(imageUri: Uri) {
        // Verify user is authenticated
        val currentUid = FirebaseProvider.auth.currentUser?.uid
        if (currentUid == null || currentUid != uid) {
            uiState = uiState.copy(
                errorMessage = "User not authenticated. Please login again."
            )
            return
        }

        // Validate image before upload
        val validationError = validateImage(imageUri)
        if (validationError != null) {
            uiState = uiState.copy(errorMessage = validationError)
            return
        }

        // Copy image to temp file for reliable access
        val tempFileResult = copyImageToTempFile(imageUri)
        if (tempFileResult.isFailure) {
            uiState = uiState.copy(
                errorMessage = "Failed to prepare image for upload. Please try again."
            )
            return
        }

        val tempFile = tempFileResult.getOrNull() ?: return

        uiState = uiState.copy(isUploading = true, uploadProgress = 0f, errorMessage = null)
        viewModelScope.launch {
            try {
                val result = storageRepository.uploadAadhaarDocumentFromFile(
                    uid = uid,
                    documentType = "front",
                    imageFile = tempFile,
                    onProgress = { progress ->
                        uiState = uiState.copy(uploadProgress = progress.toFloat())
                    }
                )

                result.onSuccess { url ->
                    uiState = uiState.copy(
                        aadhaarFrontUrl = url,
                        aadhaarFrontUploaded = true,
                        isUploading = false,
                        uploadProgress = 0f,
                        errorMessage = null
                    )
                    saveDocumentUrls()
                }.onFailure { exception ->
                    uiState = uiState.copy(
                        isUploading = false,
                        uploadProgress = 0f,
                        errorMessage = exception.message ?: "Upload failed. Please try again."
                    )
                }
            } finally {
                // Clean up temp file
                try {
                    tempFile.delete()
                    android.util.Log.d("OnboardingViewModel", "Cleaned up temp file: ${tempFile.absolutePath}")
                } catch (e: Exception) {
                    android.util.Log.w("OnboardingViewModel", "Failed to clean up temp file: ${e.message}")
                }
            }
        }
    }

    fun uploadAadhaarBack(imageUri: Uri) {
        // Verify user is authenticated
        val currentUid = FirebaseProvider.auth.currentUser?.uid
        if (currentUid == null || currentUid != uid) {
            uiState = uiState.copy(
                errorMessage = "User not authenticated. Please login again."
            )
            return
        }

        // Validate image before upload
        val validationError = validateImage(imageUri)
        if (validationError != null) {
            uiState = uiState.copy(errorMessage = validationError)
            return
        }

        // Copy image to temp file for reliable access
        val tempFileResult = copyImageToTempFile(imageUri)
        if (tempFileResult.isFailure) {
            uiState = uiState.copy(
                errorMessage = "Failed to prepare image for upload. Please try again."
            )
            return
        }

        val tempFile = tempFileResult.getOrNull() ?: return

        uiState = uiState.copy(isUploading = true, uploadProgress = 0f, errorMessage = null)
        viewModelScope.launch {
            try {
                val result = storageRepository.uploadAadhaarDocumentFromFile(
                    uid = uid,
                    documentType = "back",
                    imageFile = tempFile,
                    onProgress = { progress ->
                        uiState = uiState.copy(uploadProgress = progress.toFloat())
                    }
                )

                result.onSuccess { url ->
                    uiState = uiState.copy(
                        aadhaarBackUrl = url,
                        aadhaarBackUploaded = true,
                        isUploading = false,
                        uploadProgress = 0f,
                        errorMessage = null
                    )
                    saveDocumentUrls()
                }.onFailure { exception ->
                    uiState = uiState.copy(
                        isUploading = false,
                        uploadProgress = 0f,
                        errorMessage = exception.message ?: "Upload failed. Please try again."
                    )
                }
            } finally {
                // Clean up temp file
                try {
                    tempFile.delete()
                    android.util.Log.d("OnboardingViewModel", "Cleaned up temp file: ${tempFile.absolutePath}")
                } catch (e: Exception) {
                    android.util.Log.w("OnboardingViewModel", "Failed to clean up temp file: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Deletes the front Aadhaar image and allows re-upload.
     */
    fun deleteAadhaarFront() {
        uiState = uiState.copy(
            aadhaarFrontUrl = "",
            aadhaarFrontUploaded = false,
            errorMessage = null
        )
        // Optionally delete from Firestore
        viewModelScope.launch {
            firestoreRepository.saveDocumentUrls(uid, "", uiState.aadhaarBackUrl)
        }
    }
    
    /**
     * Deletes the back Aadhaar image and allows re-upload.
     */
    fun deleteAadhaarBack() {
        uiState = uiState.copy(
            aadhaarBackUrl = "",
            aadhaarBackUploaded = false,
            errorMessage = null
        )
        // Optionally delete from Firestore
        viewModelScope.launch {
            firestoreRepository.saveDocumentUrls(uid, uiState.aadhaarFrontUrl, "")
        }
    }
    
    /**
     * Validates image before upload.
     * Checks format and file size.
     * @param imageUri The image URI to validate
     * @return Error message if validation fails, null if valid
     */
    private fun validateImage(imageUri: Uri): String? {
        return try {
            android.util.Log.d("OnboardingViewModel", "Validating image URI: $imageUri")

            // Check if URI is valid
            if (imageUri.toString().isEmpty()) {
                return "Invalid image URI. Please select an image again."
            }

            // Try to take persistable URI permission if possible
            try {
                context.contentResolver.takePersistableUriPermission(
                    imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                android.util.Log.d("OnboardingViewModel", "Took persistable URI permission")
            } catch (e: Exception) {
                android.util.Log.d("OnboardingViewModel", "Could not take persistable permission: ${e.message}")
                // This is okay, not all URIs support persistable permissions
            }

            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return "Cannot open image file. The selected image may not be accessible. Please try selecting the image again from your gallery."

            android.util.Log.d("OnboardingViewModel", "Successfully opened input stream")

            // Check file size (max 10MB before compression)
            val fileSize = inputStream.available().toLong()
            inputStream.close()

            android.util.Log.d("OnboardingViewModel", "Image file size: ${fileSize / 1024}KB")

            if (fileSize > 10 * 1024 * 1024) { // 10MB
                return "Image file is too large. Maximum size is 10MB. Please select a smaller image."
            }

            // Check MIME type
            val mimeType = context.contentResolver.getType(imageUri)
            android.util.Log.d("OnboardingViewModel", "Image MIME type: $mimeType")

            if (mimeType == null || !mimeType.startsWith("image/")) {
                return "Please select a valid image file (JPG, PNG, etc.)"
            }

            android.util.Log.d("OnboardingViewModel", "Image validation passed")
            null // Valid
        } catch (e: Exception) {
            android.util.Log.e("OnboardingViewModel", "Image validation error: ${e.message}", e)
            "Error accessing image: ${e.message ?: "Unknown error"}. Please try selecting the image again."
        }
    }

    /**
     * Copy image data to a temporary file to ensure reliable access during upload
     */
    private fun copyImageToTempFile(imageUri: Uri): Result<java.io.File> {
        return try {
            android.util.Log.d("OnboardingViewModel", "Copying image to temp file: $imageUri")

            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return Result.failure(Exception("Cannot open image file for copying"))

            val tempFile = java.io.File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")

            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            android.util.Log.d("OnboardingViewModel", "Image copied to temp file: ${tempFile.absolutePath}")
            Result.success(tempFile)
        } catch (e: Exception) {
            android.util.Log.e("OnboardingViewModel", "Failed to copy image to temp file: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun uploadProfilePhoto(imageUri: Uri) {
        // Verify user is authenticated
        val currentUid = FirebaseProvider.auth.currentUser?.uid
        if (currentUid == null || currentUid != uid) {
            uiState = uiState.copy(
                errorMessage = "User not authenticated. Please login again."
            )
            return
        }

        // Validate image before upload
        val validationError = validateImage(imageUri)
        if (validationError != null) {
            uiState = uiState.copy(errorMessage = validationError)
            return
        }

        // Copy image to temp file for reliable access
        val tempFileResult = copyImageToTempFile(imageUri)
        if (tempFileResult.isFailure) {
            uiState = uiState.copy(
                errorMessage = "Failed to prepare image for upload. Please try again."
            )
            return
        }

        val tempFile = tempFileResult.getOrNull() ?: return

        uiState = uiState.copy(isUploading = true, uploadProgress = 0f, errorMessage = null)
        viewModelScope.launch {
            try {
                val result = storageRepository.uploadProfilePhotoFromFile(
                    uid = uid,
                    imageFile = tempFile,
                    onProgress = { progress ->
                        uiState = uiState.copy(uploadProgress = progress.toFloat())
                    }
                )

                result.onSuccess { url ->
                    uiState = uiState.copy(
                        profilePhotoUrl = url,
                        profilePhotoUploaded = true,
                        isUploading = false,
                        uploadProgress = 0f,
                        errorMessage = null
                    )
                    saveDocumentUrls()
                }.onFailure { exception ->
                    uiState = uiState.copy(
                        isUploading = false,
                        uploadProgress = 0f,
                        errorMessage = exception.message ?: "Upload failed. Please try again."
                    )
                }
            } finally {
                // Clean up temp file
                try {
                    tempFile.delete()
                    android.util.Log.d("OnboardingViewModel", "Cleaned up temp file: ${tempFile.absolutePath}")
                } catch (e: Exception) {
                    android.util.Log.w("OnboardingViewModel", "Failed to clean up temp file: ${e.message}")
                }
            }
        }
    }
    
    fun deleteProfilePhoto() {
        uiState = uiState.copy(
            profilePhotoUrl = "",
            profilePhotoUploaded = false,
            errorMessage = null
        )
        // Update Firestore
        viewModelScope.launch {
            firestoreRepository.updateProviderData(uid, mapOf("profilePhotoUrl" to ""))
        }
    }

    private fun saveDocumentUrls() {
        viewModelScope.launch {
            firestoreRepository.saveDocumentUrls(
                uid,
                uiState.aadhaarFrontUrl,
                uiState.aadhaarBackUrl
            )
            // Also save profile photo URL
            firestoreRepository.updateProviderData(uid, mapOf("profilePhotoUrl" to uiState.profilePhotoUrl))
        }
    }

    // Step 5 functions
    private var isSubmitting = false // Prevent multiple submissions
    
    fun submitOnboarding() {
        // Prevent multiple submissions
        if (isSubmitting || uiState.isLoading) {
            return
        }
        
        // Allow resubmission if rejected
        if (uiState.onboardingStatus == "SUBMITTED" && uiState.approvalStatus != "REJECTED") {
            return
        }
        
        // VALIDATION: Comprehensive validation before submission
        val validationErrors = mutableListOf<String>()
        
        // Step 1 validation
        val nameValidation = ValidationUtils.validateName(uiState.fullName)
        if (!nameValidation.isValid) {
            validationErrors.add("Step 1: ${nameValidation.errorMessage}")
        }
        if (uiState.gender.isEmpty()) {
            validationErrors.add("Step 1: Please select your gender")
        }
        if (uiState.primaryService.isEmpty()) {
            validationErrors.add("Step 1: Please select a primary service")
        }
        val emailValidation = ValidationUtils.validateEmail(uiState.email)
        if (!emailValidation.isValid) {
            validationErrors.add("Step 1: ${emailValidation.errorMessage}")
        }
        
        // Step 2 validation
        if (uiState.selectedMainService == "Other Services") {
            if (uiState.otherService.isEmpty()) {
                validationErrors.add("Step 2: Please specify your service")
            }
        } else {
            if (uiState.selectedSubServices.isEmpty()) {
                validationErrors.add("Step 2: Please select at least one sub-service")
            }
        }
        
        // Step 3 validation
        val addressValidation = ValidationUtils.validateAddress(
            uiState.state,
            uiState.city,
            uiState.address
        )
        if (!addressValidation.isValid) {
            validationErrors.add("Step 3: ${addressValidation.errorMessage}")
        }
        val pincodeValidation = ValidationUtils.validatePincode(uiState.locationPincode)
        if (!pincodeValidation.isValid) {
            validationErrors.add("Step 3: ${pincodeValidation.errorMessage}")
        }
        val coordinateValidation = ValidationUtils.validateCoordinates(
            uiState.latitude,
            uiState.longitude
        )
        if (!coordinateValidation.isValid) {
            validationErrors.add("Step 3: ${coordinateValidation.errorMessage}")
        }
        val radiusValidation = ValidationUtils.validateServiceRadius(uiState.serviceRadius)
        if (!radiusValidation.isValid) {
            validationErrors.add("Step 3: ${radiusValidation.errorMessage}")
        }
        
        // Step 4 validation
        if (!uiState.aadhaarFrontUploaded) {
            validationErrors.add("Step 4: Please upload Aadhaar front side")
        }
        if (!uiState.aadhaarBackUploaded) {
            validationErrors.add("Step 4: Please upload Aadhaar back side")
        }
        if (!uiState.profilePhotoUploaded) {
            validationErrors.add("Step 4: Please upload profile photo")
        }
        
        // If validation fails, show error and block submission
        if (validationErrors.isNotEmpty()) {
            uiState = uiState.copy(
                errorMessage = validationErrors.joinToString("\n")
            )
            return
        }
        
        // Set submitting flag
        isSubmitting = true
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            // Reset approval status when resubmitting after rejection
            val updateData = mutableMapOf<String, Any>(
                "onboardingStatus" to "SUBMITTED",
                "approvalStatus" to "PENDING",
                "submittedAt" to com.google.firebase.Timestamp.now(),
                "updatedAt" to com.google.firebase.Timestamp.now(),
                "rejectionReason" to "", // Clear rejection reason
                "language" to uiState.language
            )
            
            val result = firestoreRepository.updateProviderData(uid, updateData)
            result.onSuccess {
                uiState = uiState.copy(
                    isLoading = false,
                    isSubmitted = true,
                    onboardingStatus = "SUBMITTED",
                    approvalStatus = "PENDING"
                )
                isSubmitting = false // Reset flag on success
                // Note: FCM notification "Your profile is under review" will be sent by backend
            }.onFailure { exception ->
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Submission failed. Please try again."
                )
                isSubmitting = false // Reset flag on failure
            }
        }
    }
    
    /**
     * Resets the submission flag (called when user cancels confirmation dialog)
     */
    fun resetSubmissionFlag() {
        isSubmitting = false
    }

    /**
     * Parse address components from a full address string
     */
    private fun parseAddressComponents(fullAddress: String): AddressComponents {
        val parts = fullAddress.split(",").map { it.trim() }

        var city = ""
        var state = ""
        var pincode = ""

        // Common patterns for Indian addresses:
        // Format: "Building/Area, City, State, Pincode, Country"

        parts.forEachIndexed { index, part ->
            // Look for pincode (6 digits)
            if (part.matches(Regex("\\d{6}"))) {
                pincode = part
            }
            // Common Indian states
            else if (part.contains("Maharashtra", ignoreCase = true) ||
                     part.contains("Gujarat", ignoreCase = true) ||
                     part.contains("Rajasthan", ignoreCase = true) ||
                     part.contains("Delhi", ignoreCase = true) ||
                     part.contains("Karnataka", ignoreCase = true) ||
                     part.contains("Tamil Nadu", ignoreCase = true) ||
                     part.contains("Uttar Pradesh", ignoreCase = true) ||
                     part.contains("West Bengal", ignoreCase = true) ||
                     part.contains("Punjab", ignoreCase = true) ||
                     part.contains("Haryana", ignoreCase = true) ||
                     part.contains("Madhya Pradesh", ignoreCase = true) ||
                     part.contains("Bihar", ignoreCase = true) ||
                     part.contains("Andhra Pradesh", ignoreCase = true) ||
                     part.contains("Telangana", ignoreCase = true) ||
                     part.contains("Kerala", ignoreCase = true) ||
                     part.contains("Odisha", ignoreCase = true) ||
                     part.contains("Chhattisgarh", ignoreCase = true) ||
                     part.contains("Jharkhand", ignoreCase = true) ||
                     part.contains("Uttarakhand", ignoreCase = true) ||
                     part.contains("Himachal Pradesh", ignoreCase = true) ||
                     part.contains("Jammu and Kashmir", ignoreCase = true) ||
                     part.contains("Goa", ignoreCase = true) ||
                     part.contains("Puducherry", ignoreCase = true) ||
                     part.contains("Chandigarh", ignoreCase = true) ||
                     part.contains("Dadra and Nagar Haveli", ignoreCase = true) ||
                     part.contains("Daman and Diu", ignoreCase = true) ||
                     part.contains("Lakshadweep", ignoreCase = true) ||
                     part.contains("Andaman and Nicobar", ignoreCase = true)) {
                state = part
            }
            // If not pincode and not state, and we're in the right position, it might be city
            else if (part.isNotEmpty() && city.isEmpty() && part != "India" && !part.contains("India", ignoreCase = true)) {
                // Skip the first part (usually building/area) and country
                if (index > 0 && index < parts.size - 2) {
                    city = part
                }
            }
        }

        return AddressComponents(city, state, pincode)
    }

    /**
     * Extract landmark/area information from address components
     */
    private fun extractLandmarkInfo(address: android.location.Address, fullAddress: String): String {
        // Priority order for landmark information:
        // 1. Thoroughfare (street name)
        // 2. Feature name (building/landmark)
        // 3. Parse from address line (area names between city and pincode)

        // Try thoroughfare first (street name)
        address.thoroughfare?.let { return it }

        // Try feature name (building/landmark name)
        address.featureName?.let { featureName ->
            // Skip plus codes (like V8GQ+8P4) as they're not meaningful landmarks
            if (!featureName.matches(Regex("[A-Z0-9]{4}\\+[A-Z0-9]{2,3}"))) {
                return featureName
            }
        }

        // Parse from full address string
        // Format: "PlusCode, Area1, Area2, City, State, Pincode, Country"
        val parts = fullAddress.split(",").map { it.trim() }

        // Find city and pincode positions
        val cityIndex = parts.indexOfFirst { part ->
            part == address.locality || part == address.subAdminArea
        }
        val pincodeIndex = parts.indexOfFirst { it.matches(Regex("\\d{6}")) }

        if (cityIndex >= 0 && pincodeIndex >= 0 && pincodeIndex > cityIndex) {
            // Extract areas between city and pincode
            val landmarkParts = parts.subList(cityIndex + 1, pincodeIndex)
            val landmark = landmarkParts.joinToString(", ").trim()

            if (landmark.isNotEmpty() && landmark != "India") {
                return landmark
            }
        }

        // Fallback: extract meaningful parts from the beginning
        val meaningfulParts = parts.filter { part ->
            part.isNotEmpty() &&
            part != "India" &&
            !part.matches(Regex("\\d{6}")) && // Not pincode
            !part.contains(address.adminArea ?: "") && // Not state
            !part.contains(address.locality ?: "") && // Not city
            !part.matches(Regex("[A-Z0-9]{4}\\+[A-Z0-9]{2,3}")) // Not plus code
        }

        return meaningfulParts.firstOrNull() ?: ""
    }

    private data class AddressComponents(
        val city: String,
        val state: String,
        val pincode: String
    )
}
