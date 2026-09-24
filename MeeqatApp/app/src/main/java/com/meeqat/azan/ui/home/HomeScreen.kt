package com.meeqat.azan.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meeqat.azan.domain.model.Prayer
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Simple palette exactly like reference image: soft blue/orange sky + white card
private val SimpleBgTop = Color(0xFFB8D8EA)
private val SimpleBgBottom = Color(0xFFDDECF5)
private val MosqueBlue = Color(0xFF4A5A85)
private val MosqueDark = Color(0xFF2F3A5A)
private val CardWhite = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF1A2742)
private val TextMuted = Color(0xFF6B7A90)
private val TextTime = Color(0xFF3A4A6A)

@Composable
fun HomeScreen(
    onLocationClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }

    val times = ui.todayTimes
    // Simple list as in image: Saheri, Fajr Start, Fajr End, Ishraq, Zohar (mapped from our 6)
    val simpleItems = remember(times, now) {
        if (times == null) {
            listOf(
                "Saheri Time" to "04:49" to false,
                "Fajr Start" to "05:11" to false,
                "Fajr End" to "06:06" to false,
                "Ishraq" to "06:26" to false,
                "Zohar" to "12:19" to false,
            )
        } else {
            listOf(
                "Saheri Time" to formatTime(times.fajr - 22 * 60 * 1000) to (times.fajr > now),
                "Fajr Start" to formatTime(times.fajr) to isNext(times.fajr, times, now),
                "Fajr End" to formatTime(times.sunrise) to isNext(times.sunrise, times, now),
                "Ishraq" to formatTime(times.sunrise + 15 * 60 * 1000) to false,
                "Zohar" to formatTime(times.dhuhr) to (times.dhuhr > now),
            )
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SimpleBgTop, SimpleBgBottom)))) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            // Header -- exactly like image: Nizamul Awqat Nanded top left, Today 16 Oct, Thursday, calendar icon
            Box(
                Modifier.fillMaxWidth().height(320.dp).padding(horizontal = 0.dp)
            ) {
                // Mosque silhouette background
                Canvas(Modifier.fillMaxSize()) {
                    drawMosqueSilhouette(this, MosqueBlue, MosqueDark)
                    // soft sun/moon glow
                    drawCircle(Color.White.copy(alpha = 0.35f), radius = size.width * 0.18f, center = Offset(size.width * 0.62f, size.height * 0.42f))
                    drawCircle(Color.White.copy(alpha = 0.15f), radius = size.width * 0.26f, center = Offset(size.width * 0.62f, size.height * 0.42f))
                }
                // top bar
                Row(
                    Modifier.fillMaxWidth().padding(top = 48.dp, start = 20.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Nizamul Awqat Nanded",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF3A4A6A), fontWeight = FontWeight.W600, letterSpacing = 0.3.sp, fontSize = 11.sp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            ui.gregorianDate.ifBlank { "Today 16 Oct, 2022 ," },
                            style = MaterialTheme.typography.titleSmall.copy(color = TextDark, fontWeight = FontWeight.W700, fontSize = 14.sp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Thursday",
                            style = MaterialTheme.typography.titleSmall.copy(color = TextDark, fontWeight = FontWeight.W700, fontSize = 14.sp),
                            maxLines = 1
                        )
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(32.dp)) {
                        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.CalendarToday, null, tint = MosqueBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                // Tap for location (hidden but keeps function)
                Box(Modifier.fillMaxSize().padding(bottom = 24.dp), contentAlignment = Alignment.BottomCenter) {
                    // small handle
                    Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.5f)))
                }
            }

            // White card — fluid rounded 24dp top, like image
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                color = CardWhite,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    simpleItems.forEach { (pair, isActive) ->
                        val (name, time) = pair
                        SimplePrayerRow(
                            name = name,
                            time = time,
                            isActive = isActive,
                            onToggle = {}
                        )
                    }
                    // extra bottom padding for nav
                    Spacer(Modifier.height(96.dp))
                }
            }
        }
    }
}

@Composable
private fun SimplePrayerRow(
    name: String,
    time: String,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    var enabled by remember(isActive) { mutableStateOf(isActive) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFFF0F4F8) else Color(0xFFF7F8FA),
            contentColor = TextDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (enabled) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E8F0)) else null,
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextDark, fontWeight = FontWeight.W500, fontSize = 14.sp),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    time,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextTime, fontWeight = FontWeight.W600, fontSize = 14.sp),
                    maxLines = 1
                )
                // simple alarm toggle — fluid
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it; onToggle() },
                    thumbContent = {
                        Icon(
                            if (enabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                            null, modifier = Modifier.size(12.dp)
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MosqueBlue,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFD0D8E5),
                        checkedBorderColor = Color.Transparent,
                        uncheckedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.size(width = 44.dp, height = 26.dp)
                )
            }
        }
    }
}

private fun isNext(millis: Long, times: com.meeqat.azan.domain.model.DailyPrayerTimes, now: Long): Boolean {
    val list = times.asList().map { it.second }.sorted()
    val idx = list.indexOf(millis)
    if (idx == -1) return false
    val prevDone = if (idx > 0) now > list[idx - 1] else true
    return prevDone && now < millis + 30 * 60 * 1000
}

private fun DrawScope.drawMosqueSilhouette(mosque: Color, dark: Color) {
    val w = size.width; val h = size.height
    val baseY = h * 0.78f
    val path = Path().apply {
        moveTo(0f, baseY)
        // left minaret
        lineTo(w * 0.18f, baseY); lineTo(w * 0.18f, h * 0.52f); lineTo(w * 0.20f, h * 0.48f); lineTo(w * 0.22f, h * 0.52f); lineTo(w * 0.22f, baseY)
        // left dome
        cubicTo(w * 0.24f, h * 0.45f, w * 0.30f, h * 0.38f, w * 0.36f, h * 0.45f)
        lineTo(w * 0.36f, baseY)
        // center big dome
        cubicTo(w * 0.38f, h * 0.30f, w * 0.52f, h * 0.28f, w * 0.60f, h * 0.42f)
        lineTo(w * 0.60f, baseY)
        // right minaret + domes
        lineTo(w * 0.72f, baseY); lineTo(w * 0.72f, h * 0.50f); lineTo(w * 0.74f, h * 0.46f); lineTo(w * 0.76f, h * 0.50f); lineTo(w * 0.76f, baseY)
        cubicTo(w * 0.78f, h * 0.44f, w * 0.82f, h * 0.40f, w * 0.86f, h * 0.48f)
        lineTo(w * 0.86f, baseY); lineTo(w, baseY); lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(path, mosque)
    // crescents
    val crescentPath = Path().apply {
        addOval(Rect(Offset(w * 0.44f, h * 0.34f), androidx.compose.ui.geometry.Size(14.dp.toPx(), 14.dp.toPx())))
    }
    drawPath(crescentPath, Color.White.copy(alpha = 0.9f))
    // second crescent right
    val c2 = Path().apply { addOval(Rect(Offset(w * 0.74f, h * 0.38f), androidx.compose.ui.geometry.Size(10.dp.toPx(), 10.dp.toPx()))) }
    drawPath(c2, Color.White.copy(alpha = 0.9f))
}

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
private fun formatTime(millis: Long): String = try { timeFmt.format(Instant.ofEpochMilli(millis)) } catch (_: Exception) { "--:--" }
