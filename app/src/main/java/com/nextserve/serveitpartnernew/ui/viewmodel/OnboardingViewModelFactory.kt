package com.nextserve.serveitpartnernew.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider

/**
 * Factory for creating OnboardingViewModel with required dependencies.
 * Provides uid from current Firebase user and context.
 */
class OnboardingViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            // Get current user ID from FirebaseAuth
            val uid = FirebaseProvider.auth.currentUser?.uid
                ?: throw IllegalStateException("User must be authenticated to access onboarding")

            return OnboardingViewModel(uid, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
