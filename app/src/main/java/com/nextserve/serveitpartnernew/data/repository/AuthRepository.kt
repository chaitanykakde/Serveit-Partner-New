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
 * Repository for Firebase Authentication operations.
 * Handles OTP sending, verification, and resending.
 */
class AuthRepository(
    private val auth: FirebaseAuth,
    private val activity: Activity? = null
) {
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    /**
     * Sends OTP to the specified phone number.
     * @param phoneNumber The phone number (with or without +91 prefix)
     * @param onVerificationComplete Callback when auto-verification completes
     * @param onCodeSent Callback when OTP code is sent
     * @param onError Callback when error occurs
     */
    suspend fun sendOtp(
        phoneNumber: String,
        onVerificationComplete: (PhoneAuthCredential) -> Unit,
        onCodeSent: (String, PhoneAuthProvider.ForceResendingToken?) -> Unit,
        onError: (String) -> Unit
    ) {
        val formattedPhone = if (phoneNumber.startsWith("+91")) {
            phoneNumber
        } else {
            "+91$phoneNumber"
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    onVerificationComplete(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    val friendlyError = ErrorMapper.getErrorMessage(e)
                    onError(friendlyError)
                }

                override fun onCodeSent(
                    id: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = id
                    resendToken = token
                    onCodeSent(id, token)
                }
            })
        
        activity?.let { optionsBuilder.setActivity(it) }
        val options = optionsBuilder.build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun verifyOtp(code: String): Result<String> {
        return try {
            val credential = verificationId?.let {
                PhoneAuthProvider.getCredential(it, code)
            } ?: return Result.failure(IllegalStateException("No verification ID"))

            val result = auth.signInWithCredential(credential).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("User ID is null"))
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyOtpWithCredential(credential: PhoneAuthCredential): Result<String> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("User ID is null"))
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resends OTP to the specified phone number.
     * @param phoneNumber The phone number (with or without +91 prefix)
     * @param resendToken The resend token from previous OTP send (optional)
     * @param onCodeSent Callback when OTP code is sent
     * @param onError Callback when error occurs
     */
    suspend fun resendOtp(
        phoneNumber: String,
        resendToken: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken? = null,
        onCodeSent: (String, com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken?) -> Unit,
        onError: (String) -> Unit
    ) {
        val formattedPhone = if (phoneNumber.startsWith("+91")) {
            phoneNumber
        } else {
            "+91$phoneNumber"
        }

        val token = resendToken ?: this.resendToken
        if (token != null) {
            val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        // Auto-verification handled
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        val friendlyError = ErrorMapper.getErrorMessage(e)
                        onError(friendlyError)
                    }

                    override fun onCodeSent(
                        id: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        verificationId = id
                        this@AuthRepository.resendToken = token
                        onCodeSent(id, token)
                    }
                })
                .setForceResendingToken(token)
            
            activity?.let { optionsBuilder.setActivity(it) }
            val options = optionsBuilder.build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        } else {
            // First time send (no resend token available)
            sendOtp(
                formattedPhone,
                {}, // onVerificationComplete
                onCodeSent, // onCodeSent
                onError
            )
        }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun signOut() {
        auth.signOut()
    }
}

