package com.nextserve.serveitpartnernew.ui.viewmodel

import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Settlement status enum
 */
enum class SettlementStatus {
    PENDING,
    READY,
    REQUESTED,
    PROCESSING,
    SETTLED,
    FAILED
}

/**
 * Monthly settlement data model for admin management
 */
data class MonthlySettlement(
    val settlementId: String,
    val partnerId: String,
    val yearMonth: YearMonth,
    val totalEarnings: Double,
    val platformFees: Double,
    val partnerShare: Double,
    val completedJobs: Int,
    val paidAmount: Double,
    val pendingAmount: Double,
    val settlementStatus: SettlementStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    /**
     * Display period string (e.g., "January 2024")
     */
    val displayPeriod: String
        get() = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
}
