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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.ui.components.ErrorState
import com.nextserve.serveitpartnernew.ui.sections.*
import com.nextserve.serveitpartnernew.ui.sections.NotesSection
import com.nextserve.serveitpartnernew.ui.sections.ServiceInformationSection
import com.nextserve.serveitpartnernew.ui.viewmodel.JobDetailsViewModel
import com.nextserve.serveitpartnernew.utils.JobStatusUtils

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
    var showPaymentCollection by remember { mutableStateOf(false) }
    var showOtpDialog by remember { mutableStateOf(false) }
    var otpError by remember { mutableStateOf<String?>(null) }

    // Define action callbacks for the footer
    val onAcceptClick = { showAcceptDialog = true }
    val onRejectClick = { showRejectDialog = true }
    val onStatusUpdateClick = { status: String ->
        when (status) {
            "collect_payment" -> showPaymentCollection = true
            "completed" -> {
                val job = uiState.job!!
                if (job.paymentMode == "CASH" && job.paymentStatus == "DONE") {
                    // For cash payments, show OTP dialog
                    showStatusUpdateDialog = "completed"
                } else {
                    // For legacy bookings, proceed directly
                    showStatusUpdateDialog = "completed"
                }
            }
            else -> showStatusUpdateDialog = status
        }
    }
    val onOtpDialogClick = { showOtpDialog = true }
    val onJobCompleted = {
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Job completed successfully!")
            kotlinx.coroutines.delay(1500)
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Job Details") },
                    navigationIcon = {
                        androidx.compose.material3.IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Back",
                                tint = Color(0xFF8E8E93)
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
                                    contentDescription = "Refresh",
                                    tint = Color(0xFF8E8E93)
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
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
                uiState.job != null && uiState.errorMessage != null -> {
                    // Show error banner at top if job is loaded but there's an error
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)) {
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
                            onStatusUpdateClick = { status ->
                                when (status) {
                                    "collect_payment" -> showPaymentCollection = true
                                    "completed" -> {
                                        val job = uiState.job!!
                                        if (job.paymentMode == "CASH" && job.paymentStatus == "DONE") {
                                            // For cash payments, show OTP dialog
                                            showStatusUpdateDialog = "completed"
                                        } else {
                                            // For legacy bookings, proceed directly
                                            showStatusUpdateDialog = "completed"
                                        }
                                    }
                                    else -> showStatusUpdateDialog = status
                                }
                            },
                            onOtpDialogClick = { showOtpDialog = true },
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
                        onStatusUpdateClick = { status ->
                            when (status) {
                                "collect_payment" -> showPaymentCollection = true
                                "completed" -> {
                                    val job = uiState.job!!
                                    if (job.paymentMode == "CASH" && job.paymentStatus == "DONE") {
                                        // For cash payments, show OTP dialog
                                        showStatusUpdateDialog = "completed"
                                    } else {
                                        // For UPI or legacy bookings, proceed directly
                                        showStatusUpdateDialog = "completed"
                                    }
                                }
                                else -> onStatusUpdateClick(status)
                            }
                        },
                        onOtpDialogClick = { showOtpDialog = true },
                        onJobCompleted = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Job completed successfully!")
                                // Navigate back after a short delay
                                kotlinx.coroutines.delay(1500)
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
            }
        }
    }

    // Job Details Dialogs
    JobDetailsDialogs(
        uiState = uiState,
        viewModel = viewModel,
        coroutineScope = coroutineScope,
        snackbarHostState = snackbarHostState,
        showAcceptDialog = showAcceptDialog,
        showRejectDialog = showRejectDialog,
        showStatusUpdateDialog = showStatusUpdateDialog,
        showOtpDialog = showOtpDialog,
        showPaymentCollection = showPaymentCollection,
            providerId = providerId,
        otpError = otpError,
        onDismissAcceptDialog = { showAcceptDialog = false },
        onDismissRejectDialog = { showRejectDialog = false },
        onDismissStatusUpdateDialog = { showStatusUpdateDialog = null },
        onDismissOtpDialog = {
            showOtpDialog = false
            otpError = null
        },
        onDismissPaymentCollection = { showPaymentCollection = false },
        onJobAccepted = onJobAccepted,
        onJobRejected = onJobRejected,
            onPaymentCompleted = {
                showPaymentCollection = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Payment collected successfully!")
                    // Reload job details to show updated payment status
                    viewModel.loadJobDetails()
                }
        },
        onOtpError = { error -> otpError = error },
        onBack = onBack
    )

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
    onOtpDialogClick: () -> Unit = {},
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
            .padding(bottom = 100.dp), // Add bottom padding to account for bottom navigation
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Enhanced Header Section with Status, Service Name, and Price
        JobHeaderSection(job = job, viewModel = viewModel)

        // Service Information Section (merged)
        ServiceInformationSection(job = job)

        // Customer Information Section
        CustomerInformationSection(job = job)

        // Location Details Section
        LocationDetailsSection(
            job = job,
            viewModel = viewModel,
            onNavigateClick = { viewModel.navigateToLocation(context) }
        )

        // Job Timeline Section
        JobTimelineSection(job = job, viewModel = viewModel)

        // Notes Section (if available)
        if (!job.notes.isNullOrEmpty()) {
            NotesSection(notes = job.notes!!)
        }

        // Action Buttons Section (inside scrollable content)
        ActionButtonsSection(
            job = job,
                viewModel = viewModel,
            context = context,
            uiState = uiState,
            onAcceptClick = onAcceptClick,
            onRejectClick = onRejectClick,
            onStatusUpdateClick = onStatusUpdateClick,
            onOtpDialogClick = onOtpDialogClick,
            onJobCompleted = onJobCompleted
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
