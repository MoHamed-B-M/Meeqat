package com.vibeprayer.app.ui.theme

import androidx.compose.ui.graphics.Color

// Tier 1: primitives, named by hue/step. Never used directly in components.
object VibePrimitives {
    val OledBlack = Color(0xFF000000)
    val Slate = Color(0xFF121318)
    val FrostedDark = Color(0xFF1C1D24)
    val NothingRed = Color(0xFFD71921)
    val White = Color(0xFFFFFFFF)
    val Gray100 = Color(0xFFE8EAF0)
    val Gray400 = Color(0xFF9AA0B2)
    val Gray600 = Color(0xFF5A6072)
}

// Tier 2: semantics, named by role. Components use only this tier.
object VibeSemantics {
    val bgPage = VibePrimitives.OledBlack
    val bgSurface = VibePrimitives.Slate
    val bgRaised = VibePrimitives.FrostedDark
    val textPrimary = VibePrimitives.White
    val textSecondary = VibePrimitives.Gray400
    val textDisabled = VibePrimitives.Gray600
    val borderSubtle = Color.White.copy(alpha = 0.15f)
    val borderStrong = Color.White.copy(alpha = 0.28f)
    val accentSolid = VibePrimitives.NothingRed
    val accentText = VibePrimitives.NothingRed
    val imageOutline = Color.White.copy(alpha = 0.10f)
}
