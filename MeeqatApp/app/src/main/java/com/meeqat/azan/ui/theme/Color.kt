package com.meeqat.azan.ui.theme

import androidx.compose.ui.graphics.Color

// Meeqat seed — Deep Emerald #0D2C2A + Warm Sand #EADDC8 + Gold #D9AD6A
// Serene, spiritual, paper & light — not neon.
object MeeqatSeed {
    val DeepEmerald = Color(0xFF0D2C2A)
    val WarmSand = Color(0xFFEADDC8)
    val Gold = Color(0xFFD9AD6A)
}

// Generated tonal-spot palettes from Deep Emerald seed
// Light scheme — warm, airy, paper-like surfaces
val MeeqatLightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF1B4D46),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBCECE2),
    onPrimaryContainer = Color(0xFF00201C),
    inversePrimary = Color(0xFF8BD0C2),
    secondary = Color(0xFF4A635E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E1),
    onSecondaryContainer = Color(0xFF051F1B),
    tertiary = Color(0xFF7A5A00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDF9A),
    onTertiaryContainer = Color(0xFF261A00),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6FFF8),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFF6FFF8),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFDBE5E0),
    onSurfaceVariant = Color(0xFF3F4946),
    inverseSurface = Color(0xFF2C3230),
    inverseOnSurface = Color(0xFFECF2EF),
    outline = Color(0xFF6F7976),
    outlineVariant = Color(0xFFBFC9C5),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF1B4D46),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF7F3),
    surfaceContainer = Color(0xFFE9F0ED),
    surfaceContainerHigh = Color(0xFFE3EBE8),
    surfaceContainerHighest = Color(0xFFDDE4E0),
    surfaceBright = Color(0xFFF6FFF8),
    surfaceDim = Color(0xFFD5DBD8),
)

val MeeqatDarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFF8BD0C2),
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF005049),
    onPrimaryContainer = Color(0xFFBCECE2),
    inversePrimary = Color(0xFF1B4D46),
    secondary = Color(0xFFB1CCC5),
    onSecondary = Color(0xFF1C3530),
    secondaryContainer = Color(0xFF334B47),
    onSecondaryContainer = Color(0xFFCCE8E1),
    tertiary = Color(0xFFF0BF3D),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFDF9A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1513),
    onBackground = Color(0xFFDDE4E0),
    surface = Color(0xFF0F1513),
    onSurface = Color(0xFFDDE4E0),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBFC9C5),
    inverseSurface = Color(0xFFDDE4E0),
    inverseOnSurface = Color(0xFF2C3230),
    outline = Color(0xFF89938F),
    outlineVariant = Color(0xFF3F4946),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF8BD0C2),
    surfaceContainerLowest = Color(0xFF0A0F0E),
    surfaceContainerLow = Color(0xFF171D1B),
    surfaceContainer = Color(0xFF1B211F),
    surfaceContainerHigh = Color(0xFF252B29),
    surfaceContainerHighest = Color(0xFF303633),
    surfaceBright = Color(0xFF343A38),
    surfaceDim = Color(0xFF0F1513),
)

// Legacy aliases for non-expressive fallback if needed
val LightCustom = MeeqatLightColorScheme
val DarkCustom = MeeqatDarkColorScheme
