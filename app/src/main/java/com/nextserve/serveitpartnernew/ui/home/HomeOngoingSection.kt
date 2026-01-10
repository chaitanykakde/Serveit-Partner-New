package com.nextserve.serveitpartnernew.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.ui.components.OngoingJobCard

/**
 * Home Ongoing Section - All ongoing jobs
 */
fun LazyListScope.HomeOngoingSection(
    ongoingJobs: List<Job>,
    onOngoingJobClick: (Job) -> Unit
) {
    // Show all ongoing jobs
    items(
        items = ongoingJobs,
        key = { it.bookingId }
    ) { job ->
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            OngoingJobCard(
                job = job,
                onClick = { onOngoingJobClick(job) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
