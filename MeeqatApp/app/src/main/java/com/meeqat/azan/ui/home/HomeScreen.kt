package com.meeqat.azan.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meeqat.azan.domain.model.Prayer
import com.meeqat.azan.ui.components.LocationPill
import com.meeqat.azan.ui.components.PrayerCard
import com.meeqat.azan.ui.components.PrayerHeroCard
import com.meeqat.azan.ui.theme.MeeqatMotion
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    onLocationClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    // Tick every second for countdown spring
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }

    val next = ui.nextPrayer
    val times = ui.todayTimes
    val progress = remember(next, now) {
        if (next == null || times == null) 0f else {
            val start = previousPrayerTime(times, next)
            val end = times.timeFor(next)
            if (end <= start) 0f else ((now - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Top: Hijri + Gregorian + Location pill
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(ui.hijriDate, style = MaterialTheme.typography.titleMedium)
                Text(ui.gregorianDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LocationPill(
                city = ui.location?.city ?: "Locate…",
                sourceLabel = ui.location?.source?.name ?: "Cached",
                onClick = onLocationClick,
                dotColor = when (ui.location?.source?.name) {
                    "Precise" -> MaterialTheme.colorScheme.primary
                    "Manual" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.outline
                },
            )
        }

        // Hero — extraLarge 28dp, wavy progress, expressive motion
        if (next != null && times != null) {
            val countdown = formatCountdown(times.timeFor(next) - now)
            val timeText = formatTime(times.timeFor(next))
            AnimatedContent(
                targetState = countdown,
                transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.97f, animationSpec = spring(dampingRatio = 0.7f))).togetherWith(fadeOut()) },
                label = "countdown",
            ) { c ->
                PrayerHeroCard(
                    nextPrayerName = next.name,
                    timeText = timeText,
                    countdownText = c,
                    progress = progress,
                    hijriText = ui.hijriDate,
                    gregorianText = ui.gregorianDate,
                )
            }
        } else {
            PrayerHeroCard(
                nextPrayerName = "—",
                timeText = "--:--",
                countdownText = "00:00:00",
                progress = 0f,
                hijriText = ui.hijriDate,
                gregorianText = ui.gregorianDate,
            )
        }

        // Grid 2-col — passed at 50% alpha, next highlighted primaryContainer, morph 28->16
        if (times != null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                items(times.asList()) { (prayer, millis) ->
                    val isNext = prayer == next
                    val isPassed = millis < now
                    val adjusted = formatTime(millis)
                    val raw = ui.rawTimes?.timeFor(prayer)?.let { formatTime(it) }
                    val hasSound = ui.soundConfig?.uriFor(prayer) != null
                    Box(modifier = Modifier.fillMaxWidth()) {
                        PrayerCard(
                            name = prayer.name,
                            time = adjusted,
                            originalTime = raw,
                            isNext = isNext,
                            isPassed = isPassed,
                            hasCustomSound = hasSound,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // Footer mini Qibla + time to Isha
        if (times != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Qibla ${ui.qibla?.bearingDegrees?.toInt() ?: "—"}° • ${ui.qibla?.distanceKm?.toInt() ?: "—"} km", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val toIsha = times.isha - now
                Text(if (toIsha > 0) "Isha in ${formatCountdown(toIsha)}" else "Isha passed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun previousPrayerTime(times: com.meeqat.azan.domain.model.DailyPrayerTimes, next: Prayer): Long {
    val order = listOf(Prayer.Fajr, Prayer.Sunrise, Prayer.Dhuhr, Prayer.Asr, Prayer.Maghrib, Prayer.Isha)
    val idx = order.indexOf(next)
    return if (idx <= 0) times.fajr - 24 * 60 * 60 * 1000L else times.timeFor(order[idx - 1])
}

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
private fun formatTime(millis: Long): String = timeFmt.format(Instant.ofEpochMilli(millis))
private fun formatCountdown(millis: Long): String {
    val m = millis.coerceAtLeast(0)
    val s = (m / 1000) % 60
    val min = (m / 1000 / 60) % 60
    val h = m / 1000 / 60 / 60
    return String.format("%02d:%02d:%02d", h, min, s)
}
