package com.vibeprayer.app.data.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException

data class LatLng(val lat: Double, val lng: Double, val label: String = "")

class LocationTracker(private val context: Context) {
    @SuppressLint("MissingPermission")
    suspend fun current(): LatLng? {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val task = client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            )
            val loc = suspendCancellableCoroutine { cont ->
                task.addOnSuccessListener { cont.resume(it, null) }
                task.addOnFailureListener { cont.resumeWithException(it) }
            } ?: return lastKnown()
            LatLng(loc.latitude, loc.longitude)
        } catch (_: Exception) {
            lastKnown()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun lastKnown(): LatLng? = withContext(Dispatchers.IO) {
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val loc = com.google.android.gms.tasks.Tasks.await(client.lastLocation)
            loc?.let { LatLng(it.latitude, it.longitude) }
        } catch (_: Exception) {
            null
        }
    }

    fun geocode(query: String): LatLng? {
        return try {
            @Suppress("DEPRECATION")
            val results = android.location.Geocoder(context).getFromLocationName(query, 1)
            val first = results?.firstOrNull() ?: return null
            LatLng(first.latitude, first.longitude, first.getAddressLine(0) ?: query)
        } catch (_: Exception) {
            null
        }
    }
}
