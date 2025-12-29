package com.nextserve.serveitpartnernew.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.data.repository.JobsRepository
import com.nextserve.serveitpartnernew.utils.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UI State for Job Details Screen
 */
data class JobDetailsUiState(
    val job: Job? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAccepting: Boolean = false,
    val isRejecting: Boolean = false,
    val isUpdatingStatus: Boolean = false,
    val updatingStatusType: String? = null // "arrived", "in_progress", "payment_pending", "completed"
)

/**
 * ViewModel for Job Details Screen
 */
class JobDetailsViewModel(
    private val bookingId: String,
    private val customerPhoneNumber: String,
    private val providerId: String,
    private val jobsRepository: JobsRepository,
    val networkMonitor: NetworkMonitor? = null, // Made public for UI access
    private val bookingIndex: Int? = null // Optional: if provided, use optimized direct access
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobDetailsUiState())
    val uiState: StateFlow<JobDetailsUiState> = _uiState.asStateFlow()

    init {
        loadJobDetails()
    }

    /**
     * Load complete job details from SOURCE OF TRUTH
     * If bookingIndex is provided, use optimized direct access
     * Otherwise, fall back to searching all bookings
     */
    fun loadJobDetails() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            val result = if (bookingIndex != null) {
                // OPTIMIZED: Direct access using bookingIndex (from inbox)
                jobsRepository.getFullBookingDetails(customerPhoneNumber, bookingIndex)
            } else {
                // FALLBACK: Search method (for backward compatibility)
                jobsRepository.getJobDetails(bookingId, customerPhoneNumber)
            }
            
            result.fold(
                onSuccess = { job ->
                    _uiState.value = _uiState.value.copy(
                        job = job,
                        isLoading = false,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load job details"
                    )
                }
            )
        }
    }

    /**
     * Accept a job
     */
    fun acceptJob(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val job = _uiState.value.job ?: return
        
        if (networkMonitor != null && !networkMonitor.isConnected()) {
            onError("No internet connection. Please check your network and try again.")
            return
        }

        _uiState.value = _uiState.value.copy(isAccepting = true, errorMessage = null)
        
        viewModelScope.launch {
            val result = jobsRepository.acceptJob(bookingId, providerId)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isAccepting = false)
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isAccepting = false,
                        errorMessage = error.message ?: "Failed to accept job"
                    )
                    onError(error.message ?: "Failed to accept job")
                }
            )
        }
    }

    /**
     * Reject a job (local only)
     */
    fun rejectJob(onSuccess: () -> Unit) {
        _uiState.value = _uiState.value.copy(isRejecting = true)
        // Rejection is handled locally in JobsViewModel
        // This is just for UI feedback
        viewModelScope.launch {
            kotlinx.coroutines.delay(300) // Brief delay for UI feedback
            _uiState.value = _uiState.value.copy(isRejecting = false)
            onSuccess()
        }
    }

    /**
     * Mark job as arrived
     */
    fun markAsArrived(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val job = _uiState.value.job ?: return
        if (!job.canTransitionTo("arrived")) {
            onError("Cannot mark as arrived. Current status: ${job.status}")
            return
        }
        updateJobStatus("arrived", onSuccess, onError)
    }

    /**
     * Mark job as in progress
     */
    fun markAsInProgress(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val job = _uiState.value.job ?: return
        if (!job.canTransitionTo("in_progress")) {
            onError("Cannot start service. Current status: ${job.status}")
            return
        }
        updateJobStatus("in_progress", onSuccess, onError)
    }

    /**
     * Mark job as payment pending
     */
    fun markAsPaymentPending(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val job = _uiState.value.job ?: return
        if (!job.canTransitionTo("payment_pending")) {
            onError("Cannot mark as payment pending. Current status: ${job.status}")
            return
        }
        updateJobStatus("payment_pending", onSuccess, onError)
    }

    /**
     * Mark job as completed
     */
    fun markAsCompleted(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val job = _uiState.value.job ?: return
        if (!job.canTransitionTo("completed")) {
            onError("Cannot mark as completed. Current status: ${job.status}")
            return
        }
        updateJobStatus("completed", onSuccess, onError)
    }

    /**
     * Generic function to update job status
     */
    private fun updateJobStatus(newStatus: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val job = _uiState.value.job ?: return

        if (networkMonitor != null && !networkMonitor.isConnected()) {
            onError("No internet connection. Please check your network and try again.")
            return
        }

        _uiState.value = _uiState.value.copy(
            isUpdatingStatus = true,
            updatingStatusType = newStatus,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = when (newStatus) {
                "arrived" -> jobsRepository.markJobAsArrived(bookingId, customerPhoneNumber)
                "in_progress" -> jobsRepository.markJobAsInProgress(bookingId, customerPhoneNumber)
                "payment_pending" -> jobsRepository.markJobAsPaymentPending(bookingId, customerPhoneNumber)
                "completed" -> jobsRepository.markJobAsCompleted(bookingId, customerPhoneNumber)
                else -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdatingStatus = false,
                        updatingStatusType = null
                    )
                    onError("Invalid status: $newStatus")
                    return@launch
                }
            }

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isUpdatingStatus = false,
                        updatingStatusType = null
                    )
                    // Reload job details to get updated status
                    loadJobDetails()
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isUpdatingStatus = false,
                        updatingStatusType = null,
                        errorMessage = error.message ?: "Failed to update job status"
                    )
                    onError(error.message ?: "Failed to update job status")
                }
            )
        }
    }

    /**
     * Format timestamp to relative time string
     */
    fun formatRelativeTime(timestamp: com.google.firebase.Timestamp?): String {
        if (timestamp == null) return "Not set"
        
        val date = timestamp.toDate()
        val now = Date()
        val diff = now.time - date.time
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "$minutes minute${if (minutes != 1L) "s" else ""} ago"
            hours < 24 -> "$hours hour${if (hours != 1L) "s" else ""} ago"
            days < 7 -> "$days day${if (days != 1L) "s" else ""} ago"
            else -> {
                val format = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                format.format(date)
            }
        }
    }

    /**
     * Format timestamp to full date string
     */
    fun formatFullDate(timestamp: com.google.firebase.Timestamp?): String {
        if (timestamp == null) return "Not set"
        
        val format = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return format.format(timestamp.toDate())
    }

    /**
     * Calculate distance string
     */
    fun formatDistance(distance: Double?): String {
        if (distance == null) return "Distance not available"
        
        return when {
            distance < 1.0 -> "${(distance * 1000).toInt()} m"
            else -> String.format(Locale.getDefault(), "%.2f km", distance)
        }
    }

    /**
     * Open phone dialer with customer number
     */
    fun callCustomer(context: Context) {
        val phoneNumber = _uiState.value.job?.customerPhoneNumber ?: return
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        context.startActivity(intent)
    }

    /**
     * Open maps with customer location
     */
    fun navigateToLocation(context: Context) {
        val coordinates = _uiState.value.job?.jobCoordinates ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("geo:${coordinates.latitude},${coordinates.longitude}?q=${coordinates.latitude},${coordinates.longitude}")
        }
        context.startActivity(intent)
    }

    companion object {
        /**
         * Factory for creating JobDetailsViewModel
         */
        fun factory(
            bookingId: String,
            customerPhoneNumber: String,
            providerId: String,
            context: Context,
            bookingIndex: Int? = null // Optional: from inbox entry for optimized access
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(JobDetailsViewModel::class.java)) {
                        val networkMonitor = NetworkMonitor(context)
                        val jobsRepository = JobsRepository(
                            com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider.firestore,
                            com.google.firebase.functions.FirebaseFunctions.getInstance()
                        )
                        return JobDetailsViewModel(
                            bookingId = bookingId,
                            customerPhoneNumber = customerPhoneNumber,
                            providerId = providerId,
                            jobsRepository = jobsRepository,
                            networkMonitor = networkMonitor,
                            bookingIndex = bookingIndex
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

