package com.tgserver.streamer.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppleDarkScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentBlue,
    tertiary = StatusGreen,
    background = BackgroundOled,
    surface = SurfaceCard,
    surfaceVariant = SurfaceCardElevated,
    onPrimary = Color.White,
    onSecondary = BackgroundOled,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle
)

@Composable
fun TGServerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppleDarkScheme,
        typography = Typography,
        content = content
    )
}
