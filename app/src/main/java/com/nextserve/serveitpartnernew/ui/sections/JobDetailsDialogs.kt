package com.nextserve.serveitpartnernew.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.ui.viewmodel.JobDetailsUiState
import com.nextserve.serveitpartnernew.ui.viewmodel.JobDetailsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun JobDetailsDialogs(
    uiState: JobDetailsUiState,
    viewModel: JobDetailsViewModel,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    showAcceptDialog: Boolean,
    showRejectDialog: Boolean,
    showStatusUpdateDialog: String?,
    showOtpDialog: Boolean,
    showPaymentCollection: Boolean,
    providerId: String,
    otpError: String?,
    onDismissAcceptDialog: () -> Unit,
    onDismissRejectDialog: () -> Unit,
    onDismissStatusUpdateDialog: () -> Unit,
    onDismissOtpDialog: () -> Unit,
    onDismissPaymentCollection: () -> Unit,
    onJobAccepted: () -> Unit,
    onJobRejected: () -> Unit,
    onPaymentCompleted: () -> Unit,
    onOtpError: (String?) -> Unit,
    onBack: () -> Unit
) {
    // Accept job dialog
    if (showAcceptDialog) {
        AlertDialog(
            onDismissRequest = onDismissAcceptDialog,
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
                                onDismissAcceptDialog()
                                onJobAccepted()
                            },
                            onError = { error ->
                                onDismissAcceptDialog()
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
                androidx.compose.material3.TextButton(onClick = onDismissAcceptDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reject job dialog
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = onDismissRejectDialog,
            title = { Text("Reject Job?") },
            text = {
                Text("Are you sure you want to reject this job? You won't be able to accept it later.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectJob {
                            onDismissRejectDialog()
                            onJobRejected()
                        }
                    },
                    enabled = !uiState.isRejecting
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = onDismissRejectDialog) {
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
            "completed" -> {
                val job = uiState.job!!
                if (job.paymentMode == "CASH" && job.paymentStatus == "DONE") {
                    // Cash payment - show OTP verification dialog instead
                    Triple(
                        "Verify Payment",
                        "Enter the 6-digit OTP provided by the customer to complete the job.",
                        "Verify & Complete"
                    )
                } else {
                    Triple(
                        "Mark as Completed?",
                        "Confirm that the job is fully completed and payment has been received.",
                        "Mark Completed"
                    )
                }
            }
            else -> Triple("Confirm Action?", "Are you sure?", "Confirm")
        }

        AlertDialog(
            onDismissRequest = onDismissStatusUpdateDialog,
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
                                                onDismissStatusUpdateDialog()
                                            }
                                        },
                                        onError = { error ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(error)
                                                onDismissStatusUpdateDialog()
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
                                                onDismissStatusUpdateDialog()
                                            }
                                        },
                                        onError = { error ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(error)
                                                onDismissStatusUpdateDialog()
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
                                                onDismissStatusUpdateDialog()
                                            }
                                        },
                                        onError = { error ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(error)
                                                onDismissStatusUpdateDialog()
                                            }
                                        }
                                    )
                                }
                            }
                            "completed" -> {
                                val job = uiState.job!!
                                if (job.paymentMode == "CASH" && job.paymentStatus == "DONE") {
                                    // For cash payments, trigger OTP verification via ViewModel
                                    {
                                        viewModel.markAsCompleted(
                                            onSuccess = {
                                                onDismissStatusUpdateDialog()
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Job completed successfully!")
                                                    kotlinx.coroutines.delay(1500)
                                                    onBack()
                                                }
                                            },
                                            onError = { error ->
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(error)
                                                    onDismissStatusUpdateDialog()
                                                }
                                            },
                                            onOtpRequired = {
                                                // Show OTP dialog for cash payments
                                                onDismissStatusUpdateDialog()
                                                // showOtpDialog = true - handled by callback
                                            }
                                        )
                                    }
                                } else {
                                    // For legacy bookings, proceed directly
                                    {
                                        viewModel.markAsCompleted(
                                            onSuccess = {
                                                onDismissStatusUpdateDialog()
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Job completed successfully!")
                                                    kotlinx.coroutines.delay(1500)
                                                    onBack()
                                                }
                                            },
                                            onError = { error ->
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(error)
                                                    onDismissStatusUpdateDialog()
                                                }
                                            },
                                            onOtpRequired = {
                                                // Show OTP dialog for cash payments
                                                onDismissStatusUpdateDialog()
                                                // showOtpDialog = true - handled by callback
                                            }
                                        )
                                    }
                                }
                            }
                            else -> { onDismissStatusUpdateDialog }
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
                androidx.compose.material3.TextButton(onClick = onDismissStatusUpdateDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    // Payment Collection Screen
    if (showPaymentCollection && uiState.job != null) {
        com.nextserve.serveitpartnernew.ui.screen.main.PaymentCollectionScreen(
            job = uiState.job!!,
            providerId = providerId,
            onBack = onDismissPaymentCollection,
            onPaymentCompleted = onPaymentCompleted
        )
    }

    // OTP Verification Dialog for cash payments
    // Clear error when dialog is shown
    LaunchedEffect(showOtpDialog) {
        if (showOtpDialog) {
            onOtpError(null)
        }
    }

    if (showOtpDialog) {
        com.nextserve.serveitpartnernew.ui.components.OtpVerificationDialog(
            onDismiss = {
                onDismissOtpDialog()
                onOtpError(null) // Clear error when dismissed
            },
            onVerify = { otp ->
                onOtpError(null) // Clear previous error
                viewModel.verifyOtpAndComplete(otp) { success, error ->
                    if (success) {
                        onDismissOtpDialog()
                        onOtpError(null)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Job completed successfully!")
                            kotlinx.coroutines.delay(1500)
                            onBack()
                        }
                    } else {
                        onOtpError(error ?: "Invalid OTP. Please check with customer.")
                    }
                }
            },
            errorMessage = otpError,
            isVerifying = uiState.isUpdatingStatus && uiState.updatingStatusType == "completed"
        )
    }
}
