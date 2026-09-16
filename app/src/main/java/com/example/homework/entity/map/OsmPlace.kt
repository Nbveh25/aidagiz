package com.example.homework.entity.map

data class OsmPlace(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val category: PlaceCategory,
    val description: String = "",
    val address: String? = null,
    val imageUrl: String? = null,
    val categoryIconUrl: String? = null,
    val openingHours: String? = null,
) {
    fun distanceMetersTo(location: GeoLocation): Int =
        distanceMeters(lat, lon, location.lat, location.lon)
}
