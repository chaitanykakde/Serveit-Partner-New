package com.nextserve.serveitpartnernew.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.ui.components.TodayJobCard

/**
 * Home Today Section - Today's completed jobs
 */
fun LazyListScope.HomeTodaySection(
    todayCompletedJobs: List<Job>,
    todayEarnings: Double,
    onOngoingJobClick: (Job) -> Unit
) {
    // Summary line with highlighted earnings
    item(key = "today_summary") {
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${todayCompletedJobs.size} jobs completed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Text(
                text = "•",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outlineVariant,
                fontSize = 12.sp
            )
            Text(
                text = "₹${todayEarnings.toInt()} earned",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
    
    // Today's completed jobs list with dividers (divide-y divide-slate-100)
    itemsIndexed(
        items = todayCompletedJobs,
        key = { _, job -> job.bookingId }
    ) { index, job ->
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column {
            TodayJobCard(
                job = job,
                onClick = { onOngoingJobClick(job) },
                modifier = Modifier.fillMaxWidth()
            )
                // Divider between items (divide-y divide-slate-100)
                if (index < todayCompletedJobs.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
        }
    }
}
