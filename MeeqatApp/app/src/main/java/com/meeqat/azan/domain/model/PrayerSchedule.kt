package com.meeqat.azan.domain.model

/**
 * Origin of the prayer times currently served to UI and alarms.
 * Logged on every refresh for debugging; drives the offline badge in the UI.
 */
enum class DataSource { API, LOCAL_CALCULATION }

/**
 * Unified schedule returned to UI and alarm scheduler regardless of origin.
 * [times] holds raw cached millis (manual offsets are applied at read time
 * by [com.meeqat.azan.data.repo.CalculationRepository.todayTimes]).
 */
data class PrayerSchedule(
    val times: DailyPrayerTimes,
    val source: DataSource,
    val hijri: HijriDate? = null,
    val syncedAtMillis: Long = System.currentTimeMillis(),
)

/** Hijri metadata as returned by the AlAdhan API (display only). */
data class HijriDate(
    val day: String,
    val monthEn: String,
    val year: String,
) {
    fun display(): String = "$day $monthEn $year AH".trim()
}
