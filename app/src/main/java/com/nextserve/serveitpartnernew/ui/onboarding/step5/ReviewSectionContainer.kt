package com.nextserve.serveitpartnernew.ui.onboarding.step5

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable section container matching HTML design.
 * No card, just clean borders and minimal styling.
 */
@Composable
fun SectionContainer(
    title: String,
    onEditClick: () -> Unit,
    showBottomBorder: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp) // py-6 equivalent
        ) {
            // Section header with title and edit icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Section title - Increased font size
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp, // Increased from 11sp
                        letterSpacing = 2.sp // tracking-[0.2em]
                    ),
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                
                // Edit icon button (matching HTML: p-1.5, rounded-full, hover effect)
                Box(
                    modifier = Modifier
                        .clickable(onClick = onEditClick)
                        .padding(6.dp) // p-1.5 equivalent
                        .size(36.dp), // Touch target
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit $title",
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp)) // mb-5 equivalent
            
            // Section content
            content()
        }
        
        // Bottom border divider (matching HTML: border-gray-100/80)
        if (showBottomBorder) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colorScheme.outlineVariant.copy(alpha = 0.5f)) // border-gray-100/80 equivalent
            )
        }
    }
}

