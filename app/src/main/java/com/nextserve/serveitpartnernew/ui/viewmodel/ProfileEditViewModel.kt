package com.nextserve.serveitpartnernew.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.model.MainServiceModel
import com.nextserve.serveitpartnernew.data.model.ProviderData
import com.nextserve.serveitpartnernew.data.model.SubServiceModel
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import com.nextserve.serveitpartnernew.data.repository.LocationData
import com.nextserve.serveitpartnernew.data.repository.LocationRepository
import com.nextserve.serveitpartnernew.data.repository.StorageRepository
import kotlinx.coroutines.launch

data class ProfileEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val providerData: ProviderData? = null,
    val mainServices: List<MainServiceModel> = emptyList(),
    val subServices: List<SubServiceModel> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ProfileEditViewModel(
    private val uid: String,
    private val firestoreRepository: FirestoreRepository,
    private val storageRepository: StorageRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    var uiState by mutableStateOf(ProfileEditUiState())
        private set

    init {
        loadProfile()
    }

    fun loadProfile() {
        uiState = uiState.copy(isLoading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            val result = firestoreRepository.getProviderData(uid)
            result.onSuccess { data ->
                uiState = uiState.copy(
                    isLoading = false,
                    providerData = data
                )
                val gender = data?.gender ?: ""
                if (gender.isNotEmpty()) {
                    loadMainServices(gender)
                }
            }.onFailure { e ->
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load profile"
                )
            }
        }
    }

    fun updateBasicInfo(fullName: String, email: String, gender: String) {
        performSave(
            data = mapOf(
                "fullName" to fullName,
                "email" to email,
                "gender" to gender
            )
        ) {
            val current = uiState.providerData
            uiState = uiState.copy(
                providerData = current?.copy(
                    fullName = fullName,
                    email = email,
                    gender = gender
                ),
                successMessage = "Basic info updated"
            )
        }
    }

    fun updateServices(
        gender: String,
        mainService: String,
        subServices: List<String>,
        otherService: String = ""
    ) {
        performSave(
            data = mapOf(
                "gender" to gender,
                "selectedMainService" to mainService,
                "selectedSubServices" to subServices,
                "otherService" to otherService
            )
        ) {
            val current = uiState.providerData
            uiState = uiState.copy(
                providerData = current?.copy(
                    gender = gender,
                    selectedMainService = mainService,
                    selectedSubServices = subServices,
                    otherService = otherService
                ),
                successMessage = "Services updated"
            )
        }
    }

    fun updateAddress(
        state: String,
        city: String,
        address: String,
        fullAddress: String,
        pincode: String,
        serviceRadius: Double,
        latitude: Double?,
        longitude: Double?
    ) {
        performSave(
            data = mapOf(
                "state" to state,
                "city" to city,
                "address" to address,
                "fullAddress" to fullAddress,
                "pincode" to pincode,
                "serviceRadius" to serviceRadius,
                "latitude" to latitude,
                "longitude" to longitude
            )
        ) {
            val current = uiState.providerData
            uiState = uiState.copy(
                providerData = current?.copy(
                    state = state,
                    city = city,
                    address = address,
                    fullAddress = fullAddress,
                    pincode = pincode,
                    serviceRadius = serviceRadius,
                    latitude = latitude,
                    longitude = longitude
                ),
                successMessage = "Address updated"
            )
        }
    }

    fun updatePreferences(languageCode: String, notificationsEnabled: Boolean, context: android.content.Context) {
        // Apply language immediately
        com.nextserve.serveitpartnernew.utils.LanguageManager.applyLanguage(context, languageCode)
        
        performSave(
            data = mapOf(
                "language" to languageCode,
                "notificationsEnabled" to notificationsEnabled
            )
        ) {
            val current = uiState.providerData
            uiState = uiState.copy(
                providerData = current?.copy(language = languageCode),
                successMessage = "Preferences saved"
            )
        }
    }

    fun uploadDocuments(frontUri: Uri?, backUri: Uri?) {
        if (frontUri == null && backUri == null) return
        uiState = uiState.copy(isSaving = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            try {
                var frontUrl: String? = null
                var backUrl: String? = null

                if (frontUri != null) {
                    frontUrl = storageRepository.uploadAadhaarDocument(
                        uid = uid,
                        documentType = "front",
                        imageUri = frontUri,
                        onProgress = {}
                    ).getOrThrow()
                }
                if (backUri != null) {
                    backUrl = storageRepository.uploadAadhaarDocument(
                        uid = uid,
                        documentType = "back",
                        imageUri = backUri,
                        onProgress = {}
                    ).getOrThrow()
                }

                val data = mutableMapOf<String, Any>()
                frontUrl?.let { data["aadhaarFrontUrl"] = it }
                backUrl?.let { data["aadhaarBackUrl"] = it }

                if (data.isNotEmpty()) {
                    firestoreRepository.updateProviderData(uid, data).onFailure {
                        throw it
                    }
                }

                val current = uiState.providerData
                uiState = uiState.copy(
                    isSaving = false,
                    providerData = current?.copy(
                        aadhaarFrontUrl = frontUrl ?: current.aadhaarFrontUrl,
                        aadhaarBackUrl = backUrl ?: current.aadhaarBackUrl
                    ),
                    successMessage = "Documents uploaded"
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Failed to upload documents"
                )
            }
        }
    }

    fun loadMainServices(gender: String) {
        viewModelScope.launch {
            val result = firestoreRepository.getMainServices(gender)
            result.onSuccess { list ->
                uiState = uiState.copy(mainServices = list)
            }.onFailure { e ->
                uiState = uiState.copy(errorMessage = e.message ?: "Failed to load services")
            }
        }
    }

    fun loadSubServices(gender: String, mainService: String) {
        viewModelScope.launch {
            val result = firestoreRepository.getSubServices(gender, mainService)
            result.onSuccess { list ->
                uiState = uiState.copy(subServices = list)
            }.onFailure { e ->
                uiState = uiState.copy(errorMessage = e.message ?: "Failed to load sub services")
            }
        }
    }

    fun useCurrentLocation(onResult: (Result<LocationData>) -> Unit) {
        viewModelScope.launch {
            val result = locationRepository.getCurrentLocationWithAddress()
            result.onSuccess { onResult(Result.success(it)) }
                .onFailure { onResult(Result.failure(it)) }
        }
    }

    private fun performSave(data: Map<String, Any?>, onSuccess: () -> Unit) {
        uiState = uiState.copy(isSaving = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            val filtered = data
                .filterValues { it != null }
                .mapValues { it.value as Any }
            val result = firestoreRepository.updateProviderData(uid, filtered)
            result.onSuccess {
                uiState = uiState.copy(isSaving = false)
                onSuccess()
            }.onFailure { e ->
                uiState = uiState.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Failed to save"
                )
            }
        }
    }

    companion object {
        fun factory(context: Context, uid: String): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ProfileEditViewModel::class.java)) {
                        val firestoreRepo = FirestoreRepository(FirebaseProvider.firestore)
                        val storageRepo = StorageRepository(FirebaseProvider.storage, context.applicationContext)
                        val locationRepo = LocationRepository(context.applicationContext)
                        @Suppress("UNCHECKED_CAST")
                        return ProfileEditViewModel(uid, firestoreRepo, storageRepo, locationRepo) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

