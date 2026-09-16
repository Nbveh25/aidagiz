package com.example.homework.core.domain.repository

import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace

interface PlacesRepository {
    suspend fun getPlacesInArea(
        location: GeoLocation,
        radiusMeters: Int = 5000,
        limit: Int = 300,
    ): List<OsmPlace>
}
