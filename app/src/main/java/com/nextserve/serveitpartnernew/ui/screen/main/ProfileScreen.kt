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
                modifier = Modifier.size(28.dp),
                tint = Color.Black
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    color = Color.Black,
                    fontWeight = FontWeight.Normal
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Black
            )
        }

        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(
                color = Color.Black.copy(alpha = 0.1f),
                thickness = 1.dp,
                modifier = Modifier.padding(start = 60.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
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

    val providerData = uiState.providerData
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                actions = {
                    TextButton(
                        onClick = { FirebaseProvider.auth.signOut() }
                    ) {
                        Text(
                            text = "Logout",
                            color = Color.Black,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White,
        modifier = modifier
    ) { scaffoldPaddingValues ->
        // Calculate bottom padding for bottom navigation
        val parentBottomPadding = parentPaddingValues.calculateBottomPadding()
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(scaffoldPaddingValues)
                .background(Color.White)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

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
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
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
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = providerData?.fullName?.firstOrNull()?.uppercase() ?: "U",
                                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp),
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    // Small Edit Button Overlay (bottom-right)
                    Surface(
                        modifier = Modifier
                            .size(28.dp)
                            .offset(x = 6.dp, y = 6.dp)
                            .clip(CircleShape)
                            .clickable(onClick = { imagePicker.launch("image/*") }),
                        color = Color.Black,
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                // Name, ID, and Badge (right side, vertically stacked)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Name
                    Text(
                        text = providerData?.fullName ?: "—",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    // ID Number
                    Text(
                        text = "ID No: ${providerData?.uid?.takeLast(10) ?: "—"}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                        color = Color.Black.copy(alpha = 0.6f)
                    )

                    // Verified Badge
                    val badgeText = when (providerData?.approvalStatus) {
                        "APPROVED" -> "Verified"
                        "PENDING" -> "Pending Approval"
                        "REJECTED" -> "Review Required"
                        else -> "Incomplete Profile"
                    }
                    
                    Surface(
                        modifier = Modifier.padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.05f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (providerData?.approvalStatus == "APPROVED") {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.Black
                                )
                            }
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Settings Section Header
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
            subtitle = LanguageManager.getLanguageDisplayName(LanguageManager.getSavedLanguage(context), context),
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
}
