package com.nextserve.serveitpartnernew.ui.onboarding.step4

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileSaveButton

// Data class for document items in LazyColumn
private data class DocumentItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isUploaded: Boolean,
    val imageUrl: String,
    val onUpload: () -> Unit,
    val onReplace: () -> Unit,
    val onRemove: () -> Unit
)

@Composable
fun VerificationContent(
    // Document states
    aadhaarFrontUploaded: Boolean,
    aadhaarBackUploaded: Boolean,
    aadhaarFrontUrl: String = "",
    aadhaarBackUrl: String = "",
    profilePhotoUploaded: Boolean = false,
    profilePhotoUrl: String = "",
    isUploading: Boolean = false,
    uploadProgress: Float = 0f,
    uploadingDocumentType: String? = null,
    errorMessage: String? = null,

    // Callbacks
    onAadhaarFrontUpload: () -> Unit,
    onAadhaarBackUpload: () -> Unit,
    onAadhaarFrontReplace: () -> Unit,
    onAadhaarBackReplace: () -> Unit,
    onAadhaarFrontRemove: () -> Unit,
    onAadhaarBackRemove: () -> Unit,
    onProfilePhotoUpload: () -> Unit,
    onProfilePhotoReplace: () -> Unit,
    onProfilePhotoRemove: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,

    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    // Define document data for LazyColumn items
    val documents = listOf(
        DocumentItem(
            title = "Aadhaar Card (Front)",
            subtitle = "Clear photo of front side",
            icon = Icons.Default.Info,
            isUploaded = aadhaarFrontUploaded,
            imageUrl = aadhaarFrontUrl,
            onUpload = onAadhaarFrontUpload,
            onReplace = onAadhaarFrontReplace,
            onRemove = onAadhaarFrontRemove
        ),
        DocumentItem(
            title = "Aadhaar Card (Back)",
            subtitle = "Clear photo of back side",
            icon = Icons.Default.Person,
            isUploaded = aadhaarBackUploaded,
            imageUrl = aadhaarBackUrl,
            onUpload = onAadhaarBackUpload,
            onReplace = onAadhaarBackReplace,
            onRemove = onAadhaarBackRemove
        ),
        DocumentItem(
            title = "Profile Photo",
            subtitle = "Clear photo of your face",
            icon = Icons.Default.AccountCircle,
            isUploaded = profilePhotoUploaded,
            imageUrl = profilePhotoUrl,
            onUpload = onProfilePhotoUpload,
            onReplace = onProfilePhotoReplace,
            onRemove = onProfilePhotoRemove
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
    ) {
        // Header Section
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Verify your identity",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Upload the required documents to start receiving jobs.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        // Error message
        if (errorMessage != null) {
            item {
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
        }

        // Upload progress indicator removed - now shown in each DocumentUploadCard

        // Document Upload Cards
        items(documents.size) { index ->
            val document = documents[index]
            // Determine if this specific document is uploading
            val isThisDocumentUploading = when (index) {
                0 -> isUploading && uploadingDocumentType == "front"
                1 -> isUploading && uploadingDocumentType == "back"
                2 -> isUploading && uploadingDocumentType == "profile"
                else -> false
            }
            val thisDocumentProgress = if (isThisDocumentUploading) uploadProgress else 0f
            
            DocumentUploadCard(
                title = document.title,
                subtitle = document.subtitle,
                icon = document.icon,
                isUploaded = document.isUploaded,
                imageUrl = document.imageUrl,
                isUploading = isThisDocumentUploading,
                uploadProgress = thisDocumentProgress,
                onUpload = document.onUpload,
                onReplace = document.onReplace,
                onRemove = document.onRemove
            )
        }

        // Security message
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your documents are securely stored and used only for verification.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        // Navigation Buttons - Previous & Next side by side
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileSaveButton(
                    text = "Previous",
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f),
                    showTrailingArrow = false
                )
                ProfileSaveButton(
                    text = "Next",
                    onClick = onNext,
                    enabled = aadhaarFrontUploaded && aadhaarBackUploaded && profilePhotoUploaded && !isUploading,
                    modifier = Modifier.weight(1f),
                    showTrailingArrow = true
                )
            }
        }
    }
}
