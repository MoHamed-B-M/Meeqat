package com.vibeprayer.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AdhanService : Service() {
    private var player: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayer = intent?.getStringExtra("prayer") ?: "Prayer"
        startForeground(1, buildNotification(prayer))
        playConfiguredSound()
        return START_NOT_STICKY
    }

    private fun buildNotification(prayer: String): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel("adhan_play", "Adhan playback", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        return NotificationCompat.Builder(this, "adhan_play")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("VibePrayer — $prayer")
            .setContentText("Playing adhan")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    private fun playConfiguredSound() {
        try {
            player?.release()
            player = MediaPlayer().apply {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                // Slice 1: system default alarm tone; per-prayer SAF uri lands in slice 2.
                setDataSource(this@AdhanService, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                isLooping = false
                prepare()
                start()
                setOnCompletionListener { stopSelf() }
            }
        } catch (_: Exception) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {}
        player = null
        super.onDestroy()
    }
}
