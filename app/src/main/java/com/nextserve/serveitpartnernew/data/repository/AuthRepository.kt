package com.nextserve.serveitpartnernew.data.repository

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.nextserve.serveitpartnernew.utils.ErrorMapper
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Firebase PhoneAuth operations only.
 * No state storage, no UI knowledge.
 * Production-grade for 1M+ users.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /**
     * Send OTP to phone number.
     * Returns verificationId and resendToken for persistence.
     */
    suspend fun sendOtp(
        phoneNumber: String,
        activity: Activity
    ): Result<OtpSession> {
        return try {
            val formattedPhone = formatPhoneNumber(phoneNumber)

            var session: OtpSession? = null
            var error: Exception? = null

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        // Auto-verification - return credential for immediate use
                        session = OtpSession(
                            phoneNumber = phoneNumber,
                            verificationId = null,
                            resendToken = null,
                            autoCredential = credential
                        )
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        session = OtpSession(
                            phoneNumber = phoneNumber,
                            verificationId = verificationId,
                            resendToken = token,
                            autoCredential = null
                        )
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        error = e
                    }
                })
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)

            // Wait for callback or timeout
            val startTime = System.currentTimeMillis()
            while (session == null && error == null) {
                if (System.currentTimeMillis() - startTime > 65000) { // 65 seconds timeout
                    return Result.failure(Exception("OTP request timeout"))
                }
                kotlinx.coroutines.delay(100)
            }

            when {
                error != null -> Result.failure(error!!)
                session != null -> Result.success(session!!)
                else -> Result.failure(Exception("Unknown error during OTP request"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resend OTP using existing token.
     */
    suspend fun resendOtp(
        phoneNumber: String,
        resendToken: PhoneAuthProvider.ForceResendingToken,
        activity: Activity
    ): Result<OtpSession> {
        return try {
            val formattedPhone = formatPhoneNumber(phoneNumber)

            var session: OtpSession? = null
            var error: Exception? = null

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setForceResendingToken(resendToken)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        session = OtpSession(
                            phoneNumber = phoneNumber,
                            verificationId = null,
                            resendToken = resendToken,
                            autoCredential = credential
                        )
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        session = OtpSession(
                            phoneNumber = phoneNumber,
                            verificationId = verificationId,
                            resendToken = token,
                            autoCredential = null
                        )
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        error = e
                    }
                })
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)

            // Wait for callback
            val startTime = System.currentTimeMillis()
            while (session == null && error == null) {
                if (System.currentTimeMillis() - startTime > 65000) {
                    return Result.failure(Exception("OTP resend timeout"))
                }
                kotlinx.coroutines.delay(100)
            }

            when {
                error != null -> Result.failure(error!!)
                session != null -> Result.success(session!!)
                else -> Result.failure(Exception("Unknown error during OTP resend"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verify OTP code.
     */
    suspend fun verifyOtp(
        verificationId: String,
        otpCode: String
    ): Result<String> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
            val result = auth.signInWithCredential(credential).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("User ID is null"))
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verify with auto-obtained credential.
     */
    suspend fun verifyWithCredential(
        credential: PhoneAuthCredential
    ): Result<String> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("User ID is null"))
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out current user.
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Get current user ID if authenticated.
     */
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Format phone number for Firebase.
     */
    private fun formatPhoneNumber(phoneNumber: String): String {
        val cleaned = phoneNumber.replace(Regex("[^0-9]"), "")
        return if (cleaned.startsWith("91")) "+$cleaned" else "+91$cleaned"
    }
}

/**
 * OTP session data returned by Firebase.
 */
data class OtpSession(
    val phoneNumber: String,
    val verificationId: String?,
    val resendToken: PhoneAuthProvider.ForceResendingToken?,
    val autoCredential: PhoneAuthCredential?
)