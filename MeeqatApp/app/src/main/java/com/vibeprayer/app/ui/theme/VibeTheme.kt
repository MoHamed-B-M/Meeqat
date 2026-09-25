package com.vibeprayer.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

data class VibeColors(
    val bgPage: androidx.compose.ui.graphics.Color = VibeSemantics.bgPage,
    val bgSurface: androidx.compose.ui.graphics.Color = VibeSemantics.bgSurface,
    val bgRaised: androidx.compose.ui.graphics.Color = VibeSemantics.bgRaised,
    val textPrimary: androidx.compose.ui.graphics.Color = VibeSemantics.textPrimary,
    val textSecondary: androidx.compose.ui.graphics.Color = VibeSemantics.textSecondary,
    val textDisabled: androidx.compose.ui.graphics.Color = VibeSemantics.textDisabled,
    val borderSubtle: androidx.compose.ui.graphics.Color = VibeSemantics.borderSubtle,
    val accent: androidx.compose.ui.graphics.Color = VibeSemantics.accentSolid
)

val LocalVibeColors = compositionLocalOf { VibeColors() }

@Composable
fun VibeTheme(
    theme: String = "oled",
    redAccents: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = remember(theme, redAccents) {
        when (theme) {
            "slate" -> VibeColors(bgPage = VibePrimitives.Slate)
            "light" -> VibeColors(
                bgPage = VibePrimitives.Gray100,
                bgSurface = VibePrimitives.White,
                bgRaised = VibePrimitives.White,
                textPrimary = VibePrimitives.OledBlack,
                textSecondary = VibePrimitives.Gray600,
                borderSubtle = VibePrimitives.OledBlack.copy(alpha = 0.15f),
                accent = if (redAccents) VibePrimitives.NothingRed else VibePrimitives.OledBlack
            )
            else -> VibeColors(
                accent = if (redAccents) VibePrimitives.NothingRed else VibePrimitives.White
            )
        }
    }
    CompositionLocalProvider(LocalVibeColors provides colors, content = content)
}
