package com.example.homework.entity.map

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

val KazanCenter = GeoLocation(lat = 55.7908, lon = 49.1144)

const val KazanMinLat = 55.55
const val KazanMaxLat = 56.05
const val KazanMinLon = 48.75
const val KazanMaxLon = 49.45

fun GeoLocation.clampToKazan(): GeoLocation = copy(
    lat = lat.coerceIn(KazanMinLat, KazanMaxLat),
    lon = lon.coerceIn(KazanMinLon, KazanMaxLon),
)

fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val earthRadius = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (earthRadius * c).toInt()
}

fun formatDistance(meters: Int): String =
    if (meters < 1000) "$meters м" else "%.1f км".format(meters / 1000f)
