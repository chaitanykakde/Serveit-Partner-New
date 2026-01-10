package com.nextserve.serveitpartnernew.ui.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import com.nextserve.serveitpartnernew.ui.components.EmptyState
import com.nextserve.serveitpartnernew.ui.components.ErrorState
import com.nextserve.serveitpartnernew.ui.components.SectionHeader
import com.nextserve.serveitpartnernew.ui.viewmodel.HomeViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Home Screen - Google-grade Material 3 design with premium UX
 * Coordinator pattern: Composes sections without containing UI logic
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    providerId: String,
    onJobAccepted: (String) -> Unit = {},
    onOngoingJobClick: (Job) -> Unit = {},
    onViewAllJobs: () -> Unit = {},
    viewModel: HomeViewModel
) {
    android.util.Log.d("HomeScreen", "🎨 HomeScreen composable called for provider: $providerId")

    // Collect state from ViewModel - this properly triggers recomposition when Flow emits
    val uiState by viewModel.uiState.collectAsState()
    val highlightedJob = uiState.highlightedJob
    val ongoingJobs = uiState.ongoingJobs
    val todayCompletedJobs = uiState.todayCompletedJobs.take(3)
    val todayStats = uiState.todayStats
    val hasOngoingJob = uiState.hasOngoingJob
    val isLoading = uiState.isLoading
    val errorMessage = uiState.errorMessage
    val acceptingJobId = uiState.acceptingJobId

    android.util.Log.d("HomeScreen", "📊 UI State - highlightedJob: ${highlightedJob != null}, ongoingJobs: ${ongoingJobs.size}, isLoading: $isLoading, hasOngoingJob: $hasOngoingJob")

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val firestoreRepo = remember { FirestoreRepository(FirebaseProvider.firestore) }

    var providerName by remember { mutableStateOf("") }
    var showAcceptDialog by remember { mutableStateOf<Job?>(null) }

    // Trigger data loading on first composition (Google-grade Compose pattern)
    LaunchedEffect(Unit) {
        android.util.Log.d("HomeScreen", "🚀 LaunchedEffect(Unit) triggered - calling viewModel.refresh()")
        viewModel.refresh()
    }

    // Track if we've attempted to load data to avoid showing empty state immediately
    var hasAttemptedDataLoad by remember { mutableStateOf(false) }

    // Load provider name and mark data load as attempted
    LaunchedEffect(providerId) {
        // Data loading is initiated by ViewModel, mark as attempted after a brief delay
        delay(100) // Small delay to ensure ViewModel has started loading
        hasAttemptedDataLoad = true

        try {
            val result = firestoreRepo.getProviderData(providerId)
            result.onSuccess { data ->
                providerName = data?.fullName?.split(" ")?.firstOrNull() ?: "Provider"
            }
        } catch (e: Exception) {
            providerName = "Provider"
        }
    }

    // Show error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            providerName.isNotEmpty() -> "Welcome back, ${providerName.split(" ").firstOrNull() ?: providerName}!"
                            else -> "Welcome to Serveit Partner!"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        // HARD-GATED: Skeleton fully owns screen during loading
        android.util.Log.d("HomeRender", "isLoading = $isLoading")

        if (isLoading) {
            android.util.Log.d("HomeRender", "Rendering HomeSkeleton (hard gate)")
            HomeSkeleton(paddingValues = paddingValues)
        } else {
            android.util.Log.d("HomeRender", "Rendering HomeContent (hard gate)")
            HomeContent(
                paddingValues = paddingValues,
                highlightedJob = highlightedJob,
                hasOngoingJob = hasOngoingJob,
                acceptingJobId = acceptingJobId,
                ongoingJobs = ongoingJobs,
                todayCompletedJobs = todayCompletedJobs,
                todayStats = todayStats,
                errorMessage = errorMessage,
                hasAttemptedDataLoad = hasAttemptedDataLoad,
                onShowAcceptDialog = { showAcceptDialog = it },
                onJobReject = { viewModel.rejectJob(it) },
                onViewAllJobs = onViewAllJobs,
                onOngoingJobClick = onOngoingJobClick,
                onRefresh = { viewModel.refresh() }
            )
        }
    }

    // Accept job dialog
    showAcceptDialog?.let { job ->
        AlertDialog(
            onDismissRequest = { showAcceptDialog = null },
            title = {
                Text("Accept Job")
            },
            text = {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Service: ${job.serviceName}")
                    Text("Customer: ${job.userName}")
                    Text("Amount: ₹${job.totalPrice.toInt()}")
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (hasOngoingJob == false) {
                        viewModel.acceptJob(
                            job = job,
                            onSuccess = {
                                showAcceptDialog = null
                                onJobAccepted(job.bookingId)
                            },
                            onError = { error: String ->
                                showAcceptDialog = null
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(error)
                                }
                            }
                        )
                    } else {
                        showAcceptDialog = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Complete ongoing job to accept new requests")
                        }
                    }
                }) {
                    Text("Accept")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAcceptDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Home Skeleton - Google-grade professional loading placeholders
 */
@Composable
private fun HomeSkeleton(paddingValues: PaddingValues) {
    LaunchedEffect(Unit) {
        android.util.Log.d("HomeRender", "HomeSkeleton entered composition")
    }

    // Subtle alpha animation for professional feel
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // 3 professional skeleton cards with internal structure
        items(3) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    // Circular avatar placeholder
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                CircleShape
                            )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Content structure simulation
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Title line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(14.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    MaterialTheme.shapes.small
                                )
                        )

                        // Subtitle line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(12.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    MaterialTheme.shapes.small
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Home Content - Shows actual data after loading
 */
