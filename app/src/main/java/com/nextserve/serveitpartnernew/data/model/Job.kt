package com.nextserve.serveitpartnernew.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Job data model representing a booking/job from Firestore
 * Handles both array and single booking formats
 */
data class Job(
    val bookingId: String,
    val serviceName: String,
    val status: String, // "pending", "accepted", "arrived", "in_progress", "payment_pending", "completed"
    val totalPrice: Double,
    val userName: String,
    val customerPhoneNumber: String, // Document ID from Bookings collection
    val notifiedProviderIds: List<String> = emptyList(),
    val providerId: String? = null,
    val providerName: String? = null,
    val providerMobileNo: String? = null,
    val acceptedByProviderId: String? = null,
    val jobCoordinates: JobCoordinates? = null,
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Timestamp? = null,
    @get:PropertyName("acceptedAt") @set:PropertyName("acceptedAt")
    var acceptedAt: Timestamp? = null,
    @get:PropertyName("arrivedAt") @set:PropertyName("arrivedAt")
    var arrivedAt: Timestamp? = null,
    @get:PropertyName("serviceStartedAt") @set:PropertyName("serviceStartedAt")
    var serviceStartedAt: Timestamp? = null,
    @get:PropertyName("completedAt") @set:PropertyName("completedAt")
    var completedAt: Timestamp? = null,
    val subServicesSelected: Map<String, Any>? = null,
    val distance: Double? = null, // Calculated distance from provider location
    val locationName: String? = null, // Location name/city for display
    val customerAddress: String? = null, // Full customer address
    val notes: String? = null, // Special instructions or notes
    val customerEmail: String? = null, // Customer email (if available)
    val estimatedDuration: Int? = null // Estimated service duration in minutes
) {
    /**
     * Check if job is available (pending and provider was notified)
     */
    fun isAvailable(providerId: String): Boolean {
        return status == "pending" && 
               providerId.isNotEmpty() &&
               (this.providerId.isNullOrEmpty()) &&
               notifiedProviderIds.contains(providerId)
    }

    /**
     * Check if job is ongoing (accepted by this provider)
     */
    fun isOngoing(providerId: String): Boolean {
        return this.providerId == providerId && 
               status in listOf("accepted", "arrived", "in_progress", "payment_pending")
    }

    /**
     * Check if job is completed
     */
    fun isCompleted(providerId: String): Boolean {
        return this.providerId == providerId && status == "completed"
    }

    /**
     * Check if status transition is allowed
     * Valid transitions:
     * - pending → accepted (via acceptJob)
     * - accepted → arrived
     * - arrived → in_progress
     * - in_progress → payment_pending
     * - payment_pending → completed
     */
    fun canTransitionTo(newStatus: String): Boolean {
        val currentStatus = status.lowercase()
        val targetStatus = newStatus.lowercase()

        return when (currentStatus) {
            "pending" -> targetStatus == "accepted" // Only via acceptJob
            "accepted" -> targetStatus == "arrived"
            "arrived" -> targetStatus == "in_progress"
            "in_progress" -> targetStatus == "payment_pending"
            "payment_pending" -> targetStatus == "completed"
            "completed" -> false // Cannot transition from completed
            else -> false
        }
    }
}

data class JobCoordinates(
    val latitude: Double,
    val longitude: Double
)

