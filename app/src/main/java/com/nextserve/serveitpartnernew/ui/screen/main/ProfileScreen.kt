package com.nextserve.serveitpartnernew.ui.screen.main

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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.data.model.ProviderData
import com.nextserve.serveitpartnernew.ui.components.LogoutRow
import com.nextserve.serveitpartnernew.ui.components.ProfileHeader
import com.nextserve.serveitpartnernew.ui.components.ProfileIcons
import com.nextserve.serveitpartnernew.ui.components.ProfileOptionRow
import com.nextserve.serveitpartnernew.ui.components.ProfileSection
import com.nextserve.serveitpartnernew.ui.components.QuickActionChip
import com.nextserve.serveitpartnernew.ui.components.StatChip
import com.nextserve.serveitpartnernew.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val uid = FirebaseProvider.auth.currentUser?.uid ?: return
    
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(uid)
    )
    val uiState = viewModel.uiState

    if (uiState.isLoading) {
        ProfileLoadingPlaceholder(modifier = modifier)
        return
    }

    val providerData = uiState.providerData

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        ProfileHeader(
            fullName = providerData?.fullName ?: "",
            phoneNumber = providerData?.phoneNumber ?: "",
            approvalStatus = providerData?.approvalStatus ?: "PENDING",
            onboardingStatus = providerData?.onboardingStatus ?: "IN_PROGRESS",
            currentStep = providerData?.currentStep ?: 1
        )

        StatusBanner(providerData)

        QuickActionsRow(navController)

        StatsRow()

        ProfileSection(title = "Account") {
            ProfileOptionRow(
                icon = ProfileIcons.PersonalInformation,
                title = "Personal Information",
                subtitle = "Name, phone, email",
                showDivider = true,
                onClick = { navController.navigate("profile/edit/basic") }
            )
            ProfileOptionRow(
                icon = ProfileIcons.ServiceInformation,
                title = "Service Information",
                subtitle = "Skills, categories",
                showDivider = true,
                onClick = { navController.navigate("profile/edit/services") }
            )
            ProfileOptionRow(
                icon = ProfileIcons.AddressInformation,
                title = "Address Information",
                subtitle = "City, radius, pincode",
                showDivider = true,
                onClick = { navController.navigate("profile/edit/address") }
            )
            ProfileOptionRow(
                icon = ProfileIcons.Documents,
                title = "Documents",
                subtitle = "Aadhaar, verification",
                showDivider = false,
                onClick = { navController.navigate("profile/edit/documents") }
            )
        }

        ProfileSection(title = "Preferences") {
            ProfileOptionRow(
                icon = ProfileIcons.Language,
                title = "Language",
                subtitle = "English",
                showDivider = true,
                onClick = { navController.navigate("profile/edit/preferences") }
            )
            ProfileOptionRow(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                subtitle = "Manage push alerts",
                showDivider = false,
                onClick = { navController.navigate("profile/edit/preferences") }
            )
        }

        ProfileSection(title = "Support & About") {
            ProfileOptionRow(
                icon = Icons.Filled.Info,
                title = "Help & Support",
                subtitle = "FAQs, contact us",
                showDivider = true,
                onClick = { }
            )
            ProfileOptionRow(
                icon = Icons.Outlined.Info,
                title = "About App",
                subtitle = "Version, terms & privacy",
                showDivider = false,
                onClick = { }
            )
        }

        LogoutRow(
            onClick = {
                FirebaseProvider.auth.signOut()
            }
        )

        Text(
            text = "App version 1.0",
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
            label = "Edit Profile",
            onClick = { navController.navigate("profile/edit/basic") }
        )
        QuickActionChip(
            icon = ProfileIcons.ServiceInformation,
            label = "Services",
            onClick = { navController.navigate("profile/edit/services") }
        )
        QuickActionChip(
            icon = Icons.Filled.Info,
            label = "Documents",
            onClick = { navController.navigate("profile/edit/documents") }
        )
    }
}

@Composable
private fun StatsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatChip(value = "4.8", label = "Rating", modifier = Modifier.weight(1f))
        StatChip(value = "24", label = "Jobs done", modifier = Modifier.weight(1f))
        StatChip(value = "₹12k", label = "Earnings", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatusBanner(providerData: ProviderData?) {
    val (title, subtitle, color) = when (providerData?.approvalStatus) {
        "APPROVED" -> Triple(
            "Approved",
            "You are ready to accept jobs",
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        )
        "REJECTED" -> Triple(
            "Profile rejected",
            providerData.rejectionReason ?: "Please review your documents",
            MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        )
        "PENDING" -> {
            if (providerData?.onboardingStatus == "SUBMITTED") {
                Triple(
                    "Under review",
                    "We’ll notify you once verification completes",
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                )
            } else {
                Triple(
                    "Complete your profile",
                    "Finish the steps to submit for review",
                    MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
        else -> Triple(
            "Complete your profile",
            "Finish the steps to submit for review",
            MaterialTheme.colorScheme.surfaceVariant
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = color,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

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

