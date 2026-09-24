package com.meeqat.azan.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meeqat.azan.ui.theme.ShapeExtraLarge
import com.meeqat.azan.ui.theme.ShapeFull

@Composable
fun MeeqatTopAppBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
        label = {
            Text(
                "$city • $sourceLabel",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
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
    progress: Float,
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Top: descriptor + prayer name + date context
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        "NEXT PRAYER",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        nextPrayerName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.W700),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                // Active indicator badge — mosque icon in primary circle
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Mosque,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            // Countdown block with label
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "REMAINING",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    countdownText,
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.W400),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                LinearWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
                )
            }

            // Bottom row: AT time + dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "AT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        timeText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                ) {
                    Text(
                        hijriText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        gregorianText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun PrayerHeroCardSkeleton(
    hijriText: String = "",
    gregorianText: String = "",
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeExtraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier.width(96.dp).height(12.dp).clip(RoundedCornerShape(6.dp))
                            .shimmerPlaceholder(),
                    )
                    Box(
                        modifier = Modifier.width(140.dp).height(22.dp).clip(RoundedCornerShape(6.dp))
                            .shimmerPlaceholder(),
                    )
                    if (hijriText.isNotEmpty()) {
                        Text(
                            hijriText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (gregorianText.isNotEmpty()) {
                        Text(
                            gregorianText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier.width(88.dp).height(12.dp).clip(RoundedCornerShape(6.dp))
                        .shimmerPlaceholder(),
                )
                Box(
                    modifier = Modifier.width(220.dp).height(48.dp).clip(RoundedCornerShape(8.dp))
                        .shimmerPlaceholder(),
                )
                Box(
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(999.dp))
                        .shimmerPlaceholder(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.width(32.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).shimmerPlaceholder())
                    Box(modifier = Modifier.width(84.dp).height(24.dp).clip(RoundedCornerShape(6.dp)).shimmerPlaceholder())
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.width(120.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).shimmerPlaceholder())
                    Box(modifier = Modifier.width(140.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).shimmerPlaceholder())
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    "Loading prayer times…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun PrayerCard(
    name: String,
    time: String,
    originalTime: String? = null,
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
        shape = if (isNext) ShapeExtraLarge else RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (isNext) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 2.dp else 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    if (isNext) {
                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "next prayer",
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    } else if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        name,
                        style = if (isNext) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W600) else MaterialTheme.typography.titleMedium,
                        color = if (isNext) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hasCustomSound) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "custom sound",
                            modifier = Modifier.size(16.dp),
                            tint = if (isNext) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (isNext) {
                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(
                                "NEXT",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Text(
                time,
                style = MaterialTheme.typography.headlineSmall,
                color = when {
                    isNext -> MaterialTheme.colorScheme.onPrimaryContainer
                    isPassed -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (originalTime != null && originalTime != time) {
                Text(
                    "was $originalTime",
                    style = MaterialTheme.typography.bodySmall.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                    color = if (isNext) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun PrayerCardSkeleton(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(modifier = Modifier.width(88.dp).height(16.dp).clip(RoundedCornerShape(6.dp)).shimmerPlaceholder())
                Box(modifier = Modifier.size(18.dp).clip(ShapeFull).shimmerPlaceholder())
            }
            Box(modifier = Modifier.width(96.dp).height(28.dp).clip(RoundedCornerShape(6.dp)).shimmerPlaceholder())
            Box(modifier = Modifier.width(64.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).shimmerPlaceholder())
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun Modifier.shimmerPlaceholder(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    this.then(
        Modifier.background(base.copy(alpha = alpha)),
    )
}
