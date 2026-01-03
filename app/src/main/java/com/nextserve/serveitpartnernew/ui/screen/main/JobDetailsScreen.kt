package com.nextserve.serveitpartnernew.ui.screen.main

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.ui.components.ErrorState
import com.nextserve.serveitpartnernew.ui.viewmodel.JobDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailsScreen(
    bookingId: String,
    customerPhoneNumber: String,
    providerId: String,
    bookingIndex: Int? = null, // Optional: from inbox entry for optimized access
    onBack: () -> Unit,
    onJobAccepted: () -> Unit = {},
    onJobRejected: () -> Unit = {},
    viewModel: JobDetailsViewModel = viewModel(
        factory = JobDetailsViewModel.factory(
            bookingId = bookingId,
            customerPhoneNumber = customerPhoneNumber,
            providerId = providerId,
            context = LocalContext.current,
            bookingIndex = bookingIndex
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAcceptDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showStatusUpdateDialog by remember { mutableStateOf<String?>(null) } // Status to update

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Details") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Refresh button
                    androidx.compose.material3.IconButton(
                        onClick = { viewModel.loadJobDetails() },
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null && uiState.job == null -> {
                    ErrorState(
                        message = uiState.errorMessage ?: "Failed to load job details",
                        onRetry = { 
                            viewModel.loadJobDetails()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                uiState.job != null && uiState.errorMessage != null -> {
                    // Show error banner at top if job is loaded but there's an error
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Error banner
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.errorMessage ?: "An error occurred",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.loadJobDetails() }) {
                                    Text("Retry", color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                        // Job content
                        JobDetailsContent(
                            job = uiState.job!!,
                            viewModel = viewModel,
                            context = context,
                            uiState = uiState,
                            snackbarHostState = snackbarHostState,
                            coroutineScope = coroutineScope,
                            onAcceptClick = { showAcceptDialog = true },
                            onRejectClick = { showRejectDialog = true },
                            onStatusUpdateClick = { status -> showStatusUpdateDialog = status },
                            onJobCompleted = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Job completed successfully!")
                                    kotlinx.coroutines.delay(1500)
                                    onBack()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                uiState.job != null -> {
                    JobDetailsContent(
                        job = uiState.job!!,
                        viewModel = viewModel,
                        context = context,
                        uiState = uiState,
                        snackbarHostState = snackbarHostState,
                        coroutineScope = coroutineScope,
                        onAcceptClick = { showAcceptDialog = true },
                        onRejectClick = { showRejectDialog = true },
                        onStatusUpdateClick = { status -> showStatusUpdateDialog = status },
                        onJobCompleted = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Job completed successfully!")
                                // Navigate back after a short delay
                                kotlinx.coroutines.delay(1500)
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Accept job dialog
    if (showAcceptDialog) {
        AlertDialog(
            onDismissRequest = { showAcceptDialog = false },
            title = { Text("Accept Job?") },
            text = {
                Column {
                    Text("Service: ${uiState.job?.serviceName ?: ""}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Customer: ${uiState.job?.userName ?: ""}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Amount: ₹${uiState.job?.totalPrice?.toInt() ?: 0}")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.acceptJob(
                            onSuccess = {
                                showAcceptDialog = false
                                onJobAccepted()
                            },
                            onError = { error ->
                                showAcceptDialog = false
                                // Error will be shown in UI state
                            }
                        )
                    },
                    enabled = !uiState.isAccepting
                ) {
                    if (uiState.isAccepting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Accept")
                    }
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showAcceptDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reject job dialog
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Job?") },
            text = {
                Text("Are you sure you want to reject this job? You won't be able to accept it later.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectJob {
                            showRejectDialog = false
                            onJobRejected()
                        }
                    },
                    enabled = !uiState.isRejecting
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Status update confirmation dialogs
    showStatusUpdateDialog?.let { status ->
        val (title, message, confirmText) = when (status) {
            "arrived" -> Triple(
                "Mark as Arrived?",
                "Confirm that you have arrived at the customer's location.",
                "Mark Arrived"
            )
            "in_progress" -> Triple(
                "Start Service?",
                "Confirm that you are starting the service work.",
                "Start Service"
            )
            "payment_pending" -> Triple(
                "Complete Service?",
                "Confirm that the service work is complete and waiting for payment.",
                "Complete Service"
            )
            "completed" -> Triple(
                "Mark as Completed?",
                "Confirm that the job is fully completed and payment has been received.",
                "Mark Completed"
            )
            else -> Triple("Confirm Action?", "Are you sure?", "Confirm")
        }

        AlertDialog(
            onDismissRequest = { showStatusUpdateDialog = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                Button(
                    onClick = {
                        val action: () -> Unit = when (status) {
                            "arrived" -> {
                                {
                                    viewModel.markAsArrived(
                                        onSuccess = {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Marked as arrived")
                                                showStatusUpdateDialog = null
                                            }
                                        },
                                        onError = { error ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(error)
                                                showStatusUpdateDialog = null
                                            }
                                        }
                                    )
                                }
                            }
                            "in_progress" -> {
                                {
                                    viewModel.markAsInProgress(
                                        onSuccess = {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Service started")
                                                showStatusUpdateDialog = null
                                            }
                                        },
                                        onError = { error ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(error)
                                                showStatusUpdateDialog = null
                                            }
                                        }
                                    )
                                }
                            }
                            "payment_pending" -> {
                                {
                                    viewModel.markAsPaymentPending(
                                        onSuccess = {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Service completed, waiting for payment")
                                                showStatusUpdateDialog = null
                                            }
                                        },
                                        onError = { error ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(error)
                                                showStatusUpdateDialog = null
                                            }
                                        }
                                    )
                                }
                            }
                            "completed" -> {
                                {
                                    viewModel.markAsCompleted(
                                        onSuccess = {
                                            showStatusUpdateDialog = null
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Job completed successfully!")
                                                kotlinx.coroutines.delay(1500)
                                                onBack()
                                            }
                                        },
                                        onError = { error ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(error)
                                                showStatusUpdateDialog = null
                                            }
                                        }
                                    )
                                }
                            }
                            else -> { { showStatusUpdateDialog = null } }
                        }
                        action()
                    },
                    enabled = !(uiState.isUpdatingStatus && uiState.updatingStatusType == status)
                ) {
                    if (uiState.isUpdatingStatus && uiState.updatingStatusType == status) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(confirmText)
                    }
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showStatusUpdateDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Show error message if any
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            if (uiState.job != null) { // Only show if job is loaded (not initial load error)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(error)
                }
            }
        }
    }
}

@Composable
private fun JobDetailsContent(
    job: Job,
    viewModel: JobDetailsViewModel,
    context: Context,
    uiState: com.nextserve.serveitpartnernew.ui.viewmodel.JobDetailsUiState,
    snackbarHostState: SnackbarHostState,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit,
    onStatusUpdateClick: (String) -> Unit = {},
    onJobCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 80.dp), // Add bottom padding to account for bottom navigation bar
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Enhanced Header Section with Status, Service Name, and Price
        JobHeaderSection(job = job, viewModel = viewModel)

        // Service Breakdown Card with Sub-services
        ServiceBreakdownCard(job = job)

        // Customer Contact Card
        CustomerContactCard(
            job = job,
            onCallClick = { viewModel.callCustomer(context) }
        )

        // Location Details Card
        LocationDetailsCard(
            job = job,
            viewModel = viewModel,
            onNavigateClick = { viewModel.navigateToLocation(context) }
        )

        // Job Timeline Card with Visual Timeline
        JobTimelineCard(job = job, viewModel = viewModel)

        // Sub-Services Breakdown Card (if sub-services exist)
        if (job.subServicesSelected != null && job.subServicesSelected.isNotEmpty()) {
            SubServicesCard(job = job)
        }

        // Notes/Instructions Card (if available)
        if (!job.notes.isNullOrEmpty()) {
            NotesCard(notes = job.notes!!)
        }

        // Provider Information Card (if job is accepted)
        if (job.providerId != null && job.providerName != null) {
            ProviderInfoCard(job = job, viewModel = viewModel)
        }

        // Smart Action Buttons - Context-aware based on status
        SmartActionButtons(
            job = job,
            uiState = uiState,
            viewModel = viewModel,
            context = context,
            snackbarHostState = snackbarHostState,
            coroutineScope = coroutineScope,
            onAcceptClick = onAcceptClick,
            onRejectClick = onRejectClick,
            onStatusUpdateClick = onStatusUpdateClick,
            onJobCompleted = onJobCompleted
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun JobHeaderSection(job: Job, viewModel: JobDetailsViewModel) {
    val (statusColor, statusText, statusIcon) = when (job.status.lowercase()) {
        "pending" -> Triple(Color(0xFFFF9800), "Pending", Icons.Default.Info)
        "accepted" -> Triple(Color(0xFF2196F3), "Accepted", Icons.Default.CheckCircle)
        "arrived" -> Triple(Color(0xFF9C27B0), "Arrived", Icons.Default.LocationOn)
        "in_progress" -> Triple(Color(0xFF9C27B0), "In Progress", Icons.Default.Build)
        "payment_pending" -> Triple(Color(0xFFFF9800), "Payment Pending", Icons.Default.Info)
        "completed" -> Triple(Color(0xFF4CAF50), "Completed", Icons.Default.CheckCircle)
        else -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, job.status, Icons.Default.Info)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                // Booking ID (small, subtle)
                Text(
                    text = "ID: ${job.bookingId.take(8)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            HorizontalDivider()

            // Service Name
            Text(
                text = job.serviceName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Total Price - Large and Prominent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Amount",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹${job.totalPrice.toInt()}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    // modifier = Modifier.semantics {
                    //     contentDescription = AccessibilityUtils.JOB_PRICE_DESCRIPTION
                    // } // Temporarily disabled
                )
            }

            // Estimated Duration (if available)
            job.estimatedDuration?.let { duration ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Estimated Duration: $duration minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceBreakdownCard(job: Job) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Service Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            HorizontalDivider()

            // Service Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Service Type:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = job.serviceName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


@Composable
private fun NotesCard(notes: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Notes / Special Instructions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            Text(
                text = notes,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SubServicesCard(job: Job) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Sub-Services Breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            job.subServicesSelected?.forEach { (serviceName, serviceData) ->
                val serviceMap = serviceData as? Map<*, *>
                val name = serviceMap?.get("name") as? String ?: serviceName
                val description = serviceMap?.get("description") as? String
                val price = (serviceMap?.get("price") as? Number)?.toDouble() ?: 0.0
                val unit = serviceMap?.get("unit") as? String ?: ""

                SubServiceItem(
                    name = name,
                    description = description,
                    price = price,
                    unit = unit
                )
                if (serviceName != job.subServicesSelected.keys.last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₹${job.totalPrice.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SubServiceItem(
    name: String,
    description: String?,
    price: Double,
    unit: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "₹${price.toInt()}${if (unit.isNotEmpty()) " / $unit" else ""}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CustomerContactCard(
    job: Job,
    onCallClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customer Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onCallClick,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call Customer",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            HorizontalDivider()

            // Customer Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.userName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Customer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Phone Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = job.customerPhoneNumber,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Email (if available)
            job.customerEmail?.let { email ->
                if (email.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationDetailsCard(
    job: Job,
    viewModel: JobDetailsViewModel,
    onNavigateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Location Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (job.jobCoordinates != null) {
                    Button(
                        onClick = onNavigateClick,
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Navigate",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Location Name
            if (!job.locationName.isNullOrEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = job.locationName!!,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Full Address
            if (!job.customerAddress.isNullOrEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = job.customerAddress!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else if (job.jobCoordinates != null) {
                Text(
                    text = "Coordinates: ${job.jobCoordinates.latitude}, ${job.jobCoordinates.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Distance
            if (job.distance != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Distance: ${viewModel.formatDistance(job.distance)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun JobTimelineCard(job: Job, viewModel: JobDetailsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Job Timeline",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            HorizontalDivider()

            // Visual Timeline
            TimelineItem(
                label = "Created",
                timestamp = job.createdAt,
                viewModel = viewModel,
                isCompleted = true,
                icon = Icons.Default.Info
            )
            TimelineItem(
                label = "Accepted",
                timestamp = job.acceptedAt,
                viewModel = viewModel,
                isCompleted = job.status != "pending",
                icon = Icons.Default.CheckCircle
            )
            TimelineItem(
                label = "Arrived",
                timestamp = job.arrivedAt,
                viewModel = viewModel,
                isCompleted = job.status in listOf("arrived", "in_progress", "payment_pending", "completed"),
                icon = Icons.Default.LocationOn
            )
            TimelineItem(
                label = "Service Started",
                timestamp = job.serviceStartedAt,
                viewModel = viewModel,
                isCompleted = job.status in listOf("in_progress", "payment_pending", "completed"),
                icon = Icons.Default.Build
            )
            TimelineItem(
                label = "Payment Pending",
                timestamp = null, // We don't have a separate timestamp for payment_pending
                viewModel = viewModel,
                isCompleted = job.status in listOf("payment_pending", "completed"),
                icon = Icons.Default.Info
            )
            TimelineItem(
                label = "Completed",
                timestamp = job.completedAt,
                viewModel = viewModel,
                isCompleted = job.status == "completed",
                icon = Icons.Default.CheckCircle
            )
        }
    }
}

@Composable
private fun TimelineItem(
    label: String,
    timestamp: com.google.firebase.Timestamp?,
    viewModel: JobDetailsViewModel,
    isCompleted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCompleted) FontWeight.Medium else FontWeight.Normal,
                color = if (isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            if (isCompleted && timestamp != null) {
                Text(
                    text = viewModel.formatRelativeTime(timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!isCompleted) {
                Text(
                    text = "Pending",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun ProviderInfoCard(job: Job, viewModel: JobDetailsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Provider Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            job.providerName?.let { name ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Service Provider",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            job.providerMobileNo?.let { phone ->
                if (phone.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            job.acceptedAt?.let { acceptedAt ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Accepted: ${viewModel.formatRelativeTime(acceptedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartActionButtons(
    job: Job,
    uiState: com.nextserve.serveitpartnernew.ui.viewmodel.JobDetailsUiState,
    viewModel: JobDetailsViewModel,
    context: Context,
    snackbarHostState: SnackbarHostState,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit,
    onStatusUpdateClick: (String) -> Unit = {},
    onJobCompleted: () -> Unit
) {
    val isUpdating = uiState.isUpdatingStatus
    val updatingType = uiState.updatingStatusType

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (job.status.lowercase()) {
            "pending" -> {
                // Accept and Reject buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRejectClick,
                        modifier = Modifier.weight(1f).height(56.dp),
                        enabled = !uiState.isRejecting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = "✕",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reject", fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = onAcceptClick,
                        modifier = Modifier.weight(1f).height(56.dp),
                        enabled = !uiState.isAccepting,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isAccepting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "✓",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Accept", fontWeight = FontWeight.Medium)
                    }
                }
            }
            "accepted" -> {
                // Primary: Mark as Arrived, Secondary: Call Customer, Navigate
                Button(
                    onClick = { onStatusUpdateClick("arrived") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isUpdating,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUpdating && updatingType == "arrived") {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Mark as Arrived",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Mark as Arrived", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
                }

                // Secondary actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.callCustomer(context) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call", fontWeight = FontWeight.Medium)
                    }
                    if (job.jobCoordinates != null) {
                        OutlinedButton(
                            onClick = { viewModel.navigateToLocation(context) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Navigate", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Navigate", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            "arrived" -> {
                // Primary: Start Service, Secondary: Call Customer
                Button(
                    onClick = { onStatusUpdateClick("in_progress") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isUpdating,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUpdating && updatingType == "in_progress") {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = "Start Service",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Start Service", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
                }

                OutlinedButton(
                    onClick = { viewModel.callCustomer(context) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Customer", fontWeight = FontWeight.Medium)
                }
            }
            "in_progress" -> {
                // Primary: Mark Payment Pending, Secondary: Call Customer
                Button(
                    onClick = { onStatusUpdateClick("payment_pending") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isUpdating,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUpdating && updatingType == "payment_pending") {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Complete Service",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Complete Service", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
                }

                OutlinedButton(
                    onClick = { viewModel.callCustomer(context) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Customer", fontWeight = FontWeight.Medium)
                }
            }
            "payment_pending" -> {
                // Primary: Mark as Completed (large, prominent), Secondary: Call Customer
                Button(
                    onClick = { onStatusUpdateClick("completed") },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    enabled = !isUpdating,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isUpdating && updatingType == "completed") {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Mark as Completed",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Mark as Completed",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.callCustomer(context) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Customer", fontWeight = FontWeight.Medium)
                }
            }
            "completed" -> {
                // Completed state - show success message
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Job Completed Successfully",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(
    job: Job,
    uiState: com.nextserve.serveitpartnernew.ui.viewmodel.JobDetailsUiState,
    viewModel: JobDetailsViewModel,
    snackbarHostState: SnackbarHostState,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit,
    onJobCompleted: () -> Unit
) {
    val context = LocalContext.current
    val isUpdating = uiState.isUpdatingStatus
    val updatingType = uiState.updatingStatusType

    when (job.status.lowercase()) {
        "pending" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRejectClick,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isRejecting
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
                    enabled = !uiState.isAccepting
                ) {
                    if (uiState.isAccepting) {
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
        "accepted" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.callCustomer(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Call",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Customer")
                }

                Button(
                    onClick = {
                        viewModel.markAsArrived(
                            onSuccess = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Marked as arrived")
                                }
                            },
                            onError = { error ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(error)
                                }
                            }
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isUpdating
                ) {
                    if (isUpdating && updatingType == "arrived") {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Arrived",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Arrived")
                }
            }
        }
        "arrived" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.callCustomer(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Call",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Customer")
                }

                Button(
                    onClick = {
                        viewModel.markAsInProgress(
                            onSuccess = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Service started")
                                }
                            },
                            onError = { error ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(error)
                                }
                            }
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isUpdating
                ) {
                    if (isUpdating && updatingType == "in_progress") {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Start",
                        modifier = Modifier.size(18.dp)
                    )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Service")
                }
            }
        }
        "in_progress" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.callCustomer(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Call",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Customer")
                }

                Button(
                    onClick = {
                        viewModel.markAsPaymentPending(
                            onSuccess = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Service completed, waiting for payment")
                                }
                            },
                            onError = { error ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(error)
                                }
                            }
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isUpdating
                ) {
                    if (isUpdating && updatingType == "payment_pending") {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Complete",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Complete Service")
                }
            }
        }
        "payment_pending" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.callCustomer(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Call",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Customer")
                }

                Button(
                    onClick = {
                        viewModel.markAsCompleted(
                            onSuccess = {
                                onJobCompleted()
                            },
                            onError = { error ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(error)
                                }
                            }
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isUpdating
                ) {
                    if (isUpdating && updatingType == "completed") {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Complete",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Completed")
                }
            }
        }
        "completed" -> {
            // Completed job - no actions, just show message
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Job completed",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

