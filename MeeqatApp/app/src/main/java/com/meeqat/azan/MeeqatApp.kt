package com.meeqat.azan

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.meeqat.azan.worker.CalendarSyncWorker
import java.util.concurrent.TimeUnit

class MeeqatApp : Application() {
    // Manual ServiceLocator init — offline-first, no Hilt
    override fun onCreate() {
        super.onCreate()
        com.meeqat.azan.di.ServiceLocator.init(this)
        enqueueWeeklyCalendarSync()
    }

    /**
     * Weekly online calendar + Hijri sync. Constrained to CONNECTED network so
     * it only runs when online; [CalendarSyncWorker] falls back to on-device
     * calculation if the API fails, and alarms always read cached Room rows.
     */
    private fun enqueueWeeklyCalendarSync() {
        try {
            val request = PeriodicWorkRequestBuilder<CalendarSyncWorker>(7, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "meeqat-calendar-sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } catch (_: Exception) {}
    }
}
