package com.nextserve.serveitpartnernew.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Notification model for provider notifications
 */
data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "", // "profile_status_update", "new_job_alert", "earnings_summary", etc.
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Timestamp? = null,
    val isRead: Boolean = false,
    val userId: String = "",
    val relatedData: Map<String, Any>? = null
) {
    /**
     * Check if notification is recent (within last 24 hours)
     */
    fun isRecent(): Boolean {
        timestamp?.let { ts ->
            val now = Timestamp.now()
            val diff = now.toDate().time - ts.toDate().time
            return diff < 24 * 60 * 60 * 1000 // 24 hours
        }
        return false
    }
}

