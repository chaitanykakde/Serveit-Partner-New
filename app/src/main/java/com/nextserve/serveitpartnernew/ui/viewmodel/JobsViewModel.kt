package com.nextserve.serveitpartnernew.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.data.model.JobInboxEntry
import com.nextserve.serveitpartnernew.data.repository.JobsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import com.nextserve.serveitpartnernew.utils.NetworkMonitor
import java.util.Date

/**
 * UI State for Jobs Fragment
 */
data class JobsUiState(
    val newJobs: List<Job> = emptyList(),
    val filteredJobs: List<Job> = emptyList(), // Filtered and sorted jobs for display
    val inboxEntries: List<JobInboxEntry> = emptyList(), // Inbox entries for job discovery
    val completedJobs: List<Job> = emptyList(),
    val isLoadingNewJobs: Boolean = false,
    val isLoadingHistory: Boolean = false,
    val isLoadingMoreHistory: Boolean = false,
    val hasOngoingJob: Boolean = false,
    val errorMessage: String? = null,
    val rejectedJobIds: Set<String> = emptySet(), // Local state for rejected jobs
    val acceptingJobId: String? = null, // Track which job is being accepted
    val hasMoreHistory: Boolean = true, // Pagination flag
    val isOffline: Boolean = false, // Network connectivity status
    val useInbox: Boolean = false, // Flag to use inbox for job discovery (new optimized method) - DISABLED UNTIL INBOX IS POPULATED
    // Filter and sort state
    val selectedServiceFilter: String? = null, // Filter by service type
    val maxDistanceFilter: Double? = null, // Filter by maximum distance in km
    val minPriceFilter: Double? = null, // Filter by minimum price
    val maxPriceFilter: Double? = null, // Filter by maximum price
    val sortBy: String = "distance", // "distance", "price", "time" (newest first)
    val searchQuery: String = "", // Search by service name, location, or booking ID
    val acceptanceRate: Double? = null // Provider's job acceptance rate percentage
)

/**
 * ViewModel for Jobs Fragment
 */
