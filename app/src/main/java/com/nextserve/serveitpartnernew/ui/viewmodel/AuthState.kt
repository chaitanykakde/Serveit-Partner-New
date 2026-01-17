package com.nextserve.serveitpartnernew.ui.viewmodel

/**
 * ULTRA-STRICT OTP authentication state machine.
 * ONLY 5 phases with explicit navigation rules:
 *
 * IDLE: Initial state, no navigation
 * OTP_REQUESTED: OTP being requested, no navigation
 * OTP_SENT: OTP sent successfully - SCREEN MUST REMAIN STABLE
 * OTP_VERIFYING: OTP being verified - SCREEN MUST REMAIN STABLE
 * AUTHENTICATED: SUCCESS - ONLY STATE THAT TRIGGERS NAVIGATION AWAY
 *
 * Navigation Rules:
 * - TO OTP screen: ONLY when state becomes OTP_SENT
 * - AWAY from OTP screen: ONLY when state becomes AUTHENTICATED
 * - STAY on OTP screen: For OTP_SENT and OTP_VERIFYING phases
 */
sealed class AuthState {

    // Phase 0: Initial state
    object Idle : AuthState()

    // Phase 1: Firebase authentication check
    object Authenticating : AuthState()
    object LoggedOut : AuthState()
    object Authenticated : AuthState()  // Firebase auth success (NOT terminal state)

    // Phase 2: Phone validation (no navigation)
    object PhoneValidating : AuthState()
    data class PhoneError(val message: String) : AuthState()

    // Phase 3: OTP being requested (no navigation)
    object OtpSending : AuthState()
    data class OtpSendError(
        val message: String,
        val canRetry: Boolean = true
    ) : AuthState()

    // Phase 4: OTP successfully sent - SCREEN SHOULD BE STABLE HERE
    data class OtpSent(
        val phoneNumber: String,
        val canResendAt: Long = 0L
    ) : AuthState()

    // Phase 5: OTP being verified (screen should remain stable)
    object OtpVerifying : AuthState()
    data class OtpVerificationError(
        val message: String,
        val attemptsRemaining: Int,
        val canRetry: Boolean = true
    ) : AuthState()

    // Phase 6: Provider verification states (AFTER authentication)
    object ProviderOnboarding : AuthState()      // Needs to complete onboarding
    object ProviderPending : AuthState()         // Submitted, waiting for approval
    data class ProviderRejected(val reason: String?) : AuthState()  // Rejected by admin
    object ProviderApproved : AuthState()        // Approved, can use app

    // General error state (fallback)
    data class Error(val message: String) : AuthState()
}