package com.example.homework.core.data.repository

import com.example.homework.core.data.source.route.RouteApiSource
import com.example.homework.core.domain.repository.RouteRepository
import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.tour.AdventureRoute

class RouteRepositoryImpl(
    private val routeApiSource: RouteApiSource,
) : RouteRepository {
    override suspend fun buildRoute(location: GeoLocation): AdventureRoute =
        routeApiSource.buildRoute(location)

    override suspend fun rebuildRoute(
        userLocation: GeoLocation,
        visitedPlaces: List<GeoLocation>,
        remainingPlaces: List<GeoLocation>,
        aiRequest: String,
    ): AdventureRoute = routeApiSource.rebuildRoute(
        userLocation = userLocation,
        visitedPlaces = visitedPlaces,
        remainingPlaces = remainingPlaces,
        aiRequest = aiRequest,
    )
}
