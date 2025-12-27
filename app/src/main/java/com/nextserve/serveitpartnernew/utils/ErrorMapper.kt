package com.nextserve.serveitpartnernew.utils

import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.FirebaseNetworkException

/**
 * Maps Firebase exceptions to user-friendly error messages.
 */
object ErrorMapper {
    /**
     * Maps a Firebase exception to a user-friendly error message.
     * @param throwable The throwable to map (can be Exception or Throwable)
     * @return User-friendly error message
     */
    fun getErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is FirebaseAuthInvalidCredentialsException -> {
                when (throwable.errorCode) {
                    "ERROR_INVALID_VERIFICATION_CODE" -> "Invalid OTP. Please check and try again."
                    "ERROR_INVALID_PHONE_NUMBER" -> "Invalid phone number. Please enter a valid 10-digit mobile number."
                    else -> "Invalid credentials. Please try again."
                }
            }
            is FirebaseAuthException -> {
                // Check for too many requests error code
                if (throwable.errorCode == "ERROR_TOO_MANY_REQUESTS") {
                    "Too many requests. Please wait a few minutes before trying again."
                } else {
                    throwable.message ?: "Authentication error. Please try again."
                }
            }
            is FirebaseNetworkException -> {
                "Network error. Please check your internet connection and try again."
            }
            is FirebaseException -> {
                when (throwable.message) {
                    "The SMS quota for this project has been exceeded." -> 
                        "SMS quota exceeded. Please try again later."
                    "Invalid phone number format." -> 
                        "Invalid phone number format. Please enter a valid 10-digit mobile number."
                    else -> throwable.message ?: "An error occurred. Please try again."
                }
            }
            else -> {
                throwable.message ?: "An unexpected error occurred. Please try again."
            }
        }
    }

    /**
     * Gets an error code for programmatic handling.
     * @param throwable The throwable
     * @return Error code string
     */
    fun getErrorCode(throwable: Throwable): String {
        return when (throwable) {
            is FirebaseAuthInvalidCredentialsException -> "INVALID_CREDENTIALS"
            is FirebaseAuthException -> {
                if (throwable.errorCode == "ERROR_TOO_MANY_REQUESTS") {
                    "TOO_MANY_REQUESTS"
                } else {
                    "AUTH_ERROR"
                }
            }
            is FirebaseNetworkException -> "NETWORK_ERROR"
            is FirebaseException -> "FIREBASE_ERROR"
            else -> "UNKNOWN_ERROR"
        }
    }
}

