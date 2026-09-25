package com.vibeprayer.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.vibeprayer.app.data.model.Prayer

object AdhanScheduler {
    fun schedule(context: Context, prayer: Prayer, triggerAt: Long) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
                putExtra("prayer", prayer.name)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                prayer.ordinal,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (_: Exception) {}
    }

    fun cancel(context: Context, prayer: Prayer) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                context,
                prayer.ordinal,
                Intent(context, AdhanAlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(pi)
        } catch (_: Exception) {}
    }
}
