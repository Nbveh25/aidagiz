package com.example.homework.core.domain.usecase.impl

import com.example.homework.core.domain.repository.RouteRepository
import com.example.homework.core.domain.usecase.RebuildAdventureRouteUseCase
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.tour.AdventureRoute

class RebuildAdventureRouteUseCaseImpl(
    private val routeRepository: RouteRepository,
) : RebuildAdventureRouteUseCase {
    override suspend fun invoke(
        userLocation: GeoLocation,
        visitedPlaces: List<GeoLocation>,
        remainingPlaces: List<GeoLocation>,
        aiRequest: String,
        currentAt: String?,
    ): AdventureRoute = routeRepository.rebuildRoute(
        userLocation = userLocation,
        visitedPlaces = visitedPlaces,
        remainingPlaces = remainingPlaces,
        aiRequest = aiRequest,
        currentAt = currentAt,
    )
}
