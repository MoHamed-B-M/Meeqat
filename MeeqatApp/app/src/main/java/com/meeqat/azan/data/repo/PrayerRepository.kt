package com.meeqat.azan.data.repo

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.meeqat.azan.data.local.DailyPrayerEntity
import com.meeqat.azan.data.remote.AlAdhanApiClient
import com.meeqat.azan.data.remote.TimingsResult
import com.meeqat.azan.data.remote.aladhanId
import com.meeqat.azan.data.remote.aladhanId
import com.meeqat.azan.domain.model.DailyPrayerTimes
import com.meeqat.azan.domain.model.DataSource
import com.meeqat.azan.domain.model.PrayerSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

private const val TAG = "PrayerRepository"

/**
 * Hybrid online-first repository.
 *
 * 1. Try the AlAdhan API (daily timings / monthly calendar).
 * 2. Cache the API response in Room for fast startup and calendar rendering.
 * 3. On any network failure, timeout, or rate limit → fall back to the
 *    on-device `adhan` engine via [CalculationRepository] (100% offline).
 *
 * Returns a unified [PrayerSchedule] either way and records the origin in
 * DataStore ([DataSource.API] vs [DataSource.LOCAL_CALCULATION]) for
 * debugging and the offline badge. Exact alarms always read the cached
 * Room rows, so they keep working fully offline.
 */
class PrayerRepository constructor(
    private val context: Context,
    private val db: com.meeqat.azan.data.local.AppDatabase,
    private val settings: SettingsRepository,
    private val localCalc: CalculationRepository,
    private val api: AlAdhanApiClient = AlAdhanApiClient(),
) {
    val lastSource: Flow<DataSource> = settings.lastSourceFlow
    val lastSync: Flow<Long> = settings.lastSyncFlow

    suspend fun refreshToday(
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): PrayerSchedule {
        val today = LocalDate.now(zoneId)
        if (hasNetwork()) {
            try {
                val method = settings.methodFlow.first()
                val result = api.fetchDaily(today, latitude, longitude, method)
                persist(result, latitude, longitude, method.aladhanId())
                markSynced(DataSource.API)
                Log.i(TAG, "Today from API (method=${method.aladhanId()})")
                return PrayerSchedule(
                    times = result.toDailyPrayerTimes(latitude, longitude),
                    source = DataSource.API,
                    hijri = result.hijri,
                )
            } catch (e: Exception) {
                Log.w(TAG, "API daily failed, local fallback", e)
            }
        } else {
            Log.i(TAG, "No network, local fallback for today")
        }
        return localToday(latitude, longitude, zoneId)
    }

    suspend fun refreshMonth(
        year: Int,
        month: Int,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): PrayerSchedule? {
        if (hasNetwork()) {
            try {
                val method = settings.methodFlow.first()
                val days = api.fetchMonth(year, month, latitude, longitude, method)
                val entities = days.map { it.toEntity(latitude, longitude) }
                db.dailyPrayerDao().insertAll(entities)
                markSynced(DataSource.API)
                Log.i(TAG, "Month $year-$month from API (${entities.size} days)")
                val today = LocalDate.now(zoneId).toString()
                val todayResult = days.firstOrNull { it.date.toString() == today }
                return todayResult?.let {
                    PrayerSchedule(
                        times = it.toDailyPrayerTimes(latitude, longitude),
                        source = DataSource.API,
                        hijri = it.hijri,
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "API calendar failed, local fallback", e)
            }
        } else {
            Log.i(TAG, "No network, local fallback for $year-$month")
        }
        localCalc.refresh30Days(latitude, longitude, zoneId)
        markSynced(DataSource.LOCAL_CALCULATION)
        val e = db.dailyPrayerDao().getByDate(LocalDate.now(zoneId).toString())
        return e?.let { PrayerSchedule(times = it.toDailyPrayerTimes(), source = DataSource.LOCAL_CALCULATION) }
    }

    private suspend fun localToday(
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
    ): PrayerSchedule {
        localCalc.refresh30Days(latitude, longitude, zoneId)
        markSynced(DataSource.LOCAL_CALCULATION)
        val e = db.dailyPrayerDao().getByDate(LocalDate.now(zoneId).toString())
        val times = e?.toDailyPrayerTimes()
            ?: com.meeqat.azan.domain.engine.PrayerEngine.calculateDailyPrayers(
                latitude, longitude, LocalDate.now(zoneId), zoneId,
                settings.methodFlow.first(), settings.madhabFlow.first(), settings.highLatFlow.first()
            )
        return PrayerSchedule(times = times, source = DataSource.LOCAL_CALCULATION)
    }

    private suspend fun markSynced(source: DataSource) {
        try {
            settings.setLastSource(source)
            settings.setLastSync(System.currentTimeMillis())
        } catch (_: Exception) {}
    }

    private suspend fun persist(result: TimingsResult, lat: Double, lng: Double, methodId: Int) {
        db.dailyPrayerDao().insert(result.toEntity(lat, lng, methodId))
    }

    private fun hasNetwork(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.activeNetwork != null
        } catch (_: Exception) { true }
    }

    private fun TimingsResult.toEntity(lat: Double, lng: Double, methodId: Int = meta.methodId): DailyPrayerEntity =
        DailyPrayerEntity(
            date = date.toString(),
            fajr = timings.fajr,
            sunrise = timings.sunrise,
            dhuhr = timings.dhuhr,
            asr = timings.asr,
            maghrib = timings.maghrib,
            isha = timings.isha,
            method = "AlAdhan:$methodId",
            lat = lat,
            lng = lng,
        )

    private fun TimingsResult.toDailyPrayerTimes(lat: Double, lng: Double): DailyPrayerTimes =
        DailyPrayerTimes(
            date = date.toString(),
            fajr = timings.fajr,
            sunrise = timings.sunrise,
            dhuhr = timings.dhuhr,
            asr = timings.asr,
            maghrib = timings.maghrib,
            isha = timings.isha,
            method = "AlAdhan:${meta.methodId}",
            lat = lat,
            lng = lng,
        )

    private fun DailyPrayerEntity.toDailyPrayerTimes(): DailyPrayerTimes =
        DailyPrayerTimes(
            date = date,
            fajr = fajr,
            sunrise = sunrise,
            dhuhr = dhuhr,
            asr = asr,
            maghrib = maghrib,
            isha = isha,
            method = method,
            lat = lat,
            lng = lng,
        )

    companion object {
        /** This month as [YearMonth], for weekly sync scheduling. */
        fun currentMonth(zoneId: ZoneId = ZoneId.systemDefault()): YearMonth = YearMonth.now()
    }
}
