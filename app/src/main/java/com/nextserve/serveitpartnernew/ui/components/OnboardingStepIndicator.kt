package com.nextserve.serveitpartnernew.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Simple step indicator component for onboarding flow.
 * Shows "Step X of Y" text with linear progress bar.
 */
@Composable
fun OnboardingStepIndicator(
    currentStep: Int,
    totalSteps: Int = 5,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val progress = currentStep.toFloat() / totalSteps.toFloat()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = colorScheme.primary,
            trackColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )

        // Step text
        Text(
            text = "Step $currentStep of $totalSteps",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
