package com.nextserve.serveitpartnernew.ui.onboarding.step5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ReviewContent(
    // Basic Info
    fullName: String,
    gender: String,
    email: String,

    // Services
    primaryService: String,
    selectedMainService: String,
    selectedSubServices: List<String>,
    otherService: String,

    // Location
    state: String,
    city: String,
    address: String,
    fullAddress: String,
    locationPincode: String,
    serviceRadius: Float,

    // Documents
    aadhaarFrontUploaded: Boolean,
    aadhaarBackUploaded: Boolean,
    profilePhotoUploaded: Boolean,

    // State
    isSubmitted: Boolean,
    verificationStatus: String? = null, // "pending", "rejected", "verified"
    rejectionReason: String? = null,
    submittedAt: Long? = null, // Timestamp in milliseconds
    isLoading: Boolean = false,
    errorMessage: String? = null,

    // Callbacks
    onEditBasicInfo: () -> Unit,
    onEditServices: () -> Unit,
    onEditLocation: () -> Unit,
    onEditDocuments: () -> Unit,
    onSubmit: () -> Unit,
    onEditRejectedProfile: (() -> Unit)? = null,
    onContactSupport: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null, // Callback to edit rejected profile

    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    if (isSubmitted) {
        // Show modern verification status screen (matching HTML design)
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Header with icon
            VerificationStatusHeader(
                status = verificationStatus ?: "pending",
                title = when (verificationStatus?.lowercase()) {
                    "verified" -> "Profile Verified Successfully"
                    "rejected" -> "Profile Rejected"
                    else -> "Profile Submitted Successfully"
                },
                subtitle = when (verificationStatus?.lowercase()) {
                    "verified" -> "Your profile has been approved. You can now start receiving jobs."
                    "rejected" -> if (rejectionReason != null && rejectionReason.isNotEmpty()) {
                        "Your profile was rejected because:\n\n$rejectionReason"
                    } else {
                        "Your profile has been rejected. Please review the feedback and update your information."
                    }
                    else -> "Your profile is currently under review by our verification team."
                }
            )

            // Status card
            VerificationStatusCard(
                status = verificationStatus ?: "pending",
                submittedDate = submittedAt,
                verifiedBy = if (verificationStatus?.lowercase() == "pending") "Serveit Admin Team" else null
            )

            // What happens next section
            when (verificationStatus?.lowercase()) {
                "rejected" -> {
                    // Show "What happens next" for rejected (if needed)
                    // The Edit & Resubmit button will be shown separately below
                }
                else -> {
                    // Show default pending steps
                    WhatHappensNextSection(
                        items = getDefaultPendingNextSteps()
                    )
                }
            }

            // Edit & Resubmit button for rejected status
            if (verificationStatus?.lowercase() == "rejected" && onEditRejectedProfile != null) {
                Button(
                    onClick = onEditRejectedProfile,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit & Resubmit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Action buttons (only show if callbacks are provided)
            if (onContactSupport != null || onLogout != null) {
                VerificationActionButtons(
                    onContactSupport = onContactSupport ?: { },
                    onLogout = onLogout ?: { }
                )
            }

            // Bottom spacing
            Spacer(modifier = Modifier.height(16.dp))
        }
    } else {
        // Show review form (matching HTML design exactly)
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp) // Space for fixed button
            ) {
                // Header section - matching HTML (progress bar + title)
                ReviewHeader()
                
                // Main content - matching HTML (px-6 equivalent)
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp) // px-6
                ) {
                    // Basic Info Section (matching HTML: space-y-5)
                    SectionContainer(
                        title = "Basic Info",
                        onEditClick = onEditBasicInfo
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) { // space-y-5
                            // Full Name (full width)
                            LabelValueItem(
                                label = "Full Name",
                                value = fullName.ifEmpty { "Not provided" }
                            )
                            
                            // Gender + Email in 2-column grid (matching HTML)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp) // gap-4
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    LabelValueItem(
                                        label = "Gender",
                                        value = gender.ifEmpty { "Not provided" }
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    LabelValueItem(
                                        label = "Email",
                                        value = if (email.isNotEmpty()) {
                                            // Display full email (or truncate only if extremely long)
                                            if (email.length > 35) "${email.take(32)}..." else email
                                        } else {
                                            "Not provided"
                                        }
                                    )
                                }
                            }
                        }
                    }
                
                    // Services Section (matching HTML: space-y-5)
                    SectionContainer(
                        title = "Services",
                        onEditClick = onEditServices
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) { // space-y-5
                            // Category (matching HTML: label "Category")
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // gap-2
                                LabelValueItem(
                                    label = "Category",
                                    value = "" // Empty value, we'll show chip below
                                )
                                // Category chip (matching HTML: blue-50/50 background)
                                ServiceChip(
                                    text = selectedMainService.ifEmpty { primaryService.ifEmpty { "Not selected" } },
                                    isPrimary = true
                                )
                            }
                            
                            // Skills (matching HTML: label "Skills")
                            if (selectedSubServices.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // gap-2
                                    LabelValueItem(
                                        label = "Skills",
                                        value = "" // Empty value, we'll show chips below
                                    )
                                    // Natural flow wrap - chips wrap based on available width
                                    ServiceChipFlowRow(
                                        items = selectedSubServices,
                                        isPrimary = false,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { service, _ ->
                                        ServiceChip(text = service, isPrimary = false)
                                    }
                                }
                            }
                        }
                    }
                
                    // Location Section (matching HTML: space-y-4)
                    SectionContainer(
                        title = "Location",
                        onEditClick = onEditLocation
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { // space-y-4
                            // Service Address - Use horizontal space, don't break unnecessarily
                            LabelValueItem(
                                label = "Service Address",
                                value = if (fullAddress.isNotEmpty()) {
                                    // Keep address on single line, only break at natural points if needed
                                    fullAddress
                                } else {
                                    "Not provided"
                                }
                            )
                            
                            // Service Radius with modern icon (matching HTML: radar icon)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp), // pt-1
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Modern location icon with background circle
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(
                                            color = colorScheme.primaryContainer.copy(alpha = 0.3f),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp)) // gap-2.5
                                Text(
                                    text = "${serviceRadius.toInt()} km Service Radius",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp // Increased from 13sp
                                    ),
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f) // text-gray-600
                                )
                            }
                        }
                    }
                
                    // Documents Section (matching HTML: gap-4, py-1)
                    SectionContainer(
                        title = "Documents",
                        onEditClick = onEditDocuments,
                        showBottomBorder = false
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { // gap-4
                            // Aadhaar Front (matching HTML: py-1)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp), // py-1
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Aadhaar Card (Front)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp // Increased from 14sp
                                    ),
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface // text-gray-900
                                )
                                if (aadhaarFrontUploaded) {
                                    StatusPill()
                                }
                            }
                            
                            // Aadhaar Back (matching HTML: py-1)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp), // py-1
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Aadhaar Card (Back)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp // Increased from 14sp
                                    ),
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface // text-gray-900
                                )
                                if (aadhaarBackUploaded) {
                                    StatusPill()
                                }
                            }
                            
                            // Profile Photo (matching HTML: py-1)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp), // py-1
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Profile Photo",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp // Increased from 14sp
                                    ),
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface // text-gray-900
                                )
                                if (profilePhotoUploaded) {
                                    StatusPill()
                                }
                            }
                        }
                    }
                
                // Error message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                }
            }
            
            // Fixed bottom button (matching HTML: fixed bottom footer)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                FixedPrimaryButton(
                    text = "Submit for Verification",
                    onClick = onSubmit,
                    isLoading = isLoading,
                    enabled = !isLoading && aadhaarFrontUploaded && aadhaarBackUploaded && profilePhotoUploaded
                )
            }
        }
    }
}
