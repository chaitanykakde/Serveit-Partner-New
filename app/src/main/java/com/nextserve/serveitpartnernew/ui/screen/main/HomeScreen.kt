package com.nextserve.serveitpartnernew.ui.screen.main

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import com.nextserve.serveitpartnernew.data.repository.JobsRepository
import com.nextserve.serveitpartnernew.ui.components.EmptyState
import com.nextserve.serveitpartnernew.ui.components.ErrorState
import com.nextserve.serveitpartnernew.ui.viewmodel.HomeViewModel
import com.nextserve.serveitpartnernew.utils.NetworkMonitor
import kotlinx.coroutines.launch

/**
 * Home Screen - Modern, minimalist design matching screenshot
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    providerId: String,
    onJobAccepted: (String) -> Unit = {},
    onViewAllJobs: () -> Unit = {},
    onOngoingJobClick: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(
            providerId = providerId,
            context = LocalContext.current
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val firestoreRepo = remember { FirestoreRepository(FirebaseProvider.firestore) }
    
    var providerName by remember { mutableStateOf("") }
    var showAcceptDialog by remember { mutableStateOf<Job?>(null) }

    // Load provider name
    LaunchedEffect(providerId) {
        firestoreRepo.getProviderData(providerId).fold(
            onSuccess = { data -> providerName = data?.fullName?.split(" ")?.firstOrNull() ?: "Provider" },
            onFailure = { providerName = "Provider" }
        )
    }

    // Show error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.clearError()
            }
        }
    }

    Scaffold(
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
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with icon and greeting
            item {
                HomeHeader(providerName = providerName)
            }

            // Highlighted New Job Card
            val highlightedJob = uiState.highlightedJob
            if (highlightedJob != null) {
                item {
                    HighlightedJobCard(
                        job = highlightedJob,
                        hasOngoingJob = uiState.hasOngoingJob,
                        isAccepting = uiState.acceptingJobId == highlightedJob.bookingId,
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
                        onViewAllClick = onViewAllJobs
                    )
                }
                
                // View all jobs link
                item {
                    TextButton(
                        onClick = onViewAllJobs,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "View all jobs →",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Ongoing Jobs Section
            if (uiState.ongoingJobs.isNotEmpty()) {
                item {
                    Text(
                        text = "Ongoing Jobs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp
                    )
                }

                items(
                    items = uiState.ongoingJobs,
                    key = { it.bookingId }
                ) { job ->
                    OngoingJobCard(
                        job = job,
                        onClick = { onOngoingJobClick(job.bookingId) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Today Section
            if (uiState.todayCompletedJobs.isNotEmpty()) {
                item {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp
                    )
                }

                items(
                    items = uiState.todayCompletedJobs.take(3), // Show max 3 today's jobs
                    key = { it.bookingId }
                ) { job ->
                    TodayJobCard(
                        job = job,
                        onClick = { onOngoingJobClick(job.bookingId) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Stats Card - Only show if there are stats
            if (uiState.todayStats.first > 0 || uiState.todayStats.second > 0) {
                item {
                    StatsCard(
                        jobsCompleted = uiState.todayStats.first,
                        earnings = uiState.todayStats.second
                    )
                }
            }

            // Error state with retry
            if (uiState.errorMessage != null && !uiState.isLoading && uiState.highlightedJob == null && uiState.ongoingJobs.isEmpty()) {
                item {
                    ErrorState(
                        message = uiState.errorMessage ?: "An error occurred",
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Empty state
            if (uiState.highlightedJob == null && uiState.ongoingJobs.isEmpty() && !uiState.isLoading && uiState.errorMessage == null) {
                item {
                    EmptyState(
                        icon = Icons.Default.Home,
                        title = "No Jobs Available",
                        description = "New job requests will appear here when available",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Loading state
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
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
 * Get service icon based on service name
 * Uses available Material Icons for better visual distinction
 */
