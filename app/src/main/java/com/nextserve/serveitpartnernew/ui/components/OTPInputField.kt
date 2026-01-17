package com.nextserve.serveitpartnernew.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * OTP input field with support for paste, SMS autofill, and improved backspace handling.
 * @param otpLength The length of OTP (default 6)
 * @param onOtpChange Callback when OTP changes
 * @param modifier Modifier for the component
 */
@Composable
fun OTPInputField(
    otpLength: Int = 6,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var otpText by remember { mutableStateOf("") }
    val focusRequesters = remember { List(otpLength) { FocusRequester() } }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isFocusedStates = remember { mutableStateOf(List(otpLength) { false }) }

    LaunchedEffect(otpText) {
        onOtpChange(otpText)
        // NEVER lock keyboard or block editing - allow full editability
        // Optionally hide keyboard after delay when complete, but input remains editable
        if (otpText.length == otpLength) {
            // Small delay before hiding keyboard (UX polish), but never block editing
            delay(500)
            keyboardController?.hide()
        }
    }

    // Responsive sizing: Calculate box width based on available space
    // Formula: (availableWidth - (gaps * (otpLength - 1))) / otpLength
    // Ensure boxes are at least 56dp but never exceed screen width
    BoxWithConstraints(modifier = modifier) {
        val availableWidth = maxWidth - (32.dp * 2) // Account for screen padding (32dp each side)
        val gapSize = 12.dp // Increased gap for larger boxes
        val minBoxWidth = 56.dp
        val maxBoxWidth = 68.dp // Maximum box width for larger screens
        val calculatedBoxWidth = ((availableWidth - (gapSize * (otpLength - 1))) / otpLength)
            .coerceIn(minBoxWidth, maxBoxWidth)
        
        val boxHeight = (calculatedBoxWidth * 1.15f).coerceAtMost(76.dp) // Height proportional to width
        
        Row(
            modifier = Modifier
                .wrapContentWidth(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(gapSize),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(otpLength) { index ->
            val char = if (index < otpText.length) otpText[index].toString() else ""
            val isFocused = isFocusedStates.value[index]

            val colorScheme = MaterialTheme.colorScheme
            
            // Premium styling: Glass effect with focus animations
            // Adapt to dark/light theme for proper contrast
            val backgroundColor = if (isFocused) {
                colorScheme.surface // Surface when focused
            } else {
                // Dark theme: use surface with slight opacity, Light: surfaceVariant
                val isDark = isSystemInDarkTheme()
                if (isDark) {
                    colorScheme.surface.copy(alpha = 0.8f)
                } else {
                    colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            }
            
            // Animated values for focus effects
            val elevation by animateFloatAsState(
                targetValue = if (isFocused) 12f else 0f,
                animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                label = "elevation"
            )
            
            val translateY by animateFloatAsState(
                targetValue = if (isFocused) -4f else 0f,
                animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                label = "translateY"
            )
            
            // Underline pulse animation
            val underlineAlpha by animateFloatAsState(
                targetValue = if (isFocused) 1f else 0f,
                animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                label = "underlineAlpha"
            )
            
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )

                Box(
                    modifier = Modifier
                        .width(calculatedBoxWidth) // Responsive width
                        .height(boxHeight) // Responsive height
                        .offset(y = translateY.dp)
                    .shadow(
                        elevation = elevation.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        ambientColor = if (isFocused) colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                        spotColor = if (isFocused) colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                    )
                    .background(
                        color = backgroundColor,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    .focusRequester(focusRequesters[index])
                    .onFocusChanged { focusState ->
                        val newStates = isFocusedStates.value.toMutableList()
                        newStates[index] = focusState.isFocused
                        isFocusedStates.value = newStates
                    },
                contentAlignment = Alignment.Center
            ) {
                if (char.isNotEmpty()) {
                    Text(
                        text = char,
                        style = TextStyle(
                            fontSize = 28.sp, // Increased for larger boxes
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                BasicTextField(
                    value = TextFieldValue(
                        text = "",
                        selection = TextRange(0)
                    ),
                    onValueChange = { newValue ->
                        val input = newValue.text.filter { it.isDigit() }
                        val previousLength = otpText.length
                        
                        // Handle paste: if input length matches or exceeds OTP length, take first N digits
                        if (input.length >= otpLength) {
                            val pastedOtp = input.take(otpLength)
                            otpText = pastedOtp
                            // Focus last box after paste
                            if (pastedOtp.length == otpLength) {
                                focusRequesters[otpLength - 1].requestFocus()
                                keyboardController?.hide()
                            }
                            return@BasicTextField
                        }
                        
                        // Handle backspace: detect when text length decreased (backspace pressed)
                        if (input.isEmpty() && previousLength > 0) {
                            // Backspace was pressed on empty field - delete previous digit
                            if (index > 0 && otpText.length == index) {
                                // Current box is empty - delete previous digit and move focus
                                otpText = otpText.substring(0, index - 1)
                                focusRequesters[index - 1].requestFocus()
                            } else if (otpText.length > index) {
                                // Current box has a digit - delete it
                                otpText = otpText.substring(0, index) + otpText.substring(index + 1)
                                focusRequesters[index].requestFocus()
                            }
                            return@BasicTextField
                        }
                        
                        if (input.isNotEmpty()) {
                            val digit = input.last().toString()
                            // Always allow editing - even if OTP is already 6 digits
                            if (otpText.length == index) {
                                // At correct position - append digit
                                val newOtp = otpText + digit
                                otpText = newOtp.take(otpLength) // Clamp to max length
                                if (index < otpLength - 1) {
                                    focusRequesters[index + 1].requestFocus()
                                }
                            } else if (otpText.length < index) {
                                // Fill gap if user skipped boxes
                                val newOtp = otpText + "0".repeat(index - otpText.length) + digit
                                otpText = newOtp.take(otpLength) // Clamp to max length
                                if (index < otpLength - 1) {
                                    focusRequesters[index + 1].requestFocus()
                                }
                            } else if (otpText.length > index) {
                                // Replace digit at current position (editing existing)
                                val newOtp = otpText.substring(0, index) + digit + otpText.substring(index + 1)
                                otpText = newOtp.take(otpLength) // Clamp to max length
                                if (index < otpLength - 1) {
                                    focusRequesters[index + 1].requestFocus()
                                }
                            }
                        }
                    },
                    textStyle = TextStyle(
                        fontSize = 28.sp, // Increased for larger boxes
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = androidx.compose.ui.graphics.Color.Transparent // Hide the actual text, we show it above
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = if (index == otpLength - 1) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            // Optional: hide keyboard, but don't prevent editing
                            keyboardController?.hide()
                        },
                        onNext = {
                            // Allow moving to next box even at 6 digits
                            if (index < otpLength - 1) {
                                focusRequesters[index + 1].requestFocus()
                            }
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onKeyEvent { keyEvent ->
                            // Additional backspace handling via key event (backup for better reliability)
                            if (keyEvent.key == Key.Backspace) {
                                if (otpText.length > index) {
                                    // Current box has a digit - delete it
                                    otpText = otpText.substring(0, index) + otpText.substring(index + 1)
                                    focusRequesters[index].requestFocus()
                                } else if (index > 0) {
                                    // Current box is empty - delete previous digit and move focus
                                    otpText = otpText.substring(0, index - 1)
                                    focusRequesters[index - 1].requestFocus()
                                }
                                true // Consume the event
                            } else {
                                false // Don't consume other keys
                            }
                        }
                )
                
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
                            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                        )
                )
                }
            }
        }
    }

    // Auto-focus first box
    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }
}

