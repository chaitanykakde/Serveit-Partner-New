package com.nextserve.serveitpartnernew.ui.screen.payout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.ui.components.ErrorDisplay
import com.nextserve.serveitpartnernew.ui.components.OfflineIndicator
import com.nextserve.serveitpartnernew.ui.viewmodel.PayoutUiState
import com.nextserve.serveitpartnernew.ui.viewmodel.PayoutViewModel
import com.nextserve.serveitpartnernew.utils.CurrencyUtils

/**
 * Main Payout screen showing bank account, settlements, and payout history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayoutScreen(
    modifier: Modifier = Modifier,
    viewModel: PayoutViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        PayoutViewModel(
            uid = FirebaseProvider.auth.currentUser?.uid ?: "",
            networkMonitor = null // TODO: Inject network monitor
        )
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Payouts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshPayoutData() },
                        enabled = !uiState.isOffline && !uiState.isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshPayoutData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Offline indicator
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.isOffline,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    OfflineIndicator(modifier = Modifier.fillMaxWidth())
                }

                // Error display
                uiState.error?.let { error ->
                    ErrorDisplay(
                        error = error,
                        onRetry = {
                            viewModel.clearError()
                            viewModel.loadPayoutData()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Content
                if (uiState.error == null) {
                    PayoutContent(
                        uiState = uiState,
                        onRequestPayout = { settlementId, amount ->
                            viewModel.requestPayout(settlementId, amount)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Main content with bank account and settlements.
 */
@Composable
private fun PayoutContent(
    uiState: PayoutUiState,
    onRequestPayout: (String, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading && uiState.settlements.isEmpty()) {
        PayoutSkeleton()
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bank Account Section
            item {
                BankAccountSection(
                    bankAccount = uiState.bankAccount,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Available Balance Summary
            item {
                AvailableBalanceCard(
                    settlements = uiState.settlements,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Monthly Settlements
            if (uiState.settlements.isNotEmpty()) {
                item {
                    Text(
                        text = "Monthly Settlements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(
                    items = uiState.settlements,
                    key = { it.settlementId }
                ) { settlement ->
                    SettlementCard(
                        settlement = settlement,
                        onRequestPayout = { amount ->
                            onRequestPayout(settlement.settlementId, amount)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Payout History
            if (uiState.payoutRequests.isNotEmpty()) {
                item {
                    Text(
                        text = "Payout History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(
                    items = uiState.payoutRequests,
                    key = { it.requestId }
                ) { request ->
                    PayoutRequestCard(
                        request = request,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Empty state
            if (uiState.settlements.isEmpty() && uiState.payoutRequests.isEmpty() && !uiState.isLoading) {
                item {
                    PayoutEmptyState(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
