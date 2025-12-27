package com.nextserve.serveitpartnernew.ui.screen.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.model.ProviderData
import com.nextserve.serveitpartnernew.ui.components.LogoutRow
import com.nextserve.serveitpartnernew.ui.components.ProfileHeader
import com.nextserve.serveitpartnernew.ui.components.ProfileIcons
import com.nextserve.serveitpartnernew.ui.components.ProfileOptionRow
import com.nextserve.serveitpartnernew.ui.components.ProfileSection
import com.nextserve.serveitpartnernew.ui.components.QuickActionChip
import com.nextserve.serveitpartnernew.ui.components.StatChip
import com.nextserve.serveitpartnernew.ui.viewmodel.ProfileEditViewModel
import com.nextserve.serveitpartnernew.ui.viewmodel.ProfileViewModel
import com.nextserve.serveitpartnernew.utils.LanguageManager

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

    if (uiState.isLoading) {
        ProfileLoadingPlaceholder(modifier = modifier)
        return
    }

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

        ProfileHeader(
            fullName = providerData?.fullName ?: "",
            phoneNumber = providerData?.phoneNumber ?: "",
            approvalStatus = providerData?.approvalStatus ?: "PENDING",
            onboardingStatus = providerData?.onboardingStatus ?: "IN_PROGRESS",
            currentStep = providerData?.currentStep ?: 1,
            profilePhotoUrl = providerData?.profilePhotoUrl ?: "",
            onProfilePhotoClick = { imagePicker.launch("image/*") }
        )

        StatusBanner(providerData, navController)

        QuickActionsRow(navController)

        StatsRow(
            stats = uiState.stats,
            isLoading = uiState.isLoadingStats
        )

        val context = LocalContext.current
        
        ProfileSection(title = stringResource(R.string.account)) {
            ProfileOptionRow(
                icon = ProfileIcons.PersonalInformation,
                title = stringResource(R.string.personal_information),
                subtitle = stringResource(R.string.name_phone_email),
                showDivider = true,
                onClick = { navController.navigate("profile/edit/basic") }
            )
            ProfileOptionRow(
                icon = ProfileIcons.ServiceInformation,
                title = stringResource(R.string.service_information),
                subtitle = stringResource(R.string.skills_categories),
                showDivider = true,
                onClick = { navController.navigate("profile/edit/services") }
            )
            ProfileOptionRow(
                icon = ProfileIcons.AddressInformation,
                title = stringResource(R.string.address_information),
                subtitle = stringResource(R.string.city_radius_pincode),
                showDivider = true,
                onClick = { navController.navigate("profile/edit/address") }
            )
            ProfileOptionRow(
                icon = ProfileIcons.Documents,
                title = stringResource(R.string.documents),
                subtitle = stringResource(R.string.aadhaar_verification),
                showDivider = false,
                onClick = { navController.navigate("profile/edit/documents") }
            )
        }

        ProfileSection(title = stringResource(R.string.preferences)) {
            ProfileOptionRow(
                icon = ProfileIcons.Language,
                title = stringResource(R.string.language),
                subtitle = LanguageManager.getLanguageDisplayName(LanguageManager.getSavedLanguage(context), context),
                showDivider = true,
                onClick = { navController.navigate("profile/edit/preferences") }
            )
            ProfileOptionRow(
                icon = Icons.Filled.Notifications,
                title = stringResource(R.string.notifications),
                subtitle = stringResource(R.string.manage_push_alerts),
                showDivider = false,
                onClick = { navController.navigate("profile/edit/preferences") }
            )
        }

        ProfileSection(title = stringResource(R.string.support_about)) {
            ProfileOptionRow(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.help_support),
                subtitle = stringResource(R.string.faqs_contact_us),
                showDivider = true,
                onClick = { navController.navigate("help/support") }
            )
            ProfileOptionRow(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.about_app),
                subtitle = stringResource(R.string.version_terms_privacy),
                showDivider = false,
                onClick = { navController.navigate("about/app") }
            )
        }

        LogoutRow(
            onClick = {
                FirebaseProvider.auth.signOut()
            }
        )

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

