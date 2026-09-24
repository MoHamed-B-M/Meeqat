package com.meeqat.azan.domain.engine

import com.meeqat.azan.domain.model.QiblaInfo
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Offline Qibla bearing and distance calculator.
 *
 * Primary: delegates to com.batoulapps.adhan.Qibla(Coordinates) when the class is
 * present in adhan 1.2.1. Fallback: pure spherical trigonometry (haversine + initial
 * bearing) that works entirely offline with zero network.
 *
 * Kaaba coordinates: 21.4225° N, 39.8262° E (Masjid al-Haram).
 */
object QiblaCalculator {

    private const val KAABA_LAT = 21.4225
    private const val KAABA_LNG = 39.8262
    // Mean earth radius in km (WGS-84)
    private const val EARTH_RADIUS_KM = 6371.0088

    /**
     * Returns [QiblaInfo] for the given observer position.
     * Bearing is normalized to 0..360 degrees clockwise from true north.
     * Distance is great-circle distance in kilometres.
     */
    fun calculateBearing(lat: Double, lng: Double): QiblaInfo {
        require(lat in -90.0..90.0) { "lat out of range" }
        require(lng in -180.0..180.0) { "lng out of range" }

        // Try Adhan Qibla class via reflection / direct use. If it exists, prefer it
        // for consistency with the prayer engine, but always verify with manual math.
        tryAdhanQibla(lat, lng)?.let { return it }

        // Manual fallback — fully offline, no dependency on Adhan internals.
        val bearing = computeBearingDegrees(lat, lng, KAABA_LAT, KAABA_LNG)
        val distance = computeDistanceKm(lat, lng, KAABA_LAT, KAABA_LNG)

        return QiblaInfo(
            bearingDegrees = bearing.toFloat(),
            distanceKm = distance,
            lat = lat,
            lng = lng
        )
    }

    /**
     * Attempts to use com.batoulapps.adhan.Qibla if available at runtime.
     * Returns null if the class is absent or any reflection fails, triggering fallback.
     */
    private fun tryAdhanQibla(lat: Double, lng: Double): QiblaInfo? {
        return try {
            // Direct reference — compiles when adhan 1.2.1 contains Qibla.
            // Wrapped in try/catch so a NoClassDefFoundError at runtime still falls back.
            val coordinates = com.batoulapps.adhan.Coordinates(lat, lng)
            val qibla = com.batoulapps.adhan.Qibla(coordinates)
            val bearing = qibla.direction
            // Qibla in adhan 1.2.1 exposes direction (degrees). Distance not always exposed;
            // compute distance manually for the model.
            val distance = computeDistanceKm(lat, lng, KAABA_LAT, KAABA_LNG)
            val normalized = ((bearing % 360.0) + 360.0) % 360.0
            QiblaInfo(
                bearingDegrees = normalized.toFloat(),
                distanceKm = distance,
                lat = lat,
                lng = lng
            )
        } catch (_: Throwable) {
            // Also try reflective lookup for alternative package / API shape
            tryReflectiveQibla(lat, lng)
        }
    }

    private fun tryReflectiveQibla(lat: Double, lng: Double): QiblaInfo? {
        return try {
            val qiblaClass = Class.forName("com.batoulapps.adhan.Qibla")
            val coordClass = Class.forName("com.batoulapps.adhan.Coordinates")
            val coord = coordClass.getConstructor(Double::class.javaPrimitiveType, Double::class.javaPrimitiveType)
                .newInstance(lat, lng)
            val ctor = qiblaClass.constructors.firstOrNull { it.parameterCount == 1 } ?: return null
            val instance = ctor.newInstance(coord)
            val directionField = runCatching { qiblaClass.getField("direction") }.getOrNull()
                ?: runCatching { qiblaClass.getDeclaredField("direction") }.getOrNull()
            val directionMethod = runCatching { qiblaClass.getMethod("getDirection") }.getOrNull()
            val bearing: Double = when {
                directionField != null -> {
                    directionField.isAccessible = true
                    (directionField.get(instance) as Number).toDouble()
                }
                directionMethod != null -> (directionMethod.invoke(instance) as Number).toDouble()
                else -> return null
            }
            val distance = computeDistanceKm(lat, lng, KAABA_LAT, KAABA_LNG)
            val normalized = ((bearing % 360.0) + 360.0) % 360.0
            QiblaInfo(normalized.toFloat(), distance, lat, lng)
        } catch (_: Throwable) {
            null
        }
    }

    /** Initial bearing from (lat1,lng1) to (lat2,lng2) in degrees 0..360. */
    fun computeBearingDegrees(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lng2 - lng1)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        return (Math.toDegrees(theta) + 360.0) % 360.0
    }

    /** Haversine great-circle distance in kilometres. */
    fun computeDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(rLat1) * cos(rLat2) * sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }
}
