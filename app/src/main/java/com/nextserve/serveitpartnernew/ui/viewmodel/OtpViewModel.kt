package com.nextserve.serveitpartnernew.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.repository.AuthRepository
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class OtpUiState(
    val otp: String = "",
    val isOtpValid: Boolean = false,
    val phoneNumber: String = "",
    val verificationId: String? = null,
    val resendToken: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken? = null,
    val timeRemaining: Int = 60,
    val canResend: Boolean = false,
    val isVerifying: Boolean = false,
    val errorMessage: String? = null
)

class OtpViewModel(
    private val authRepository: AuthRepository = AuthRepository(
        FirebaseProvider.auth,
        null
    ),
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(
        FirebaseProvider.firestore
    )
) : ViewModel() {
    var uiState by mutableStateOf(OtpUiState())
        private set

    private var timerJob: Job? = null
    var onVerificationSuccess: ((String) -> Unit)? = null // UID callback

    init {
        startTimer()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    fun updateOtp(otp: String) {
        val cleaned = otp.filter { it.isDigit() }
        val isValid = cleaned.length == 6
        
        uiState = uiState.copy(
            otp = cleaned,
            isOtpValid = isValid,
            errorMessage = null
        )
    }

    fun setPhoneNumber(phoneNumber: String) {
        uiState = uiState.copy(phoneNumber = phoneNumber)
    }

    fun setVerificationId(verificationId: String) {
        uiState = uiState.copy(verificationId = verificationId)
    }

    private fun startTimer() {
        timerJob?.cancel()
        uiState = uiState.copy(
            timeRemaining = 60,
            canResend = false
        )
        
        timerJob = viewModelScope.launch {
            var timeLeft = 60
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
                uiState = uiState.copy(
                    timeRemaining = timeLeft,
                    canResend = timeLeft == 0
                )
            }
        }
    }

    fun resendOtp(activity: android.app.Activity?) {
        if (!uiState.canResend) return

        val repository = if (activity != null) {
            AuthRepository(FirebaseProvider.auth, activity)
        } else {
            authRepository
        }

        viewModelScope.launch {
            repository.resendOtp(
                phoneNumber = uiState.phoneNumber,
                resendToken = uiState.resendToken,
                onCodeSent = { verificationId, resendToken ->
                    uiState = uiState.copy(
                        verificationId = verificationId,
                        resendToken = resendToken
                    )
                    startTimer()
                },
                onError = { error ->
                    uiState = uiState.copy(errorMessage = error)
                }
            )
        }
    }

    fun verifyOtp(activity: android.app.Activity?) {
        if (!uiState.isOtpValid) return
        
        val verificationId = uiState.verificationId
        if (verificationId == null) {
            uiState = uiState.copy(
                errorMessage = "No verification ID. Please request OTP again."
            )
            return
        }

        uiState = uiState.copy(isVerifying = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(
                    verificationId,
                    uiState.otp
                )
                
                val result = authRepository.verifyOtpWithCredential(credential)
                
                result.onSuccess { uid ->
                    // Create or update provider document
                    val providerResult = firestoreRepository.getProviderData(uid)
                    providerResult.onSuccess { providerData ->
                        if (providerData == null) {
                            // Create new document with phone number
                            val phoneToSave = if (uiState.phoneNumber.startsWith("+91")) {
                                uiState.phoneNumber
                            } else {
                                "+91${uiState.phoneNumber}"
                            }
                            firestoreRepository.createProviderDocument(uid, phoneToSave)
                        } else {
                            // Update last login and phone number if missing
                            if (providerData.phoneNumber.isEmpty()) {
                                val phoneToSave = if (uiState.phoneNumber.startsWith("+91")) {
                                    uiState.phoneNumber
                                } else {
                                    "+91${uiState.phoneNumber}"
                                }
                                firestoreRepository.updateProviderData(uid, mapOf("phoneNumber" to phoneToSave))
                            }
                            firestoreRepository.updateLastLogin(uid)
                        }
                    }
                    
                    // Save FCM token after successful verification
                    com.nextserve.serveitpartnernew.data.fcm.FcmTokenManager.getAndSaveToken(uid)
                    
                    uiState = uiState.copy(isVerifying = false)
                    onVerificationSuccess?.invoke(uid)
                }.onFailure { exception ->
                    uiState = uiState.copy(
                        isVerifying = false,
                        errorMessage = exception.message ?: "Verification failed"
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isVerifying = false,
                    errorMessage = e.message ?: "Verification failed"
                )
            }
        }
    }

    fun verifyWithCredential(credential: com.google.firebase.auth.PhoneAuthCredential) {
        uiState = uiState.copy(isVerifying = true, errorMessage = null)

        viewModelScope.launch {
            val result = authRepository.verifyOtpWithCredential(credential)
            
            result.onSuccess { uid ->
                val providerResult = firestoreRepository.getProviderData(uid)
                providerResult.onSuccess { providerData ->
                    if (providerData == null) {
                        // Create new document with phone number
                        val phoneToSave = if (uiState.phoneNumber.startsWith("+91")) {
                            uiState.phoneNumber
                        } else {
                            "+91${uiState.phoneNumber}"
                        }
                        firestoreRepository.createProviderDocument(uid, phoneToSave)
                    } else {
                        // Update last login and phone number if missing
                        if (providerData.phoneNumber.isEmpty()) {
                            val phoneToSave = if (uiState.phoneNumber.startsWith("+91")) {
                                uiState.phoneNumber
                            } else {
                                "+91${uiState.phoneNumber}"
                            }
                            firestoreRepository.updateProviderData(uid, mapOf("phoneNumber" to phoneToSave))
                        }
                        firestoreRepository.updateLastLogin(uid)
                    }
                }
                
                uiState = uiState.copy(isVerifying = false)
                onVerificationSuccess?.invoke(uid)
            }.onFailure { exception ->
                uiState = uiState.copy(
                    isVerifying = false,
                    errorMessage = exception.message ?: "Verification failed"
                )
            }
        }
    }
}

