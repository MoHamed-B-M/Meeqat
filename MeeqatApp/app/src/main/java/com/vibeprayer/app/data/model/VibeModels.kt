package com.vibeprayer.app.data.model

enum class Prayer {
    Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha
}

data class PrayerTimesData(
    val fajr: Long,
    val sunrise: Long,
    val dhuhr: Long,
    val asr: Long,
    val maghrib: Long,
    val isha: Long,
    val hijriDate: String = "",
    val gregorianDate: String = "",
    val timezone: String = "",
    val fromNetwork: Boolean = true
) {
    fun asList(): List<Pair<Prayer, Long>> = listOf(
        Prayer.Fajr to fajr,
        Prayer.Sunrise to sunrise,
        Prayer.Dhuhr to dhuhr,
        Prayer.Asr to asr,
        Prayer.Maghrib to maghrib,
        Prayer.Isha to isha
    )

    fun millisFor(prayer: Prayer): Long = when (prayer) {
        Prayer.Fajr -> fajr
        Prayer.Sunrise -> sunrise
        Prayer.Dhuhr -> dhuhr
        Prayer.Asr -> asr
        Prayer.Maghrib -> maghrib
        Prayer.Isha -> isha
    }
}

enum class LocationMode { Automatic, Manual }

data class UserSettings(
    val locationMode: LocationMode = LocationMode.Automatic,
    val manualLat: Double? = null,
    val manualLng: Double? = null,
    val manualLabel: String = "",
    val methodId: Int = 4,
    val redAccents: Boolean = true,
    val theme: String = "oled",
    val offsets: Map<String, Int> = emptyMap(),
    val adhanUri: String = ""
) {
    fun offsetFor(prayer: Prayer): Int = offsets[prayer.name] ?: 0
}
