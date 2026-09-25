package com.vibeprayer.app.data.remote

import com.vibeprayer.app.data.model.PrayerTimesData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AlAdhanApi {
    suspend fun fetchDaily(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        methodId: Int
    ): PrayerTimesData = withContext(Dispatchers.IO) {
        val path = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        val url = "https://api.aladhan.com/v1/timings/$path" +
            "?latitude=$latitude&longitude=$longitude&method=$methodId"
        val root = getJson(url)
        if (root.optInt("code", 0) != 200) {
            throw IllegalStateException("AlAdhan HTTP ${root.optInt("code", -1)}")
        }
        parseDay(root.getJSONObject("data"))
    }

    private fun getJson(url: String): JSONObject {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "VibePrayer/1.0")
            connectTimeout = 8_000
            readTimeout = 8_000
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code == 429) throw IllegalStateException("AlAdhan rate limited (429)")
            if (code !in 200..299) throw IllegalStateException("AlAdhan HTTP $code")
            return JSONObject(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseDay(data: JSONObject): PrayerTimesData {
        val timings = data.getJSONObject("timings")
        val dateObj = data.getJSONObject("date")
        val hijriObj = dateObj.getJSONObject("hijri")
        val gregReadable = dateObj.optString("readable", "")
        val hijriMonth = hijriObj.optJSONObject("month")?.optString("en", "").orEmpty()
        val hijriDate = "${hijriObj.optString("day", "")} $hijriMonth ${hijriObj.optString("year", "")} AH".trim()
        val gregStr = dateObj.getJSONObject("gregorian").optString("date", "")
        val meta = data.optJSONObject("meta")
        val tzId = meta?.optString("timezone", "").orEmpty()
        val zone = runCatching { ZoneId.of(tzId) }.getOrDefault(ZoneId.systemDefault())
        val date = runCatching {
            LocalDate.parse(gregStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        }.getOrDefault(LocalDate.now(zone))

        fun millisOf(key: String): Long {
            val raw = timings.optString(key, "").substringBefore(" ").trim()
            val time = LocalTime.parse(raw, DateTimeFormatter.ofPattern("HH:mm"))
            return ZonedDateTime.of(date, time, zone).toInstant().toEpochMilli()
        }

        return PrayerTimesData(
            fajr = millisOf("Fajr"),
            sunrise = millisOf("Sunrise"),
            dhuhr = millisOf("Dhuhr"),
            asr = millisOf("Asr"),
            maghrib = millisOf("Maghrib"),
            isha = millisOf("Isha"),
            hijriDate = hijriDate,
            gregorianDate = gregReadable.ifBlank { date.toString() },
            timezone = tzId.ifBlank { zone.id },
            fromNetwork = true
        )
    }
}
