package com.nextserve.serveitpartnernew.utils

import android.util.Patterns
import java.util.regex.Pattern

/**
 * Utility class for security and input validation
 */
object SecurityUtils {

    // Phone number validation patterns
    private val INDIAN_PHONE_PATTERN = Pattern.compile("^(\\+91|91|0)?[6-9]\\d{9}$")
    private val INTERNATIONAL_PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$")

    // Email validation
    private val EMAIL_PATTERN = Patterns.EMAIL_ADDRESS

    // Name validation (allow letters, spaces, hyphens, apostrophes)
    private val NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s\\-']{2,50}$")

    // Service name validation (allow alphanumeric, spaces, hyphens, parentheses)
    private val SERVICE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-\\(\\)]{2,100}$")

    // Address validation
    private val ADDRESS_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-.,#/()]{5,200}$")

    // Pincode validation (Indian pincode: 6 digits)
    private val PINCODE_PATTERN = Pattern.compile("^\\d{6}$")

    /**
     * Validate Indian phone number
     */
    fun isValidIndianPhoneNumber(phone: String): Boolean {
        if (phone.isBlank()) return false
        val cleanPhone = phone.replace("\\s".toRegex(), "")
        return INDIAN_PHONE_PATTERN.matcher(cleanPhone).matches()
    }

    /**
     * Validate international phone number
     */
    fun isValidInternationalPhoneNumber(phone: String): Boolean {
        if (phone.isBlank()) return false
        val cleanPhone = phone.replace("\\s".toRegex(), "")
        return INTERNATIONAL_PHONE_PATTERN.matcher(cleanPhone).matches()
    }

    /**
     * Validate email address
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && EMAIL_PATTERN.matcher(email).matches()
    }

    /**
     * Validate full name
     */
    fun isValidName(name: String): Boolean {
        return name.isNotBlank() && NAME_PATTERN.matcher(name.trim()).matches()
    }

    /**
     * Validate service name
     */
    fun isValidServiceName(serviceName: String): Boolean {
        return serviceName.isNotBlank() && SERVICE_NAME_PATTERN.matcher(serviceName.trim()).matches()
    }

    /**
     * Validate address
     */
    fun isValidAddress(address: String): Boolean {
        return address.isNotBlank() && ADDRESS_PATTERN.matcher(address.trim()).matches()
    }

    /**
     * Validate Indian pincode
     */
    fun isValidPincode(pincode: String): Boolean {
        return pincode.isNotBlank() && PINCODE_PATTERN.matcher(pincode.trim()).matches()
    }

    /**
     * Sanitize user input by removing potentially harmful characters
     */
    fun sanitizeInput(input: String, maxLength: Int = 1000): String {
        return input
            .trim()
            .take(maxLength)
            .replace("[<>\"';&]".toRegex(), "") // Remove HTML/script injection characters
            .replace("\\s+".toRegex(), " ") // Normalize whitespace
    }

    /**
     * Validate password strength (for future use if password auth is added)
     */
    fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        // Check for at least one uppercase, one lowercase, one digit
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        return hasUpperCase && hasLowerCase && hasDigit
    }

    /**
     * Validate monetary amount
     */
    fun isValidAmount(amount: String): Boolean {
        return try {
            val value = amount.toDouble()
            value >= 0 && value <= 100000 // Reasonable bounds
        } catch (e: NumberFormatException) {
            false
        }
    }

    /**
     * Validate booking ID format
     */
    fun isValidBookingId(bookingId: String): Boolean {
        // Allow alphanumeric characters, hyphens, underscores
        return bookingId.matches("^[a-zA-Z0-9_-]{5,50}$".toRegex())
    }

    /**
     * Check for SQL injection patterns (additional security)
     */
    fun containsInjectionPatterns(input: String): Boolean {
        val injectionPatterns = listOf(
            "(\\b(union|select|insert|update|delete|drop|create|alter)\\b)",
            "(\\b(script|javascript|vbscript|onload|onerror)\\b)",
            "(<[^>]*>)", // HTML tags
            "([';]\\s*--)", // SQL comments
            "(\\b(or|and)\\b.*[=<>])" // SQL logic
        )

        return injectionPatterns.any { pattern ->
            Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(input)
        }
    }

    /**
     * Generate secure random string for temporary IDs
     */
    fun generateSecureId(length: Int = 32): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }
}
