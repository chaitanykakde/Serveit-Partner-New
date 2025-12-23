package com.nextserve.serveitpartnernew.ui.screen.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nextserve.serveitpartnernew.ui.components.EmptyState

@Composable
fun EarningsScreen(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Default.Star,
        title = "Earnings",
        description = "Your earnings will appear here once you complete your first service.",
        modifier = modifier
    )
}

