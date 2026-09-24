package com.meeqat.azan.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.meeqat.azan.ui.theme.ShapeExtraLarge
import com.meeqat.azan.ui.theme.ShapeFull

@Composable
fun MeeqatTopAppBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    // M3 Expressive: center-aligned / medium app bar pattern — calm, not gaming.
    // Using tonal surfaceContainer, no shadows.
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LocationPill(
    city: String,
    sourceLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dotColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    AssistChip(
        onClick = onClick,
        label = { Text("$city • $sourceLabel") },
        leadingIcon = {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = ShapeFull,
                color = dotColor,
                content = {},
            )
        },
        modifier = modifier,
        shape = ShapeFull,
        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PrayerHeroCard(
    nextPrayerName: String,
    timeText: String,
    countdownText: String,
    progress: Float, // 0..1 time left
    hijriText: String,
    gregorianText: String,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 120f),
        label = "heroProgress",
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeExtraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // tonal only
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(hijriText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Text(gregorianText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
                AssistChip(
                    onClick = {},
                    label = { Text(nextPrayerName) },
                    shape = ShapeFull,
                )
            }
            Text(
                countdownText,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                timeText,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            LinearWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
            )
        }
    }
}

@Composable
fun PrayerCard(
    name: String,
    time: String,
    originalTime: String? = null, // strikethrough if adjusted
    isNext: Boolean = false,
    isPassed: Boolean = false,
    hasCustomSound: Boolean = false,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isNext) 1f else 0.98f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "cardScale",
    )
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier.scale(scale),
        shape = if (isNext) ShapeExtraLarge else androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (isNext) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(name, style = if (isNext) MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.W600) else MaterialTheme.typography.titleMedium)
                if (hasCustomSound) {
                    Icon(imageVector = androidx.compose.material.icons.Icons.Filled.MusicNote, contentDescription = "custom sound", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
                if (leadingIcon != null) {
                    Icon(imageVector = leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                time,
                style = MaterialTheme.typography.headlineSmall,
                color = if (isPassed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
            )
            if (originalTime != null && originalTime != time) {
                Text(
                    "was $originalTime",
                    style = MaterialTheme.typography.bodySmall.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun TipCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f))
        }
    }
}
