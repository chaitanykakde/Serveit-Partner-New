package com.nextserve.serveitpartnernew.ui.onboarding.step4

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DocumentStatusBadge(
    isUploaded: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    val (backgroundColor, textColor, text) = if (isUploaded) {
        Triple(
            colorScheme.primary.copy(alpha = 0.1f),
            colorScheme.primary,
            "Uploaded"
        )
    } else {
        Triple(
            colorScheme.onSurface.copy(alpha = 0.1f),
            colorScheme.onSurfaceVariant,
            "Not uploaded"
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
