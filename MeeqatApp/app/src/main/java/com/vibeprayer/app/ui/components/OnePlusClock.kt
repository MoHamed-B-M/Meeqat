package com.vibeprayer.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibeprayer.app.ui.theme.LocalVibeColors
import com.vibeprayer.app.ui.theme.VibeType
import com.vibeprayer.app.utils.formatClock
import kotlinx.coroutines.delay

@Composable
fun OnePlusClock(
    modifier: Modifier = Modifier,
    redEnabled: Boolean = true
) {
    val colors = LocalVibeColors.current
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val text = remember(now) { formatClock(now) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RedOneText(
            text = text,
            style = VibeType.clock,
            baseColor = colors.textPrimary,
            redEnabled = redEnabled
        )
    }
}

@Composable
fun LiveCountdown(
    targetMillis: Long,
    modifier: Modifier = Modifier,
    redEnabled: Boolean = true
) {
    val colors = LocalVibeColors.current
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(targetMillis) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val remaining = (targetMillis - now).coerceAtLeast(0)
    val text = remember(remaining) { formatCountdown(remaining) }
    RedOneText(
        text = text,
        modifier = modifier.padding(horizontal = 16.dp),
        style = VibeType.clock.copy(fontSize = 40.sp),
        baseColor = colors.textPrimary,
        redEnabled = redEnabled
    )
}

private fun formatCountdown(millis: Long): String {
    val total = millis / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

@Composable
fun PrayerTimeText(
    timeMillis: Long,
    modifier: Modifier = Modifier,
    redEnabled: Boolean = true
) {
    val colors = LocalVibeColors.current
    val text = remember(timeMillis) { formatTimeShort(timeMillis) }
    RedOneText(
        text = text,
        modifier = modifier,
        style = VibeType.time,
        baseColor = colors.textPrimary,
        redEnabled = redEnabled
    )
}

private fun formatTimeShort(millis: Long): String {
    return try {
        val fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
        fmt.format(java.time.Instant.ofEpochMilli(millis))
    } catch (_: Exception) {
        "--:--"
    }
}
