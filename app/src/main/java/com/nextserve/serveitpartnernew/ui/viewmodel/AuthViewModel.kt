package com.nextserve.serveitpartnernew.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.nextserve.serveitpartnernew.data.fcm.FcmTokenManager
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.repository.AuthRepository
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import com.nextserve.serveitpartnernew.utils.ErrorMapper
import com.nextserve.serveitpartnernew.utils.LanguageManager
import com.nextserve.serveitpartnernew.utils.NetworkMonitor
import com.nextserve.serveitpartnernew.utils.PhoneNumberFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Master ViewModel for authentication state management.
 * Single source of truth for all auth-related state and navigation.
 */
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(FirebaseProvider.auth),
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(FirebaseProvider.firestore),
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Uninitialized)
    val authState: StateFlow<AuthState> = _authState

    // OTP-related state
    private var currentVerificationId: String? = null
    private var currentResendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var currentPhoneNumber: String = ""

    // Security & Rate limiting
    private var lastOtpRequestTime: Long = 0
    private val OTP_REQUEST_COOLDOWN_MS = 1000L // 1 second between requests
    private var lastResendTime: Long = 0
    private val RESEND_COOLDOWN_MS = 30000L // 30 seconds between resends
    private var otpRequestCount = 0
    private var lastOtpRequestWindow: Long = 0
    private val OTP_REQUEST_WINDOW_MS = 60000L // 1 minute window
    private val MAX_OTP_REQUESTS_PER_WINDOW = 5 // Max 5 requests per minute
    private var verificationAttemptCount = 0
    private val MAX_VERIFICATION_ATTEMPTS = 3 // Max 3 wrong OTP attempts

    init {
        // Monitor network connectivity
        networkMonitor?.let { monitor ->
            viewModelScope.launch {
                monitor.connectivityFlow().collectLatest { isConnected ->
                    // Handle network state changes if needed
                    // For now, just log the state
                }
            }
        }

        // Listen to Firebase Auth state changes
        setupAuthStateListener()

        // Initialize auth state from current Firebase user
        initializeAuthState()
    }

    /**
     * Setup Firebase Auth state listener for real-time auth changes
     */
    private fun setupAuthStateListener() {
        FirebaseProvider.auth.addAuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            viewModelScope.launch {
                if (currentUser == null) {
                    // User signed out
                    _authState.value = AuthState.LoggedOut
                } else {
                    // User signed in - load their data
                    loadUserData(currentUser.uid)
                }
            }
        }
    }

    /**
     * Initialize authentication state on app start.
     * Firebase AuthStateListener will handle the actual state changes.
     */
    private fun initializeAuthState() {
        // AuthStateListener will handle state changes, just set initial state
        val currentUser = FirebaseProvider.auth.currentUser
        if (currentUser == null) {
            _authState.value = AuthState.LoggedOut
        } else {
            // If user exists, AuthStateListener will call loadUserData
            _authState.value = AuthState.Loading
        }
    }

    /**
     * Load user data and determine auth state
     */
    private suspend fun loadUserData(uid: String) {
        try {
            val providerData = firestoreRepository.getProviderData(uid)

            providerData.onSuccess { data ->
                _authState.value = when {
                    data == null -> AuthState.Authenticated(uid)
                    data.onboardingStatus == "SUBMITTED" && data.approvalStatus == "PENDING" ->
                        AuthState.PendingApproval(uid)
                    data.approvalStatus == "REJECTED" ->
                        AuthState.Rejected(uid, data.rejectionReason)
                    data.approvalStatus == "APPROVED" ->
                        AuthState.Approved
                    data.onboardingStatus == "IN_PROGRESS" ->
                        AuthState.Onboarding(uid, data.currentStep)
                    else -> AuthState.Onboarding(uid, 1)
                }
            }.onFailure { exception ->
                // If we can't load data, assume onboarding needed
                _authState.value = AuthState.Authenticated(uid)
            }
        } catch (e: Exception) {
            // If anything fails, reset to logged out state
            signOut()
            _authState.value = AuthState.LoggedOut
        }
    }

    /**
     * Update phone number and validate it.
     */
    fun updatePhoneNumber(phoneNumber: String) {
        currentPhoneNumber = PhoneNumberFormatter.cleanPhoneNumber(phoneNumber)
    }

    /**
     * Check if current phone number is valid for OTP request.
     */
    fun isPhoneNumberValid(): Boolean {
        return PhoneNumberFormatter.isValidIndianPhoneNumber(currentPhoneNumber)
    }

    /**
     * Send OTP to the entered phone number with security checks.
     */
    fun sendOtp(activity: Activity?) {
        // Input validation
        if (!isPhoneNumberValid()) {
            _authState.value = AuthState.Error("Please enter a valid 10-digit phone number", canRetry = false)
            return
        }

        // Security: Phone number sanitization check
        val sanitizedPhone = currentPhoneNumber.filter { it.isDigit() }
        if (sanitizedPhone != currentPhoneNumber) {
            _authState.value = AuthState.Error("Invalid phone number format", canRetry = false)
            return
        }

        val currentTime = System.currentTimeMillis()

        // Rate limiting: Basic cooldown
        if (currentTime - lastOtpRequestTime < OTP_REQUEST_COOLDOWN_MS) {
            return // Ignore rapid clicks
        }

        // Security: Window-based rate limiting
        if (currentTime - lastOtpRequestWindow > OTP_REQUEST_WINDOW_MS) {
            // Reset window
            otpRequestCount = 0
            lastOtpRequestWindow = currentTime
        }

        if (otpRequestCount >= MAX_OTP_REQUESTS_PER_WINDOW) {
            val remainingTime = ((lastOtpRequestWindow + OTP_REQUEST_WINDOW_MS) - currentTime) / 1000
            _authState.value = AuthState.Error(
                "Too many OTP requests. Please wait ${remainingTime}s before trying again.",
                canRetry = false
            )
            return
        }

        // Update rate limiting counters
        lastOtpRequestTime = currentTime
        otpRequestCount++

        // Check network connectivity
        if (networkMonitor != null && !networkMonitor.isConnected()) {
            _authState.value = AuthState.Error("No internet connection. Please check your network and try again.", canRetry = true)
            return
        }

        _authState.value = AuthState.Loading

        viewModelScope.launch {
            try {
                authRepository.sendOtp(
                    phoneNumber = currentPhoneNumber,
                    onVerificationComplete = { credential ->
                        // Handle auto-verification
                        handleAutoVerification(credential, activity)
                    },
                    onCodeSent = { verificationId, resendToken ->
                        currentVerificationId = verificationId
                        currentResendToken = resendToken
                        lastResendTime = System.currentTimeMillis()
                        _authState.value = AuthState.OtpSent(currentPhoneNumber, verificationId)
                    },
                    onError = { errorMessage ->
                        _authState.value = AuthState.Error(errorMessage, canRetry = true)
                    }
                )
            } catch (e: Exception) {
                val errorMessage = ErrorMapper.getErrorMessage(e)
                _authState.value = AuthState.Error(errorMessage, canRetry = true)
            }
        }
    }

    /**
     * Resend OTP with proper cooldown management.
     */
    fun resendOtp(activity: Activity?) {
        if (currentResendToken == null) {
            _authState.value = AuthState.Error("Cannot resend OTP. Please restart the login process.", canRetry = true)
            return
        }

        // Check cooldown
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastResendTime < RESEND_COOLDOWN_MS) {
            val remainingSeconds = ((RESEND_COOLDOWN_MS - (currentTime - lastResendTime)) / 1000).toInt()
            _authState.value = AuthState.Error("Please wait $remainingSeconds seconds before resending OTP", canRetry = false)
            return
        }

        // Check network connectivity
        if (networkMonitor != null && !networkMonitor.isConnected()) {
            _authState.value = AuthState.Error("No internet connection. Please check your network and try again.", canRetry = true)
            return
        }

        _authState.value = AuthState.Loading

        viewModelScope.launch {
            try {
                authRepository.resendOtp(
                    phoneNumber = currentPhoneNumber,
                    resendToken = currentResendToken,
                    onCodeSent = { verificationId, resendToken ->
                        currentVerificationId = verificationId
                        currentResendToken = resendToken
                        lastResendTime = System.currentTimeMillis()
                        _authState.value = AuthState.OtpSent(currentPhoneNumber, verificationId)
                    },
                    onError = { errorMessage ->
                        _authState.value = AuthState.Error(errorMessage, canRetry = true)
                    }
                )
            } catch (e: Exception) {
                val errorMessage = ErrorMapper.getErrorMessage(e)
                _authState.value = AuthState.Error(errorMessage, canRetry = true)
            }
        }
    }

    /**
     * Verify OTP code with security checks.
     */
    fun verifyOtp(otp: String) {
        val verificationId = currentVerificationId
        if (verificationId == null) {
            _authState.value = AuthState.Error("No verification ID. Please request OTP again.", canRetry = true)
            return
        }

        // Security: Input validation
        val sanitizedOtp = otp.filter { it.isDigit() }
        if (sanitizedOtp.length != 6) {
            _authState.value = AuthState.Error("Please enter a valid 6-digit OTP", canRetry = false)
            return
        }

        // Security: Check for too many failed attempts
        if (verificationAttemptCount >= MAX_VERIFICATION_ATTEMPTS) {
            _authState.value = AuthState.Error(
                "Too many incorrect attempts. Please request a new OTP.",
                canRetry = true
            )
            // Reset verification ID to force new OTP request
            currentVerificationId = null
            currentResendToken = null
            verificationAttemptCount = 0
            return
        }

        // Check network connectivity
        if (networkMonitor != null && !networkMonitor.isConnected()) {
            _authState.value = AuthState.Error("No internet connection. Please check your network and try again.", canRetry = true)
            return
        }

        _authState.value = AuthState.Loading

        viewModelScope.launch {
            try {
                val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                val result = authRepository.verifyOtpWithCredential(credential)

                result.onSuccess { uid ->
                    // Reset attempt counter on success
                    verificationAttemptCount = 0
                    handleSuccessfulLogin(uid)
                }.onFailure { exception ->
                    // Increment attempt counter on failure
                    verificationAttemptCount++

                    val errorMessage = ErrorMapper.getErrorMessage(exception)
                    val canRetry = verificationAttemptCount < MAX_VERIFICATION_ATTEMPTS
                    _authState.value = AuthState.Error(
                        if (!canRetry) {
                            "Too many incorrect attempts. Please request a new OTP."
                        } else {
                            "$errorMessage (${MAX_VERIFICATION_ATTEMPTS - verificationAttemptCount} attempts remaining)"
                        },
                        canRetry = canRetry
                    )
                }
            } catch (e: Exception) {
                val errorMessage = ErrorMapper.getErrorMessage(e)
                _authState.value = AuthState.Error(errorMessage, canRetry = true)
            }
        }
    }

    /**
     * Handle auto-verification when Firebase automatically verifies the phone.
     */
    private fun handleAutoVerification(credential: PhoneAuthCredential, activity: Activity?) {
        viewModelScope.launch {
            try {
                val result = authRepository.verifyOtpWithCredential(credential)
                result.onSuccess { uid ->
                    handleSuccessfulLogin(uid)
                }.onFailure { exception ->
                    // Auto-verification failed, user needs to enter OTP manually
                    // Keep the verification ID for manual OTP entry
                    _authState.value = AuthState.OtpSent(currentPhoneNumber, currentVerificationId ?: "")
                }
            } catch (e: Exception) {
                // If auto-verification fails completely, show error and allow retry
                val errorMessage = ErrorMapper.getErrorMessage(e)
                _authState.value = AuthState.Error("$errorMessage. Please try requesting OTP again.", canRetry = true)
            }
        }
    }

    /**
     * Handle successful login - create/update user profile and setup FCM.
     */
    private suspend fun handleSuccessfulLogin(uid: String) {
        try {
            // Check if provider document exists
            val providerResult = firestoreRepository.getProviderData(uid)

            providerResult.onSuccess { providerData ->
                if (providerData == null) {
                    // Create new provider document
                    val formattedPhone = PhoneNumberFormatter.formatPhoneNumber(currentPhoneNumber)
                    firestoreRepository.createProviderDocument(uid, formattedPhone)
                } else {
                    // Update existing document if phone number is missing
                    if (providerData.phoneNumber.isEmpty()) {
                        val formattedPhone = PhoneNumberFormatter.formatPhoneNumber(currentPhoneNumber)
                        firestoreRepository.updateProviderData(uid, mapOf("phoneNumber" to formattedPhone))
                    }
                    // Update last login
                    firestoreRepository.updateLastLogin(uid)
                }
            }

            // Setup FCM token AFTER successful login (non-blocking)
            viewModelScope.launch {
                try {
                    FcmTokenManager.getAndSaveToken(uid)
                } catch (e: Exception) {
                    // FCM token setup failure is not critical for login flow
                }
            }

            // Determine next state based on onboarding status
            val finalProviderData = firestoreRepository.getProviderData(uid)
            finalProviderData.onSuccess { data ->
                _authState.value = when {
                    data == null -> AuthState.Authenticated(uid)
                    data.onboardingStatus == "SUBMITTED" && data.approvalStatus == "PENDING" ->
                        AuthState.PendingApproval(uid)
                    data.approvalStatus == "REJECTED" ->
                        AuthState.Rejected(uid, data.rejectionReason)
                    data.approvalStatus == "APPROVED" ->
                        AuthState.Approved
                    data.onboardingStatus == "IN_PROGRESS" ->
                        AuthState.Onboarding(uid, data.currentStep)
                    else -> AuthState.Onboarding(uid, 1)
                }
            }.onFailure {
                _authState.value = AuthState.Authenticated(uid)
            }

        } catch (e: Exception) {
            val errorMessage = ErrorMapper.getErrorMessage(e)
            _authState.value = AuthState.Error("Login successful but profile setup failed: $errorMessage", canRetry = true)
        }
    }

    /**
     * Start onboarding process.
     */
    fun startOnboarding(uid: String) {
        _authState.value = AuthState.Onboarding(uid, 1)
    }

    /**
     * Update onboarding step.
     */
    fun updateOnboardingStep(uid: String, step: Int) {
        _authState.value = AuthState.Onboarding(uid, step)
    }

    /**
     * Complete onboarding and move to pending approval.
     */
    fun completeOnboarding(uid: String) {
        _authState.value = AuthState.PendingApproval(uid)
    }

    /**
     * Move to approved state (called by backend status updates).
     */
    fun moveToApproved() {
        _authState.value = AuthState.Approved
    }

    /**
     * Sign out user and clean up state.
     */
    fun signOut() {
        val currentUid = when (authState) {
            is AuthState.Authenticated -> (authState as AuthState.Authenticated).uid
            is AuthState.Onboarding -> (authState as AuthState.Onboarding).uid
            is AuthState.PendingApproval -> (authState as AuthState.PendingApproval).uid
            is AuthState.Rejected -> (authState as AuthState.Rejected).uid
            else -> null
        }

        // Clean up FCM token if user was logged in
        if (currentUid != null) {
            viewModelScope.launch {
                try {
                    FcmTokenManager.removeToken(currentUid)
                } catch (e: Exception) {
                    // FCM cleanup failure is not critical
                }
            }
        }

        // Sign out from Firebase
        authRepository.signOut()

        // Clear local state and security counters
        currentVerificationId = null
        currentResendToken = null
        currentPhoneNumber = ""
        lastOtpRequestTime = 0
        lastResendTime = 0
        otpRequestCount = 0
        lastOtpRequestWindow = 0
        verificationAttemptCount = 0

        // State will be updated by AuthStateListener when Firebase signs out
        // _authState.value = AuthState.LoggedOut
    }

    /**
     * Clear current error state.
     */
    fun clearError() {
        // Only clear error if we're in error state
        if (authState is AuthState.Error) {
                _authState.value = AuthState.LoggedOut
        }
    }

    /**
     * Get current phone number for display.
     */
    fun getCurrentPhoneNumber(): String = currentPhoneNumber

    /**
     * Check if resend is available (cooldown check).
     */
    fun canResendOtp(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - lastResendTime >= RESEND_COOLDOWN_MS
    }

    /**
     * Get remaining cooldown time in seconds.
     */
    fun getResendCooldownSeconds(): Int {
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastResendTime
        val remaining = RESEND_COOLDOWN_MS - elapsed
        return maxOf(0, (remaining / 1000).toInt())
    }

    /**
     * Reset to phone entry state (for when user wants to change phone number).
     */
    fun resetToPhoneEntry() {
        currentVerificationId = null
        currentResendToken = null
        verificationAttemptCount = 0
        lastOtpRequestTime = 0
        lastResendTime = 0
                _authState.value = AuthState.LoggedOut
    }
}
