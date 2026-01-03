package com.nextserve.serveitpartnernew.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import com.nextserve.serveitpartnernew.utils.ErrorMapper
import com.nextserve.serveitpartnernew.utils.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.YearMonth

/**
 * UI state for admin settlement screen
 */
data class AdminSettlementUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val settlements: List<MonthlySettlement> = emptyList(),
    val error: UiError? = null,
    val isOffline: Boolean = false
)

/**
 * ViewModel for admin settlement management
 */
class AdminSettlementViewModel(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(FirebaseProvider.firestore),
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminSettlementUiState())
    val uiState: StateFlow<AdminSettlementUiState> = _uiState

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
        loadSettlements()
    }

    /**
     * Load all settlements for admin management
     */
    fun loadSettlements() {
        if (_uiState.value.isOffline) {
            _uiState.value = _uiState.value.copy(
                error = UiError.networkError(),
                isLoading = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val settlements = loadAllSettlements()
                _uiState.value = _uiState.value.copy(
                    settlements = settlements,
                    isLoading = false
                )
            } catch (e: Exception) {
                val error = when (e) {
                    is FirebaseFirestoreException -> {
                        when (e.code) {
                            com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                                UiError(message = "Access denied. Admin permissions required.", canRetry = false)
                            com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE ->
                                UiError.networkError()
                            else -> UiError(message = "Failed to load settlements. Please try again.")
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
     * Refresh settlements data
     */
    fun refreshSettlements() {
        if (_uiState.value.isOffline) return

        _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

        viewModelScope.launch {
            try {
                val settlements = loadAllSettlements()
                _uiState.value = _uiState.value.copy(
                    settlements = settlements,
                    isRefreshing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = UiError(message = "Failed to refresh settlements. Please try again.")
                )
            }
        }
    }

    /**
     * Update settlement status (admin only)
     */
    fun updateSettlementStatus(settlementId: String, newStatus: SettlementStatus) {
        if (_uiState.value.isOffline) {
            _uiState.value = _uiState.value.copy(
                error = UiError.networkError()
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Update settlement status
                FirebaseProvider.firestore
                    .collection("monthlySettlements")
                    .document(settlementId)
                    .update(mapOf(
                        "settlementStatus" to newStatus.name,
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    ))
                    .await()

                // Refresh settlements to reflect the change
                val settlements = loadAllSettlements()
                _uiState.value = _uiState.value.copy(
                    settlements = settlements,
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiError(message = "Failed to update settlement status. Please try again.")
                )
            }
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Load all settlements from Firestore (admin view)
     */
    private suspend fun loadAllSettlements(): List<MonthlySettlement> {
        return try {
            val snapshot = FirebaseProvider.firestore
                .collection("monthlySettlements")
                .orderBy("yearMonth", Query.Direction.DESCENDING)
                .limit(500) // Admin view - load more settlements
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null

                    // Parse YearMonth from string (e.g., "2024-01")
                    val yearMonthStr = data["yearMonth"] as? String ?: return@mapNotNull null
                    val yearMonth = YearMonth.parse(yearMonthStr)

                    MonthlySettlement(
                        settlementId = doc.id,
                        partnerId = data["partnerId"] as? String ?: "",
                        yearMonth = yearMonth,
                        totalEarnings = (data["totalEarnings"] as? Number)?.toDouble() ?: 0.0,
                        platformFees = (data["platformFees"] as? Number)?.toDouble() ?: 0.0,
                        partnerShare = (data["partnerShare"] as? Number)?.toDouble() ?: 0.0,
                        completedJobs = (data["completedJobs"] as? Number)?.toInt() ?: 0,
                        paidAmount = (data["paidAmount"] as? Number)?.toDouble() ?: 0.0,
                        pendingAmount = (data["pendingAmount"] as? Number)?.toDouble() ?: 0.0,
                        settlementStatus = SettlementStatus.valueOf(
                            (data["settlementStatus"] as? String) ?: "PENDING"
                        ),
                        createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
                            ?.toInstant()?.atZone(ZoneOffset.systemDefault())?.toLocalDateTime()
                            ?: LocalDateTime.now(),
                        updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()
                            ?.toInstant()?.atZone(ZoneOffset.systemDefault())?.toLocalDateTime()
                            ?: LocalDateTime.now()
                    )
                } catch (e: Exception) {
                    // Skip malformed settlements
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
