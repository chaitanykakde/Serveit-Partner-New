package com.nextserve.serveitpartnernew.ui.home

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.ui.components.HighlightedJobCard

/**
 * Home New Job Section - Shows highlighted new job for acceptance
 */
fun LazyListScope.HomeNewJobSection(
    highlightedJob: Job,
    hasOngoingJob: Boolean,
    acceptingJobId: String?,
    onShowAcceptDialog: (Job) -> Unit,
    onJobReject: (Job) -> Unit,
    onViewAllJobs: () -> Unit
) {
    item(key = "home_new_job") {
        HighlightedJobCard(
            job = highlightedJob,
            hasOngoingJob = hasOngoingJob,
            isAccepting = acceptingJobId == highlightedJob.bookingId,
            onAcceptClick = { onShowAcceptDialog(highlightedJob) },
            onRejectClick = { onJobReject(highlightedJob) },
            onViewAllClick = onViewAllJobs,
            isHero = true
        )
    }
}
