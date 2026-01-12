package com.nextserve.serveitpartnernew.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextserve.serveitpartnernew.data.model.Job

@Composable
fun CustomerInformationSection(job: Job) {
    // Flat section - no card wrapper
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Section header
        Text(
            text = "Customer".uppercase(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8E8E93), // secondary color
            letterSpacing = 0.5.sp
        )

        // Customer Name
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF8E8E93) // Gray color
            )
            Column {
                Text(
                    text = job.userName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = "Customer",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93) // secondary color
                )
            }
        }

        // Phone Number
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF8E8E93) // Gray color
            )
            Text(
                text = job.customerPhoneNumber,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )
        }

        // Email (if available)
        job.customerEmail?.let { email ->
            if (email.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF8E8E93) // Gray color
                    )
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8E8E93) // secondary color
                    )
                }
            }
        }
    }
}
