package com.meeqat.azan.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// M3 Motion tokens — expressive spring via MotionScheme.expressive()
// Legacy easing/duration kept for transitions (enter/exit/shared-axis)
object MeeqatMotion {
    // Emphasized (Expressive) — begin & end on screen
    const val EmphasizedDuration = 500
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    const val EmphasizedDecelerateDuration = 400
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    const val EmphasizedAccelerateDuration = 200
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    const val StandardDuration = 300
    const val StandardDecelerateDuration = 250
    const val StandardAccelerateDuration = 200

    val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val StandardDecelerateEasing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val StandardAccelerateEasing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    // Helpers
    fun emphasized tweenSpec() = tween<androidx.compose.ui.graphics.Color>(EmphasizedDuration, easing = EmphasizedEasing)

    // 8dp spacing system — margins/padding/gaps adaptive by WindowSizeClass
    val SpacingXs: Dp = 4.dp
    val SpacingSm: Dp = 8.dp
    val SpacingMd: Dp = 16.dp
    val SpacingLg: Dp = 24.dp
    val SpacingXl: Dp = 32.dp
    val MaxContentWidth: Dp = 840.dp // phones constrained, tablets use pane scaffolds
}
