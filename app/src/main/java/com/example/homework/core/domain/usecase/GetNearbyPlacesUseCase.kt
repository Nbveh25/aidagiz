package com.example.homework.core.domain.usecase

import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace

interface GetNearbyPlacesUseCase {
    suspend operator fun invoke(
        location: GeoLocation,
        radiusMeters: Int = 5000,
        limit: Int = 300,
    ): List<OsmPlace>
}
