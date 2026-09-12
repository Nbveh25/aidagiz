package com.example.homework.entity.map

data class OsmPlace(
    val id: Long,
    val name: String,
    val lat: Double,
    val lon: Double,
    val category: PlaceCategory,
    val openingHours: String? = null,
) {
    fun distanceMetersTo(location: GeoLocation): Int =
        distanceMeters(lat, lon, location.lat, location.lon)
}
