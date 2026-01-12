package com.nextserve.serveitpartnernew.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotesSection(notes: String) {
    // Flat section - no card wrapper
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF007AFF), // primary color
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Notes".uppercase(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8E8E93), // secondary color
                letterSpacing = 0.5.sp
            )
        }

        Text(
            text = notes,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
