package com.nextserve.serveitpartnernew.ui.sections

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.ui.viewmodel.JobDetailsViewModel
import com.nextserve.serveitpartnernew.ui.viewmodel.JobDetailsUiState
import kotlinx.coroutines.launch

@Composable
fun ActionButtonsSection(
    job: Job,
    viewModel: JobDetailsViewModel,
    context: Context,
    uiState: JobDetailsUiState,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit,
    onStatusUpdateClick: (String) -> Unit,
    onOtpDialogClick: () -> Unit,
    onJobCompleted: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Primary action button
        when (job.status.lowercase()) {
            "pending" -> {
                // Accept button
                Button(
                    onClick = onAcceptClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !uiState.isAccepting,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isAccepting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("✓", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Accept", fontWeight = FontWeight.Medium)
                }
            }
            "accepted" -> {
                // Mark as Arrived
                Button(
                    onClick = { onStatusUpdateClick("arrived") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !uiState.isUpdatingStatus,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isUpdatingStatus && uiState.updatingStatusType == "arrived") {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.LocationOn, contentDescription = "Mark as Arrived", modifier = Modifier.size(20.dp), tint = Color(0xFF8E8E93))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Mark as Arrived", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
                }
            }
            "arrived" -> {
                // Start Service
                Button(
                    onClick = { onStatusUpdateClick("in_progress") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !uiState.isUpdatingStatus,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isUpdatingStatus && uiState.updatingStatusType == "in_progress") {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Build, contentDescription = "Start Service", modifier = Modifier.size(20.dp), tint = Color(0xFF8E8E93))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Start Service", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
                }
            }
            "in_progress" -> {
                // Complete Service
                Button(
                    onClick = { onStatusUpdateClick("payment_pending") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !uiState.isUpdatingStatus,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isUpdatingStatus && uiState.updatingStatusType == "payment_pending") {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Complete Service", modifier = Modifier.size(20.dp), tint = Color(0xFF8E8E93))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Complete Service", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
                }
            }
            "payment_pending" -> {
                // Complex payment logic preserved
                val qrGeneratedButNotConfirmed = job.paymentMode == "UPI_QR" &&
                    job.qrGeneratedAt != null &&
                    job.paymentStatus != "DONE"

                if (qrGeneratedButNotConfirmed) {
                    // Confirm Payment Received
                    Button(
                        onClick = { onStatusUpdateClick("collect_payment") },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                    ) {
                        Icon(Icons.Default.Build, contentDescription = "Confirm Payment", modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Confirm Payment Received", fontWeight = FontWeight.Medium)
                    }
                } else if (job.paymentStatus == "DONE") {
                    // Verify OTP & Complete
                    when (job.paymentMode) {
                        "CASH", "UPI_QR" -> {
                            Button(
                                onClick = {
                                    viewModel.markAsCompleted(
                                        onSuccess = {
                                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                                onJobCompleted()
                                            }
                                        },
                                        onError = { error ->
                                            // Error handling would be done in the viewmodel
                                        },
                                        onOtpRequired = { onOtpDialogClick() }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Verify OTP & Complete", modifier = Modifier.size(20.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Verify OTP & Complete", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        else -> {
                            // Collect Payment for legacy
                            Button(
                                onClick = { onStatusUpdateClick("collect_payment") },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                            ) {
                                Icon(Icons.Default.Build, contentDescription = "Collect Payment", modifier = Modifier.size(20.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Collect Payment", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                } else {
                    // Collect Payment
                    Button(
                        onClick = { onStatusUpdateClick("collect_payment") },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                    ) {
                        Icon(Icons.Default.Build, contentDescription = "Collect Payment", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Collect Payment", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            "completed" -> {
                // No primary action for completed jobs
            }
        }

        // Secondary action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Navigate button (if coordinates exist)
            if (job.jobCoordinates != null) {
                OutlinedButton(
                    onClick = { viewModel.navigateToLocation(context) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF007AFF)
                    )
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Navigate", modifier = Modifier.size(18.dp), tint = Color(0xFF8E8E93))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Navigate", fontWeight = FontWeight.Medium)
                }
            }

            // Voice Call button (for active jobs)
            if (job.status in listOf("accepted", "arrived", "in_progress")) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            com.nextserve.serveitpartnernew.ui.screen.call.ProviderCallActivity.createIntent(
                                context,
                                job.bookingId
                            )
                        )
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF007AFF)
                    )
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Start Voice Call", modifier = Modifier.size(18.dp), tint = Color(0xFF8E8E93))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Voice Call", fontWeight = FontWeight.Medium)
                }
            }

            // Reject button for pending jobs (secondary action)
            if (job.status.lowercase() == "pending") {
                OutlinedButton(
                    onClick = onRejectClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = !uiState.isRejecting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF3B30) // Error color
                    )
                ) {
                    Text("✕", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reject", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
