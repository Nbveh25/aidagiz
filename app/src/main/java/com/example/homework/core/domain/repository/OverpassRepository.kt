package com.example.homework.core.domain.repository

import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace

interface OverpassRepository {
    suspend fun getNearbyPlaces(location: GeoLocation): List<OsmPlace>
}
