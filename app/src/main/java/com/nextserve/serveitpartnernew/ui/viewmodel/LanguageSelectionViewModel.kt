package com.nextserve.serveitpartnernew.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.utils.LanguageManager
import kotlinx.coroutines.launch

/**
 * UI state for Language Selection screen.
 */
data class LanguageSelectionUiState(
    val selectedLanguageCode: String? = null,
    val availableLanguages: List<LanguageOption> = listOf(
        LanguageOption("en", "English", "🇮🇳"),
        LanguageOption("hi", "हिंदी", "हिं"),
        LanguageOption("mr", "मराठी", "मर")
    ),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Represents a language option.
 */
data class LanguageOption(
    val code: String,
    val displayName: String,
    val icon: String
)

/**
 * ViewModel for Language Selection screen.
 * Handles language selection and persistence (both local and Firestore).
 */
class LanguageSelectionViewModel(
    private val context: android.content.Context
) : ViewModel() {
    private val firestoreRepository = FirestoreRepository(FirebaseProvider.firestore)
    
    var uiState by mutableStateOf(
        LanguageSelectionUiState(
            selectedLanguageCode = LanguageManager.getSavedLanguage(context)
        )
    )
        private set

    /**
     * Selects a language and saves it to both local storage and Firestore.
     * @param languageCode The language code to select
     */
    fun selectLanguage(languageCode: String) {
        uiState = uiState.copy(
            selectedLanguageCode = languageCode,
            isSaving = true,
            errorMessage = null
        )
        
        // Save to local storage immediately
        LanguageManager.applyLanguage(context, languageCode)
        
        // Save to Firestore if user is authenticated
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    firestoreRepository.updateProviderData(
                        currentUser.uid,
                        mapOf("language" to languageCode)
                    )
                    uiState = uiState.copy(isSaving = false)
                } catch (e: Exception) {
                    // Language is already saved locally, so this is not critical
                    uiState = uiState.copy(
                        isSaving = false,
                        errorMessage = "Failed to save language to server, but saved locally"
                    )
                }
            }
        } else {
            // User not authenticated yet - just save locally
            uiState = uiState.copy(isSaving = false)
        }
    }

    /**
     * Checks if a language is selected.
     * @return true if a language is selected, false otherwise
     */
    fun isLanguageSelected(): Boolean {
        return uiState.selectedLanguageCode != null
    }
}