@Composable
private fun HomeContent(
    paddingValues: PaddingValues,
    highlightedJob: Job?,
    hasOngoingJob: Boolean,
    acceptingJobId: String?,
    ongoingJobs: List<Job>,
    todayCompletedJobs: List<Job>,
    todayStats: Pair<Int, Double>,
    errorMessage: String?,
    hasAttemptedDataLoad: Boolean,
    onShowAcceptDialog: (Job) -> Unit,
    onJobReject: (Job) -> Unit,
    onViewAllJobs: () -> Unit,
    onOngoingJobClick: (Job) -> Unit,
    onRefresh: () -> Unit
) {
    LaunchedEffect(Unit) {
        android.util.Log.d("HomeRender", "HomeContent entered composition")
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .animateContentSize()
    ) {
        // New Requests Section
        if (highlightedJob != null) {
            item { SectionHeader("New Requests") }
            HomeNewJobSection(
                highlightedJob = highlightedJob,
                hasOngoingJob = hasOngoingJob,
                acceptingJobId = acceptingJobId,
                onShowAcceptDialog = onShowAcceptDialog,
                onJobReject = onJobReject,
                onViewAllJobs = onViewAllJobs
            )
        }

        // Ongoing Jobs Section
        if (ongoingJobs.isNotEmpty()) {
            item { SectionHeader("Ongoing Jobs") }
            HomeOngoingSection(
                ongoingJobs = ongoingJobs,
                onOngoingJobClick = onOngoingJobClick
            )
        }

        // Today Section
        if (todayCompletedJobs.isNotEmpty()) {
            item { SectionHeader("Today") }
            HomeTodaySection(
                todayCompletedJobs = todayCompletedJobs,
                onOngoingJobClick = onOngoingJobClick
            )
        }

        // Today's Summary Section
        item { SectionHeader("Today's Summary") }
        HomeStatsSection(
            todayJobsCompleted = todayStats.first,
            todayEarnings = todayStats.second,
            isLoading = false
        )

        // Error state with retry
        item {
            if (errorMessage != null && highlightedJob == null && ongoingJobs.isEmpty()) {
                ErrorState(
                    message = errorMessage ?: "An error occurred",
                    onRetry = onRefresh,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Empty state - only show after attempting to load data and when no jobs
        item {
            if (hasAttemptedDataLoad && highlightedJob == null && ongoingJobs.isEmpty() && errorMessage == null) {
                EmptyState(
                    icon = Icons.Default.Home,
                    title = "No Jobs Available",
                    description = "New job requests will appear here when available",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
