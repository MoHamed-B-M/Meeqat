package com.meeqat.azan.domain.engine

import com.batoulapps.adhan.CalculationMethod as AdhanMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule as AdhanHighLat
import com.batoulapps.adhan.Madhab as AdhanMadhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.meeqat.azan.domain.model.CalculationMethod
import com.meeqat.azan.domain.model.DailyPrayerTimes
import com.meeqat.azan.domain.model.HighLatitudeRule
import com.meeqat.azan.domain.model.Madhab
import com.meeqat.azan.domain.model.Prayer
import java.time.LocalDate
import java.time.ZoneId

/**
 * Offline-first prayer calculation engine wrapping com.batoulapps.adhan 1.2.1.
 * Pure computation — no network, no I/O. All dates are produced as epoch millis
 * in the supplied [zoneId] via conversion of the Adhan Date objects (which are
 * absolute instants). Safe to call from any thread.
 */
object PrayerEngine {

    fun calculateDailyPrayers(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        zoneId: ZoneId,
        method: CalculationMethod,
        madhab: Madhab,
        highLatRule: HighLatitudeRule
    ): DailyPrayerTimes {
        require(latitude in -90.0..90.0) { "latitude out of range" }
        require(longitude in -180.0..180.0) { "longitude out of range" }

        val coordinates = Coordinates(latitude, longitude)
        val params = toAdhanParameters(method).apply {
            this.madhab = toAdhanMadhab(madhab)
            this.highLatitudeRule = toAdhanHighLat(highLatRule)
        }

        val components = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val times = PrayerTimes(coordinates, components, params)

        // Adhan returns java.util.Date (absolute instant). Convert directly to epoch millis.
        // No zone conversion needed for the instant itself; zoneId is kept for callers that
        // need to interpret the millis locally and for the date string.
        // If future Adhan versions return Instant, this still works via Date.time.
        val fajrMillis = times.fajr.time
        val sunriseMillis = times.sunrise.time
        val dhuhrMillis = times.dhuhr.time
        val asrMillis = times.asr.time
        val maghribMillis = times.maghrib.time
        val ishaMillis = times.isha.time

        return DailyPrayerTimes(
            date = date.toString(), // yyyy-MM-dd
            fajr = fajrMillis,
            sunrise = sunriseMillis,
            dhuhr = dhuhrMillis,
            asr = asrMillis,
            maghrib = maghribMillis,
            isha = ishaMillis,
            method = method.name,
            lat = latitude,
            lng = longitude
        )
    }

    /**
     * Returns the next upcoming prayer after [now]. Scans in chronological order
     * Fajr → Sunrise → Dhuhr → Asr → Maghrib → Isha. Returns null if all prayers
     * for this day are in the past (caller should compute next day).
     */
    fun getNextPrayer(times: DailyPrayerTimes, now: Long): Prayer? {
        // Ordered check — earliest future prayer wins.
        return when {
            now < times.fajr -> Prayer.Fajr
            now < times.sunrise -> Prayer.Sunrise
            now < times.dhuhr -> Prayer.Dhuhr
            now < times.asr -> Prayer.Asr
            now < times.maghrib -> Prayer.Maghrib
            now < times.isha -> Prayer.Isha
            else -> null
        }
    }

    /**
     * Convenience: returns the next prayer and its epoch millis, or null if none remain today.
     */
    fun getNextPrayerWithTime(times: DailyPrayerTimes, now: Long): Pair<Prayer, Long>? {
        val prayer = getNextPrayer(times, now) ?: return null
        return prayer to times.timeFor(prayer)
    }

    // ---- Mapping ----

    private fun toAdhanParameters(method: CalculationMethod): CalculationParameters {
        // Use valueOf lookup to avoid compile-time failure if adhan version lacks a constant.
        // Falls back gracefully to OTHER / CUSTOM / MUSLIM_WORLD_LEAGUE.
        fun resolve(name: String): AdhanMethod {
            return runCatching { AdhanMethod.valueOf(name) }.getOrNull()
                ?: runCatching { AdhanMethod.valueOf("OTHER") }.getOrNull()
                ?: runCatching { AdhanMethod.valueOf("CUSTOM") }.getOrNull()
                ?: AdhanMethod.valueOf("MUSLIM_WORLD_LEAGUE")
        }
        val adhanMethod = when (method) {
            CalculationMethod.UmmAlQura -> resolve("UMM_AL_QURA")
            CalculationMethod.Egyptian -> resolve("EGYPTIAN")
            CalculationMethod.Karachi -> resolve("KARACHI")
            CalculationMethod.MuslimWorldLeague -> resolve("MUSLIM_WORLD_LEAGUE")
            CalculationMethod.Dubai -> resolve("DUBAI")
            CalculationMethod.MoonsightingCommittee -> resolve("MOON_SIGHTING_COMMITTEE")
            CalculationMethod.Kuwait -> resolve("KUWAIT")
            CalculationMethod.Qatar -> resolve("QATAR")
            CalculationMethod.Singapore -> resolve("SINGAPORE")
            CalculationMethod.Custom -> resolve("OTHER")
        }
        return adhanMethod.parameters
    }

    private fun toAdhanMadhab(madhab: Madhab): AdhanMadhab = when (madhab) {
        Madhab.Shafii -> AdhanMadhab.SHAFI
        Madhab.Hanafi -> AdhanMadhab.HANAFI
    }

    private fun toAdhanHighLat(rule: HighLatitudeRule): AdhanHighLat = when (rule) {
        HighLatitudeRule.AngleBased -> AdhanHighLat.TWILIGHT_ANGLE
        HighLatitudeRule.MiddleOfNight -> AdhanHighLat.MIDDLE_OF_THE_NIGHT
        HighLatitudeRule.OneSeventh -> AdhanHighLat.SEVENTH_OF_THE_NIGHT
    }
}
