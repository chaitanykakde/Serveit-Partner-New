package com.nextserve.serveitpartnernew.ui.auth.login

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium gradient CTA button with arrow icon and press animations.
 * Matches HTML reference design.
 */
@Composable
fun PrimaryCtaButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Press animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    // Button colors - gradient using theme colors only
    // In dark theme, ensure primary is visible and vibrant
    val isDark = isSystemInDarkTheme()
    val buttonGradient = if (enabled) {
        if (isDark) {
            // Dark theme: use primary color with slight variation for gradient
            Brush.verticalGradient(
                colors = listOf(
                    colorScheme.primary.copy(alpha = 0.9f),
                    colorScheme.primary
                )
            )
        } else {
            // Light theme: slight darker for gradient effect
            Brush.verticalGradient(
                colors = listOf(
                    colorScheme.primary.copy(alpha = 0.85f),
                    colorScheme.primary
                )
            )
        }
    } else {
        Brush.verticalGradient(
            colors = listOf(
                colorScheme.surfaceVariant,
                colorScheme.surfaceVariant
            )
        )
    }
    
    // Shadow elevation
    val shadowElevation = if (enabled && !isPressed) 15f else 0f
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp) // pt-4 equivalent
            .scale(scale)
            .shadow(
                elevation = shadowElevation.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = colorScheme.primary.copy(alpha = 0.4f),
                spotColor = colorScheme.primary.copy(alpha = 0.4f)
            )
            .height(56.dp) // py-4 equivalent
            .background(
                brush = buttonGradient,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                enabled = enabled && !isLoading,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp), // px-6 equivalent
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Button text
            Text(
                text = if (isLoading) "Sending..." else text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = if (enabled) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp) // pl-2 equivalent
            )
            
            // Arrow icon in circle - using theme colors
            Box(
                modifier = Modifier
                    .size(32.dp) // w-8 h-8
                    .background(
                        color = if (enabled) colorScheme.onPrimary.copy(alpha = 0.2f) else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (enabled) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

