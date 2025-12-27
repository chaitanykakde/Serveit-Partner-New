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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.R
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
                    text = stringResource(R.string.submit_for_verification),
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
                                text = stringResource(R.string.details_submitted_successfully),
                                style = MaterialTheme.typography.headlineSmall,
                                color = colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.profile_under_verification),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Title
                    Text(
                        text = stringResource(R.string.review_submit),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    // Subtitle
                    Text(
                        text = stringResource(R.string.verify_details_before_submit),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Basic Information Card
                    ReviewSectionCard(
                        title = stringResource(R.string.basic_information),
                        onEdit = onEditBasicInfo,
                        content = {
                            ReviewLabelValue(label = stringResource(R.string.name), value = fullName.ifEmpty { stringResource(R.string.not_provided) })
                            ReviewLabelValue(label = stringResource(R.string.gender), value = gender.ifEmpty { stringResource(R.string.not_provided) })
                            ReviewLabelValue(
                                label = stringResource(R.string.primary_service),
                                value = primaryService.ifEmpty { stringResource(R.string.not_provided) }
                            )
                            if (email.isNotEmpty()) {
                                ReviewLabelValue(label = stringResource(R.string.email), value = email)
                            }
                        }
                    )

                    // Services Card
                    ReviewSectionCard(
                        title = stringResource(R.string.services),
                        onEdit = onEditServices,
                        content = {
                            ReviewLabelValue(
                                label = stringResource(R.string.main_service),
                                value = selectedMainService.ifEmpty { stringResource(R.string.not_selected) }
                            )
                            if (selectedSubServices.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.sub_services),
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
                                ReviewLabelValue(label = stringResource(R.string.custom_service), value = otherService)
                            }
                        }
                    )

                    // Location Card
                    ReviewSectionCard(
                        title = stringResource(R.string.location),
                        onEdit = onEditLocation,
                        content = {
                            ReviewLabelValue(label = stringResource(R.string.state), value = state.ifEmpty { stringResource(R.string.not_provided) })
                            ReviewLabelValue(label = stringResource(R.string.city), value = city.ifEmpty { stringResource(R.string.not_provided) })
                            if (address.isNotEmpty()) {
                                ReviewLabelValue(label = stringResource(R.string.area), value = address)
                            }
                            if (fullAddress.isNotEmpty()) {
                                ReviewLabelValue(label = stringResource(R.string.full_address), value = fullAddress)
                            }
                            if (locationPincode.isNotEmpty()) {
                                ReviewLabelValue(label = stringResource(R.string.pincode), value = locationPincode)
                            }
                            ReviewLabelValue(
                                label = stringResource(R.string.service_radius),
                                value = "${serviceRadius.toInt()} ${stringResource(R.string.km)}"
                            )
                        }
                    )

                    // Documents Card
                    ReviewSectionCard(
                        title = stringResource(R.string.documents),
                        onEdit = onEditDocuments,
                        content = {
                            DocumentStatusRow(
                                label = stringResource(R.string.aadhaar_front),
                                isUploaded = aadhaarFrontUploaded
                            )
                            DocumentStatusRow(
                                label = stringResource(R.string.aadhaar_back),
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
                        text = stringResource(R.string.edit),
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
                    contentDescription = stringResource(R.string.uploaded),
                    modifier = Modifier.size(20.dp),
                    tint = colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.uploaded),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.primary
                )
            } else {
                Text(
                    text = stringResource(R.string.not_uploaded),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
