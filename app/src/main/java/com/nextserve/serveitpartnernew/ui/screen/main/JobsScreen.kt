package com.nextserve.serveitpartnernew.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.data.repository.JobsRepository
import com.nextserve.serveitpartnernew.ui.components.EmptyState
import com.nextserve.serveitpartnernew.ui.components.ErrorState
import com.nextserve.serveitpartnernew.ui.components.*
import com.nextserve.serveitpartnernew.ui.viewmodel.JobsViewModel
import com.nextserve.serveitpartnernew.utils.NetworkMonitor
import kotlinx.coroutines.launch

/**
 * JOBS SCREEN SCROLL CONTRACT
 *
 * - One LazyColumn per tab
 * - No nested vertical scrolling
 * - Loading/Error/Empty are LazyColumn items
 * - Scaffold padding must be respected
 * - Parent padding (from MainAppScreen) must be combined with local Scaffold padding
 */

/**
 * Jobs Screen with tabs for New Jobs and History
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(
    modifier: Modifier = Modifier,
    providerId: String,
    parentPaddingValues: PaddingValues = PaddingValues(),
    onNavigateToJobDetails: (String, String, Int?) -> Unit = { _, _, _ -> }, // bookingId, customerPhoneNumber, bookingIndex
    onJobAccepted: (String) -> Unit = {}, // Navigate to job details
    viewModel: JobsViewModel = viewModel(
        factory = JobsViewModel.factory(
            providerId = providerId,
            context = LocalContext.current
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 }, initialPage = 0)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAcceptDialog by remember { mutableStateOf<Job?>(null) }

    // Sync pager with tab selection
    LaunchedEffect(selectedTabIndex) {
        pagerState.animateScrollToPage(selectedTabIndex)
    }

    // Sync tab with pager
    LaunchedEffect(pagerState.currentPage) {
        selectedTabIndex = pagerState.currentPage
    }

    // Load history when tab 2 is selected
    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 1 && uiState.completedJobs.isEmpty()) {
            viewModel.loadHistory()
        }
    }

    // Show error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Jobs",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    actions = {
                        IconButton(onClick = { /* Settings */ }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Tab row matching HTML design
                JobsTabRow(
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { index ->
                        selectedTabIndex = index
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        floatingActionButton = {
            // Enhanced floating action button for filter management
            if (uiState.selectedServiceFilter != null ||
                uiState.maxDistanceFilter != null ||
                uiState.minPriceFilter != null ||
                uiState.maxPriceFilter != null ||
                uiState.searchQuery.isNotEmpty()) {

                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = { viewModel.clearFilters() },
                    icon = {
                    Text(
                        text = "❌",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    },
                    text = { Text("Clear Filters") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        modifier = modifier
    ) { scaffoldPaddingValues ->
        // Combine parent bottom padding (from MainAppScreen's bottomBar) with local Scaffold padding
        val parentBottomPadding = parentPaddingValues.calculateBottomPadding()
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPaddingValues),
            userScrollEnabled = true
        ) { page ->
            when (page) {
                0 -> NewJobsTab(
                        jobs = uiState.filteredJobs.ifEmpty { uiState.newJobs },
                        searchQuery = uiState.searchQuery,
                    isLoading = uiState.isLoadingNewJobs,
                    hasOngoingJob = uiState.hasOngoingJob,
                    acceptingJobId = uiState.acceptingJobId,
                    errorMessage = uiState.errorMessage?.takeIf { !it.contains("load more") },
                    onRetry = { viewModel.loadNewJobs() },
                    onJobClick = { job ->
                        // For completed jobs, bookingIndex is not available, use null
                        onNavigateToJobDetails(job.bookingId, job.customerPhoneNumber, null)
                    },
                    onAcceptClick = { job ->
                        if (uiState.hasOngoingJob) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Complete ongoing job to accept new requests")
                            }
                        } else {
                            showAcceptDialog = job
                        }
                    },
                    onRejectClick = { job ->
                        viewModel.rejectJob(job)
                    },
                    uiState = uiState,
                    viewModel = viewModel,
                    parentBottomPadding = parentBottomPadding,
                    modifier = Modifier.fillMaxSize()
                )
                1 -> HistoryTab(
                    jobs = uiState.completedJobs,
                    isLoading = uiState.isLoadingHistory,
                    isLoadingMore = uiState.isLoadingMoreHistory,
                    hasMore = uiState.hasMoreHistory,
                    onLoadMore = { viewModel.loadMoreHistory() },
                    onJobClick = { job ->
                        // For completed jobs, bookingIndex is not available, use null
                        onNavigateToJobDetails(job.bookingId, job.customerPhoneNumber, null)
                    },
                    errorMessage = uiState.errorMessage?.takeIf { it.contains("load more") },
                    parentBottomPadding = parentBottomPadding,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Accept job dialog
    showAcceptDialog?.let { job ->
        AcceptJobDialog(
            job = job,
            onConfirm = {
                viewModel.acceptJob(
                    job = job,
                    onSuccess = {
                        showAcceptDialog = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Job accepted successfully!")
                        }
                        // Navigate to job details after accepting
                        // Get bookingIndex from inbox entry if available
                        val inboxEntry = uiState.inboxEntries.find { it.bookingId == job.bookingId }
                        val bookingIndex = inboxEntry?.bookingIndex
                        onNavigateToJobDetails(job.bookingId, job.customerPhoneNumber, bookingIndex)
                    },
                    onError = { error ->
                        showAcceptDialog = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(error)
                        }
                    }
                )
            },
            onDismiss = { showAcceptDialog = null }
        )
    }
}

/**
 * New Jobs Tab - Shows available jobs
 * Uses LazyColumn items for all states (loading, error, empty, content) - Google-grade pattern
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewJobsTab(
    jobs: List<Job>,
    searchQuery: String = "",
    isLoading: Boolean,
    hasOngoingJob: Boolean,
    acceptingJobId: String?,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    onJobClick: (Job) -> Unit = {},
    onAcceptClick: (Job) -> Unit,
    onRejectClick: (Job) -> Unit,
    uiState: com.nextserve.serveitpartnernew.ui.viewmodel.JobsUiState,
    viewModel: JobsViewModel,
    parentBottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // Determine content state (priority: loading > error > empty > content)
    val showError = errorMessage != null && !isLoading && jobs.isEmpty()
    val showEmpty = !isLoading && jobs.isEmpty() && errorMessage == null

    PullToRefreshBox(
        isRefreshing = isLoading && jobs.isNotEmpty(), // Only show refresh indicator when we have content
        onRefresh = { onRetry() }
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = 12.dp + parentBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            // UI state priority (strict order, early returns):
            // 1. Loading - Skeleton placeholder items
            // 2. Error - Error state with retry
            // 3. Empty - Empty state placeholder
            // 4. Content - Normal job cards
            
            // 1️⃣ LOADING STATE: Render skeleton placeholder items
            if (isLoading) {
                items(3, key = { "skeleton_$it" }) {
                Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
                return@LazyColumn
            }

            // 2️⃣ ERROR STATE: Render error item (non-fullscreen)
            if (showError) {
                item(key = "error_state") {
                    ErrorState(
                        message = errorMessage ?: "An error occurred",
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                return@LazyColumn
            }

            // 3️⃣ EMPTY STATE: Render empty item (non-fullscreen)
            if (showEmpty) {
                item(key = "empty_state") {
                EmptyState(
                    icon = Icons.Default.List,
                    title = "No New Jobs",
                    description = "New job requests will appear here",
                        modifier = Modifier.fillMaxWidth()
                )
            }
                return@LazyColumn
            }

            // 4️⃣ NORMAL CONTENT: Render job cards
            // Ongoing job banner (if applicable)
                    if (hasOngoingJob) {
                item(key = "ongoing_banner") {
                    InfoBanner(
                        text = "Complete ongoing job to accept new requests",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

                            // Show active filters indicator
                            if (searchQuery.isNotEmpty() ||
                                uiState.selectedServiceFilter != null ||
                                uiState.maxDistanceFilter != null ||
                                uiState.minPriceFilter != null ||
                                uiState.maxPriceFilter != null) {
                item(key = "filter_indicator") {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Filters applied",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "Clear",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.clickable { viewModel.clearFilters() }
                                            )
                                        }
                                    }
                                }
                            }

            // Job cards
            items(
                items = jobs,
                key = { it.bookingId }
            ) { job ->
                NewJobItem(
                    job = job,
                    onJobClick = onJobClick,
                    onAcceptClick = onAcceptClick,
                    onRejectClick = onRejectClick,
                    hasOngoingJob = hasOngoingJob,
                    isAccepting = acceptingJobId == job.bookingId,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * History Tab - Shows completed jobs
 * Uses LazyColumn items for all states (loading, error, empty, content) - Google-grade pattern
 */
@Composable
private fun HistoryTab(
    jobs: List<Job>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onJobClick: (Job) -> Unit = {},
    errorMessage: String? = null,
    parentBottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Load more when scrolled near end
    LaunchedEffect(listState) {
        if (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == jobs.size - 1 && hasMore && !isLoadingMore) {
            onLoadMore()
        }
    }

    // Determine content state (priority: loading > empty > content)
    val showEmpty = !isLoading && jobs.isEmpty()

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 16.dp + parentBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp), // gap-2 in HTML
            modifier = modifier.fillMaxWidth()
        ) {
        // UI state priority (strict order, early returns):
        // 1. Loading - Loading indicator item
        // 2. Empty - Empty state placeholder
        // 3. Content - Job cards + pagination UI
        
        // 1️⃣ LOADING STATE: Render loading indicator item
        if (isLoading && jobs.isEmpty()) {
            item(key = "loading_state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            return@LazyColumn
        }

        // 2️⃣ EMPTY STATE: Render empty item (non-fullscreen)
        if (showEmpty) {
            item(key = "empty_state") {
                EmptyState(
                    icon = Icons.Default.Info,
                    title = "No Completed Jobs",
                    description = "Your completed jobs will appear here",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return@LazyColumn
        }

        // 3️⃣ NORMAL CONTENT: Render job items
                    items(
                        items = jobs,
                        key = { it.bookingId }
                    ) { job ->
                        HistoryJobItem(
                            job = job,
                            onJobClick = onJobClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

        // Pagination loading indicator
                    if (isLoadingMore) {
            item(key = "pagination_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    
                    // Show error message if pagination failed
                    if (!isLoadingMore && hasMore && errorMessage != null) {
            item(key = "pagination_error") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = onLoadMore,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Retry")
                    }
                }
            }
        }
    }
}





/**
 * Accept Job Dialog
 */
@Composable
private fun AcceptJobDialog(
    job: Job,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accept Job?") },
        text = {
            Column {
                Text("Service: ${job.serviceName}")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Customer: ${job.userName}")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Amount: ₹${job.totalPrice.toInt()}")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Accept")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Format timestamp to relative time string (e.g., "2 hours ago", "Today at 2:30 PM")
 */
private fun formatRelativeTime(timestamp: com.google.firebase.Timestamp): String {
    val date = timestamp.toDate()
    val now = Date()
    val diff = now.time - date.time
    
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "$minutes minute${if (minutes != 1L) "s" else ""} ago"
        hours < 24 -> "$hours hour${if (hours != 1L) "s" else ""} ago"
        days == 1L -> "Yesterday"
        days < 7 -> "$days days ago"
        else -> {
            val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            format.format(date)
        }
    }
}

/**
 * Format timestamp to readable date (legacy function for compatibility)
 */
private fun formatDate(timestamp: com.google.firebase.Timestamp): String {
    val date = timestamp.toDate()
    val now = Date()
    val diff = now.time - date.time
    val days = (diff / (1000 * 60 * 60 * 24)).toInt()
    
    return when {
        days == 0 -> "Today"
        days == 1 -> "Yesterday"
        days < 7 -> "$days days ago"
        else -> {
            val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            format.format(date)
        }
    }
}
