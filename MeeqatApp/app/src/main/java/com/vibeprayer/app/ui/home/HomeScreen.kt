package com.vibeprayer.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibeprayer.app.VibeApp
import com.vibeprayer.app.data.model.Prayer
import com.vibeprayer.app.domain.PrayerBackgroundEngine
import com.vibeprayer.app.ui.components.CustomCard
import com.vibeprayer.app.ui.components.DotMatrixText
import com.vibeprayer.app.ui.components.LiveCountdown
import com.vibeprayer.app.ui.components.OnePlusClock
import com.vibeprayer.app.ui.components.PrayerTimeText
import com.vibeprayer.app.ui.theme.LocalVibeColors
import com.vibeprayer.app.ui.theme.VibeType
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(LocalContext.current.applicationContext as VibeApp)
    )
) {
    val ui by viewModel.ui.collectAsState()
    val colors = LocalVibeColors.current
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val times = ui.times
    val red = ui.settings.redAccents
    val active = remember(now, times) { PrayerBackgroundEngine.currentPrayer(now, times) }
    val next = remember(now, times) { PrayerBackgroundEngine.nextPrayer(now, times) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: live clock + dates with red divider.
        OnePlusClock(redEnabled = red)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.weight(1f)) {
                BasicText(
                    text = times?.gregorianDate?.ifBlank { null } ?: ui.settings.manualLabel.ifBlank { "TODAY" },
                    style = VibeType.body.copy(color = colors.textPrimary)
                )
                BasicText(
                    text = ui.locationLabel,
                    style = VibeType.caption.copy(color = colors.textSecondary)
                )
            }
            Box(
                Modifier
                    .width(2.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (red) colors.accent else colors.borderSubtle)
            )
            Column(horizontalAlignment = Alignment.End) {
                BasicText(
                    text = times?.hijriDate?.ifBlank { null } ?: "HIJRI --",
                    style = VibeType.caption.copy(color = colors.textSecondary)
                )
                DotMatrixText(text = "ALADHAN API", color = colors.textSecondary)
            }
        }

        // Next prayer banner.
        CustomCard(active = true, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                val label = next?.first?.name?.uppercase() ?: "LOADING"
                DotMatrixText(text = "$label IN", color = colors.textSecondary)
                Spacer(Modifier.height(4.dp))
                if (next != null) {
                    LiveCountdown(targetMillis = next.second, redEnabled = red)
                } else {
                    BasicText(
                        text = if (ui.loading) "SYNCING…" else (ui.error ?: "--:--:--"),
                        style = VibeType.clock.copy(color = colors.textSecondary)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DotMatrixText(
                        text = "TAP SETTINGS FOR METHOD + OFFSETS",
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Schedule list: all 6 prayers.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val order = listOf(Prayer.Fajr, Prayer.Sunrise, Prayer.Dhuhr, Prayer.Asr, Prayer.Maghrib, Prayer.Isha)
            if (times == null) {
                order.forEach { p ->
                    PrayerRow(name = p.name, millis = null, offset = 0, isActive = false, red = red)
                }
            } else {
                order.forEach { p ->
                    val off = ui.settings.offsets[p.name] ?: 0
                    PrayerRow(
                        name = p.name,
                        millis = times.millisFor(p),
                        offset = off,
                        isActive = p == active,
                        red = red
                    )
                }
            }
        }

        // Retry / settings affordance.
        if (ui.error != null && times == null) {
            CustomCard(onClick = { viewModel.refresh() }, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    DotMatrixText(text = "RETRY SYNC", color = colors.textPrimary)
                }
            }
        }
        CustomCard(onClick = onSettingsClick, static = true, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                DotMatrixText(text = "OPEN SETTINGS", color = colors.textSecondary)
            }
        }
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun PrayerRow(
    name: String,
    millis: Long?,
    offset: Int,
    isActive: Boolean,
    red: Boolean
) {
    val colors = LocalVibeColors.current
    CustomCard(active = isActive, modifier = Modifier.fillMaxWidth().height(72.dp)) {
        Row(
            Modifier.fillMaxSize().padding(start = 0.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isActive) {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(44.dp)
                        .padding(start = 12.dp, end = 0.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (red) colors.accent else colors.textPrimary)
                )
                Spacer(Modifier.width(12.dp))
            } else {
                Spacer(Modifier.width(16.dp))
            }
            DotMatrixText(text = name, modifier = Modifier.weight(1f))
            if (offset != 0) {
                BasicText(
                    text = if (offset > 0) "+${offset}m" else "${offset}m",
                    style = VibeType.caption.copy(color = colors.textSecondary),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            if (millis != null) {
                PrayerTimeText(timeMillis = millis, redEnabled = red)
            } else {
                BasicText(text = "--:--", style = VibeType.time.copy(color = colors.textDisabled))
            }
        }
    }
}
