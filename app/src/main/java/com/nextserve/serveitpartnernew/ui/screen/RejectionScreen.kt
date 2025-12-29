package com.nextserve.serveitpartnernew.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.repository.FirestoreRepository
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton

@Composable
fun RejectionScreen(
    uid: String,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rejectionReason by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(uid) {
        val firestoreRepository = FirestoreRepository(FirebaseProvider.firestore)
        val result = firestoreRepository.getProviderData(uid)
        result.onSuccess { providerData ->
            rejectionReason = providerData?.rejectionReason
            isLoading = false
        }.onFailure {
            isLoading = false
        }
    }
    val colorScheme = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth >= 600.dp
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.surface)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .widthIn(max = if (isTablet) 600.dp else screenWidth)
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                // Attention Icon with Halo
                AttentionIcon()

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = "Profile needs attention",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = "Some information in your profile could not be verified. Please review the details below and update your profile.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Warning Box
                WarningBox(
                    rejectionReason = rejectionReason,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Fix & Resubmit Button
                PrimaryButton(
                    text = "Fix & Resubmit Profile",
                    onClick = onEditProfile,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Help Text
                Text(
                    text = "Need help? Contact support if you're unsure how to fix this.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AttentionIcon(
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val errorColor = colorScheme.error
    val haloColor = errorColor.copy(alpha = 0.25f)
    
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer halo circle (translucent)
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(haloColor)
        )
        
        // Inner solid circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(errorColor),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Attention",
                modifier = Modifier.size(40.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun WarningBox(
    rejectionReason: String?,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val errorColor = colorScheme.error
    val darkRed = Color(0xFFB71C1C)
    val lightRed = errorColor.copy(alpha = 0.1f)
    
    // Parse rejection reason into bullet points
    val issues = parseRejectionReason(rejectionReason)
    
    Card(
        modifier = modifier
            .border(
                width = 2.dp,
                color = errorColor,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = lightRed
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Heading
            Text(
                text = "What needs to be fixed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = darkRed
            )
            
            // Bullet points
            if (issues.isEmpty()) {
                Text(
                    text = "No specific issues mentioned",
                    style = MaterialTheme.typography.bodyMedium,
                    color = darkRed.copy(alpha = 0.8f)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    issues.forEach { issue ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = darkRed.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = issue,
                                style = MaterialTheme.typography.bodyMedium,
                                color = darkRed.copy(alpha = 0.8f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Parses rejection reason string into a list of bullet points.
 * Handles multiple delimiters: newline, comma, semicolon.
 */
private fun parseRejectionReason(rejectionReason: String?): List<String> {
    if (rejectionReason.isNullOrBlank()) {
        return emptyList()
    }
    
    // Split by common delimiters
    val delimiters = listOf("\n", ",", ";", "|")
    var parts = listOf(rejectionReason.trim())
    
    for (delimiter in delimiters) {
        parts = parts.flatMap { it.split(delimiter) }
    }
    
    // Clean and filter out empty strings
    return parts
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
}
