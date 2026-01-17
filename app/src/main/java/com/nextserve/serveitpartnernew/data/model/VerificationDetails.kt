package com.nextserve.serveitpartnernew.data.model

import com.google.firebase.Timestamp

/**
 * Verification details for provider verification flow.
 * This is the SINGLE SOURCE OF TRUTH for verification status.
 */
data class VerificationDetails(
    val status: String = "pending", // "pending" | "verified" | "rejected"
    val rejectedReason: String? = null,
    val verifiedBy: String? = null,
    @get:com.google.firebase.firestore.PropertyName("verifiedAt") 
    @set:com.google.firebase.firestore.PropertyName("verifiedAt")
    var verifiedAt: Timestamp? = null
) {
    companion object {
        fun pending() = VerificationDetails(status = "pending")
        fun verified(verifiedBy: String, verifiedAt: Timestamp? = null) = 
            VerificationDetails(status = "verified", verifiedBy = verifiedBy, verifiedAt = verifiedAt)
        fun rejected(reason: String) = 
            VerificationDetails(status = "rejected", rejectedReason = reason)
    }
}

