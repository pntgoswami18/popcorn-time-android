package com.popcorntime.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PopcornDark = darkColorScheme(
    primary = Color(0xFFE5A00D),          // popcorn amber
    onPrimary = Color(0xFF1A1A1A),
    secondary = Color(0xFFBB86FC),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2A2A2A),
    onBackground = Color(0xFFEEEEEE),
    onSurface = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFFAAAAAA),
    error = Color(0xFFCF6679),
)

@Composable
fun PopcornTimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PopcornDark,
        typography = PopcornTypography,
        content = content,
    )
}
