package com.nextserve.serveitpartnernew.ui.viewmodel

import java.time.LocalDateTime
import java.time.YearMonth

/**
 * Represents a partner's bank account for payouts.
 */
data class BankAccount(
    val accountId: String = "",
    val partnerId: String = "",
    val accountHolderName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = "",
    val bankName: String = "",
    val isVerified: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    val isComplete: Boolean
        get() = accountHolderName.isNotBlank() &&
                accountNumber.isNotBlank() &&
                ifscCode.isNotBlank() &&
                bankName.isNotBlank()

    val maskedAccountNumber: String
        get() = if (accountNumber.length >= 4) {
            "****${accountNumber.takeLast(4)}"
        } else {
            accountNumber
        }
}

/**
 * Monthly earnings settlement summary.
 */
data class MonthlySettlement(
    val settlementId: String = "",
    val partnerId: String = "",
    val yearMonth: YearMonth = YearMonth.now(),
    val totalEarnings: Double = 0.0,
    val platformFees: Double = 0.0,
    val partnerShare: Double = 0.0,
    val completedJobs: Int = 0,
    val paidAmount: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val settlementStatus: SettlementStatus = SettlementStatus.PENDING,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    val availableForPayout: Double
        get() = when (settlementStatus) {
            SettlementStatus.SETTLED -> 0.0
            SettlementStatus.PROCESSING -> 0.0
            else -> partnerShare - paidAmount
        }

    val displayPeriod: String
        get() = "${yearMonth.month.name.lowercase().capitalize()} ${yearMonth.year}"
}

/**
 * Settlement status enumeration.
 */
enum class SettlementStatus {
    PENDING,      // Earnings being calculated
    READY,        // Available for payout request
    REQUESTED,    // Partner requested payout
    PROCESSING,   // Payout being processed
    SETTLED,      // Payout completed
    FAILED        // Payout failed
}

/**
 * Payout request made by partner.
 */
data class PayoutRequest(
    val requestId: String = "",
    val partnerId: String = "",
    val settlementId: String = "",
    val requestedAmount: Double = 0.0,
    val bankAccountId: String = "",
    val requestStatus: PayoutStatus = PayoutStatus.PENDING,
    val requestedAt: LocalDateTime = LocalDateTime.now(),
    val processedAt: LocalDateTime? = null,
    val failureReason: String? = null,
    val transactionId: String? = null
)

/**
 * Payout status enumeration.
 */
enum class PayoutStatus {
    PENDING,      // Waiting for admin approval
    APPROVED,     // Approved, queued for payment
    PROCESSING,   // Being processed by payment provider
    COMPLETED,    // Successfully paid
    FAILED,       // Payment failed
    CANCELLED     // Cancelled by partner or admin
}

/**
 * Completed payout transaction record.
 */
data class PayoutTransaction(
    val transactionId: String = "",
    val partnerId: String = "",
    val payoutRequestId: String = "",
    val amount: Double = 0.0,
    val bankAccountId: String = "",
    val paymentMethod: String = "BANK_TRANSFER",
    val transactionRef: String? = null,
    val status: String = "SUCCESS",
    val processedAt: LocalDateTime = LocalDateTime.now(),
    val fees: Double = 0.0,
    val notes: String? = null
)

/**
 * UI state for the Payout screen.
 */
data class PayoutUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val bankAccount: BankAccount? = null,
    val settlements: List<MonthlySettlement> = emptyList(),
    val payoutRequests: List<PayoutRequest> = emptyList(),
    val selectedSettlement: MonthlySettlement? = null,
    val error: UiError? = null,
    val isOffline: Boolean = false
)
