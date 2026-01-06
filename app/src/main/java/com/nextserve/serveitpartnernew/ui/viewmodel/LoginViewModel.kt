package com.nextserve.serveitpartnernew.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.PhoneAuthCredential
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.repository.AuthRepository
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import com.nextserve.serveitpartnernew.utils.ErrorMapper
import com.nextserve.serveitpartnernew.utils.NetworkMonitor
import com.nextserve.serveitpartnernew.utils.PhoneNumberFormatter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * UI state for Login screen.
 */
data class LoginUiState(
    val phoneNumber: String = "",
    val isPhoneNumberValid: Boolean = false,
    val errorMessage: String? = null,
    val isSendingOtp: Boolean = false,
    val verificationId: String? = null,
    val resendToken: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken? = null,
    val isOffline: Boolean = false
)

/**
 * ViewModel for Login screen.
 * Handles phone number validation and OTP sending.
 */
class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository(
        FirebaseProvider.auth,
        null // Activity will be passed from screen
    ),
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(
        FirebaseProvider.firestore
    ),
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {
    var uiState by mutableStateOf(LoginUiState())
        private set

    var onOtpSent: ((String, String, com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken?) -> Unit)? = null
    var onAutoVerified: ((String) -> Unit)? = null // UID callback for auto-verification
    var onError: ((String) -> Unit)? = null

    private var lastClickTime: Long = 0
    private val CLICK_DEBOUNCE_MS = 1000L // 1 second debounce

    init {
        // Monitor network connectivity if NetworkMonitor is provided
        networkMonitor?.let { monitor ->
            viewModelScope.launch {
                monitor.connectivityFlow().collect { isConnected ->
                    uiState = uiState.copy(isOffline = !isConnected)
                    if (!isConnected && uiState.isSendingOtp) {
                        uiState = uiState.copy(
                            isSendingOtp = false,
                            errorMessage = "No internet connection. Please check your network and try again."
                        )
                    }
                }
            }
        }
    }

    /**
     * Updates phone number and validates it.
     * @param phoneNumber The phone number to update
     */
    fun updatePhoneNumber(phoneNumber: String) {
        val cleaned = PhoneNumberFormatter.cleanPhoneNumber(phoneNumber)
        val isValid = PhoneNumberFormatter.isValidIndianPhoneNumber(cleaned)
        
        uiState = uiState.copy(
            phoneNumber = cleaned,
            isPhoneNumberValid = isValid,
            errorMessage = if (cleaned.isNotEmpty() && !isValid) {
                PhoneNumberFormatter.getPhoneNumberErrorMessage(cleaned)
            } else null
        )
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }

    /**
     * Sends OTP to the phone number.
     * Includes rate limiting to prevent multiple rapid clicks.
     * @param activity The activity for reCAPTCHA verification
     */
    fun sendOtp(activity: android.app.Activity?) {
        if (!uiState.isPhoneNumberValid) return

        // Check offline status
        if (uiState.isOffline || (networkMonitor != null && !networkMonitor.isConnected())) {
            uiState = uiState.copy(
                errorMessage = "No internet connection. Please check your network and try again."
            )
            return
        }

        // Rate limiting: prevent rapid clicks
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < CLICK_DEBOUNCE_MS) {
            return
        }
        lastClickTime = currentTime

        if (uiState.isSendingOtp) return // Already sending

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
                    // Auto-verification: verify immediately
                    handleAutoVerification(credential, activity)
                },
                onCodeSent = { verificationId, resendToken ->
                    uiState = uiState.copy(
                        isSendingOtp = false,
                        verificationId = verificationId,
                        resendToken = resendToken
                    )
                    onOtpSent?.invoke(uiState.phoneNumber, verificationId, resendToken)
                },
                onError = { error ->
                    val friendlyError = ErrorMapper.getErrorMessage(Exception(error))
                    uiState = uiState.copy(
                        isSendingOtp = false,
                        errorMessage = friendlyError
                    )
                    onError?.invoke(friendlyError)
                }
            )
        }
    }

    /**
     * Handles auto-verification when Firebase automatically verifies the phone.
     * @param credential The auto-verification credential
     * @param activity The activity context
     */
    private fun handleAutoVerification(
        credential: PhoneAuthCredential,
        activity: android.app.Activity?
    ) {
        viewModelScope.launch {
            try {
                val result = authRepository.verifyOtpWithCredential(credential)
                result.onSuccess { uid ->
                    // Create or update provider document
                    val providerResult = firestoreRepository.getProviderData(uid)
                    providerResult.onSuccess { providerData ->
                        if (providerData == null) {
                            val formattedPhone = PhoneNumberFormatter.formatPhoneNumber(uiState.phoneNumber)
                            android.util.Log.d("LoginViewModel", "📱 Creating new provider document with phone: $formattedPhone")
                            firestoreRepository.createProviderDocument(uid, formattedPhone)
                        } else {
                            if (providerData.phoneNumber.isEmpty()) {
                                val formattedPhone = PhoneNumberFormatter.formatPhoneNumber(uiState.phoneNumber)
                                android.util.Log.d("LoginViewModel", "📱 Updating existing provider with phone: $formattedPhone")
                                firestoreRepository.updateProviderData(uid, mapOf("phoneNumber" to formattedPhone))
                            } else {
                                android.util.Log.d("LoginViewModel", "📱 Provider already has phone: ${providerData.phoneNumber}")
                            }
                            firestoreRepository.updateLastLogin(uid)
                        }
                    }
                    
                    // Save FCM token (non-blocking, failure is not critical)
                    try {
                        com.nextserve.serveitpartnernew.data.fcm.FcmTokenManager.getAndSaveToken(uid)
                    } catch (e: Exception) {
                        // FCM token save failure is not critical, continue anyway
                    }
                    
                    // Check if language is already set
                    val languageCheck = firestoreRepository.getProviderData(uid)
                    languageCheck.onSuccess { providerData ->
                        val language = providerData?.language?.ifEmpty { null }
                        uiState = uiState.copy(isSendingOtp = false)
                        // Pass language info to callback so navigation can decide
                        onAutoVerified?.invoke(uid)
                    }.onFailure {
                        uiState = uiState.copy(isSendingOtp = false)
                        onAutoVerified?.invoke(uid)
                    }
                }.onFailure { exception ->
                    val friendlyError = ErrorMapper.getErrorMessage(exception)
                    uiState = uiState.copy(
                        isSendingOtp = false,
                        errorMessage = friendlyError
                    )
                    onError?.invoke(friendlyError)
                }
            } catch (e: Exception) {
                val friendlyError = ErrorMapper.getErrorMessage(e)
                uiState = uiState.copy(
                    isSendingOtp = false,
                    errorMessage = friendlyError
                )
                onError?.invoke(friendlyError)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        onOtpSent = null
        onAutoVerified = null
        onError = null
    }
}

