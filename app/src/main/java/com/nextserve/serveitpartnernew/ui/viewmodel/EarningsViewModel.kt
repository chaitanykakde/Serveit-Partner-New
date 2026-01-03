package com.nextserve.serveitpartnernew.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.google.firebase.functions.FirebaseFunctions
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import com.nextserve.serveitpartnernew.data.repository.JobsRepository
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.utils.ErrorMapper
import com.nextserve.serveitpartnernew.utils.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * ViewModel for the Earnings screen.
 * Handles fetching, filtering, and calculating earnings data.
 */
class EarningsViewModel(
    private val uid: String,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(FirebaseProvider.firestore),
    private val jobsRepository: JobsRepository = JobsRepository(FirebaseProvider.firestore, FirebaseFunctions.getInstance()),
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(EarningsUiState())
    val uiState: StateFlow<EarningsUiState> = _uiState

    // Cache for raw booking data
    private var cachedBookings: List<EarningItem> = emptyList()

    init {
        // Monitor network connectivity
        networkMonitor?.let { monitor ->
            viewModelScope.launch {
                monitor.connectivityFlow().collectLatest { isConnected ->
                    _uiState.value = _uiState.value.copy(isOffline = !isConnected)
                }
            }
        }

        // Load initial data
        loadEarnings()
    }

    /**
     * Load earnings data from Firestore.
     */
    fun loadEarnings() {
        if (_uiState.value.isOffline) {
            // Show cached data if available
            if (cachedBookings.isNotEmpty()) {
                filterAndCalculateEarnings(_uiState.value.selectedRange)
            } else {
                _uiState.value = _uiState.value.copy(
                    error = UiError.networkError(),
                    isLoading = false
                )
            }
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val bookings = fetchCompletedBookings()
                cachedBookings = bookings
                filterAndCalculateEarnings(_uiState.value.selectedRange)
            } catch (e: Exception) {
                val error = when (e) {
                    is FirebaseFirestoreException -> {
                        when (e.code) {
                            com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                                UiError.firestoreIndexSetup()
                            com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                                UiError(message = "Access denied. Please contact support.", canRetry = false)
                            com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE ->
                                UiError.networkError()
                            else -> UiError(message = "Failed to load earnings data. Please try again.")
                        }
                    }
                    else -> UiError(message = "An unexpected error occurred. Please try again.")
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error
                )
            }
        }
    }

    /**
     * Refresh earnings data.
     */
    fun refreshEarnings() {
        if (_uiState.value.isOffline) return

        _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

        viewModelScope.launch {
            try {
                val bookings = fetchCompletedBookings()
                cachedBookings = bookings
                filterAndCalculateEarnings(_uiState.value.selectedRange)
            } catch (e: Exception) {
                // On refresh error, keep existing data but show appropriate error
                val error = when (e) {
                    is FirebaseFirestoreException -> {
                        when (e.code) {
                            com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                                UiError.firestoreIndexSetup()
                            else -> UiError(message = "Failed to refresh earnings. Please try again.")
                        }
                    }
                    else -> UiError(message = "Failed to refresh earnings. Please try again.")
                }

                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = error
                )
            }
        }
    }

    /**
     * Change the selected earnings range.
     */
    fun selectRange(range: EarningsRange) {
        _uiState.value = _uiState.value.copy(selectedRange = range)
        filterAndCalculateEarnings(range)
    }

    /**
     * Clear any error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Fetch completed bookings from Firestore using the index-safe query.
     */
    private suspend fun fetchCompletedBookings(): List<EarningItem> {
        // Use JobsRepository to get completed jobs from the correct collection structure
        val result = jobsRepository.getCompletedJobs(uid, limit = 100)

        return result.fold(
            onSuccess = { (jobs, _) ->
                jobs.map { job ->
                    // Convert Job to EarningItem
                    val amount = job.totalPrice
                    val platformFee = amount * 0.1 // 10% platform fee
                    val partnerEarning = amount - platformFee

                    EarningItem(
                        bookingId = job.bookingId,
                        serviceName = job.serviceName,
                        completedAt = job.completedAt?.toDate()
                            ?.toInstant()?.atZone(ZoneOffset.systemDefault())?.toLocalDateTime()
                            ?: LocalDateTime.now(),
                        amount = amount,
                        platformFee = platformFee,
                        partnerEarning = partnerEarning,
                        paymentStatus = PaymentStatus.PENDING, // Default to pending
                        customerName = job.userName
                    )
                }
            },
            onFailure = {
                // Return empty list on failure
                emptyList()
            }
        )
    }

    /**
     * Filter bookings by date range and calculate totals.
     */
    private fun filterAndCalculateEarnings(range: EarningsRange) {
        val now = LocalDateTime.now()
        val startDate = when (range) {
            EarningsRange.TODAY -> now.truncatedTo(ChronoUnit.DAYS)
            EarningsRange.WEEK -> now.minusDays(7)
            EarningsRange.MONTH -> now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS)
        }

        val filteredItems = cachedBookings.filter { item ->
            item.completedAt.isAfter(startDate) || item.completedAt.isEqual(startDate)
        }

        val totalEarnings = filteredItems.sumOf { it.partnerEarning }
        val completedJobs = filteredItems.size
        val paidAmount = filteredItems
            .filter { it.paymentStatus == PaymentStatus.PAID }
            .sumOf { it.partnerEarning }
        val pendingAmount = totalEarnings - paidAmount

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isRefreshing = false,
            totalEarnings = totalEarnings,
            completedJobs = completedJobs,
            paidAmount = paidAmount,
            pendingAmount = pendingAmount,
            items = filteredItems.sortedByDescending { it.completedAt },
            error = null
        )
    }
}
