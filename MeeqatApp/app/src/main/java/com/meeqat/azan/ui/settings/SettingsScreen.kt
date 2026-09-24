package com.meeqat.azan.ui.settings

import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meeqat.azan.data.repo.SettingsRepository
import com.meeqat.azan.domain.model.AzanMode
import com.meeqat.azan.domain.model.CalculationMethod
import com.meeqat.azan.domain.model.ManualOffset
import com.meeqat.azan.domain.model.Prayer
import com.meeqat.azan.domain.model.SoundConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val method = settingsRepository.methodFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalculationMethod.UmmAlQura)
    val offset = settingsRepository.offsetFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ManualOffset())
    val sound = settingsRepository.soundFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SoundConfig())
    val dynamicColor = settingsRepository.dynamicColorFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val location = settingsRepository.locationFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setMethod(m: CalculationMethod) = viewModelScope.launch { settingsRepository.setMethod(m) }
    fun setOffset(o: ManualOffset) = viewModelScope.launch { settingsRepository.setOffset(o) }
    fun setSound(c: SoundConfig) = viewModelScope.launch { settingsRepository.setSound(c) }
    fun setDynamicColor(v: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColor(v) }
    fun setGlobalOffset(v: Int) = viewModelScope.launch { settingsRepository.setGlobalOffset(v) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateLocation: () -> Unit = {},
    onNavigateMethod: () -> Unit = {},
    onNavigateAdjust: () -> Unit = {},
    onNavigateSound: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val method by viewModel.method.collectAsState()
    val offset by viewModel.offset.collectAsState()
    val sound by viewModel.sound.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val location by viewModel.location.collectAsState()

    val scroll = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Text("Settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))

        SettingsGroup(title = "Location") {
            ListItem(
                headlineContent = { Text(location?.city ?: "Automatic") },
                supportingContent = { Text(if (location != null) "${String.format("%.4f", location!!.latitude)}, ${String.format("%.4f", location!!.longitude)} • ${location!!.source.name}" else "Tap to set location") },
                leadingContent = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            Card(
                modifier = Modifier.fillMaxWidth().height(140.dp).padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Map placeholder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Drag pin to set manual location", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        SettingsGroup(title = "Calculation Method") {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.padding(horizontal = 16.dp)) {
                TextField(
                    value = method.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Method") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    CalculationMethod.entries.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m.displayName) },
                            onClick = { viewModel.setMethod(m); expanded = false }
                        )
                    }
                }
            }
        }

        SettingsGroup(title = "Adjustments  (-60..+60 min)") {
            Prayer.entries.filter { it != Prayer.Sunrise }.forEach { prayer ->
                PrayerAdjustRow(
                    prayer = prayer,
                    value = offset.forPrayer(prayer),
                    onValueChange = { v ->
                        val newOffset = when (prayer) {
                            Prayer.Fajr -> offset.copy(fajr = v)
                            Prayer.Sunrise -> offset.copy(sunrise = v)
                            Prayer.Dhuhr -> offset.copy(dhuhr = v)
                            Prayer.Asr -> offset.copy(asr = v)
                            Prayer.Maghrib -> offset.copy(maghrib = v)
                            Prayer.Isha -> offset.copy(isha = v)
                        }
                        viewModel.setOffset(newOffset)
                    }
                )
                if (prayer != Prayer.Isha) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
        }

        SettingsGroup(title = "Sound") {
            SoundPickerSection(sound = sound, onSoundChange = { viewModel.setSound(it) })
        }

        SettingsGroup(title = "Notifications") {
            AzanModeSection()
        }

        SettingsGroup(title = "Appearance") {
            ListItem(
                headlineContent = { Text("Dynamic color") },
                supportingContent = { Text("Use wallpaper colors (Android 12+)") },
                leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null) },
                trailingContent = { Switch(checked = dynamicColor, onCheckedChange = { viewModel.setDynamicColor(it) }) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            var darkMode by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text("Dark theme") },
                supportingContent = { Text(if (darkMode) "Dark" else "Light / System") },
                leadingContent = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
                trailingContent = { Switch(checked = darkMode, onCheckedChange = { darkMode = it }) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                SegmentedButton(selected = !darkMode, onClick = { darkMode = false }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)) { Text("Light") }
                SegmentedButton(selected = darkMode, onClick = { darkMode = true }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)) { Text("Dark") }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            content()
        }
    }
}

