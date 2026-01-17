package com.nextserve.serveitpartnernew.ui.onboarding.step5

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Status pill for uploaded documents.
 * Green container with check icon and uppercase "UPLOADED" text.
 */
@Composable
fun StatusPill(
    text: String = "UPLOADED",
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Emerald/green VERIFIED badge matching HTML (text-emerald-500, text-emerald-600)
    val isDark = isSystemInDarkTheme()
    val emeraldColor = if (!isDark) {
        Color(0xFF10B981) // emerald-500
    } else {
        Color(0xFF34D399) // emerald-400 (lighter for dark theme)
    }
    
    val emeraldTextColor = if (!isDark) {
        Color(0xFF059669) // emerald-600
    } else {
        Color(0xFF6EE7B7) // emerald-300 (lighter for dark theme)
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp), // gap-1.5
        verticalAlignment = Alignment.CenterVertically
    ) {
        // VERIFIED text - 10px, bold, uppercase, tracking-wider (matching HTML)
        Text(
            text = "VERIFIED",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 1.5.sp // tracking-wider
            ),
            fontWeight = FontWeight.Bold,
            color = emeraldTextColor // text-emerald-600
        )
        
        // Check icon - 18px (matching HTML)
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = emeraldColor // text-emerald-500
        )
    }
}

