package com.nextserve.serveitpartnernew.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.repository.AuthRepository
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import com.nextserve.serveitpartnernew.utils.ErrorMapper
import com.nextserve.serveitpartnernew.utils.NetworkMonitor
import com.nextserve.serveitpartnernew.utils.PhoneNumberFormatter
import com.nextserve.serveitpartnernew.utils.ValidationUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * UI state for OTP screen.
 */
data class OtpUiState(
    val otp: String = "",
    val isOtpValid: Boolean = false,
    val phoneNumber: String = "",
    val verificationId: String? = null,
    val resendToken: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken? = null,
    val timeRemaining: Int = 60,
    val canResend: Boolean = false,
    val isVerifying: Boolean = false,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val isOffline: Boolean = false
)

/**
 * ViewModel for OTP verification screen.
 * Handles OTP input, verification, resend, and timer management.
 */
class OtpViewModel(
    private val authRepository: AuthRepository = AuthRepository(
        FirebaseProvider.auth,
        null
    ),
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(
        FirebaseProvider.firestore
    ),
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {
    var uiState by mutableStateOf(OtpUiState())
        private set

    private var timerJob: Job? = null
    private var verificationJob: Job? = null
    var onVerificationSuccess: ((String) -> Unit)? = null // UID callback
    
    // Store activity reference for auto-submit
    private var currentActivity: android.app.Activity? = null

    private var lastResendClickTime: Long = 0
    private val RESEND_DEBOUNCE_MS = 2000L // 2 seconds debounce for resend
    
    /**
     * Sets the activity reference for auto-submit functionality.
     * @param activity The activity context
     */
    fun setActivity(activity: android.app.Activity?) {
        currentActivity = activity
    }

    init {
        // Monitor network connectivity if NetworkMonitor is provided
        networkMonitor?.let { monitor ->
            viewModelScope.launch {
                monitor.connectivityFlow().collect { isConnected ->
                    uiState = uiState.copy(isOffline = !isConnected)
                    if (!isConnected && (uiState.isVerifying || uiState.canResend)) {
                        uiState = uiState.copy(
                            isVerifying = false,
                            errorMessage = "No internet connection. Please check your network and try again."
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        verificationJob?.cancel()
        onVerificationSuccess = null
        currentActivity = null
    }

    /**
     * Updates OTP input and validates it.
     * @param otp The OTP string to update
     */
    fun updateOtp(otp: String) {
        val cleaned = otp.filter { it.isDigit() }
        val validationResult = ValidationUtils.validateOtp(cleaned)
        
        uiState = uiState.copy(
            otp = cleaned,
            isOtpValid = validationResult.isValid,
            errorMessage = if (cleaned.isNotEmpty() && !validationResult.isValid) {
                validationResult.errorMessage
            } else null
        )
        
        // Auto-submit when 6 digits are entered
        if (cleaned.length == 6 && validationResult.isValid) {
            // Trigger auto-verification after a short delay to allow UI to update
            viewModelScope.launch {
                kotlinx.coroutines.delay(300) // Small delay for better UX
                if (uiState.otp.length == 6 && uiState.isOtpValid && !uiState.isVerifying) {
                    verifyOtp(currentActivity) // Use stored activity reference
                }
            }
        }
    }

    /**
     * Sets the phone number.
     * @param phoneNumber The phone number
     */
    fun setPhoneNumber(phoneNumber: String) {
        val cleaned = PhoneNumberFormatter.cleanPhoneNumber(phoneNumber)
        uiState = uiState.copy(phoneNumber = cleaned)
    }

    /**
     * Sets the verification ID and starts the timer.
     * @param verificationId The verification ID from Firebase
     */
    fun setVerificationId(verificationId: String) {
        if (verificationId.isNotEmpty()) {
            uiState = uiState.copy(verificationId = verificationId)
            startTimer()
        }
    }

    /**
     * Sets the resend token.
     * @param resendToken The resend token from Firebase
     */
    fun setResendToken(resendToken: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken?) {
        uiState = uiState.copy(resendToken = resendToken)
    }

    /**
     * Starts the resend timer.
     */
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

    /**
     * Resends OTP to the phone number.
     * Includes rate limiting and proper state management.
     * @param activity The activity for reCAPTCHA verification
     */
    fun resendOtp(activity: android.app.Activity?) {
        if (!uiState.canResend) return

        // Check offline status
        if (uiState.isOffline || (networkMonitor != null && !networkMonitor.isConnected())) {
            uiState = uiState.copy(
                errorMessage = "No internet connection. Please check your network and try again."
            )
            return
        }

        // Rate limiting: prevent rapid clicks
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastResendClickTime < RESEND_DEBOUNCE_MS) {
            return
        }
        lastResendClickTime = currentTime

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
                        resendToken = resendToken,
                        errorMessage = null,
                        retryCount = 0 // Reset retry count on successful resend
                    )
                    startTimer()
                },
                onError = { error ->
                    val friendlyError = ErrorMapper.getErrorMessage(Exception(error))
                    uiState = uiState.copy(errorMessage = friendlyError)
                }
            )
        }
    }

    /**
     * Verifies the OTP code.
     * Handles Firestore operations with proper error handling.
     * @param activity The activity context
     */
    fun verifyOtp(activity: android.app.Activity?) {
        if (!uiState.isOtpValid) return
        
        val verificationId = uiState.verificationId
        if (verificationId == null) {
            uiState = uiState.copy(
                errorMessage = "No verification ID. Please request OTP again."
            )
            return
        }

        // Check offline status
        if (uiState.isOffline || (networkMonitor != null && !networkMonitor.isConnected())) {
            uiState = uiState.copy(
                errorMessage = "No internet connection. Please check your network and try again."
            )
            return
        }

        if (uiState.isVerifying) return // Already verifying

        // Cancel any previous verification
        verificationJob?.cancel()

        uiState = uiState.copy(isVerifying = true, errorMessage = null)

        verificationJob = viewModelScope.launch {
            try {
                val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(
                    verificationId,
                    uiState.otp
                )
                
                val result = authRepository.verifyOtpWithCredential(credential)
                
                result.onSuccess { uid ->
                    // Handle Firestore operations with proper error handling
                    handleFirestoreOperations(uid)
                }.onFailure { exception ->
                    val friendlyError = ErrorMapper.getErrorMessage(exception)
                    uiState = uiState.copy(
                        isVerifying = false,
                        errorMessage = friendlyError,
                        retryCount = uiState.retryCount + 1
                    )
                }
            } catch (e: Exception) {
                val friendlyError = ErrorMapper.getErrorMessage(e)
                uiState = uiState.copy(
                    isVerifying = false,
                    errorMessage = friendlyError,
                    retryCount = uiState.retryCount + 1
                )
            }
        }
    }

    /**
     * Handles Firestore operations after successful authentication.
     * Creates or updates provider document with proper error handling.
     * @param uid The user ID
     */
    private suspend fun handleFirestoreOperations(uid: String) {
        val providerResult = firestoreRepository.getProviderData(uid)
        
        providerResult.onSuccess { providerData ->
            if (providerData == null) {
                // Create new document
                val formattedPhone = PhoneNumberFormatter.formatPhoneNumber(uiState.phoneNumber)
                val createResult = firestoreRepository.createProviderDocument(uid, formattedPhone)
                
                createResult.onSuccess {
                    // Successfully created
                    saveFcmTokenAndNavigate(uid)
                }.onFailure { exception ->
                    val friendlyError = ErrorMapper.getErrorMessage(exception)
                    uiState = uiState.copy(
                        isVerifying = false,
                        errorMessage = "Failed to create profile: $friendlyError"
                    )
                }
            } else {
                // Update existing document
                val updateJobs = mutableListOf<kotlinx.coroutines.Job>()
                
                if (providerData.phoneNumber.isEmpty()) {
                    val formattedPhone = PhoneNumberFormatter.formatPhoneNumber(uiState.phoneNumber)
                    val updateResult = firestoreRepository.updateProviderData(uid, mapOf("phoneNumber" to formattedPhone))
                    updateResult.onFailure { exception ->
                        val friendlyError = ErrorMapper.getErrorMessage(exception)
                        uiState = uiState.copy(
                            isVerifying = false,
                            errorMessage = "Failed to update phone number: $friendlyError"
                        )
                        return
                    }
                }
                
                val lastLoginResult = firestoreRepository.updateLastLogin(uid)
                lastLoginResult.onSuccess {
                    saveFcmTokenAndNavigate(uid)
                }.onFailure { exception ->
                    // Last login update failure is not critical, proceed anyway
                    saveFcmTokenAndNavigate(uid)
                }
            }
        }.onFailure { exception ->
            val friendlyError = ErrorMapper.getErrorMessage(exception)
            uiState = uiState.copy(
                isVerifying = false,
                errorMessage = "Failed to load profile: $friendlyError"
            )
        }
    }

    /**
     * Saves FCM token and navigates to onboarding.
     * @param uid The user ID
     */
    private suspend fun saveFcmTokenAndNavigate(uid: String) {
        // Save FCM token (non-blocking, failure is not critical)
        try {
            com.nextserve.serveitpartnernew.data.fcm.FcmTokenManager.getAndSaveToken(uid)
        } catch (e: Exception) {
            // FCM token save failure is not critical, continue anyway
        }
        
        uiState = uiState.copy(isVerifying = false)
        onVerificationSuccess?.invoke(uid)
    }

    /**
     * Retries the last failed operation.
     * @param activity The activity context
     */
    fun retryVerification(activity: android.app.Activity?) {
        if (uiState.retryCount >= uiState.maxRetries) {
            uiState = uiState.copy(
                errorMessage = "Maximum retry attempts reached. Please request a new OTP."
            )
            return
        }
        verifyOtp(activity)
    }
}

