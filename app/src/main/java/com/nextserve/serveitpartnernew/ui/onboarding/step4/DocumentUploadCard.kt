package com.nextserve.serveitpartnernew.ui.onboarding.step4

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun DocumentUploadCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isUploaded: Boolean,
    imageUrl: String = "",
    isUploading: Boolean = false,
    uploadProgress: Float = 0f,
    onUpload: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header row: Icon, Title/Subtitle, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(28.dp),
                        tint = colorScheme.primary
                    )
                }

                // Title and Subtitle
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                // Status Badge
                DocumentStatusBadge(
                    isUploaded = isUploaded,
                    modifier = Modifier.align(Alignment.Top)
                )
            }

            // Image preview (if uploaded) or Loading indicator
            if (isUploading) {
                // Show loading indicator with progress
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = colorScheme.primary
                        )
                        Text(
                            text = if (uploadProgress > 0f) {
                                "Uploading... ${uploadProgress.toInt()}%"
                            } else {
                                "Preparing upload..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        if (uploadProgress > 0f) {
                            LinearProgressIndicator(
                                progress = { uploadProgress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = colorScheme.primary,
                                trackColor = colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            } else if (isUploaded && imageUrl.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Document preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Action buttons
            DocumentActionRow(
                isUploaded = isUploaded,
                isUploading = isUploading,
                onUpload = onUpload,
                onReplace = onReplace,
                onRemove = onRemove
            )
        }
    }
}
