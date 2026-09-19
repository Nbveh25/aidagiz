package com.example.homework.core.data.repository

import com.example.homework.core.data.source.api.GuideApiReachability
import com.example.homework.core.data.source.api.isGuideUnavailable
import com.example.homework.core.data.source.overpass.OverpassSource
import com.example.homework.core.data.source.place.HistoricalPlacesApiSource
import com.example.homework.core.domain.repository.HistoricalPlacesRepository
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.PlaceCategory
import com.example.homework.entity.map.YearRange
import com.example.homework.entity.place.HistoricalPlaceDetails

class HistoricalPlacesRepositoryImpl(
    private val historicalPlacesApiSource: HistoricalPlacesApiSource,
    private val overpassSource: OverpassSource,
    private val guideApiReachability: GuideApiReachability,
) : HistoricalPlacesRepository {
    override suspend fun getHistoricalPlaces(
        location: GeoLocation,
        radiusMeters: Int,
        years: YearRange,
    ): List<OsmPlace> {
        if (!guideApiReachability.isDown()) {
            val apiResult = runGuideCall {
                historicalPlacesApiSource.getHistoricalPlaces(location, radiusMeters, years)
            }
            if (apiResult.isSuccess) {
                guideApiReachability.markUp()
                return apiResult.getOrNull().orEmpty()
            }
            val apiError = apiResult.exceptionOrNull()
            if (apiError != null && apiError.isGuideUnavailable()) {
                guideApiReachability.markDown()
                return historicFromOverpass(location, radiusMeters)
            }
            throw apiError ?: IllegalStateException("Historical places request failed")
        }
        return historicFromOverpass(location, radiusMeters)
    }

    override fun cachedPlaceDetails(placeId: String): HistoricalPlaceDetails? =
        historicalPlacesApiSource.cachedPlaceDetails(placeId)

    override suspend fun getPlaceDetails(placeId: String): HistoricalPlaceDetails =
        historicalPlacesApiSource.getPlaceDetails(placeId)

    private suspend fun historicFromOverpass(
        location: GeoLocation,
        radiusMeters: Int,
    ): List<OsmPlace> =
        overpassSource.getNearbyPlaces(location, radiusMeters)
            .filter { place ->
                place.category == PlaceCategory.Historic || place.category == PlaceCategory.Attraction
            }
}
