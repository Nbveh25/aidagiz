package com.example.homework.core.data.repository

import com.example.homework.core.data.source.overpass.OverpassSource
import com.example.homework.core.domain.repository.OverpassRepository
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace

class OverpassRepositoryImpl(
    private val overpassSource: OverpassSource,
) : OverpassRepository {
    override suspend fun getNearbyPlaces(location: GeoLocation): List<OsmPlace> =
        overpassSource.getNearbyPlaces(location)
}
