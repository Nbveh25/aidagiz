package com.example.homework.core.data.repository

import com.example.homework.core.data.source.overpass.OverpassSource
import com.example.homework.core.data.source.place.PlacesApiSource
import com.example.homework.core.domain.repository.PlacesRepository
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace

class PlacesRepositoryImpl(
    private val placesApiSource: PlacesApiSource,
    private val overpassSource: OverpassSource,
) : PlacesRepository {
    override suspend fun getPlacesInArea(
        location: GeoLocation,
        radiusMeters: Int,
        limit: Int,
    ): List<OsmPlace> {
        val apiResult = runCatching { placesApiSource.getPlacesInArea(location) }
        val apiPlaces = apiResult.getOrNull()
        if (!apiPlaces.isNullOrEmpty()) return apiPlaces.take(limit)
        val overpassPlaces = runCatching {
            overpassSource.getNearbyPlaces(location, radiusMeters, limit)
        }.getOrNull()
        if (!overpassPlaces.isNullOrEmpty()) return overpassPlaces
        apiResult.getOrThrow()
        return emptyList()
    }
}
