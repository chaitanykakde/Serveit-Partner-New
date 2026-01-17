package com.nextserve.serveitpartnernew.ui.auth.otp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium OTP header with title and subtitle (phone number).
 * Matches HTML reference typography and spacing.
 */
@Composable
fun OtpHeader(
    title: String = "Verify OTP",
    subtitle: String = "Enter the 6-digit code sent to",
    phoneNumber: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp, top = 16.dp) // mb-12 mt-4 equivalent
    ) {
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 34.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.5).sp
            ),
            color = colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp) // mb-4 equivalent
        )
        
        // Subtitle with phone number
        Text(
            text = "$subtitle\n$phoneNumber",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 0.5.sp
            ),
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

