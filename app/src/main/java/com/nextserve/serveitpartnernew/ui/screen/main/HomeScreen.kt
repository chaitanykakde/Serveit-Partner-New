package com.nextserve.serveitpartnernew.ui.screen.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nextserve.serveitpartnernew.ui.components.EmptyState

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Default.Home,
        title = "Home",
        description = "Welcome to Serveit Partner. Your dashboard will appear here once you start receiving service requests.",
        modifier = modifier
    )
}

