package com.meeqat.azan.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meeqat.azan.data.local.AppDatabase
import com.meeqat.azan.data.repo.SettingsRepository
import com.meeqat.azan.domain.model.Prayer
import com.meeqat.azan.service.AzanForegroundService
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class PrayerScheduleWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val db: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "meeqat.db")
            .fallbackToDestructiveMigration()
            .build()
    }
    private val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext)
    }

    override suspend fun doWork(): Result {
        return try {
            scheduleNext7Days()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun scheduleNext7Days() {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val offset = try { settingsRepository.offsetFlow.first() } catch (_: Exception) { com.meeqat.azan.domain.model.ManualOffset() }
        val global = try { settingsRepository.globalOffsetFlow.first() } catch (_: Exception) { 0 }

        val today = LocalDate.now()
        val entities = try { db.dailyPrayerDao().observeAll().first().sortedBy { it.date } } catch (_: Exception) { emptyList() }

        val toSchedule = mutableListOf<Pair<Prayer, Long>>()
        for (i in 0 until 7) {
            val dateStr = today.plusDays(i.toLong()).toString()
            val e = entities.find { it.date == dateStr } ?: continue
            fun adj(base: Long, delta: Int) = base + (delta + global) * 60L * 1000L
            toSchedule += Prayer.Fajr to adj(e.fajr, offset.fajr)
            toSchedule += Prayer.Dhuhr to adj(e.dhuhr, offset.dhuhr)
            toSchedule += Prayer.Asr to adj(e.asr, offset.asr)
            toSchedule += Prayer.Maghrib to adj(e.maghrib, offset.maghrib)
            toSchedule += Prayer.Isha to adj(e.isha, offset.isha)
        }

        val now = System.currentTimeMillis()
        var requestCode = 0
        for ((prayer, millis) in toSchedule) {
            if (millis <= now) { requestCode++; continue }
            val intent = Intent(appContext, AzanForegroundService::class.java).apply {
                action = AzanForegroundService.ACTION_AZAN_TRIGGER
                putExtra(AzanForegroundService.EXTRA_PRAYER, prayer.name)
                putExtra(AzanForegroundService.EXTRA_TIME, millis)
            }
            val pi = PendingIntent.getForegroundService(
                appContext,
                1000 + requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else true

            if (canScheduleExact) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
                } catch (_: SecurityException) {
                    alarmManager.setWindow(AlarmManager.RTC_WAKEUP, millis, 60_000L, pi)
                }
            } else {
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, millis, 60_000L, pi)
            }
            requestCode++
        }
    }
}
