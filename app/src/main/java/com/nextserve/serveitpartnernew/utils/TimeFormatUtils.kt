package com.nextserve.serveitpartnernew.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Time and date formatting utilities for job cards
 * Formats timestamps based on job state and context
 */
object TimeFormatUtils {
    private val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val timeRangeFormat = SimpleDateFormat("h a", Locale.getDefault())

    /**
     * Format time for NEW REQUEST (Pending Job)
     * Uses createdAt timestamp
     * Format: "Today · 6–7 PM" or "Tomorrow · 10–11 AM" or "12 Jan · 6–7 PM"
     */
    fun formatNewRequestTime(createdAt: Timestamp?): String? {
        android.util.Log.d("TimeFormatUtils", "⏰ Formatting time for createdAt: $createdAt")
        if (createdAt == null) {
            android.util.Log.w("TimeFormatUtils", "⚠️ createdAt is null")
            return null
        }

        val createdDate = createdAt.toDate()
        android.util.Log.d("TimeFormatUtils", "📅 Created date: $createdDate")
        val now = Date()
        val calendar = Calendar.getInstance()
        calendar.time = now
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.time

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStart = calendar.time

        val createdCalendar = Calendar.getInstance()
        createdCalendar.time = createdDate

        // Determine day label
        val dayLabel = when {
            createdDate >= todayStart && createdDate < tomorrowStart -> "Today"
            createdDate >= tomorrowStart && createdDate.before(Date(tomorrowStart.time + 24 * 60 * 60 * 1000)) -> "Tomorrow"
            else -> dateFormat.format(createdDate)
        }

        // Format time range (using created time + 1 hour as estimate)
        val startTime = timeRangeFormat.format(createdDate)
        createdCalendar.add(Calendar.HOUR_OF_DAY, 1)
        val endTime = timeRangeFormat.format(createdCalendar.time)

        return "$dayLabel · $startTime–$endTime"
    }

    /**
     * Format time for ONGOING JOB
     * Uses acceptedAt timestamp (status-focused, not time-focused)
     * Returns null as time is not primary focus for ongoing jobs
     */
    fun formatOngoingJobTime(acceptedAt: Timestamp?): String? {
        // Time not required for ongoing jobs per requirements
        // Status is more important than time
        return null
    }

    /**
     * Format time for COMPLETED JOB
     * Uses completedAt timestamp
     * Returns null as time is not required on Home screen for completed jobs
     */
    fun formatCompletedJobTime(completedAt: Timestamp?): String? {
        // Time not required for completed jobs on Home screen per requirements
        return null
    }
}

