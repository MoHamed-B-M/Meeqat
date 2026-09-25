package com.vibeprayer.app.domain

import com.vibeprayer.app.R
import com.vibeprayer.app.data.model.Prayer
import com.vibeprayer.app.data.model.PrayerTimesData

object PrayerBackgroundEngine {
    fun backgroundFor(now: Long, times: PrayerTimesData?): Int {
        if (times == null) return R.drawable.default_bg
        return when {
            now in times.fajr until times.sunrise -> R.drawable.fajer
            now in times.dhuhr until times.asr -> R.drawable.doher
            now in times.asr until times.maghrib -> R.drawable.aser
            now in times.maghrib until times.isha -> R.drawable.magreb
            else -> R.drawable.default_bg
        }
    }

    fun currentPrayer(now: Long, times: PrayerTimesData?): Prayer? {
        if (times == null) return null
        val list = times.asList().sortedBy { it.second }
        var current: Prayer? = null
        for ((prayer, millis) in list) {
            if (now >= millis) current = prayer
        }
        return current
    }

    fun nextPrayer(now: Long, times: PrayerTimesData?): Pair<Prayer, Long>? {
        if (times == null) return null
        val sorted = times.asList().sortedBy { it.second }
        for ((prayer, millis) in sorted) {
            if (millis > now) return prayer to millis
        }
        return Prayer.Fajr to (times.fajr + 24L * 60 * 60 * 1000)
    }
}
