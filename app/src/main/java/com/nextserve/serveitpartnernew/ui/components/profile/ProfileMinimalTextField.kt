package com.nextserve.serveitpartnernew.ui.components.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Minimal text field component extracted from Edit Basic Info screen.
 * Reused across all Profile-related screens for consistency.
 */
@Composable
fun ProfileMinimalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Label ABOVE field (ALL CAPS, small, light gray)
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            ),
            color = Color(0xFF9E9E9E),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Tall field with rounded corners, subtle border
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (singleLine) Modifier.height(68.dp) else Modifier),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF1C1C1C),
                unfocusedTextColor = Color(0xFF1C1C1C),
                focusedBorderColor = Color(0xFF2196F3), // Primary blue when focused
                unfocusedBorderColor = Color(0xFFE0E0E0), // Light gray border
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Color(0xFF2196F3)
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

