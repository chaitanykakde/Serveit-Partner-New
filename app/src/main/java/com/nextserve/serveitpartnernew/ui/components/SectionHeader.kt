package com.nextserve.serveitpartnernew.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Section Header - Consistent heading for Home screen sections
 * Matches HTML: text-[17px] font-semibold tracking-tight
 */
@Composable
fun SectionHeader(
    title: String,
    showPriorityBadge: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A), // slate-900
            fontSize = 17.sp,
            letterSpacing = (-0.3).sp // tracking-tight
        )
        // Priority badge for New Requests
        if (showPriorityBadge) {
            Box(
                modifier = Modifier
                    .background(
                        Color(0xFFEFF6FF), // blue-50
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "PRIORITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF1E40AF), // blue-700
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
