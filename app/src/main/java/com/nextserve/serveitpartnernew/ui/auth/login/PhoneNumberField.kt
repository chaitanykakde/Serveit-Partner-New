package com.nextserve.serveitpartnernew.ui.auth.login

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor

/**
 * Premium phone number input field with glass-like container and focus animations.
 * Text NEVER disappears while typing - fixed glitch.
 */
@Composable
fun PhoneNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
    label: String = "MOBILE NUMBER"
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Focus state for animations
    var isFocused by remember { mutableStateOf(false) }
    
    // Animated values for focus effects
    val elevation by animateFloatAsState(
        targetValue = if (isFocused) 12f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "elevation"
    )
    
    val translateY by animateFloatAsState(
        targetValue = if (isFocused) -4f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "translateY"
    )
    
    // Underline pulse animation
    val underlineAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "underlineAlpha"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Column(modifier = modifier) {
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 2.4.sp, // lux tracking
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            ),
            color = if (isFocused) colorScheme.primary else colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .padding(start = 4.dp),
            maxLines = 1
        )
        
        // Input container with glass effect and animations
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = translateY.dp)
                .shadow(
                    elevation = elevation.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = if (isFocused) colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                    spotColor = if (isFocused) colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                )
        ) {
            // Glass container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp) // Tall field (py-5 equivalent)
                    .background(
                        color = if (isFocused) {
                            colorScheme.surface
                        } else {
                            // Dark theme: use surface with slight opacity, Light: surfaceVariant
                            val isDark = isSystemInDarkTheme()
                            if (isDark) {
                                colorScheme.surface.copy(alpha = 0.8f)
                            } else {
                                colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = if (isError) 1.dp else 0.dp,
                        color = if (isError) colorScheme.error else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 20.dp), // px-5 equivalent
                verticalAlignment = Alignment.CenterVertically
            ) {
                // +91 Prefix
                Row(
                    modifier = Modifier
                        .padding(end = 16.dp), // gap-3 pr-4 equivalent
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+91",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        ),
                        color = colorScheme.onSurface,
                        letterSpacing = (-0.5).sp
                    )
                    
                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )
                }
                
                // Phone input - Use BasicTextField to ensure text never disappears
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                        },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        letterSpacing = 8.sp, // tracking-widest
                        color = colorScheme.onSurface, // Explicitly ensure text is visible
                        textAlign = TextAlign.Start
                    ),
                    cursorBrush = SolidColor(colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp) // ml-4 equivalent
                        ) {
                            // Placeholder
                            if (value.isEmpty()) {
                                Text(
                                    text = "00000 00000",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 20.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                        letterSpacing = 8.sp,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
            
            // Animated gradient underline (bottom border animation)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .alpha(underlineAlpha * pulseAlpha)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colorScheme.primary,
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = Float.POSITIVE_INFINITY
                        ),
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    )
            )
        }
    }
}

