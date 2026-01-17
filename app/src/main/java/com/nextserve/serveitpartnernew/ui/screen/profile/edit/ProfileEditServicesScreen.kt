package com.nextserve.serveitpartnernew.ui.screen.profile.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nextserve.serveitpartnernew.data.firebase.FirebaseProvider
import android.util.Log
import java.io.File
import java.io.FileWriter
import com.nextserve.serveitpartnernew.ui.components.ServiceSelector
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileEditScreenLayout
import com.nextserve.serveitpartnernew.ui.components.profile.ProfileSaveButton
import com.nextserve.serveitpartnernew.ui.viewmodel.ProfileEditViewModel

@Composable
fun ProfileEditServicesScreen(
    navController: NavController,
    parentPaddingValues: PaddingValues = PaddingValues()
) {
    val uid = FirebaseProvider.auth.currentUser?.uid ?: return
    val context = LocalContext.current
    val viewModel: ProfileEditViewModel = viewModel(
        factory = ProfileEditViewModel.factory(context, uid)
    )
    val state = viewModel.uiState

    var gender by rememberSaveable { mutableStateOf("male") }
    var mainService by rememberSaveable { mutableStateOf("") }
    // ID-BASED SELECTION: Set<String> using subservice names as IDs
    // NEVER use index-based state (List<Boolean>, mutableStateListOf, etc.)
    var selectedSubServiceIds by rememberSaveable { mutableStateOf(setOf<String>()) }

    LaunchedEffect(state.providerData) {
        state.providerData?.let {
            gender = if (it.gender.isNotEmpty()) it.gender else gender
            mainService = it.selectedMainService
            viewModel.loadMainServices(gender)
            if (mainService.isNotEmpty()) {
                viewModel.loadSubServices(gender, mainService)
            }
        }
    }

    LaunchedEffect(state.mainServices) {
        if (mainService.isEmpty() && state.mainServices.isNotEmpty()) {
            mainService = state.mainServices.first().name
            viewModel.loadSubServices(gender, mainService)
        }
    }

    // SINGLE SOURCE OF TRUTH: Load selection from Firestore when both subservices and data are available
    // ID-BASED: Uses subservice names as stable identifiers (NOT index-based)
    // Key: Use mainService + subServices list hash + providerData hash to ensure reload when data changes
    // This ensures index 0 and all other items are correctly preserved
    LaunchedEffect(
        key1 = mainService,
        key2 = state.subServices.joinToString(","),
        key3 = state.providerData?.selectedSubServices?.joinToString(",")
    ) {
        // #region agent log
        try {
            val logDir = File(context.filesDir, "debug_logs")
            if (!logDir.exists()) logDir.mkdirs()
            val logFile = File(logDir, "debug.log")
            val logJson = """{"hypothesisId":"A,C","location":"ProfileEditServicesScreen.kt:70","message":"LaunchedEffect triggered","data":{"mainService":"$mainService","subServicesCount":${state.subServices.size},"providerDataExists":${state.providerData != null},"providerMainService":"${state.providerData?.selectedMainService ?: "null"}","subServicesList":${state.subServices.joinToString("\",\"", "[\"", "\"]")},"savedSubServicesList":${(state.providerData?.selectedSubServices ?: emptyList()).joinToString("\",\"", "[\"", "\"]")}},"timestamp":${System.currentTimeMillis()},"sessionId":"debug-session","runId":"run1"}""" + "\n"
            FileWriter(logFile, true).use { it.append(logJson) }
            Log.d("DebugLog", "LaunchedEffect triggered: mainService=$mainService, subServicesCount=${state.subServices.size}, savedCount=${state.providerData?.selectedSubServices?.size ?: 0}")
        } catch (e: Exception) { Log.e("DebugLog", "Failed to write log", e) }
        // #endregion
        
        // Only load selection if:
        // 1. We have subservices loaded
        // 2. We have provider data
        // 3. Main service matches (prevents loading wrong service's selection)
        val hasSubServices = state.subServices.isNotEmpty()
        val hasProviderData = state.providerData != null
        val mainServiceMatches = mainService == state.providerData?.selectedMainService
        val mainServiceNotEmpty = mainService.isNotEmpty()
        
        // #region agent log
        try {
            val logDir = File(context.filesDir, "debug_logs")
            if (!logDir.exists()) logDir.mkdirs()
            val logFile = File(logDir, "debug.log")
            val willLoad = hasSubServices && hasProviderData && mainServiceMatches && mainServiceNotEmpty
            val logJson = """{"hypothesisId":"A","location":"ProfileEditServicesScreen.kt:95","message":"Condition check before selection load","data":{"hasSubServices":$hasSubServices,"hasProviderData":$hasProviderData,"mainServiceMatches":$mainServiceMatches,"mainServiceNotEmpty":$mainServiceNotEmpty,"willLoadSelection":$willLoad},"timestamp":${System.currentTimeMillis()},"sessionId":"debug-session","runId":"run1"}""" + "\n"
            FileWriter(logFile, true).use { it.append(logJson) }
            Log.d("DebugLog", "Condition check: willLoad=$willLoad, mainServiceMatches=$mainServiceMatches, mainService='$mainService', providerMainService='${state.providerData?.selectedMainService}'")
        } catch (e: Exception) { Log.e("DebugLog", "Failed to write log", e) }
        // #endregion
        
        if (hasSubServices && hasProviderData && mainServiceMatches && mainServiceNotEmpty) {
            
            val savedSubServiceNames = state.providerData.selectedSubServices.toSet()
            val loadedSubServiceNames = state.subServices.toSet()
            
            // #region agent log
            try {
                val logDir = File(context.filesDir, "debug_logs")
                if (!logDir.exists()) logDir.mkdirs()
                val logFile = File(logDir, "debug.log")
                val savedIndex0 = state.providerData.selectedSubServices.getOrNull(0) ?: "null"
                val loadedIndex0 = state.subServices.getOrNull(0) ?: "null"
                val savedIndex0InSet = savedSubServiceNames.contains(savedIndex0)
                val loadedIndex0InSet = loadedSubServiceNames.contains(loadedIndex0)
                val logJson = """{"hypothesisId":"B","location":"ProfileEditServicesScreen.kt:110","message":"Before intersect - saved vs loaded","data":{"savedSubServiceNames":${savedSubServiceNames.joinToString("\",\"", "[\"", "\"]")},"loadedSubServiceNames":${loadedSubServiceNames.joinToString("\",\"", "[\"", "\"]")},"savedIndex0":"$savedIndex0","loadedIndex0":"$loadedIndex0","savedIndex0InSet":$savedIndex0InSet,"loadedIndex0InSet":$loadedIndex0InSet},"timestamp":${System.currentTimeMillis()},"sessionId":"debug-session","runId":"run1"}""" + "\n"
                FileWriter(logFile, true).use { it.append(logJson) }
                Log.d("DebugLog", "Before intersect: savedIndex0='$savedIndex0', loadedIndex0='$loadedIndex0', savedInSet=$savedIndex0InSet, loadedInSet=$loadedIndex0InSet")
            } catch (e: Exception) { Log.e("DebugLog", "Failed to write log", e) }
            // #endregion
            
            // ID-BASED MATCHING: Match saved names (IDs) with loaded names (IDs)
            // Only include subservices that exist in both saved and loaded lists
            // This ensures we only select subservices that are actually available
            // CRITICAL: intersect() preserves all matching items including index 0
            val matchedIds = savedSubServiceNames.intersect(loadedSubServiceNames)
            
            // #region agent log
            try {
                val logDir = File(context.filesDir, "debug_logs")
                if (!logDir.exists()) logDir.mkdirs()
                val logFile = File(logDir, "debug.log")
                val index0Name = state.subServices.getOrNull(0) ?: "null"
                val index0Matched = matchedIds.contains(index0Name)
                val logJson = """{"hypothesisId":"B","location":"ProfileEditServicesScreen.kt:130","message":"After intersect - matched IDs","data":{"matchedIds":${matchedIds.joinToString("\",\"", "[\"", "\"]")},"matchedCount":${matchedIds.size},"savedCount":${savedSubServiceNames.size},"loadedCount":${loadedSubServiceNames.size},"index0Matched":$index0Matched,"index0Name":"$index0Name"},"timestamp":${System.currentTimeMillis()},"sessionId":"debug-session","runId":"run1"}""" + "\n"
                FileWriter(logFile, true).use { it.append(logJson) }
                Log.d("DebugLog", "After intersect: matchedCount=${matchedIds.size}, index0Matched=$index0Matched, index0Name='$index0Name', matchedIds=${matchedIds.toList()}")
            } catch (e: Exception) { Log.e("DebugLog", "Failed to write log", e) }
            // #endregion
            
            // Update selection with matched IDs (all items including index 0 will be preserved)
            val previousSelection = selectedSubServiceIds
            selectedSubServiceIds = matchedIds
            
            // #region agent log
            try {
                val logDir = File(context.filesDir, "debug_logs")
                if (!logDir.exists()) logDir.mkdirs()
                val logFile = File(logDir, "debug.log")
                val index0InNewSelection = selectedSubServiceIds.contains(state.subServices.getOrNull(0))
                val logJson = """{"hypothesisId":"E","location":"ProfileEditServicesScreen.kt:150","message":"Selection state updated","data":{"previousSelection":${previousSelection.joinToString("\",\"", "[\"", "\"]")},"newSelection":${selectedSubServiceIds.joinToString("\",\"", "[\"", "\"]")},"index0InNewSelection":$index0InNewSelection},"timestamp":${System.currentTimeMillis()},"sessionId":"debug-session","runId":"run1"}""" + "\n"
                FileWriter(logFile, true).use { it.append(logJson) }
                Log.d("DebugLog", "Selection updated: index0InNewSelection=$index0InNewSelection, newSelection=${selectedSubServiceIds.toList()}")
            } catch (e: Exception) { Log.e("DebugLog", "Failed to write log", e) }
            // #endregion
        }
    }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            navController.popBackStack()
        }
    }

    ProfileEditScreenLayout(
        navController = navController,
        title = "Edit Services",
        subtitle = "Update the services you offer",
        parentPaddingValues = parentPaddingValues
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            ServiceSelector(
                selectedService = mainService,
                services = state.mainServices.map { it.name },
                onServiceSelected = {
                    // #region agent log
                    try {
                        val logDir = File(context.filesDir, "debug_logs")
                        if (!logDir.exists()) logDir.mkdirs()
                        val logFile = File(logDir, "debug.log")
                        val logJson = """{"hypothesisId":"E","location":"ProfileEditServicesScreen.kt:118","message":"Service changed - clearing selection","data":{"oldMainService":"$mainService","newMainService":"$it","selectionBeforeClear":${selectedSubServiceIds.joinToString("\",\"", "[\"", "\"]")}},"timestamp":${System.currentTimeMillis()},"sessionId":"debug-session","runId":"run1"}""" + "\n"
                        FileWriter(logFile, true).use { it.append(logJson) }
                        Log.d("DebugLog", "Service changed: old=$mainService, new=$it, clearing selection")
                    } catch (e: Exception) { Log.e("DebugLog", "Failed to write log", e) }
                    // #endregion
                    mainService = it
                    viewModel.loadSubServices(gender, it)
                    // Clear selection when switching services (user intent)
                    selectedSubServiceIds = emptySet()
                },
                label = "Primary service"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Sub-services",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (state.subServices.isEmpty()) {
                    Text(
                        text = "Select a primary service to see sub-services",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // ID-BASED CHECKBOX: Uses subservice name (ID) for selection matching
                    // NEVER uses index - selection is independent of list position
                    state.subServices.forEachIndexed { index, subServiceName ->
                        val isChecked = selectedSubServiceIds.contains(subServiceName)
                        // #region agent log
                        if (index == 0) {
                            try {
                                val logDir = File(context.filesDir, "debug_logs")
                                if (!logDir.exists()) logDir.mkdirs()
                                val logFile = File(logDir, "debug.log")
                                val logJson = """{"hypothesisId":"B","location":"ProfileEditServicesScreen.kt:142","message":"Rendering checkbox for index 0","data":{"index":$index,"subServiceName":"$subServiceName","isChecked":$isChecked,"selectedSubServiceIds":${selectedSubServiceIds.joinToString("\",\"", "[\"", "\"]")},"containsCheck":${selectedSubServiceIds.contains(subServiceName)}},"timestamp":${System.currentTimeMillis()},"sessionId":"debug-session","runId":"run1"}""" + "\n"
                                FileWriter(logFile, true).use { it.append(logJson) }
                                Log.d("DebugLog", "Rendering index 0: name='$subServiceName', isChecked=$isChecked, contains=${selectedSubServiceIds.contains(subServiceName)}, selection=${selectedSubServiceIds.toList()}")
                            } catch (e: Exception) { Log.e("DebugLog", "Failed to write log", e) }
                        }
                        // #endregion
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = subServiceName, fontWeight = FontWeight.Medium)
                            }
                            Checkbox(
                                // ID-BASED CHECK: Check by name (ID), NOT by index
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    // #region agent log
                                    if (index == 0) {
                                        try {
                                            val logDir = File(context.filesDir, "debug_logs")
                                            if (!logDir.exists()) logDir.mkdirs()
                                            val logFile = File(logDir, "debug.log")
                                            val logJson = """{"hypothesisId":"E","location":"ProfileEditServicesScreen.kt:165","message":"Index 0 checkbox changed","data":{"index":$index,"subServiceName":"$subServiceName","checked":$checked,"selectionBefore":${selectedSubServiceIds.joinToString("\",\"", "[\"", "\"]")}},"timestamp":${System.currentTimeMillis()},"sessionId":"debug-session","runId":"run1"}""" + "\n"
                                            FileWriter(logFile, true).use { it.append(logJson) }
                                            Log.d("DebugLog", "Index 0 checkbox changed: checked=$checked, name='$subServiceName'")
                                        } catch (e: Exception) { Log.e("DebugLog", "Failed to write log", e) }
                                    }
                                    // #endregion
                                    // ID-BASED UPDATE: Add/remove by name (ID)
                                    selectedSubServiceIds = if (checked) {
                                        selectedSubServiceIds + subServiceName
                                    } else {
                                        selectedSubServiceIds - subServiceName
                                    }
                                }
                            )
                        }
                    }
                }
            }

            ProfileSaveButton(
                text = if (state.isSaving) "Saving..." else "Save services",
                isLoading = state.isSaving,
                enabled = !state.isSaving && mainService.isNotEmpty(),
                onClick = {
                    // ID-BASED SAVE: Save selected subservices by ID (name)
                    // Filter all subservices to only include selected ones by ID match
                    val selectedSubServices = state.subServices.filter { subServiceName ->
                        selectedSubServiceIds.contains(subServiceName)
                    }
                    viewModel.updateServices(
                        gender = gender,
                        mainService = mainService,
                        subServices = selectedSubServices
                    )
                }
            )

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

