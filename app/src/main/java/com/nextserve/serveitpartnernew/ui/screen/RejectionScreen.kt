package com.nextserve.serveitpartnernew.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalConfiguration
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
            .padding(24.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = if (isTablet) 600.dp else screenWidth)
                    .align(Alignment.Center)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Rejected",
                modifier = Modifier.size(80.dp),
                tint = colorScheme.error
            )

            Spacer(modifier = Modifier.padding(24.dp))

            Text(
                text = "Profile Rejected",
                style = MaterialTheme.typography.headlineMedium,
                color = colorScheme.error,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.padding(16.dp))

            Text(
                text = "Your profile has been rejected. Please review the reason below and update your information.",
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.padding(24.dp))

            // Rejection Reason Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Rejection Reason:",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.onErrorContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = rejectionReason ?: "No reason provided",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.padding(32.dp))

                PrimaryButton(
                    text = "Edit Profile",
                    onClick = onEditProfile,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

