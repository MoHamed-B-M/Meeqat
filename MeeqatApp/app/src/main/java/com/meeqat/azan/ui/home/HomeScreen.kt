package com.meeqat.azan.ui.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CardWhite = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF1A2742)
private val TextTime = Color(0xFF3A4A6A)
private val MosqueBlue = Color(0xFF4A5A85)

@Composable
fun HomeScreen(
    onLocationClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }

    val times = ui.todayTimes
    val context = LocalContext.current
    // Top default image from bundled assets.
    val headerBitmap = remember {
        try {
            context.assets.open("images/default.jpg").use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        } catch (_: Exception) { null }
    }

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

    // Next prayer headline (big text).
    val nextName = remember(times, now) {
        if (times == null) "Fajr Start" else {
            val all = times.asList()
            all.firstOrNull { it.second > now }?.first?.name ?: "Fajr"
        }
    }
    val nextTime = remember(times, now) {
        if (times == null) "--:--" else {
            val all = times.asList()
            val millis = all.firstOrNull { it.second > now }?.second ?: times.fajr
            formatTime(millis)
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFF2F4F8))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Header with default.jpg background, big text overlay.
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                if (headerBitmap != null) {
                    Image(
                        bitmap = headerBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFB8D8EA), Color(0xFFDDECF5)))))
                }
                // Scrim for readability.
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.55f))
                        )
                    )
                )
                // Top bar: date + calendar.
                Row(
                    Modifier.fillMaxWidth().padding(top = 48.dp, start = 20.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Nizamul Awqat",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.W600, fontSize = 15.sp
                            ),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            ui.gregorianDate.ifBlank { "Today 16 Oct, 2022" },
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White, fontWeight = FontWeight.W700, fontSize = 20.sp
                            ),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(48.dp)) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.CalendarToday, null, tint = MosqueBlue, modifier = Modifier.size(24.dp))
                        }
                    }
                }
                // Bottom hero: next prayer big text.
                Column(
                    Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                ) {
                    Text(
                        "Next: $nextName • $nextTime",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = Color.White, fontWeight = FontWeight.W800, fontSize = 26.sp
                        ),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        ui.hijriDate.ifBlank { "5 Sha'ban 1442 AH" },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White.copy(alpha = 0.9f), fontSize = 17.sp
                        ),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Simple big list card.
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = CardWhite,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    simpleItems.forEach { (pair, isActive) ->
                        val (name, time) = pair
                        BigPrayerRow(name = name, time = time, isActive = isActive, onToggle = {})
                    }
                    Spacer(Modifier.height(96.dp))
                }
            }
        }
    }
}

@Composable
private fun BigPrayerRow(
    name: String,
    time: String,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    var enabled by remember(isActive) { mutableStateOf(isActive) }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFFF0F4F8) else Color(0xFFF7F8FA),
            contentColor = TextDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (enabled) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E8F0)) else null,
        modifier = Modifier.fillMaxWidth().height(76.dp)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                name,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = TextDark, fontWeight = FontWeight.W600, fontSize = 20.sp
                ),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    time,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextTime, fontWeight = FontWeight.W700, fontSize = 22.sp
                    ),
                    maxLines = 1
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it; onToggle() },
                    thumbContent = {
                        Icon(
                            if (enabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                            null, modifier = Modifier.size(16.dp)
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
                    modifier = Modifier.size(width = 56.dp, height = 32.dp)
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

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
private fun formatTime(millis: Long): String = try { timeFmt.format(Instant.ofEpochMilli(millis)) } catch (_: Exception) { "--:--" }
