package com.nextserve.serveitpartnernew.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.ui.components.EmptyState
import com.nextserve.serveitpartnernew.ui.components.HighlightedJobCard
import com.nextserve.serveitpartnernew.ui.components.JobCardSkeleton
import com.nextserve.serveitpartnernew.ui.components.OngoingJobHeroCard
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home

/**
 * Home Primary Section - Hero job display (ongoing or highlighted new job)
 */
fun LazyListScope.HomePrimarySection(
    ongoingJobs: List<Job>,
    highlightedJob: Job?,
    hasOngoingJob: Boolean,
    acceptingJobId: String?,
    isLoading: Boolean,
    onJobAccepted: (String) -> Unit,
    onViewAllJobs: () -> Unit,
    onOngoingJobClick: (Job) -> Unit,
    onJobReject: (Job) -> Unit
) {
    android.util.Log.d("HomePrimarySection", "🎯 HomePrimarySection called - ongoingJobs: ${ongoingJobs.size}, highlightedJob: ${highlightedJob != null}, isLoading: $isLoading, hasOngoingJob: $hasOngoingJob")

    // LEVEL 1: Primary Action (Hero) - Loading / Empty / Content in single slot
    item(key = "home_primary") {
        when {
            // LOADING STATE: Show skeleton only during loading
            isLoading -> {
                android.util.Log.d("HomePrimarySection", "🏗️ Rendering LOADING state - JobCardSkeleton")
                JobCardSkeleton()
            }
            // CONTENT STATE: Show jobs when available
            ongoingJobs.isNotEmpty() -> {
                android.util.Log.d("HomePrimarySection", "📋 Rendering CONTENT state - OngoingJobHeroCard")
                // Primary: First ongoing job as hero
                OngoingJobHeroCard(
                    job = ongoingJobs.first(),
                    onClick = { onOngoingJobClick(ongoingJobs.first()) }
                )
            }
            highlightedJob != null -> {
                android.util.Log.d("HomePrimarySection", "⭐ Rendering CONTENT state - HighlightedJobCard (hero)")
                // Primary: Highlighted new job (hero style when no ongoing jobs)
                HighlightedJobCard(
                    job = highlightedJob,
                    hasOngoingJob = hasOngoingJob,
                    isAccepting = acceptingJobId == highlightedJob.bookingId,
                    onAcceptClick = { onJobAccepted(highlightedJob.bookingId) },
                    onRejectClick = { onJobReject(highlightedJob) },
                    onViewAllClick = onViewAllJobs,
                    isHero = true // Make it hero-style when it's the primary action
                )
            }
            // EMPTY STATE: Show calm empty message when no jobs and not loading
            else -> {
                android.util.Log.d("HomePrimarySection", "📭 Rendering EMPTY state - EmptyState")
                EmptyState(
                    icon = Icons.Default.Home,
                    title = "No jobs right now",
                    description = "You're all set. New jobs will appear here when available.",
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        }
    }
}
