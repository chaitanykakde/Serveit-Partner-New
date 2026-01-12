package com.nextserve.serveitpartnernew.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
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
fun SubServiceItem(
    name: String,
    description: String?,
    price: Double,
    unit: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "₹${price.toInt()}${if (unit.isNotEmpty()) " / $unit" else ""}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ServiceInformationSection(job: Job) {
    // Flat section - no card wrapper
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = Color(0xFF8E8E93), // Gray color
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Service Information".uppercase(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8E8E93), // secondary color
                letterSpacing = 0.5.sp
            )
        }

        // Service Type
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Service Type",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8E8E93) // secondary color
            )
            Text(
                text = job.serviceName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }

        // Sub-services (if they exist) - same logic as SubServicesCard
        job.subServicesSelected?.let { subServices ->
            if (subServices.isNotEmpty()) {
                // Divider before sub-services
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color(0xFFEFEFF4)
                )

                // Sub-services list
                subServices.forEach { (serviceName, serviceData) ->
                    val serviceMap = serviceData as? Map<*, *>
                    val name = serviceMap?.get("name") as? String ?: serviceName
                    val description = serviceMap?.get("description") as? String
                    val price = (serviceMap?.get("price") as? Number)?.toDouble() ?: 0.0
                    val unit = serviceMap?.get("unit") as? String ?: ""

                    SubServiceItem(
                        name = name,
                        description = description,
                        price = price,
                        unit = unit
                    )
                    if (serviceName != subServices.keys.last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                // Total section - same as SubServicesCard
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "₹${job.totalPrice.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF007AFF) // primary color
                    )
                }
            }
        }
    }
}
