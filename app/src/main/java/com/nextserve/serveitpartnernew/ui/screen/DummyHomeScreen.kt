package com.nextserve.serveitpartnernew.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DummyHomeScreen() {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome!",
            style = MaterialTheme.typography.displayMedium,
            color = colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "You have successfully logged in",
            style = MaterialTheme.typography.bodyLarge,
            color = colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

