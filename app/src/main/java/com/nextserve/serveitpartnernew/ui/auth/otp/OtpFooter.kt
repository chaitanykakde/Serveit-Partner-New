package com.nextserve.serveitpartnernew.ui.auth.otp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium OTP footer with "Need help?" link.
 * Matches HTML reference minimal design.
 */
@Composable
fun OtpFooter(
    modifier: Modifier = Modifier,
    onHelpClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp), // pb-4 equivalent
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Need help?",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Light
            ),
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.clickable(onClick = onHelpClick)
        )
    }
}

