package com.nextserve.serveitpartnernew.ui.screen.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.EmptyState

@Composable
fun JobsScreen(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Default.List,
        title = stringResource(R.string.nav_jobs),
        description = stringResource(R.string.jobs_empty_message),
        modifier = modifier
    )
}
