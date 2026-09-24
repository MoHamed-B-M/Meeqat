package com.meeqat.azan.ui.tracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meeqat.azan.data.local.DailyPrayerEntity
import com.meeqat.azan.data.repo.CalculationRepository
import com.meeqat.azan.ui.theme.MeeqatRef
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class TrackerViewModel(
    calculationRepository: CalculationRepository = com.meeqat.azan.di.ServiceLocator.calculationRepository
) : ViewModel() {
    val allEntities: StateFlow<List<DailyPrayerEntity>> = calculationRepository.observeAll()
        .map { it.sortedBy { e -> e.date } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@Composable
fun TrackerScreen(viewModel: TrackerViewModel = viewModel()) {
    val entities by viewModel.allEntities.collectAsState()
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    val month = remember(currentDate) { YearMonth.from(currentDate) }

    BoxWithConstraints(Modifier.fillMaxSize().background(MeeqatRef.Navy)) {
        val maxW = 840.dp
        Column(
            Modifier.fillMaxSize().widthIn(max = maxW).align(Alignment.TopCenter)
                .padding(horizontal = if (maxWidth >= 840.dp) 24.dp else 0.dp)
        ) {
            // Top TODAY + date
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = RoundedCornerShape(6.dp), color = Color.Transparent, border = androidx.compose.foundation.BorderStroke(1.dp, MeeqatRef.Peach.copy(alpha = 0.9f))) {
                    Text("TODAY", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.W600, color = MeeqatRef.Peach), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { currentDate = currentDate.minusDays(1) }) { Icon(Icons.Filled.ChevronLeft, null, tint = MeeqatRef.Peach, modifier = Modifier.size(22.dp)) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            currentDate.format(DateTimeFormatter.ofPattern("EEEE d'" + ordinalSuffix(currentDate.dayOfMonth) + "' MMMM", Locale.ENGLISH)),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.W700, color = MeeqatRef.Peach, fontSize = 18.sp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            hijriLabel(currentDate).uppercase(),
                            style = MaterialTheme.typography.bodySmall.copy(color = MeeqatRef.Peach, letterSpacing = 0.5.sp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { currentDate = currentDate.plusDays(1) }) { Icon(Icons.Filled.ChevronRight, null, tint = MeeqatRef.Peach, modifier = Modifier.size(22.dp)) }
                    // droplet top right like reference
                    Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF0B1E36)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.WaterDrop, null, tint = MeeqatRef.Peach, modifier = Modifier.size(16.dp))
                    }
                }
            }

            LazyColumn(
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
            ) {
                // 5 prayers + sunrise? reference shows 5: Fajr, Dhuhr, Asr, Maghreb, Isha (no sunrise)
                val prayers = listOf(
                    "Fajr Prayer" to "03:53" to TrackerState.DOT,
                    "Dhuhr Prayer" to "13:12" to TrackerState.CHECK,
                    "Asr Prayer" to "17:10" to TrackerState.EMPTY,
                    "Maghreb Prayer" to "20:39" to TrackerState.EMPTY,
                    "Isha Prayer" to "21:42" to TrackerState.EMPTY,
                )
                items(prayers.size) { idx ->
                    val (pair, state) = prayers[idx]
                    val (name, time) = pair
                    PrayerTrackerRow(name = name, time = time, state = state)
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    // dots pagination
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        repeat(4) { i ->
                            Box(Modifier.padding(horizontal = 3.dp).size(if (i == 0) 6.dp else 5.dp).clip(CircleShape).background(if (i == 0) Color.White else Color.White.copy(alpha = 0.3f)))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Weekly M T W T F S S rings
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF0B1E36), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                        ) {
                            val dagen = listOf("M" to 1f, "T" to 1f, "W" to 0.75f, "T" to 0.35f, "F" to 0.45f, "S" to 0f, "S" to 0f)
                            dagen.forEach { (label, prog) ->
                                WeeklyRing(label = label, progress = prog)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(96.dp))
        }
    }
}

private enum class TrackerState { DOT, CHECK, EMPTY }

@Composable
private fun PrayerTrackerRow(name: String, time: String, state: TrackerState) {
    Surface(shape = RoundedCornerShape(16.dp), color = MeeqatRef.NavyContainer, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(1f)) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(
                        when (state) {
                            TrackerState.DOT -> MeeqatRef.NavyContainer
                            TrackerState.CHECK -> MeeqatRef.NavyContainer
                            else -> Color.Transparent
                        }
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        TrackerState.DOT -> {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(Color.Transparent), contentAlignment = Alignment.Center) {
                                Canvas(Modifier.size(28.dp)) {
                                    drawCircle(Color(0xFF2A415E), 14.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
                                    drawCircle(MeeqatRef.Peach, 5.dp.toPx(), center = center)
                                }
                            }
                        }
                        TrackerState.CHECK -> {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(Color.Transparent), contentAlignment = Alignment.Center) {
                                Canvas(Modifier.size(28.dp)) {
                                    drawCircle(MeeqatRef.Peach, 14.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
                                }
                                Icon(Icons.Filled.Check, null, tint = MeeqatRef.Peach, modifier = Modifier.size(16.dp))
                            }
                        }
                        else -> {
                            Canvas(Modifier.size(28.dp)) { drawCircle(Color(0xFF2A415E), 14.dp.toPx(), center = center, style = Stroke(2.dp.toPx())) }
                        }
                    }
                }
                Text(name, style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontWeight = FontWeight.W500), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(time, style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF8EA0B8), fontWeight = FontWeight.W600), maxLines = 1)
        }
    }
}

@Composable
private fun WeeklyRing(label: String, progress: Float) {
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(40.dp)) {
            val stroke = 3.dp.toPx()
            drawCircle(Color(0xFF2A415E), radius = size.minDimension/2 - stroke/2, center = center, style = Stroke(stroke))
            if (progress > 0) {
                drawArc(
                    color = MeeqatRef.Peach, startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f,1f),
                    useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.W600), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private fun ordinalSuffix(day: Int): String = when {
    day in 11..13 -> "th"
    day % 10 == 1 -> "st"
    day % 10 == 2 -> "nd"
    day % 10 == 3 -> "rd"
    else -> "th"
}

private fun hijriLabel(date: LocalDate): String {
    return try {
        val greg = java.util.GregorianCalendar(date.year, date.monthValue-1, date.dayOfMonth)
        val hijri = com.github.msarhan.ummalqura.calendar.UmmalquraCalendar()
        hijri.setTime(greg.time)
        val d = hijri.get(java.util.Calendar.DAY_OF_MONTH)
        val m = hijri.get(java.util.Calendar.MONTH)
        val y = hijri.get(java.util.Calendar.YEAR)
        val names = arrayOf("Muharram","Safar","Rabi' I","Rabi' II","Jumada I","Jumada II","Rajab","Sha'ban","Ramadan","Shawwal","Dhu al-Qi'dah","Dhu al-Hijjah")
        "$d ${names[m.coerceIn(0,11)]} $y AH"
    } catch (_: Exception) { "" }
}
