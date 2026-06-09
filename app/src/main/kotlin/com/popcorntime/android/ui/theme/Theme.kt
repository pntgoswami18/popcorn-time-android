package com.popcorntime.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.popcorntime.android.data.preferences.ThemeMode

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

private val PopcornLight = lightColorScheme(
    primary = Color(0xFFB87800),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF7B4FC4),
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8E8E8),
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF555555),
    error = Color(0xFFB00020),
)

@Composable
fun PopcornTimeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) PopcornDark else PopcornLight,
        typography = PopcornTypography,
        content = content,
    )
}
