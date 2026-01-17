package com.nextserve.serveitpartnernew.ui.onboarding.step5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium Review & Submit header matching HTML design.
 * Includes progress bar, step label, and title.
 */
/**
 * Premium Review & Submit header matching HTML design.
 * NO progress bar (removed - using default OnboardingStepIndicator).
 * Only title with step label.
 */
@Composable
fun ReviewHeader(
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Title only (no progress bar - using default stepper at top)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Step label: "STEP 5 OF 5" - Increased font size
        Text(
            text = "STEP 5 OF 5",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp, // Increased from 10sp
                letterSpacing = 2.5.sp // tracking-[0.25em]
            ),
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary.copy(alpha = 0.9f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Title: "Review & Submit" - Increased font size
        Text(
            buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Light
                    )
                ) {
                    append("Review & ")
                }
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append("Submit")
                }
            },
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 32.sp, // Increased from 28sp
                lineHeight = 38.sp
            ),
            color = colorScheme.onSurface
        )
    }
}

