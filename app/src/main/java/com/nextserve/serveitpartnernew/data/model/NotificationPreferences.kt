package com.nextserve.serveitpartnernew.data.model

/**
 * Notification preferences for service providers
 */
data class NotificationPreferences(
    val providerId: String = "",
    val jobOffers: Boolean = true, // New job offers
    val jobUpdates: Boolean = true, // Job status updates
    val earningsSummary: Boolean = true, // Daily/weekly earnings
    val verificationUpdates: Boolean = true, // Profile verification status
    val promotionalOffers: Boolean = false, // Marketing/promotional notifications
    val soundEnabled: Boolean = true, // Notification sound
    val vibrationEnabled: Boolean = true, // Notification vibration
    val quietHoursEnabled: Boolean = false, // Enable quiet hours
    val quietHoursStart: String = "22:00", // HH:mm format
    val quietHoursEnd: String = "08:00", // HH:mm format
    val pushNotificationsEnabled: Boolean = true // Master switch for push notifications
) {
    /**
     * Check if notifications are allowed at current time
     */
    fun areNotificationsAllowedAt(time: String): Boolean {
        if (!pushNotificationsEnabled) return false
        if (!quietHoursEnabled) return true

        // Parse current time and quiet hours
        return try {
            val current = timeToMinutes(time)
            val start = timeToMinutes(quietHoursStart)
            val end = timeToMinutes(quietHoursEnd)

            if (start < end) {
                // Quiet hours don't span midnight
                current !in start..end
            } else {
                // Quiet hours span midnight
                current !in start..(24 * 60) && current !in 0..end
            }
        } catch (e: Exception) {
            true // Default to allowing notifications if parsing fails
        }
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        if (parts.size != 2) throw IllegalArgumentException("Invalid time format")
        val hours = parts[0].toInt()
        val minutes = parts[1].toInt()
        return hours * 60 + minutes
    }
}
