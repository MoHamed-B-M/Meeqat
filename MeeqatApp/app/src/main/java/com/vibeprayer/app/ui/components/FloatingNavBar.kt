package com.vibeprayer.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.vibeprayer.app.ui.theme.LocalVibeColors
import com.vibeprayer.app.ui.theme.VibeType

@Composable
fun FloatingNavBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalVibeColors.current
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(200),
        label = "navAlpha"
    )
    val yOff by animateFloatAsState(
        targetValue = if (visible) 0f else 120f,
        animationSpec = tween(250),
        label = "navY"
    )
    if (alpha <= 0.01f && !visible) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
            .graphicsLayer(alpha = alpha, translationY = yOff),
        contentAlignment = Alignment.BottomCenter
    ) {
        val shape = RoundedCornerShape(999.dp)
        Box(
            Modifier
                .clip(shape)
                .background(Color.Black.copy(alpha = 0.72f), shape)
                .border(1.dp, colors.borderSubtle, shape)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NavTab(label = "HOME", glyph = "◉", selected = selected == 0, onClick = { onSelect(0) }, modifier = Modifier.weight(1f))
                NavTab(label = "SETTINGS", glyph = "◎", selected = selected == 1, onClick = { onSelect(1) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NavTab(
    label: String,
    glyph: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVibeColors.current
    val interaction = remember { MutableInteractionSource() }
    val pillW by animateDpAsState(
        targetValue = if (selected) 72.dp else 0.dp,
        animationSpec = tween(250),
        label = "pill"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                text = glyph,
                style = VibeType.caption.copy(
                    color = if (selected) colors.accent else colors.textSecondary
                )
            )
            BasicText(
                text = label,
                style = VibeType.caption.copy(
                    color = if (selected) colors.textPrimary else colors.textSecondary
                )
            )
            if (selected) {
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .width(pillW)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.accent)
                )
            }
        }
    }
}

@Suppress("unused")
private val NavHeight = 76.dp
