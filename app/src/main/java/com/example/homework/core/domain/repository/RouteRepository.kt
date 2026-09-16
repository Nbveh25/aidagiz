package com.example.homework.core.domain.repository

import com.example.homework.entity.map.GeoLocation
import com.example.homework.entity.tour.AdventureRoute

interface RouteRepository {
    suspend fun buildRoute(location: GeoLocation): AdventureRoute
    suspend fun rebuildRoute(
        userLocation: GeoLocation,
        visitedPlaces: List<GeoLocation>,
        remainingPlaces: List<GeoLocation>,
        aiRequest: String,
    ): AdventureRoute
}
