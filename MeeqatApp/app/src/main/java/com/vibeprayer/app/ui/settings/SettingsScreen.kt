package com.vibeprayer.app.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vibeprayer.app.VibeApp
import com.vibeprayer.app.data.model.LocationMode
import com.vibeprayer.app.ui.components.CustomCard
import com.vibeprayer.app.ui.components.DotMatrixText
import com.vibeprayer.app.ui.theme.LocalVibeColors
import com.vibeprayer.app.ui.theme.VibeType
import com.vibeprayer.app.utils.InAppUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val METHOD_NAMES = mapOf(
    1 to "Karachi", 2 to "ISNA", 3 to "MWL", 4 to "Makkah",
    5 to "Egyptian", 9 to "Kuwait", 10 to "Qatar",
    11 to "Singapore", 15 to "Moon Committee", 16 to "Dubai"
)

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as VibeApp
    val settings by app.prefs.settings.collectAsState(initial = com.vibeprayer.app.data.model.UserSettings())
    val colors = LocalVibeColors.current
    val scope = rememberCoroutineScope()
    var cityQuery by remember { mutableStateOf("") }
    var methodOpen by remember { mutableStateOf(false) }
    var updateMsg by remember { mutableStateOf("CHECK FOR UPDATES") }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        scope.launch { app.prefs.setLocationMode(LocationMode.Automatic) }
    }
    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            scope.launch { app.prefs.setAdhanUri(uri.toString()) }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DotMatrixText(text = "SETTINGS")
        BasicText(text = InAppUpdater.VERSION, style = VibeType.caption.copy(color = colors.textSecondary))

        // Location mode segmented.
        SectionTitle("LOCATION")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SegmentButton(
                label = "AUTO GPS",
                selected = settings.locationMode == LocationMode.Automatic,
                modifier = Modifier.weight(1f),
                onClick = {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            )
            SegmentButton(
                label = "MANUAL",
                selected = settings.locationMode == LocationMode.Manual,
                modifier = Modifier.weight(1f),
                onClick = { scope.launch { app.prefs.setLocationMode(LocationMode.Manual) } }
            )
        }
        if (settings.locationMode == LocationMode.Manual) {
            CustomCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BasicTextField(
                        value = cityQuery,
                        onValueChange = { cityQuery = it },
                        textStyle = VibeType.body.copy(color = colors.textPrimary),
                        modifier = Modifier.fillMaxWidth()
                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    )
                    CustomCard(
                        onClick = {
                            val q = cityQuery.trim()
                            if (q.isNotEmpty()) {
                                scope.launch {
                                    val res = withContext(Dispatchers.IO) { app.locationTracker.geocode(q) }
                                    if (res != null) {
                                        app.prefs.setManualLocation(res.lat, res.lng, res.label.ifBlank { q })
                                    }
                                }
                            }
                        },
                        static = true,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                            DotMatrixText(text = "SEARCH CITY", color = colors.textPrimary)
                        }
                    }
                    if (settings.manualLabel.isNotBlank()) {
                        BasicText(
                            text = settings.manualLabel,
                            style = VibeType.caption.copy(color = colors.textSecondary)
                        )
                    }
                }
            }
        }

        // Calculation method.
        SectionTitle("CALCULATION")
        CustomCard(
            onClick = { methodOpen = !methodOpen },
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.fillMaxWidth().padding(14.dp)) {
                DotMatrixText(
                    text = "METHOD: ${METHOD_NAMES[settings.methodId] ?: settings.methodId}",
                    color = colors.textPrimary
                )
            }
        }
        if (methodOpen) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                METHOD_NAMES.forEach { (id, name) ->
                    CustomCard(
                        active = id == settings.methodId,
                        onClick = {
                            scope.launch { app.prefs.setMethod(id) }
                            methodOpen = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(Modifier.fillMaxWidth().padding(12.dp)) {
                            DotMatrixText(text = name.uppercase(), color = colors.textPrimary)
                        }
                    }
                }
            }
        }

        // Theme.
        SectionTitle("THEME")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("oled" to "OLED", "slate" to "SLATE", "light" to "LIGHT").forEach { (id, label) ->
                SegmentButton(
                    label = label,
                    selected = settings.theme == id,
                    modifier = Modifier.weight(1f),
                    onClick = { scope.launch { app.prefs.setTheme(id) } }
                )
            }
        }
        SettingRow(
            label = "ONEPLUS RED ACCENTS",
            value = if (settings.redAccents) "ON" else "OFF",
            onClick = { scope.launch { app.prefs.setRedAccents(!settings.redAccents) } }
        )

        // Offsets.
        SectionTitle("MANUAL OFFSETS (-30..+30)")
        listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { name ->
            val v = settings.offsets[name] ?: 0
            CustomCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DotMatrixText(text = name.uppercase(), modifier = Modifier.weight(1f))
                    StepperButton("-", onClick = { scope.launch { app.prefs.setOffset(name, v - 1) } })
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        text = if (v > 0) "+${v}m" else "${v}m",
                        style = VibeType.body.copy(color = colors.textPrimary),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    StepperButton("+", onClick = { scope.launch { app.prefs.setOffset(name, v + 1) } })
                }
            }
        }

        // Sound.
        SectionTitle("ADHAN SOUND")
        CustomCard(onClick = { soundPicker.launch("audio/*") }, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().padding(14.dp)) {
                DotMatrixText(
                    text = if (settings.adhanUri.isBlank()) "PICK MP3 / WAV" else "SOUND SET — TAP TO CHANGE",
                    color = colors.textPrimary
                )
            }
        }
        if (settings.adhanUri.isNotBlank()) {
            BasicText(
                text = settings.adhanUri.take(80),
                style = VibeType.caption.copy(color = colors.textSecondary)
            )
        }

        // About + updater.
        SectionTitle("ABOUT")
        CustomCard(
            onClick = {
                scope.launch {
                    updateMsg = "CHECKING…"
                    val res = InAppUpdater.checkForUpdates()
                    updateMsg = res ?: "UP TO DATE ${InAppUpdater.VERSION}"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                DotMatrixText(text = updateMsg, color = colors.textPrimary)
            }
        }
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    DotMatrixText(text = text, color = LocalVibeColors.current.textSecondary)
}

@Composable
private fun SegmentButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalVibeColors.current
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.accent.copy(alpha = 0.9f) else colors.bgRaised.copy(alpha = 0.6f), shape)
            .border(1.dp, if (selected) colors.accent else colors.borderSubtle, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        DotMatrixText(text = label, color = colors.textPrimary)
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    val colors = LocalVibeColors.current
    CustomCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            DotMatrixText(text = label, modifier = Modifier.weight(1f))
            BasicText(text = value, style = VibeType.body.copy(color = colors.accent))
        }
    }
}

@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
    val colors = LocalVibeColors.current
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .clip(shape)
            .border(1.dp, colors.borderSubtle, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(text = text, style = VibeType.body.copy(color = colors.textPrimary))
    }
}

@Suppress("unused")
private val RowH = 48.dp
