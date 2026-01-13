package com.nextserve.serveitpartnernew.data.store

import androidx.lifecycle.SavedStateHandle
import com.google.firebase.auth.PhoneAuthProvider
import java.io.Serializable

/**
 * Safe persistence for OTP session data.
 * Never stores OTP digits - only session metadata.
 * Survives process death and configuration changes.
 */
interface OtpSessionStore {

    fun saveSession(session: OtpSessionData)
    fun getSession(): OtpSessionData?
    fun clearSession()
    fun saveAttemptCount(count: Int)
    fun getAttemptCount(): Int
    fun clearAttemptCount()
}

/**
 * Production implementation using SavedStateHandle.
 */
class SavedStateOtpSessionStore(
    private val savedStateHandle: SavedStateHandle
) : OtpSessionStore {

    companion object {
        private const val KEY_SESSION = "otp_session"
        private const val KEY_ATTEMPTS = "otp_attempts"
    }

    override fun saveSession(session: OtpSessionData) {
        savedStateHandle[KEY_SESSION] = session
    }

    override fun getSession(): OtpSessionData? {
        return savedStateHandle[KEY_SESSION]
    }

    override fun clearSession() {
        savedStateHandle.remove<String>(KEY_SESSION)
    }

    override fun saveAttemptCount(count: Int) {
        savedStateHandle[KEY_ATTEMPTS] = count
    }

    override fun getAttemptCount(): Int {
        return savedStateHandle[KEY_ATTEMPTS] ?: 0
    }

    override fun clearAttemptCount() {
        savedStateHandle.remove<Int>(KEY_ATTEMPTS)
    }
}

/**
 * OTP session data that can be safely persisted.
 * No sensitive information - only session metadata.
 */
data class OtpSessionData(
    val phoneNumber: String,
    val verificationId: String?,
    val resendTokenString: String?, // Serialized token
    val canResendAt: Long,
    val lastRequestAt: Long
) : Serializable {

    /**
     * Convert back to Firebase resend token.
     * Returns null if token cannot be reconstructed.
     */
    fun getResendToken(): PhoneAuthProvider.ForceResendingToken? {
        // Note: PhoneAuthProvider.ForceResendingToken doesn't support serialization
        // In production, you'd need to store the token differently or handle resend differently
        // For now, return null to force new OTP request on process death
        return null
    }

    companion object {
        fun fromFirebaseToken(
            phoneNumber: String,
            verificationId: String?,
            token: PhoneAuthProvider.ForceResendingToken?,
            canResendAt: Long = 0L
        ): OtpSessionData {
            return OtpSessionData(
                phoneNumber = phoneNumber,
                verificationId = verificationId,
                resendTokenString = null, // Cannot serialize ForceResendingToken
                canResendAt = canResendAt,
                lastRequestAt = System.currentTimeMillis()
            )
        }
    }
}
