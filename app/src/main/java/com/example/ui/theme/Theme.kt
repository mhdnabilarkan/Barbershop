package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BarbershopDarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    secondary = BrassAccent,
    tertiary = GoldAccentLight,
    background = DarkBg,
    surface = DarkSurface,
    onPrimary = DarkBg,
    onSecondary = TextWhite,
    onTertiary = DarkBg,
    onBackground = TextWhite,
    onSurface = TextWhite,
    surfaceVariant = LightSurface,
    onSurfaceVariant = TextGray,
    error = SignalRed,
    onError = Color.Black
)

@Composable
fun MyApplicationTheme(
    // We enforce the premium dark barbershop theme as it sets the specific brand atmosphere (as requested)
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BarbershopDarkColorScheme,
        typography = Typography,
        content = content
    )
}
