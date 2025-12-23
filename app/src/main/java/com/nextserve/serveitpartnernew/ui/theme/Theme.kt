package com.nextserve.serveitpartnernew.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryDarkMode,
    onPrimary = TextOnPrimaryDark,
    primaryContainer = BluePrimaryDarkModeVariant,
    onPrimaryContainer = TextPrimaryDark,
    secondary = TealSecondaryDarkMode,
    onSecondary = TextOnPrimaryDark,
    secondaryContainer = TealSecondaryDarkModeVariant,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = TealSecondaryDarkMode,
    onTertiary = TextOnPrimaryDark,
    tertiaryContainer = TealSecondaryDarkModeVariant,
    onTertiaryContainer = TextPrimaryDark,
    error = ErrorRedDark,
    onError = TextOnPrimaryDark,
    errorContainer = ErrorRedDarkVariant,
    onErrorContainer = TextPrimaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    outlineVariant = OutlineDark.copy(alpha = 0.5f),
    scrim = BackgroundDark.copy(alpha = 0.32f),
    inverseSurface = SurfaceLight,
    inverseOnSurface = TextPrimaryLight,
    inversePrimary = BluePrimary
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = TextOnPrimaryLight,
    primaryContainer = BluePrimaryLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = TealSecondary,
    onSecondary = TextOnPrimaryLight,
    secondaryContainer = TealSecondaryLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = TealSecondary,
    onTertiary = TextOnPrimaryLight,
    tertiaryContainer = TealSecondaryLight,
    onTertiaryContainer = TextPrimaryLight,
    error = ErrorRed,
    onError = TextOnPrimaryLight,
    errorContainer = ErrorRedLight,
    onErrorContainer = TextPrimaryLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    outlineVariant = OutlineLight.copy(alpha = 0.5f),
    scrim = BackgroundDark.copy(alpha = 0.32f),
    inverseSurface = SurfaceDark,
    inverseOnSurface = TextPrimaryDark,
    inversePrimary = BluePrimaryDarkMode
)

@Composable
fun ServeitPartnerNewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}