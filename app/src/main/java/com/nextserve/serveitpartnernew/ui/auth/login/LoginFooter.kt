package com.nextserve.serveitpartnernew.ui.auth.login

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
 * Premium footer with Terms & Privacy links.
 * Matches HTML reference minimal design.
 */
@Composable
fun LoginFooter(
    modifier: Modifier = Modifier,
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp), // pb-20 equivalent (spacing from bottom)
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Terms of Service
        Text(
            text = "Terms of Service",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.5.sp, // tracking-wide
                fontWeight = FontWeight.Light
            ),
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clickable(onClick = onTermsClick)
                .padding(horizontal = 12.dp) // gap-x-6 equivalent
        )
        
        // Privacy Policy
        Text(
            text = "Privacy Policy",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Light
            ),
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clickable(onClick = onPrivacyClick)
                .padding(horizontal = 12.dp)
        )
    }
}

