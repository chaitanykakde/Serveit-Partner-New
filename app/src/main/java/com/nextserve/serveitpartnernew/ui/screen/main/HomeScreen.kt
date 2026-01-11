package com.nextserve.serveitpartnernew.ui.screen.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.ui.home.HomeScreen as HomeScreenNew
import com.nextserve.serveitpartnernew.ui.viewmodel.HomeViewModel

/**
 * Home Screen - Delegates to refactored HomeScreen in ui.home package
 * This maintains backward compatibility while using the new modular structure
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    providerId: String,
    viewModel: HomeViewModel,
    onJobAccepted: (String) -> Unit = {},
    onViewAllJobs: () -> Unit = {},
    onOngoingJobClick: (Job) -> Unit = {},
    parentPaddingValues: PaddingValues = PaddingValues()
) {
    HomeScreenNew(
        modifier = modifier,
        providerId = providerId,
        onJobAccepted = onJobAccepted,
        onViewAllJobs = onViewAllJobs,
        onOngoingJobClick = onOngoingJobClick,
        viewModel = viewModel,
        parentPaddingValues = parentPaddingValues
    )
}
