package com.yourcompany.pawplay.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PawPrimary,
    onPrimary = PawOnPrimary,
    secondary = PawSecondary,
    background = PawBackground,
    surface = PawSurface,
    onBackground = PawOnBackground,
    onSurface = PawOnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = PawPrimary,
    onPrimary = PawOnPrimary,
    secondary = PawSecondary,
    background = PawDarkBackground,
    surface = PawDarkSurface,
    onBackground = PawDarkOnBackground,
    onSurface = PawDarkOnBackground
)

@Composable
fun PawPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PawTypography,
        content = content
    )
}
