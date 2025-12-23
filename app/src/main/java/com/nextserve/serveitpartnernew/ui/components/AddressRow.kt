package com.nextserve.serveitpartnernew.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddressRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.ifEmpty { "—" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                thickness = 1.dp
            )
        }
    }
}

