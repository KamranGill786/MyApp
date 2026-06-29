package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HighDensityColorScheme = darkColorScheme(
    primary = LavLight,
    onPrimary = LavDark,
    primaryContainer = LavContainer,
    onPrimaryContainer = LavOnContainer,
    secondary = LavLight,
    onSecondary = LavDark,
    background = SlateDarkBg,
    onBackground = TextPrimary,
    surface = SlateCardBg,
    onSurface = TextPrimary,
    surfaceVariant = SlateBorder,
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force professional dark mode dashboard by default
    dynamicColor: Boolean = false, // Disable dynamic color to enforce our precise toolkit branding
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = HighDensityColorScheme,
        typography = Typography,
        content = content
    )
}
