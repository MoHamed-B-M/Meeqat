package com.meeqat.azan.ui.theme

import androidx.compose.ui.graphics.Color

// Meeqat — Fluid reference: Deep Navy #0F2B4A + Peach #F2B18A + Warm Sand #EADDC8
// Matches the three reference images: dark navy background, peach accent, rounded fluid shapes.
// Material 3 Expressive — tonal surfaces, dynamic still supported but default is this serene dark.
object MeeqatRef {
    val Navy = Color(0xFF0F2B4A)
    val NavyDeep = Color(0xFF0B1E36)
    val NavySurface = Color(0xFF13294E)
    val NavyContainer = Color(0xFF1A3659)
    val NavyHigh = Color(0xFF1F3B5F)
    val NavyHighest = Color(0xFF244062)
    val Peach = Color(0xFFF2B18A)
    val PeachLight = Color(0xFFFFDCC5)
    val PeachContainer = Color(0xFF5D3D2A)
    val WarmSand = Color(0xFFEADDC8)
    val MoonOuter = Color(0xFF6B7C94)
    val MoonMid = Color(0xFFA8B6C9)
    val Moon = Color(0xFFFFFFFF)
    val OutlineNavy = Color(0xFF3A4F6A)
    val OutlineLow = Color(0xFF2A415E)
    val Passed = Color(0xFF5A6E89)
}

// Light — kept for system light, but app defaults to dark navy fluid.
val MeeqatLightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF6F4A2F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF4A2E18),
    secondary = Color(0xFF5A6E89),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E4F5),
    onSecondaryContainer = Color(0xFF0F2B4A),
    tertiary = Color(0xFF3F6A8A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6E4F5),
    onTertiaryContainer = Color(0xFF0F2B4A),
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF0F2B4A),
    surface = Color(0xFFF6F8FC),
    onSurface = Color(0xFF0F2B4A),
    surfaceVariant = Color(0xFFD6E4F5),
    onSurfaceVariant = Color(0xFF3A4F6A),
    outline = Color(0xFF5A6A85),
    outlineVariant = Color(0xFFC2D0E5),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFFF2B18A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5FF),
    surfaceContainer = Color(0xFFE6ECF8),
    surfaceContainerHigh = Color(0xFFDCE4F2),
    surfaceContainerHighest = Color(0xFFD0DAEA),
    surfaceBright = Color(0xFFF6F8FC),
    surfaceDim = Color(0xFFD6DBE5),
)

// Dark — Fluid navy/peach, exactly like reference images
val MeeqatDarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = MeeqatRef.Peach,
    onPrimary = Color(0xFF543A24),
    primaryContainer = MeeqatRef.PeachContainer,
    onPrimaryContainer = MeeqatRef.PeachLight,
    inversePrimary = Color(0xFF8B5A3A),
    secondary = Color(0xFFB9C6DA),
    onSecondary = Color(0xFF0F2B4A),
    secondaryContainer = MeeqatRef.NavyContainer,
    onSecondaryContainer = Color(0xFFD6E4F5),
    tertiary = MeeqatRef.Peach,
    onTertiary = Color(0xFF543A24),
    tertiaryContainer = MeeqatRef.PeachContainer,
    onTertiaryContainer = MeeqatRef.PeachLight,
    error = Color(0xFFFFB4A9),
    onError = Color(0xFF690003),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = MeeqatRef.Navy,
    onBackground = Color(0xFFFFFFFF),
    surface = MeeqatRef.Navy,
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = MeeqatRef.NavyContainer,
    onSurfaceVariant = Color(0xFFB9C6DA),
    inverseSurface = Color(0xFFE6ECF8),
    inverseOnSurface = Color(0xFF0F2B4A),
    outline = MeeqatRef.OutlineNavy,
    outlineVariant = MeeqatRef.OutlineLow,
    scrim = Color(0xFF000000),
    surfaceTint = MeeqatRef.Peach,
    surfaceContainerLowest = MeeqatRef.NavyDeep,
    surfaceContainerLow = MeeqatRef.NavySurface,
    surfaceContainer = MeeqatRef.NavyContainer,
    surfaceContainerHigh = MeeqatRef.NavyHigh,
    surfaceContainerHighest = MeeqatRef.NavyHighest,
    surfaceBright = Color(0xFF1F3B5F),
    surfaceDim = MeeqatRef.NavyDeep,
)

val LightCustom = MeeqatLightColorScheme
val DarkCustom = MeeqatDarkColorScheme
