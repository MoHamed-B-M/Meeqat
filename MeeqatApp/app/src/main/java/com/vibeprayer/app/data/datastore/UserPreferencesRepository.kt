package com.vibeprayer.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vibeprayer.app.data.model.LocationMode
import com.vibeprayer.app.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vibeStore by preferencesDataStore("vibe_prefs")

class UserPreferencesRepository(private val context: Context) {
    private val methodKey = intPreferencesKey("method_id")
    private val redKey = booleanPreferencesKey("red_accents")
    private val themeKey = stringPreferencesKey("theme")
    private val modeKey = stringPreferencesKey("location_mode")
    private val latKey = doublePreferencesKey("manual_lat")
    private val lngKey = doublePreferencesKey("manual_lng")
    private val labelKey = stringPreferencesKey("manual_label")
    private val adhanKey = stringPreferencesKey("adhan_uri")

    private fun offsetKey(prayer: String) = intPreferencesKey("offset_$prayer")

    val prayers = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")

    val settings: Flow<UserSettings> = context.vibeStore.data.map { p ->
        val offsets = prayers.associateWith { name ->
            (p[offsetKey(name)] ?: 0).coerceIn(-30, 30)
        }
        UserSettings(
            locationMode = if (p[modeKey] == "manual") LocationMode.Manual else LocationMode.Automatic,
            manualLat = p[latKey],
            manualLng = p[lngKey],
            manualLabel = p[labelKey] ?: "",
            methodId = p[methodKey] ?: 4,
            redAccents = p[redKey] ?: true,
            theme = p[themeKey] ?: "oled",
            offsets = offsets,
            adhanUri = p[adhanKey] ?: ""
        )
    }

    suspend fun setMethod(id: Int) {
        context.vibeStore.edit { it[methodKey] = id }
    }

    suspend fun setRedAccents(enabled: Boolean) {
        context.vibeStore.edit { it[redKey] = enabled }
    }

    suspend fun setTheme(theme: String) {
        context.vibeStore.edit { it[themeKey] = theme }
    }

    suspend fun setLocationMode(mode: LocationMode) {
        context.vibeStore.edit { it[modeKey] = if (mode == LocationMode.Manual) "manual" else "auto" }
    }

    suspend fun setManualLocation(lat: Double, lng: Double, label: String) {
        context.vibeStore.edit {
            it[latKey] = lat
            it[lngKey] = lng
            it[labelKey] = label
            it[modeKey] = "manual"
        }
    }

    suspend fun setOffset(prayer: String, minutes: Int) {
        context.vibeStore.edit { it[offsetKey(prayer)] = minutes.coerceIn(-30, 30) }
    }

    suspend fun setAdhanUri(uri: String) {
        context.vibeStore.edit { it[adhanKey] = uri }
    }
}
