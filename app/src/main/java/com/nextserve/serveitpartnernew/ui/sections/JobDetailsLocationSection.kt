package com.nextserve.serveitpartnernew.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.ui.viewmodel.JobDetailsViewModel

@Composable
fun LocationDetailsSection(
    job: Job,
    viewModel: JobDetailsViewModel,
    onNavigateClick: () -> Unit
) {
    // Flat section - no card wrapper
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Section header with navigate button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Location".uppercase(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8E8E93), // secondary color
                letterSpacing = 0.5.sp
            )
            if (job.jobCoordinates != null) {
                IconButton(
                    onClick = onNavigateClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Navigate",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF8E8E93) // Gray color
                    )
                }
            }
        }

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
                    tint = Color(0xFF8E8E93) // Gray color
                )
                Text(
                    text = job.locationName!!,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
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
                    tint = Color(0xFF8E8E93) // Gray color
                )
                Text(
                    text = job.customerAddress!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8E8E93), // secondary color
                    modifier = Modifier.weight(1f)
                )
            }
        } else if (job.jobCoordinates != null) {
            Text(
                text = "Coordinates: ${job.jobCoordinates.latitude}, ${job.jobCoordinates.longitude}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93) // secondary color
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
                    tint = Color(0xFF007AFF) // primary color
                )
                Text(
                    text = "Distance: ${viewModel.formatDistance(job.distance)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF007AFF) // primary color
                )
            }
        }
    }
}
