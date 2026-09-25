package com.vibeprayer.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.vibeprayer.app.ui.theme.LocalVibeColors

@Composable
fun CustomCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    active: Boolean = false,
    static: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalVibeColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !static) 0.96f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "press"
    )
    val borderColor = if (active) colors.accent else colors.borderSubtle
    val bg = colors.bgRaised.copy(alpha = 0.6f)
    val shape = RoundedCornerShape(18.dp)
    val clickableMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick
        )
    } else Modifier
    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(shape)
            .background(bg, shape)
            .border(BorderStroke(1.dp, borderColor), shape)
            .then(clickableMod),
        content = content
    )
}

@Composable
fun RedAccentBar(modifier: Modifier = Modifier) {
    val colors = LocalVibeColors.current
    Box(
        modifier
            .background(colors.accent, RoundedCornerShape(999.dp))
    )
}

@Suppress("unused")
private val UnusedTint = Color.Transparent
