package com.nextserve.serveitpartnernew.ui.onboarding.step5

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
 * Fixed bottom primary button matching HTML design.
 * Full width, rounded corners, no elevation in dark mode.
 */
/**
 * Premium fixed bottom button matching HTML design.
 * Gradient background, shadow glow, arrow icon with animation.
 */
@Composable
fun FixedPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    // Press scale animation (matching HTML: active:scale-[0.98])
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )
    
    // Arrow translate animation (matching HTML: group-hover:translate-x-1)
    val arrowTranslateX by animateFloatAsState(
        targetValue = if (isHovered) 4.dp.value else 0f,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "arrowTranslate"
    )
    
    // Button gradient (matching HTML: from-primary to-[#1565C0])
    val buttonGradient = if (enabled && !isLoading) {
        Brush.verticalGradient(
            colors = listOf(
                colorScheme.primary,
                Color(0xFF1565C0) // #1565C0
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                colorScheme.surfaceVariant,
                colorScheme.surfaceVariant
            )
        )
    }
    
    // Shadow glow (matching HTML: shadow-glow)
    val shadowElevation = if (enabled && !isLoading && !isPressed) {
        12f
    } else {
        0f
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.surface.copy(alpha = 0.95f),
                        colorScheme.surface.copy(alpha = 0.98f),
                        Color.Transparent
                    )
                )
            )
            .padding(24.dp) // p-6
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .shadow(
                    elevation = shadowElevation.dp,
                    shape = RoundedCornerShape(16.dp), // rounded-2xl
                    ambientColor = colorScheme.primary.copy(alpha = 0.3f), // shadow-glow
                    spotColor = colorScheme.primary.copy(alpha = 0.3f)
                )
                .background(
                    brush = buttonGradient,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(
                    enabled = enabled && !isLoading,
                    onClick = onClick,
                    interactionSource = interactionSource,
                    indication = null
                )
                .padding(vertical = 16.dp, horizontal = 24.dp), // py-4 px-6
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                Text(
                    text = if (isLoading) "Submitting..." else text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp
                    ),
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp // tracking-wide
                )
                
                if (!isLoading) {
                    Spacer(modifier = Modifier.width(12.dp)) // gap-3
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .offset(x = arrowTranslateX.dp),
                        tint = colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

