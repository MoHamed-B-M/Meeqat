package com.meeqat.azan.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.meeqat.azan.data.local.AppDatabase
import com.meeqat.azan.data.local.DailyPrayerEntity
import com.meeqat.azan.data.local.ManualOffsetEntity
import com.meeqat.azan.domain.model.CalculationMethod
import com.meeqat.azan.domain.model.DailyPrayerTimes
import com.meeqat.azan.domain.model.HighLatitudeRule
import com.meeqat.azan.domain.model.LocationState
import com.meeqat.azan.domain.model.LocationSource
import com.meeqat.azan.domain.model.Madhab
import com.meeqat.azan.domain.model.ManualOffset
import com.meeqat.azan.domain.model.SoundConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

private val Context.dataStore by preferencesDataStore(name = "meeqat_prefs")

class SettingsRepository constructor(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    object Keys {
        val method = stringPreferencesKey("method")
        val madhab = stringPreferencesKey("madhab")
        val highLat = stringPreferencesKey("high_lat")
        val offsetJson = stringPreferencesKey("offset_json")
        val soundJson = stringPreferencesKey("sound_json")
        val lat = doublePreferencesKey("lat")
        val lng = doublePreferencesKey("lng")
        val city = stringPreferencesKey("city")
        val locationSource = stringPreferencesKey("location_source")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val globalOffset = intPreferencesKey("global_offset")
        val appIconColor = stringPreferencesKey("app_icon_color")
    }

    val methodFlow: Flow<CalculationMethod> = context.dataStore.data.map {
        runCatching { CalculationMethod.valueOf(it[Keys.method] ?: "UmmAlQura") }.getOrDefault(CalculationMethod.UmmAlQura)
    }
    val madhabFlow: Flow<Madhab> = context.dataStore.data.map {
        runCatching { Madhab.valueOf(it[Keys.madhab] ?: "Shafii") }.getOrDefault(Madhab.Shafii)
    }
    val highLatFlow: Flow<HighLatitudeRule> = context.dataStore.data.map {
        runCatching { HighLatitudeRule.valueOf(it[Keys.highLat] ?: "AngleBased") }.getOrDefault(HighLatitudeRule.AngleBased)
    }
    val offsetFlow: Flow<ManualOffset> = context.dataStore.data.map {
        it[Keys.offsetJson]?.let { s -> runCatching { json.decodeFromString<ManualOffset>(s) }.getOrNull() } ?: ManualOffset()
    }
    val soundFlow: Flow<SoundConfig> = context.dataStore.data.map {
        it[Keys.soundJson]?.let { s -> runCatching { json.decodeFromString<SoundConfig>(s) }.getOrNull() } ?: SoundConfig()
    }
    val locationFlow: Flow<LocationState?> = context.dataStore.data.map { p ->
        val lat = p[Keys.lat] ?: return@map null
        val lng = p[Keys.lng] ?: return@map null
        LocationState(lat, lng, p[Keys.city], null, runCatching { LocationSource.valueOf(p[Keys.locationSource] ?: "Cached") }.getOrDefault(LocationSource.Cached))
    }
    val dynamicColorFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.dynamicColor] ?: true }
    val globalOffsetFlow: Flow<Int> = context.dataStore.data.map { it[Keys.globalOffset] ?: 0 }
    val appIconColorFlow: Flow<String> = context.dataStore.data.map { it[Keys.appIconColor] ?: "emerald" }

    suspend fun setMethod(v: CalculationMethod) = context.dataStore.edit { it[Keys.method] = v.name }
    suspend fun setMadhab(v: Madhab) = context.dataStore.edit { it[Keys.madhab] = v.name }
    suspend fun setHighLat(v: HighLatitudeRule) = context.dataStore.edit { it[Keys.highLat] = v.name }
    suspend fun setOffset(v: ManualOffset) = context.dataStore.edit { it[Keys.offsetJson] = json.encodeToString(v) }
    suspend fun setSound(v: SoundConfig) = context.dataStore.edit { it[Keys.soundJson] = json.encodeToString(v) }
    suspend fun setLocation(v: LocationState) = context.dataStore.edit {
        it[Keys.lat] = v.latitude; it[Keys.lng] = v.longitude
        if (v.city != null) it[Keys.city] = v.city else it.remove(Keys.city)
        it[Keys.locationSource] = v.source.name
    }
    suspend fun setDynamicColor(v: Boolean) = context.dataStore.edit { it[Keys.dynamicColor] = v }
    suspend fun setGlobalOffset(v: Int) = context.dataStore.edit { it[Keys.globalOffset] = v }
    suspend fun setAppIconColor(v: String) = context.dataStore.edit { it[Keys.appIconColor] = v }
}

class CalculationRepository constructor(
    private val db: AppDatabase,
    private val settings: SettingsRepository,
) {
    // Offline-first engine: uses PrayerEngine (adhan) for astronomical calc — no network.
    suspend fun refresh30Days(lat: Double, lng: Double, zoneId: ZoneId = ZoneId.systemDefault()) {
        val today = LocalDate.now(zoneId)
        val method = settings.methodFlow.first()
        val madhab = settings.madhabFlow.first()
        val highLat = settings.highLatFlow.first()
        val entities = (0 until 30).map { i ->
            val date = today.plusDays(i.toLong())
            val times = com.meeqat.azan.domain.engine.PrayerEngine.calculateDailyPrayers(
                latitude = lat, longitude = lng, date = date, zoneId = zoneId,
                method = method, madhab = madhab, highLatRule = highLat
            )
            DailyPrayerEntity(
                date = times.date,
                fajr = times.fajr,
                sunrise = times.sunrise,
                dhuhr = times.dhuhr,
                asr = times.asr,
                maghrib = times.maghrib,
                isha = times.isha,
                method = times.method,
                lat = lat,
                lng = lng,
            )
        }
        db.dailyPrayerDao().insertAll(entities)
    }

    suspend fun todayTimes(zoneId: ZoneId = ZoneId.systemDefault()): DailyPrayerTimes? {
        val date = LocalDate.now(zoneId).toString()
        val e = db.dailyPrayerDao().getByDate(date) ?: return null
        val offset = settings.offsetFlow.first()
        val global = settings.globalOffsetFlow.first()
        fun adj(base: Long, delta: Int) = base + (delta + global) * 60L * 1000L
        return DailyPrayerTimes(
            date = e.date,
            fajr = adj(e.fajr, offset.fajr),
            sunrise = adj(e.sunrise, offset.sunrise),
            dhuhr = adj(e.dhuhr, offset.dhuhr),
            asr = adj(e.asr, offset.asr),
            maghrib = adj(e.maghrib, offset.maghrib),
            isha = adj(e.isha, offset.isha),
            method = e.method,
            lat = e.lat,
            lng = e.lng,
        )
    }

    fun observeAll() = db.dailyPrayerDao().observeAll()
}

class LocationRepository constructor(
    private val context: Context,
    private val settings: SettingsRepository,
) {
    fun observeLocation() = settings.locationFlow
    suspend fun saveLocation(state: LocationState) = settings.setLocation(state)
}

class SoundRepository constructor(private val settings: SettingsRepository) {
    fun observe() = settings.soundFlow
    suspend fun set(config: SoundConfig) = settings.setSound(config)
}
