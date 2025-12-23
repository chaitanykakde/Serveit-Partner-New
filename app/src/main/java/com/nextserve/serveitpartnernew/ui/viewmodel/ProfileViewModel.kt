package com.nextserve.serveitpartnernew.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.model.ProviderData
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val providerData: ProviderData? = null,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val uid: String,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(FirebaseProvider.firestore)
) : ViewModel() {
    
    var uiState by mutableStateOf(ProfileUiState())
        private set
    
    init {
        loadProfileData()
    }
    
    private fun loadProfileData() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = firestoreRepository.getProviderData(uid)
            result.onSuccess { providerData ->
                uiState = uiState.copy(
                    isLoading = false,
                    providerData = providerData
                )
            }.onFailure { exception ->
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Failed to load profile"
                )
            }
        }
    }
    
    fun refreshProfile() {
        loadProfileData()
    }
    
    class Factory(private val uid: String) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                return ProfileViewModel(uid) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

