package com.nextserve.serveitpartnernew.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.data.repository.JobsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.nextserve.serveitpartnernew.utils.NetworkMonitor

/**
 * UI State for Home Fragment
 */
data class HomeUiState(
    val highlightedJob: Job? = null, // ONE best new job to highlight
    val ongoingJobs: List<Job> = emptyList(),
    val isLoading: Boolean = false,
    val hasOngoingJob: Boolean = false,
    val errorMessage: String? = null,
    val acceptingJobId: String? = null, // Track which job is being accepted
    val isOffline: Boolean = false, // Network connectivity status
    val todayCompletedJobs: List<Job> = emptyList(), // Today's completed jobs
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

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

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

        // Combine both flows
        combine(newJobsFlow, ongoingJobsFlow) { newJobs, ongoingJobs ->
            // Filter out rejected jobs
            val availableNewJobs = newJobs.filter { it.bookingId !in rejectedJobIds }
            
            // Select ONE highlighted job (nearest or earliest)
            val highlighted = selectHighlightedJob(availableNewJobs)
            
            // Check if has ongoing job
            val hasOngoing = ongoingJobs.isNotEmpty()

            HomeUiState(
                highlightedJob = highlighted,
                ongoingJobs = ongoingJobs,
                isLoading = false,
                hasOngoingJob = hasOngoing,
                errorMessage = null,
                acceptingJobId = _uiState.value.acceptingJobId
            )
        }
            .onEach { newState ->
                _uiState.value = newState
            }
            .launchIn(viewModelScope)
    }

    /**
     * Select ONE highlighted job from available new jobs
     * Priority: Nearest job OR earliest created
     */
    private fun selectHighlightedJob(jobs: List<Job>): Job? {
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
    fun acceptJob(job: Job, onSuccess: () -> Unit, onError: (String) -> Unit) {
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
    fun rejectJob(job: Job) {
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

