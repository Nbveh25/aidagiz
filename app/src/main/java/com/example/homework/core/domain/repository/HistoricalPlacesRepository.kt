package com.example.homework.core.domain.repository

import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.YearRange
import com.example.homework.entity.place.HistoricalPlaceDetails

interface HistoricalPlacesRepository {
    suspend fun getHistoricalPlaces(
        location: GeoLocation,
        radiusMeters: Int,
        years: YearRange,
    ): List<OsmPlace>

    fun cachedPlaceDetails(placeId: String): HistoricalPlaceDetails?

    suspend fun getPlaceDetails(placeId: String): HistoricalPlaceDetails
}
