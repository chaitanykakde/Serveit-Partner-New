package com.nextserve.serveitpartnernew.ui.screen.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.EmptyState

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Default.Home,
        title = stringResource(R.string.nav_home),
        description = stringResource(R.string.welcome_serveit),
        modifier = modifier
    )
}