@Composable
private fun QuickActionsRow(navController: NavController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionChip(
            icon = Icons.Filled.Edit,
            label = stringResource(R.string.edit_profile_button),
            onClick = { navController.navigate("profile/edit/basic") }
        )
        QuickActionChip(
            icon = ProfileIcons.ServiceInformation,
            label = stringResource(R.string.services),
            onClick = { navController.navigate("profile/edit/services") }
        )
        QuickActionChip(
            icon = Icons.Filled.Info,
            label = stringResource(R.string.documents),
            onClick = { navController.navigate("profile/edit/documents") }
        )
    }
}

@Composable
private fun StatsRow(
    stats: com.nextserve.serveitpartnernew.data.model.ProviderStats?,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isLoading || stats == null) {
            repeat(3) {
                StatChipShimmer(modifier = Modifier.weight(1f))
            }
        } else {
            StatChip(
                value = stats.getFormattedRating(),
                label = stringResource(R.string.rating),
                icon = Icons.Filled.Star,
                modifier = Modifier.weight(1f)
            )
            StatChip(
                value = stats.totalJobs.toString(),
                label = stringResource(R.string.jobs_done),
                icon = Icons.Filled.List,
                modifier = Modifier.weight(1f)
            )
            StatChip(
                value = stats.getFormattedEarnings(),
                label = stringResource(R.string.earnings),
                icon = Icons.Filled.Star,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatChipShimmer(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 20.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            )
            Box(
                modifier = Modifier
                    .size(width = 50.dp, height = 12.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
            )
        }
    }
}

@Composable
private fun StatusBanner(
    providerData: ProviderData?,
    navController: NavController
) {
    val statusInfo = when (providerData?.approvalStatus) {
        "APPROVED" -> StatusInfo(
            title = stringResource(R.string.approved_status),
            subtitle = stringResource(R.string.approved_message),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            icon = Icons.Filled.CheckCircle,
            iconTint = MaterialTheme.colorScheme.primary,
            showActionButton = false,
            actionText = "",
            actionRoute = ""
        )
        "REJECTED" -> StatusInfo(
            title = stringResource(R.string.rejected_status),
            subtitle = providerData.rejectionReason ?: stringResource(R.string.please_review_your_documents),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            icon = Icons.Filled.Close,
            iconTint = MaterialTheme.colorScheme.error,
            showActionButton = true,
            actionText = stringResource(R.string.edit_profile),
            actionRoute = "profile/edit/basic"
        )
        "PENDING" -> {
            if (providerData?.onboardingStatus == "SUBMITTED") {
                StatusInfo(
                    title = stringResource(R.string.under_review),
                    subtitle = stringResource(R.string.we_will_notify_once_verification_completes),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    icon = Icons.Filled.Info,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    showActionButton = false,
                    actionText = "",
                    actionRoute = ""
                )
            } else {
                StatusInfo(
                    title = stringResource(R.string.complete_profile),
                    subtitle = stringResource(R.string.finish_steps_to_submit),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    icon = Icons.Filled.Edit,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    showActionButton = true,
                    actionText = stringResource(R.string.complete_now),
                    actionRoute = "profile/edit/basic"
                )
            }
        }
        else -> StatusInfo(
            title = stringResource(R.string.complete_profile),
            subtitle = stringResource(R.string.finish_steps_to_submit),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            icon = Icons.Filled.Edit,
            iconTint = MaterialTheme.colorScheme.secondary,
            showActionButton = true,
            actionText = stringResource(R.string.complete_now),
            actionRoute = "profile/edit/basic"
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = statusInfo.color,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(statusInfo.iconTint.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusInfo.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = statusInfo.iconTint
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = statusInfo.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = statusInfo.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (statusInfo.showActionButton && statusInfo.actionText.isNotEmpty()) {
                Button(
                    onClick = { navController.navigate(statusInfo.actionRoute) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (providerData?.approvalStatus) {
                            "REJECTED" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Text(text = statusInfo.actionText)
                }
            }
        }
    }
}

private data class StatusInfo(
    val title: String,
    val subtitle: String,
    val color: androidx.compose.ui.graphics.Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconTint: androidx.compose.ui.graphics.Color,
    val showActionButton: Boolean,
    val actionText: String,
    val actionRoute: String
)

@Composable
private fun ProfileLoadingPlaceholder(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(4) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (it == 0) 140.dp else 96.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 2.dp
                ) {}
            }
        }
    }
}

