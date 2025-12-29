package com.nextserve.serveitpartnernew.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Job Inbox Entry - Index/snapshot data for fast job discovery
 * 
 * IMPORTANT: This is NOT the source of truth.
 * Full booking details must be fetched from Bookings collection
 * using bookingDocPath and bookingIndex.
 */
data class JobInboxEntry(
    val bookingId: String,
    val customerPhone: String,
    val bookingDocPath: String,
    val bookingIndex: Int,
    val serviceName: String,
    val priceSnapshot: Double,
    val status: String, // "pending" | "accepted"
    val distanceKm: Double?,
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Timestamp? = null,
    @get:PropertyName("expiresAt") @set:PropertyName("expiresAt")
    var expiresAt: Timestamp? = null
) {
    /**
     * Check if inbox entry is still valid (not expired)
     */
    fun isValid(): Boolean {
        val now = Timestamp.now()
        return expiresAt?.let { it.compareTo(now) > 0 } ?: true
    }
    
    /**
     * Check if job is still pending
     */
    fun isPending(): Boolean {
        return status == "pending" && isValid()
    }
}

