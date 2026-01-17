package com.nextserve.serveitpartnernew.ui.auth.login

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
 * Premium login header with logo, title, and subtitle.
 * Matches HTML reference typography and spacing.
 */
@Composable
fun LoginHeader(
    title: String = "Welcome back",
    subtitle: String = "Please enter your details to continue",
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 48.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Logo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 64.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SERVEIT ",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 6.sp, // super-wide tracking
                    fontWeight = FontWeight.Bold
                ),
                color = colorScheme.onSurface
            )
            Text(
                text = "PARTNER",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = colorScheme.primary
            )
        }
        
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 34.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.5).sp
            ),
            color = colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Subtitle
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 0.5.sp
            ),
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

