package com.nextserve.serveitpartnernew.ui.screen.earnings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.nextserve.serveitpartnernew.ui.viewmodel.EarningsRange
import com.nextserve.serveitpartnernew.ui.viewmodel.EarningsUiState
import com.nextserve.serveitpartnernew.ui.viewmodel.EarningsViewModel
import com.nextserve.serveitpartnernew.utils.formatCurrency

/**
 * Main Earnings screen with tabs, summary card, and earnings list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(
    modifier: Modifier = Modifier,
    viewModel: EarningsViewModel = viewModel {
        EarningsViewModel(
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
                        text = "Earnings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshEarnings() },
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
            onRefresh = { viewModel.refreshEarnings() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Offline indicator
                AnimatedVisibility(
                    visible = uiState.isOffline,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    OfflineIndicator(modifier = Modifier.fillMaxWidth())
                }

                // Error display
                uiState.error?.let { error ->
                    ErrorDisplay(
                        error = error,
                        onRetry = {
                            viewModel.clearError()
                            viewModel.loadEarnings()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Content
                if (uiState.error == null) {
                    EarningsContent(
                        uiState = uiState,
                        onRangeSelected = { viewModel.selectRange(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Main content with tabs and earnings data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EarningsContent(
    uiState: EarningsUiState,
    onRangeSelected: (EarningsRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Tabs
        TabRow(
            selectedTabIndex = uiState.selectedRange.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            EarningsRange.entries.forEach { range ->
                Tab(
                    selected = uiState.selectedRange == range,
                    onClick = { onRangeSelected(range) },
                    text = {
                        Text(
                            text = when (range) {
                                EarningsRange.TODAY -> "Today"
                                EarningsRange.WEEK -> "Week"
                                EarningsRange.MONTH -> "Month"
                            }
                        )
                    }
                )
            }
        }

        // Content based on loading state
        if (uiState.isLoading && uiState.items.isEmpty()) {
            EarningsSkeleton()
        } else {
            // Summary Card
            EarningsSummaryCard(
                uiState = uiState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Earnings List
            EarningsList(
                items = uiState.items,
                isEmpty = uiState.items.isEmpty() && !uiState.isLoading,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
