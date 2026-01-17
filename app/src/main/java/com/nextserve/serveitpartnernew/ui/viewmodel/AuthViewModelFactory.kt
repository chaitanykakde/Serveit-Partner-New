package com.nextserve.serveitpartnernew.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nextserve.serveitpartnernew.data.repository.AuthRepository
import com.nextserve.serveitpartnernew.data.store.OtpSessionStore
import com.nextserve.serveitpartnernew.data.store.SavedStateOtpSessionStore

/**
 * Factory for AuthViewModel to provide SavedStateHandle and Context dependencies.
 */
class AuthViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            val authRepository = AuthRepository()
            val savedStateHandle = androidx.lifecycle.SavedStateHandle()
            val sessionStore = SavedStateOtpSessionStore(savedStateHandle)

            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(
                savedStateHandle = savedStateHandle,
                authRepository = authRepository,
                sessionStore = sessionStore,
                context = context
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
