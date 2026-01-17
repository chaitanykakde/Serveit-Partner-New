package com.nextserve.serveitpartnernew.ui.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/**
 * Premium background with subtle radial gradients / blurred shapes.
 * Adapts to dark & light theme.
 */
@Composable
fun LoginBackground(
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    
    // Determine if dark mode (check if surface color is dark)
    val isDark = colorScheme.surface.toArgb().let { argb ->
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
        luminance < 0.5
    }
    
    // Color blobs (softer in dark mode) - using theme colors only
    val primaryBlobColor = if (isDark) {
        colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        colorScheme.primaryContainer.copy(alpha = 0.15f) // Light theme: subtle primary tint
    }
    
    val secondaryBlobColor = if (isDark) {
        colorScheme.secondaryContainer.copy(alpha = 0.1f)
    } else {
        colorScheme.secondaryContainer.copy(alpha = 0.12f) // Light theme: subtle secondary tint
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background) // Base background layer for proper dark theme
    ) {
        // Top-right blob (70vw equivalent)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryBlobColor,
                            Color.Transparent
                        ),
                        center = Offset(
                            x = with(density) { (1920.dp * 0.7f).toPx() },
                            y = with(density) { (-192.dp).toPx() } // -10% of 1920dp
                        ),
                        radius = with(density) { (1920.dp * 0.7f).toPx() }
                    )
                )
        )
        
        // Bottom-left blob (60vw equivalent)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondaryBlobColor,
                            Color.Transparent
                        ),
                        center = Offset(
                            x = with(density) { (-384.dp).toPx() }, // -10% of 1920dp
                            y = with(density) { (1920.dp * 0.9f).toPx() } // bottom 10%
                        ),
                        radius = with(density) { (1920.dp * 0.6f).toPx() }
                    )
                )
        )
    }
}

