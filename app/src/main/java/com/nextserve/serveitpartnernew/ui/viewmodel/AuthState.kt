package com.nextserve.serveitpartnernew.ui.viewmodel

/**
 * Single source of truth for authentication state.
 * Controls all navigation and UI behavior throughout the app.
 */
sealed class AuthState {
    object Uninitialized : AuthState()
    object LoggedOut : AuthState()
    data class PhoneEntered(val phoneNumber: String) : AuthState()
    data class OtpSent(val phoneNumber: String, val verificationId: String) : AuthState()
    data class VerifyingOtp(val phoneNumber: String, val verificationId: String) : AuthState()
    data class Authenticated(val uid: String) : AuthState()
    data class Onboarding(val uid: String, val currentStep: Int) : AuthState()
    data class PendingApproval(val uid: String) : AuthState()
    data class Rejected(val uid: String, val reason: String?) : AuthState()
    object Approved : AuthState()
    data class Error(val message: String, val canRetry: Boolean = true) : AuthState()
    object Loading : AuthState()
}
