package com.nextserve.serveitpartnernew.ui.screen.onboarding

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.BottomStickyButtonContainer
import com.nextserve.serveitpartnernew.ui.components.OutlinedInputField
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.components.SecondaryButton
import com.nextserve.serveitpartnernew.ui.theme.CardShape
import com.nextserve.serveitpartnernew.ui.util.Dimens

@Composable
fun Step2ServiceSelection(
    primaryServiceName: String,
    availableSubServices: List<com.nextserve.serveitpartnernew.data.model.SubServiceModel>,
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
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val columns = if (isTablet) 3 else 2

    BottomStickyButtonContainer(
        button = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryButton(
                    text = stringResource(R.string.previous),
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = stringResource(R.string.next),
                    onClick = onNext,
                    enabled = if (primaryServiceName == "Other Services") {
                        otherService.isNotEmpty()
                    } else {
                        selectedSubServices.isNotEmpty()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Dimens.paddingLg)
            ) {
                Spacer(modifier = Modifier.height(Dimens.spacingMd))
                
                // Title - Large and Bold
                Text(
                    text = stringResource(R.string.select_services_you_provide),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Dimens.spacingXs)
                )

                // Subtitle
                Text(
                    text = stringResource(R.string.based_on_service, primaryServiceName),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimens.spacingLg)
                )

                // Select All / Deselect All
                if (availableSubServices.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Dimens.spacingMd),
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
                        Spacer(modifier = Modifier.size(Dimens.spacingSm))
                        Text(
                            text = if (isSelectAllChecked) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface,
                            modifier = Modifier.clickable(onClick = onSelectAllToggle)
                        )
                    }
                }

                // Sub-services Grid - Scrollable
                if (isLoadingSubServices) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.loading_sub_services),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                } else if (availableSubServices.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd),
                        contentPadding = PaddingValues(
                            bottom = Dimens.spacingXl
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        items(availableSubServices) { subService ->
                            SubServiceCard(
                                subService = subService,
                                isSelected = selectedSubServices.contains(subService.name),
                                onToggle = { onSubServiceToggle(subService.name) }
                            )
                        }
                    }
                } else if (primaryServiceName == "Other Services") {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        OutlinedInputField(
                            value = otherService,
                            onValueChange = onOtherServiceChange,
                            label = stringResource(R.string.specify_other_service),
                            placeholder = stringResource(R.string.enter_service_name),
                            modifier = Modifier.fillMaxWidth(),
                            isError = errorMessage != null && otherService.isEmpty(),
                            errorMessage = if (errorMessage != null && otherService.isEmpty()) errorMessage else null
                        )
                    }
                }
                
                // Error message display
                if (errorMessage != null && primaryServiceName != "Other Services") {
                    Spacer(modifier = Modifier.height(Dimens.spacingSm))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.paddingXs)
                    )
                }
            }
        }
    )
}

@Composable
private fun SubServiceCard(
    subService: com.nextserve.serveitpartnernew.data.model.SubServiceModel,
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
            .padding(Dimens.spacingMd),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
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
                text = subService.name,
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
