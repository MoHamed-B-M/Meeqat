package com.meeqat.azan.worker

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meeqat.azan.data.local.AppDatabase
import com.meeqat.azan.data.repo.CalculationRepository
import com.meeqat.azan.data.repo.PrayerRepository
import com.meeqat.azan.data.repo.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

private const val TAG = "CalendarSync"

/**
 * Weekly calendar sync — refreshes the current month when network is available.
 *
 * Scheduled with [androidx.work.NetworkType.CONNECTED], so it only runs online.
 * Delegates to [PrayerRepository.refreshMonth], which falls back to on-device
 * calculation if the API fails. Exact alarms always read cached Room rows,
 * so they keep working 100% offline regardless of sync outcome.
 */
class CalendarSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val settings = SettingsRepository(appContext)
            val location = try { settings.locationFlow.first() } catch (_: Exception) { null }
            if (location == null) {
                Log.i(TAG, "No location yet, skipping weekly sync")
                return Result.success()
            }
            val db = Room.databaseBuilder(appContext, AppDatabase::class.java, "meeqat.db")
                .fallbackToDestructiveMigration()
                .build()
            return try {
                val calc = CalculationRepository(db, settings)
                val repo = PrayerRepository(appContext, db, settings, calc)
                val today = LocalDate.now(ZoneId.systemDefault())
                repo.refreshMonth(today.year, today.monthValue, location.latitude, location.longitude)
                try { db.close() } catch (_: Exception) {}
                Log.i(TAG, "Weekly calendar sync done")
                Result.success()
            } catch (e: Exception) {
                try { db.close() } catch (_: Exception) {}
                Log.w(TAG, "Weekly sync failed, will retry", e)
                Result.retry()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Weekly sync setup failed", e)
            Result.retry()
        }
    }
}
