package com.meeqat.azan.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meeqat.azan.domain.model.Prayer
import com.meeqat.azan.ui.theme.FluidCard
import com.meeqat.azan.ui.theme.MeeqatRef
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onLocationClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }

    val times = ui.todayTimes
    val next = ui.nextPrayer
    val nextName = next?.name ?: "Maghrib"
    val nextMillis = next?.let { times?.timeFor(it) } ?: (now + 37 * 60 * 1000)
    val remainingMins = ((nextMillis - now) / 60000).coerceAtLeast(0)
    val nextLabel = when {
        next == null -> "37 mins left until Isha"
        next == Prayer.Isha -> "${remainingMins.toInt()} mins left until Isha"
        else -> {
            val following = nextAfter(next, times)
            if (following != null) "${remainingMins.toInt()} mins left until ${following.name}" else "${remainingMins.toInt()} mins left"
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(MeeqatRef.Navy)) {
        val maxW = 840.dp
        val hPad = if (maxWidth >= 840.dp) 24.dp else 0.dp

        Column(
            Modifier.fillMaxSize().widthIn(max = maxW).align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState()).padding(horizontal = hPad)
        ) {
            // --- Header with moon & stars ---
            Box(
                Modifier.fillMaxWidth().height(220.dp).background(MeeqatRef.Navy)
            ) {
                // stars
                Canvas(Modifier.fillMaxSize()) {
                    val starColor = Color.White.copy(alpha = 0.35f)
                    listOf(
                        Offset(size.width * 0.08f, size.height * 0.18f),
                        Offset(size.width * 0.22f, size.height * 0.08f),
                        Offset(size.width * 0.35f, size.height * 0.22f),
                        Offset(size.width * 0.55f, size.height * 0.14f),
                        Offset(size.width * 0.72f, size.height * 0.30f),
                        Offset(size.width * 0.18f, size.height * 0.42f),
                        Offset(size.width * 0.42f, size.height * 0.38f),
                    ).forEach { drawCircle(starColor, radius = 1.2f, center = it) }
                    repeat(6) { i ->
                        val x = size.width * (0.05f + i * 0.16f)
                        val y = size.height * (0.25f + (i % 2) * 0.04f)
                        drawCircle(Color.White.copy(alpha = 0.18f), 2f, Offset(x, y))
                    }
                }
                // moon
                Box(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 16.dp).size(96.dp)) {
                    Box(Modifier.size(96.dp).clip(CircleShape).background(Color(0xFF6B7C94)))
                    Box(Modifier.size(76.dp).clip(CircleShape).background(Color(0xFFA8B6C9)).align(Alignment.Center))
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Color.White).align(Alignment.Center))
                }
                Column(Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 28.dp)) {
                    Text(
                        nextName, style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.W700, fontSize = 38.sp, letterSpacing = (-0.5).sp),
                        color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    // pill
                    Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFF1A3659), modifier = Modifier.height(28.dp)) {
                        Text(
                            nextLabel, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF8EA0B8)),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // wavy peach line
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(72.dp).padding(horizontal = 8.dp)) {
                    WavyTimeline(
                        times = times,
                        next = next,
                        now = now,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // TODAY + date
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = RoundedCornerShape(6.dp), color = Color.Transparent, border = androidx.compose.foundation.BorderStroke(1.dp, MeeqatRef.Peach.copy(alpha = 0.9f))) {
                    Text("TODAY", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.W600, color = MeeqatRef.Peach), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {}) { Icon(Icons.Filled.ChevronLeft, null, tint = MeeqatRef.Peach, modifier = Modifier.size(22.dp)) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(ui.gregorianDate.ifBlank { "Friday 19th March" }, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.W700, color = MeeqatRef.Peach, fontSize = 18.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(ui.hijriDate.uppercase().ifBlank { "5 SHA'BAN 1442 AH" }, style = MaterialTheme.typography.bodySmall.copy(color = MeeqatRef.Peach, letterSpacing = 0.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = {}) { Icon(Icons.Filled.ChevronRight, null, tint = MeeqatRef.Peach, modifier = Modifier.size(22.dp)) }
                }
            }

            // Prayer list — fluid rounded 16dp cards
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (times != null) {
                    times.asList().forEach { (prayer, millis) ->
                        val isActive = prayer == next
                        val time = formatTime(millis)
                        val isPast = millis < now - 5 * 60 * 1000 && !isActive
                        PrayerRowFluid(
                            name = prayer.name.let { if (it == "Isha") "Isha'a" else it },
                            time = time,
                            isActive = isActive,
                            isPast = isPast,
                        )
                    }
                } else {
                    // loading skeletons — fluid
                    repeat(6) {
                        Box(Modifier.fillMaxWidth().height(56.dp).clip(FluidCard).background(MeeqatRef.NavyContainer.copy(alpha = 0.6f)))
                    }
                }
                Spacer(Modifier.height(96.dp)) // nav padding
            }
        }
    }
}

