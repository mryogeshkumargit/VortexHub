package com.vortexai.android.core.design.theme

import androidx.compose.ui.graphics.Color

/**
 * VortexAI Color Palette
 * 
 * Defines the complete color system for the companion app.
 * Colors are designed to work well in both light and dark themes.
 */
object VortexColors {
    // Brand Colors
    val VortexPurple = Color(0xFF6B46C1)
    val VortexBlue = Color(0xFF3B82F6)
    val VortexTeal = Color(0xFF14B8A6)
    val VortexGreen = Color(0xFF10B981)
    val VortexAmber = Color(0xFFF59E0B)
    val VortexRed = Color(0xFFEF4444)
    
    // Light Theme Colors
    val Primary = VortexPurple
    val OnPrimary = Color.White
    val PrimaryContainer = Color(0xFFE9D5FF)
    val OnPrimaryContainer = Color(0xFF2D1B69)
    
    val Secondary = VortexBlue
    val OnSecondary = Color.White
    val SecondaryContainer = Color(0xFFDBEAFE)
    val OnSecondaryContainer = Color(0xFF1E3A8A)
    
    val Tertiary = VortexTeal
    val OnTertiary = Color.White
    val TertiaryContainer = Color(0xFFCCFDF7)
    val OnTertiaryContainer = Color(0xFF134E4A)
    
    val Error = VortexRed
    val OnError = Color.White
    val ErrorContainer = Color(0xFFFEE2E2)
    val OnErrorContainer = Color(0xFF991B1B)
    
    val Background = Color(0xFFFEFEFE)
    val OnBackground = Color(0xFF1A1A1A)
    val Surface = Color(0xFFFEFEFE)
    val OnSurface = Color(0xFF1A1A1A)
    val SurfaceVariant = Color(0xFFF5F5F5)
    val OnSurfaceVariant = Color(0xFF6B7280)
    
    val Outline = Color(0xFFD1D5DB)
    val OutlineVariant = Color(0xFFE5E7EB)
    val Scrim = Color(0x80000000)
    val InverseSurface = Color(0xFF1F2937)
    val InverseOnSurface = Color(0xFFF9FAFB)
    val InversePrimary = Color(0xFFA78BFA)
    
    // Surface containers
    val SurfaceDim = Color(0xFFF3F4F6)
    val SurfaceBright = Color.White
    val SurfaceContainerLowest = Color.White
    val SurfaceContainerLow = Color(0xFFFAFAFA)
    val SurfaceContainer = Color(0xFFF5F5F5)
    val SurfaceContainerHigh = Color(0xFFF0F0F0)
    val SurfaceContainerHighest = Color(0xFFEBEBEB)
    
    // Dark Theme Colors
    val DarkPrimary = Color(0xFFA78BFA)
    val DarkOnPrimary = Color(0xFF2D1B69)
    val DarkPrimaryContainer = Color(0xFF4C1D95)
    val DarkOnPrimaryContainer = Color(0xFFE9D5FF)
    
    val DarkSecondary = Color(0xFF93C5FD)
    val DarkOnSecondary = Color(0xFF1E3A8A)
    val DarkSecondaryContainer = Color(0xFF1E40AF)
    val DarkOnSecondaryContainer = Color(0xFFDBEAFE)
    
    val DarkTertiary = Color(0xFF5EEAD4)
    val DarkOnTertiary = Color(0xFF134E4A)
    val DarkTertiaryContainer = Color(0xFF0F766E)
    val DarkOnTertiaryContainer = Color(0xFFCCFDF7)
    
    val DarkError = Color(0xFFF87171)
    val DarkOnError = Color(0xFF991B1B)
    val DarkErrorContainer = Color(0xFFDC2626)
    val DarkOnErrorContainer = Color(0xFFFEE2E2)
    
    val DarkBackground = Color(0xFF0F0F0F)
    val DarkOnBackground = Color(0xFFE5E5E5)
    val DarkSurface = Color(0xFF0F0F0F)
    val DarkOnSurface = Color(0xFFE5E5E5)
    val DarkSurfaceVariant = Color(0xFF1F1F1F)
    val DarkOnSurfaceVariant = Color(0xFF9CA3AF)
    
    val DarkOutline = Color(0xFF4B5563)
    val DarkOutlineVariant = Color(0xFF374151)
    val DarkScrim = Color(0x80000000)
    val DarkInverseSurface = Color(0xFFE5E5E5)
    val DarkInverseOnSurface = Color(0xFF1F1F1F)
    val DarkInversePrimary = Color(0xFF6B46C1)
    
    // Dark surface containers
    val DarkSurfaceDim = Color(0xFF0A0A0A)
    val DarkSurfaceBright = Color(0xFF2A2A2A)
    val DarkSurfaceContainerLowest = Color(0xFF050505)
    val DarkSurfaceContainerLow = Color(0xFF141414)
    val DarkSurfaceContainer = Color(0xFF1A1A1A)
    val DarkSurfaceContainerHigh = Color(0xFF1F1F1F)
    val DarkSurfaceContainerHighest = Color(0xFF252525)
    
    // Semantic Colors (consistent across themes)
    val Success = VortexGreen
    val Warning = VortexAmber
    val Info = VortexBlue
    
    // Chat-specific colors
    val UserMessageBackground = Color(0xFFE9D5FF)
    val AssistantMessageBackground = Color(0xFFF3F4F6)
    val DarkUserMessageBackground = Color(0xFF4C1D95)
    val DarkAssistantMessageBackground = Color(0xFF1F1F1F)
    
    // Character-specific colors
    val CharacterCardBackground = Color(0xFFFAFAFA)
    val CharacterCardBorder = Color(0xFFE5E7EB)
    val DarkCharacterCardBackground = Color(0xFF1A1A1A)
    val DarkCharacterCardBorder = Color(0xFF374151)
    
    // Status colors
    val OnlineStatus = VortexGreen
    val OfflineStatus = Color(0xFF6B7280)
    val BusyStatus = VortexAmber
    val AwayStatus = VortexRed
} 