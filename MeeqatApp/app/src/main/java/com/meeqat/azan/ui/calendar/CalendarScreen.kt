package com.meeqat.azan.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar
import com.meeqat.azan.data.local.DailyPrayerEntity
import com.meeqat.azan.data.repo.CalculationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

class CalendarViewModel(
    calculationRepository: CalculationRepository = com.meeqat.azan.di.ServiceLocator.calculationRepository
) : ViewModel() {
    val allEntities: StateFlow<List<DailyPrayerEntity>> = calculationRepository.observeAll()
        .map { it.sortedBy { e -> e.date } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel()
) {
    val entities by viewModel.allEntities.collectAsState()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    val monthEntities = remember(entities, currentMonth) {
        entities.filter { e ->
            runCatching { YearMonth.parse(e.date.substring(0, 7)) == currentMonth }.getOrDefault(false)
        }
    }

    val gregorianFormatter = remember { DateTimeFormatter.ofPattern("d", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = hijriMonthLabel(currentMonth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                )
            }
        }

        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDow = currentMonth.atDay(1).dayOfWeek.value
        val leadingBlanks = (firstDow - 1) % 7
        val totalCells = leadingBlanks + daysInMonth
        val gridItems: List<GridItem> = buildList {
            repeat(leadingBlanks) { add(GridItem.Blank) }
            for (d in 1..daysInMonth) {
                val date = currentMonth.atDay(d)
                val dateStr = date.toString()
                val entity = monthEntities.find { it.date == dateStr } ?: entities.find { it.date == dateStr }
                add(GridItem.Day(date, entity))
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(gridItems) { item ->
                when (item) {
                    is GridItem.Blank -> {}
                    is GridItem.Day -> {
                        DayCard(
                            date = item.date,
                            entity = item.entity,
                            isToday = item.date == LocalDate.now(),
                            gregorianFormatter = gregorianFormatter
                        )
                    }
                }
            }
        }
    }
}

private sealed interface GridItem {
    data object Blank : GridItem
    data class Day(val date: LocalDate, val entity: DailyPrayerEntity?) : GridItem
}

@Composable
private fun DayCard(
    date: LocalDate,
    entity: DailyPrayerEntity?,
    isToday: Boolean,
    gregorianFormatter: DateTimeFormatter
) {
    val hijriLabel = remember(date) { hijriDayLabel(date) }
    val container = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    // Correct M3 mapping for secondary text: onPrimaryContainer on primaryContainer guarantees AA contrast,
    // onSurfaceVariant on surfaceContainer is the spec-compliant muted color.
    val secondaryColor = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = date.format(gregorianFormatter),
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = hijriLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                color = secondaryColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (entity != null) {
                Column(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PrayerMini(label = "F", time = formatTime(entity.fajr), isToday = isToday)
                    PrayerMini(label = "D", time = formatTime(entity.dhuhr), isToday = isToday)
                    PrayerMini(label = "A", time = formatTime(entity.asr), isToday = isToday)
                    PrayerMini(label = "M", time = formatTime(entity.maghrib), isToday = isToday)
                    PrayerMini(label = "I", time = formatTime(entity.isha), isToday = isToday)
                }
            } else {
                Text(
                    text = "--:--",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                    color = secondaryColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            if (isToday) {
                // Badge with Full shape (CircleShape), primary container ensures AA contrast for onPrimary
                // Using Surface instead of Text color to avoid clipping and ensure proper pill sizing on ~44dp cells
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerMini(label: String, time: String, isToday: Boolean) {
    val secondaryColor = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
            color = secondaryColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
            color = primaryColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatTime(millis: Long): String {
    return try {
        val fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault())
        fmt.format(java.time.Instant.ofEpochMilli(millis))
    } catch (_: Exception) { "--:--" }
}

private fun hijriDayLabel(date: LocalDate): String {
    return try {
        val greg = GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth)
        val hijri = UmmalquraCalendar()
        hijri.setTime(greg.time)
        val d = hijri.get(Calendar.DAY_OF_MONTH)
        d.toString()
    } catch (_: Exception) {
        ""
    }
}

private fun hijriMonthLabel(month: YearMonth): String {
    return try {
        val date = month.atDay(1)
        val greg = GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth)
        val hijri = UmmalquraCalendar()
        hijri.setTime(greg.time)
        val m = hijri.get(Calendar.MONTH)
        val y = hijri.get(Calendar.YEAR)
        val names = arrayOf("Muharram","Safar","Rabi' I","Rabi' II","Jumada I","Jumada II","Rajab","Sha'ban","Ramadan","Shawwal","Dhu al-Qi'dah","Dhu al-Hijjah")
        "${names[m.coerceIn(0,11)]} $y AH"
    } catch (_: Exception) { "" }
}
