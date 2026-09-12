package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.OverpassRepository
import com.example.homework.core.domain.usecase.GetNearbyPlacesUseCase
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.map.OsmPlace

class GetNearbyPlacesUseCaseImpl(
    private val overpassRepository: OverpassRepository,
) : GetNearbyPlacesUseCase {
    override suspend fun invoke(location: GeoLocation): List<OsmPlace> =
        overpassRepository.getNearbyPlaces(location)
}
