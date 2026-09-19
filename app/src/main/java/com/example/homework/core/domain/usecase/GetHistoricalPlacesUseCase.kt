package com.example.homework.core.domain.usecase

import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.YearRange

interface GetHistoricalPlacesUseCase {
    suspend operator fun invoke(
        location: GeoLocation,
        radiusMeters: Int = 10_000,
        years: YearRange,
    ): List<OsmPlace>
}
