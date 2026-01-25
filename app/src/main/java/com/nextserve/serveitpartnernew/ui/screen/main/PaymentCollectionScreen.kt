package com.nextserve.serveitpartnernew.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.ui.components.OutlinedInputField
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.components.SecondaryButton
import com.nextserve.serveitpartnernew.ui.viewmodel.JobDetailsViewModel

/**
 * Payment Collection Screen for payment_pending jobs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentCollectionScreen(
    job: Job,
    providerId: String,
    onBack: () -> Unit,
    onPaymentCompleted: () -> Unit,
    viewModel: JobDetailsViewModel = viewModel(
        factory = JobDetailsViewModel.factory(
            bookingId = job.bookingId,
            customerPhoneNumber = job.customerPhoneNumber,
            providerId = providerId,
            context = LocalContext.current
        )
    )
) {
    val context = LocalContext.current
    
    // State tracking
    var amount by remember { mutableStateOf((job.paymentAmount ?: job.totalPrice).toString()) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Track payment state
    val paymentConfirmed = remember { mutableStateOf(job.paymentStatus == "DONE") }
    
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collect Payment") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isProcessing) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .padding(bottom = 80.dp) // Add bottom padding for navigation bar
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Job Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Job Summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Service:", style = MaterialTheme.typography.bodyMedium)
                        Text(job.serviceName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Customer:", style = MaterialTheme.typography.bodyMedium)
                        Text(job.userName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount:", style = MaterialTheme.typography.bodyMedium)
                        Text("₹${job.totalPrice.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Amount Input (only show if payment not confirmed)
            if (!paymentConfirmed.value) {
                OutlinedInputField(
                    value = amount,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() || it == '.' } && newValue.count { it == '.' } <= 1) {
                            amount = newValue
                        }
                    },
                    label = "Payment Amount (₹)",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Payment Method Info (only show if payment not confirmed)
            if (!paymentConfirmed.value) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Payment Method",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Cash Payment Option (only option)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Cash Payment",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Cash Payment", fontWeight = FontWeight.Medium)
                                Text(
                                    "Customer pays in cash, generate OTP for verification",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryButton(
                        text = "Cancel",
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    )

                    PrimaryButton(
                        text = "Confirm Cash Payment",
                        onClick = {
                            val amountDouble = amount.toDoubleOrNull()
                            if (amountDouble == null || amountDouble <= 0) {
                                errorMessage = "Please enter a valid amount"
                                return@PrimaryButton
                            }

                            // Handle cash payment
                            isProcessing = true
                            viewModel.processCashPayment(amountDouble) { success, error ->
                                isProcessing = false
                                if (success) {
                                    // Payment processed successfully - OTP will be shown during completion
                                    paymentConfirmed.value = true
                                    onPaymentCompleted()
                                } else {
                                    errorMessage = error ?: "Failed to process cash payment"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing && amount.isNotEmpty(),
                        isLoading = isProcessing
                    )
                }
            }

            // Payment Confirmed State (for CASH)
            if (paymentConfirmed.value) {
                // Cash payment confirmed - show completion option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Cash Payment Confirmed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "₹${job.paymentAmount?.toInt() ?: 0}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Payment received. You can now complete the job.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        PrimaryButton(
                            text = "Complete Job",
                            onClick = {
                                // OTP will be shown during completion in JobDetailsScreen
                                onPaymentCompleted()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Error Message
            errorMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

}
