package com.nextserve.serveitpartnernew.ui.sections

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.ui.viewmodel.JobDetailsViewModel

@Composable
fun TimelineItem(
    label: String,
    timestamp: Timestamp?,
    viewModel: JobDetailsViewModel,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isPending: Boolean,
    showConnector: Boolean = true,
    duration: String? = null,
    statusLabel: String = "COMPLETED"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left side: Status indicator + connector line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Status indicator
            when {
                isCompleted -> {
                    // Filled circle with check icon - teal/green like HTML
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            drawCircle(
                                color = Color(0xFF26D0CE), // Success color from HTML
                                radius = 12.dp.toPx()
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                    }
                }
                isCurrent -> {
                    // Outlined circle - pink outline like HTML
                    Canvas(modifier = Modifier.size(24.dp)) {
                        drawCircle(
                            color = Color(0xFFFF8BA7), // On-hold color from HTML
                            radius = 11.dp.toPx(),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
                isPending -> {
                    // Small muted dot
                    Canvas(modifier = Modifier.size(8.dp)) {
                        drawCircle(
                            color = Color(0xFF8E8E93).copy(alpha = 0.4f),
                            radius = 4.dp.toPx()
                        )
                    }
                }
            }

            // Vertical connector line (dashed)
            if (showConnector) {
                Canvas(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                ) {
                    drawLine(
                        color = Color(0xFFEFEFF4),
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )
                }
            }
        }

        // Right side: Two-column layout
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left column: Step title + time info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Step title
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCompleted) Color.Black else Color(0xFF8E8E93).copy(alpha = 0.7f)
                    )

                    // Time range / schedule text
                    if (isCompleted && timestamp != null) {
                        Text(
                            text = viewModel.formatRelativeTime(timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB4B4CC), // text-muted from HTML
                            fontWeight = FontWeight.Normal
                        )
                    } else if (isPending) {
                        Text(
                            text = "Pending",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB4B4CC), // text-muted from HTML
                            fontWeight = FontWeight.Normal
                        )
                    } else if (isCurrent) {
                        Text(
                            text = "In Progress",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB4B4CC), // text-muted from HTML
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Right column: Duration + status label
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Duration (if available)
                    duration?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCompleted) Color.Black else Color(0xFFB4B4CC), // text-muted from HTML
                            textAlign = TextAlign.End
                        )
                    }

                    // Status label
                    Text(
                        text = when {
                            isCompleted -> "completed"
                            isCurrent -> "on hold"
                            else -> "pending"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB4B4CC), // text-muted from HTML
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun JobTimelineSection(job: Job, viewModel: JobDetailsViewModel) {
    // Flat section - no card wrapper
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = Color(0xFF8E8E93), // Gray color
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Timeline".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8E8E93), // secondary color
                letterSpacing = 0.5.sp
            )
        }

        // Timeline items with reduced spacing
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp) // Reduced spacing
        ) {
            val timelineItems = listOf(
                Triple("Created", job.createdAt, Icons.Default.Info),
                Triple("Accepted", job.acceptedAt, Icons.Default.CheckCircle),
                Triple("Arrived", job.arrivedAt, Icons.Default.LocationOn),
                Triple("Service Started", job.serviceStartedAt, Icons.Default.Build),
                Triple("Payment Pending", null, Icons.Default.Info),
                Triple("Completed", job.completedAt, Icons.Default.CheckCircle)
            )

            timelineItems.forEachIndexed { index, (label, timestamp, icon) ->
                val isCompleted = when (label) {
                    "Created" -> true
                    "Accepted" -> job.status != "pending"
                    "Arrived" -> job.status in listOf("arrived", "in_progress", "payment_pending", "completed")
                    "Service Started" -> job.status in listOf("in_progress", "payment_pending", "completed")
                    "Payment Pending" -> job.status in listOf("payment_pending", "completed")
                    "Completed" -> job.status == "completed"
                    else -> false
                }

                val isCurrent = when (label) {
                    "Accepted" -> job.status == "accepted"
                    "Arrived" -> job.status == "arrived"
                    "Service Started" -> job.status == "in_progress"
                    "Payment Pending" -> job.status == "payment_pending"
                    else -> false
                }

                val isPending = !isCompleted && !isCurrent

                TimelineItem(
                    label = label,
                    timestamp = timestamp,
                    viewModel = viewModel,
                    isCompleted = isCompleted,
                    isCurrent = isCurrent,
                    isPending = isPending,
                    showConnector = index < timelineItems.size - 1 // No connector on last item
                )
            }
        }
    }
}
