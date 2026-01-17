package com.nextserve.serveitpartnernew.ui.auth.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.delay

/**
 * Resend OTP timer component.
 * Shows countdown timer or resend button based on cooldown state.
 */
@Composable
fun ResendOtpTimer(
    canResend: Boolean,
    cooldownSeconds: Int,
    onResendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Didn't receive code?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        if (canResend) {
            TextButton(
                onClick = onResendClick,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "Resend OTP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            val timeString = String.format("%02d:%02d", minutes, seconds)
            
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Resend OTP in ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

