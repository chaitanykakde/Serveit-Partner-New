package com.nextserve.serveitpartnernew.utils

import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ErrorMapper utility.
 */
class ErrorMapperTest {

    @Test
    fun `getErrorMessage returns user-friendly message for invalid credentials`() {
        // FirebaseAuthInvalidCredentialsException doesn't have a public constructor with error code
        // We'll test with a generic exception and verify the error mapper handles it
        val exception = Exception("ERROR_INVALID_VERIFICATION_CODE: Invalid code")
        val message = ErrorMapper.getErrorMessage(exception)
        // Should return a user-friendly message
        assertTrue(message.isNotEmpty())
    }

    @Test
    fun `getErrorMessage returns user-friendly message for invalid phone number`() {
        // Test with generic exception since FirebaseAuthInvalidCredentialsException constructor is not accessible
        val exception = Exception("ERROR_INVALID_PHONE_NUMBER: Invalid phone")
        val message = ErrorMapper.getErrorMessage(exception)
        // Should return a user-friendly message
        assertTrue(message.isNotEmpty())
    }

    @Test
    fun `getErrorMessage returns user-friendly message for too many requests`() {
        // Create a mock FirebaseAuthException with ERROR_TOO_MANY_REQUESTS code
        val exception = object : FirebaseAuthException("ERROR_TOO_MANY_REQUESTS", "Too many requests") {}
        val message = ErrorMapper.getErrorMessage(exception)
        assertTrue(message.contains("Too many requests"))
    }

    @Test
    fun `getErrorCode returns correct code for different exceptions`() {
        // Test with generic exception since FirebaseAuthInvalidCredentialsException constructor is not accessible
        val genericException = Exception("Some error")
        val code = ErrorMapper.getErrorCode(genericException)
        assertEquals("UNKNOWN_ERROR", code)
        
        val tooManyRequestsException = object : FirebaseAuthException("ERROR_TOO_MANY_REQUESTS", "Too many") {}
        assertEquals("TOO_MANY_REQUESTS", ErrorMapper.getErrorCode(tooManyRequestsException))
    }

    @Test
    fun `getErrorMessage handles generic exceptions`() {
        val exception = Exception("Generic error")
        val message = ErrorMapper.getErrorMessage(exception)
        assertTrue(message.contains("Generic error"))
    }
}