@Composable
private fun getServiceIcon(serviceName: String): ImageVector {
    return when (serviceName.lowercase()) {
        "electrical", "electrical service", "electrician" -> Icons.Default.Settings // Electrical/technical work
        "plumbing", "plumbing service", "plumber" -> Icons.Default.Build // Tools/repair work
        "carpentry", "carpentry service", "carpenter" -> Icons.Default.Build // Construction/tools
        "cleaning", "cleaning service", "house cleaning" -> Icons.Default.Home // Home/cleaning
        "painting", "painting service", "painter" -> Icons.Default.Build // Construction work
        "ac repair", "ac service", "air conditioning" -> Icons.Default.Settings // Technical/AC
        "appliance repair", "appliance service" -> Icons.Default.Settings // Technical repair
        else -> Icons.Default.Build // Default to Build for any service
    }
}

/**
 * Home Header - Dark blue icon with greeting
 */
@Composable
private fun HomeHeader(providerName: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Dark blue square icon with house
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = Color(0xFF1976D2), // Dark blue
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Column {
            Text(
                text = "Hello, $providerName",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Ready to work today?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Highlighted New Job Card - Modern design matching screenshot
 */
@Composable
private fun HighlightedJobCard(
    job: Job,
    hasOngoingJob: Boolean,
    isAccepting: Boolean,
    onAcceptClick: (Job) -> Unit,
    onRejectClick: (Job) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5) // Light gray background
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Bell icon + "New Job Available"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "New Job Available",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Service icon + Service name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = getServiceIcon(job.serviceName),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = job.serviceName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location + Distance
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = job.locationName ?: "Location",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                job.distance?.let { distance ->
                    Text(
                        text = "${String.format("%.1f", distance)} km",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                } ?: Text(
                    text = "Distance N/A",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Price on right + Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price
                Text(
                    text = "₹${job.totalPrice.toInt()}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 24.sp
                )

                // Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Reject button (light red)
                    OutlinedButton(
                        onClick = { onRejectClick(job) },
                        enabled = !isAccepting,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD32F2F) // Light red
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reject", fontSize = 14.sp)
                    }

                    // Accept button (blue with arrow)
                    Button(
                        onClick = { onAcceptClick(job) },
                        enabled = !hasOngoingJob && !isAccepting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isAccepting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Accept", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ongoing Job Card - Modern design
 */
@Composable
private fun OngoingJobCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Service icon
                Icon(
                    imageVector = getServiceIcon(job.serviceName),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Column {
                    Text(
                        text = job.serviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = job.userName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Status badge
                    StatusBadge(status = job.status)
                }
            }
            
            // View details link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "View details",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Today Job Card - For completed jobs today
 */
@Composable
private fun TodayJobCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Service icon
                Icon(
                    imageVector = getServiceIcon(job.serviceName),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Column {
                    Text(
                        text = job.serviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = job.userName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
            
            // View details link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "View details",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Stats Card - Shows today's summary
 */
@Composable
private fun StatsCard(
    jobsCompleted: Int,
    earnings: Double
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Jobs completed
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Jobs completed: $jobsCompleted",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
            }
            
            // Earnings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "₹",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Earnings: ₹${earnings.toInt()}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/**
 * Status Badge - Light green pill for "In Progress"
 */
@Composable
private fun StatusBadge(status: String) {
    val statusData = when (status.lowercase()) {
        "accepted" -> Triple("Accepted", Color(0xFFE3F2FD), Color(0xFF1976D2))
        "arrived" -> Triple("Arrived", Color(0xFFE1F5FE), Color(0xFF0277BD))
        "in_progress" -> Triple("In Progress", Color(0xFFE8F5E9), Color(0xFF2E7D32)) // Light green
        "payment_pending" -> Triple("Payment Pending", Color(0xFFFFEBEE), Color(0xFFC62828))
        else -> Triple(status, Color(0xFFF5F5F5), MaterialTheme.colorScheme.onSurfaceVariant)
    }
    val (text, backgroundColor, textColor) = statusData

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
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
