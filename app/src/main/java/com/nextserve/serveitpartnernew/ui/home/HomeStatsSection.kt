package com.nextserve.serveitpartnernew.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.ui.components.StatsCard
import com.nextserve.serveitpartnernew.ui.components.StatsCardSkeleton

/**
 * Home Stats Section - Today's earnings and job completion summary
 */
fun LazyListScope.HomeStatsSection(
    todayJobsCompleted: Int,
    todayEarnings: Double,
    isLoading: Boolean
) {
    // LEVEL 3: Passive Summary - Today's stats
    item {
        AnimatedVisibility(
            visible = todayJobsCompleted > 0 || todayEarnings > 0 || isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (isLoading) {
                StatsCardSkeleton(modifier = Modifier.fillMaxWidth())
            } else {
                StatsCard(
                    jobsCompleted = todayJobsCompleted,
                    earnings = todayEarnings
                )
            }
        }
    }
}
