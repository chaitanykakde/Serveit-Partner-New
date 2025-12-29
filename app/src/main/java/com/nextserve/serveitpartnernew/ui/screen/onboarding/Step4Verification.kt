package com.nextserve.serveitpartnernew.ui.screen.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.BottomStickyButtonContainer
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.components.SecondaryButton
import com.nextserve.serveitpartnernew.ui.theme.CardShape

@Composable
fun Step4Verification(
    aadhaarFrontUploaded: Boolean,
    aadhaarBackUploaded: Boolean,
    aadhaarFrontUrl: String = "",
    aadhaarBackUrl: String = "",
    profilePhotoUploaded: Boolean = false,
    profilePhotoUrl: String = "",
    isUploading: Boolean = false,
    uploadProgress: Float = 0f,
    errorMessage: String? = null,
    onAadhaarFrontUpload: (Uri) -> Unit,
    onAadhaarBackUpload: (Uri) -> Unit,
    onProfilePhotoUpload: (Uri) -> Unit,
    onAadhaarFrontDelete: () -> Unit = {},
    onAadhaarBackDelete: () -> Unit = {},
    onProfilePhotoDelete: () -> Unit = {},
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    // Image pickers
    val frontImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAadhaarFrontUpload(it) }
    }

    val backImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAadhaarBackUpload(it) }
    }

    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onProfilePhotoUpload(it) }
    }

    BottomStickyButtonContainer(
        button = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryButton(
                    text = stringResource(R.string.previous),
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = "${stringResource(R.string.next)} →",
                    onClick = onNext,
                    enabled = aadhaarFrontUploaded && aadhaarBackUploaded && profilePhotoUploaded && !isUploading,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        content = {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Header Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Upload documents for verification",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Please provide the required documents to verify your identity and start receiving jobs on Servelt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                // Error message
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Upload progress
                if (isUploading && uploadProgress > 0f) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = uploadProgress / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.uploading_percent, uploadProgress.toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Document Cards
                DocumentCard(
                    title = "Aadhaar Card",
                    subtitle = "Front and Back",
                    icon = Icons.Default.Info,
                    isUploaded = aadhaarFrontUploaded,
                    imageUrl = aadhaarFrontUrl,
                    isUploading = isUploading,
                    onUploadClick = {
                        if (!isUploading) {
                            frontImagePicker.launch("image/*")
                        }
                    },
                    onViewClick = {
                        // Show preview for front
                    },
                    onDelete = if (aadhaarFrontUploaded) {
                        { onAadhaarFrontDelete() }
                    } else null
                )

                DocumentCard(
                    title = "Aadhaar Card",
                    subtitle = "Back",
                    icon = Icons.Default.Info,
                    isUploaded = aadhaarBackUploaded,
                    imageUrl = aadhaarBackUrl,
                    isUploading = isUploading,
                    onUploadClick = {
                        if (!isUploading) {
                            backImagePicker.launch("image/*")
                        }
                    },
                    onViewClick = {
                        // Show preview for back
                    },
                    onDelete = if (aadhaarBackUploaded) {
                        { onAadhaarBackDelete() }
                    } else null
                )

                DocumentCard(
                    title = "Profile Photo",
                    subtitle = "Clear photo of your face",
                    icon = Icons.Default.AccountCircle,
                    isUploaded = profilePhotoUploaded,
                    imageUrl = profilePhotoUrl,
                    isUploading = isUploading,
                    onUploadClick = {
                        if (!isUploading) {
                            profilePhotoPicker.launch("image/*")
                        }
                    },
                    onViewClick = {
                        // Show preview
                    },
                    onDelete = if (profilePhotoUploaded) {
                        { onProfilePhotoDelete() }
                    } else null
                )

                // Security text
                Text(
                    text = "Your documents are securely stored and used only for verification purposes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    )
}

@Composable
private fun DocumentCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isUploaded: Boolean,
    imageUrl: String = "",
    isUploading: Boolean = false,
    onUploadClick: () -> Unit,
    onViewClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    isDisabled: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFullScreenPreview by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Main row: Icon, Title/Subtitle, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(48.dp),
                    tint = if (isDisabled) colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else colorScheme.primary
                )

                // Title and Subtitle
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDisabled) colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                // Status Badge
                StatusBadge(isUploaded = isUploaded, isDisabled = isDisabled)
            }

            // Action Button
            if (!isDisabled) {
                TextButton(
                    onClick = {
                        if (isUploaded && onViewClick != null) {
                            if (imageUrl.isNotEmpty()) {
                                showFullScreenPreview = true
                            } else {
                                onViewClick()
                            }
                        } else if (!isUploading) {
                            onUploadClick()
                        }
                    },
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isUploaded) "View >" else "Upload ↑",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.primary
                    )
                }
            } else {
                TextButton(
                    onClick = { /* No action */ },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Upload ↑",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            // Image preview (if uploaded)
            if (isUploaded && imageUrl.isNotEmpty() && !isDisabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.surfaceVariant)
                        .clickable { showFullScreenPreview = true }
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Delete button overlay
                    if (onDelete != null) {
                        androidx.compose.material3.IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete Image?",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this image? You'll need to upload it again.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Full screen image preview dialog
    if (showFullScreenPreview && imageUrl.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showFullScreenPreview = false },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Full screen preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFullScreenPreview = false }
                ) {
                    Text("Close")
                }
            },
            containerColor = colorScheme.surface
        )
    }
}

@Composable
private fun StatusBadge(
    isUploaded: Boolean,
    isDisabled: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val (icon, text, color) = if (isDisabled || !isUploaded) {
        Triple(
            Icons.Default.Warning,
            "Pending",
            Color(0xFFFF9800) // Orange
        )
    } else {
        Triple(
            Icons.Default.CheckCircle,
            "Uploaded",
            Color(0xFF4CAF50) // Green
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
