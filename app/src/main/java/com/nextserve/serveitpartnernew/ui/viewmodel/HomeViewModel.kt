package com.nextserve.serveitpartnernew.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextserve.serveitpartnernew.data.model.Job as BookingJob
import kotlinx.coroutines.Job as CoroutineJob
import com.nextserve.serveitpartnernew.data.model.JobInboxEntry
import com.nextserve.serveitpartnernew.data.repository.JobsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.nextserve.serveitpartnernew.utils.NetworkMonitor

/**
 * UI State for Home Fragment
 */
data class HomeUiState(
    val highlightedJob: BookingJob? = null, // ONE best new job to highlight
    val ongoingJobs: List<BookingJob> = emptyList(),
    val isLoading: Boolean = false,
    val hasOngoingJob: Boolean = false,
    val errorMessage: String? = null,
    val acceptingJobId: String? = null, // Track which job is being accepted
    val isOffline: Boolean = false, // Network connectivity status
    val todayCompletedJobs: List<BookingJob> = emptyList(), // Today's completed jobs
    val todayStats: Pair<Int, Double> = Pair(0, 0.0) // (jobs count, earnings)
)

/**
 * ViewModel for Home Fragment
 */
class HomeViewModel(
    private val providerId: String,
    val jobsRepository: JobsRepository,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {


    // Timeout job to prevent infinite skeleton
    private var skeletonTimeoutJob: CoroutineJob? = null

    init {
        android.util.Log.d("HomeViewModel", "🏗️ HomeViewModel created for provider: $providerId")
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        android.util.Log.d("HomeViewModel", "🔄 HomeViewModel init block starting...")
        loadHomeData()
        loadTodayStats()
        android.util.Log.d("HomeViewModel", "✅ HomeViewModel init block completed")
    }

    private var rejectedJobIds = mutableSetOf<String>()

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
        loadHomeData()
        loadTodayStats()
    }

    /**
     * Load home data: highlighted job and ongoing jobs
     */
    private fun loadHomeData() {
        android.util.Log.d("HomeViewModel", "🚀 loadHomeData() called - setting isLoading = true")
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        // Cancel any existing timeout
        skeletonTimeoutJob?.cancel()

        // Timeout: Stop skeleton after 3 seconds even if no data (to prevent infinite loading)
        skeletonTimeoutJob = viewModelScope.launch {
            delay(3000)
            val currentState = _uiState.value
            if (currentState.isLoading && currentState.highlightedJob == null && currentState.ongoingJobs.isEmpty()) {
                android.util.Log.d("HomeViewModel", "⏰ Skeleton timeout - showing empty state")
                _uiState.value = currentState.copy(isLoading = false)
            }
        }

        // Listen to new jobs for highlighted job
        val newJobsFlow = jobsRepository.listenToNewJobs(providerId)
            .distinctUntilChanged()
            .retry(retries = 3) { throwable ->
                // Retry with exponential backoff
                delay(1000L)
                true // Retry on any error
            }
            .catch { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to load jobs. Pull down to retry."
                )
                emit(emptyList()) // Emit empty list on final failure
            }

        // Listen to ongoing jobs
        val ongoingJobsFlow = jobsRepository.listenToOngoingJobs(providerId)
            .distinctUntilChanged()
            .retry(retries = 3) { throwable ->
                delay(1000L)
                true
            }
            .catch { error ->
                // Emit empty list on error for ongoing jobs
                emit(emptyList())
            }

        // Combine both flows with immediate emission (fixes infinite skeletons)
        android.util.Log.d("HomeViewModel", "🔗 Setting up combine operation for newJobs + ongoingJobs")
        combine(
            newJobsFlow.onStart {
                android.util.Log.d("HomeViewModel", "📤 newJobsFlow.onStart - emitting emptyList()")
                emit(emptyList())
            },
            ongoingJobsFlow.onStart {
                android.util.Log.d("HomeViewModel", "📤 ongoingJobsFlow.onStart - emitting emptyList()")
                emit(emptyList())
            }
        ) { newJobs, ongoingJobs ->
            android.util.Log.d("HomeViewModel", "🔄 combine lambda called - newJobs: ${newJobs.size}, ongoingJobs: ${ongoingJobs.size}")

            // Filter out rejected jobs
            val availableNewJobs = newJobs.filter { it.bookingId !in rejectedJobIds }
            android.util.Log.d("HomeViewModel", "🔍 Filtered newJobs: ${availableNewJobs.size} (rejected: ${newJobs.size - availableNewJobs.size})")

            // Select ONE highlighted job (nearest or earliest)
            val highlighted = selectHighlightedJob(availableNewJobs)
            android.util.Log.d("HomeViewModel", "⭐ Selected highlighted job: ${highlighted?.serviceName ?: "null"}")

            // Check if has ongoing job
            val hasOngoing = ongoingJobs.isNotEmpty()
            android.util.Log.d("HomeViewModel", "📋 hasOngoingJob: $hasOngoing")

            // Only stop loading when we have data to show OR when we can show empty state
            val hasDataToShow = highlighted != null || ongoingJobs.isNotEmpty()
            val shouldStopLoading = hasDataToShow

            // Cancel timeout if we have data
            if (shouldStopLoading) {
                skeletonTimeoutJob?.cancel()
                android.util.Log.d("HomeViewModel", "✅ Data received - canceling skeleton timeout")
            }

            val newState = _uiState.value.copy(
                highlightedJob = highlighted,
                ongoingJobs = ongoingJobs,
                hasOngoingJob = hasOngoing,
                isLoading = !shouldStopLoading,
                errorMessage = null
            )
            android.util.Log.d("HomeViewModel", "📊 Emitting new state - isLoading: ${!shouldStopLoading}, highlighted: ${highlighted != null}, ongoing: ${ongoingJobs.size}")
            newState
        }
            .onEach { newState ->
                android.util.Log.d("HomeViewModel", "💾 Applying new state to _uiState")
                _uiState.value = newState
            }
            .launchIn(viewModelScope)
    }

    /**
     * Select ONE highlighted job from available new jobs
     * Priority: Nearest job OR earliest created
     */
    private fun selectHighlightedJob(jobs: List<BookingJob>): BookingJob? {
        if (jobs.isEmpty()) return null

        // If jobs have distance info, pick nearest
        val jobsWithDistance = jobs.filter { it.distance != null }
        if (jobsWithDistance.isNotEmpty()) {
            return jobsWithDistance.minByOrNull { it.distance ?: Double.MAX_VALUE }
        }

        // Otherwise, pick earliest created
        return jobs.minByOrNull { 
            it.createdAt?.toDate()?.time ?: Long.MAX_VALUE 
        }
    }

    /**
     * Accept highlighted job
     */
    fun acceptJob(job: BookingJob, onSuccess: () -> Unit, onError: (String) -> Unit) {
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
                    // Remove from highlighted (optimistic)
                    _uiState.value = _uiState.value.copy(
                        highlightedJob = null,
                        acceptingJobId = null
                    )
                    // Wait for Firestore to propagate the update
                    delay(500)
                    // Refresh ongoing jobs to pick up the newly accepted job
                    refreshOngoingJobs()
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
     * Reject highlighted job (local only)
     */
    fun rejectJob(job: BookingJob) {
        rejectedJobIds.add(job.bookingId)
        _uiState.value = _uiState.value.copy(
            highlightedJob = null
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Load today's completed jobs and stats
     */
    private fun loadTodayStats() {
        viewModelScope.launch {
            val result = jobsRepository.getCompletedJobs(providerId, limit = 50)
            result.fold(
                onSuccess = { (jobs, _) ->
                    val today = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    
                    val todayJobs = jobs.filter { job ->
                        job.completedAt?.toDate()?.time?.let { it >= today } == true
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        todayCompletedJobs = todayJobs,
                        todayStats = Pair(
                            todayJobs.size,
                            todayJobs.sumOf { it.totalPrice }
                        )
                    )
                },
                onFailure = { }
            )
        }
    }

    /**
     * Refresh home data
     */
    fun refresh() {
        loadHomeData()
        loadTodayStats()
    }

    /**
     * Manually refresh ongoing jobs
     * Used after accepting a job to ensure it appears immediately
     */
    fun refreshOngoingJobs() {
        // The ongoing jobs flow will automatically update via the listener
        // This function can be used to trigger a manual refresh if needed
        loadHomeData()
    }

    companion object {
        /**
         * Factory for creating HomeViewModel
         */
        fun factory(
            providerId: String,
            context: android.content.Context
        ): androidx.lifecycle.ViewModelProvider.Factory {
            return object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                        val networkMonitor = NetworkMonitor(context)
                        val jobsRepository = com.nextserve.serveitpartnernew.data.repository.JobsRepository(
                            com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider.firestore,
                            com.google.firebase.functions.FirebaseFunctions.getInstance()
                        )
                        return HomeViewModel(providerId, jobsRepository, networkMonitor) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

