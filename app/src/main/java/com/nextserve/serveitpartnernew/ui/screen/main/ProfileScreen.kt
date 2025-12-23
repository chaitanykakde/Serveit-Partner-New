package com.nextserve.serveitpartnernew.ui.screen.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.ui.components.LogoutRow
import com.nextserve.serveitpartnernew.ui.components.ProfileHeader
import com.nextserve.serveitpartnernew.ui.components.ProfileIcons
import com.nextserve.serveitpartnernew.ui.components.ProfileOptionRow
import com.nextserve.serveitpartnernew.ui.components.ProfileSection
import com.nextserve.serveitpartnernew.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier
) {
    val uid = FirebaseProvider.auth.currentUser?.uid ?: return
    
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(uid)
    )
    val uiState = viewModel.uiState
    
    if (uiState.isLoading) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading profile...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
        
        // 🧑‍💼 PROFILE HEADER (TOP)
        ProfileHeader(
            fullName = providerData?.fullName ?: "",
            phoneNumber = providerData?.phoneNumber ?: "",
            approvalStatus = providerData?.approvalStatus ?: "PENDING",
            onEditProfileClick = {
                // Future: Navigate to edit profile
            }
        )
        
        // 📂 ACCOUNT SECTION
        ProfileSection(title = "Account") {
            ProfileOptionRow(
                icon = ProfileIcons.PersonalInformation,
                title = "Personal Information",
                showDivider = true,
                onClick = {
                    // Future: Navigate to personal information
                }
            )
            ProfileOptionRow(
                icon = ProfileIcons.ServiceInformation,
                title = "Service Information",
                showDivider = true,
                onClick = {
                    // Future: Navigate to service information
                }
            )
            ProfileOptionRow(
                icon = ProfileIcons.AddressInformation,
                title = "Address Information",
                showDivider = true,
                onClick = {
                    // Future: Navigate to address information
                }
            )
            ProfileOptionRow(
                icon = ProfileIcons.Documents,
                title = "Documents",
                showDivider = false,
                onClick = {
                    // Future: Navigate to documents
                }
            )
        }
        
        // ⚙️ PREFERENCES SECTION
        ProfileSection(title = "Preferences") {
            ProfileOptionRow(
                icon = ProfileIcons.Language,
                title = "Language",
                showDivider = true,
                onClick = {
                    // Future: Navigate to language settings
                }
            )
            ProfileOptionRow(
                icon = ProfileIcons.Notifications,
                title = "Notifications",
                showDivider = true,
                onClick = {
                    // Future: Navigate to notification settings
                }
            )
            ProfileOptionRow(
                icon = ProfileIcons.DisplayTheme,
                title = "Display / Theme",
                showDivider = false,
                onClick = {
                    // Future: Navigate to theme settings
                }
            )
        }
        
        // 🆘 SUPPORT SECTION
        ProfileSection(title = "Support") {
            ProfileOptionRow(
                icon = ProfileIcons.HelpSupport,
                title = "Help & Support",
                showDivider = true,
                onClick = {
                    // Future: Navigate to help & support
                }
            )
            ProfileOptionRow(
                icon = ProfileIcons.AboutApp,
                title = "About App",
                showDivider = false,
                onClick = {
                    // Future: Navigate to about app
                }
            )
        }
        
        // 🚪 LOGOUT
        LogoutRow(
            onClick = {
                FirebaseProvider.auth.signOut()
                // Navigation will be handled by MainActivity observing auth state
            }
        )
        
        // ℹ️ APP VERSION
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

