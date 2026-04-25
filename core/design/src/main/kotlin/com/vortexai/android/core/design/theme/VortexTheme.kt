package com.vortexai.android.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Light theme colors
private val LightColorScheme = lightColorScheme(
    primary = VortexColors.Primary,
    onPrimary = VortexColors.OnPrimary,
    primaryContainer = VortexColors.PrimaryContainer,
    onPrimaryContainer = VortexColors.OnPrimaryContainer,
    
    secondary = VortexColors.Secondary,
    onSecondary = VortexColors.OnSecondary,
    secondaryContainer = VortexColors.SecondaryContainer,
    onSecondaryContainer = VortexColors.OnSecondaryContainer,
    
    tertiary = VortexColors.Tertiary,
    onTertiary = VortexColors.OnTertiary,
    tertiaryContainer = VortexColors.TertiaryContainer,
    onTertiaryContainer = VortexColors.OnTertiaryContainer,
    
    error = VortexColors.Error,
    onError = VortexColors.OnError,
    errorContainer = VortexColors.ErrorContainer,
    onErrorContainer = VortexColors.OnErrorContainer,
    
    background = VortexColors.Background,
    onBackground = VortexColors.OnBackground,
    surface = VortexColors.Surface,
    onSurface = VortexColors.OnSurface,
    surfaceVariant = VortexColors.SurfaceVariant,
    onSurfaceVariant = VortexColors.OnSurfaceVariant,
    
    outline = VortexColors.Outline,
    outlineVariant = VortexColors.OutlineVariant,
    scrim = VortexColors.Scrim,
    inverseSurface = VortexColors.InverseSurface,
    inverseOnSurface = VortexColors.InverseOnSurface,
    inversePrimary = VortexColors.InversePrimary,
    surfaceDim = VortexColors.SurfaceDim,
    surfaceBright = VortexColors.SurfaceBright,
    surfaceContainerLowest = VortexColors.SurfaceContainerLowest,
    surfaceContainerLow = VortexColors.SurfaceContainerLow,
    surfaceContainer = VortexColors.SurfaceContainer,
    surfaceContainerHigh = VortexColors.SurfaceContainerHigh,
    surfaceContainerHighest = VortexColors.SurfaceContainerHighest
)

// Dark theme colors
private val DarkColorScheme = darkColorScheme(
    primary = VortexColors.DarkPrimary,
    onPrimary = VortexColors.DarkOnPrimary,
    primaryContainer = VortexColors.DarkPrimaryContainer,
    onPrimaryContainer = VortexColors.DarkOnPrimaryContainer,
    
    secondary = VortexColors.DarkSecondary,
    onSecondary = VortexColors.DarkOnSecondary,
    secondaryContainer = VortexColors.DarkSecondaryContainer,
    onSecondaryContainer = VortexColors.DarkOnSecondaryContainer,
    
    tertiary = VortexColors.DarkTertiary,
    onTertiary = VortexColors.DarkOnTertiary,
    tertiaryContainer = VortexColors.DarkTertiaryContainer,
    onTertiaryContainer = VortexColors.DarkOnTertiaryContainer,
    
    error = VortexColors.DarkError,
    onError = VortexColors.DarkOnError,
    errorContainer = VortexColors.DarkErrorContainer,
    onErrorContainer = VortexColors.DarkOnErrorContainer,
    
    background = VortexColors.DarkBackground,
    onBackground = VortexColors.DarkOnBackground,
    surface = VortexColors.DarkSurface,
    onSurface = VortexColors.DarkOnSurface,
    surfaceVariant = VortexColors.DarkSurfaceVariant,
    onSurfaceVariant = VortexColors.DarkOnSurfaceVariant,
    
    outline = VortexColors.DarkOutline,
    outlineVariant = VortexColors.DarkOutlineVariant,
    scrim = VortexColors.DarkScrim,
    inverseSurface = VortexColors.DarkInverseSurface,
    inverseOnSurface = VortexColors.DarkInverseOnSurface,
    inversePrimary = VortexColors.DarkInversePrimary,
    surfaceDim = VortexColors.DarkSurfaceDim,
    surfaceBright = VortexColors.DarkSurfaceBright,
    surfaceContainerLowest = VortexColors.DarkSurfaceContainerLowest,
    surfaceContainerLow = VortexColors.DarkSurfaceContainerLow,
    surfaceContainer = VortexColors.DarkSurfaceContainer,
    surfaceContainerHigh = VortexColors.DarkSurfaceContainerHigh,
    surfaceContainerHighest = VortexColors.DarkSurfaceContainerHighest
)

/**
 * VortexAI Companion App Theme
 * 
 * Provides a consistent design system across the entire application.
 * Includes Material 3 theming with custom colors, typography, and spacing.
 */
@Composable
fun VortexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalSpacing provides VortexSpacing(),
        LocalElevation provides VortexElevation()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VortexTypography,
            shapes = VortexShapes,
            content = content
        )
    }
}

/**
 * Access to custom theme values
 */
object VortexTheme {
    val spacing: VortexSpacing
        @Composable get() = LocalSpacing.current
        
    val elevation: VortexElevation
        @Composable get() = LocalElevation.current
} 