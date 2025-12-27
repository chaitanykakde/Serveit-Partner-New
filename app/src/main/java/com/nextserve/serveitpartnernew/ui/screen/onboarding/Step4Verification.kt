package com.nextserve.serveitpartnernew.ui.screen.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.BottomStickyButtonContainer
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.components.SecondaryButton
import com.nextserve.serveitpartnernew.ui.theme.CardShape

@Composable
fun Step4Verification(
    aadhaarFrontUploaded: Boolean,
    aadhaarBackUploaded: Boolean,
    isUploading: Boolean = false,
    uploadProgress: Float = 0f,
    errorMessage: String? = null,
    onAadhaarFrontUpload: (android.net.Uri) -> Unit,
    onAadhaarBackUpload: (android.net.Uri) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val scrollState = rememberScrollState()

    // Image picker for front
    val frontImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAadhaarFrontUpload(it) }
    }

    // Image picker for back
    val backImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAadhaarBackUpload(it) }
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
                    text = stringResource(R.string.next),
                    onClick = onNext,
                    enabled = aadhaarFrontUploaded && aadhaarBackUploaded && !isUploading,
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
                Text(
                    text = stringResource(R.string.upload_aadhaar_verification),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )

                // Error message
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
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

                if (isTablet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        UploadCard(
                            title = stringResource(R.string.aadhaar_front),
                            isUploaded = aadhaarFrontUploaded,
                            isUploading = isUploading,
                            onClick = { 
                                if (!isUploading && !aadhaarFrontUploaded) {
                                    frontImagePicker.launch("image/*")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        UploadCard(
                            title = stringResource(R.string.aadhaar_back),
                            isUploaded = aadhaarBackUploaded,
                            isUploading = isUploading,
                            onClick = { 
                                if (!isUploading && !aadhaarBackUploaded) {
                                    backImagePicker.launch("image/*")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    UploadCard(
                        title = stringResource(R.string.aadhaar_front),
                        isUploaded = aadhaarFrontUploaded,
                        isUploading = isUploading,
                        onClick = { 
                            if (!isUploading && !aadhaarFrontUploaded) {
                                frontImagePicker.launch("image/*")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    UploadCard(
                        title = stringResource(R.string.aadhaar_back),
                        isUploaded = aadhaarBackUploaded,
                        isUploading = isUploading,
                        onClick = { 
                            if (!isUploading && !aadhaarBackUploaded) {
                                backImagePicker.launch("image/*")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    )
}

@Composable
private fun UploadCard(
    title: String,
    isUploaded: Boolean,
    isUploading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .height(150.dp)
            .clip(CardShape)
            .background(
                color = if (isUploaded) {
                    colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    colorScheme.surface
                }
            )
            .border(
                width = 2.dp,
                color = if (isUploaded) {
                    colorScheme.primary
                } else {
                    colorScheme.outline.copy(alpha = 0.5f)
                },
                shape = CardShape
            )
            .clickable(
                enabled = !isUploading && !isUploaded,
                onClick = onClick
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isUploaded) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Uploaded",
                    modifier = Modifier.size(48.dp),
                    tint = colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.uploaded),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.primary
                )
            } else {
                if (isUploading) {
                    Text(
                        text = stringResource(R.string.uploading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.upload),
                        modifier = Modifier.size(48.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.tap_to_upload),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

