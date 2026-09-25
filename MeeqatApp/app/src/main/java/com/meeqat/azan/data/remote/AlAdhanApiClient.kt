package com.meeqat.azan.data.remote

import android.util.Log
import com.meeqat.azan.domain.model.CalculationMethod
import com.meeqat.azan.domain.model.HijriDate
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

private const val TAG = "AlAdhanApi"

/** Strongly-typed daily timings in epoch millis (absolute instants). */
data class PrayerTimings(
    val fajr: Long,
    val sunrise: Long,
    val dhuhr: Long,
    val asr: Long,
    val maghrib: Long,
    val isha: Long,
)

/** API metadata for a timings payload (debugging + timezone resolution). */
data class PrayerMeta(
    val methodId: Int,
    val methodName: String,
    val timezone: String,
)

/** One day of API data: timings + Hijri metadata + method meta. */
data class TimingsResult(
    val date: LocalDate,
    val timings: PrayerTimings,
    val hijri: HijriDate,
    val meta: PrayerMeta,
)

/**
 * Minimal AlAdhan REST client. No API key required, no new dependencies
 * (HttpURLConnection + org.json only).
 *
 * - Daily:   GET /v1/timings/{DD-MM-YYYY}?latitude=&longitude=&method=
 * - Monthly: GET /v1/calendar/{YYYY}/{M}?latitude=&longitude=&method=
 *
 * All network errors (rate limits, timeouts, bad HTTP) throw and are left
 * to [com.meeqat.azan.data.repo.PrayerRepository] for local fallback.
 */
class AlAdhanApiClient {

    suspend fun fetchDaily(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        method: CalculationMethod,
    ): TimingsResult = withContext(Dispatchers.IO) {
        val datePath = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        val url = "https://api.aladhan.com/v1/timings/$datePath" +
            "?latitude=$latitude&longitude=$longitude&method=${method.aladhanId()}"
        val root = getJson(url)
        if (root.optInt("code", 0) != 200) {
            throw IllegalStateException("AlAdhan timings HTTP ${root.optInt("code", -1)}: ${root.optString("status")}")
        }
        parseDay(root.getJSONObject("data"))
    }

    suspend fun fetchMonth(
        year: Int,
        month: Int,
        latitude: Double,
        longitude: Double,
        method: CalculationMethod,
    ): List<TimingsResult> = withContext(Dispatchers.IO) {
        val url = "https://api.aladhan.com/v1/calendar/$year/$month" +
            "?latitude=$latitude&longitude=$longitude&method=${method.aladhanId()}"
        val root = getJson(url)
        if (root.optInt("code", 0) != 200) {
            throw IllegalStateException("AlAdhan calendar HTTP ${root.optInt("code", -1)}: ${root.optString("status")}")
        }
        val data = root.getJSONArray("data")
        buildList {
            for (i in 0 until data.length()) {
                try {
                    add(parseDay(data.getJSONObject(i)))
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping unparseable calendar day $i", e)
                }
            }
        }.also {
            if (it.isEmpty()) throw IllegalStateException("AlAdhan calendar returned no usable days")
        }
    }

    private fun getJson(url: String): JSONObject {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Meeqat/1.0")
            connectTimeout = 8_000
            readTimeout = 8_000
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code == 429) throw IllegalStateException("AlAdhan rate limited (HTTP 429)")
            if (code !in 200..299) throw IllegalStateException("AlAdhan HTTP $code")
            return JSONObject(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseDay(data: JSONObject): TimingsResult {
        val timings = data.getJSONObject("timings")
        val dateObj = data.getJSONObject("date")
        val hijriObj = dateObj.getJSONObject("hijri")
        val gregorianStr = dateObj.getJSONObject("gregorian").optString("date", "") // dd-MM-yyyy
        val metaObj = data.optJSONObject("meta")
        val methodObj = metaObj?.optJSONObject("method")
        val tzId = metaObj?.optString("timezone", "").orEmpty()
        val zoneId = runCatching { ZoneId.of(tzId) }.getOrDefault(ZoneId.systemDefault())

        val date = runCatching {
            LocalDate.parse(gregorianStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        }.getOrDefault(LocalDate.now(zoneId))

        fun millisOf(key: String): Long {
            // Values look like "05:12" or "05:12 (EET)" — strip any suffix.
            val raw = timings.optString(key, "").substringBefore(" ").trim()
            val time = LocalTime.parse(raw, DateTimeFormatter.ofPattern("HH:mm"))
            return ZonedDateTime.of(date, time, zoneId).toInstant().toEpochMilli()
        }

        return TimingsResult(
            date = date,
            timings = PrayerTimings(
                fajr = millisOf("Fajr"),
                sunrise = millisOf("Sunrise"),
                dhuhr = millisOf("Dhuhr"),
                asr = millisOf("Asr"),
                maghrib = millisOf("Maghrib"),
                isha = millisOf("Isha"),
            ),
            hijri = HijriDate(
                day = hijriObj.optString("day", ""),
                monthEn = hijriObj.optJSONObject("month")?.optString("en", "").orEmpty(),
                year = hijriObj.optString("year", ""),
            ),
            meta = PrayerMeta(
                methodId = methodObj?.optInt("id", -1) ?: -1,
                methodName = methodObj?.optString("name", "").orEmpty(),
                timezone = tzId,
            ),
        )
    }
}

/**
 * AlAdhan method IDs (https://aladhan.com/calculation-methods).
 * Custom angles are unsupported by the API → fall back to MWL (3).
 */
fun CalculationMethod.aladhanId(): Int = when (this) {
    CalculationMethod.Karachi -> 1
    CalculationMethod.MuslimWorldLeague -> 3
    CalculationMethod.UmmAlQura -> 4
    CalculationMethod.Egyptian -> 5
    CalculationMethod.Kuwait -> 9
    CalculationMethod.Qatar -> 10
    CalculationMethod.Singapore -> 11
    CalculationMethod.MoonsightingCommittee -> 15
    CalculationMethod.Dubai -> 16
    CalculationMethod.Custom -> 3
}
