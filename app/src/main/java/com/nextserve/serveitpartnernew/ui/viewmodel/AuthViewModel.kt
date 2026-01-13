package com.nextserve.serveitpartnernew.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextserve.serveitpartnernew.data.repository.AuthRepository
import com.nextserve.serveitpartnernew.data.repository.OtpSession
import com.nextserve.serveitpartnernew.data.store.OtpSessionData
import com.nextserve.serveitpartnernew.data.store.OtpSessionStore
import com.nextserve.serveitpartnernew.data.store.SavedStateOtpSessionStore
import com.nextserve.serveitpartnernew.utils.ErrorMapper
import com.nextserve.serveitpartnernew.utils.PhoneNumberFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Production-grade OTP authentication ViewModel.
 * State machine for mobile + OTP verification.
 * Lifecycle-safe with SavedStateHandle persistence.
 */
class AuthViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository = AuthRepository(),
    private val sessionStore: OtpSessionStore = SavedStateOtpSessionStore(savedStateHandle)
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // Security constants
    private val maxVerificationAttempts = 3
    private val resendCooldownMs = 30_000L // 30 seconds

    init {
        // Check FirebaseAuth state first
        checkFirebaseAuthState()
        // Then restore OTP session state if needed
        restoreAuthState()
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
        authRepository.signOut()
        clearOtpSession()
        _authState.value = AuthState.Idle
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
     * Check FirebaseAuth state on app startup.
     */
    private fun checkFirebaseAuthState() {
        val currentUser = authRepository.getCurrentUserId()
        if (currentUser != null) {
            // User is already authenticated - set to authenticated state
            _authState.value = AuthState.Authenticated
        }
        // If not authenticated, stay in Idle state (default)
    }

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
}