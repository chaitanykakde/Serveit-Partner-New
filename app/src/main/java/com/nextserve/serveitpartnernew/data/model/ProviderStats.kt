package com.nextserve.serveitpartnernew.data.model

data class ProviderStats(
    val rating: Double = 0.0,
    val totalJobs: Int = 0,
    val totalEarnings: Double = 0.0,
    val completedJobs: Int = 0,
    val pendingJobs: Int = 0,
    val cancelledJobs: Int = 0
) {
    fun getFormattedEarnings(): String {
        return when {
            totalEarnings >= 100000 -> "₹${String.format("%.1f", totalEarnings / 100000)}L"
            totalEarnings >= 1000 -> "₹${String.format("%.1f", totalEarnings / 1000)}k"
            else -> "₹${totalEarnings.toInt()}"
        }
    }
    
    fun getFormattedRating(): String {
        return String.format("%.1f", rating)
    }
}

