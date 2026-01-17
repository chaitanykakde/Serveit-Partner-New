package com.nextserve.serveitpartnernew.ui.onboarding.step5

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
 * Reusable label-value item matching HTML design.
 * Label is uppercase, small, low emphasis.
 * Value is primary text color.
 */
@Composable
fun LabelValueItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp) // gap-1 equivalent
    ) {
        // Label - Increased font size
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp, // Increased from 10sp
                letterSpacing = 3.sp // tracking-widest equivalent
            ),
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurfaceVariant // text-gray-400
        )
        
        // Value - Increased font size
        // Only show value if not empty (for chips that need label only)
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp // Increased from 15sp
                ),
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface // text-gray-900
            )
        }
    }
}

