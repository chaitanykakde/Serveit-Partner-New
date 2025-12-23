package com.nextserve.serveitpartnernew.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.nextserve.serveitpartnernew.ui.theme.InputFieldShape

@Composable
fun OutlinedInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = if (label.isNotEmpty()) { { Text(label) } } else null,
        placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder) } } else null,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage) }
        } else null,
        enabled = enabled,
        singleLine = singleLine,
        shape = InputFieldShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colorScheme.onSurface,
            unfocusedTextColor = colorScheme.onSurface,
            disabledTextColor = colorScheme.onSurface.copy(alpha = 0.38f),
            focusedLabelColor = colorScheme.primary,
            unfocusedLabelColor = colorScheme.onSurfaceVariant,
            disabledLabelColor = colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            focusedPlaceholderColor = colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = colorScheme.onSurfaceVariant,
            disabledPlaceholderColor = colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            focusedBorderColor = colorScheme.primary,
            unfocusedBorderColor = colorScheme.outline,
            disabledBorderColor = colorScheme.outline.copy(alpha = 0.12f),
            errorBorderColor = colorScheme.error,
            errorLabelColor = colorScheme.error,
            errorSupportingTextColor = colorScheme.error,
            focusedContainerColor = colorScheme.surface,
            unfocusedContainerColor = colorScheme.surface,
            disabledContainerColor = colorScheme.surface.copy(alpha = 0.12f),
            errorContainerColor = colorScheme.surface
        )
    )
}

