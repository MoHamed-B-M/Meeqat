package com.meeqat.azan.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Fluid — everything rounded, as in reference: cards 16-20dp, pills 999dp
val MeeqatShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val ShapeSmall = RoundedCornerShape(12.dp)
val ShapeMedium = RoundedCornerShape(16.dp)
val ShapeLarge = RoundedCornerShape(20.dp)
val ShapeLargeIncreased = RoundedCornerShape(24.dp)
val ShapeExtraLarge = RoundedCornerShape(28.dp)
val ShapeExtraLargeIncreased = RoundedCornerShape(32.dp)
val ShapeExtraExtraLarge = RoundedCornerShape(48.dp)
val ShapeFull = RoundedCornerShape(999.dp)

// Reference fluid radii
val FluidCard = RoundedCornerShape(16.dp)
val FluidPill = RoundedCornerShape(999.dp)
val FluidSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
