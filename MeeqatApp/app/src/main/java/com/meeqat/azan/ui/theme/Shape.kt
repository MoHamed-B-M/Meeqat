package com.meeqat.azan.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// M3 Expressive shape tokens — 8dp system
// none 0, xs 4, small 8, medium 12, large 16, largeIncreased 20, xl 28, xlIncreased 32, xxl 48, full 999
val MeeqatShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// Expressive aliases used across Meeqat cards/buttons
val ShapeSmall = RoundedCornerShape(8.dp)       // chips, snackbars
val ShapeMedium = RoundedCornerShape(12.dp)     // text fields, menus
val ShapeLarge = RoundedCornerShape(16.dp)      // FABs, buttons
val ShapeLargeIncreased = RoundedCornerShape(20.dp)
val ShapeExtraLarge = RoundedCornerShape(28.dp) // main prayer cards, dialogs
val ShapeExtraLargeIncreased = RoundedCornerShape(32.dp)
val ShapeExtraExtraLarge = RoundedCornerShape(48.dp)
val ShapeFull = RoundedCornerShape(999.dp)      // pill buttons, FAB morph
