package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.HistoricalPlacesRepository
import com.example.homework.core.domain.usecase.GetHistoricalPlacesUseCase
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.YearRange
import com.example.homework.entity.map.clampToKazan

class GetHistoricalPlacesUseCaseImpl(
    private val historicalPlacesRepository: HistoricalPlacesRepository,
) : GetHistoricalPlacesUseCase {
    override suspend fun invoke(
        location: GeoLocation,
        radiusMeters: Int,
        years: YearRange,
    ): List<OsmPlace> =
        historicalPlacesRepository.getHistoricalPlaces(location.clampToKazan(), radiusMeters, years)
}
