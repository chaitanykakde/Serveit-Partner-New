package com.nextserve.serveitpartnernew.ui.screen.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.EmptyState

@Composable
fun EarningsScreen(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Default.Star,
        title = stringResource(R.string.earnings_title),
        description = stringResource(R.string.earnings_empty_message),
        modifier = modifier
    )
}

