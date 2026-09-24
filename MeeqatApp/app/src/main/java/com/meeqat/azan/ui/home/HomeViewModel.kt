package com.meeqat.azan.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar
import com.meeqat.azan.data.repo.CalculationRepository
import com.meeqat.azan.data.repo.LocationRepository
import com.meeqat.azan.data.repo.SettingsRepository
import com.meeqat.azan.data.repo.SoundRepository
import com.meeqat.azan.domain.model.DailyPrayerTimes
import com.meeqat.azan.domain.model.LocationState
import com.meeqat.azan.domain.model.ManualOffset
import com.meeqat.azan.domain.model.Prayer
import com.meeqat.azan.domain.model.QiblaInfo
import com.meeqat.azan.domain.model.SoundConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class HomeUiState(
    val todayTimes: DailyPrayerTimes? = null,
    val rawTimes: DailyPrayerTimes? = null,
    val nextPrayer: Prayer? = null,
    val location: LocationState? = null,
    val soundConfig: SoundConfig? = null,
    val hijriDate: String = "",
    val gregorianDate: String = "",
    val qibla: QiblaInfo? = null
)

class HomeViewModel(
    private val calculationRepository: CalculationRepository = com.meeqat.azan.di.ServiceLocator.calculationRepository,
    private val locationRepository: LocationRepository = com.meeqat.azan.di.ServiceLocator.locationRepository,
    private val settingsRepository: SettingsRepository = com.meeqat.azan.di.ServiceLocator.settingsRepository,
    private val soundRepository: SoundRepository = com.meeqat.azan.di.ServiceLocator.soundRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(
        HomeUiState(
            hijriDate = computeHijriDate(),
            gregorianDate = computeGregorianDate()
        )
    )
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    init {
        observeFlows()
    }

    private fun observeFlows() {
        viewModelScope.launch {
            combine(
                locationRepository.observeLocation(),
                settingsRepository.offsetFlow,
                settingsRepository.globalOffsetFlow,
                settingsRepository.methodFlow,
                soundRepository.observe(),
                calculationRepository.observeAll()
            ) { location, _, _, _, sound, _ ->
                Pair(location as LocationState?, sound as SoundConfig)
            }.collect { (location, sound) ->
                refresh(location, sound)
            }
        }
    }

    private suspend fun refresh(location: LocationState?, soundConfig: SoundConfig?) {
        val adjusted = calculationRepository.todayTimes()
        val raw = fetchRawTimes(adjusted)
        val next = determineNextPrayer(adjusted)
        val qibla = location?.let { computeQibla(it.latitude, it.longitude) }
        _ui.value = _ui.value.copy(
            todayTimes = adjusted,
            rawTimes = raw,
            nextPrayer = next,
            location = location,
            soundConfig = soundConfig,
            hijriDate = computeHijriDate(),
            gregorianDate = computeGregorianDate(),
            qibla = qibla
        )
    }

    private suspend fun fetchRawTimes(adjusted: DailyPrayerTimes?): DailyPrayerTimes? {
        if (adjusted == null) return null
        return try {
            val offset = settingsRepository.offsetFlow.first()
            val global = settingsRepository.globalOffsetFlow.first()
            DailyPrayerTimes(
                date = adjusted.date,
                fajr = adjusted.fajr - (offset.fajr + global) * 60L * 1000L,
                sunrise = adjusted.sunrise - (offset.sunrise + global) * 60L * 1000L,
                dhuhr = adjusted.dhuhr - (offset.dhuhr + global) * 60L * 1000L,
                asr = adjusted.asr - (offset.asr + global) * 60L * 1000L,
                maghrib = adjusted.maghrib - (offset.maghrib + global) * 60L * 1000L,
                isha = adjusted.isha - (offset.isha + global) * 60L * 1000L,
                method = adjusted.method,
                lat = adjusted.lat,
                lng = adjusted.lng
            )
        } catch (_: Exception) {
            adjusted
        }
    }

    private fun determineNextPrayer(times: DailyPrayerTimes?): Prayer? {
        if (times == null) return null
        val now = System.currentTimeMillis()
        for ((prayer, millis) in times.asList()) {
            if (millis > now) return prayer
        }
        return Prayer.Fajr
    }

    private fun computeQibla(lat: Double, lng: Double): QiblaInfo {
        val bearing = qiblaBearing(lat, lng, KAABA_LAT, KAABA_LNG)
        val distance = haversineKm(lat, lng, KAABA_LAT, KAABA_LNG)
        return QiblaInfo(bearingDegrees = bearing, distanceKm = distance, lat = lat, lng = lng)
    }

    companion object {
        const val KAABA_LAT = 21.4225
        const val KAABA_LNG = 39.8262

        fun qiblaBearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
            val phi1 = Math.toRadians(lat1)
            val phi2 = Math.toRadians(lat2)
            val delta = Math.toRadians(lng2 - lng1)
            val y = sin(delta) * cos(phi2)
            val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(delta)
            var bearing = Math.toDegrees(atan2(y, x))
            bearing = (bearing + 360) % 360
            return bearing.toFloat()
        }

        fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val r = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }

        fun computeGregorianDate(): String {
            return try {
                val fmt = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
                LocalDate.now().format(fmt)
            } catch (_: Exception) {
                LocalDate.now().toString()
            }
        }

        fun computeHijriDate(): String {
            return try {
                val gregorian = GregorianCalendar()
                val hijri = UmmalquraCalendar()
                hijri.setTime(gregorian.time)
                val day = hijri.get(Calendar.DAY_OF_MONTH)
                val month = hijri.get(Calendar.MONTH)
                val year = hijri.get(Calendar.YEAR)
                val monthNames = arrayOf(
                    "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
                    "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
                    "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
                )
                val mName = monthNames[month.coerceIn(0, 11)]
                String.format(Locale.getDefault(), "%d %s %d AH", day, mName, year)
            } catch (_: Exception) {
                try {
                    val gregorian = GregorianCalendar()
                    val hijri = UmmalquraCalendar()
                    hijri.set(Calendar.YEAR, gregorian.get(Calendar.YEAR))
                    hijri.set(Calendar.MONTH, gregorian.get(Calendar.MONTH))
                    hijri.set(Calendar.DAY_OF_MONTH, gregorian.get(Calendar.DAY_OF_MONTH))
                    val day = hijri.get(Calendar.DAY_OF_MONTH)
                    val month = hijri.get(Calendar.MONTH)
                    val year = hijri.get(Calendar.YEAR)
                    "$day/${month + 1}/$year AH"
                } catch (_: Exception) {
                    LocalDate.now().toString()
                }
            }
        }
    }
}
