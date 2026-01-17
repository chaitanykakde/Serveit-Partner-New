package com.nextserve.serveitpartnernew.ui.onboarding.step4

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DocumentActionRow(
    isUploaded: Boolean,
    isUploading: Boolean,
    onUpload: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    if (isUploaded) {
        // Show Replace and Remove buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onReplace,
                enabled = !isUploading,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colorScheme.onSurface
                )
            ) {
                Text("Replace")
            }

            Button(
                onClick = onRemove,
                enabled = !isUploading,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Remove")
            }
        }
    } else {
        // Show Upload button
        Button(
            onClick = onUpload,
            enabled = !isUploading,
            modifier = modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary
            )
        ) {
            if (isUploading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Upload")
            }
        }
    }
}
