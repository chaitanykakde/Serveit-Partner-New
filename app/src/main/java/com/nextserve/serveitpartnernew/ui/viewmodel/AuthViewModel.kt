package com.nextserve.serveitpartnernew.ui.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.model.ProviderData
import com.nextserve.serveitpartnernew.data.repository.AuthRepository
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import com.nextserve.serveitpartnernew.data.repository.OtpSession
import com.nextserve.serveitpartnernew.data.store.OtpSessionData
import com.nextserve.serveitpartnernew.data.store.OtpSessionStore
import com.nextserve.serveitpartnernew.data.store.SavedStateOtpSessionStore
import com.nextserve.serveitpartnernew.utils.ErrorMapper
import com.nextserve.serveitpartnernew.utils.PhoneNumberFormatter
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.nextserve.serveitpartnernew.utils.LanguageManager

/**
 * Production-grade OTP authentication ViewModel.
 * State machine for mobile + OTP verification.
 * Lifecycle-safe with SavedStateHandle persistence.
 */
class AuthViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository = AuthRepository(),
    private val sessionStore: OtpSessionStore = SavedStateOtpSessionStore(savedStateHandle),
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(FirebaseProvider.firestore),
    private val context: Context? = null
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // NEW: Provider state flow (separate from auth state)
    private val _providerState = MutableStateFlow<ProviderState>(ProviderState.Loading)
    val providerState: StateFlow<ProviderState> = _providerState

    // NEW: Language state flow
    private val _languageState = MutableStateFlow<LanguageState>(LanguageState.Unknown)
    val languageState: StateFlow<LanguageState> = _languageState

    // NEW: Single source of truth for navigation destination
    private val _startDestination = MutableStateFlow<AppStartDestination>(AppStartDestination.Splash)
    val startDestination: StateFlow<AppStartDestination> = _startDestination

    // Security constants
    private val maxVerificationAttempts = 3
    private val resendCooldownMs = 30_000L // 30 seconds

    // Provider verification listener
    private var providerListener: ListenerRegistration? = null

    init {
        Log.d("AuthViewModel", "🔐 AuthViewModel initialized, starting authentication check...")
        // Start authentication flow
        initializeAuthenticationState()
        // Initialize language state
        initializeLanguageState()
        
        // Combine states to compute navigation destination
        viewModelScope.launch {
            combine(authState, providerState, languageState) { auth, provider, language ->
                computeStartDestination(auth, provider, language)
            }.collect { destination ->
                _startDestination.value = destination
                Log.d("AuthViewModel", "🎯 Start destination updated: $destination")
            }
        }
    }

    /**
     * Compute navigation destination based on combined states.
     * This is the SINGLE SOURCE OF TRUTH for navigation decisions.
     */
    private fun computeStartDestination(
        auth: AuthState,
        provider: ProviderState,
        language: LanguageState
    ): AppStartDestination {
        Log.d("AuthViewModel", "🧭 Computing start destination: auth=$auth, provider=$provider, language=$language")

        // Check if user is authenticated with Firebase (direct or via provider states)
        val isAuthenticated = auth is AuthState.Authenticated ||
                             auth is AuthState.ProviderApproved ||
                             auth is AuthState.ProviderPending ||
                             auth is AuthState.ProviderRejected ||
                             auth is AuthState.ProviderOnboarding

        // OTP flow states - stay on splash (they have their own navigation)
        val isOtpFlow = auth is AuthState.OtpSent ||
                       auth is AuthState.OtpSending ||
                       auth is AuthState.OtpVerifying ||
                       auth is AuthState.OtpSendError ||
                       auth is AuthState.OtpVerificationError ||
                       auth is AuthState.PhoneValidating ||
                       auth is AuthState.PhoneError

        return when {
            // User is logged out - navigate to login screen
            auth is AuthState.LoggedOut -> {
                Log.d("AuthViewModel", "📍 Destination: MobileNumber (logged out)")
                AppStartDestination.MobileNumber
            }

            // OTP flow states - stay on splash (OTP screens handle their own navigation)
            isOtpFlow -> {
                Log.d("AuthViewModel", "📍 Destination: Splash (OTP flow)")
                AppStartDestination.Splash
            }

            // Not authenticated or still authenticating - wait
            !isAuthenticated && auth !is AuthState.LoggedOut -> {
                Log.d("AuthViewModel", "📍 Destination: Splash (authenticating)")
                AppStartDestination.Splash
            }

            // Provider state still loading - wait
            provider == ProviderState.Loading -> {
                Log.d("AuthViewModel", "📍 Destination: Splash (provider loading)")
                AppStartDestination.Splash
            }

            // Provider verified - go to home
            provider == ProviderState.Verified -> {
                Log.d("AuthViewModel", "📍 Destination: Home (verified)")
                AppStartDestination.Home
            }

            // Provider pending verification - go to onboarding step 5 (review/verification status screen)
            provider == ProviderState.PendingVerification -> {
                Log.d("AuthViewModel", "📍 Destination: Onboarding Step 5 (pending verification)")
                AppStartDestination.Onboarding
            }

            // Provider rejected - go to onboarding step 5 (review/verification status screen with rejection message)
            provider is ProviderState.Rejected -> {
                Log.d("AuthViewModel", "📍 Destination: Onboarding Step 5 (rejected)")
                AppStartDestination.Onboarding
            }

            // Provider needs onboarding
            provider == ProviderState.OnboardingRequired -> {
                when (language) {
                    LanguageState.Selected -> {
                        Log.d("AuthViewModel", "📍 Destination: Onboarding (language selected)")
                        AppStartDestination.Onboarding
                    }
                    LanguageState.Unknown -> {
                        Log.d("AuthViewModel", "📍 Destination: LanguageSelection (language not selected)")
                        AppStartDestination.LanguageSelection
                    }
                }
            }

            // Fallback - stay on splash
            else -> {
                Log.d("AuthViewModel", "📍 Destination: Splash (fallback)")
                AppStartDestination.Splash
            }
        }
    }

    /**
     * Initialize language state from local storage.
     */
    private fun initializeLanguageState() {
        context?.let { ctx ->
            val isSelected = LanguageManager.isLanguageSelected(ctx)
            _languageState.value = if (isSelected) {
                LanguageState.Selected
            } else {
                LanguageState.Unknown
            }
            Log.d("AuthViewModel", "🌐 Language state initialized: ${_languageState.value}")
        } ?: run {
            // If context not available, assume unknown (will be updated when context available)
            _languageState.value = LanguageState.Unknown
            Log.w("AuthViewModel", "⚠️ Context not available, language state set to Unknown")
        }
    }

    /**
     * Update language state when language is selected.
     * Called from LanguageSelectionViewModel or when language is detected from Firestore.
     */
    fun updateLanguageState(isSelected: Boolean) {
        _languageState.value = if (isSelected) {
            LanguageState.Selected
        } else {
            LanguageState.Unknown
        }
        Log.d("AuthViewModel", "🌐 Language state updated: ${_languageState.value}")
    }

    /**
     * Validate phone number input.
     */
    fun validatePhoneNumber(phoneNumber: String) {
        val sanitized = PhoneNumberFormatter.cleanPhoneNumber(phoneNumber)

        when {
            sanitized.isEmpty() -> _authState.value = AuthState.Idle
            !PhoneNumberFormatter.isValidIndianPhoneNumber(sanitized) ->
                _authState.value = AuthState.PhoneError("Please enter a valid 10-digit phone number")
            else -> _authState.value = AuthState.Idle // Valid state
        }
    }

    /**
     * Send OTP to phone number.
     */
    fun sendOtp(phoneNumber: String, activity: Activity) {
        val sanitized = PhoneNumberFormatter.cleanPhoneNumber(phoneNumber)

        if (!PhoneNumberFormatter.isValidIndianPhoneNumber(sanitized)) {
            _authState.value = AuthState.PhoneError("Please enter a valid 10-digit phone number")
            return
        }

        _authState.value = AuthState.OtpSending

        viewModelScope.launch {
            try {
                val result = authRepository.sendOtp(sanitized, activity)

                result.onSuccess { session ->
                    handleOtpSession(session)
                }.onFailure { exception ->
                    val errorMessage = ErrorMapper.getErrorMessage(exception)
                    _authState.value = AuthState.OtpSendError(errorMessage, canRetry = true)
                }
            } catch (e: Exception) {
                val errorMessage = ErrorMapper.getErrorMessage(e)
                _authState.value = AuthState.OtpSendError(errorMessage, canRetry = true)
            }
        }
    }

    /**
     * Resend OTP using existing session.
     */
    fun resendOtp(activity: Activity) {
        val session = sessionStore.getSession()
        val resendToken = session?.getResendToken()

        if (session == null || resendToken == null) {
            _authState.value = AuthState.OtpSendError(
                "Cannot resend OTP. Please restart the login process.",
                canRetry = true
            )
            return
        }

        // Check cooldown
        val now = System.currentTimeMillis()
        if (now < session.canResendAt) {
            val remainingSeconds = ((session.canResendAt - now) / 1000).toInt()
            _authState.value = AuthState.OtpSendError(
                "Please wait $remainingSeconds seconds before resending",
                canRetry = false
            )
            return
        }

        _authState.value = AuthState.OtpSending

        viewModelScope.launch {
            try {
                val result = authRepository.resendOtp(session.phoneNumber, resendToken, activity)

                result.onSuccess { newSession ->
                    handleOtpSession(newSession)
                }.onFailure { exception ->
                    val errorMessage = ErrorMapper.getErrorMessage(exception)
                    _authState.value = AuthState.OtpSendError(errorMessage, canRetry = true)
                }
            } catch (e: Exception) {
                val errorMessage = ErrorMapper.getErrorMessage(e)
                _authState.value = AuthState.OtpSendError(errorMessage, canRetry = true)
            }
        }
    }

    /**
     * Verify OTP code.
     */
    fun verifyOtp(otpCode: String) {
        val session = sessionStore.getSession()

        if (session?.verificationId == null) {
            _authState.value = AuthState.Error("No active OTP session. Please request OTP again.")
            return
        }

        // Validate OTP format
        if (otpCode.length != 6 || !otpCode.all { it.isDigit() }) {
            _authState.value = AuthState.OtpVerificationError(
                "Please enter a valid 6-digit OTP",
                attemptsRemaining = maxVerificationAttempts - sessionStore.getAttemptCount(),
                canRetry = false
            )
            return
        }

        // Check attempt limit
        val currentAttempts = sessionStore.getAttemptCount()
        if (currentAttempts >= maxVerificationAttempts) {
            clearOtpSession()
            _authState.value = AuthState.OtpVerificationError(
                "Too many incorrect attempts. Please request a new OTP.",
                attemptsRemaining = 0,
                canRetry = true
            )
            return
        }

        _authState.value = AuthState.OtpVerifying

        viewModelScope.launch {
            try {
                val result = authRepository.verifyOtp(session.verificationId, otpCode)

                result.onSuccess { uid ->
                    // Success - clear session and authenticate
                    clearOtpSession()
                    _authState.value = AuthState.Authenticated
                    
                    // Save FCM token after successful authentication
                    saveFcmTokenAfterLogin(uid)
                    
                    // Start Firestore observation (same logic as initializeAuthenticationState)
                    // This ensures provider state is loaded before navigation
                    startProviderVerificationObservation(uid)
                    
                    // Re-initialize language state (context might be available now)
                    initializeLanguageState()
                }.onFailure { exception ->
                    // Increment attempt count
                    val newAttempts = currentAttempts + 1
                    sessionStore.saveAttemptCount(newAttempts)

                    val errorMessage = ErrorMapper.getErrorMessage(exception)
                    val attemptsRemaining = maxVerificationAttempts - newAttempts

                    _authState.value = AuthState.OtpVerificationError(
                        message = if (attemptsRemaining > 0) {
                            "$errorMessage (${attemptsRemaining} attempts remaining)"
                        } else {
                            "Too many incorrect attempts. Please request a new OTP."
                        },
                        attemptsRemaining = attemptsRemaining,
                        canRetry = attemptsRemaining > 0
                    )
                }
            } catch (e: Exception) {
                val errorMessage = ErrorMapper.getErrorMessage(e)
                _authState.value = AuthState.OtpVerificationError(
                    errorMessage,
                    attemptsRemaining = maxVerificationAttempts - sessionStore.getAttemptCount(),
                    canRetry = true
                )
            }
        }
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        when (val currentState = _authState.value) {
            is AuthState.PhoneError -> _authState.value = AuthState.Idle
            is AuthState.OtpSendError -> {
                // Restore to OTP requested state if we have a session
                val session = sessionStore.getSession()
                if (session != null) {
                    _authState.value = AuthState.OtpSent(
                        phoneNumber = session.phoneNumber,
                        canResendAt = session.canResendAt
                    )
                } else {
                    _authState.value = AuthState.Idle
                }
            }
            is AuthState.OtpVerificationError -> {
                // Restore to OTP requested state
                val session = sessionStore.getSession()
                if (session != null) {
                    _authState.value = AuthState.OtpSent(
                        phoneNumber = session.phoneNumber,
                        canResendAt = session.canResendAt
                    )
                } else {
                    _authState.value = AuthState.Idle
                }
            }
            is AuthState.Error -> _authState.value = AuthState.Idle
            else -> { /* Keep current state */ }
        }
    }

    /**
     * Reset to initial state.
     */
    fun reset() {
        clearOtpSession()
        _authState.value = AuthState.Idle
    }

    /**
     * Sign out user.
     */
    fun signOut() {
        Log.d("AuthViewModel", "🚪 Signing out user...")
        authRepository.signOut()
        stopProviderVerificationObservation()
        clearOtpSession()
        // Reset all states to logged out
        _authState.value = AuthState.LoggedOut
        _providerState.value = ProviderState.Loading
        _languageState.value = LanguageState.Unknown
        Log.d("AuthViewModel", "✅ User signed out, states reset")
    }

    /**
     * Start observing provider verification status.
     * This listens to real-time changes in Firestore.
     */
    private fun startProviderVerificationObservation(uid: String) {
        Log.d("AuthViewModel", "👀 Starting provider verification observation for uid: $uid")

        // Remove existing listener if any
        stopProviderVerificationObservation()

        // Start listening to provider document changes
        providerListener = firestoreRepository.observeProviderDocument(uid) { providerData ->
            Log.d("AuthViewModel", "📡 Firestore listener received provider data: $providerData")
            updateAuthStateBasedOnProviderVerification(providerData)
        }
    }

    /**
     * Stop observing provider verification status.
     */
    private fun stopProviderVerificationObservation() {
        providerListener?.remove()
        providerListener = null
    }

    /**
     * Update provider state based on provider verification status.
     * This is called whenever the provider document changes in Firestore.
     * Updates ProviderState (not AuthState) for navigation decisions.
     */
    private fun updateAuthStateBasedOnProviderVerification(providerData: ProviderData?) {
        Log.d("AuthViewModel", "🔄 updateProviderState called with providerData: $providerData")

        if (providerData == null) {
            // No provider data exists - user needs to complete onboarding
            Log.d("AuthViewModel", "📄 No provider data exists, setting provider state to OnboardingRequired")
            _providerState.value = ProviderState.OnboardingRequired
            // Also update AuthState for backward compatibility
            _authState.value = AuthState.ProviderOnboarding
            return
        }

        // Check verification status using verificationDetails (SINGLE SOURCE OF TRUTH)
        val verificationStatus = providerData.verificationDetails.status
        val onboardingStatus = providerData.onboardingStatus

        Log.d("AuthViewModel", "📊 Provider data - verificationStatus: '$verificationStatus', onboardingStatus: '$onboardingStatus'")

        // Update ProviderState for navigation decisions
        when (verificationStatus) {
            "verified" -> {
                Log.d("AuthViewModel", "✅ Provider verified, setting provider state to Verified")
                _providerState.value = ProviderState.Verified
                _authState.value = AuthState.ProviderApproved
            }
            "rejected" -> {
                val reason = providerData.verificationDetails.rejectedReason
                Log.d("AuthViewModel", "❌ Provider rejected, setting provider state to Rejected")
                _providerState.value = ProviderState.Rejected(reason)
                _authState.value = AuthState.ProviderRejected(reason)
            }
            "pending" -> {
                // CRITICAL: PendingVerification only if onboarding is SUBMITTED
                if (onboardingStatus == "SUBMITTED" || onboardingStatus == "submitted") {
                    Log.d("AuthViewModel", "⏳ Provider pending verification")
                    _providerState.value = ProviderState.PendingVerification
                    _authState.value = AuthState.ProviderPending
                } else {
                    // Onboarding in progress - user needs to complete it
                    Log.d("AuthViewModel", "📝 Onboarding in progress, setting provider state to OnboardingRequired")
                    _providerState.value = ProviderState.OnboardingRequired
                    _authState.value = AuthState.ProviderOnboarding
                }
            }
            else -> {
                // Unknown verification status - check onboardingStatus
                Log.d("AuthViewModel", "🤔 Unknown verificationStatus '$verificationStatus', checking onboardingStatus...")
                if (onboardingStatus == "SUBMITTED" || onboardingStatus == "submitted") {
                    Log.d("AuthViewModel", "📤 Onboarding submitted, setting provider state to PendingVerification")
                    _providerState.value = ProviderState.PendingVerification
                    _authState.value = AuthState.ProviderPending
                } else {
                    Log.d("AuthViewModel", "📝 Onboarding not complete, setting provider state to OnboardingRequired")
                    _providerState.value = ProviderState.OnboardingRequired
                    _authState.value = AuthState.ProviderOnboarding
                }
            }
        }

        // Also check if language is in Firestore and update language state
        context?.let { ctx ->
            val firestoreLanguage = providerData.language
            if (firestoreLanguage.isNotEmpty()) {
                // Language exists in Firestore - ensure it's saved locally
                val localLanguage = LanguageManager.getSavedLanguage(ctx)
                if (localLanguage != firestoreLanguage) {
                    LanguageManager.saveLanguage(ctx, firestoreLanguage)
                }
                // Update language state
                updateLanguageState(true)
            }
        }
    }

    /**
     * Get current phone number for display.
     */
    fun getCurrentPhoneNumber(): String? {
        return sessionStore.getSession()?.phoneNumber
    }

    /**
     * Check if resend is available.
     */
    fun canResendOtp(): Boolean {
        val session = sessionStore.getSession()
        return session != null && System.currentTimeMillis() >= session.canResendAt
    }

    /**
     * Get seconds until resend is available.
     */
    fun getResendCooldownSeconds(): Int {
        val session = sessionStore.getSession() ?: return 0
        val now = System.currentTimeMillis()
        val remaining = session.canResendAt - now
        return maxOf(0, (remaining / 1000).toInt())
    }

    /**
     * Get current authenticated user ID.
     */
    fun getCurrentUserId(): String? {
        return authRepository.getCurrentUserId()
    }

    // Private methods

    private fun handleOtpSession(session: OtpSession) {
        // Handle auto-verification
        if (session.autoCredential != null) {
            handleAutoVerification(session)
            return
        }

        // Save session for manual verification
        val sessionData = OtpSessionData.fromFirebaseToken(
            phoneNumber = session.phoneNumber,
            verificationId = session.verificationId,
            token = session.resendToken,
            canResendAt = System.currentTimeMillis() + resendCooldownMs
        )

        sessionStore.saveSession(sessionData)
        sessionStore.clearAttemptCount() // Reset attempts for new session

        _authState.value = AuthState.OtpSent(
            phoneNumber = session.phoneNumber,
            canResendAt = sessionData.canResendAt
        )
    }

    private fun handleAutoVerification(session: OtpSession) {
        _authState.value = AuthState.OtpVerifying

        viewModelScope.launch {
            try {
                val credential = session.autoCredential!!
                val result = authRepository.verifyWithCredential(credential)

                result.onSuccess { uid ->
                    clearOtpSession()
                    _authState.value = AuthState.Authenticated
                    
                    // Save FCM token after successful authentication
                    saveFcmTokenAfterLogin(uid)
                }.onFailure { exception ->
                    // Auto-verification failed, user needs to enter OTP manually
                    if (session.verificationId != null) {
                        val sessionData = OtpSessionData.fromFirebaseToken(
                            phoneNumber = session.phoneNumber,
                            verificationId = session.verificationId,
                            token = session.resendToken,
                            canResendAt = System.currentTimeMillis() + resendCooldownMs
                        )
                        sessionStore.saveSession(sessionData)
                        sessionStore.clearAttemptCount()

                        _authState.value = AuthState.OtpSent(
                            phoneNumber = session.phoneNumber,
                            canResendAt = sessionData.canResendAt
                        )
                    } else {
                        val errorMessage = ErrorMapper.getErrorMessage(exception)
                        _authState.value = AuthState.OtpSendError(errorMessage, canRetry = true)
                    }
                }
            } catch (e: Exception) {
                val errorMessage = ErrorMapper.getErrorMessage(e)
                _authState.value = AuthState.OtpSendError("$errorMessage. Please try requesting OTP again.", canRetry = true)
            }
        }
    }

    private fun clearOtpSession() {
        sessionStore.clearSession()
        sessionStore.clearAttemptCount()
    }

    /**
     * Initialize authentication state - Firebase first, then Firestore.
     * This ensures SplashScreen never waits for async work.
     */
    private fun initializeAuthenticationState() {
        Log.d("AuthViewModel", "🔄 Starting authentication initialization...")

        // Phase 1: Set authenticating state
        _authState.value = AuthState.Authenticating
        Log.d("AuthViewModel", "📊 State set to Authenticating")

        // Phase 2: Check Firebase Auth synchronously
        val currentUser = authRepository.getCurrentUserId()
        Log.d("AuthViewModel", "🔍 Firebase Auth check: currentUser = $currentUser")

        if (currentUser != null) {
            // User is authenticated with Firebase
            Log.d("AuthViewModel", "✅ Firebase auth success, setting state to Authenticated")
            _authState.value = AuthState.Authenticated

            // Phase 3: Save FCM token (user is authenticated - ensure token is stored)
            saveFcmTokenAfterLogin(currentUser)

            // Phase 4: Start Firestore observation (async, non-blocking)
            startProviderVerificationObservation(currentUser)
        } else {
            // No Firebase authentication
            Log.d("AuthViewModel", "❌ No Firebase auth, setting state to LoggedOut")
            _authState.value = AuthState.LoggedOut
        }

        // Phase 5: Restore OTP session if needed (for login flow)
        restoreAuthState()
    }

    /**
     * REMOVED: Safety timeout logic.
     * Navigation now waits for actual Firestore data.
     * No assumptions - ProviderState.Loading keeps us on Splash until data arrives.
     */

    private fun restoreAuthState() {
        // Only restore OTP session if not already authenticated
        if (_authState.value != AuthState.Authenticated) {
            val session = sessionStore.getSession()
            if (session != null) {
                // Restore to OTP requested state
                _authState.value = AuthState.OtpSent(
                    phoneNumber = session.phoneNumber,
                    canResendAt = session.canResendAt
                )
            }
        }
    }

    /**
     * Save FCM token after successful login.
     * Called automatically after authentication succeeds.
     * Non-blocking - does not affect UI state.
     */
    private fun saveFcmTokenAfterLogin(partnerId: String) {
        viewModelScope.launch {
            try {
                val result = authRepository.saveFcmToken(partnerId)
                result.onSuccess {
                    Log.d("AuthViewModel", "✅ FCM token saved successfully after login")
                }.onFailure { exception ->
                    Log.w("AuthViewModel", "⚠️ Failed to save FCM token after login (non-critical): ${exception.message}")
                    // Non-critical error - don't block user flow
                }
            } catch (e: Exception) {
                Log.w("AuthViewModel", "⚠️ Exception saving FCM token after login (non-critical): ${e.message}")
                // Non-critical error - don't block user flow
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up Firestore listener to prevent memory leaks
        stopProviderVerificationObservation()
    }
}