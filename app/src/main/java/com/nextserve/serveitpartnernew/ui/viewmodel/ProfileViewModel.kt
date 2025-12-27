package com.nextserve.serveitpartnernew.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.model.ProviderData
import com.nextserve.serveitpartnernew.data.model.ProviderStats
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isLoadingStats: Boolean = true,
    val providerData: ProviderData? = null,
    val stats: ProviderStats? = null,
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
        loadStats()
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
    
    private fun loadStats() {
        uiState = uiState.copy(isLoadingStats = true)
        viewModelScope.launch {
            // Placeholder stats calculation
            // TODO: Replace with actual Firestore queries when jobs/ratings collections are available
            val placeholderStats = calculatePlaceholderStats()
            uiState = uiState.copy(
                isLoadingStats = false,
                stats = placeholderStats
            )
        }
    }
    
    private fun calculatePlaceholderStats(): ProviderStats {
        // Placeholder calculation based on provider data
        // In production, this would query jobs collection and calculate real stats
        val baseRating = 4.5
        val baseJobs = 0
        val baseEarnings = 0.0
        
        // Add some variation based on approval status
        val rating = when (uiState.providerData?.approvalStatus) {
            "APPROVED" -> baseRating + 0.3 // Add slight boost for approved providers
            else -> baseRating
        }
        
        return ProviderStats(
            rating = rating.coerceIn(0.0, 5.0),
            totalJobs = baseJobs,
            totalEarnings = baseEarnings,
            completedJobs = baseJobs,
            pendingJobs = 0,
            cancelledJobs = 0
        )
    }
    
    fun refreshProfile() {
        loadProfileData()
        loadStats()
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

