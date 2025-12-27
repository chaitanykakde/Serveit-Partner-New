package com.nextserve.serveitpartnernew.ui.screen.onboarding

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nextserve.serveitpartnernew.R
import com.nextserve.serveitpartnernew.ui.components.BottomStickyButtonContainer
import com.nextserve.serveitpartnernew.ui.components.OutlinedInputField
import com.nextserve.serveitpartnernew.ui.components.PrimaryButton
import com.nextserve.serveitpartnernew.ui.components.SecondaryButton
import com.nextserve.serveitpartnernew.ui.theme.CardShape

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                // Header content (fixed at top)
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Title
                    Text(
                        text = stringResource(R.string.select_services_you_provide),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    // Subtitle
                    Text(
                        text = stringResource(R.string.based_on_service, primaryServiceName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Select All / Deselect All
                    if (availableSubServices.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelectAllChecked,
                                    onCheckedChange = { onSelectAllToggle() }
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = if (isSelectAllChecked) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(onClick = onSelectAllToggle)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Sub-services Grid - Below header, scrollable
                if (isLoadingSubServices) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 180.dp),
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
                        columns = GridCells.Fixed(columns),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            top = 180.dp, // Approximate header height
                            bottom = 8.dp
                        ),
                        modifier = Modifier.fillMaxSize()
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
                            .padding(top = 180.dp)
                    ) {
                        OutlinedInputField(
                            value = otherService,
                            onValueChange = onOtherServiceChange,
                            label = stringResource(R.string.specify_other_service),
                            placeholder = stringResource(R.string.enter_service_name),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onToggle),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                colorScheme.primary
            } else {
                colorScheme.outline.copy(alpha = 0.5f)
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subService.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        colorScheme.onPrimaryContainer
                    } else {
                        colorScheme.onSurface
                    },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
