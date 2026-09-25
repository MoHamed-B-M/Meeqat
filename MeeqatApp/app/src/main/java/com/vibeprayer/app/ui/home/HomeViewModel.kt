package com.vibeprayer.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.vibeprayer.app.VibeApp
import com.vibeprayer.app.data.model.LocationMode
import com.vibeprayer.app.data.model.Prayer
import com.vibeprayer.app.data.model.PrayerTimesData
import com.vibeprayer.app.data.model.UserSettings
import com.vibeprayer.app.domain.PrayerBackgroundEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val loading: Boolean = true,
    val times: PrayerTimesData? = null,
    val next: Pair<Prayer, Long>? = null,
    val error: String? = null,
    val settings: UserSettings = UserSettings(),
    val locationLabel: String = "Locating…"
)

class HomeViewModel(private val app: VibeApp) : ViewModel() {
    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { observeSettings() }
        refresh()
    }

    private suspend fun observeSettings() {
        app.prefs.settings.collect { settings ->
            val cur = _ui.value
            _ui.value = cur.copy(settings = settings)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            try {
                val settings = app.prefs.settings.first()
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now(zone)
                val loc = resolveLocation(settings)
                val label = loc.third
                val result = app.api.fetchDaily(today, loc.first, loc.second, settings.methodId)
                val adjusted = applyOffsets(result, settings)
                val now = System.currentTimeMillis()
                _ui.value = _ui.value.copy(
                    loading = false,
                    times = adjusted,
                    next = PrayerBackgroundEngine.nextPrayer(now, adjusted),
                    locationLabel = label
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Network error")
            }
        }
    }

    private suspend fun resolveLocation(settings: UserSettings): Triple<Double, Double, String> {
        if (settings.locationMode == LocationMode.Manual && settings.manualLat != null && settings.manualLng != null) {
            return Triple(settings.manualLat, settings.manualLng, settings.manualLabel.ifBlank { "Manual location" })
        }
        val hasFine = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            return Triple(21.4225, 39.8262, "Makkah (default — grant location)")
        }
        val ll = app.locationTracker.current() ?: app.locationTracker.lastKnown()
        if (ll != null) return Triple(ll.lat, ll.lng, "GPS ${"%.2f".format(ll.lat)}, ${"%.2f".format(ll.lng)}")
        return Triple(21.4225, 39.8262, "Makkah (offline default)")
    }

    private fun applyOffsets(data: PrayerTimesData, settings: UserSettings): PrayerTimesData {
        fun adj(base: Long, prayer: Prayer): Long {
            val m = settings.offsets[prayer.name] ?: 0
            return base + m * 60L * 1000L
        }
        return data.copy(
            fajr = adj(data.fajr, Prayer.Fajr),
            sunrise = adj(data.sunrise, Prayer.Sunrise),
            dhuhr = adj(data.dhuhr, Prayer.Dhuhr),
            asr = adj(data.asr, Prayer.Asr),
            maghrib = adj(data.maghrib, Prayer.Maghrib),
            isha = adj(data.isha, Prayer.Isha)
        )
    }

    companion object {
        fun factory(app: VibeApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return HomeViewModel(app) as T
            }
        }
    }
}
