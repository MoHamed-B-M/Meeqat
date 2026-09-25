package com.vibeprayer.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun DynamicBackground(
    bgRes: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = bgRes,
            animationSpec = tween(durationMillis = 600),
            label = "bg"
        ) { res ->
            Image(
                painter = painterResource(id = res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Dark vignette for readability.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.25f),
                        Color.Black.copy(alpha = 0.72f)
                    )
                )
            )
        )
        // Subtle LED dot-matrix grid overlay.
        Canvas(Modifier.fillMaxSize()) {
            val step = 14.dp.toPx()
            var y = 0f
            while (y < size.height) {
                var x = 0f
                while (x < size.width) {
                    drawCircle(Color.White.copy(alpha = 0.05f), radius = 1.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                    x += step
                }
                y += step
            }
        }
    }
}
