package com.nextserve.serveitpartnernew.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun StepperHeader(
    currentStep: Int,
    totalSteps: Int = 5,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val stepSubtitles = listOf(
        "Basic Information",
        "Select Services",
        "Location Details",
        "Verification",
        "Review & Submit"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Stepper circles and lines
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSteps) { step ->
                val stepNumber = step + 1
                val isCompleted = stepNumber < currentStep
                val isCurrent = stepNumber == currentStep

                // Circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            color = if (isCompleted || isCurrent) {
                                colorScheme.primary
                            } else {
                                colorScheme.surface
                            }
                        )
                        .then(
                            if (!isCompleted && !isCurrent) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = colorScheme.outline,
                                    shape = CircleShape
                                )
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted || isCurrent) {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onPrimary
                        )
                    }
                }

                // Connecting line
                if (step < totalSteps - 1) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(
                                color = if (stepNumber < currentStep) {
                                    colorScheme.primary
                                } else {
                                    colorScheme.outline.copy(alpha = 0.3f)
                                }
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step label
        Text(
            text = "Step $currentStep of $totalSteps",
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle
        Text(
            text = stepSubtitles[currentStep - 1],
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant
        )
    }
}

