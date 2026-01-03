package com.nextserve.serveitpartnernew.ui.viewmodel

/**
 * Standardized error model for consistent error handling across the app.
 */
data class UiError(
    val title: String? = null,
    val message: String,
    val canRetry: Boolean = true,
    val technicalDetails: String? = null,
    val errorCode: String? = null,
    val isInformational: Boolean = false
) {
    companion object {
        fun fromThrowable(throwable: Throwable, canRetry: Boolean = true): UiError {
            return UiError(
                message = throwable.message ?: "An unexpected error occurred",
                canRetry = canRetry,
                technicalDetails = throwable.toString(),
                errorCode = throwable.javaClass.simpleName
            )
        }

        fun networkError(): UiError {
            return UiError(
                message = "No internet connection. Please check your network and try again.",
                canRetry = true,
                errorCode = "NETWORK_ERROR"
            )
        }

        fun validationError(message: String): UiError {
            return UiError(
                message = message,
                canRetry = false,
                errorCode = "VALIDATION_ERROR"
            )
        }

        fun firestoreIndexSetup(): UiError {
            return UiError(
                title = "Preparing Earnings Data",
                message = "Earnings data is being set up. Please try again in a minute.",
                canRetry = true,
                errorCode = "INDEX_SETUP",
                isInformational = true
            )
        }
    }
}
