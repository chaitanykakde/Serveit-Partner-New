package com.nextserve.serveitpartnernew.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.model.NotificationPreferences
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationPreferencesUiState(
    val preferences: NotificationPreferences = NotificationPreferences(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false
)

class NotificationPreferencesViewModel(
    private val providerId: String,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(FirebaseProvider.firestore)
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationPreferencesUiState())
    val uiState: StateFlow<NotificationPreferencesUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            // For now, load default preferences
            // In production, this would load from Firestore
            val defaultPreferences = NotificationPreferences(providerId = providerId)
            _uiState.value = _uiState.value.copy(
                preferences = defaultPreferences,
                isLoading = false
            )
        }
    }

    fun updateJobOffers(enabled: Boolean) {
        updatePreferences { it.copy(jobOffers = enabled) }
    }

    fun updateJobUpdates(enabled: Boolean) {
        updatePreferences { it.copy(jobUpdates = enabled) }
    }

    fun updateEarningsSummary(enabled: Boolean) {
        updatePreferences { it.copy(earningsSummary = enabled) }
    }

    fun updateVerificationUpdates(enabled: Boolean) {
        updatePreferences { it.copy(verificationUpdates = enabled) }
    }

    fun updatePromotionalOffers(enabled: Boolean) {
        updatePreferences { it.copy(promotionalOffers = enabled) }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        updatePreferences { it.copy(soundEnabled = enabled) }
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        updatePreferences { it.copy(vibrationEnabled = enabled) }
    }

    fun updateQuietHoursEnabled(enabled: Boolean) {
        updatePreferences { it.copy(quietHoursEnabled = enabled) }
    }

    fun updateQuietHoursStart(time: String) {
        updatePreferences { it.copy(quietHoursStart = time) }
    }

    fun updateQuietHoursEnd(time: String) {
        updatePreferences { it.copy(quietHoursEnd = time) }
    }

    fun updatePushNotificationsEnabled(enabled: Boolean) {
        updatePreferences { it.copy(pushNotificationsEnabled = enabled) }
    }

    private fun updatePreferences(update: (NotificationPreferences) -> NotificationPreferences) {
        val currentPrefs = _uiState.value.preferences
        val updatedPrefs = update(currentPrefs)
        _uiState.value = _uiState.value.copy(
            preferences = updatedPrefs,
            saveSuccess = false
        )
    }

    fun savePreferences() {
        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, saveSuccess = false)
        viewModelScope.launch {
            try {
                // Save to Firestore
                val preferencesMap = mapOf(
                    "jobOffers" to _uiState.value.preferences.jobOffers,
                    "jobUpdates" to _uiState.value.preferences.jobUpdates,
                    "earningsSummary" to _uiState.value.preferences.earningsSummary,
                    "verificationUpdates" to _uiState.value.preferences.verificationUpdates,
                    "promotionalOffers" to _uiState.value.preferences.promotionalOffers,
                    "soundEnabled" to _uiState.value.preferences.soundEnabled,
                    "vibrationEnabled" to _uiState.value.preferences.vibrationEnabled,
                    "quietHoursEnabled" to _uiState.value.preferences.quietHoursEnabled,
                    "quietHoursStart" to _uiState.value.preferences.quietHoursStart,
                    "quietHoursEnd" to _uiState.value.preferences.quietHoursEnd,
                    "pushNotificationsEnabled" to _uiState.value.preferences.pushNotificationsEnabled
                )

                firestoreRepository.updateProviderData(providerId, preferencesMap)

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Failed to save preferences"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    companion object {
        fun factory(providerId: String): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(NotificationPreferencesViewModel::class.java)) {
                        return NotificationPreferencesViewModel(providerId) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}
