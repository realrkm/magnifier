package com.magnifier.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LoupeCyan = Color(0xFF00E5FF)
val LoupeAmber = Color(0xFFFFB300)
val LoupeSurface = Color(0xFF10141A)
val LoupeSurfaceVariant = Color(0xFF1A2029)

private val MagnifierColorScheme = darkColorScheme(
    primary = LoupeCyan,
    onPrimary = Color(0xFF001F24),
    secondary = LoupeAmber,
    background = Color.Black,
    surface = LoupeSurface,
    surfaceVariant = LoupeSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun MagnifierTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MagnifierColorScheme,
        content = content
    )
}
