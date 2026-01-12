package com.nextserve.serveitpartnernew.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.graphics.Color

/**
 * Utility functions for job status display and formatting
 */
object JobStatusUtils {

    /**
     * Get status display information for a job
     * @param status The job status string (lowercased)
     * @return Triple of (color, displayText, icon)
     */
    fun getStatusDisplayInfo(status: String): Triple<Color, String, androidx.compose.ui.graphics.vector.ImageVector> {
        return when (status.lowercase()) {
            "pending" -> Triple(Color(0xFFFF9800), "Pending", Icons.Default.Info)
            "accepted" -> Triple(Color(0xFF2196F3), "Accepted", Icons.Default.CheckCircle)
            "arrived" -> Triple(Color(0xFF9C27B0), "Arrived", Icons.Default.LocationOn)
            "in_progress" -> Triple(Color(0xFF9C27B0), "In Progress", Icons.Default.Build)
            "payment_pending" -> Triple(Color(0xFFFF9800), "Payment Pending", Icons.Default.Info)
            "completed" -> Triple(Color(0xFF4CAF50), "Completed", Icons.Default.CheckCircle)
            else -> Triple(Color(0xFF8E8E93), status, Icons.Default.Info) // Gray for unknown status
        }
    }
}
