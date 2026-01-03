package com.nextserve.serveitpartnernew.data.model

import com.google.firebase.Timestamp

/**
 * Job timer data model for tracking work time on jobs
 */
data class JobTimer(
    val jobId: String = "",
    val providerId: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val totalDuration: Long = 0, // in milliseconds
    val isRunning: Boolean = false,
    val pauses: List<TimerPause> = emptyList()
)

data class TimerPause(
    val startTime: Timestamp,
    val endTime: Timestamp? = null,
    val duration: Long = 0 // in milliseconds
)

/**
 * Job notes/updates data model
 */
data class JobNote(
    val id: String = "",
    val jobId: String = "",
    val providerId: String = "",
    val note: String = "",
    val timestamp: Timestamp? = null,
    val type: String = "note", // "note", "update", "issue", "completion"
    val isVisibleToCustomer: Boolean = false
)

/**
 * Job completion checklist
 */
data class JobCompletionItem(
    val id: String = "",
    val label: String = "",
    val isCompleted: Boolean = false,
    val isRequired: Boolean = false
)

data class JobCompletionChecklist(
    val jobId: String = "",
    val providerId: String = "",
    val items: List<JobCompletionItem> = emptyList(),
    val allRequiredCompleted: Boolean = false
)

/**
 * Job photo data model
 */
data class JobPhoto(
    val id: String = "",
    val jobId: String = "",
    val providerId: String = "",
    val photoUrl: String = "",
    val photoType: String = "work", // "before", "during", "after", "issue"
    val caption: String? = null,
    val timestamp: Timestamp? = null
)

/**
 * Customer feedback data model
 */
data class CustomerFeedback(
    val jobId: String = "",
    val providerId: String = "",
    val customerId: String = "",
    val rating: Int = 5, // 1-5 stars
    val comment: String? = null,
    val timestamp: Timestamp? = null,
    val feedbackType: String = "completion" // "completion", "followup"
)
