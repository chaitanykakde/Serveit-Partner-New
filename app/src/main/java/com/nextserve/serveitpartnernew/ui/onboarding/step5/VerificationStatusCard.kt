package com.nextserve.serveitpartnernew.ui.onboarding.step5

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reusable status card showing verification information.
 * Matches the modern Google-grade design from HTML.
 */
@Composable
fun VerificationStatusCard(
    status: String, // "pending", "rejected", "verified"
    submittedDate: Long? = null,
    verifiedBy: String? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Status badge colors using Material 3 semantic colors
    val (badgeBg, badgeText, badgeDot) = when (status.lowercase()) {
        "pending" -> {
            // Use tertiary for warning/amber in Material 3 (pending status)
            Triple(
                colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                colorScheme.onTertiaryContainer,
                colorScheme.tertiary
            )
        }
        "rejected" -> {
            Triple(
                colorScheme.errorContainer,
                colorScheme.onErrorContainer,
                colorScheme.error
            )
        }
        "verified" -> {
            // Use primary container for success
            Triple(
                colorScheme.primaryContainer.copy(alpha = 0.3f),
                colorScheme.onPrimaryContainer,
                colorScheme.primary
            )
        }
        else -> Triple(
            colorScheme.surfaceVariant,
            colorScheme.onSurfaceVariant,
            colorScheme.primary
        )
    }
    
    // Format date
    val formattedDate = submittedDate?.let {
        try {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
        } catch (e: Exception) {
            null
        }
    }
    
    // Animated pulse for pending status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status.lowercase() == "pending") 0.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    androidx.compose.material3.Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STATUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                
                // Status badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = badgeBg,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (status.lowercase() == "pending") {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = badgeDot,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .alpha(pulseAlpha)
                        )
                    }
                    Text(
                        text = when (status.lowercase()) {
                            "pending" -> "Pending Verification"
                            "rejected" -> "Rejected"
                            "verified" -> "Verified"
                            else -> status.uppercase()
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }
            
            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        color = colorScheme.outlineVariant
                    )
            )
            
            // Submitted on row
            if (formattedDate != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Submitted on",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Verified by row (optional)
            if (verifiedBy != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            color = colorScheme.outlineVariant
                        )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Verified by",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = verifiedBy,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

