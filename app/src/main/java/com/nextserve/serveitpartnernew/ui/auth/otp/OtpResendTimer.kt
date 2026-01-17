package com.nextserve.serveitpartnernew.ui.auth.otp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Premium resend timer matching HTML reference.
 * Center aligned, monospaced timer digits.
 */
@Composable
fun OtpResendTimer(
    canResend: Boolean,
    cooldownSeconds: Int,
    onResendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var remainingSeconds by remember { mutableStateOf(cooldownSeconds) }
    
    // Update timer if cooldown changes
    LaunchedEffect(cooldownSeconds) {
        remainingSeconds = cooldownSeconds
    }
    
    // Countdown timer
    if (!canResend && remainingSeconds > 0) {
        LaunchedEffect(remainingSeconds) {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp), // space-y-12 equivalent
        contentAlignment = Alignment.Center
    ) {
        if (!canResend && remainingSeconds > 0) {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            val timeString = String.format("%02d:%02d", minutes, seconds)
            
            Text(
                text = "Resend code in $timeString",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.5.sp
                ),
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        } else if (canResend) {
            Text(
                text = "Resend code",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = colorScheme.primary,
                modifier = Modifier.clickable(onClick = onResendClick)
            )
        }
    }
}

