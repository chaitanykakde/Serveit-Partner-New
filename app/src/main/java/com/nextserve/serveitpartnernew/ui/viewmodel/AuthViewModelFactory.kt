package com.nextserve.serveitpartnernew.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nextserve.serveitpartnernew.data.repository.AuthRepository
import com.nextserve.serveitpartnernew.data.store.OtpSessionStore
import com.nextserve.serveitpartnernew.data.store.SavedStateOtpSessionStore

/**
 * Factory for AuthViewModel to provide SavedStateHandle dependency.
 */
class AuthViewModelFactory : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            // For now, create with empty SavedStateHandle
            // In production, this should be handled by SavedStateViewModelFactory
            val authRepository = AuthRepository()
            val savedStateHandle = androidx.lifecycle.SavedStateHandle()
            val sessionStore = SavedStateOtpSessionStore(savedStateHandle)

            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(savedStateHandle, authRepository, sessionStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
