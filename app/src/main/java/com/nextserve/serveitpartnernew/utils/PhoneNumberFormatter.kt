package com.nextserve.serveitpartnernew.utils

/**
 * Utility class for formatting and validating Indian phone numbers.
 * Ensures consistent phone number format across the app.
 */
object PhoneNumberFormatter {
    /**
     * Formats a phone number to include +91 prefix if not present.
     * @param phoneNumber The phone number to format (with or without +91)
     * @return Formatted phone number with +91 prefix
     */
    fun formatPhoneNumber(phoneNumber: String): String {
        val cleaned = phoneNumber.filter { it.isDigit() || it == '+' }
        return when {
            cleaned.startsWith("+91") -> cleaned
            cleaned.startsWith("91") && cleaned.length > 10 -> "+$cleaned"
            cleaned.length == 10 -> "+91$cleaned"
            else -> cleaned // Return as-is if doesn't match expected format
        }
    }

    /**
     * Validates if a phone number is a valid Indian mobile number.
     * Indian mobile numbers must:
     * - Be 10 digits long
     * - Start with 6, 7, 8, or 9
     * 
     * @param phoneNumber The phone number to validate (digits only, no prefix)
     * @return true if valid, false otherwise
     */
    fun isValidIndianPhoneNumber(phoneNumber: String): Boolean {
        val cleaned = phoneNumber.filter { it.isDigit() }
        if (cleaned.length != 10) return false
        
        // Indian mobile numbers start with 6, 7, 8, or 9
        val firstDigit = cleaned.firstOrNull()?.digitToIntOrNull() ?: return false
        return firstDigit in 6..9
    }

    /**
     * Cleans a phone number by removing all non-digit characters except +.
     * @param phoneNumber The phone number to clean
     * @return Cleaned phone number (digits only)
     */
    fun cleanPhoneNumber(phoneNumber: String): String {
        return phoneNumber.filter { it.isDigit() }
    }

    /**
     * Extracts the 10-digit phone number from a formatted number.
     * @param phoneNumber The phone number (may include +91 prefix)
     * @return 10-digit phone number without prefix
     */
    fun extractPhoneNumber(phoneNumber: String): String {
        val cleaned = cleanPhoneNumber(phoneNumber)
        return if (cleaned.length >= 10) {
            cleaned.takeLast(10)
        } else {
            cleaned
        }
    }
}

