package com.nextserve.serveitpartnernew.data.repository

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.utils.ErrorMapper
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Firebase PhoneAuth operations only.
 * No state storage, no UI knowledge.
 * Production-grade for 1M+ users.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseProvider.firestore
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

    /**
     * Save FCM token to Firestore.
     * 
     * CRITICAL: Uses exact structure required by Cloud Functions:
     * - Collection: partners
     * - Document: {partnerId}
     * - Field: fcmToken (root-level string)
     * 
     * Uses SetOptions.merge() to avoid overwriting other document fields.
     * Idempotent - safe to call multiple times.
     * 
     * @param partnerId The partner/user ID (Firebase Auth UID)
     * @return Result<Unit> Success if token saved, failure otherwise
     */
    suspend fun saveFcmToken(partnerId: String): Result<Unit> {
        return try {
            // Fetch FCM token
            val token = FirebaseMessaging.getInstance().token.await()
            
            if (token.isNullOrBlank()) {
                Log.w("AuthRepository", "FCM token is null or blank, cannot save")
                return Result.failure(Exception("FCM token is null or blank"))
            }

            // Write to Firestore using exact structure Cloud Functions expect
            // partners/{partnerId}.fcmToken = token
            firestore
                .collection("partners")
                .document(partnerId)
                .set(
                    mapOf("fcmToken" to token),
                    SetOptions.merge()
                )
                .await()

            Log.d("AuthRepository", "✅ FCM token saved successfully to partners/$partnerId.fcmToken")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Failed to save FCM token to Firestore", e)
            Result.failure(e)
        }
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