package com.nextserve.serveitpartnernew.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.ui.components.TodayJobCard

/**
 * Home Today Section - Today's completed jobs
 */
fun LazyListScope.HomeTodaySection(
    todayCompletedJobs: List<Job>,
    onOngoingJobClick: (Job) -> Unit
) {
    // Today's completed jobs list
    items(
        items = todayCompletedJobs,
        key = { it.bookingId }
    ) { job ->
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TodayJobCard(
                job = job,
                onClick = { onOngoingJobClick(job) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
