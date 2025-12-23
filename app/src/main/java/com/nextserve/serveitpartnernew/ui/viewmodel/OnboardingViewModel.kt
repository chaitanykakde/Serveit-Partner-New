package com.nextserve.serveitpartnernew.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.model.MainServiceModel
import com.nextserve.serveitpartnernew.data.model.ProviderData
import com.nextserve.serveitpartnernew.data.model.SubServiceModel
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import com.nextserve.serveitpartnernew.data.repository.LocationRepository
import com.nextserve.serveitpartnernew.data.repository.StorageRepository
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
    val isLoadingServices: Boolean = false,
    // Step 2 data
    val selectedMainService: String = "",
    val isInSubServiceView: Boolean = false,
    val availableSubServices: List<SubServiceModel> = emptyList(),
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
    private val locationRepository: LocationRepository = LocationRepository(context),
    private val storageRepository: StorageRepository = StorageRepository(FirebaseProvider.storage, context)
) : ViewModel() {
    var uiState by mutableStateOf(OnboardingUiState())
        private set

    // Main services loaded from Firestore
    private val _mainServices = mutableStateOf<List<MainServiceModel>>(emptyList())
    val mainServices: State<List<MainServiceModel>> = _mainServices

    init {
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
                        isSubmitted = onboardingStatus == "SUBMITTED"
                    )
                    
                    // Load sub-services if primary service is set
                    // Always select all by default when loading from Firestore (user can customize later)
                    if (providerData.primaryService.isNotEmpty() && providerData.gender.isNotEmpty()) {
                        // Clear any existing selections to ensure all are selected by default
                        uiState = uiState.copy(selectedSubServices = emptySet(), selectedMainService = "")
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
            val newStep = uiState.currentStep + 1
            uiState = uiState.copy(currentStep = newStep)
            saveCurrentStep(newStep)
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
        uiState = uiState.copy(fullName = name)
        saveStep1Data()
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
            }.onFailure { exception ->
                uiState = uiState.copy(
                    isLoadingServices = false,
                    errorMessage = "Failed to load services: ${exception.message}"
                )
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
            result.onSuccess { subServices ->
                // Always select all sub-services by default when loading a new service
                // Only preserve selections if it's the same service and we have existing selections
                val isSameService = uiState.selectedMainService == mainServiceName
                val currentSelected = if (isSameService && uiState.selectedSubServices.isNotEmpty()) {
                    // Same service - preserve existing selections
                    uiState.selectedSubServices
                } else {
                    // New service or no existing selections - select all by default
                    subServices.map { it.name }.toSet()
                }
                
                val allSelected = currentSelected.size == subServices.size && subServices.isNotEmpty()
                
                uiState = uiState.copy(
                    availableSubServices = subServices,
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
        uiState = uiState.copy(email = email)
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
                    "email" to uiState.email
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
        val allSelected = current.size == uiState.availableSubServices.size
        uiState = uiState.copy(
            selectedSubServices = current,
            isSelectAllChecked = allSelected
        )
        saveStep2Data()
    }

    fun toggleSelectAll() {
        if (uiState.isSelectAllChecked) {
            uiState = uiState.copy(
                selectedSubServices = emptySet(),
                isSelectAllChecked = false
            )
        } else {
            val allSubServiceNames = uiState.availableSubServices.map { it.name }.toSet()
            uiState = uiState.copy(
                selectedSubServices = allSubServiceNames,
                isSelectAllChecked = true
            )
        }
        saveStep2Data()
    }

    fun updateOtherService(service: String) {
        uiState = uiState.copy(otherService = service)
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
        uiState = uiState.copy(city = city)
        saveStep3Data()
    }

    fun updateState(state: String) {
        uiState = uiState.copy(state = state)
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
        val filtered = pincode.filter { it.isDigit() }.take(6)
        uiState = uiState.copy(locationPincode = filtered)
        saveStep3Data()
    }

    fun updateServiceRadius(radius: Float) {
        uiState = uiState.copy(serviceRadius = radius)
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
                val locationResult = locationRepository.getCurrentLocationWithAddress()
                locationResult.onSuccess { locationData ->
                    uiState = uiState.copy(
                        isLocationLoading = false,
                        state = locationData.state,
                        city = locationData.city,
                        address = locationData.address,
                        fullAddress = locationData.fullAddress,
                        locationPincode = locationData.pincode,
                        latitude = locationData.latitude,
                        longitude = locationData.longitude
                    )
                    saveStep3Data()
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
        
        uiState = uiState.copy(isUploading = true, uploadProgress = 0f, errorMessage = null)
        viewModelScope.launch {
            val result = storageRepository.uploadAadhaarDocument(
                uid = uid,
                documentType = "front",
                imageUri = imageUri,
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
        
        uiState = uiState.copy(isUploading = true, uploadProgress = 0f, errorMessage = null)
        viewModelScope.launch {
            val result = storageRepository.uploadAadhaarDocument(
                uid = uid,
                documentType = "back",
                imageUri = imageUri,
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
        }
    }

    private fun saveDocumentUrls() {
        viewModelScope.launch {
            firestoreRepository.saveDocumentUrls(
                uid,
                uiState.aadhaarFrontUrl,
                uiState.aadhaarBackUrl
            )
        }
    }

    // Step 5 functions
    fun submitOnboarding() {
        // Allow resubmission if rejected
        if (uiState.onboardingStatus == "SUBMITTED" && uiState.approvalStatus != "REJECTED") return
        
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            // Reset approval status when resubmitting after rejection
            val updateData = mutableMapOf<String, Any>(
                "onboardingStatus" to "SUBMITTED",
                "approvalStatus" to "PENDING",
                "submittedAt" to com.google.firebase.Timestamp.now(),
                "updatedAt" to com.google.firebase.Timestamp.now(),
                "rejectionReason" to "" // Clear rejection reason
            )
            
            val result = firestoreRepository.updateProviderData(uid, updateData)
            result.onSuccess {
                uiState = uiState.copy(
                    isLoading = false,
                    isSubmitted = true,
                    onboardingStatus = "SUBMITTED",
                    approvalStatus = "PENDING"
                )
                // Note: FCM notification "Your profile is under review" will be sent by backend
            }.onFailure { exception ->
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Submission failed. Please try again."
                )
            }
        }
    }
}
