package com.nextserve.serveitpartnernew.ui.onboarding.step5

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Service chip for displaying service names.
 * Neutral surfaceVariant background, auto-wrap.
 */
@Composable
fun ServiceChip(
    text: String,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Primary service: blue-50/50 bg, primary text, blue-100/60 border
    // Skills: white bg, gray-600 text, gray-200/60 border
    val backgroundColor = if (isPrimary) {
        colorScheme.primaryContainer.copy(alpha = 0.5f) // blue-50/50
    } else {
        colorScheme.surface // white
    }
    
    val textColor = if (isPrimary) {
        colorScheme.primary // primary color
    } else {
        colorScheme.onSurfaceVariant.copy(alpha = 0.7f) // text-gray-600
    }
    
    val borderColor = if (isPrimary) {
        colorScheme.primary.copy(alpha = 0.6f) // blue-100/60
    } else {
        colorScheme.outlineVariant.copy(alpha = 0.6f) // gray-200/60
    }
    
    Box(
        modifier = modifier
            .wrapContentWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp) // rounded-lg
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp), // px-3 py-1.5
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = if (isPrimary) 14.sp else 13.sp // Category: 14px (increased), Skills: 13px (increased)
            ),
            fontWeight = FontWeight.Medium,
            color = textColor,
            maxLines = 1
        )
    }
}

