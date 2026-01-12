package com.nextserve.serveitpartnernew.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextserve.serveitpartnernew.data.model.Job as BookingJob
import kotlinx.coroutines.Job as CoroutineJob
import com.nextserve.serveitpartnernew.data.model.JobInboxEntry
import com.nextserve.serveitpartnernew.data.repository.JobsRepository
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import com.nextserve.serveitpartnernew.utils.DistanceUtils
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
    private val firestoreRepository: FirestoreRepository,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    // Provider location for distance calculation
    private var providerLatitude: Double? = null
    private var providerLongitude: Double? = null

    // Timeout job to prevent infinite skeleton
    private var skeletonTimeoutJob: CoroutineJob? = null

    init {
        android.util.Log.d("HomeViewModel", "🏗️ HomeViewModel created for provider: $providerId")
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        android.util.Log.d("HomeViewModel", "🔄 HomeViewModel init block starting...")
        loadProviderLocation()
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
     * Load provider location for distance calculation
     */
    private fun loadProviderLocation() {
        viewModelScope.launch {
            android.util.Log.d("HomeViewModel", "🔍 Loading provider location for providerId: $providerId")
            val result = firestoreRepository.getProviderData(providerId)
            result.onSuccess { providerData ->
                if (providerData == null) {
                    android.util.Log.w("HomeViewModel", "⚠️ Provider data is null for providerId: $providerId")
                } else {
                    providerLatitude = providerData.latitude
                    providerLongitude = providerData.longitude
                    android.util.Log.d("HomeViewModel", "📍 Provider location loaded: lat=${providerData.latitude}, lon=${providerData.longitude}")
                    if (providerData.latitude == null || providerData.longitude == null) {
                        android.util.Log.w("HomeViewModel", "⚠️ Provider location is missing! lat=${providerData.latitude}, lon=${providerData.longitude}")
                    }
                }
            }.onFailure { error ->
                android.util.Log.e("HomeViewModel", "❌ Failed to load provider location", error)
            }
        }
    }

    /**
     * Calculate distance for a job using provider location and job coordinates (synchronous version)
     * Returns the distance in km, or null if cannot calculate
     * For testing: returns minimum 1 km if calculated distance is 0
     */
    private fun calculateJobDistanceSync(job: BookingJob): Double? {
        android.util.Log.d("HomeViewModel", "📏 Calculating distance for job: ${job.bookingId}")
        
        // If distance already exists, use it (but ensure minimum 1 km for testing)
        if (job.distance != null && job.distance!! > 0) {
            val finalDistance = if (job.distance == 0.0) 1.0 else job.distance!!
            android.util.Log.d("HomeViewModel", "✅ Job already has distance: ${job.distance} km -> using $finalDistance km for testing")
            return finalDistance
        }

        // Calculate distance if we have both provider and job coordinates
        val providerLat = providerLatitude
        val providerLon = providerLongitude
        val jobCoords = job.jobCoordinates

        android.util.Log.d("HomeViewModel", "🔍 Provider location: lat=$providerLat, lon=$providerLon")
        android.util.Log.d("HomeViewModel", "🔍 Job coordinates: ${jobCoords?.let { "lat=${it.latitude}, lon=${it.longitude}" } ?: "null"}")

        if (providerLat != null && providerLon != null && jobCoords != null) {
            val distance = DistanceUtils.calculateDistance(
                providerLat,
                providerLon,
                jobCoords.latitude,
                jobCoords.longitude
            )
            // For testing: if distance is 0, return 1 km
            val finalDistance = if (distance != null && distance == 0.0) {
                android.util.Log.d("HomeViewModel", "🧪 Testing: Distance is 0, returning 1.0 km for testing")
                1.0
            } else {
                distance
            }
            android.util.Log.d("HomeViewModel", "📐 Calculated distance: ${distance ?: "null"} km -> final: ${finalDistance ?: "null"} km")
            return finalDistance
        } else {
            android.util.Log.w("HomeViewModel", "⚠️ Cannot calculate distance - missing data: providerLat=$providerLat, providerLon=$providerLon, jobCoords=$jobCoords")
        }

        return null
    }

    /**
     * Fetch customer address from serveit_users collection if not available in job
     */
    private suspend fun enrichJobWithAddress(job: BookingJob): BookingJob {
        // If address already exists, return as is
        if (!job.customerAddress.isNullOrBlank()) {
            android.util.Log.d("HomeViewModel", "✅ Job ${job.bookingId} already has address: ${job.customerAddress?.take(30)}")
            return job
        }

        // Try to fetch from serveit_users
        android.util.Log.d("HomeViewModel", "🔍 Job ${job.bookingId} missing address, fetching from serveit_users/${job.customerPhoneNumber}")
        val address = jobsRepository.fetchCustomerAddress(job.customerPhoneNumber)
        
        if (address != null) {
            android.util.Log.d("HomeViewModel", "✅ Fetched address for job ${job.bookingId}: ${address.take(50)}")
            return job.copy(customerAddress = address)
        } else {
            android.util.Log.w("HomeViewModel", "⚠️ Could not fetch address for job ${job.bookingId} from serveit_users")
            return job
        }
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

            // Calculate distances for all jobs and update Job objects
            // Note: Address enrichment will happen asynchronously in a separate flow
            android.util.Log.d("HomeViewModel", "📊 Calculating distances for ${availableNewJobs.size} new jobs and ${ongoingJobs.size} ongoing jobs")
            
            // Process new jobs: calculate distance synchronously (for testing: min 1 km if 0)
            val newJobsWithDistance = availableNewJobs.map { job ->
                val distance = calculateJobDistanceSync(job)
                if (distance != null) {
                    android.util.Log.d("HomeViewModel", "✅ New job ${job.bookingId}: distance=$distance km, address=${job.customerAddress?.take(30)}")
                    job.copy(distance = distance)
                } else {
                    android.util.Log.w("HomeViewModel", "⚠️ New job ${job.bookingId}: no distance calculated, address=${job.customerAddress?.take(30)}")
                    job
                }
            }
            
            // Process ongoing jobs: calculate distance synchronously (for testing: min 1 km if 0)
            val ongoingJobsWithDistance = ongoingJobs.map { job ->
                val distance = calculateJobDistanceSync(job)
                if (distance != null) {
                    android.util.Log.d("HomeViewModel", "✅ Ongoing job ${job.bookingId}: distance=$distance km, address=${job.customerAddress?.take(30)}")
                    job.copy(distance = distance)
                } else {
                    android.util.Log.w("HomeViewModel", "⚠️ Ongoing job ${job.bookingId}: no distance calculated, address=${job.customerAddress?.take(30)}")
                    job
                }
            }

            // Select ONE highlighted job (nearest or earliest)
            val highlighted = selectHighlightedJob(newJobsWithDistance, emptyMap())
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
                ongoingJobs = ongoingJobsWithDistance,
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
                
                // Enrich jobs with addresses asynchronously if missing
                viewModelScope.launch {
                    val jobsToEnrich = mutableListOf<Pair<String, BookingJob>>() // (type, job) where type is "highlighted" or "ongoing"
                    
                    newState.highlightedJob?.let { job ->
                        if (job.customerAddress.isNullOrBlank()) {
                            jobsToEnrich.add("highlighted" to job)
                        }
                    }
                    
                    newState.ongoingJobs.forEach { job ->
                        if (job.customerAddress.isNullOrBlank()) {
                            jobsToEnrich.add("ongoing" to job)
                        }
                    }
                    
                    if (jobsToEnrich.isNotEmpty()) {
                        android.util.Log.d("HomeViewModel", "🔄 Enriching ${jobsToEnrich.size} jobs with addresses from serveit_users")
                        
                        val enrichedJobs = jobsToEnrich.map { (type, job) ->
                            type to enrichJobWithAddress(job)
                        }
                        
                        var updatedHighlighted = newState.highlightedJob
                        val updatedOngoing = newState.ongoingJobs.toMutableList()
                        
                        enrichedJobs.forEach { (type, enrichedJob) ->
                            if (type == "highlighted" && updatedHighlighted?.bookingId == enrichedJob.bookingId) {
                                updatedHighlighted = enrichedJob
                                android.util.Log.d("HomeViewModel", "✅ Enriched highlighted job ${enrichedJob.bookingId} with address: ${enrichedJob.customerAddress?.take(50)}")
                            } else if (type == "ongoing") {
                                val index = updatedOngoing.indexOfFirst { it.bookingId == enrichedJob.bookingId }
                                if (index >= 0) {
                                    updatedOngoing[index] = enrichedJob
                                    android.util.Log.d("HomeViewModel", "✅ Enriched ongoing job ${enrichedJob.bookingId} with address: ${enrichedJob.customerAddress?.take(50)}")
                                }
                            }
                        }
                        
                        // Update state with enriched jobs
                        _uiState.value = _uiState.value.copy(
                            highlightedJob = updatedHighlighted,
                            ongoingJobs = updatedOngoing
                        )
                        android.util.Log.d("HomeViewModel", "✅ Updated state with enriched addresses")
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Select ONE highlighted job from available new jobs
     * Priority: Nearest job OR earliest created
     */
    private fun selectHighlightedJob(jobs: List<BookingJob>, distances: Map<String, Double>): BookingJob? {
        if (jobs.isEmpty()) return null

        // If jobs have distance info, pick nearest
        val jobsWithDistance = jobs.filter { it.distance != null && it.distance!! > 0 }
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

                    // Enrich completed jobs with addresses (like new/ongoing jobs)
                    android.util.Log.d("HomeViewModel", "🏠 Enriching addresses for ${todayJobs.size} completed jobs")
                    val todayJobsWithAddresses = todayJobs.map { job ->
                        enrichJobWithAddress(job)
                    }

                    _uiState.value = _uiState.value.copy(
                        todayCompletedJobs = todayJobsWithAddresses,
                        todayStats = Pair(
                            todayJobsWithAddresses.size,
                            todayJobsWithAddresses.sumOf { it.totalPrice }
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
                        val firestoreRepository = com.nextserve.serveitpartnernew.data.repository.FirestoreRepository(
                            com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider.firestore
                        )
                        return HomeViewModel(providerId, jobsRepository, firestoreRepository, networkMonitor) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

