package com.nextserve.serveitpartnernew.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nextserve.serveitpartnernew.ui.theme.InputFieldShape

@Composable
fun ServiceSelector(
    selectedService: String,
    services: List<String>,
    onServiceSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Primary Service",
    placeholder: String = "Select primary service"
) {
    val colorScheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedService,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            enabled = false,
            shape = InputFieldShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colorScheme.onSurface,
                unfocusedTextColor = colorScheme.onSurface,
                disabledTextColor = colorScheme.onSurface,
                focusedLabelColor = colorScheme.primary,
                unfocusedLabelColor = colorScheme.onSurfaceVariant,
                disabledLabelColor = colorScheme.onSurfaceVariant,
                focusedPlaceholderColor = colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                focusedBorderColor = colorScheme.primary,
                unfocusedBorderColor = colorScheme.outline,
                disabledBorderColor = colorScheme.outline,
                focusedContainerColor = colorScheme.surface,
                unfocusedContainerColor = colorScheme.surface,
                disabledContainerColor = colorScheme.surface
            )
        )

        // Invisible clickable overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            services.forEach { service ->
                DropdownMenuItem(
                    text = { Text(service) },
                    onClick = {
                        onServiceSelected(service)
                        expanded = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

