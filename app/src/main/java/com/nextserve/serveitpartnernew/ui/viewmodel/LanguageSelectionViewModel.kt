package com.nextserve.serveitpartnernew.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.nextserve.serveitpartnernew.utils.LanguageManager

/**
 * UI state for Language Selection screen.
 */
data class LanguageSelectionUiState(
    val selectedLanguageCode: String? = null,
    val availableLanguages: List<LanguageOption> = listOf(
        LanguageOption("en", "English", "🇮🇳"),
        LanguageOption("hi", "हिंदी", "हिं"),
        LanguageOption("mr", "मराठी", "मर")
    )
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
 * Handles language selection and persistence.
 */
class LanguageSelectionViewModel(
    private val context: android.content.Context
) : ViewModel() {
    var uiState by mutableStateOf(
        LanguageSelectionUiState(
            selectedLanguageCode = LanguageManager.getSavedLanguage(context)
        )
    )
        private set

    /**
     * Selects a language and saves it.
     * @param languageCode The language code to select
     */
    fun selectLanguage(languageCode: String) {
        uiState = uiState.copy(selectedLanguageCode = languageCode)
        LanguageManager.applyLanguage(context, languageCode)
    }

    /**
     * Checks if a language is selected.
     * @return true if a language is selected, false otherwise
     */
    fun isLanguageSelected(): Boolean {
        return uiState.selectedLanguageCode != null
    }
}

