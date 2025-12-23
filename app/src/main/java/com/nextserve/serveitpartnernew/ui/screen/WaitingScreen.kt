package com.nextserve.serveitpartnernew.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun WaitingScreen(
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth >= 600.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (isTablet) 600.dp else screenWidth)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Waiting",
                modifier = Modifier.size(80.dp),
                tint = colorScheme.primary
            )

            Spacer(modifier = Modifier.padding(24.dp))

            Text(
                text = "Under Verification",
                style = MaterialTheme.typography.headlineMedium,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.padding(16.dp))

            Text(
                text = "Your profile is being reviewed by our team. We'll notify you once the verification is complete.",
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

