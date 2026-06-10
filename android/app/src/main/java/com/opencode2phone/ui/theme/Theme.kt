package com.opencode2phone.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = SecondaryTeal,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    onBackground = OnDarkText,
    onSurface = OnDarkText,
    onSurfaceVariant = OnDarkTextSecondary,
    error = ErrorRed,
    onError = androidx.compose.ui.graphics.Color.Black
)

@Composable
fun OpencodeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
