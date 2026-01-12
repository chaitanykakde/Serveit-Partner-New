package com.nextserve.serveitpartnernew.ui.sections

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import kotlin.math.PI
import kotlin.math.sin
import com.nextserve.serveitpartnernew.data.model.Job
import com.nextserve.serveitpartnernew.ui.viewmodel.JobDetailsViewModel
import com.nextserve.serveitpartnernew.utils.JobStatusUtils

@Composable
fun AnimatedCircularIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = modifier.size(24.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Outer rotating ring
            drawArc(
                color = Color(0xFF007AFF),
                startAngle = rotation,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx()),
                alpha = 0.7f
            )

            // Inner pulsing circle
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension / 2) * pulse * 0.6f
            drawCircle(
                color = Color(0xFF007AFF),
                radius = radius,
                center = center,
                alpha = 0.3f
            )
        }
    }
}

@Composable
fun JobHeaderSection(job: Job, viewModel: JobDetailsViewModel) {
    val (statusColor, statusText, statusIcon) = JobStatusUtils.getStatusDisplayInfo(job.status)

    // Flat section - no card wrapper
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9F9F9)) // surface color from HTML reference
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Use animated circular indicator for all states
                AnimatedCircularIndicator()
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            // Booking ID (small, subtle)
            Text(
                text = "ID: ${job.bookingId.take(8)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93) // secondary color from HTML
            )
        }

    }
}
