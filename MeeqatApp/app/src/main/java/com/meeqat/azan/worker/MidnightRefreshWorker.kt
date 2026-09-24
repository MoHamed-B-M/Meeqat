package com.meeqat.azan.worker

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meeqat.azan.data.local.AppDatabase
import com.meeqat.azan.data.repo.SettingsRepository
import com.meeqat.azan.domain.engine.PrayerEngine
import com.meeqat.azan.domain.model.LocationState
import com.meeqat.azan.domain.scheduler.AthanScheduler
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Midnight refresh worker — runs at 00:05 local time.
 *
 * - Recomputes today's (and next 7 days) prayer times via [PrayerEngine] and reschedules
 *   exact alarms via [AthanScheduler].
 * - Detects significant location drift (> 30 km from last saved location) and triggers
 *   a full 30-day recalculation when needed.
 * - Fully offline: uses only local DB + DataStore + Adhan computation. No network.
 *
 * Scheduling: enqueue via WorkManager PeriodicWork at ~00:05 or via BootReceiver's
 * OneTimeWork. The worker itself does not loop — it runs once and [Result.success]
 * tells WorkManager to apply the next periodic interval.
 */
class MidnightRefreshWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val db: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "meeqat.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    private val settings: SettingsRepository by lazy {
        SettingsRepository(appContext)
    }

    private val scheduler: AthanScheduler by lazy {
        AthanScheduler(appContext, settings)
    }

    override suspend fun doWork(): Result {
        return try {
            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now(zoneId)

            // Load current settings
            val location: LocationState? = try { settings.locationFlow.first() } catch (_: Exception) { null }
            val method = try { settings.methodFlow.first() } catch (_: Exception) { com.meeqat.azan.domain.model.CalculationMethod.UmmAlQura }
            val madhab = try { settings.madhabFlow.first() } catch (_: Exception) { com.meeqat.azan.domain.model.Madhab.Shafii }
            val highLat = try { settings.highLatFlow.first() } catch (_: Exception) { com.meeqat.azan.domain.model.HighLatitudeRule.AngleBased }

            if (location == null) {
                // No location yet — nothing to schedule, but not a failure
                return Result.success()
            }

            // ---- Location drift check (> 30 km) ----
            // If a fresher location is available via inputData (optional), compare distance.
            // Otherwise we just ensure stored location is still valid.
            val inputLat = inputData.getDouble("input_lat", Double.NaN)
            val inputLng = inputData.getDouble("input_lng", Double.NaN)
            var effectiveLocation = location
            var drifted = false

            if (!inputLat.isNaN() && !inputLng.isNaN()) {
                val dist = haversineKm(location.latitude, location.longitude, inputLat, inputLng)
                if (dist > 30.0) {
                    drifted = true
                    effectiveLocation = location.copy(latitude = inputLat, longitude = inputLng)
                    // Persist the new location so future runs use it
                    try { settings.setLocation(effectiveLocation) } catch (_: Exception) {}
                }
            } else {
                // Also check against last known FusedLocation if available in DataStore lat/lng vs DB's lat/lng
                // For now, no extra check — drift detection via inputData is the contract.
            }

            // ---- Recalculate and persist 30 days (keep DB fresh) ----
            try {
                recalculateAndPersist(effectiveLocation.latitude, effectiveLocation.longitude, zoneId, method, madhab, highLat, today)
            } catch (_: Exception) {
                // Persistence failure should not block alarm scheduling — in-memory schedule still possible
            }

            // ---- Reschedule alarms for next 7 days ----
            try {
                scheduler.scheduleNext7Days(effectiveLocation.latitude, effectiveLocation.longitude)
            } catch (_: Exception) {
                // Alarm scheduling can fail on exact alarm denial — not retryable as periodic
            }

            // Prune old prayer rows (> 7 days ago) to keep DB lean
            try {
                val cutoff = today.minusDays(7).toString()
                db.dailyPrayerDao().pruneBefore(cutoff)
            } catch (_: Exception) {}

            Result.success()
        } catch (e: Exception) {
            // Transient failure (e.g., DB locked) — retry with backoff
            Result.retry()
        }
    }

    private suspend fun recalculateAndPersist(
        lat: Double,
        lng: Double,
        zoneId: ZoneId,
        method: com.meeqat.azan.domain.model.CalculationMethod,
        madhab: com.meeqat.azan.domain.model.Madhab,
        highLat: com.meeqat.azan.domain.model.HighLatitudeRule,
        today: LocalDate
    ) {
        val entities = (0 until 30).map { i ->
            val date = today.plusDays(i.toLong())
            val times = PrayerEngine.calculateDailyPrayers(lat, lng, date, zoneId, method, madhab, highLat)
            com.meeqat.azan.data.local.DailyPrayerEntity(
                date = times.date,
                fajr = times.fajr,
                sunrise = times.sunrise,
                dhuhr = times.dhuhr,
                asr = times.asr,
                maghrib = times.maghrib,
                isha = times.isha,
                method = times.method,
                lat = lat,
                lng = lng
            )
        }
        db.dailyPrayerDao().insertAll(entities)
    }

    /** Haversine distance in km between two WGS-84 points. */
    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0088
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
