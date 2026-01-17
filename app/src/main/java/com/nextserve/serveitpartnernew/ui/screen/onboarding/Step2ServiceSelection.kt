package com.nextserve.serveitpartnernew.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.ServiceSelector
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileMinimalTextField
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileSaveButton

@Composable
fun Step2ServiceSelection(
    primaryServiceName: String,
    availableSubServices: List<String>,
    selectedSubServices: Set<String>,
    isSelectAllChecked: Boolean,
    isLoadingSubServices: Boolean = false,
    otherService: String,
    onSubServiceToggle: (String) -> Unit,
    onSelectAllToggle: () -> Unit,
    onOtherServiceChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = stringResource(R.string.select_services_you_provide),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Subtitle
        Text(
            text = stringResource(R.string.based_on_service, primaryServiceName),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            color = colorScheme.onSurfaceVariant
        )

        // Primary Service Selector
        ServiceSelector(
            selectedService = primaryServiceName,
            services = listOf(primaryServiceName), // Display current selection
            onServiceSelected = { /* Read-only in this step */ },
            label = stringResource(R.string.primary_service),
            modifier = Modifier.fillMaxWidth()
        )

        // Select All / Deselect All
        if (availableSubServices.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Checkbox(
                    checked = isSelectAllChecked,
                    onCheckedChange = { onSelectAllToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = if (isSelectAllChecked) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface,
                    modifier = Modifier.clickable(onClick = onSelectAllToggle)
                )
            }
        }

        // Sub-services Grid
        if (isLoadingSubServices) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.loading_sub_services),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        } else if (availableSubServices.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Group services into rows of 2
                val serviceRows = availableSubServices.chunked(2)

                serviceRows.forEach { rowServices ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowServices.forEach { subServiceName ->
                            SubServiceCard(
                                subService = subServiceName,
                                isSelected = selectedSubServices.contains(subServiceName),
                                onToggle = { onSubServiceToggle(subServiceName) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Fill remaining space if odd number of items
                        repeat(2 - rowServices.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else if (primaryServiceName == "Other Services") {
            // Other Service Input
            ProfileMinimalTextField(
                value = otherService,
                onValueChange = onOtherServiceChange,
                label = "Specify Other Service",
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Error message display
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color(0xFFD32F2F), // Error red - same as Step 1
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Navigation Buttons - Previous & Next side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileSaveButton(
                text = "Previous",
                onClick = onPrevious,
                modifier = Modifier.weight(1f),
                showTrailingArrow = false
            )
            ProfileSaveButton(
                text = "Next",
                onClick = onNext,
                enabled = if (primaryServiceName == "Other Services") {
                    otherService.isNotEmpty()
                } else {
                    selectedSubServices.isNotEmpty()
                },
                modifier = Modifier.weight(1f),
                showTrailingArrow = true
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SubServiceCard(
    subService: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(
                color = if (isSelected) {
                    colorScheme.primaryContainer.copy(alpha = 0.2f)
                } else {
                    colorScheme.surface
                },
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    colorScheme.primary
                } else {
                    colorScheme.outline.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onToggle)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = colorScheme.primary
                )
            )
            Text(
                text = subService,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) {
                    colorScheme.primary
                } else {
                    colorScheme.onSurface
                },
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
