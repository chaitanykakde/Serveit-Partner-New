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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable header with icon and status message.
 * Matches the modern Google-grade design with glow effect.
 */
@Composable
fun VerificationStatusHeader(
    status: String, // "pending", "rejected", "verified"
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon with glow effect
        Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow background (subtle for light, soft neon for dark)
            // Simple dark theme detection: check if surface color is dark
            val surfaceArgb = colorScheme.surface.toArgb()
            val isDark = (surfaceArgb and 0xFF000000.toInt()) != 0 && 
                        ((surfaceArgb shr 16) and 0xFF) + ((surfaceArgb shr 8) and 0xFF) + (surfaceArgb and 0xFF) < 384
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = if (isDark) 0.3f else 0.15f),
                                primaryColor.copy(alpha = 0.0f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .blur(24.dp)
            )
            
            // Main icon container
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        color = colorScheme.primaryContainer.copy(alpha = if (isDark) 0.2f else 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (status.lowercase()) {
                        "verified" -> Icons.Default.CheckCircle
                        "rejected" -> Icons.Default.Close
                        else -> Icons.Default.AccountCircle
                    },
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = when (status.lowercase()) {
                        "verified" -> colorScheme.primary
                        "rejected" -> colorScheme.error
                        else -> colorScheme.primary
                    }
                )
                
                // Schedule overlay for pending status
                if (status.lowercase() == "pending") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(32.dp)
                            .background(
                                color = colorScheme.surface,
                                shape = CircleShape
                            )
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = colorScheme.primary
                        )
                    }
                }
            }
        }
        
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        // Subtitle
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

