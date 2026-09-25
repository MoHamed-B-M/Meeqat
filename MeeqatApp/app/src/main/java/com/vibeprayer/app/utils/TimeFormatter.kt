package com.vibeprayer.app.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val clockFmt = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

fun formatClock(now: Long): String {
    return try {
        clockFmt.format(Instant.ofEpochMilli(now))
    } catch (_: Exception) {
        "--:--:--"
    }
}

fun formatCountdown(millis: Long): String {
    val total = (millis.coerceAtLeast(0) / 1000)
    return "%02d:%02d:%02d".format(total / 3600, (total % 3600) / 60, total % 60)
}
