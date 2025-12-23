package com.nextserve.serveitpartnernew.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.PhoneAuthCredential
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.repository.AuthRepository
import kotlinx.coroutines.launch

data class LoginUiState(
    val phoneNumber: String = "",
    val isPhoneNumberValid: Boolean = false,
    val errorMessage: String? = null,
    val isSendingOtp: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository(
        FirebaseProvider.auth,
        null // Activity will be passed from screen
    )
) : ViewModel() {
    var uiState by mutableStateOf(LoginUiState())
        private set

    var onOtpSent: ((String) -> Unit)? = null
    var onAutoVerified: ((PhoneAuthCredential) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var autoVerificationCredential: PhoneAuthCredential? = null

    fun updatePhoneNumber(phoneNumber: String) {
        val cleaned = phoneNumber.filter { it.isDigit() }
        val isValid = cleaned.length == 10
        
        uiState = uiState.copy(
            phoneNumber = cleaned,
            isPhoneNumberValid = isValid,
            errorMessage = if (cleaned.isNotEmpty() && !isValid) {
                "Please enter a valid 10-digit mobile number"
            } else null
        )
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }

    fun sendOtp(activity: android.app.Activity?) {
        if (!uiState.isPhoneNumberValid) return

        uiState = uiState.copy(isSendingOtp = true, errorMessage = null)

        val repository = if (activity != null) {
            AuthRepository(FirebaseProvider.auth, activity)
        } else {
            authRepository
        }

        viewModelScope.launch {
            repository.sendOtp(
                phoneNumber = uiState.phoneNumber,
                onVerificationComplete = { credential ->
                    uiState = uiState.copy(isSendingOtp = false)
                    autoVerificationCredential = credential
                    onAutoVerified?.invoke(credential)
                },
                onCodeSent = { verificationId, _ ->
                    uiState = uiState.copy(isSendingOtp = false)
                    onOtpSent?.invoke(verificationId)
                },
                onError = { error ->
                    uiState = uiState.copy(
                        isSendingOtp = false,
                        errorMessage = error
                    )
                    onError?.invoke(error)
                }
            )
        }
    }
}

