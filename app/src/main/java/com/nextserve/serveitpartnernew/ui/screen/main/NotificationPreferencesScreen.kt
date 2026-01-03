package com.nextserve.serveitpartnernew.ui.screen.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
// import androidx.compose.material.icons.filled.VolumeUp // Temporarily disabled
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import com.nextserve.serveitpartnernew.ui.viewmodel.NotificationPreferencesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPreferencesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    providerId: String = FirebaseProvider.auth.currentUser?.uid ?: "",
    viewModel: NotificationPreferencesViewModel = viewModel(
        factory = NotificationPreferencesViewModel.factory(providerId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("Preferences saved successfully")
            viewModel.clearSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Preferences") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                text = "Push Notifications",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Enable or disable all push notifications",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Switch(
                        checked = uiState.preferences.pushNotificationsEnabled,
                        onCheckedChange = { viewModel.updatePushNotificationsEnabled(it) }
                    )
                }
            }

            // Notification Types
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Notification Types",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )

                    HorizontalDivider()

                    NotificationPreferenceRow(
                        title = "Job Offers",
                        description = "New service requests in your area",
                        checked = uiState.preferences.jobOffers,
                        enabled = uiState.preferences.pushNotificationsEnabled,
                        onCheckedChange = { viewModel.updateJobOffers(it) }
                    )

                    NotificationPreferenceRow(
                        title = "Job Updates",
                        description = "Status changes for your accepted jobs",
                        checked = uiState.preferences.jobUpdates,
                        enabled = uiState.preferences.pushNotificationsEnabled,
                        onCheckedChange = { viewModel.updateJobUpdates(it) }
                    )

                    NotificationPreferenceRow(
                        title = "Earnings Summary",
                        description = "Daily and weekly earnings reports",
                        checked = uiState.preferences.earningsSummary,
                        enabled = uiState.preferences.pushNotificationsEnabled,
                        onCheckedChange = { viewModel.updateEarningsSummary(it) }
                    )

                    NotificationPreferenceRow(
                        title = "Verification Updates",
                        description = "Profile verification status changes",
                        checked = uiState.preferences.verificationUpdates,
                        enabled = uiState.preferences.pushNotificationsEnabled,
                        onCheckedChange = { viewModel.updateVerificationUpdates(it) }
                    )

                    NotificationPreferenceRow(
                        title = "Promotional Offers",
                        description = "Marketing and special offers",
                        checked = uiState.preferences.promotionalOffers,
                        enabled = uiState.preferences.pushNotificationsEnabled,
                        onCheckedChange = { viewModel.updatePromotionalOffers(it) }
                    )
                }
            }

            // Sound & Vibration
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Sound & Vibration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )

                    HorizontalDivider()

                    NotificationPreferenceRow(
                        title = "Sound",
                        description = "Play notification sound",
                        checked = uiState.preferences.soundEnabled,
                        enabled = uiState.preferences.pushNotificationsEnabled,
                        onCheckedChange = { viewModel.updateSoundEnabled(it) }
                    )

                    NotificationPreferenceRow(
                        title = "Vibration",
                        description = "Vibrate on notifications",
                        checked = uiState.preferences.vibrationEnabled,
                        enabled = uiState.preferences.pushNotificationsEnabled,
                        onCheckedChange = { viewModel.updateVibrationEnabled(it) }
                    )
                }
            }

            // Quiet Hours
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Quiet Hours",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                            Text(
                                text = "Pause notifications during specified hours",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.preferences.quietHoursEnabled,
                            enabled = uiState.preferences.pushNotificationsEnabled,
                            onCheckedChange = { viewModel.updateQuietHoursEnabled(it) }
                        )
                    }

                    if (uiState.preferences.quietHoursEnabled) {
                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimePickerField(
                                label = "Start Time",
                                time = uiState.preferences.quietHoursStart,
                                enabled = uiState.preferences.quietHoursEnabled && uiState.preferences.pushNotificationsEnabled,
                                onTimeChange = { viewModel.updateQuietHoursStart(it) },
                                modifier = Modifier.weight(1f)
                            )

                            TimePickerField(
                                label = "End Time",
                                time = uiState.preferences.quietHoursEnd,
                                enabled = uiState.preferences.quietHoursEnabled && uiState.preferences.pushNotificationsEnabled,
                                onTimeChange = { viewModel.updateQuietHoursEnd(it) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = { viewModel.savePreferences() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                Text(
                    text = if (uiState.isSaving) "Saving..." else "Save Preferences"
                )
            }
        }
    }
}

@Composable
private fun NotificationPreferenceRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerField(
    label: String,
    time: String,
    enabled: Boolean,
    onTimeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        androidx.compose.material3.OutlinedTextField(
            value = time,
            onValueChange = { },
            label = { Text(label) },
            enabled = enabled,
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // TODO: Implement time picker dialog
    // For now, this is a placeholder - in production, implement a proper time picker
}
