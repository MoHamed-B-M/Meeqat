package com.vibeprayer.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AdhanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = intent.getStringExtra("prayer") ?: "Fajr"
        try {
            val serviceIntent = Intent(context, AdhanService::class.java).apply {
                putExtra("prayer", prayer)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (_: Exception) {}
        showFallbackNotification(context, prayer)
    }

    private fun showFallbackNotification(context: Context, prayer: String) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel("adhan", "Adhan", NotificationManager.IMPORTANCE_HIGH)
                )
            }
            val full = Intent(context, AdhanAlarmActivity::class.java).apply {
                putExtra("prayer", prayer)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context, 0, full,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(context, "adhan")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("VibePrayer — $prayer")
                .setContentText("Time for $prayer prayer")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pi, true)
                .setAutoCancel(true)
                .build()
            nm.notify(prayer.hashCode(), n)
        } catch (_: Exception) {}
    }
}
