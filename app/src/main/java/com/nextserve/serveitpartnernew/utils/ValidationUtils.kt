package com.nextserve.serveitpartnernew.utils

import java.util.regex.Pattern

/**
 * Utility class for validating user input across the app.
 * Provides consistent validation rules and user-friendly error messages.
 */
object ValidationUtils {
    
    // Email validation pattern
    private val EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
    )
    
    // Name validation: 2-50 characters, letters, spaces, and common name characters (apostrophes, hyphens)
    private val NAME_PATTERN = Pattern.compile(
        "^[a-zA-Z\\s'-]{2,50}\$"
    )
    
    // Pincode validation: exactly 6 digits
    private val PINCODE_PATTERN = Pattern.compile("^[0-9]{6}\$")
    
    /**
     * Validates an email address format.
     * @param email The email to validate
     * @return ValidationResult with isValid flag and error message
     */
    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isEmpty() -> ValidationResult(
                isValid = true, // Email is optional
                errorMessage = null
            )
            email.length > 100 -> ValidationResult(
                isValid = false,
                errorMessage = "Email address is too long. Maximum 100 characters allowed."
            )
            !EMAIL_PATTERN.matcher(email).matches() -> ValidationResult(
                isValid = false,
                errorMessage = "Please enter a valid email address (e.g., name@example.com)"
            )
            else -> ValidationResult(isValid = true, errorMessage = null)
        }
    }
    
    /**
     * Validates a full name.
     * @param name The name to validate
     * @return ValidationResult with isValid flag and error message
     */
    fun validateName(name: String): ValidationResult {
        return when {
            name.isEmpty() -> ValidationResult(
                isValid = false,
                errorMessage = "Name is required"
            )
            name.length < 2 -> ValidationResult(
                isValid = false,
                errorMessage = "Name must be at least 2 characters long"
            )
            name.length > 50 -> ValidationResult(
                isValid = false,
                errorMessage = "Name is too long. Maximum 50 characters allowed."
            )
            !NAME_PATTERN.matcher(name.trim()).matches() -> ValidationResult(
                isValid = false,
                errorMessage = "Name can only contain letters, spaces, apostrophes, and hyphens"
            )
            else -> ValidationResult(isValid = true, errorMessage = null)
        }
    }
    
    /**
     * Validates a pincode (6 digits).
     * @param pincode The pincode to validate
     * @return ValidationResult with isValid flag and error message
     */
    fun validatePincode(pincode: String): ValidationResult {
        return when {
            pincode.isEmpty() -> ValidationResult(
                isValid = false,
                errorMessage = "Pincode is required"
            )
            pincode.length != 6 -> ValidationResult(
                isValid = false,
                errorMessage = "Pincode must be exactly 6 digits"
            )
            !PINCODE_PATTERN.matcher(pincode).matches() -> ValidationResult(
                isValid = false,
                errorMessage = "Pincode must contain only numbers"
            )
            else -> ValidationResult(isValid = true, errorMessage = null)
        }
    }
    
    /**
     * Validates coordinates (latitude and longitude).
     * @param latitude Latitude value
     * @param longitude Longitude value
     * @return ValidationResult with isValid flag and error message
     */
    fun validateCoordinates(latitude: Double?, longitude: Double?): ValidationResult {
        return when {
            latitude == null || longitude == null -> ValidationResult(
                isValid = false,
                errorMessage = "Location coordinates are required. Please use 'Use Current Location' or enter address manually."
            )
            latitude < -90 || latitude > 90 -> ValidationResult(
                isValid = false,
                errorMessage = "Invalid latitude. Must be between -90 and 90."
            )
            longitude < -180 || longitude > 180 -> ValidationResult(
                isValid = false,
                errorMessage = "Invalid longitude. Must be between -180 and 180."
            )
            else -> ValidationResult(isValid = true, errorMessage = null)
        }
    }
    
    /**
     * Validates service radius.
     * @param radius Service radius in kilometers
     * @return ValidationResult with isValid flag and error message
     */
    fun validateServiceRadius(radius: Float): ValidationResult {
        return when {
            radius < 3f -> ValidationResult(
                isValid = false,
                errorMessage = "Service radius must be at least 3 km"
            )
            radius > 10f -> ValidationResult(
                isValid = false,
                errorMessage = "Service radius cannot exceed 10 km"
            )
            else -> ValidationResult(isValid = true, errorMessage = null)
        }
    }
    
    /**
     * Validates address fields.
     * @param state State name
     * @param city City name
     * @param address Address/area
     * @return ValidationResult with isValid flag and error message
     */
    fun validateAddress(state: String, city: String, address: String): ValidationResult {
        return when {
            state.isEmpty() -> ValidationResult(
                isValid = false,
                errorMessage = "State is required"
            )
            city.isEmpty() -> ValidationResult(
                isValid = false,
                errorMessage = "City is required"
            )
            address.isEmpty() -> ValidationResult(
                isValid = false,
                errorMessage = "Area/Locality is required"
            )
            state.length > 50 -> ValidationResult(
                isValid = false,
                errorMessage = "State name is too long"
            )
            city.length > 50 -> ValidationResult(
                isValid = false,
                errorMessage = "City name is too long"
            )
            address.length > 200 -> ValidationResult(
                isValid = false,
                errorMessage = "Address is too long. Maximum 200 characters allowed."
            )
            else -> ValidationResult(isValid = true, errorMessage = null)
        }
    }
    
    /**
     * Validates OTP input (must be exactly 6 digits).
     * @param otp The OTP string
     * @return ValidationResult with isValid flag and error message
     */
    fun validateOtp(otp: String): ValidationResult {
        return when {
            otp.isEmpty() -> ValidationResult(
                isValid = false,
                errorMessage = "Please enter the OTP"
            )
            otp.length != 6 -> ValidationResult(
                isValid = false,
                errorMessage = "OTP must be 6 digits"
            )
            !otp.all { it.isDigit() } -> ValidationResult(
                isValid = false,
                errorMessage = "OTP must contain only numbers"
            )
            else -> ValidationResult(isValid = true, errorMessage = null)
        }
    }
}

/**
 * Data class representing validation result.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String?
)

