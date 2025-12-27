package com.nextserve.serveitpartnernew.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory for creating LanguageSelectionViewModel with Context dependency.
 */
class LanguageSelectionViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LanguageSelectionViewModel::class.java)) {
            return LanguageSelectionViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

