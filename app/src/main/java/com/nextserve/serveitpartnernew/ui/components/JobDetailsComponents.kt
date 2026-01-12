package com.nextserve.serveitpartnernew.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.ui.viewmodel.JobDetailsViewModel
import com.nextserve.serveitpartnernew.utils.JobStatusUtils
import com.google.firebase.Timestamp

/**
 * Job Details UI Primitives - Stub implementations for refactor
 * TODO: Implement these composables based on the final design
 */

// TODO: Implement based on HTML reference design
@Composable
fun JobSummaryHeader(
    job: Job,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusText, statusIcon) = JobStatusUtils.getStatusDisplayInfo(job.status)

    // Stub implementation - replace with actual design
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF9F9F9)) // surface color from HTML
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            // Status badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            // Booking ID
            Text(
                text = "ID: ${job.bookingId.take(8)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93)
            )
        }

        // Amount
        Text(
            text = "₹${job.totalPrice.toInt()}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF007AFF)
        )
    }

    // Service name (below the header)
    Text(
        text = job.serviceName,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// TODO: Implement based on HTML reference design
@Composable
fun FlatSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    // Stub implementation - replace with actual design
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8E8E93),
            letterSpacing = 0.5.sp
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = Color(0xFFEFEFF4)
        )
    }
}

// TODO: Implement based on HTML reference design
@Composable
fun LabeledValueRow(
    label: String,
    value: String,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Stub implementation - replace with actual design
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = onClick ?: {})
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF007AFF)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

        onClick?.let {
            Icon(
                imageVector = Icons.Default.CheckCircle, // placeholder
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF8E8E93)
            )
        }
    }
}

// TODO: Implement based on HTML reference design
@Composable
fun TimelineStepRow(
    label: String,
    timestamp: Timestamp?,
    viewModel: JobDetailsViewModel,
    isCompleted: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    // Stub implementation - replace with actual design
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (isCompleted) Color(0xFF007AFF) else Color(0xFF8E8E93).copy(alpha = 0.4f)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCompleted) FontWeight.Medium else FontWeight.Normal,
                color = if (isCompleted) Color.Black else Color(0xFF8E8E93).copy(alpha = 0.6f)
            )

            if (isCompleted && timestamp != null) {
                Text(
                    text = viewModel.formatRelativeTime(timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93)
                )
            } else if (!isCompleted) {
                Text(
                    text = "Pending",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93).copy(alpha = 0.6f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun PrimaryActionFooter(
    primaryAction: @Composable () -> Unit,
    secondaryActions: List<@Composable () -> Unit> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Fixed footer implementation based on HTML reference
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.9f)) // backdrop-blur-xl effect
            .border(
                width = 0.5.dp,
                color = Color(0xFFEFEFF4) // divider color from HTML reference
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Primary action (full width)
        primaryAction()

        // Secondary actions (equal width row)
        if (secondaryActions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                secondaryActions.forEach { action ->
                    Box(modifier = Modifier.weight(1f)) {
                        action()
                    }
                }
            }
        }
    }
}
