package com.nextserve.serveitpartnernew.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.utils.TimeFormatUtils

/**
 * Helper function to extract local area name from full address or locationName
 * Returns first part of address (before first comma) or first 30 characters
 */
fun getLocalAreaName(address: String?, locationName: String? = null): String? {
    android.util.Log.d("JobCard", "🏠 Extracting local area from address: ${address?.take(50)}, locationName: ${locationName?.take(50)}")
    
    // Try address first, then fallback to locationName
    val source = address ?: locationName
    
    if (source.isNullOrBlank()) {
        android.util.Log.w("JobCard", "⚠️ Both address and locationName are null or blank")
        return null
    }
    // Split by comma and take first part, or take first 30 chars if no comma
    val localArea = source.split(",").firstOrNull()?.trim()?.take(30) ?: source.take(30)
    android.util.Log.d("JobCard", "✅ Extracted local area: $localArea")
    return localArea
}

/**
 * Get service icon based on service name
 * Uses available Material Icons for better visual distinction
 */
@Composable
private fun getServiceIcon(serviceName: String): ImageVector {
    return when (serviceName.lowercase()) {
        "electrical", "electrical service", "electrician" -> Icons.Default.Settings // Electrical/technical work
        "plumbing", "plumbing service", "plumber" -> Icons.Default.Build // Tools/repair work
        "carpentry", "carpentry service", "carpenter" -> Icons.Default.Build // Construction/tools
        "cleaning", "cleaning service", "house cleaning" -> Icons.Default.Home // Home/cleaning
        "painting", "painting service", "painter" -> Icons.Default.Build // Construction work
        "ac repair", "ac service", "air conditioning" -> Icons.Default.Settings // Technical/AC
        "appliance repair", "appliance service" -> Icons.Default.Settings // Technical repair
        else -> Icons.Default.Build // Default to Build for any service
    }
}

/**
 * Ongoing Job Hero Card - Primary action, more prominent than regular ongoing jobs
 */
@Composable
fun OngoingJobHeroCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Service icon - larger for hero
                Icon(
                    imageVector = getServiceIcon(job.serviceName),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )

                Column {
                    Text(
                        text = job.serviceName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = job.userName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Status badge and distance metadata row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    // Status badge
                    StatusChip(status = job.status)
                        // Distance
                        job.distance?.let { distance ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF757575),
                                        modifier = Modifier.size(14.dp)
                                    )
                                Text(
                                    text = "${String.format("%.1f", distance)} km",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Action arrow
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Highlighted New Job Card - Material 3 design matching screenshot
 */
@Composable
fun HighlightedJobCard(
    job: Job,
    hasOngoingJob: Boolean,
    isAccepting: Boolean,
    onAcceptClick: (Job) -> Unit,
    onRejectClick: (Job) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHero: Boolean = false // New parameter to make it hero-style when primary
) {
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                color = Color(0xFFF1F5F9), // slate-100
                shape = RoundedCornerShape(16.dp)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Service name and amount row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.serviceName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A) // slate-900
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = job.userName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B), // slate-500
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    // Local area name (first part of address)
                    android.util.Log.d("HighlightedJobCard", "🏠 Checking address for job ${job.bookingId}: address=${job.customerAddress?.take(50)}, locationName=${job.locationName?.take(50)}")
                    getLocalAreaName(job.customerAddress, job.locationName)?.let { localArea ->
                        android.util.Log.d("HighlightedJobCard", "✅ Displaying local area: $localArea")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = localArea,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8), // slate-400
                            fontSize = 12.sp
                        )
                    } ?: run {
                        android.util.Log.w("HighlightedJobCard", "⚠️ No local area to display for job ${job.bookingId}")
                    }
                }
                // Amount on the right
                Text(
                    text = "₹${job.totalPrice.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A), // slate-900
                    fontSize = 20.sp
                )
            }

            // Distance and time metadata row - Combined format
            val hasDistance = job.distance != null && job.distance!! > 0
            // For NEW REQUEST: Use createdAt timestamp, format as "Today · 6–7 PM"
            val timeDisplay = TimeFormatUtils.formatNewRequestTime(job.createdAt)
            val hasTime = !timeDisplay.isNullOrBlank()
            
            if (hasDistance || hasTime) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Distance
                    if (hasDistance) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8), // slate-400
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${String.format("%.1f", job.distance!!)} km",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B), // slate-500
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    // Separator dot
                    if (hasDistance && hasTime) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B) // slate-500
                        )
                    }
                    // Time display for new requests
                    if (hasTime) {
                        Text(
                            text = timeDisplay ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B), // slate-500
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Urgency helper text - amber-50 background, amber-800 text
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFFFFFBEB), // amber-50
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color(0xFFD97706), // amber-600
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Respond quickly to secure this job",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E), // amber-800
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }

            // Action buttons - Using ProfileSaveButton style
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Accept button - Primary blue #0056d2 with shadow
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(
                            color = if (!hasOngoingJob && !isAccepting) Color(0xFF0056D2) else Color(0xFF0056D2).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = !hasOngoingJob && !isAccepting, onClick = { onAcceptClick(job) }),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAccepting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Accepting...",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White
                            )
                        }
                    } else {
                        Text(
                            text = "Accept Job",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White
                        )
                    }
                }

                // Reject button - slate-100 background, slate-600 text
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(
                            color = Color(0xFFF1F5F9), // slate-100
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(onClick = { onRejectClick(job) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Reject",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF475569) // slate-600
                    )
                }
            }
        }
    }
}

