package com.nextserve.serveitpartnernew.ui.screen.main

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
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
import com.nextserve.serveitpartnernew.utils.QrUtils

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
    var selectedPaymentMode by remember { mutableStateOf<String?>(job.paymentMode) }
    var amount by remember { mutableStateOf((job.paymentAmount ?: job.totalPrice).toString()) }
    var isProcessing by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Track payment states
    val qrGenerated = remember { mutableStateOf(job.qrGeneratedAt != null) }
    val paymentConfirmed = remember { mutableStateOf(job.paymentStatus == "DONE") }
    
    val scrollState = rememberScrollState()
    
    // Generate QR bitmap if QR was already generated
    LaunchedEffect(job.qrUpiUri, job.paymentMode) {
        if (job.paymentMode == "UPI_QR" && job.qrUpiUri != null && qrBitmap == null) {
            // QR was generated previously, regenerate bitmap for display
            val amountValue = job.paymentAmount ?: job.totalPrice
            qrBitmap = QrUtils.generateUpiQRCode(
                customerName = job.userName,
                customerContact = job.customerPhoneNumber,
                serviceName = job.serviceName,
                providerName = job.providerName ?: "Provider",
                providerContact = job.providerMobileNo,
                bookingId = job.bookingId,
                amount = amountValue
            )
            qrGenerated.value = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collect Payment") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isProcessing) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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

            // Payment Mode Selection (only show if payment not confirmed)
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

                        // Cash Payment Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (selectedPaymentMode == "CASH")
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPaymentMode == "CASH",
                                onClick = { selectedPaymentMode = "CASH" }
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

                        // UPI QR Payment Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (selectedPaymentMode == "UPI_QR")
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPaymentMode == "UPI_QR",
                                onClick = { selectedPaymentMode = "UPI_QR" }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Online Payment (UPI QR)", fontWeight = FontWeight.Medium)
                                Text(
                                    "Generate QR code for customer to scan and pay",
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
                        text = when (selectedPaymentMode) {
                            "CASH" -> "Confirm Cash Payment"
                            "UPI_QR" -> "Generate QR Code"
                            else -> "Select Payment Method"
                        },
                        onClick = {
                            val amountDouble = amount.toDoubleOrNull()
                            if (amountDouble == null || amountDouble <= 0) {
                                errorMessage = "Please enter a valid amount"
                                return@PrimaryButton
                            }

                            when (selectedPaymentMode) {
                                "CASH" -> {
                                    // Handle cash payment
                                    isProcessing = true
                                    viewModel.processCashPayment(amountDouble) { success, error ->
                                        isProcessing = false
                                        if (success) {
                                            // Payment processed successfully - OTP will be shown during completion
                                            onPaymentCompleted()
                                        } else {
                                            errorMessage = error ?: "Failed to process cash payment"
                                        }
                                    }
                                }
                                "UPI_QR" -> {
                                    // Generate UPI QR
                                    isProcessing = true
                                    viewModel.processUpiPayment(amountDouble, null) { success, error ->
                                        isProcessing = false
                                        if (success) {
                                            // Generate QR bitmap for display
                                            qrBitmap = QrUtils.generateUpiQRCode(
                                                customerName = job.userName,
                                                customerContact = job.customerPhoneNumber,
                                                serviceName = job.serviceName,
                                                providerName = job.providerName ?: "Provider",
                                                providerContact = job.providerMobileNo,
                                                bookingId = job.bookingId,
                                                amount = amountDouble
                                            )
                                            qrGenerated.value = true
                                            // Reload job to get updated state
                                            viewModel.loadJobDetails()
                                        } else {
                                            errorMessage = error ?: "Failed to generate QR code"
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedPaymentMode != null && !isProcessing && amount.isNotEmpty(),
                        isLoading = isProcessing
                    )
                }
            }

            // QR Code Display (for UPI payments)
            if (qrGenerated.value && qrBitmap != null && selectedPaymentMode == "UPI_QR") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Scan QR Code to Pay",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "UPI QR Code",
                            modifier = Modifier.size(200.dp)
                        )

                        Text(
                            text = "Amount: ₹${amount}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Ask customer to scan this QR code and complete payment",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!paymentConfirmed.value) {
                            // Show "Confirm Payment Received" button
                            PrimaryButton(
                                text = "Confirm Payment Received",
                                onClick = {
                                    isProcessing = true
                                    viewModel.confirmPaymentReceived { success, error ->
                                        isProcessing = false
                                        if (success) {
                                            paymentConfirmed.value = true
                                            viewModel.loadJobDetails()
                                        } else {
                                            errorMessage = error ?: "Failed to confirm payment"
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                isLoading = isProcessing
                            )
                        } else {
                            // Payment confirmed, show completion button
                            PrimaryButton(
                                text = "Complete Job",
                                onClick = {
                                    // For UPI payments, complete directly
                                    // For cash payments, OTP will be handled in JobDetailsScreen
                                    viewModel.markAsCompleted(
                                        onSuccess = onPaymentCompleted,
                                        onError = { error ->
                                            errorMessage = error
                                        },
                                        onOtpRequired = {
                                            // This shouldn't happen for UPI, but handle gracefully
                                            errorMessage = "OTP verification required. Please complete from job details."
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Payment Confirmed State (for both CASH and UPI)
            if (paymentConfirmed.value && !qrGenerated.value) {
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