class JobsViewModel(
    private val providerId: String,
    private val jobsRepository: JobsRepository,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobsUiState())
    val uiState: StateFlow<JobsUiState> = _uiState.asStateFlow()

    private var totalJobsOffered = 0
    private var totalJobsAccepted = 0

    private var lastHistoryDocument: com.google.firebase.firestore.DocumentSnapshot? = null

    init {
        // Monitor network connectivity
        networkMonitor?.let { monitor ->
            viewModelScope.launch {
                monitor.connectivityFlow().collect { isConnected ->
                    _uiState.value = _uiState.value.copy(isOffline = !isConnected)
                    if (!isConnected) {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "No internet connection. Please check your network."
                        )
                    }
                }
            }
        }
        loadNewJobs()
        checkOngoingJob()
    }

    /**
     * Load new jobs using inbox (optimized) or fallback to old method
     */
    fun loadNewJobs() {
        _uiState.value = _uiState.value.copy(isLoadingNewJobs = true, errorMessage = null)

        if (_uiState.value.useInbox) {
            // NEW: Use inbox for efficient job discovery
            jobsRepository.listenToNewJobsFromInbox(providerId)
                .distinctUntilChanged()
                .catch { error ->
                    // Fallback to old method if inbox fails
                    _uiState.value = _uiState.value.copy(
                        useInbox = false,
                        errorMessage = "Inbox unavailable, using fallback method"
                    )
                    loadNewJobs() // Retry with old method
                }
                .onEach { inboxEntries ->
                    // Filter out rejected jobs
                    val rejectedIds = _uiState.value.rejectedJobIds
                    val filteredEntries = inboxEntries.filter { 
                        it.bookingId !in rejectedIds && it.isPending()
                    }
                    
                    // Convert inbox entries to Job objects for UI compatibility
                    // Note: These are lightweight - full details fetched when opening job details
                    val jobs = filteredEntries.map { entry ->
                        Job(
                            bookingId = entry.bookingId,
                            serviceName = entry.serviceName,
                            status = entry.status,
                            totalPrice = entry.priceSnapshot,
                            userName = "Customer", // Will be fetched from full details
                            customerPhoneNumber = entry.customerPhone,
                            distance = entry.distanceKm,
                            locationName = null, // Will be fetched from full details
                            createdAt = entry.createdAt,
                            expiresAt = entry.expiresAt, // Include expiry for UI display
                            // Add priority based on service type or urgency
                            notes = if (entry.serviceName.contains("Emergency", ignoreCase = true) ||
                                       entry.serviceName.contains("Urgent", ignoreCase = true))
                                "High Priority" else null
                        )
                    }

                    // Update acceptance rate calculation
                    totalJobsOffered += jobs.size
                    
                    _uiState.value = _uiState.value.copy(
                        newJobs = jobs,
                        inboxEntries = filteredEntries,
                        isLoadingNewJobs = false,
                        errorMessage = null
                    )
                    applyFiltersAndSort()
                }
                .launchIn(viewModelScope)
        } else {
            // FALLBACK: Old method (query all Bookings)
            jobsRepository.listenToNewJobs(providerId)
                .distinctUntilChanged()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingNewJobs = false,
                        errorMessage = error.message ?: "Failed to load jobs"
                    )
                }
                .onEach { jobs ->
                    // Filter out rejected jobs
                    val rejectedIds = _uiState.value.rejectedJobIds
                    val filteredJobs = jobs.filter { it.bookingId !in rejectedIds }
                    
                    _uiState.value = _uiState.value.copy(
                        newJobs = filteredJobs,
                        isLoadingNewJobs = false,
                        errorMessage = null
                    )
                    applyFiltersAndSort()
                }
                .launchIn(viewModelScope)
        }
    }

    /**
     * Check if provider has ongoing job
     */
    private fun checkOngoingJob() {
        viewModelScope.launch {
            try {
                val hasOngoing = jobsRepository.hasOngoingJob(providerId)
                _uiState.value = _uiState.value.copy(hasOngoingJob = hasOngoing)
            } catch (e: Exception) {
                // Silently fail - not critical
            }
        }
    }

    /**
     * Load completed jobs history (first page)
     */
    fun loadHistory() {
        if (_uiState.value.completedJobs.isNotEmpty()) {
            return // Already loaded
        }

        // Check network connectivity
        if (networkMonitor != null && !networkMonitor.isConnected()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No internet connection. Please check your network and try again."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHistory = true, errorMessage = null)

            val result = jobsRepository.getCompletedJobs(providerId, limit = 20)
            result.fold(
                onSuccess = { (jobs, lastDoc) ->
                    lastHistoryDocument = lastDoc
                    _uiState.value = _uiState.value.copy(
                        completedJobs = jobs,
                        isLoadingHistory = false,
                        hasMoreHistory = jobs.isNotEmpty() && lastDoc != null,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    android.util.Log.e("JobsViewModel", "Failed to load job history", error)
                    _uiState.value = _uiState.value.copy(
                        isLoadingHistory = false,
                        errorMessage = when {
                            error.message?.contains("network", ignoreCase = true) == true ->
                                "Network connection failed. Please check your internet connection and try again."
                            error.message?.contains("permission", ignoreCase = true) == true ->
                                "Permission denied. Please check your permissions and try again."
                            else -> "Unable to load job history. Please try again later."
                        }
                    )
                }
            )
        }
    }

    /**
     * Load more completed jobs (pagination)
     */
    fun loadMoreHistory() {
        if (_uiState.value.isLoadingMoreHistory || !_uiState.value.hasMoreHistory) {
            return
        }

        // Check network connectivity
        if (networkMonitor != null && !networkMonitor.isConnected()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No internet connection. Please check your network and try again."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMoreHistory = true)

            val result = jobsRepository.getCompletedJobs(
                providerId,
                limit = 20,
                lastDocument = lastHistoryDocument
            )

            result.fold(
                onSuccess = { (jobs, lastDoc) ->
                    lastHistoryDocument = lastDoc
                    val currentJobs = _uiState.value.completedJobs
                    _uiState.value = _uiState.value.copy(
                        completedJobs = currentJobs + jobs,
                        isLoadingMoreHistory = false,
                        hasMoreHistory = jobs.isNotEmpty() && lastDoc != null,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMoreHistory = false,
                        hasMoreHistory = true, // Keep hasMoreHistory true to allow retry
                        errorMessage = "Failed to load more history: ${error.message ?: "Unknown error"}. Pull down to retry."
                    )
                }
            )
        }
    }

    /**
     * Accept a job
     */
    fun acceptJob(job: Job, onSuccess: () -> Unit, onError: (String) -> Unit) {
        totalJobsAccepted += 1 // Track accepted jobs for rate calculation
        updateAcceptanceRate()
        if (_uiState.value.hasOngoingJob) {
            onError("Complete ongoing job to accept new requests")
            return
        }

        if (_uiState.value.acceptingJobId != null) {
            return // Already processing
        }

        // Check network connectivity
        if (networkMonitor != null && !networkMonitor.isConnected()) {
            onError("No internet connection. Please check your network and try again.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                acceptingJobId = job.bookingId,
                errorMessage = null
            )

            val result = jobsRepository.acceptJob(job.bookingId, providerId)
            result.fold(
                onSuccess = {
                    // Remove job from new jobs list (optimistic)
                    val updatedJobs = _uiState.value.newJobs.filter { it.bookingId != job.bookingId }
                    _uiState.value = _uiState.value.copy(
                        newJobs = updatedJobs,
                        acceptingJobId = null
                    )
                    // Wait for Firestore to propagate the update
                    delay(500)
                    // Refresh new jobs to ensure list is up to date
                    loadNewJobs()
                    // Refresh ongoing job status
                    refreshOngoingJobStatus()
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        acceptingJobId = null,
                        errorMessage = error.message ?: "Failed to accept job"
                    )
                    onError(error.message ?: "Failed to accept job")
                }
            )
        }
    }

    /**
     * Reject a job (local only, no Firestore write)
     */
    fun rejectJob(job: Job) {
        val updatedRejectedIds = _uiState.value.rejectedJobIds + job.bookingId
        val updatedJobs = _uiState.value.newJobs.filter { it.bookingId != job.bookingId }
        
        _uiState.value = _uiState.value.copy(
            rejectedJobIds = updatedRejectedIds,
            newJobs = updatedJobs
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Refresh ongoing job status
     */
    fun refreshOngoingJobStatus() {
        checkOngoingJob()
    }

    /**
     * Apply filters and sorting to new jobs
     */
    private fun applyFiltersAndSort() {
        var filtered = _uiState.value.newJobs

        // Apply service filter
        _uiState.value.selectedServiceFilter?.let { service ->
            if (service.isNotEmpty()) {
                filtered = filtered.filter { 
                    it.serviceName.contains(service, ignoreCase = true) 
                }
            }
        }

        // Apply distance filter
        _uiState.value.maxDistanceFilter?.let { maxDistance ->
            filtered = filtered.filter { 
                it.distance == null || it.distance <= maxDistance 
            }
        }

        // Apply price filters
        _uiState.value.minPriceFilter?.let { minPrice ->
            filtered = filtered.filter { it.totalPrice >= minPrice }
        }
        _uiState.value.maxPriceFilter?.let { maxPrice ->
            filtered = filtered.filter { it.totalPrice <= maxPrice }
        }

        // Apply search query
        if (_uiState.value.searchQuery.isNotEmpty()) {
            val query = _uiState.value.searchQuery.lowercase()
            filtered = filtered.filter { job ->
                job.serviceName.lowercase().contains(query) ||
                job.locationName?.lowercase()?.contains(query) == true ||
                job.bookingId.lowercase().contains(query) ||
                job.userName.lowercase().contains(query)
            }
        }

        // Apply sorting
        filtered = when (_uiState.value.sortBy) {
            "distance" -> filtered.sortedBy { it.distance ?: Double.MAX_VALUE }
            "price" -> filtered.sortedByDescending { it.totalPrice }
            "time" -> filtered.sortedByDescending { it.createdAt?.toDate() ?: Date(0) }
            else -> filtered
        }

        _uiState.value = _uiState.value.copy(filteredJobs = filtered)
    }

    /**
     * Set service filter
     */
    fun setServiceFilter(service: String?) {
        _uiState.value = _uiState.value.copy(selectedServiceFilter = service)
        applyFiltersAndSort()
    }

    /**
     * Set distance filter
     */
    fun setDistanceFilter(maxDistance: Double?) {
        _uiState.value = _uiState.value.copy(maxDistanceFilter = maxDistance)
        applyFiltersAndSort()
    }

    /**
     * Set price filter
     */
    fun setPriceFilter(minPrice: Double?, maxPrice: Double?) {
        _uiState.value = _uiState.value.copy(
            minPriceFilter = minPrice,
            maxPriceFilter = maxPrice
        )
        applyFiltersAndSort()
    }

    /**
     * Set sort option
     */
    fun setSortBy(sortBy: String) {
        _uiState.value = _uiState.value.copy(sortBy = sortBy)
        applyFiltersAndSort()
    }

    /**
     * Set search query
     */
    fun setSearchQuery(query: String) {
        // Sanitize input - trim whitespace and limit length
        val sanitizedQuery = query.trim().take(100) // Limit to 100 characters for security
        _uiState.value = _uiState.value.copy(searchQuery = sanitizedQuery)
        applyFiltersAndSort()
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            selectedServiceFilter = null,
            maxDistanceFilter = null,
            minPriceFilter = null,
            maxPriceFilter = null,
            searchQuery = "",
            sortBy = "distance"
        )
        applyFiltersAndSort()
    }

    /**
     * Calculate and update acceptance rate
     */
    private fun updateAcceptanceRate() {
        val rate = if (totalJobsOffered > 0) {
            (totalJobsAccepted.toDouble() / totalJobsOffered.toDouble()) * 100.0
        } else {
            null
        }
        _uiState.value = _uiState.value.copy(acceptanceRate = rate)
    }

    /**
     * Reject job with reason tracking
     */
    fun rejectJobWithReason(job: Job, reason: String) {
        // Store rejection reason (could be saved to Firestore in future)
        rejectJob(job)
        // TODO: Track rejection reasons for analytics
    }

    companion object {
        /**
         * Factory for creating JobsViewModel
         */
        fun factory(
            providerId: String,
            context: android.content.Context
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(JobsViewModel::class.java)) {
                        val networkMonitor = NetworkMonitor(context)
                        val jobsRepository = com.nextserve.serveitpartnernew.data.repository.JobsRepository(
                            com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider.firestore,
                            com.google.firebase.functions.FirebaseFunctions.getInstance()
                        )
                        return JobsViewModel(providerId, jobsRepository, networkMonitor) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

