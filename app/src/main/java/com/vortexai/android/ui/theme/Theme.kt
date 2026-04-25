package com.vortexai.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// VortexAI Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = VortexPrimary,
    onPrimary = VortexOnPrimary,
    primaryContainer = VortexPrimaryContainer,
    onPrimaryContainer = VortexOnPrimaryContainer,
    secondary = VortexSecondary,
    onSecondary = VortexOnSecondary,
    secondaryContainer = VortexSecondaryContainer,
    onSecondaryContainer = VortexOnSecondaryContainer,
    tertiary = VortexTertiary,
    onTertiary = VortexOnTertiary,
    tertiaryContainer = VortexTertiaryContainer,
    onTertiaryContainer = VortexOnTertiaryContainer,
    error = VortexError,
    onError = VortexOnError,
    errorContainer = VortexErrorContainer,
    onErrorContainer = VortexOnErrorContainer,
    background = VortexBackground,
    onBackground = VortexOnBackground,
    surface = VortexSurface,
    onSurface = VortexOnSurface,
    surfaceVariant = VortexSurfaceVariant,
    onSurfaceVariant = VortexOnSurfaceVariant,
    outline = VortexOutline,
    outlineVariant = VortexOutlineVariant,
)

// VortexAI Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = VortexPrimary,
    onPrimary = VortexOnPrimary,
    primaryContainer = VortexPrimaryContainer.copy(alpha = 0.7f),
    onPrimaryContainer = VortexOnPrimaryContainer,
    secondary = VortexSecondary,
    onSecondary = VortexOnSecondary,
    secondaryContainer = VortexSecondaryContainer.copy(alpha = 0.7f),
    onSecondaryContainer = VortexOnSecondaryContainer,
    tertiary = VortexTertiary,
    onTertiary = VortexOnTertiary,
    tertiaryContainer = VortexTertiaryContainer.copy(alpha = 0.7f),
    onTertiaryContainer = VortexOnTertiaryContainer,
    error = VortexError,
    onError = VortexOnError,
    errorContainer = VortexErrorContainer.copy(alpha = 0.7f),
    onErrorContainer = VortexOnErrorContainer,
    background = androidx.compose.ui.graphics.Color(0xFF121212),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE1E1E1),
    surface = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE1E1E1),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2D2D2D),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB8B8B8),
    outline = androidx.compose.ui.graphics.Color(0xFF404040),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF505050),
)

@Composable
fun VortexAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: String? = null,
    content: @Composable () -> Unit
) {
    // Base scheme
    var colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Apply accent overrides based on themeColor selection
    themeColor?.let { selected ->
        val name = selected.lowercase()
        val (primary, secondary, tertiary) = when (name) {
            "blue" -> Triple(Color(0xFF3B82F6), Color(0xFF60A5FA), Color(0xFF2563EB))
            "purple" -> Triple(Color(0xFF8B5CF6), Color(0xFFA78BFA), Color(0xFF7C3AED))
            "green" -> Triple(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF059669))
            "orange" -> Triple(Color(0xFFF59E0B), Color(0xFFFBBF24), Color(0xFFD97706))
            "red" -> Triple(Color(0xFFEF4444), Color(0xFFF87171), Color(0xFFDC2626))
            "pink" -> Triple(Color(0xFFEC4899), Color(0xFFF472B6), Color(0xFFDB2777))
            else -> Triple(VortexPrimary, VortexSecondary, VortexTertiary)
        }
        colorScheme = colorScheme.copy(
            primary = primary,
            primaryContainer = primary.copy(alpha = if (darkTheme) 0.7f else 0.15f),
            secondary = secondary,
            secondaryContainer = secondary.copy(alpha = if (darkTheme) 0.7f else 0.15f),
            tertiary = tertiary,
            tertiaryContainer = tertiary.copy(alpha = if (darkTheme) 0.7f else 0.15f)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VortexTypography,
        shapes = VortexShapes,
        content = content
    )
}