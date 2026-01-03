package com.nextserve.serveitpartnernew.utils

import java.text.NumberFormat
import java.util.Locale

/**
 * Utility functions for currency formatting.
 */
object CurrencyUtils {

    private val indianRupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        currency = java.util.Currency.getInstance("INR")
    }

    /**
     * Format a double value as Indian Rupees.
     */
    fun formatCurrency(amount: Double): String {
        return try {
            indianRupeeFormat.format(amount)
        } catch (e: Exception) {
            "₹${String.format("%.2f", amount)}"
        }
    }
}

/**
 * Extension function for Double to format as currency.
 */
fun Double.formatCurrency(): String = CurrencyUtils.formatCurrency(this)
