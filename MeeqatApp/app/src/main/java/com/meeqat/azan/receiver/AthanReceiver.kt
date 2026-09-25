package com.meeqat.azan.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.meeqat.azan.ui.athan.AzanActivity
import com.meeqat.azan.service.AzanForegroundService

/**
 * Receives exact alarm intents from [com.meeqat.azan.domain.scheduler.AthanScheduler]
 * and hands off to [AzanForegroundService] for playback.
 *
 * Responsibilities:
 * - Acquires a short PARTIAL_WAKE_LOCK (10 s) to keep CPU awake until the service starts.
 * - Starts AzanForegroundService via startForegroundService (required on Android 8+).
 * - Shows a fallback high-priority notification with Stop/Mute actions if the service
 *   cannot be started (e.g., background start restrictions).
 *
 * This receiver is fully offline — no network access.
 */
class AthanReceiver : BroadcastReceiver() {

    companion object {
        private const val WAKE_LOCK_TAG = "Meeqat:AthanWakeLock"
        private const val WAKE_LOCK_TIMEOUT_MS = 10_000L
        private const val FALLBACK_CHANNEL_ID = "meeqat_azan_channel"
        private const val FALLBACK_NOTIFICATION_ID = 7002
    }

    override fun onReceive(context: Context, intent: Intent) {
        // goAsync allows us to do short async work without blocking the main thread
        val pending = goAsync()

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        try {
            // Acquire with timeout — auto-releases even if we crash
            wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
        } catch (_: Exception) {}

        try {
            val action = intent.action ?: ""
            val prayerName = intent.getStringExtra("extra_prayer")
                ?: intent.getStringExtra(com.meeqat.azan.domain.scheduler.AthanScheduler.EXTRA_PRAYER)
                ?: intent.getStringExtra(AzanForegroundService.EXTRA_PRAYER)
                // Fallback: extract from action suffix ACTION_ATHAN_Fajr
                ?: action.substringAfterLast("_", "").takeIf { it.isNotBlank() }
                ?: "Fajr"

            val time = intent.getLongExtra("extra_time", System.currentTimeMillis())
                .let { if (it == 0L) System.currentTimeMillis() else it }

            // Start the foreground service that handles audio + ongoing notification
            val serviceIntent = Intent(context, AzanForegroundService::class.java).apply {
                this.action = AzanForegroundService.ACTION_AZAN_TRIGGER
                putExtra(AzanForegroundService.EXTRA_PRAYER, prayerName)
                putExtra(AzanForegroundService.EXTRA_TIME, time)
            }

            var started = false
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                started = true
            } catch (_: IllegalStateException) {
                // Background start denied (Android 12+ restrictions) — fallback to notification
                started = false
            } catch (_: SecurityException) {
                started = false
            } catch (_: Exception) {
                started = false
            }

            if (!started) {
                showFallbackNotification(context, prayerName, time)
            }
        } finally {
            try {
                // Pending result must be finished
                pending.finish()
            } catch (_: Exception) {}
            try {
                if (wakeLock.isHeld) wakeLock.release()
            } catch (_: Exception) {}
        }
    }

    private fun showFallbackNotification(context: Context, prayerName: String, time: Long) {
        try {
            ensureChannel(context)

            val openIntent = Intent(context, AzanActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(AzanForegroundService.EXTRA_PRAYER, prayerName)
                putExtra(AzanForegroundService.EXTRA_TIME, time)
            }
            val openPi = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val dismissIntent = Intent(context, AzanForegroundService::class.java).apply {
                action = AzanForegroundService.ACTION_DISMISS
            }
            val dismissPi = PendingIntent.getService(
                context, 100, dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(context, AzanForegroundService::class.java).apply {
                action = AzanForegroundService.ACTION_SNOOZE
            }
            val snoozePi = PendingIntent.getService(
                context, 101, snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, FALLBACK_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Azan • $prayerName")
                .setContentText("It's time for $prayerName — tap to open")
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(false)
                .setAutoCancel(true)
                .setFullScreenIntent(openPi, true)
                .addAction(android.R.drawable.ic_delete, "Dismiss", dismissPi)
                .addAction(android.R.drawable.ic_media_pause, "Snooze 5m", snoozePi)
                .setContentIntent(openPi)
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(FALLBACK_NOTIFICATION_ID + prayerName.hashCode() % 100, notification)
        } catch (_: Exception) {}
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(FALLBACK_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    FALLBACK_CHANNEL_ID,
                    "Azan",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Azan prayer alerts"
                    enableVibration(true)
                    setSound(null, null)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(channel)
            }
        }
    }
}
