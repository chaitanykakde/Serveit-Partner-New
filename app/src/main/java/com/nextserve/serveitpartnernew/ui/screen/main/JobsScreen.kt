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
                            fontWeight = FontWeight.Medium
                        )
                    },
                    actions = {
                        IconButton(
                                    onClick = {
                                        // Cycle through sort options
                                        val nextSort = when (uiState.sortBy) {
                                            "distance" -> "price"
                                            "price" -> "time"
                                            else -> "distance"
                                        }
                                        viewModel.setSortBy(nextSort)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Sort",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                                    )
                )
                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { 
                            selectedTabIndex = 0
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        text = { Text("New Jobs") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { 
                            selectedTabIndex = 1
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        },
                        text = { Text("History") }
                    )
                }
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
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 16.dp + parentBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        OngoingJobBanner(
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
                                NewJobCard(
                                    job = job,
                                    isAccepting = acceptingJobId == job.bookingId,
                                    isAcceptDisabled = hasOngoingJob,
                                    onJobClick = { onJobClick(job) },
                                    onAcceptClick = { onAcceptClick(job) },
                                    onRejectClick = { onRejectClick(job) },
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
            top = 16.dp,
            bottom = 16.dp + parentBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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

        // 3️⃣ NORMAL CONTENT: Render job cards
                    items(
                        items = jobs,
                        key = { it.bookingId }
                    ) { job ->
                        CompletedJobCard(
                            job = job,
                            onClick = { onJobClick(job) },
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
 * New Job Card - Modern Material Design 3 UI
 */
@Composable
private fun NewJobCard(
    job: Job,
    isAccepting: Boolean,
    isAcceptDisabled: Boolean,
    onJobClick: () -> Unit = {},
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onJobClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Service name and price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Service icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = job.serviceName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // Enhanced Status badge with better design
                        androidx.compose.material3.Surface(
                            modifier = Modifier.padding(top = 6.dp),
                            color = when (job.status.lowercase()) {
                                "pending" -> MaterialTheme.colorScheme.secondaryContainer
                                "accepted" -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(8.dp),
                            tonalElevation = 1.dp
                        ) {
                            Text(
                                text = when (job.status.lowercase()) {
                                    "pending" -> "Available"
                                    "accepted" -> "Accepted"
                                    else -> job.status.capitalize()
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (job.status.lowercase()) {
                                    "pending" -> MaterialTheme.colorScheme.onSecondaryContainer
                                    "accepted" -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Priority badge for high-priority jobs
                        if (job.notes == "High Priority") {
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "High Priority",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        // Expiry indicator for pending jobs
                        job.expiresAt?.let { expiry ->
                            if (job.status == "pending") {
                                val timeRemaining = job.getTimeRemainingMinutes()
                                if (timeRemaining != null && timeRemaining > 0) {
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = if (timeRemaining <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = if (timeRemaining <= 5) {
                                                "Expires in $timeRemaining min"
                                            } else {
                                                "$timeRemaining min left"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (timeRemaining <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else if (job.isExpired()) {
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "Expired",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                // Price
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${job.totalPrice.toInt()}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Customer information
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = job.userName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location and distance
            if (job.locationName != null || job.distance != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        if (job.locationName != null) {
                            Text(
                                text = job.locationName!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                        if (job.distance != null) {
                            Text(
                                text = "${String.format("%.1f", job.distance)} km away",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Sub-services preview
            job.subServicesSelected?.let { subServices ->
                if (subServices.isNotEmpty()) {
                    val serviceNames = subServices.keys.take(3).joinToString(", ")
                    val moreCount = subServices.size - 3
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (moreCount > 0) {
                                "$serviceNames +$moreCount more"
                            } else {
                                serviceNames
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Timestamp
            job.createdAt?.let { timestamp ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatRelativeTime(timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            } ?: Spacer(modifier = Modifier.height(4.dp))

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // View Details button
            OutlinedButton(
                onClick = onJobClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("View Details")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRejectClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = !isAccepting,
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Reject",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reject", fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = onAcceptClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = !isAcceptDisabled && !isAccepting,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isAccepting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Accept",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Accept", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Status Badge Component
 */
@Composable
private fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (color, text, icon) = when (status.lowercase()) {
        "pending" -> Triple(
            MaterialTheme.colorScheme.secondary,
            "Pending",
            Icons.Default.Warning
        )
        "accepted" -> Triple(
            MaterialTheme.colorScheme.primary,
            "Accepted",
            Icons.Default.CheckCircle
        )
        "arrived" -> Triple(
            MaterialTheme.colorScheme.tertiary,
            "Arrived",
            Icons.Default.LocationOn
        )
        "in_progress" -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            "In Progress",
            Icons.Default.Build
        )
        "payment_pending" -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            "Payment Pending",
            Icons.Default.Info
        )
        "completed" -> Triple(
            MaterialTheme.colorScheme.primary,
            "Completed",
            Icons.Default.CheckCircle
        )
        else -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            status,
            Icons.Default.Info
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = color
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

/**
 * Completed Job Card - Modern Material Design 3 UI
 */
@Composable
private fun CompletedJobCard(
    job: Job,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Service name and price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Service icon with completed indicator
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = job.serviceName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // Completed badge
                        StatusBadge(
                            status = "completed",
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                // Price
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${job.totalPrice.toInt()}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Customer information
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = job.userName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location
            if (job.locationName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = job.locationName!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Completion date
            job.completedAt?.let { completedAt ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Completed: ${formatRelativeTime(completedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            } ?: Spacer(modifier = Modifier.height(4.dp))

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // View Details button
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("View Details")
            }
        }
    }
}

/**
 * Ongoing Job Banner
 */
@Composable
private fun OngoingJobBanner(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(12.dp)
    ) {
        Text(
            text = "Complete ongoing job to accept new requests",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
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
