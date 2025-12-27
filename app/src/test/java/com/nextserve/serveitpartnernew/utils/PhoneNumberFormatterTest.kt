package com.nextserve.serveitpartnernew.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PhoneNumberFormatter utility.
 */
class PhoneNumberFormatterTest {

    @Test
    fun `formatPhoneNumber adds +91 prefix for 10 digit number`() {
        val result = PhoneNumberFormatter.formatPhoneNumber("9876543210")
        assertEquals("+919876543210", result)
    }

    @Test
    fun `formatPhoneNumber keeps +91 prefix if already present`() {
        val result = PhoneNumberFormatter.formatPhoneNumber("+919876543210")
        assertEquals("+919876543210", result)
    }

    @Test
    fun `formatPhoneNumber handles 91 prefix`() {
        val result = PhoneNumberFormatter.formatPhoneNumber("919876543210")
        assertEquals("+919876543210", result)
    }

    @Test
    fun `isValidIndianPhoneNumber returns true for valid 10 digit number starting with 6-9`() {
        assertTrue(PhoneNumberFormatter.isValidIndianPhoneNumber("9876543210"))
        assertTrue(PhoneNumberFormatter.isValidIndianPhoneNumber("8765432109"))
        assertTrue(PhoneNumberFormatter.isValidIndianPhoneNumber("7654321098"))
        assertTrue(PhoneNumberFormatter.isValidIndianPhoneNumber("6543210987"))
    }

    @Test
    fun `isValidIndianPhoneNumber returns false for number starting with 0-5`() {
        assertFalse(PhoneNumberFormatter.isValidIndianPhoneNumber("0123456789"))
        assertFalse(PhoneNumberFormatter.isValidIndianPhoneNumber("1234567890"))
        assertFalse(PhoneNumberFormatter.isValidIndianPhoneNumber("2345678901"))
        assertFalse(PhoneNumberFormatter.isValidIndianPhoneNumber("3456789012"))
        assertFalse(PhoneNumberFormatter.isValidIndianPhoneNumber("4567890123"))
        assertFalse(PhoneNumberFormatter.isValidIndianPhoneNumber("5678901234"))
    }

    @Test
    fun `isValidIndianPhoneNumber returns false for invalid length`() {
        assertFalse(PhoneNumberFormatter.isValidIndianPhoneNumber("987654321"))
        assertFalse(PhoneNumberFormatter.isValidIndianPhoneNumber("98765432101"))
        assertFalse(PhoneNumberFormatter.isValidIndianPhoneNumber(""))
    }

    @Test
    fun `cleanPhoneNumber removes non-digit characters`() {
        assertEquals("9876543210", PhoneNumberFormatter.cleanPhoneNumber("987-654-3210"))
        assertEquals("9876543210", PhoneNumberFormatter.cleanPhoneNumber("(987) 654-3210"))
        assertEquals("9876543210", PhoneNumberFormatter.cleanPhoneNumber("+91 98765 43210"))
    }

    @Test
    fun `extractPhoneNumber extracts 10 digits from formatted number`() {
        assertEquals("9876543210", PhoneNumberFormatter.extractPhoneNumber("+919876543210"))
        assertEquals("9876543210", PhoneNumberFormatter.extractPhoneNumber("919876543210"))
        assertEquals("9876543210", PhoneNumberFormatter.extractPhoneNumber("9876543210"))
    }

    @Test
    fun `extractPhoneNumber handles short numbers`() {
        assertEquals("123", PhoneNumberFormatter.extractPhoneNumber("123"))
    }
}

