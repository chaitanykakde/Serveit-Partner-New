package com.nextserve.serveitpartnernew.ui.screen.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nextserve.serveitpartnernew.ui.components.EmptyState

@Composable
fun JobsScreen(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Default.List,
        title = "Jobs",
        description = "No jobs available yet. Your jobs will appear here once active.",
        modifier = modifier
    )
}
