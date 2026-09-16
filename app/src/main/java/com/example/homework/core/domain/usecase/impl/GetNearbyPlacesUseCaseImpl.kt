package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.PlacesRepository
import com.example.homework.core.domain.usecase.GetNearbyPlacesUseCase
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.clampToKazan

class GetNearbyPlacesUseCaseImpl(
    private val placesRepository: PlacesRepository,
) : GetNearbyPlacesUseCase {
    override suspend fun invoke(
        location: GeoLocation,
        radiusMeters: Int,
        limit: Int,
    ): List<OsmPlace> =
        placesRepository.getPlacesInArea(location.clampToKazan(), radiusMeters, limit)
}
