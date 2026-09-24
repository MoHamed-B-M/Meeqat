package com.meeqat.azan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.meeqat.azan.MainActivity
import com.meeqat.azan.di.ServiceLocator
import com.meeqat.azan.domain.model.Prayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AzanForegroundService : Service() {

    private val settingsRepository by lazy { ServiceLocator.settingsRepository }

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val channelId = "meeqat_azan_channel"
    private val notificationId = 7001

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_AZAN_TRIGGER -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER) ?: Prayer.Fajr.name
                val time = intent.getLongExtra(EXTRA_TIME, System.currentTimeMillis())
                startAzan(prayerName, time)
            }
            ACTION_DISMISS -> {
                stopAzan()
                stopSelf()
            }
            ACTION_SNOOZE -> {
                snooze()
            }
        }
        return START_NOT_STICKY
    }

    private fun startAzan(prayerName: String, time: Long) {
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_PRAYER, prayerName)
        }
        val fullScreenPending = PendingIntent.getActivity(this, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val dismissIntent = Intent(this, AzanForegroundService::class.java).apply { action = ACTION_DISMISS }
        val dismissPending = PendingIntent.getService(this, 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val snoozeIntent = Intent(this, AzanForegroundService::class.java).apply { action = ACTION_SNOOZE }
        val snoozePending = PendingIntent.getService(this, 2, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Azan • $prayerName")
            .setContentText("It's time for $prayerName")
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPending, true)
            .addAction(android.R.drawable.ic_delete, "Dismiss", dismissPending)
            .addAction(android.R.drawable.ic_media_pause, "Snooze 5m", snoozePending)
            .setContentIntent(fullScreenPending)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(notificationId, notification)
        }

        scope.launch {
            val uriString = resolveUriForPrayer(prayerName)
            playAudio(uriString)
        }
    }

    private suspend fun resolveUriForPrayer(prayerName: String): String? {
        return try {
            val config = settingsRepository.soundFlow.first()
            val prayer = runCatching { Prayer.valueOf(prayerName) }.getOrNull() ?: Prayer.Fajr
            config.uriFor(prayer) ?: config.defaultUri
        } catch (_: Exception) { null }
    }

    private fun playAudio(uriString: String?) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                if (uriString != null && uriString.startsWith("asset://")) {
                    val assetName = uriString.removePrefix("asset://")
                    val afd = try { applicationContext.assets.openFd("azan/$assetName.mp3") } catch (_: Exception) { null }
                    if (afd != null) {
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                    } else {
                        setDataSource(applicationContext, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                    }
                } else if (uriString != null && uriString.startsWith("content://")) {
                    setDataSource(applicationContext, android.net.Uri.parse(uriString))
                } else {
                    setDataSource(applicationContext, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                }
                isLooping = false
                prepare()
                start()
                setOnCompletionListener {
                    stopSelf()
                }
            }
        } catch (_: Exception) {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(applicationContext, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                mediaPlayer?.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                mediaPlayer?.start()
            } catch (_: Exception) {}
        }
    }

    private fun snooze() {
        stopAzan()
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(this, AzanForegroundService::class.java).apply { action = ACTION_AZAN_TRIGGER }
        val pi = PendingIntent.getForegroundService(this, 9999, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val triggerAt = System.currentTimeMillis() + 5 * 60 * 1000L
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setWindow(android.app.AlarmManager.RTC_WAKEUP, triggerAt, 60_000L, pi)
            } else {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: Exception) {}
        stopSelf()
    }

    private fun stopAzan() {
        try { mediaPlayer?.stop() } catch (_: Exception) {}
        try { mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(channelId, "Azan", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Azan prayer alerts"
                enableVibration(true)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            mgr.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        try { mediaPlayer?.release() } catch (_: Exception) {}
        super.onDestroy()
    }

    companion object {
        const val ACTION_AZAN_TRIGGER = "com.meeqat.azan.ACTION_AZAN_TRIGGER"
        const val ACTION_DISMISS = "com.meeqat.azan.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.meeqat.azan.ACTION_SNOOZE"
        const val EXTRA_PRAYER = "extra_prayer"
        const val EXTRA_TIME = "extra_time"
    }
}
