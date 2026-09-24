package com.meeqat.azan.data.repo

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Reverse-geocoding repository with offline-first strategy.
 *
 * 1) Android [Geocoder] (native, may work offline if provider has cached data).
 * 2) Fallback to OpenStreetMap Nominatim reverse API (requires network).
 *
 * Both branches are wrapped in comprehensive error handling and return null on
 * any failure, so callers can gracefully degrade to showing coordinates only.
 *
 * Nominatim usage policy: https://operations.osmfoundation.org/policies/nominatim/
 * - Must set a valid User-Agent / Referer, rate limit 1 req/sec.
 * - This implementation uses HttpURLConnection (no extra dependency) with
 *   timeouts and proper stream closing.
 */
class NominatimRepository constructor(
    private val context: Context
) {

    /**
     * Reverse geocodes [lat]/[lng] to a short human-readable string like
     * "Riyadh, Riyadh Province" or "Jeddah, Makkah".
     * Returns null if neither strategy yields a result or on any error.
     */
    suspend fun reverseGeocode(lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        require(lat in -90.0..90.0)
        require(lng in -180.0..180.0)

        // Strategy A: platform Geocoder (works offline on some devices, no API key).
        geocodeViaPlatform(lat, lng)?.let { return@withContext it }

        // Strategy B: Nominatim HTTPS (requires network, but no key).
        geocodeViaNominatim(lat, lng)
    }

    // -------------------------------------------------------------------------
    // Strategy A — Android Geocoder
    // -------------------------------------------------------------------------

    private suspend fun geocodeViaPlatform(lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(context, Locale.getDefault())

            val addresses: List<Address>? = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Use synchronous compat shim via reflection to avoid callback complexity on API 33+.
                    // The async API requires a listener; we keep the deprecated sync path which still works.
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lng, 1)
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lng, 1)
                }
            } catch (_: Exception) {
                null
            }

            if (addresses.isNullOrEmpty()) return@withContext null
            val a = addresses[0]
            formatAddress(a)
        } catch (_: Exception) {
            null
        }
    }

    private fun formatAddress(a: Address): String? {
        // Prefer locality (city), then subAdminArea, then adminArea
        val city = a.locality ?: a.subLocality ?: a.subAdminArea ?: a.featureName
        val state = a.adminArea
        val country = a.countryName

        return when {
            !city.isNullOrBlank() && !state.isNullOrBlank() -> "$city, $state"
            !city.isNullOrBlank() -> city
            !state.isNullOrBlank() && !country.isNullOrBlank() -> "$state, $country"
            !state.isNullOrBlank() -> state
            !country.isNullOrBlank() -> country
            else -> {
                // Fallback to address line
                val line = a.getAddressLine(0)
                if (!line.isNullOrBlank()) line else null
            }
        }
    }

    // -------------------------------------------------------------------------
    // Strategy B — Nominatim (OpenStreetMap)
    // -------------------------------------------------------------------------

    private fun geocodeViaNominatim(lat: Double, lng: Double): String? {
        var connection: HttpURLConnection? = null
        var reader: BufferedReader? = null
        return try {
            val urlString = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lng&zoom=10&addressdetails=1"
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            // Nominatim requires a descriptive User-Agent with contact info
            connection.setRequestProperty("User-Agent", "Meeqat/1.0 (com.meeqat.azan)")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Accept-Language", Locale.getDefault().language)
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.useCaches = false

            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) return null

            reader = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
            val response = reader.readText()
            if (response.isBlank()) return null

            parseNominatimResponse(response)
        } catch (_: Exception) {
            null
        } finally {
            try { reader?.close() } catch (_: Exception) {}
            try { connection?.disconnect() } catch (_: Exception) {}
        }
    }

    private fun parseNominatimResponse(json: String): String? {
        return try {
            val root = JSONObject(json)
            // Prefer display_name short, but build from address parts for cleaner output
            val address = root.optJSONObject("address")
            if (address != null) {
                val city = address.optString("city").takeIf { it.isNotBlank() }
                    ?: address.optString("town").takeIf { it.isNotBlank() }
                    ?: address.optString("village").takeIf { it.isNotBlank() }
                    ?: address.optString("municipality").takeIf { it.isNotBlank() }
                    ?: address.optString("county").takeIf { it.isNotBlank() }
                    ?: address.optString("hamlet").takeIf { it.isNotBlank() }

                val state = address.optString("state").takeIf { it.isNotBlank() }
                    ?: address.optString("region").takeIf { it.isNotBlank() }
                    ?: address.optString("province").takeIf { it.isNotBlank() }
                    ?: address.optString("state_district").takeIf { it.isNotBlank() }

                val country = address.optString("country").takeIf { it.isNotBlank() }

                when {
                    !city.isNullOrBlank() && !state.isNullOrBlank() -> "$city, $state"
                    !city.isNullOrBlank() -> city
                    !state.isNullOrBlank() && !country.isNullOrBlank() -> "$state, $country"
                    !city.isNullOrBlank() && !country.isNullOrBlank() -> "$city, $country"
                    !state.isNullOrBlank() -> state
                    !country.isNullOrBlank() -> country
                    else -> root.optString("display_name").takeIf { it.isNotBlank() }
                }
            } else {
                root.optString("display_name").takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            null
        }
    }
}
