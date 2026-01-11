package com.nextserve.serveitpartnernew.ui.screen.main

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.model.ProviderData
import com.nextserve.serveitpartnernew.ui.components.ProfileIcons
import com.nextserve.serveitpartnernew.ui.viewmodel.ProfileEditViewModel
import com.nextserve.serveitpartnernew.ui.viewmodel.ProfileViewModel
import com.nextserve.serveitpartnernew.utils.LanguageManager
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import kotlin.math.roundToInt

@Composable
private fun FlatOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    showDivider: Boolean = true,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                thickness = 1.dp,
                modifier = Modifier.padding(start = 56.dp)
            )
        }
    }
}

@Composable
fun ProfileScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val uid = FirebaseProvider.auth.currentUser?.uid ?: return
    val context = LocalContext.current
    
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(uid)
    )
    val uiState = viewModel.uiState
    
    val editViewModel: ProfileEditViewModel = viewModel(
        factory = ProfileEditViewModel.factory(context, uid)
    )
    var uploadProgress by remember { mutableStateOf(0.0) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            editViewModel.uploadProfilePhoto(it) { progress ->
                uploadProgress = progress
            }
        }
    }

    LaunchedEffect(editViewModel.uiState.successMessage) {
        editViewModel.uiState.successMessage?.let {
            viewModel.refreshProfile()
            uploadProgress = 0.0
        }
    }

    // Skip loading state for now - show content directly

    val providerData = uiState.providerData
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // New flat Profile Header (minimalist)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Photo (64dp, CircleShape)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable(onClick = { imagePicker.launch("image/*") }),
                contentAlignment = Alignment.Center
            ) {
                if (providerData?.profilePhotoUrl?.isNotEmpty() == true) {
                    AsyncImage(
                        model = providerData.profilePhotoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = providerData?.fullName?.firstOrNull()?.uppercase() ?: "U",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Name, Phone, and Completion text
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Name (titleLarge)
                Text(
                    text = providerData?.fullName ?: "—",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Phone number (bodySmall, muted)
                Text(
                    text = providerData?.phoneNumber ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Completion text: "Profile X% complete" (bodySmall)
                val progress = ((providerData?.currentStep ?: 1).coerceAtLeast(0).coerceAtMost(5)).toFloat() / 5.toFloat()
                val progressPercent = (progress * 100).roundToInt()
                Text(
                    text = "Profile ${progressPercent}% complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Inline Status Row (flat, no banner)
        val statusInfo = when (providerData?.approvalStatus) {
            "APPROVED" -> Pair("Ready to accept jobs", MaterialTheme.colorScheme.primary)
            "REJECTED" -> Pair("Review your documents", MaterialTheme.colorScheme.error)
            "PENDING" -> if (providerData?.onboardingStatus == "SUBMITTED")
                Pair("Under review", MaterialTheme.colorScheme.secondary)
                else Pair("Complete your profile", MaterialTheme.colorScheme.secondary)
            else -> Pair("Complete your profile", MaterialTheme.colorScheme.secondary)
        }

        val showAction = when (providerData?.approvalStatus) {
            "APPROVED", "PENDING" -> providerData?.onboardingStatus != "SUBMITTED"
            "REJECTED" -> true
            else -> true
        }

        val actionRoute = when {
            providerData?.approvalStatus == "REJECTED" -> "profile/edit/basic"
            showAction -> "profile/edit/basic"
            else -> ""
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small colored dot
            Canvas(modifier = Modifier.size(6.dp)) {
                drawCircle(color = statusInfo.second.copy(alpha = 0.7f))
            }

            // Status text
            Text(
                text = statusInfo.first,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            // Optional action button
            if (showAction && actionRoute.isNotEmpty()) {
                TextButton(
                    onClick = { navController.navigate(actionRoute) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = when (providerData?.approvalStatus) {
                            "REJECTED" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Text(
                        text = when (providerData?.approvalStatus) {
                            "REJECTED" -> stringResource(R.string.edit_profile)
                            else -> stringResource(R.string.complete_now)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Flat Quick Actions (no chips)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            TextButton(
                onClick = { navController.navigate("profile/edit/basic") }
            ) {
                Text(
                    text = stringResource(R.string.edit_profile_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Flat Stats Row (no card, no elevation)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            if (uiState.isLoadingStats || uiState.stats == null) {
                repeat(3) {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.stats.getFormattedRating(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Rating",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.stats.totalJobs.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Jobs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.stats.getFormattedEarnings(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Earnings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val context = LocalContext.current
        
        // Flat Account Section (Android Settings style)
        Text(
            text = stringResource(R.string.account),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        FlatOptionRow(
            icon = ProfileIcons.PersonalInformation,
            title = stringResource(R.string.personal_information),
            subtitle = stringResource(R.string.name_phone_email),
            showDivider = true,
            onClick = { navController.navigate("profile/edit/basic") }
        )
        FlatOptionRow(
            icon = ProfileIcons.ServiceInformation,
            title = stringResource(R.string.service_information),
            subtitle = stringResource(R.string.skills_categories),
            showDivider = true,
            onClick = { navController.navigate("profile/edit/services") }
        )
        FlatOptionRow(
            icon = ProfileIcons.AddressInformation,
            title = stringResource(R.string.address_information),
            subtitle = stringResource(R.string.city_radius_pincode),
            showDivider = true,
            onClick = { navController.navigate("profile/edit/address") }
        )
        FlatOptionRow(
            icon = ProfileIcons.Documents,
            title = stringResource(R.string.documents),
            subtitle = stringResource(R.string.aadhaar_verification),
            showDivider = false,
            onClick = { navController.navigate("profile/edit/documents") }
        )

        // Flat Preferences Section
        Text(
            text = stringResource(R.string.preferences),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        FlatOptionRow(
            icon = ProfileIcons.Language,
            title = stringResource(R.string.language),
            subtitle = LanguageManager.getLanguageDisplayName(LanguageManager.getSavedLanguage(context), context),
            showDivider = true,
            onClick = { navController.navigate("profile/edit/preferences") }
        )
        FlatOptionRow(
            icon = Icons.Filled.Notifications,
            title = stringResource(R.string.notifications),
            subtitle = stringResource(R.string.manage_push_alerts),
            showDivider = false,
            onClick = { navController.navigate("profile/edit/preferences") }
        )

        // Flat Support/About Section
        Text(
            text = stringResource(R.string.support_about),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        FlatOptionRow(
            icon = Icons.Filled.Info,
            title = stringResource(R.string.help_support),
            subtitle = stringResource(R.string.faqs_contact_us),
            showDivider = true,
            onClick = { navController.navigate("help/support") }
        )
        FlatOptionRow(
            icon = Icons.Outlined.Info,
            title = stringResource(R.string.about_app),
            subtitle = stringResource(R.string.version_terms_privacy),
            showDivider = false,
            onClick = { navController.navigate("about/app") }
        )

        // Flat Logout (subtle text button)
        TextButton(
            onClick = { FirebaseProvider.auth.signOut() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(
                text = stringResource(R.string.logout),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            text = stringResource(R.string.app_version),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

