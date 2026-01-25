package com.nextserve.serveitpartnernew.ui.screen.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.ui.viewmodel.AdminSettlementUiState
import com.nextserve.serveitpartnernew.ui.viewmodel.AdminSettlementViewModel
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.ui.components.ErrorDisplay
import com.nextserve.serveitpartnernew.ui.components.OfflineIndicator
import com.nextserve.serveitpartnernew.ui.viewmodel.MonthlySettlement
import com.nextserve.serveitpartnernew.ui.viewmodel.SettlementStatus
import com.nextserve.serveitpartnernew.utils.CurrencyUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Admin settlement management screen
 * Shows all settlements across all partners for admin management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettlementScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminSettlementViewModel = viewModel {
        AdminSettlementViewModel()
    }
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val pullToRefreshState = rememberPullToRefreshState()

    // Filter state
    var selectedStatus by remember { mutableStateOf<SettlementStatus?>(null) }
    var selectedMonth by remember { mutableStateOf<YearMonth?>(null) }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settlement Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshSettlements() },
                        enabled = !uiState.isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshSettlements() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Offline indicator
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.isOffline,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    OfflineIndicator(modifier = Modifier.fillMaxWidth())
                }

                // Filters
                SettlementFilters(
                    selectedStatus = selectedStatus,
                    selectedMonth = selectedMonth,
                    onStatusChange = { selectedStatus = it },
                    onMonthChange = { selectedMonth = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // Error display
                uiState.error?.let { error ->
                    ErrorDisplay(
                        error = error,
                        onRetry = { viewModel.refreshSettlements() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Content
                if (uiState.error == null) {
                    SettlementContent(
                        uiState = uiState,
                        selectedStatus = selectedStatus,
                        selectedMonth = selectedMonth,
                        onUpdateSettlement = { settlementId, newStatus ->
                            viewModel.updateSettlementStatus(settlementId, newStatus)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Filter controls for settlements
 */
@Composable
private fun SettlementFilters(
    selectedStatus: SettlementStatus?,
    selectedMonth: YearMonth?,
    onStatusChange: (SettlementStatus?) -> Unit,
    onMonthChange: (YearMonth?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Status filters
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { onStatusChange(null) },
                        label = { Text("All") }
                    )
                    SettlementStatus.values().forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { onStatusChange(status) },
                            label = { Text(status.name.lowercase().capitalize()) }
                        )
                    }
                }
            }

            // Month filters
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Month",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val currentMonth = YearMonth.now()
                    val months = listOf(
                        currentMonth.minusMonths(2),
                        currentMonth.minusMonths(1),
                        currentMonth
                    )

                    FilterChip(
                        selected = selectedMonth == null,
                        onClick = { onMonthChange(null) },
                        label = { Text("All") }
                    )

                    months.forEach { month ->
                        FilterChip(
                            selected = selectedMonth == month,
                            onClick = { onMonthChange(month) },
                            label = {
                                Text(month.format(DateTimeFormatter.ofPattern("MMM yyyy")))
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Main content showing settlements list
 */
@Composable
private fun SettlementContent(
    uiState: AdminSettlementUiState,
    selectedStatus: SettlementStatus?,
    selectedMonth: YearMonth?,
    onUpdateSettlement: (String, SettlementStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredSettlements = remember(uiState.settlements, selectedStatus, selectedMonth) {
        uiState.settlements.filter { settlement ->
            (selectedStatus == null || settlement.settlementStatus == selectedStatus) &&
            (selectedMonth == null || settlement.yearMonth == selectedMonth)
        }
    }

    if (uiState.isLoading && uiState.settlements.isEmpty()) {
        SettlementSkeleton()
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary stats
            item {
                SettlementSummaryCard(
                    settlements = uiState.settlements,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Settlements list
            if (filteredSettlements.isNotEmpty()) {
                items(
                    items = filteredSettlements.sortedByDescending { it.yearMonth },
                    key = { it.settlementId }
                ) { settlement ->
                    AdminSettlementCard(
                        settlement = settlement,
                        onUpdateStatus = { newStatus ->
                            onUpdateSettlement(settlement.settlementId, newStatus)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                item {
                    SettlementEmptyState(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/**
 * Summary statistics card
 */
@Composable
private fun SettlementSummaryCard(
    settlements: List<MonthlySettlement>,
    modifier: Modifier = Modifier
) {
    val totalEarnings = settlements.fold(0.0) { acc, it -> acc + it.totalEarnings }
    val totalPaid = settlements.fold(0.0) { acc, it -> acc + it.paidAmount }
    val totalPending = settlements.fold(0.0) { acc, it -> acc + it.pendingAmount }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Settlement Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryItem(
                    label = "Total Earnings",
                    value = CurrencyUtils.formatCurrency(totalEarnings),
                    modifier = Modifier.weight(1f)
                )
                SummaryItem(
                    label = "Total Paid",
                    value = CurrencyUtils.formatCurrency(totalPaid),
                    modifier = Modifier.weight(1f)
                )
                SummaryItem(
                    label = "Pending",
                    value = CurrencyUtils.formatCurrency(totalPending),
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "${settlements.size} settlements • ${settlements.count { it.settlementStatus == SettlementStatus.READY }} ready for payout",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Individual settlement card for admin management
 */
@Composable
private fun AdminSettlementCard(
    settlement: MonthlySettlement,
    onUpdateStatus: (SettlementStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with period and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = settlement.displayPeriod,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                SettlementStatusChip(status = settlement.settlementStatus)
            }

            // Partner info
            Text(
                text = "Partner ID: ${settlement.partnerId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(
                    label = "Jobs",
                    value = settlement.completedJobs.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Earned",
                    value = CurrencyUtils.formatCurrency(settlement.partnerShare),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Paid",
                    value = CurrencyUtils.formatCurrency(settlement.paidAmount),
                    modifier = Modifier.weight(1f)
                )
            }

            // Status update actions
            if (settlement.settlementStatus == SettlementStatus.PENDING) {
                OutlinedButton(
                    onClick = { onUpdateStatus(SettlementStatus.READY) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mark as Ready")
                }
            }
        }
    }
}

/**
 * Settlement status chip
 */
@Composable
private fun SettlementStatusChip(status: SettlementStatus) {
    val (text, color) = when (status) {
        SettlementStatus.PENDING -> "Calculating" to androidx.compose.ui.graphics.Color(0xFFFF9800)
        SettlementStatus.READY -> "Ready" to androidx.compose.ui.graphics.Color(0xFF4CAF50)
        SettlementStatus.REQUESTED -> "Requested" to androidx.compose.ui.graphics.Color(0xFF2196F3)
        SettlementStatus.PROCESSING -> "Processing" to androidx.compose.ui.graphics.Color(0xFFFF9800)
        SettlementStatus.SETTLED -> "Settled" to androidx.compose.ui.graphics.Color(0xFF4CAF50)
        SettlementStatus.FAILED -> "Failed" to androidx.compose.ui.graphics.Color(0xFFF44336)
    }

    AssistChip(
        onClick = { /* No action */ },
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color
        )
    )
}

/**
 * Summary item component
 */
@Composable
private fun SummaryItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

/**
 * Statistic item for settlement cards
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Empty state when no settlements match filters
 */
@Composable
private fun SettlementEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📊",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Settlements Found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Try adjusting your filters or check back later for new settlements",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * Skeleton loading state
 */
@Composable
private fun SettlementSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary skeleton
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(120.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading settlement summary...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Settlement cards skeleton
        repeat(3) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(140.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading settlement...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
