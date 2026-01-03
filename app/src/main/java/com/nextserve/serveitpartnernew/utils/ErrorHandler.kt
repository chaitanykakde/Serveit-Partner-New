package com.nextserve.serveitpartnernew.utils

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.nextserve.serveitpartnernew.R

/**
 * Centralized error handling utility
 */
class ErrorHandler(private val context: Context) {

    /**
     * Handle and display errors to user
     */
    fun handleError(error: Throwable, showToast: Boolean = true): String {
        val userMessage = getUserFriendlyMessage(error)

        if (showToast) {
            showToast(userMessage)
        }

        // Log error for debugging (in production, send to crash reporting)
        android.util.Log.e("ErrorHandler", "Error occurred", error)

        return userMessage
    }

    /**
     * Convert technical errors to user-friendly messages
     */
    private fun getUserFriendlyMessage(error: Throwable): String {
        return when {
            // Network errors
            error.message?.contains("network", ignoreCase = true) == true ||
            error.message?.contains("connect", ignoreCase = true) == true ||
            error.message?.contains("timeout", ignoreCase = true) == true ->
                context.getString(R.string.error_network)

            // Authentication errors
            error.message?.contains("auth", ignoreCase = true) == true ||
            error.message?.contains("login", ignoreCase = true) == true ||
            error.message?.contains("credential", ignoreCase = true) == true ->
                context.getString(R.string.error_auth)

            // Permission errors
            error.message?.contains("permission", ignoreCase = true) == true ||
            error.message?.contains("denied", ignoreCase = true) == true ->
                context.getString(R.string.error_permission)

            // Storage errors
            error.message?.contains("storage", ignoreCase = true) == true ||
            error.message?.contains("upload", ignoreCase = true) == true ->
                context.getString(R.string.error_storage)

            // Location errors
            error.message?.contains("location", ignoreCase = true) == true ||
            error.message?.contains("gps", ignoreCase = true) == true ->
                context.getString(R.string.error_location)

            // Validation errors
            error.message?.contains("validation", ignoreCase = true) == true ||
            error.message?.contains("invalid", ignoreCase = true) == true ->
                context.getString(R.string.error_validation)

            // Server errors
            error.message?.contains("server", ignoreCase = true) == true ||
            error.message?.contains("500", ignoreCase = true) == true ->
                context.getString(R.string.error_server)

            // Rate limit errors
            error.message?.contains("rate", ignoreCase = true) == true ||
            error.message?.contains("limit", ignoreCase = true) == true ->
                context.getString(R.string.error_rate_limit)

            // Default error
            else -> context.getString(R.string.error_generic)
        }
    }

    /**
     * Show toast message
     */
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Handle specific error types with custom actions
     */
    fun handleNetworkError(onRetry: () -> Unit) {
        showToast(context.getString(R.string.error_network_retry))
        // Could show a retry dialog here
    }

    fun handleAuthError(onLogin: () -> Unit) {
        showToast(context.getString(R.string.error_auth_retry))
        // Could navigate to login screen
    }

    fun handlePermissionError(onSettings: () -> Unit) {
        showToast(context.getString(R.string.error_permission_settings))
        // Could open app settings
    }

    companion object {
        // Error message constants
        const val ERROR_NETWORK = "Network connection failed. Please check your internet connection."
        const val ERROR_AUTH = "Authentication failed. Please login again."
        const val ERROR_PERMISSION = "Permission required. Please grant the necessary permissions."
        const val ERROR_STORAGE = "File upload failed. Please try again."
        const val ERROR_LOCATION = "Location access failed. Please enable location services."
        const val ERROR_VALIDATION = "Invalid input. Please check your data and try again."
        const val ERROR_SERVER = "Server error. Please try again later."
        const val ERROR_RATE_LIMIT = "Too many requests. Please wait and try again."
        const val ERROR_GENERIC = "Something went wrong. Please try again."
    }
}

/**
 * Composable function to get ErrorHandler instance
 */
@Composable
fun rememberErrorHandler(): ErrorHandler {
    val context = LocalContext.current
    return remember { ErrorHandler(context) }
}
