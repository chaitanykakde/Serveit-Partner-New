package com.nextserve.serveitpartnernew.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Represents a single earning entry from Firestore earnings collection
 */
data class Earning(
    val id: String = "",
    val partnerId: String = "",
    val amount: Double = 0.0,
    @get:PropertyName("date") @set:PropertyName("date")
    var date: Timestamp? = null,
    val bookingId: String? = null,
    val serviceName: String? = null,
    val jobCompletedAt: Timestamp? = null,
    val paymentStatus: String = "pending" // pending, paid
)

/**
 * Earnings summary for a time period
 */
data class EarningsSummary(
    val totalEarnings: Double = 0.0,
    val jobsCount: Int = 0,
    val period: String = "", // "today", "week", "month"
    val earningsByService: Map<String, Double> = emptyMap()
)

