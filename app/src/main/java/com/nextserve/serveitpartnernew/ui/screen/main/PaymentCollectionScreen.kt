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
    var selectedPaymentMode by remember { mutableStateOf<String?>(null) }
    var amount by remember { mutableStateOf(job.totalPrice.toString()) }
    var isProcessing by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showOtpDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

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

            // Amount Input (only show if payment not already done)
            if (job.paymentStatus != "DONE") {
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

            // Payment Mode Selection
            if (job.paymentStatus != "DONE") {
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
                                                serviceName = job.serviceName,
                                                providerName = job.providerName ?: "Provider",
                                                bookingId = job.bookingId,
                                                amount = amountDouble
                                            )
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
            if (selectedPaymentMode == "UPI_QR" && qrBitmap != null) {
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

                        PrimaryButton(
                            text = "Mark as Completed",
                            onClick = {
                                viewModel.markAsCompleted(
                                    onSuccess = onPaymentCompleted,
                                    onError = { error ->
                                        errorMessage = error
                                    }
                                )
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

    // OTP Verification Dialog for cash payments
    if (showOtpDialog) {
        com.nextserve.serveitpartnernew.ui.components.OtpVerificationDialog(
            onDismiss = { showOtpDialog = false },
            onVerify = { otp ->
                viewModel.verifyOtpAndComplete(otp) { success, error ->
                    if (success) {
                        showOtpDialog = false
                        onPaymentCompleted()
                    } else {
                        errorMessage = error ?: "Invalid OTP"
                    }
                }
            }
        )
    }
}
