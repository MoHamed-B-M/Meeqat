package com.vibeprayer.app.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.vibeprayer.app.ui.theme.LocalVibeColors
import com.vibeprayer.app.ui.theme.VibePrimitives
import com.vibeprayer.app.ui.theme.VibeType

@Composable
fun DotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = VibeType.heroLabel,
    color: Color = LocalVibeColors.current.textPrimary,
    maxLines: Int = 1
) {
    BasicText(
        text = text.uppercase(),
        modifier = modifier,
        style = style.copy(color = color),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun RedOneText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    baseColor: Color,
    redEnabled: Boolean,
    maxLines: Int = 1
) {
    val annotated = remember(text, baseColor, redEnabled) {
        if (!redEnabled || !text.contains('1')) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                val red = SpanStyle(color = VibePrimitives.NothingRed)
                val base = SpanStyle(color = baseColor)
                for (ch in text) {
                    if (ch == '1') pushStyle(red) else pushStyle(base)
                    append(ch.toString())
                    pop()
                }
            }
        }
    }
    BasicText(
        text = annotated,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}
