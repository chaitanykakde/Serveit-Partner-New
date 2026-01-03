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
 * ViewModel for the Payout screen.
 * Manages bank accounts, settlements, and payout requests.
 */
class PayoutViewModel(
    private val uid: String,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(FirebaseProvider.firestore),
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PayoutUiState())
    val uiState: StateFlow<PayoutUiState> = _uiState

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
        loadPayoutData()
    }

    /**
     * Load all payout-related data: bank account, settlements, and payout requests.
     */
    fun loadPayoutData() {
        if (_uiState.value.isOffline) {
            // Show offline state
            _uiState.value = _uiState.value.copy(
                error = UiError.networkError(),
                isLoading = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Load data in parallel for better performance
                val bankAccountDeferred = loadBankAccount()
                val settlementsDeferred = loadSettlements()
                val payoutRequestsDeferred = loadPayoutRequests()

                // Wait for all to complete
                val bankAccount = bankAccountDeferred
                val settlements = settlementsDeferred
                val payoutRequests = payoutRequestsDeferred

                _uiState.value = _uiState.value.copy(
                    bankAccount = bankAccount,
                    settlements = settlements.sortedByDescending { it.yearMonth },
                    payoutRequests = payoutRequests.sortedByDescending { it.requestedAt },
                    isLoading = false
                )
            } catch (e: Exception) {
                val error = when (e) {
                    is FirebaseFirestoreException -> {
                        when (e.code) {
                            com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                                UiError(message = "Access denied. Please contact support.", canRetry = false)
                            com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE ->
                                UiError.networkError()
                            else -> UiError(message = "Failed to load payout data. Please try again.")
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
     * Refresh payout data.
     */
    fun refreshPayoutData() {
        if (_uiState.value.isOffline) return

        _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

        viewModelScope.launch {
            try {
                val bankAccount = loadBankAccount()
                val settlements = loadSettlements()
                val payoutRequests = loadPayoutRequests()

                _uiState.value = _uiState.value.copy(
                    bankAccount = bankAccount,
                    settlements = settlements.sortedByDescending { it.yearMonth },
                    payoutRequests = payoutRequests.sortedByDescending { it.requestedAt },
                    isRefreshing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = UiError(message = "Failed to refresh payout data. Please try again.")
                )
            }
        }
    }

    /**
     * Update or create bank account details.
     */
    fun updateBankAccount(
        accountHolderName: String,
        accountNumber: String,
        ifscCode: String,
        bankName: String
    ) {
        if (_uiState.value.isOffline) {
            _uiState.value = _uiState.value.copy(
                error = UiError.networkError()
            )
            return
        }

        val bankAccount = BankAccount(
            partnerId = uid,
            accountHolderName = accountHolderName.trim(),
            accountNumber = accountNumber.trim(),
            ifscCode = ifscCode.trim().uppercase(),
            bankName = bankName.trim()
        )

        if (!bankAccount.isComplete) {
            _uiState.value = _uiState.value.copy(
                error = UiError(message = "Please fill in all bank account details.", canRetry = false)
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Save to Firestore
                val accountData = mapOf(
                    "partnerId" to bankAccount.partnerId,
                    "accountHolderName" to bankAccount.accountHolderName,
                    "accountNumber" to bankAccount.accountNumber,
                    "ifscCode" to bankAccount.ifscCode,
                    "bankName" to bankAccount.bankName,
                    "isVerified" to false,
                    "createdAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                )

                val existingAccount = _uiState.value.bankAccount
                if (existingAccount != null) {
                    // Update existing
                    FirebaseProvider.firestore
                        .collection("bankAccounts")
                        .document(existingAccount.accountId)
                        .update(accountData)
                        .await()
                } else {
                    // Create new
                    val docRef = FirebaseProvider.firestore
                        .collection("bankAccounts")
                        .add(accountData)
                        .await()

                    // Update with the generated ID
                    FirebaseProvider.firestore
                        .collection("bankAccounts")
                        .document(docRef.id)
                        .update("accountId", docRef.id)
                        .await()
                }

                // Reload data to reflect changes
                loadPayoutData()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiError(message = "Failed to save bank account details. Please try again.")
                )
            }
        }
    }

    /**
     * Request payout for a specific settlement.
     */
    fun requestPayout(settlementId: String, amount: Double) {
        val bankAccount = _uiState.value.bankAccount
        if (bankAccount == null || !bankAccount.isVerified) {
            _uiState.value = _uiState.value.copy(
                error = UiError(
                    message = "Please add and verify your bank account details before requesting payout.",
                    canRetry = false
                )
            )
            return
        }

        if (amount <= 0) {
            _uiState.value = _uiState.value.copy(
                error = UiError(message = "Invalid payout amount.", canRetry = false)
            )
            return
        }

        if (_uiState.value.isOffline) {
            _uiState.value = _uiState.value.copy(
                error = UiError.networkError()
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val payoutRequestData = mapOf(
                    "partnerId" to uid,
                    "settlementId" to settlementId,
                    "requestedAmount" to amount,
                    "bankAccountId" to bankAccount.accountId,
                    "requestStatus" to "PENDING",
                    "requestedAt" to Timestamp.now()
                )

                FirebaseProvider.firestore
                    .collection("payoutRequests")
                    .add(payoutRequestData)
                    .await()

                // Reload data to reflect the new request
                loadPayoutData()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiError(message = "Failed to submit payout request. Please try again.")
                )
            }
        }
    }

    /**
     * Select a settlement for payout operations.
     */
    fun selectSettlement(settlement: MonthlySettlement?) {
        _uiState.value = _uiState.value.copy(selectedSettlement = settlement)
    }

    /**
     * Clear any error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Load bank account details for the partner.
     */
    private suspend fun loadBankAccount(): BankAccount? {
        return try {
            val snapshot = FirebaseProvider.firestore
                .collection("bankAccounts")
                .whereEqualTo("partnerId", uid)
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val doc = snapshot.documents.first()
                val data = doc.data ?: return null

                BankAccount(
                    accountId = doc.id,
                    partnerId = data["partnerId"] as? String ?: "",
                    accountHolderName = data["accountHolderName"] as? String ?: "",
                    accountNumber = data["accountNumber"] as? String ?: "",
                    ifscCode = data["ifscCode"] as? String ?: "",
                    bankName = data["bankName"] as? String ?: "",
                    isVerified = data["isVerified"] as? Boolean ?: false,
                    createdAt = (data["createdAt"] as? Timestamp)?.toDate()
                        ?.toInstant()?.atZone(ZoneOffset.systemDefault())?.toLocalDateTime()
                        ?: LocalDateTime.now(),
                    updatedAt = (data["updatedAt"] as? Timestamp)?.toDate()
                        ?.toInstant()?.atZone(ZoneOffset.systemDefault())?.toLocalDateTime()
                        ?: LocalDateTime.now()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null // Return null if bank account loading fails
        }
    }

    /**
     * Load monthly settlements for the partner.
     */
    private suspend fun loadSettlements(): List<MonthlySettlement> {
        return try {
            val snapshot = FirebaseProvider.firestore
                .collection("monthlySettlements")
                .whereEqualTo("partnerId", uid)
                .orderBy("yearMonth", Query.Direction.DESCENDING)
                .limit(12) // Last 12 months
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
                        )
                    )
                } catch (e: Exception) {
                    null // Skip malformed settlements
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Load payout requests for the partner.
     */
    private suspend fun loadPayoutRequests(): List<PayoutRequest> {
        return try {
            val snapshot = FirebaseProvider.firestore
                .collection("payoutRequests")
                .whereEqualTo("partnerId", uid)
                .orderBy("requestedAt", Query.Direction.DESCENDING)
                .limit(20) // Last 20 requests
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null

                    PayoutRequest(
                        requestId = doc.id,
                        partnerId = data["partnerId"] as? String ?: "",
                        settlementId = data["settlementId"] as? String ?: "",
                        requestedAmount = (data["requestedAmount"] as? Number)?.toDouble() ?: 0.0,
                        bankAccountId = data["bankAccountId"] as? String ?: "",
                        requestStatus = PayoutStatus.valueOf(
                            (data["requestStatus"] as? String) ?: "PENDING"
                        ),
                        requestedAt = (data["requestedAt"] as? Timestamp)?.toDate()
                            ?.toInstant()?.atZone(ZoneOffset.systemDefault())?.toLocalDateTime()
                            ?: LocalDateTime.now(),
                        processedAt = (data["processedAt"] as? Timestamp)?.toDate()
                            ?.toInstant()?.atZone(ZoneOffset.systemDefault())?.toLocalDateTime(),
                        failureReason = data["failureReason"] as? String,
                        transactionId = data["transactionId"] as? String
                    )
                } catch (e: Exception) {
                    null // Skip malformed requests
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
