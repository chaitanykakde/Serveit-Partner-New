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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.ui.components.ProfileIcons
import com.nextserve.serveitpartnernew.ui.viewmodel.ProfileEditViewModel
import com.nextserve.serveitpartnernew.ui.viewmodel.ProfileViewModel
import com.nextserve.serveitpartnernew.utils.LanguageManager

@Composable
private fun FlatOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    showDivider: Boolean = true,
    languageChip: String? = null,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .heightIn(min = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in soft rounded container
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Title and subtitle
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    languageChip?.let {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Chevron right
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                thickness = 1.dp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: com.nextserve.serveitpartnernew.ui.viewmodel.AuthViewModel,
    parentPaddingValues: PaddingValues = PaddingValues(),
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

    // Helper function to convert URI to ByteArray immediately
    fun uriToByteArray(uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            null
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            uriToByteArray(selectedUri)?.let { bytes ->
                editViewModel.uploadProfilePhoto(bytes) { progress ->
                    uploadProgress = progress
                }
            }
        }
    }

    LaunchedEffect(editViewModel.uiState.successMessage) {
        editViewModel.uiState.successMessage?.let {
            viewModel.refreshProfile()
            uploadProgress = 0.0
        }
    }

    val providerData = uiState.providerData
    val scrollState = rememberScrollState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(
                        onClick = {
                            android.util.Log.d("ProfileScreen", "🚪 Logout button clicked")
                            showLogoutDialog = true
                        }
                    ) {
                        Text(
                            text = "Logout",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) { scaffoldPaddingValues ->
        // Calculate bottom padding for bottom navigation
        val parentBottomPadding = parentPaddingValues.calculateBottomPadding()
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
            .verticalScroll(scrollState)
                .padding(scaffoldPaddingValues)
                .background(MaterialTheme.colorScheme.surface)
    ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal Profile Header: Icon on left, Info on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
                // Profile Photo with Edit Button Overlay (left side)
                Box(
                    modifier = Modifier.size(80.dp)
                ) {
            Box(
                modifier = Modifier
                            .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = { imagePicker.launch("image/*") }),
                contentAlignment = Alignment.Center
            ) {
                if (providerData?.profilePhotoUrl?.isNotEmpty() == true) {
                    AsyncImage(
                        model = providerData.profilePhotoUrl,
                        contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                    )
                } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = providerData?.fullName?.firstOrNull()?.uppercase() ?: "U",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
            }
        }

                    // Small Edit Button Overlay (bottom-right)
                    Surface(
            modifier = Modifier
                            .size(24.dp)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .clip(CircleShape)
                            .clickable(onClick = { imagePicker.launch("image/*") }),
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                // Name, ID, and Badge (right side, vertically stacked)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
                    // Name
                    Text(
                        text = providerData?.fullName ?: "—",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // ID Number
                    Text(
                        text = "ID No: ${providerData?.uid?.takeLast(10) ?: "—"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Verified Badge
                    val badgeText = when (providerData?.approvalStatus) {
                        "APPROVED" -> "Verified"
                        "PENDING" -> "Pending Approval"
                        "REJECTED" -> "Review Required"
                        else -> "Incomplete Profile"
                    }

                    val (badgeColor, badgeTextColor) = when (providerData?.approvalStatus) {
                        "APPROVED" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Surface(
                        modifier = Modifier.padding(top = 6.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (providerData?.approvalStatus == "APPROVED") {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = badgeTextColor
                                )
                            }
                    Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor
                    )
                }
            }
        }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Section Header
        Text(
                text = "Settings".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
        )

            // Settings Options (consolidated from all sections)
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
                showDivider = true,
            onClick = { navController.navigate("profile/edit/documents") }
        )
        FlatOptionRow(
            icon = ProfileIcons.Language,
            title = stringResource(R.string.language),
            subtitle = "App interface language",
            languageChip = "EN",
            showDivider = true,
            onClick = { navController.navigate("profile/edit/preferences") }
        )
        FlatOptionRow(
            icon = Icons.Filled.Notifications,
            title = stringResource(R.string.notifications),
            subtitle = stringResource(R.string.manage_push_alerts),
                showDivider = true,
            onClick = { navController.navigate("profile/edit/preferences") }
        )
        FlatOptionRow(
            icon = Icons.Filled.Info,
            title = stringResource(R.string.help_support),
            subtitle = stringResource(R.string.faqs_contact_us),
            showDivider = true,
            onClick = { navController.navigate("help/support") }
        )
        FlatOptionRow(
                icon = Icons.Filled.Info,
            title = stringResource(R.string.about_app),
            subtitle = stringResource(R.string.version_terms_privacy),
            showDivider = false,
            onClick = { navController.navigate("about/app") }
        )

            Spacer(modifier = Modifier.height(16.dp + parentBottomPadding))
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Logout",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to logout?",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        android.util.Log.d("ProfileScreen", "🚪 Logout confirmed")
                        authViewModel.signOut()
                    }
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