/**
 * Ongoing Job Card - Material 3 design (secondary level)
 */
@Composable
fun OngoingJobCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = Color(0xFFF1F5F9), // slate-100
                shape = RoundedCornerShape(16.dp)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Service icon in slate-50 background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color(0xFFF8FAFC), // slate-50
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getServiceIcon(job.serviceName),
                        contentDescription = null,
                        tint = Color(0xFF94A3B8), // slate-400
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = job.serviceName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A), // slate-900
                            fontSize = 16.sp
                        )
                        Text(
                            text = "• ${job.userName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8), // slate-400
                            fontSize = 12.sp
                        )
                    }
                    // Local area name (first part of address)
                    android.util.Log.d("OngoingJobCard", "🏠 Checking address for job ${job.bookingId}: address=${job.customerAddress?.take(50)}, locationName=${job.locationName?.take(50)}")
                    getLocalAreaName(job.customerAddress, job.locationName)?.let { localArea ->
                        android.util.Log.d("OngoingJobCard", "✅ Displaying local area: $localArea")
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = localArea,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8), // slate-400
                            fontSize = 11.sp
                        )
                    } ?: run {
                        android.util.Log.w("OngoingJobCard", "⚠️ No local area to display for job ${job.bookingId}")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Distance and status combined metadata row
                    android.util.Log.d("OngoingJobCard", "🔍 Job ${job.bookingId}: distance=${job.distance}")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Distance - Show if available
                        job.distance?.let { distance ->
                            android.util.Log.d("OngoingJobCard", "📏 Distance found: $distance km")
                            if (distance > 0) {
                                Text(
                                    text = "${String.format("%.1f", distance)} km",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B), // slate-500
                                    fontSize = 12.sp
                                )
                                // Separator dot
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B), // slate-500
                                    fontSize = 12.sp
                                )
                            } else {
                                android.util.Log.w("OngoingJobCard", "⚠️ Distance is 0 or negative: $distance")
                            }
                        } ?: run {
                            android.util.Log.w("OngoingJobCard", "⚠️ No distance available for job ${job.bookingId}")
                        }
                        // Status text
                        Text(
                            text = when (job.status.lowercase()) {
                                "accepted" -> "Accepted"
                                "arrived" -> "Arrived"
                                "in_progress" -> "In Progress"
                                "payment_pending" -> "Payment Pending"
                                else -> job.status
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B), // slate-500
                            fontSize = 12.sp
                        )
                        // Status badge - emerald for in_progress
                        if (job.status.lowercase() == "in_progress") {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color(0xFFECFDF5), // emerald-50
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "IN PROGRESS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF059669), // emerald-600
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // View details arrow - chevron_right style
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View details",
                tint = Color(0xFFCBD5E1), // slate-300
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Today Job Card - Material 3 design for completed jobs (tertiary level)
 * Matches HTML structure: no card wrapper, divider-based layout
 */
@Composable
fun TodayJobCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Service icon in rounded-full slate-50 background (40dp)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color(0xFFF8FAFC), // slate-50
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getServiceIcon(job.serviceName),
                        contentDescription = null,
                        tint = Color(0xFF94A3B8), // slate-400
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = job.serviceName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A), // slate-900
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = job.userName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B), // slate-500
                        fontSize = 13.sp
                    )
                }
            }

            // Completed indicator - emerald-500 checkmark + text if payment done
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = Color(0xFF10B981), // emerald-500
                    modifier = Modifier.size(18.dp)
                )
                // Show "COMPLETED" text only if payment is done
                if (job.paymentStatus?.uppercase() == "DONE") {
                    Text(
                        text = "COMPLETED",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF10B981), // emerald-500
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Distance and payment mode metadata - Below, indented (ml-[52px] in HTML)
        val hasDistance = job.distance != null && job.distance!! > 0
        val hasPaymentMode = job.paymentMode != null
        
        if (hasDistance || hasPaymentMode) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.padding(start = 52.dp), // Align with content below icon
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distance
                if (hasDistance) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8), // slate-400
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${String.format("%.1f", job.distance!!)} km",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8), // slate-400
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                // Separator dot (slate-200)
                if (hasDistance && hasPaymentMode) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(Color(0xFFE2E8F0), CircleShape) // slate-200
                    )
                }
                // Payment mode
                job.paymentMode?.let { paymentMode ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Using LocationOn as placeholder - payments icon would need different import
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8), // slate-400
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = when (paymentMode.uppercase()) {
                                "CASH" -> "Cash"
                                "UPI_QR" -> "UPI"
                                else -> paymentMode
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8), // slate-400
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