@Composable
private fun PrayerAdjustRow(
    prayer: Prayer,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    val view = LocalView.current
    val baseMillis = remember { System.currentTimeMillis() }
    val calculated = formatMinutes(baseMillis)
    val adjusted = formatMinutes(baseMillis + value * 60L * 1000L)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(prayer.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(90.dp))
            AssistChip(
                onClick = {},
                label = { Text("Calculated: $calculated → Adjusted: $adjusted", style = MaterialTheme.typography.labelSmall) },
                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onValueChange((value - 1).coerceIn(-60, 60))
            }) { Icon(Icons.Filled.Remove, contentDescription = "Decrease") }
            Slider(
                value = value.toFloat(),
                onValueChange = { v ->
                    val iv = v.toInt().coerceIn(-60, 60)
                    if (iv != value) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onValueChange(iv)
                },
                valueRange = -60f..60f,
                steps = 119,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onValueChange((value + 1).coerceIn(-60, 60))
            }) { Icon(Icons.Filled.Add, contentDescription = "Increase") }
            Text(
                text = if (value >= 0) "+$value" else "$value",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(40.dp)
            )
        }
    }
}

@Composable
private fun SoundPickerSection(
    sound: SoundConfig,
    onSoundChange: (SoundConfig) -> Unit
) {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var selectedPrayer by remember { mutableStateOf(Prayer.Fajr) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            val uriStr = uri.toString()
            val newConfig = if (sound.useSameForAll) {
                sound.copy(defaultUri = uriStr)
            } else {
                when (selectedPrayer) {
                    Prayer.Fajr -> sound.copy(fajrUri = uriStr)
                    Prayer.Dhuhr -> sound.copy(dhuhrUri = uriStr)
                    Prayer.Asr -> sound.copy(asrUri = uriStr)
                    Prayer.Maghrib -> sound.copy(maghribUri = uriStr)
                    Prayer.Isha -> sound.copy(ishaUri = uriStr)
                    else -> sound.copy(defaultUri = uriStr)
                }
            }
            onSoundChange(newConfig)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Per-prayer Azan", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = !sound.useSameForAll, onCheckedChange = { perPrayer -> onSoundChange(sound.copy(useSameForAll = !perPrayer)) })
        }

        if (!sound.useSameForAll) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(Prayer.Fajr, Prayer.Dhuhr, Prayer.Asr, Prayer.Maghrib, Prayer.Isha).forEachIndexed { idx, p ->
                    SegmentedButton(
                        selected = selectedPrayer == p,
                        onClick = { selectedPrayer = p },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 5)
                    ) { Text(p.name.take(2)) }
                }
            }
        }

        TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Built-in") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("My Files") })
        }

        when (tab) {
            0 -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Makkah", "Madinah", "Al-Aqsa", "Egypt", "Turkey", "Morocco").forEach { name ->
                        ListItem(
                            headlineContent = { Text(name) },
                            supportingContent = { Text("Built-in • tap to preview") },
                            leadingContent = { Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = {
                                AssistChip(onClick = { onSoundChange(sound.copy(defaultUri = "asset://$name")) }, label = { Text("Use") })
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
            1 -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val currentUri = if (sound.useSameForAll) sound.defaultUri else sound.uriFor(selectedPrayer)
                    if (currentUri != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(currentUri.substringAfterLast("/").take(32), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    Text("Custom file", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                }
                                AssistChip(onClick = {
                                    val cleared = if (sound.useSameForAll) sound.copy(defaultUri = null) else when (selectedPrayer) {
                                        Prayer.Fajr -> sound.copy(fajrUri = null)
                                        Prayer.Dhuhr -> sound.copy(dhuhrUri = null)
                                        Prayer.Asr -> sound.copy(asrUri = null)
                                        Prayer.Maghrib -> sound.copy(maghribUri = null)
                                        Prayer.Isha -> sound.copy(ishaUri = null)
                                        else -> sound
                                    }
                                    onSoundChange(cleared)
                                }, label = { Text("Clear") })
                            }
                        }
                    }
                    Card(
                        onClick = { pickLauncher.launch(arrayOf("audio/*")) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pick audio file", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Text("Supports mp3, m4a, wav, ogg, flac • < 5 min • stored with persistable permission", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AzanModeSection() {
    val modes = remember { mutableStateOf(mapOf<Prayer, AzanMode>()) }
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Prayer.entries.filter { it != Prayer.Sunrise }.forEach { p ->
            val current = modes.value[p] ?: AzanMode.FullAzan
            ListItem(
                headlineContent = { Text(p.name) },
                supportingContent = { Text(current.name) },
                leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                trailingContent = {
                    SingleChoiceSegmentedButtonRow {
                        AzanMode.entries.forEachIndexed { idx, m ->
                            SegmentedButton(
                                selected = current == m,
                                onClick = { modes.value = modes.value.toMutableMap().apply { put(p, m) } },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3)
                            ) { Text(m.name.take(1)) }
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
private fun formatMinutes(millis: Long): String = try { timeFmt.format(Instant.ofEpochMilli(millis)) } catch (_: Exception) { "--:--" }
