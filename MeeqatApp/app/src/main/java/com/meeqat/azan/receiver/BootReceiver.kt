package com.meeqat.azan.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.meeqat.azan.worker.PrayerScheduleWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_DATE_CHANGED
            )
        ) {
            enqueueReschedule(context)
        }
    }

    private fun enqueueReschedule(context: Context) {
        val request = OneTimeWorkRequestBuilder<PrayerScheduleWorker>()
            .addTag("meeqat_reschedule")
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
