package com.example.homework.core.data.source.userLocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.homework.entity.map.GeoLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class UserLocationSource(
    private val context: Context,
) {
    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun getUserLocation(): Flow<GeoLocation> = callbackFlow {
        val manager = context.getSystemService(LocationManager::class.java)
        if (manager == null) {
            close()
            return@callbackFlow
        }

        val listener = LocationListener { location ->
            trySend(location.toGeoLocation())
        }

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
        ).filter { manager.isProviderEnabled(it) }

        providers
            .mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }
            ?.let { trySend(it.toGeoLocation()) }

        providers.forEach { provider ->
            manager.requestLocationUpdates(
                provider,
                2_000L,
                5f,
                listener,
                Looper.getMainLooper(),
            )
        }

        awaitClose { manager.removeUpdates(listener) }
    }

    private fun Location.toGeoLocation() = GeoLocation(
        lat = latitude,
        lon = longitude,
        accuracyMeters = accuracy,
        bearing = bearing,
    )
}
