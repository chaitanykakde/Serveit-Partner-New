package com.nextserve.serveitpartnernew.ui.screen.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.ui.components.BottomStickyButtonContainer
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.theme.CardShape

@Composable
fun Step5Review(
    fullName: String,
    gender: String,
    email: String,
    primaryService: String,
    selectedMainService: String,
    selectedSubServices: List<String>,
    otherService: String,
    state: String,
    city: String,
    address: String,
    fullAddress: String,
    locationPincode: String,
    serviceRadius: Float,
    aadhaarFrontUploaded: Boolean,
    aadhaarBackUploaded: Boolean,
    isSubmitted: Boolean,
    onEditBasicInfo: () -> Unit,
    onEditServices: () -> Unit,
    onEditLocation: () -> Unit,
    onEditDocuments: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    BottomStickyButtonContainer(
        button = {
            if (!isSubmitted) {
                PrimaryButton(
                    text = "Submit for Verification",
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        content = {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isSubmitted) {
                    // Confirmation message
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            modifier = Modifier.size(64.dp),
                            tint = colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Details submitted successfully",
                                style = MaterialTheme.typography.headlineSmall,
                                color = colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your profile is under verification",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Title
                    Text(
                        text = "Review & Submit",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    // Subtitle
                    Text(
                        text = "Please verify your details before submitting",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Basic Information Card
                    ReviewSectionCard(
                        title = "Basic Information",
                        onEdit = onEditBasicInfo,
                        content = {
                            ReviewLabelValue(label = "Name", value = fullName.ifEmpty { "Not provided" })
                            ReviewLabelValue(label = "Gender", value = gender.ifEmpty { "Not provided" })
                            ReviewLabelValue(
                                label = "Primary Service",
                                value = primaryService.ifEmpty { "Not provided" }
                            )
                            if (email.isNotEmpty()) {
                                ReviewLabelValue(label = "Email", value = email)
                            }
                        }
                    )

                    // Services Card
                    ReviewSectionCard(
                        title = "Services",
                        onEdit = onEditServices,
                        content = {
                            ReviewLabelValue(
                                label = "Main Service",
                                value = selectedMainService.ifEmpty { "Not selected" }
                            )
                            if (selectedSubServices.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Sub-Services:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                selectedSubServices.forEach { subService ->
                                    Row(
                                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                                    ) {
                                        Text(
                                            text = "• ",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = subService,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            if (selectedMainService == "Other Services" && otherService.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                ReviewLabelValue(label = "Custom Service", value = otherService)
                            }
                        }
                    )

                    // Location Card
                    ReviewSectionCard(
                        title = "Location",
                        onEdit = onEditLocation,
                        content = {
                            ReviewLabelValue(label = "State", value = state.ifEmpty { "Not provided" })
                            ReviewLabelValue(label = "City", value = city.ifEmpty { "Not provided" })
                            if (address.isNotEmpty()) {
                                ReviewLabelValue(label = "Area", value = address)
                            }
                            if (fullAddress.isNotEmpty()) {
                                ReviewLabelValue(label = "Full Address", value = fullAddress)
                            }
                            if (locationPincode.isNotEmpty()) {
                                ReviewLabelValue(label = "Pincode", value = locationPincode)
                            }
                            ReviewLabelValue(
                                label = "Service Radius",
                                value = "${serviceRadius.toInt()} km"
                            )
                        }
                    )

                    // Documents Card
                    ReviewSectionCard(
                        title = "Documents",
                        onEdit = onEditDocuments,
                        content = {
                            DocumentStatusRow(
                                label = "Aadhaar Front",
                                isUploaded = aadhaarFrontUploaded
                            )
                            DocumentStatusRow(
                                label = "Aadhaar Back",
                                isUploaded = aadhaarBackUploaded
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    )
}

@Composable
private fun ReviewSectionCard(
    title: String,
    onEdit: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row with Edit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onEdit
                ) {
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            content()
        }
    }
}

@Composable
private fun ReviewLabelValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = colorScheme.onSurface
        )
    }
}

@Composable
private fun DocumentStatusRow(
    label: String,
    isUploaded: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colorScheme.onSurface
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isUploaded) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Uploaded",
                    modifier = Modifier.size(20.dp),
                    tint = colorScheme.primary
                )
                Text(
                    text = "Uploaded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.primary
                )
            } else {
                Text(
                    text = "Not uploaded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
