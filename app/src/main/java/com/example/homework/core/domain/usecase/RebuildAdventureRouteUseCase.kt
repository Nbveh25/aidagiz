package com.example.homework.core.domain.usecase

import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.tour.AdventureRoute

interface RebuildAdventureRouteUseCase {
    suspend operator fun invoke(
        userLocation: GeoLocation,
        visitedPlaces: List<GeoLocation>,
        remainingPlaces: List<GeoLocation>,
        aiRequest: String,
        currentAt: String? = null,
    ): AdventureRoute
}
