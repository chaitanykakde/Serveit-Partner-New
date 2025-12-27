package com.nextserve.serveitpartnernew.ui.screen.main

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
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
 * Jobs Screen with tabs for New Jobs and History
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(
    modifier: Modifier = Modifier,
    providerId: String,
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
                Text(
                    text = "Jobs",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { 
                            selectedTabIndex = 0
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        text = { Text("New Jobs") },
                        icon = { Icon(Icons.Default.List, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { 
                            selectedTabIndex = 1
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        },
                        text = { Text("History") },
                        icon = { Icon(Icons.Default.Info, contentDescription = null) }
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
        modifier = modifier
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            userScrollEnabled = true
        ) { page ->
            when (page) {
                0 -> NewJobsTab(
                    jobs = uiState.newJobs,
                    isLoading = uiState.isLoadingNewJobs,
                    hasOngoingJob = uiState.hasOngoingJob,
                    acceptingJobId = uiState.acceptingJobId,
                    errorMessage = uiState.errorMessage?.takeIf { !it.contains("load more") },
                    onRetry = { viewModel.loadNewJobs() },
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
                    modifier = Modifier.fillMaxSize()
                )
                1 -> HistoryTab(
                    jobs = uiState.completedJobs,
                    isLoading = uiState.isLoadingHistory,
                    isLoadingMore = uiState.isLoadingMoreHistory,
                    hasMore = uiState.hasMoreHistory,
                    onLoadMore = { viewModel.loadMoreHistory() },
                    errorMessage = uiState.errorMessage?.takeIf { it.contains("load more") },
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
                        onJobAccepted(job.bookingId)
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
 */
@Composable
private fun NewJobsTab(
    jobs: List<Job>,
    isLoading: Boolean,
    hasOngoingJob: Boolean,
    acceptingJobId: String?,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    onAcceptClick: (Job) -> Unit,
    onRejectClick: (Job) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when {
            errorMessage != null && !isLoading && jobs.isEmpty() -> {
                ErrorState(
                    message = errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize()
                )
            }
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            jobs.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.List,
                    title = "No New Jobs",
                    description = "New job requests will appear here",
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Ongoing job banner
                    if (hasOngoingJob) {
                        OngoingJobBanner(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = jobs,
                            key = { it.bookingId }
                        ) { job ->
                            NewJobCard(
                                job = job,
                                isAccepting = acceptingJobId == job.bookingId,
                                isAcceptDisabled = hasOngoingJob,
                                onAcceptClick = { onAcceptClick(job) },
                                onRejectClick = { onRejectClick(job) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * History Tab - Shows completed jobs
 */
@Composable
private fun HistoryTab(
    jobs: List<Job>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Load more when scrolled near end
    LaunchedEffect(listState) {
        if (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == jobs.size - 1 && hasMore && !isLoadingMore) {
            onLoadMore()
        }
    }

    Box(modifier = modifier) {
        when {
            isLoading && jobs.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            jobs.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.Info,
                    title = "No Completed Jobs",
                    description = "Your completed jobs will appear here",
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = jobs,
                        key = { it.bookingId }
                    ) { job ->
                        CompletedJobCard(
                            job = job,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (isLoadingMore) {
                        item {
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
                        item {
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
        }
    }
}

/**
 * New Job Card
 */
@Composable
private fun NewJobCard(
    job: Job,
    isAccepting: Boolean,
    isAcceptDisabled: Boolean,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Service name
            Text(
                text = job.serviceName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Customer name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Customer: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = job.userName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Price
            Text(
                text = "₹${job.totalPrice.toInt()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRejectClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isAccepting
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Reject",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reject")
                }

                Button(
                    onClick = onAcceptClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isAcceptDisabled && !isAccepting
                ) {
                    if (isAccepting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Accept",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Accept")
                }
            }
        }
    }
}

/**
 * Completed Job Card
 */
@Composable
private fun CompletedJobCard(
    job: Job,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.serviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = job.userName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                job.completedAt?.let { completedAt ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Completed: ${formatDate(completedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
            Text(
                text = "₹${job.totalPrice.toInt()}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
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
 * Format timestamp to readable date
 */
private fun formatDate(timestamp: com.google.firebase.Timestamp): String {
    val date = timestamp.toDate()
    val now = java.util.Date()
    val diff = now.time - date.time
    val days = (diff / (1000 * 60 * 60 * 24)).toInt()
    
    return when {
        days == 0 -> "Today"
        days == 1 -> "Yesterday"
        days < 7 -> "$days days ago"
        else -> {
            val format = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            format.format(date)
        }
    }
}
