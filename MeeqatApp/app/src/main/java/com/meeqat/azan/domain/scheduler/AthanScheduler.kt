package com.meeqat.azan.domain.scheduler

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import com.meeqat.azan.data.repo.SettingsRepository
import com.meeqat.azan.domain.engine.PrayerEngine
import com.meeqat.azan.domain.model.DailyPrayerTimes
import com.meeqat.azan.domain.model.Prayer
import com.meeqat.azan.receiver.AthanReceiver
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

/**
 * Offline-first alarm scheduler for Athan.
 *
 * Schedules exact alarms via [AlarmManager.setExactAndAllowWhileIdle] for the five
 * daily prayers (excluding Sunrise) targeting [AthanReceiver]. Gracefully falls back
 * to [AlarmManagerCompat.setWindow] when SCHEDULE_EXACT_ALARM is denied (Android 12+)
 * or on SecurityException.
 *
 * Alarms are RTC_WAKEUP based on epoch millis produced by [PrayerEngine].
 * All scheduling is local — no network required after prayer times are computed.
 */
class AthanScheduler constructor(
    private val appContext: Context,
    private val settings: SettingsRepository
) {

    companion object {
        const val ACTION_ATHAN_PREFIX = "com.meeqat.azan.ACTION_ATHAN_"
        const val EXTRA_PRAYER = "extra_prayer"
        const val EXTRA_TIME = "extra_time"
        const val CHANNEL_ID = "meeqat_azan_channel"
        const val REQUEST_CODE_BASE = 1000

        /** Stable requestCode per prayer (0..4). Day offset is added for multi-day scheduling. */
        fun requestCodeFor(prayer: Prayer, dayOffset: Int = 0): Int {
            val prayerIndex = when (prayer) {
                Prayer.Fajr -> 0
                Prayer.Sunrise -> 1
                Prayer.Dhuhr -> 2
                Prayer.Asr -> 3
                Prayer.Maghrib -> 4
                Prayer.Isha -> 5
            }
            return REQUEST_CODE_BASE + dayOffset * 10 + prayerIndex
        }

        val SCHEDULABLE_PRAYERS = listOf(
            Prayer.Fajr,
            Prayer.Dhuhr,
            Prayer.Asr,
            Prayer.Maghrib,
            Prayer.Isha
        )
    }

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Azan",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Azan prayer alerts"
                    enableVibration(true)
                    setSound(null, null)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                mgr.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Schedules today's prayers that are still in the future.
     * Cancels previous today's alarms before scheduling to avoid duplicates.
     */
    suspend fun scheduleToday(context: Context, times: DailyPrayerTimes) {
        ensureChannel(context)
        val am = alarmManager(context)
        val now = System.currentTimeMillis()

        // Cancel today's existing alarms first (idempotent)
        cancelToday(context)

        for (prayer in SCHEDULABLE_PRAYERS) {
            val millis = times.timeFor(prayer)
            if (millis <= now) continue

            val intent = Intent(context, AthanReceiver::class.java).apply {
                action = ACTION_ATHAN_PREFIX + prayer.name
                putExtra(EXTRA_PRAYER, prayer.name)
                putExtra(EXTRA_TIME, millis)
                putExtra("extra_date", times.date)
            }
            val requestCode = requestCodeFor(prayer, dayOffset = 0)
            val pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleExactOrWindow(am, millis, pi)
        }
    }

    /**
     * Overload using the application context.
     */
    suspend fun scheduleToday(times: DailyPrayerTimes) = scheduleToday(appContext, times)

    /**
     * Schedules alarms for the next 7 days (including today) for [lat]/[lng].
     * Computes each day's times via [PrayerEngine] using current settings, then
     * schedules only future instants.
     *
     * Fully offline: no network call.
     */
    suspend fun scheduleNext7Days(lat: Double, lng: Double) {
        scheduleNext7Days(appContext, lat, lng)
    }

    suspend fun scheduleNext7Days(context: Context, lat: Double, lng: Double) {
        ensureChannel(context)
        val am = alarmManager(context)
        val zoneId = ZoneId.systemDefault()
        val method = settings.methodFlow.first()
        val madhab = settings.madhabFlow.first()
        val highLat = settings.highLatFlow.first()

        // Cancel all before rescheduling to keep alarm table clean
        cancelAll(context)

        val today = LocalDate.now(zoneId)
        val now = System.currentTimeMillis()

        for (dayOffset in 0 until 7) {
            val date = today.plusDays(dayOffset.toLong())
            val times = try {
                PrayerEngine.calculateDailyPrayers(lat, lng, date, zoneId, method, madhab, highLat)
            } catch (_: Exception) {
                continue
            }

            for (prayer in SCHEDULABLE_PRAYERS) {
                val millis = times.timeFor(prayer)
                if (millis <= now) continue

                val intent = Intent(context, AthanReceiver::class.java).apply {
                    action = ACTION_ATHAN_PREFIX + prayer.name
                    putExtra(EXTRA_PRAYER, prayer.name)
                    putExtra(EXTRA_TIME, millis)
                    putExtra("extra_date", times.date)
                }
                val requestCode = requestCodeFor(prayer, dayOffset)
                val pi = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                scheduleExactOrWindow(am, millis, pi)
            }
        }
    }

    /**
     * Convenience: reads last known location from [SettingsRepository] and schedules
     * next 7 days if available. No-op if location is absent.
     */
    suspend fun scheduleNext7DaysFromSettings() {
        val loc = try { settings.locationFlow.first() } catch (_: Exception) { null } ?: return
        scheduleNext7Days(loc.latitude, loc.longitude)
    }

    fun cancelAll() = cancelAll(appContext)

    fun cancelAll(context: Context) {
        val am = alarmManager(context)
        // Cancel all request codes for 0..7 days × 6 prayers (over-cancel is safe)
        for (dayOffset in 0 until 8) {
            for (prayer in Prayer.entries) {
                val rc = requestCodeFor(prayer, dayOffset)
                val intent = Intent(context, AthanReceiver::class.java).apply {
                    action = ACTION_ATHAN_PREFIX + prayer.name
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    rc,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pi != null) {
                    am.cancel(pi)
                    pi.cancel()
                }
            }
        }
        // Also cancel legacy request codes used by PrayerScheduleWorker (1000+ increment)
        for (rc in 1000..1100) {
            val intent = Intent(context, AthanReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, rc, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                am.cancel(pi)
                pi.cancel()
            }
        }
    }

    private fun cancelToday(context: Context) {
        val am = alarmManager(context)
        for (prayer in SCHEDULABLE_PRAYERS) {
            val rc = requestCodeFor(prayer, 0)
            val intent = Intent(context, AthanReceiver::class.java).apply {
                action = ACTION_ATHAN_PREFIX + prayer.name
            }
            val pi = PendingIntent.getBroadcast(
                context, rc, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                am.cancel(pi)
                pi.cancel()
            }
        }
    }

    private fun scheduleExactOrWindow(am: AlarmManager, triggerAtMillis: Long, pi: PendingIntent) {
        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try { am.canScheduleExactAlarms() } catch (_: Exception) { false }
        } else true

        try {
            if (canScheduleExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                // Fallback: window of 60 seconds — inexact but still wakes device
                AlarmManagerCompat.setWindow(am, AlarmManager.RTC_WAKEUP, triggerAtMillis, 60_000L, pi)
            }
        } catch (e: SecurityException) {
            // Extra fallback if exact alarm permission revoked between check and call
            try {
                AlarmManagerCompat.setWindow(am, AlarmManager.RTC_WAKEUP, triggerAtMillis, 60_000L, pi)
            } catch (_: Exception) {}
        } catch (_: Exception) {
            try {
                AlarmManagerCompat.setWindow(am, AlarmManager.RTC_WAKEUP, triggerAtMillis, 60_000L, pi)
            } catch (_: Exception) {}
        }
    }
}