@Composable
private fun PrayerRowFluid(name: String, time: String, isActive: Boolean, isPast: Boolean) {
    val border = if (isActive) 2.dp else 1.dp
    val borderColor = if (isActive) Color.White else MeeqatRef.OutlineLow
    val textColor = when {
        isActive -> Color.White
        isPast -> MeeqatRef.Passed
        else -> Color.White.copy(alpha = 0.9f)
    }
    val timeColor = when {
        isActive -> Color.White
        isPast -> MeeqatRef.Passed
        else -> Color.White
    }
    Surface(
        shape = FluidCard,
        color = MeeqatRef.NavyContainer.copy(alpha = if (isActive) 1f else 0.55f),
        border = androidx.compose.foundation.BorderStroke(border, borderColor),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (isActive) FontWeight.W700 else FontWeight.W500, color = textColor), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(time, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W600, color = timeColor, fontSize = 18.sp), maxLines = 1)
                Icon(Icons.Filled.NotificationsOff, null, tint = if (isActive) Color.White else MeeqatRef.Passed, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun WavyTimeline(times: com.meeqat.azan.domain.model.DailyPrayerTimes?, next: Prayer?, now: Long, modifier: Modifier = Modifier) {
    val peach = MeeqatRef.Peach
    val muted = MeeqatRef.NavyHigh
    val activeIdx = remember(next, times) {
        if (times == null || next == null) 4 else times.asList().indexOfFirst { it.first == next }.coerceAtLeast(0)
    }
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val cy = h * 0.45f
        val pts = if (times != null) {
            val list = times.asList()
            // map prayers to x 0..w, y via sine wave peaking at Dhuhr/Asr region
            list.mapIndexed { i, _ ->
                val x = w * (0.02f + 0.96f * i / 5f)
                // wave: start low, peak at 0.4-0.6, then down
                val t = i / 5f
                val wave = kotlin.math.sin(t * Math.PI).toFloat() * 0.9f + kotlin.math.sin(t * Math.PI * 2 + 0.3f).toFloat() * 0.12f
                val y = cy - wave * (h * 0.42f)
                Offset(x, y)
            }
        } else {
            // fallback static
            listOf(0.02f, 0.22f, 0.42f, 0.60f, 0.76f, 0.96f).mapIndexed { i, t ->
                val x = w * t
                val wave = kotlin.math.sin(t * Math.PI).toFloat()
                Offset(x, cy - wave * h * 0.35f)
            }
        }
        // horizontal TODAY line
        val lineY = h * 0.72f
        drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, lineY), Offset(w, lineY), strokeWidth = 1f)
        // peach wavy path up to active
        val pathPeach = Path(); val pathMuted = Path()
        fun buildPath(path: Path, points: List<Offset>) {
            if (points.isEmpty()) return
            path.moveTo(points[0].x, points[0].y)
            for (i in 0 until points.size - 1) {
                val cX = (points[i].x + points[i+1].x)/2
                path.quadraticTo(points[i].x, points[i].y, cX, (points[i].y+points[i+1].y)/2)
            }
            if (points.size > 1) {
                val last = points.last(); val prev = points[points.size-2]
                val cX = (prev.x + last.x)/2
                path.quadraticTo(last.x, last.y, last.x, last.y)
            }
        }
        // split at active
        val before = pts.take(activeIdx + 1)
        val after = pts.drop(activeIdx)
        buildPath(pathPeach, before)
        buildPath(pathMuted, after)
        drawPath(pathPeach, peach, style = Stroke(width = 3.5f))
        drawPath(pathMuted, Color(0xFF5A6E89), style = Stroke(width = 2.5f))
        // dots
        pts.forEachIndexed { i, p ->
            val isActive = i == activeIdx
            val isFuture = i > activeIdx
            val fill = when {
                isActive -> Color.White
                isFuture -> MeeqatRef.NavyContainer
                else -> Color.White
            }
            val stroke = if (isActive) peach else if (isFuture) Color(0xFF5A6E89) else Color.White.copy(alpha=0.6f)
            drawCircle(fill, radius = if (isActive) 7f else 6f, center = p)
            drawCircle(stroke, radius = if (isActive) 7f else 6f, center = p, style = Stroke(2f))
            // inner dot for active
            if (isActive) drawCircle(peach, 3f, p)
        }
    }
}

private fun nextAfter(current: Prayer, times: com.meeqat.azan.domain.model.DailyPrayerTimes?): Prayer? {
    if (times == null) return null
    val order = listOf(Prayer.Fajr, Prayer.Sunrise, Prayer.Dhuhr, Prayer.Asr, Prayer.Maghrib, Prayer.Isha)
    val idx = order.indexOf(current)
    return if (idx in 0 until order.size - 1) order[idx + 1] else null
}

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
private fun formatTime(millis: Long): String = try { timeFmt.format(Instant.ofEpochMilli(millis)) } catch (_: Exception) { "--:--" }
