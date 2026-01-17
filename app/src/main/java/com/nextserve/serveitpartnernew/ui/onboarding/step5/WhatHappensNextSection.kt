package com.nextserve.serveitpartnernew.ui.onboarding.step5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

/**
 * Data class for "What happens next?" items.
 */
data class NextStepItem(
    val icon: ImageVector,
    val title: String,
    val description: String
)

/**
 * Reusable "What happens next?" section.
 * Matches the modern Google-grade design from HTML.
 */
@Composable
fun WhatHappensNextSection(
    items: List<NextStepItem>,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "What happens next?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items.forEach { item ->
                NextStepItemRow(
                    icon = item.icon,
                    title = item.title,
                    description = item.description
                )
            }
        }
    }
}

@Composable
private fun NextStepItemRow(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = colorScheme.surfaceVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colorScheme.onSurfaceVariant
            )
        }
        
        // Text content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Default items for pending verification status.
 */
fun getDefaultPendingNextSteps(): List<NextStepItem> {
    return listOf(
        NextStepItem(
            icon = Icons.Default.CheckCircle,
            title = "Document Verification",
            description = "Our compliance team will review your uploaded identification and business documents."
        ),
        NextStepItem(
            icon = Icons.Default.Notifications,
            title = "Notification",
            description = "You will receive a push notification and email once your account has been activated."
        ),
        NextStepItem(
            icon = Icons.Default.Info,
            title = "Processing Time",
            description = "This process usually takes 24-48 business hours depending on the volume of applications."
        )
    )
}

/**
 * Default items for simple pending status (2 items).
 */
fun getSimplePendingNextSteps(): List<NextStepItem> {
    return listOf(
        NextStepItem(
            icon = Icons.Default.Notifications,
            title = "Notification",
            description = "You'll receive a notification once your profile is approved."
        ),
        NextStepItem(
            icon = Icons.Default.Edit,
            title = "Edit & Resubmit",
            description = "If any information is rejected, you can easily edit and re-submit."
        )
    )
}

