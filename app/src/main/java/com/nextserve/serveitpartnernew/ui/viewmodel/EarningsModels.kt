package com.nextserve.serveitpartnernew.ui.viewmodel

import java.time.LocalDateTime

/**
 * Represents a single earning entry from a completed booking/job.
 */
data class EarningItem(
    val bookingId: String,
    val serviceName: String,
    val completedAt: LocalDateTime,
    val amount: Double,
    val platformFee: Double,
    val partnerEarning: Double,
    val paymentStatus: PaymentStatus,
    val customerName: String? = null
)

/**
 * Payment status for earnings.
 */
enum class PaymentStatus {
    PAID,
    PENDING,
    FAILED;

    companion object {
        fun fromString(status: String): PaymentStatus {
            return when (status.lowercase()) {
                "paid" -> PAID
                "pending" -> PENDING
                "failed" -> FAILED
                else -> PENDING // Default to pending for unknown statuses
            }
        }
    }
}

/**
 * Time range filters for earnings.
 */
enum class EarningsRange {
    TODAY,
    WEEK,
    MONTH
}

/**
 * UI state for the Earnings screen.
 */
data class EarningsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedRange: EarningsRange = EarningsRange.TODAY,
    val totalEarnings: Double = 0.0,
    val completedJobs: Int = 0,
    val paidAmount: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val items: List<EarningItem> = emptyList(),
    val error: UiError? = null,
    val isOffline: Boolean = false
)
