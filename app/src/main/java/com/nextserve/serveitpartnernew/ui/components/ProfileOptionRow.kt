package com.nextserve.serveitpartnernew.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun ProfileOptionRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    onClick: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                thickness = 1.dp,
                modifier = Modifier.padding(start = 56.dp)
            )
        }
    }
}

// Icon mapping helper - using available Material icons
object ProfileIcons {
    val PersonalInformation = Icons.Outlined.AccountCircle
    val ServiceInformation = Icons.Outlined.Build
    val AddressInformation = Icons.Outlined.LocationOn
    val Documents = Icons.Default.Settings  // Using Settings as placeholder
    val Language = Icons.Default.Settings    // Using Settings as placeholder
    val Notifications = Icons.Outlined.Notifications
    val DisplayTheme = Icons.Default.Settings  // Using Settings as placeholder
    val HelpSupport = Icons.Default.Info    // Using Info as placeholder
    val AboutApp = Icons.Default.Info
}
