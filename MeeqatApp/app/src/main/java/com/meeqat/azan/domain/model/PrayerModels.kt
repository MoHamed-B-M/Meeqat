package com.meeqat.azan.domain.model

import kotlinx.serialization.Serializable

enum class Prayer { Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha }

data class LocationState(
    val latitude: Double,
    val longitude: Double,
    val city: String? = null,
    val district: String? = null,
    val source: LocationSource = LocationSource.Cached,
)

enum class LocationSource { Precise, Manual, Cached }

data class DailyPrayerTimes(
    val date: String, // yyyy-MM-dd
    val fajr: Long,
    val sunrise: Long,
    val dhuhr: Long,
    val asr: Long,
    val maghrib: Long,
    val isha: Long,
    val method: String = "UmmAlQura",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
) {
    fun timeFor(prayer: Prayer): Long = when (prayer) {
        Prayer.Fajr -> fajr
        Prayer.Sunrise -> sunrise
        Prayer.Dhuhr -> dhuhr
        Prayer.Asr -> asr
        Prayer.Maghrib -> maghrib
        Prayer.Isha -> isha
    }

    fun asList(): List<Pair<Prayer, Long>> = listOf(
        Prayer.Fajr to fajr,
        Prayer.Sunrise to sunrise,
        Prayer.Dhuhr to dhuhr,
        Prayer.Asr to asr,
        Prayer.Maghrib to maghrib,
        Prayer.Isha to isha,
    )
}

@Serializable
data class ManualOffset(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0,
) {
    fun forPrayer(p: Prayer): Int = when (p) {
        Prayer.Fajr -> fajr
        Prayer.Sunrise -> sunrise
        Prayer.Dhuhr -> dhuhr
        Prayer.Asr -> asr
        Prayer.Maghrib -> maghrib
        Prayer.Isha -> isha
    }
    companion object { const val MIN = -60; const val MAX = 60 }
}

@Serializable
data class SoundConfig(
    val useSameForAll: Boolean = true,
    val defaultUri: String? = null,
    val fajrUri: String? = null,
    val dhuhrUri: String? = null,
    val asrUri: String? = null,
    val maghribUri: String? = null,
    val ishaUri: String? = null,
) {
    fun uriFor(prayer: Prayer): String? = if (useSameForAll) defaultUri else when (prayer) {
        Prayer.Fajr -> fajrUri ?: defaultUri
        Prayer.Sunrise -> null
        Prayer.Dhuhr -> dhuhrUri ?: defaultUri
        Prayer.Asr -> asrUri ?: defaultUri
        Prayer.Maghrib -> maghribUri ?: defaultUri
        Prayer.Isha -> ishaUri ?: defaultUri
    }
}

data class QiblaInfo(
    val bearingDegrees: Float, // 0..360 from north
    val distanceKm: Double,
    val lat: Double,
    val lng: Double,
)

enum class CalculationMethod(val displayName: String) {
    UmmAlQura("Umm Al-Qura"),
    Egyptian("Egyptian"),
    Karachi("Karachi"),
    MuslimWorldLeague("Muslim World League"),
    Dubai("Dubai"),
    MoonsightingCommittee("Moonsighting Committee"),
    Kuwait("Kuwait"),
    Qatar("Qatar"),
    Singapore("Singapore"),
    Custom("Custom Angles"),
}

enum class Madhab(val displayName: String) { Shafii("Shafi'i"), Hanafi("Hanafi") }
enum class HighLatitudeRule(val displayName: String) { AngleBased("Angle Based"), MiddleOfNight("Middle of Night"), OneSeventh("One Seventh") }
enum class AzanMode { FullAzan, NotificationOnly, Silent }
